package org.example;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Partie 2 du TP Systèmes Distribues – Serveur IMAP
 * Conforme aux principes de l'automate à etats finis (RFC 9051).
 *
 * Commandes implementees : LOGIN, SELECT, FETCH (complet + en-têtes seuls),
 *                          STORE (+FLAGS \Seen), SEARCH (ALL, FROM, SUBJECT),
 *                          LOGOUT, NOOP, CAPABILITY
 *
 * Automate à etats :
 *   NOT_AUTHENTICATED -> (LOGIN reussi) -> AUTHENTICATED
 *   AUTHENTICATED     -> (SELECT reussi) -> SELECTED
 *   Tout etat          -> (LOGOUT)        -> LOGOUT
 */
public class ImapServer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ServerLogger logger;
    private List<ImapSession> activeSessions = Collections.synchronizedList(new ArrayList<>());

    public ImapServer(int port, ServerLogger logger) {
        this.port = port;
        this.logger = logger;
    }

    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                logger.log("IMAP Server started on port " + port);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.log("IMAP: New connection from " + clientSocket.getInetAddress());
                        ImapSession session = new ImapSession(clientSocket, logger, this);
                        activeSessions.add(session);
                        session.start();
                    } catch (IOException e) {
                        if (running) logger.log("IMAP Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.log("IMAP could not listen on port " + port);
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (ImapSession session : activeSessions) {
                session.closeSocket();
            }
            activeSessions.clear();
            logger.log("IMAP Server stopped.");
        } catch (IOException e) {
            logger.log("IMAP error stopping: " + e.getMessage());
        }
    }

    public void removeSession(ImapSession session) {
        activeSessions.remove(session);
    }

    public int getConnectedCount() {
        return activeSessions.size();
    }

    public static void main(String[] args) {
        new ImapServer(143, System.out::println).start();
    }
}

class ImapSession extends Thread {

    // ================================================================
    //  Automate à etats finis — RFC 9051 §3
    // ================================================================
    private enum ImapState {
        NOT_AUTHENTICATED,  // Seules CAPABILITY, NOOP, LOGOUT et LOGIN sont autorisees
        AUTHENTICATED,      // LOGIN reussi. SELECT est maintenant disponible.
        SELECTED,           // Boîte ouverte. FETCH, STORE, SEARCH disponibles.
        LOGOUT              // Session fermee
    }

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private ImapState state = ImapState.NOT_AUTHENTICATED;
    private String username;
    private File currentFolder;
    private List<MessageMetadata> messages = new ArrayList<>();

    // Repertoire racine de stockage des mails (même que SMTP/POP3)
    private static final String MAIL_ROOT = "mailserver";

    private ServerLogger logger;
    private ImapServer server;

    public ImapSession(Socket socket, ServerLogger logger, ImapServer server) {
        this.socket = socket;
        this.logger = logger;
        this.server = server;
    }

