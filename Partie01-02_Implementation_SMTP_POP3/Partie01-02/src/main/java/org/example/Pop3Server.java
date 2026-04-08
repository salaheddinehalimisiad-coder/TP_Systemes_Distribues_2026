package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

public class Pop3Server {
    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ServerLogger logger;
    private List<Pop3Session> activeSessions = Collections.synchronizedList(new ArrayList<>());

    public Pop3Server(int port, ServerLogger logger) {
        this.port = port;
        this.logger = logger;
    }

    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                logger.log("POP3 Server started on port " + port);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.log("POP3: New connection from " + clientSocket.getInetAddress());
                        Pop3Session session = new Pop3Session(clientSocket, logger, this);
                        activeSessions.add(session);
                        session.start();
                    } catch (IOException e) {
                        if (running) logger.log("POP3 Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.log("POP3 could not listen on port " + port);
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (Pop3Session session : activeSessions) {
                session.closeSocket();
            }
            activeSessions.clear();
            logger.log("POP3 Server stopped.");
        } catch (IOException e) {
            logger.log("POP3 error stopping: " + e.getMessage());
        }
    }

    public void removeSession(Pop3Session session) {
        activeSessions.remove(session);
    }

    public int getConnectedCount() {
        return activeSessions.size();
    }

    public static void main(String[] args) {
        new Pop3Server(110, System.out::println).start();
    }
}

