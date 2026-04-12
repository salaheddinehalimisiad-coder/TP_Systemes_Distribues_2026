package org.example;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SmtpServer {
    private final int port;
    private ServerSocket serverSocket;
    private boolean running = false;
    private ServerLogger logger;
    private List<SmtpSession> activeSessions = Collections.synchronizedList(new ArrayList<>());

    public SmtpServer(int port, ServerLogger logger) {
        this.port = port;
        this.logger = logger;
    }

    public void start() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                logger.log("SMTP Server started on port " + port);
                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        logger.log("SMTP: New connection from " + clientSocket.getInetAddress());
                        SmtpSession session = new SmtpSession(clientSocket, logger, this);
                        activeSessions.add(session);
                        session.start();
                    } catch (IOException e) {
                        if (running) logger.log("SMTP Accept error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                logger.log("Could not listen on port " + port);
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
            for (SmtpSession session : activeSessions) {
                session.closeSocket();
            }
            activeSessions.clear();
            logger.log("SMTP Server stopped.");
        } catch (IOException e) {
            logger.log("Error stopping server: " + e.getMessage());
        }
    }

    public void removeSession(SmtpSession session) {
        activeSessions.remove(session);
    }

    public int getConnectedCount() {
        return activeSessions.size();
    }

    public static void main(String[] args) {
        // Mode console par défaut
        new SmtpServer(2525, System.out::println).start();
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}