    public void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    @Override
    public void run() {
        try {
            // RFC exige ASCII sur le flux réseau — évite les artefacts Telnet Windows
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "US-ASCII")), true);

            // Greeting obligatoire (RFC 9051 §3)
            send("* OK [CAPABILITY IMAP4rev2 AUTH=PLAIN] IMAP Server Ready");

            String line;
            while (state != ImapState.LOGOUT && (line = in.readLine()) != null) {
                logger.log("IMAP Received: " + line);
                handleCommand(line.trim());
            }

        } catch (IOException e) {
            logger.log("IMAP Session error: " + e.getMessage());
        } finally {
            try {
                server.removeSession(this);
                socket.close();
            } catch (IOException e) {
                /* ignore */ }
        }
    }

    /** Envoie une ligne au client avec terminaison CRLF (RFC 9051). */
    private void send(String response) {
        logger.log("SERVER -> " + response);
        out.print(response + "\r\n");
        out.flush();
    }

    // ================================================================
    //  Routeur de commandes : extrait tag + commande + arguments
    // ================================================================
    private void handleCommand(String line) {
        if (line.isEmpty()) return;

        // Format IMAP :  <tag> <COMMANDE> [arguments]
        String[] parts = line.split(" ", 3);
        if (parts.length < 2) {
            send("* BAD Format invalide : ligne vide ou tag manquant");
            return;
        }

        String tag     = parts[0];
        String command = parts[1].toUpperCase();
        String args    = (parts.length > 2) ? parts[2] : "";

        switch (command) {
            case "CAPABILITY":
                send("* CAPABILITY IMAP4rev2 AUTH=PLAIN LITERAL+ IDLE");
                send(tag + " OK CAPABILITY completed");
                break;
            case "NOOP":
                send(tag + " OK NOOP completed");
                break;
            case "LOGIN":
                handleLogin(tag, args);
                break;
            case "SELECT":
                handleSelect(tag, args);
                break;
            case "FETCH":
                handleFetch(tag, args);
                break;
            case "STORE":
                handleStore(tag, args);
                break;
            case "SEARCH":
                handleSearch(tag, args);
                break;
            case "LOGOUT":
                handleLogout(tag);
                break;
            default:
                // Scenario 6 : commande inconnue ou refusee dans cet etat
                send(tag + " BAD Commande inconnue ou non autorisee dans l'etat actuel ["
                        + state.name() + "]");
                break;
        }
    }

    // ================================================================
    //  Scenario 1 & 6 — LOGIN (seulement en etat NOT_AUTHENTICATED)
    // ================================================================
    private void handleLogin(String tag, String args) {
        if (state != ImapState.NOT_AUTHENTICATED) {
            send(tag + " NO Already authenticated");
            return;
        }
        String[] creds = args.split(" ", 2);
        if (creds.length < 2) {
            send(tag + " BAD Usage : LOGIN <utilisateur> <motdepasse>");
            return;
        }

        String user = creds[0].replace("\"", "");
        File dir    = new File(MAIL_ROOT + "/" + user);

        if (dir.exists() && dir.isDirectory()) {
            this.username = user;
            this.state    = ImapState.AUTHENTICATED;
            send(tag + " OK LOGIN successful - welcome " + user);
        } else {
            // User folder not found
            send(tag + " NO [AUTHENTICATIONFAILED] Authentication failed");
        }
    }

    // ================================================================
    //  Scenario 1 & 2 — SELECT
    // ================================================================
    private void handleSelect(String tag, String folderName) {
        if (state == ImapState.NOT_AUTHENTICATED) {
            send(tag + " NO Authentication required before SELECT");
            return;
        }

        folderName = folderName.replace("\"", "").trim();

        // Scenario 2 : boîte inexistante
        if (!folderName.equalsIgnoreCase("INBOX")) {
            send(tag + " NO [NONEXISTENT] Mailbox not found: " + folderName);
            return;
        }

        currentFolder = new File(MAIL_ROOT + "/" + username);
        refreshMessageList();
        state = ImapState.SELECTED;

        send("* " + messages.size() + " EXISTS");
        send("* 0 RECENT");
        send("* OK [UNSEEN 1] First unseen message");
        send("* OK [UIDVALIDITY 1] UIDs valid");
        send("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
        send(tag + " OK [READ-WRITE] SELECT completed");
    }

    /** Recharge la liste des messages depuis le dossier utilisateur. */
    private void refreshMessageList() {
        messages.clear();
        File[] files = currentFolder.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < files.length; i++) {
                // Charge les flags persistes (ou cree une fiche vierge)
                messages.add(MessageMetadata.load(i + 1, files[i]));
            }
        }
    }

    // ================================================================
    //  Scenarios 1 & 3 — FETCH (complet ou en-têtes seuls)
    // ================================================================
    private void handleFetch(String tag, String args) {
        if (state != ImapState.SELECTED) {
            send(tag + " NO Select a mailbox first [current state: "
                    + state.name() + "]");
            return;
        }

        try {
            String[] parts = args.split(" ", 2);
            int id = Integer.parseInt(parts[0]);
            String dataItem = (parts.length > 1) ? parts[1].toUpperCase() : "BODY[]";

            if (id < 1 || id > messages.size()) {
                send(tag + " NO Message " + id + " not found");
                return;
            }

            MessageMetadata meta = messages.get(id - 1);
            File emailFile        = meta.getFile();

            if (dataItem.contains("BODY[HEADER]") || dataItem.contains("RFC822.HEADER")) {
                // ---- Scenario 3 : lecture partielle (en-têtes uniquement) ----
                send("* " + id + " FETCH (BODY[HEADER] {?})");
                StringBuilder headers = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        headers.append(line).append("\r\n");
                    }
                }
                // On envoie le literal : {taille}\r\n <donnees>
                send("* " + id + " FETCH (FLAGS (" + meta.getFlagsAsString()
                        + ") BODY[HEADER] {" + headers.length() + "}");
                for (String hl : headers.toString().split("\r\n")) {
                    send(hl);
                }
                send(")\r\n" + tag + " OK FETCH HEADER completed");

            } else {
                // ---- Scenario 1 : corps complet ----
                send("* " + id + " FETCH (FLAGS (" + meta.getFlagsAsString()
                        + ") BODY[] {" + emailFile.length() + "}");
                try (BufferedReader reader = new BufferedReader(new FileReader(emailFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) send(line);
                }
                send(")");
                send(tag + " OK FETCH completed");
            }

        } catch (NumberFormatException e) {
            send(tag + " BAD Invalid message number");
        } catch (IOException e) {
            send(tag + " NO Error reading message: " + e.getMessage());
        }
    }

    // ================================================================
    //  Scenario 4 — STORE (gestion des flags, persistance)
    // ================================================================
    private void handleStore(String tag, String args) {
        if (state != ImapState.SELECTED) {
            send(tag + " NO Not allowed without SELECT [state: " + state.name() + "]");
            return;
        }

        try {
            // Format attendu : STORE <id> +FLAGS (\Seen)
            String[] parts = args.split(" ", 3);
            int id = Integer.parseInt(parts[0]);

            if (id < 1 || id > messages.size()) {
                send(tag + " NO Message introuvable");
                return;
            }

            MessageMetadata meta = messages.get(id - 1);

            if (parts.length >= 3) {
                String flagSpec = parts[1].toUpperCase();
                String flagStr  = parts[2].replaceAll("[()\\[\\]]", "").trim();

                if (flagSpec.equals("+FLAGS") || flagSpec.equals("+FLAGS.SILENT")) {
                    meta.addFlag(flagStr);
                } else if (flagSpec.equals("-FLAGS") || flagSpec.equals("-FLAGS.SILENT")) {
                    meta.removeFlag(flagStr);
                }
                // Persistance : sauvegarde les flags dans un fichier .flags
                meta.saveFlags();
            }

            send("* " + id + " FETCH (FLAGS (" + meta.getFlagsAsString() + "))");
            send(tag + " OK STORE completed");

        } catch (NumberFormatException e) {
            send(tag + " BAD Missing credentials (usage: LOGIN user password)");
        }
    }

    // ================================================================
    //  Scenario 5 — SEARCH (ALL, FROM:<val>, SUBJECT:<val>)
    // ================================================================
    private void handleSearch(String tag, String args) {
        if (state != ImapState.SELECTED) {
            send(tag + " NO Non autorise sans SELECT [" + state.name() + "]");
            return;
        }

        String criterion = args.trim().toUpperCase();
        StringBuilder result = new StringBuilder("* SEARCH");

        for (MessageMetadata meta : messages) {
            boolean match = false;

            if (criterion.equals("ALL") || criterion.equals("UNSEEN")) {
                match = true;
            } else if (criterion.startsWith("FROM:")) {
                String val = args.substring(5).trim().toLowerCase();
                match = searchInHeader(meta.getFile(), "From:", val);
            } else if (criterion.startsWith("SUBJECT:")) {
                String val = args.substring(8).trim().toLowerCase();
                match = searchInHeader(meta.getFile(), "Subject:", val);
            } else {
                // Critère non supporte → on cherche dans tout le fichier
                match = searchInBody(meta.getFile(), args.trim().toLowerCase());
            }

            if (match) result.append(" ").append(meta.getId());
        }

        send(result.toString());
        send(tag + " OK SEARCH completed");
    }

    /** Recherche une valeur dans une en-tête specifique du fichier email. */
    private boolean searchInHeader(File f, String headerName, String value) {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null && !line.isEmpty()) {
                if (line.toLowerCase().startsWith(headerName.toLowerCase())
                        && line.toLowerCase().contains(value)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    /** Recherche dans le corps complet du message. */
    private boolean searchInBody(File f, String value) {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.toLowerCase().contains(value)) return true;
            }
        } catch (IOException ignored) {}
        return false;
    }

    // ================================================================
    //  LOGOUT — valide dans tous les etats
    // ================================================================
    private void handleLogout(String tag) {
        send("* BYE IMAP Server logging out");
        send(tag + " OK LOGOUT terminated");
        this.state = ImapState.LOGOUT;
    }

    // ================================================================
    //  Classe interne : Metadonnees + Flags d'un message
    // ================================================================
    private static class MessageMetadata {
        private final int      id;
        private final File     file;
        private final Set<String> flags = new LinkedHashSet<>();

        private MessageMetadata(int id, File file) {
            this.id   = id;
            this.file = file;
        }

        /**
         * Charge les flags depuis le fichier .flags associe au message.
         * Si ce fichier n'existe pas, on initialise avec \Recent.
         */
        public static MessageMetadata load(int id, File emailFile) {
            MessageMetadata m = new MessageMetadata(id, emailFile);
            File flagFile = flagFile(emailFile);
            if (flagFile.exists()) {
                try (BufferedReader r = new BufferedReader(new FileReader(flagFile))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        if (!line.trim().isEmpty()) m.flags.add(line.trim());
                    }
                } catch (IOException ignored) {}
            } else {
                m.flags.add("\\Recent");
            }
            return m;
        }

        /** Persiste les flags sur le disque (fichier .flags à côte du .txt). */
        public void saveFlags() {
            try (PrintWriter w = new PrintWriter(new FileWriter(flagFile(file)))) {
                for (String flag : flags) w.println(flag);
            } catch (IOException e) {
                System.err.println("[IMAP] Impossible de sauvegarder les flags : " + e.getMessage());
            }
        }

        private static File flagFile(File emailFile) {
            String name = emailFile.getName().replace(".txt", ".flags");
            return new File(emailFile.getParent(), name);
        }

        public int    getId()           { return id; }
        public File   getFile()         { return file; }
        public void   addFlag(String f) { flags.add(f); }
        public void   removeFlag(String f) { flags.remove(f); }
        public String getFlagsAsString(){ return String.join(" ", flags); }
    }
}