class Pop3Session extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private File userDir;
    private List<File> emails;
    private boolean authenticated;
    private List<Boolean> deletionFlags;
    private ServerLogger logger;
    private Pop3Server server;

    public Pop3Session(Socket socket, ServerLogger logger, Pop3Server server) {
        this.socket = socket;
        this.logger = logger;
        this.server = server;
        this.authenticated = false;
    }

    public void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            send("+OK POP3 server ready");

            String line;
            // 1. Lit chaque ligne envoyée par le client (via le flux 'in') tant que la
            // connexion est ouverte.
            while ((line = in.readLine()) != null) {

                // 2. Affiche dans la console du serveur ce qui a été reçu (utile pour le
                // débogage).
                logger.log("POP3 Received: " + line);

                // 3. Découpe la ligne reçue en deux parties maximum, en utilisant l'espace
                // comme séparateur.
                // Le "2" signifie qu'on s'arrête au premier espace rencontré :
                // tout ce qui suit le premier espace est considéré comme un seul argument.
                String[] parts = line.split(" ", 2);

                // 4. La première partie (index 0) correspond au nom de la commande (ex: "USER",
                // "RETR").
                // Elle est convertie en MAJUSCULES pour faciliter la comparaison dans le
                // 'switch'.
                String command = parts[0].toUpperCase();

                // 5. La deuxième partie (index 1) correspond à l'argument de la commande (ex:
                // le nom d'utilisateur).
                // Si la commande n'a pas d'argument (ex: "STAT"), on utilise une chaîne vide
                // "".
                String argument = parts.length > 1 ? parts[1] : "";

                switch (command.toUpperCase()) {
                    case "USER":
                        handleUser(argument);
                        break;
                    case "PASS":
                        handlePass(argument);
                        break;
                    case "STAT":
                        handleStat();
                        break;
                    case "LIST":
                        handleList();
                        break;
                    case "RETR":
                        handleRetr(argument);
                        break;
                    case "DELE":
                        handleDele(argument);
                        break;
                    case "RSET":
                        handleRset();
                        break;
                    case "NOOP":
                        send("+OK");
                        break;
                    case "CAPA":
                        handleCapa();
                        break;
                    case "QUIT":
                        handleQuit();
                        return; // Terminer la session
                    default:
                        send("-ERR Unknown command");
                        break;
                }

            }
            // Si la boucle se termine, cela signifie que la connexion a été interrompue
            // sans QUIT.
            if (authenticated) {
                logger.log("POP3 Info: Connection interrupted without QUIT for " + username);
            }
        } catch (IOException e) {
            logger.log("POP3 Session error: " + e.getMessage());
        } finally {
            try {
                server.removeSession(this);
                socket.close();
            } catch (IOException e) {
                /* Ignore */ }
        }
    }

    private void handleUser(String arg) {
        File dir = new File("mailserver/" + arg);
        if (dir.exists() && dir.isDirectory()) {
            username = arg;
            userDir = dir;
            out.println("+OK User accepted");
        } else {
            out.println("-ERR User not found");
        }
    }

    private void handlePass(String arg) {
        if (username == null) {
            out.println("-ERR USER required first");
            return;
        }
        
        // --- DEBUT RMI CHECK ---
        try {
            java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry("127.0.0.1", 1099);
            org.example.auth.IAuthService authService = (org.example.auth.IAuthService) registry.lookup("AuthService");
            String result = authService.authenticate(username, arg);
            if (result.contains("error")) {
                 out.println("-ERR Authentication failed");
                 return;
            }
        } catch (Exception e) {
            logger.log("Erreur RMI (POP3 auth): " + e.getMessage());
            out.println("-ERR Authorization server unavailable");
            return;
        }
        // --- FIN RMI CHECK ---

        authenticated = true;
        // Créer le dossier s'il n'existe pas encore (nouvel utilisateur RMI sans email)
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        
        // Chargez les fichiers du répertoire dans une ArrayList mutable
        File[] files = userDir.listFiles();
        if (files == null) {
            emails = new ArrayList<>();
        } else {
            emails = new ArrayList<>(java.util.Arrays.asList(files));
        }
        // Initialisez les flags de suppression : aucun email n'est marqué (false)
        deletionFlags = new ArrayList<>();
        for (int i = 0; i < emails.size(); i++) {
            deletionFlags.add(false);
        }
        out.println("+OK Password accepted");
    }

    private void handleStat() {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        long size = emails.stream().mapToLong(File::length).sum();
        out.println("+OK " + emails.size() + " " + size);
    }

    private void handleList() {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        out.println("+OK " + emails.size() + " messages");
        for (int i = 0; i < emails.size(); i++) {
            out.println((i + 1) + " " + emails.get(i).length());
        }
        out.println(".");
    }

    private void handleRetr(String arg) {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        try {
            int index = Integer.parseInt(arg) - 1;
            if (index < 0 || index >= emails.size()) {
                out.println("-ERR No such message");
                return;
            }
            File emailFile = emails.get(index);
            out.println("+OK " + emailFile.length() + " octets");
            BufferedReader reader = new BufferedReader(new FileReader(emailFile));
            String line;
            while ((line = reader.readLine()) != null) {
                out.println(line);
            }
            out.println(".");
            reader.close();
        } catch (Exception e) {
            out.println("-ERR Invalid message number");
        }
    }

    private void handleDele(String arg) {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        try {
            arg = arg.trim();
            int index = Integer.parseInt(arg) - 1; // Les messages sont numérotés à partir de 1
            if (index < 0 || index >= emails.size()) {
                out.println("-ERR No such message");
                return;
            }
            // Vérifier si le message est déjà marqué pour suppression
            if (deletionFlags.get(index)) {
                out.println("-ERR Message already marked for deletion");
                return;
            }
            // Marquer le message pour suppression (ne pas le supprimer tout de suite)
            deletionFlags.set(index, true);
            out.println("+OK Message marked for deletion");
        } catch (NumberFormatException nfe) {
            out.println("-ERR Invalid message number");
        } catch (Exception e) {
            out.println("-ERR Invalid message number");
        }
    }

    private void handleRset() {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        // Remise à zéro de tous les flags de suppression
        for (int i = 0; i < deletionFlags.size(); i++) {
            deletionFlags.set(i, false);
        }
        send("+OK Deletion marks reset");
    }

    private void handleQuit() {
        // Pour chaque email marqué pour suppression, supprimez le fichier
        if (authenticated) {
            for (int i = deletionFlags.size() - 1; i >= 0; i--) {
                if (deletionFlags.get(i)) {
                    File emailFile = emails.get(i);
                    if (emailFile.delete()) {
                        logger.log("POP3: Deleted email " + emailFile.getName());
                        emails.remove(i);
                        deletionFlags.remove(i);
                    } else {
                        System.err.println("Failed to delete email: " + emailFile.getAbsolutePath());
                    }
                }
            }
        }
        send("+OK POP3 server signing off");
    }

    private void handleCapa() {
        out.println("+OK Capability list follows");
        send("USER");
        send("RESP-CODES");
        send("EXPIRE 31");
        send("Implementation: CustomJavaPop3Server");
    }

    private void send(String msg) {
        logger.log("SERVER -> " + msg);
        out.println(msg);
    }
}
