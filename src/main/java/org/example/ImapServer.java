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
    private List<Map<String, Object>> emails = new ArrayList<>();

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
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "US-ASCII")), true);

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
            } catch (IOException e) {}
        }
    }

    private void send(String response) {
        logger.log("SERVER -> " + response);
        out.print(response + "\r\n");
        out.flush();
    }

    private void handleCommand(String line) {
        if (line.isEmpty()) return;
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
                send(tag + " BAD Commande inconnue ou non autorisee dans l'etat actuel [" + state.name() + "]");
                break;
        }
    }

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
        String pass = creds[1].replace("\"", "");
        
        try {
            java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry("127.0.0.1", 1099);
            org.example.auth.IAuthService authService = (org.example.auth.IAuthService) registry.lookup("AuthService");
            String result = authService.authenticate(user, pass);
            if (result.contains("error")) {
                 send(tag + " NO [AUTHENTICATIONFAILED] Authentication failed");
                 return;
            }
        } catch (Exception e) {
            logger.log("Erreur RMI (IMAP auth): " + e.getMessage());
            send(tag + " NO [SERVERBUG] Authorization server unavailable");
            return;
        }

        this.username = user;
        this.state    = ImapState.AUTHENTICATED;
        send(tag + " OK LOGIN successful - welcome " + user);
    }

    private void handleSelect(String tag, String folderName) {
        if (state == ImapState.NOT_AUTHENTICATED) {
            send(tag + " NO Authentication required before SELECT");
            return;
        }

        folderName = folderName.replace("\"", "").trim();
        if (!folderName.equalsIgnoreCase("INBOX")) {
            send(tag + " NO [NONEXISTENT] Mailbox not found: " + folderName);
            return;
        }

        refreshMessageList();
        state = ImapState.SELECTED;

        send("* " + emails.size() + " EXISTS");
        send("* 0 RECENT");
        send("* OK [UIDVALIDITY 1] UIDs valid");
        send("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft)");
        send(tag + " OK [READ-WRITE] SELECT completed");
    }

    private void refreshMessageList() {
        emails = DatabaseManager.fetchEmails(username);
    }

    private void handleFetch(String tag, String args) {
        if (state != ImapState.SELECTED) {
            send(tag + " NO Select a mailbox first");
            return;
        }

        try {
            String[] parts = args.split(" ", 2);
            int id = Integer.parseInt(parts[0]);
            String dataItem = (parts.length > 1) ? parts[1].toUpperCase() : "BODY[]";

            if (id < 1 || id > emails.size()) {
                send(tag + " NO Message " + id + " not found");
                return;
            }

            Map<String, Object> email = emails.get(id - 1);
            String content = (String) email.get("content");
            boolean isRead = (boolean) email.get("is_read");
            String flags = isRead ? "\\Seen" : "";

            if (dataItem.contains("BODY[HEADER]") || dataItem.contains("RFC822.HEADER")) {
                String headers = extractHeaders(content);
                send("* " + id + " FETCH (FLAGS (" + flags + ") BODY[HEADER] {" + headers.length() + "}");
                send(headers);
                send(")");
            } else {
                send("* " + id + " FETCH (FLAGS (" + flags + ") BODY[] {" + content.length() + "}");
                send(content);
                send(")");
            }
            send(tag + " OK FETCH completed");

        } catch (Exception e) {
            send(tag + " BAD Invalid fetch arguments or server error");
        }
    }

    private String extractHeaders(String content) {
        int index = content.indexOf("\r\n\r\n");
        if (index == -1) index = content.indexOf("\n\n");
        if (index == -1) return content;
        return content.substring(0, index + 2);
    }

    private void handleStore(String tag, String args) {
        if (state != ImapState.SELECTED) {
            send(tag + " NO Not allowed without SELECT");
            return;
        }

        try {
            String[] parts = args.split(" ", 3);
            int id = Integer.parseInt(parts[0]);
            if (id < 1 || id > emails.size()) {
                send(tag + " NO Message introuvable");
                return;
            }

            if (parts.length >= 3) {
                String flagSpec = parts[1].toUpperCase();
                String flagStr  = parts[2].toUpperCase();

                if (flagStr.contains("\\SEEN")) {
                    int emailId = (int) emails.get(id - 1).get("id");
                    if (flagSpec.contains("+FLAGS")) {
                        // Mark as read in DB
                        markAsRead(emailId, true);
                        emails.get(id - 1).put("is_read", true);
                    } else if (flagSpec.contains("-FLAGS")) {
                        // Mark as unread in DB
                        markAsRead(emailId, false);
                        emails.get(id - 1).put("is_read", false);
                    }
                }
            }
            boolean isRead = (boolean) emails.get(id - 1).get("is_read");
            send("* " + id + " FETCH (FLAGS (" + (isRead ? "\\Seen" : "") + "))");
            send(tag + " OK STORE completed");
        } catch (Exception e) {
            send(tag + " BAD Invalid STORE arguments");
        }
    }

    private void markAsRead(int emailId, boolean isRead) {
        String sql = "UPDATE emails SET is_read = ? WHERE id = ?";
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, isRead);
            stmt.setInt(2, emailId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleSearch(String tag, String args) {
        if (state != ImapState.SELECTED) {
            send(tag + " NO Non autorise sans SELECT");
            return;
        }

        String criterion = args.trim().toUpperCase();
        StringBuilder result = new StringBuilder("* SEARCH");

        for (int i = 0; i < emails.size(); i++) {
            Map<String, Object> email = emails.get(i);
            boolean match = false;

            if (criterion.equals("ALL")) {
                match = true;
            } else if (criterion.equals("UNSEEN")) {
                match = !(boolean) email.get("is_read");
            } else if (criterion.startsWith("FROM")) {
                String val = args.substring(args.indexOf(" ") + 1).replace("\"", "").toLowerCase();
                match = ((String)email.get("sender")).toLowerCase().contains(val);
            } else if (criterion.startsWith("SUBJECT")) {
                String val = args.substring(args.indexOf(" ") + 1).replace("\"", "").toLowerCase();
                match = ((String)email.get("subject")).toLowerCase().contains(val);
            }

            if (match) result.append(" ").append(i + 1);
        }

        send(result.toString());
        send(tag + " OK SEARCH completed");
    }

    private void handleLogout(String tag) {
        send("* BYE IMAP Server logging out");
        send(tag + " OK LOGOUT terminated");
        this.state = ImapState.LOGOUT;
    }
}