class SmtpSession extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Finite state machine for the SMTP session
    private enum SmtpState {
        CONNECTED, // Connection established; waiting for HELO/EHLO.
        HELO_RECEIVED, // HELO/EHLO received; ready for MAIL FROM.
        MAIL_FROM_SET, // MAIL FROM command processed; ready for RCPT TO.
        RCPT_TO_SET, // At least one RCPT TO received; ready for DATA.
        DATA_RECEIVING // DATA command received; reading email content.
    }

    private SmtpState state;
    private String sender;
    private List<String> recipients;
    private StringBuilder dataBuffer;

    private ServerLogger logger;
    private SmtpServer server;

    public SmtpSession(Socket socket, ServerLogger logger, SmtpServer server) {
        this.socket = socket;
        this.logger = logger;
        this.server = server;
        this.state = SmtpState.CONNECTED;
        this.recipients = new ArrayList<>();
        this.dataBuffer = new StringBuilder();
    }

    public void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException e) {}
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send initial greeting (RFC 5321 specifies a 220 response)
            send("220 smtp.example.com Service Ready");

            String line;
            while ((line = in.readLine()) != null) {
                logger.log("SMTP Received: " + line);
                // If we are in DATA receiving state, accumulate message lines
                if (state == SmtpState.DATA_RECEIVING) {
                    // End of DATA input is signaled by a single dot on a line.
                    if (line.equals(".")) {
                        // Store the email and reset for next message.
                        storeEmail(dataBuffer.toString());
                        dataBuffer.setLength(0);
                        // After DATA, we allow additional RCPT TO commands for new messages,
                        // or can reset to HELO_RECEIVED depending on design.
                        state = SmtpState.HELO_RECEIVED;
                        send("250 OK: Message accepted for delivery");
                    } else {
                        // RFC 5321 : Gestion du Dot-stuffing
                        // Si la ligne commence par deux points, on retire le premier
                        if (line.startsWith("..")) {
                            dataBuffer.append(line.substring(1)).append("\r\n");
                        } else {
                            dataBuffer.append(line).append("\r\n");
                        }
                    }
                    continue;
                }

                // Process commands outside of DATA state.
                String command = extractToken(line).toUpperCase();
                String argument = extractArgument(line);

                switch (command) {
                    case "HELO":
                    case "EHLO":
                        handleHelo(argument);
                        break;
                    case "MAIL":
                        handleMailFrom(argument);
                        break;
                    case "RCPT":
                        handleRcptTo(argument);
                        break;
                    case "DATA":
                        handleData();
                        break;
                    case "RSET":
                        handleRset();
                        break;
                    case "NOOP":
                        send("250 OK");
                        break;
                    case "QUIT":
                        handleQuit();
                        return; // Terminate session after QUIT.
                    default:
                        send("500 Command unrecognized");
                        break;
                }
            }
            // Si la boucle se termine alors que nous étions en train de recevoir les
            // données,
            // cela signifie que la connexion a été interrompue avant la réception du point
            // final.
            if (state == SmtpState.DATA_RECEIVING) {
                System.err.println("Connection interrupted during DATA phase. Email incomplete, not stored.");
            }
        } catch (IOException e) {
            logger.log("SMTP Session error: " + e.getMessage());
        } finally {
            try {
                server.removeSession(this);
                socket.close();
            } catch (IOException e) {
                /* ignore */ }
        }
    }

    private void handleHelo(String arg) {
        // Reset any previous session data
        state = SmtpState.HELO_RECEIVED;
        sender = "";
        recipients.clear();
        send("250 Hello " + arg);
    }

    private void handleMailFrom(String arg) {
        // Vérifier que l'argument correspond exactement au format "FROM:<email>"
        if (!arg.toUpperCase().matches("^FROM:\\s*<[^>]+>$")) {
            send("501 Syntax error in parameters or arguments");
            send(arg.toUpperCase());
            return;
        }
        // Extraire l'adresse email en retirant "FROM:" et les chevrons.
        String potentialEmail = arg.substring(5).trim(); // Extrait ce qui suit "FROM:"
        // Retirer les chevrons (< et >)
        potentialEmail = potentialEmail.substring(1, potentialEmail.length() - 1).trim();

        String email = extractEmail(potentialEmail);
        if (email == null) {
            out.println("501 Syntax error in parameters or arguments");
            return;
        }
        
        // --- DEBUT RMI CHECK ---
        String username = email.split("@")[0];
        try {
            java.rmi.registry.Registry registry = java.rmi.registry.LocateRegistry.getRegistry("127.0.0.1", 1099);
            org.example.auth.IAuthService authService = (org.example.auth.IAuthService) registry.lookup("AuthService");
            if (!authService.userExists(username)) {
                send("550 No such user here");
                return;
            }
        } catch (Exception e) {
            logger.log("Erreur RMI (SMTP vérif): " + e.getMessage());
            send("451 Requested action aborted: RMIServer unavailable");
            return;
        }
        // --- FIN RMI CHECK ---

        sender = email;
        state = SmtpState.MAIL_FROM_SET;
        send("250 OK");
    }

    private void handleRcptTo(String arg) {
        if (state != SmtpState.MAIL_FROM_SET && state != SmtpState.RCPT_TO_SET) {
            send("503 Bad sequence of commands");
            return;
        }
        if (!arg.toUpperCase().startsWith("TO:")) {
            out.println("501 Syntax error in parameters or arguments");
            return;
        }
        String potentialEmail = arg.substring(3).trim();
        String email = extractEmail(potentialEmail);
        if (email == null) {
            out.println("501 Syntax error in parameters or arguments");
            return;
        }

        recipients.add(email);
        state = SmtpState.RCPT_TO_SET;
        send("250 OK");
    }

    private void handleData() {
        if (state != SmtpState.RCPT_TO_SET || recipients.isEmpty()) {
            send("503 Bad sequence of commands");
            return;
        }
        state = SmtpState.DATA_RECEIVING;
        send("354 Start mail input; end with <CRLF>.<CRLF>");
    }

    private void handleQuit() {
        send("221 smtp.example.com Service closing transmission channel");
    }

    private void handleRset() {
        state = SmtpState.HELO_RECEIVED;
        sender = "";
        recipients.clear();
        dataBuffer.setLength(0);
        send("250 OK: Session reset");
    }

    // Helper to extract the first token (command) from the input line.
    private String extractToken(String line) {
        String[] parts = line.split(" ");
        return parts.length > 0 ? parts[0] : "";
    }

    // Helper to extract the argument portion (everything after the command).
    private String extractArgument(String line) {
        int index = line.indexOf(' ');
        return index > 0 ? line.substring(index).trim() : "";
    }

    // Simple email extraction: removes angle brackets and performs a basic
    // validation.
    private String extractEmail(String input) {
        // Remove any surrounding angle brackets.
        input = input.replaceAll("[<>]", "");
        if (input.contains("@") && input.indexOf("@") > 0 && input.indexOf("@") < input.length() - 1) {
            return input;
        }
        return null;
    }

    // Store the email for each recipient in the corresponding user directory.
    // Files are named using the current timestamp.

    private void storeEmail(String data) {
        String subject = "No Subject";
        // Parse subject from data if possible
        if (data.contains("Subject: ")) {
            int start = data.indexOf("Subject: ") + 9;
            int end = data.indexOf("\r\n", start);
            if (end != -1) {
                subject = data.substring(start, end);
            }
        }

        // --- Simple Spam Filter ---
        String lowerData = data.toLowerCase();
        boolean isSpam = lowerData.contains("casino") || lowerData.contains("free money") || 
                         lowerData.contains("win") || lowerData.contains("lottery") ||
                         lowerData.contains("cryptocurrency");
        
        if (isSpam && !subject.startsWith("[SPAM]")) {
            subject = "[SPAM] " + subject;
        }

        for (String recipient : recipients) {
            boolean stored = DatabaseManager.storeEmail(sender, recipient, subject, data);
            if (stored) {
                logger.log("Stored email for " + recipient + " in Database");
                // Notify Web nodes for real-time WebSocket push
                notifyWebNodes(sender, recipient, subject);
            } else {
                logger.log("Failed to store email for " + recipient + " in Database");
            }
        }
    }

    private void notifyWebNodes(String from, String recipient, String subject) {
        String[] nodes = {"mail-node-1", "mail-node-2", "mail-node-3"};
        for (String node : nodes) {
            new Thread(() -> {
                try {
                    String urlStr = "http://" + node + ":8080/api/internal/notify?recipient=" + 
                                     URLEncoder.encode(recipient, "UTF-8") +
                                     "&from=" + URLEncoder.encode(from, "UTF-8") +
                                     "&subject=" + URLEncoder.encode(subject, "UTF-8");
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setConnectTimeout(1000);
                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception e) {
                    // Ignore nodes that might not be up or have different IP
                }
            }).start();
        }
    }

    private void send(String msg) {
        logger.log("SERVER -> " + msg);
        out.println(msg);
    }
}
