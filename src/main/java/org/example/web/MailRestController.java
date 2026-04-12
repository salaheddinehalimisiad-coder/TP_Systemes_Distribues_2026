package org.example.web;

import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.auth.IAuthService;
import org.example.DatabaseManager;
import org.json.JSONArray;
import org.json.JSONObject;

import io.javalin.http.staticfiles.Location;
import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import io.javalin.websocket.WsContext;

/**
 * Partie 6 : Intégration d'une Interface Web de Messagerie (Serveur REST)
 * Ce serveur fait office de pont entre l'interface Web et les serveurs SMTP/POP3/RMI.
 */
public class MailRestController {
    private static IAuthService authService;
    private static final String POP3_HOST = System.getenv("POP3_HOST") != null ? System.getenv("POP3_HOST") : "localhost";
    private static final int POP3_PORT = 110;
    private static final String SMTP_HOST = System.getenv("SMTP_HOST") != null ? System.getenv("SMTP_HOST") : "localhost";
    private static final int SMTP_PORT = 2525;

    // Stockage temporaire des mots de passe pour l'authentification socket (POP3)
    private static final Map<String, String> sessionPasswords = new ConcurrentHashMap<>();
    
    // WebSocket Sessions: User -> Set of contexts
    private static final Map<String, Set<WsContext>> userWsSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        initRmiConnection();

