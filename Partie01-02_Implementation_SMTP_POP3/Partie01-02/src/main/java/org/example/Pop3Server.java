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
    private List<Map<String, Object>> emails;
    private boolean authenticated;
    private List<Boolean> deletionFlags;
    private ServerLogger logger;
    private Pop3Server server;
    private String username;

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
            while ((line = in.readLine()) != null) {
                logger.log("POP3 Received: " + line);
                String[] parts = line.split(" ", 2);
                String command = parts[0].toUpperCase();
                String argument = parts.length > 1 ? parts[1] : "";

                switch (command) {
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
                        return;
                    default:
                        send("-ERR Unknown command");
                        break;
                }
            }
        } catch (IOException e) {
            logger.log("POP3 Session error: " + e.getMessage());
        } finally {
            try {
                server.removeSession(this);
                socket.close();
            } catch (IOException e) {}
        }
    }

    private void handleUser(String arg) {
        if (DatabaseManager.userExists(arg)) {
            username = arg;
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

        authenticated = true;
        emails = DatabaseManager.fetchEmails(username);
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
        long size = 0;
        for (Map<String, Object> email : emails) {
            size += ((String)email.get("content")).length();
        }
        out.println("+OK " + emails.size() + " " + size);
    }

    private void handleList() {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        out.println("+OK " + emails.size() + " messages");
        for (int i = 0; i < emails.size(); i++) {
            out.println((i + 1) + " " + ((String)emails.get(i).get("content")).length());
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
            Map<String, Object> email = emails.get(index);
            String content = (String) email.get("content");
            out.println("+OK " + content.length() + " octets");
            out.println(content);
            out.println(".");
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
            int index = Integer.parseInt(arg.trim()) - 1;
            if (index < 0 || index >= emails.size()) {
                out.println("-ERR No such message");
                return;
            }
            if (deletionFlags.get(index)) {
                out.println("-ERR Message already marked for deletion");
                return;
            }
            deletionFlags.set(index, true);
            out.println("+OK Message marked for deletion");
        } catch (Exception e) {
            out.println("-ERR Invalid message number");
        }
    }

    private void handleRset() {
        if (!authenticated) {
            out.println("-ERR Authentication required");
            return;
        }
        for (int i = 0; i < deletionFlags.size(); i++) {
            deletionFlags.set(i, false);
        }
        send("+OK Deletion marks reset");
    }

    private void handleQuit() {
        if (authenticated) {
            for (int i = deletionFlags.size() - 1; i >= 0; i--) {
                if (deletionFlags.get(i)) {
                    int emailId = (int) emails.get(i).get("id");
                    if (DatabaseManager.deleteEmail(emailId)) {
                        logger.log("POP3: Deleted email ID " + emailId + " from DB");
                        emails.remove(i);
                        deletionFlags.remove(i);
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