        int port = (args.length > 0) ? Integer.parseInt(args[0]) : 8080;
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/web", Location.CLASSPATH); 
            config.enableCorsForAllOrigins();
        }).start(port);

        // Routes API
        app.post("/api/login", MailRestController::login);
        app.get("/api/inbox", MailRestController::getInbox);
        app.get("/api/messages/{id}", ctx -> getMessage(ctx));
        app.post("/api/messages/{id}/delete", ctx -> deleteMessage(ctx));
        app.post("/api/send", ctx -> sendMessage(ctx));
        app.get("/api/stats", ctx -> getStats(ctx));
        app.post("/api/ai/summarize", ctx -> summarizeEmail(ctx));
        app.get("/api/contacts", ctx -> getContacts(ctx));
        app.get("/api/inbox/count", ctx -> getInboxCount(ctx));
        app.get("/api/admin/users", MailRestController::getAdminUsers);
        app.get("/api/admin/cluster", MailRestController::getClusterStatus);
        
        // Internal route for SMTP notifications
        app.post("/api/internal/notify", MailRestController::handleInternalNotification);

        // WebSockets
        app.ws("/ws", ws -> {
            ws.onConnect(ctx -> {
                System.out.println("WS: Nouveau client connecté");
            });
            ws.onMessage(ctx -> {
                JSONObject msg = new JSONObject(ctx.message());
                if ("auth".equals(msg.optString("type"))) {
                    String token = msg.optString("token");
                    if (authService.verifyToken(token)) {
                        String user = authService.getUsernameFromToken(token);
                        userWsSessions.computeIfAbsent(user, k -> new CopyOnWriteArraySet<>()).add(ctx);
                        ctx.send(new JSONObject().put("type", "auth_ok").toString());
                        System.out.println("WS: Client authentifié pour: " + user);
                    }
                }
            });
            ws.onClose(ctx -> {
                userWsSessions.values().forEach(set -> set.remove(ctx));
                System.out.println("WS: Client déconnecté");
            });
        });

        System.out.println("====================================================");
        System.out.println("🚀 SERVEUR WEB DE MESSAGERIE DÉMARRÉ");
        System.out.println("🔗 URL : http://localhost:8080");
        System.out.println("====================================================");
    }

    private static void initRmiConnection() {
        try {
            String rmiHost = System.getenv("RMI_HOST") != null ? System.getenv("RMI_HOST") : "localhost";
            Registry registry = LocateRegistry.getRegistry(rmiHost, 1099);
            authService = (IAuthService) registry.lookup("AuthService");
            System.out.println("✅ Connecté au service d'authentification RMI (" + rmiHost + ").");
        } catch (Exception e) {
            System.err.println("❌ Erreur : Impossible de se connecter au service RMI Auth.");
        }
    }

    private static void login(Context ctx) throws Exception {
        JSONObject body = new JSONObject(ctx.body());
        String user = body.getString("username");
        String pass = body.getString("password");

        if (authService == null) {
            ctx.status(503).result("{\"error\": \"Service RMI non disponible\"}");
            return;
        }

        String res = authService.authenticate(user, pass);
        JSONObject jsonRes = new JSONObject(res);
        if (jsonRes.has("token")) {
            sessionPasswords.put(jsonRes.getString("token"), pass);
        }
        ctx.result(res).contentType("application/json");
    }

    private static void summarizeEmail(Context ctx) throws Exception {
        String token = ctx.header("Authorization");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\"error\": \"Non autorisé\"}");
            return;
        }
        JSONObject body = new JSONObject(ctx.body());
        String text = body.optString("text", "");
        
        // Mock AI logic
        String summary;
        if (text.length() < 50) {
            summary = "Cet email est très court, il s'agit probablement d'une notification ou d'une réponse rapide.";
        } else if (text.toLowerCase().contains("urgent") || text.toLowerCase().contains("important")) {
            summary = "⚠️ Attention, ce message semble prioritaire. L'expéditeur signale une urgence ou une information importante.";
        } else if (text.toLowerCase().contains("réunion") || text.toLowerCase().contains("rendez-vous")) {
            summary = "📅 L'email parle d'une planification (réunion ou rendez-vous). Pensez à vérifier votre agenda.";
        } else {
            summary = "ℹ️ Ce message donne des détails standard. Aucune urgence détectée, lecture recommandée à votre convenance.";
        }

        // Add a slight delay to simulate AI processing
        Thread.sleep(800);

        JSONObject res = new JSONObject();
        res.put("summary", "✨ Résumé IA : " + summary);
        ctx.result(res.toString()).contentType("application/json");
    }

    private static void getInbox(Context ctx) throws Exception {
        String token = ctx.header("Authorization");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\"error\": \"Non autorisé\"}");
            return;
        }

        String username = authService.getUsernameFromToken(token);
        String password = sessionPasswords.get(token);
        if (password == null) {
            ctx.status(401).result("{\"error\": \"Session expiree, veuillez vous reconnecter.\"}");
            return;
        }

        try (Socket socket = new Socket(POP3_HOST, POP3_PORT);
             BufferedReader pop3In = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter pop3Out = new PrintWriter(socket.getOutputStream(), true)) {

            pop3In.readLine(); // greeting
            pop3Out.println("USER " + username); pop3In.readLine();
            pop3Out.println("PASS " + password);
            String passRes = pop3In.readLine();

            if (!passRes.startsWith("+OK")) {
                ctx.status(500).result("{\"error\": \"Échec login POP3\"}");
                return;
            }

            // Get message list
            pop3Out.println("LIST");
            String line = pop3In.readLine();
            java.util.List<String[]> msgList = new java.util.ArrayList<>();
            if (line.startsWith("+OK")) {
                while (!(line = pop3In.readLine()).equals(".")) {
                    msgList.add(line.split(" "));
                }
            }

            JSONArray messages = new JSONArray();
            for (String[] parts : msgList) {
                String msgId = parts[0];
                String size  = parts.length > 1 ? parts[1] : "0";

                // Use TOP to get headers only (0 body lines)
                pop3Out.println("TOP " + msgId + " 0");
                String topRes = pop3In.readLine();
                String from    = "Expéditeur inconnu";
                String subject = "(sans objet)";
                String date    = "";

                if (topRes.startsWith("+OK")) {
                    String hLine;
                    while (!(hLine = pop3In.readLine()).equals(".")) {
                        String lower = hLine.toLowerCase();
                        if (lower.startsWith("from:"))
                            from = hLine.substring(5).trim();
                        else if (lower.startsWith("subject:"))
                            subject = hLine.substring(8).trim();
                        else if (lower.startsWith("date:"))
                            date = hLine.substring(5).trim();
                    }
                }

                JSONObject msg = new JSONObject();
                msg.put("id", msgId);
                msg.put("size", size);
                msg.put("from", from);
                msg.put("subject", subject.isEmpty() ? "(sans objet)" : subject);
                msg.put("date", date);
                messages.put(msg);
            }

            pop3Out.println("QUIT");
            ctx.result(messages.toString()).contentType("application/json");

        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void getMessage(Context ctx) throws Exception {

        String token = ctx.header("Authorization");
        String msgId = ctx.pathParam("id");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\"error\": \"Non autorisé\"}");
            return;
        }

        String username = authService.getUsernameFromToken(token);
        String password = sessionPasswords.get(token);
        if (password == null) {
            ctx.status(401).result("{\"error\": \"Session expirée\"}");
            return;
        }

        try (Socket socket = new Socket(POP3_HOST, POP3_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine();
            out.println("USER " + username); in.readLine();
            out.println("PASS " + password); in.readLine();

            out.println("RETR " + msgId);
            String line = in.readLine();
            if (!line.startsWith("+OK")) {
                ctx.status(404).result("{\"error\": \"Message non trouvé\"}");
                return;
            }

            // Parse headers + body
            String fromH = "";
            String subjectH = "";
            String dateH = "";
            StringBuilder bodyContent = new StringBuilder();
            boolean inHeaders = true;

            while (!(line = in.readLine()).equals(".")) {
                if (inHeaders) {
                    if (line.isEmpty()) {
                        inHeaders = false; // blank line = end of headers
                        continue;
                    }
                    String lower = line.toLowerCase();
                    if (lower.startsWith("from:"))    fromH    = line.substring(5).trim();
                    else if (lower.startsWith("subject:")) subjectH = line.substring(8).trim();
                    else if (lower.startsWith("date:"))    dateH    = line.substring(5).trim();
                } else {
                    bodyContent.append(line).append("\n");
                }
            }

            JSONObject res = new JSONObject();
            res.put("id", msgId);
            res.put("from", fromH.isEmpty() ? "Expéditeur inconnu" : fromH);
            res.put("subject", subjectH.isEmpty() ? "(sans objet)" : subjectH);
            res.put("date", dateH);
            res.put("body", bodyContent.toString());
            ctx.result(res.toString()).contentType("application/json");

        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void deleteMessage(Context ctx) throws Exception {
        String token = ctx.header("Authorization");
        String msgId = ctx.pathParam("id");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\"error\": \"Non autorisé\"}");
            return;
        }

        String username = authService.getUsernameFromToken(token);
        String password = sessionPasswords.get(token);
        if (password == null) {
            ctx.status(401).result("{\"error\": \"Session expirée\"}");
            return;
        }

        try (Socket socket = new Socket(POP3_HOST, POP3_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine();
            out.println("USER " + username); in.readLine();
            out.println("PASS " + password); in.readLine();

            out.println("DELE " + msgId);
            String res = in.readLine();
            out.println("QUIT");

            if (res.startsWith("+OK")) {
                ctx.result("{\"status\": \"success\"}");
            } else {
                ctx.status(400).result("{\"error\": \"Suppression échouée\"}");
            }

        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void sendMessage(Context ctx) throws Exception {
        String token = ctx.header("Authorization");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\"error\": \"Non autorisé\"}");
            return;
        }

        JSONObject body = new JSONObject(ctx.body());
        String from = authService.getUsernameFromToken(token);
        String to = body.getString("to");
        String subject = body.getString("subject");
        String content = body.getString("content");

        try (Socket socket = new Socket(SMTP_HOST, SMTP_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine();
            out.println("HELO localhost"); in.readLine();
            out.println("MAIL FROM:<" + from + "@mon-domaine.com>"); in.readLine();
            out.println("RCPT TO:<" + to + ">"); in.readLine();
            out.println("DATA"); in.readLine();
            out.println("Subject: " + subject);
            out.println();
            out.println(content);
            out.println(".");
            String res = in.readLine();
            out.println("QUIT");

            if (res.startsWith("250")) {
                ctx.result("{\"status\": \"sent\"}");
            } else {
                ctx.status(500).result("{\"error\": \"Échec envoi SMTP: " + res + "\"}");
            }

        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void getStats(Context ctx) {
        String token = ctx.header("Authorization");
        try {
            if (token == null || !authService.verifyToken(token)) {
                ctx.status(401).result("{\"error\": \"Non autorisé\"}");
                return;
            }
            String username = authService.getUsernameFromToken(token);
            Map<String, Object> stats = DatabaseManager.getUserStats(username);
            stats.put("weeklyActivity", new int[]{12, 19, 3, 5, 2, 3, 10});
            stats.put("storageLimit", 1024 * 1024 * 50);
            ctx.json(stats);
        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void getContacts(Context ctx) {
        String token = ctx.header("Authorization");
        try {
            if (token == null || !authService.verifyToken(token)) {
                ctx.status(401).result("{\"error\": \"Non autorisé\"}");
                return;
            }
            java.util.List<String> users = DatabaseManager.getAllUsernames();
            JSONArray arr = new JSONArray();
            for (String u : users) {
                JSONObject obj = new JSONObject();
                obj.put("username", u);
                obj.put("email", u + "@emp-mail.local");
                obj.put("initials", String.valueOf(u.charAt(0)).toUpperCase());
                arr.put(obj);
            }
            ctx.result(arr.toString()).contentType("application/json");
        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private static void getInboxCount(Context ctx) {
        String token = ctx.header("Authorization");
        if (token == null) { ctx.status(401).result("{}"); return; }
        String username;
        try {
            if (!authService.verifyToken(token)) { ctx.status(401).result("{}"); return; }
            username = authService.getUsernameFromToken(token);
        } catch (Exception e) { ctx.status(500).result("{}"); return; }

        String password = sessionPasswords.get(token);
        if (password == null) { ctx.status(401).result("{}"); return; }

        try (Socket socket = new Socket(POP3_HOST, POP3_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine();
            out.println("USER " + username); in.readLine();
            out.println("PASS " + password);
            String passRes = in.readLine();
            if (!passRes.startsWith("+OK")) { ctx.result("{\"count\": 0}"); return; }

            out.println("STAT");
            String stat = in.readLine(); // +OK <count> <size>
            int count = 0;
            if (stat.startsWith("+OK")) {
                String[] parts = stat.split(" ");
                if (parts.length >= 2) count = Integer.parseInt(parts[1]);
            }
            out.println("QUIT");
            ctx.result("{\"count\": " + count + "}").contentType("application/json");
        } catch (Exception e) {
            ctx.result("{\"count\": 0}").contentType("application/json");
        }
    }

    private static void getAdminUsers(Context ctx) throws Exception {

        String token = ctx.header("Authorization");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\"error\": \"Non autorisé\"}");
            return;
        }

        String username = authService.getUsernameFromToken(token);
        if (!"admin".equals(username)) {
            ctx.status(403).result("{\"error\": \"Accès refusé. Nécessite des droits d'administrateur.\"}");
            return;
        }

        java.util.List<java.util.Map<String, Object>> users = DatabaseManager.getUsersDetailedStats();
        ctx.json(users);
    }

    private static void getClusterStatus(Context ctx) throws Exception {
        String token = ctx.header("Authorization");
        String username = authService.getUsernameFromToken(token);
        if (!"admin".equals(username)) {
            ctx.status(403); return;
        }

        JSONArray nodes = new JSONArray();
        String[] nodeNames = {"mail-node-1", "mail-node-2", "mail-node-3"};
        
        for (String name : nodeNames) {
            JSONObject node = new JSONObject();
            node.put("name", name);
            
            long start = System.currentTimeMillis();
            try {
                java.net.URL url = new java.net.URL("http://" + name + ":8080/api/stats");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(500);
                conn.setRequestMethod("GET");
                int code = conn.getResponseCode();
                long end = System.currentTimeMillis();
                
                node.put("status", code == 200 ? "Online" : "Partial");
                node.put("latency", (end - start) + "ms");
            } catch (Exception e) {
                node.put("status", "Offline");
                node.put("latency", "---");
            }
            nodes.put(node);
        }
        
        JSONObject res = new JSONObject();
        res.put("nodes", nodes);
        res.put("currentNode", System.getenv("HOSTNAME") != null ? System.getenv("HOSTNAME") : "localhost");
        
        ctx.result(res.toString()).contentType("application/json");
    }

    private static void handleInternalNotification(Context ctx) {
        String recipient = ctx.queryParam("recipient");
        if (recipient == null) { ctx.status(400); return; }

        Set<WsContext> sessions = userWsSessions.get(recipient);
        if (sessions != null) {
            JSONObject msg = new JSONObject();
            msg.put("type", "new_mail");
            msg.put("from", ctx.queryParam("from"));
            msg.put("subject", ctx.queryParam("subject"));
            
            String json = msg.toString();
            sessions.removeIf(s -> !s.session.isOpen());
            sessions.forEach(s -> s.send(json));
        }
        ctx.status(200);
    }
}
