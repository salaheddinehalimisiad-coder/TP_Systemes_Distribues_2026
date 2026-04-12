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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Partie 6 : Intégration d'une Interface Web de Messagerie (Serveur REST)
 * Ce serveur fait office de pont entre l'interface Web et les serveurs SMTP/POP3/RMI.
 */
public class MailRestController {
    private static IAuthService authService;
    private static final String POP3_HOST = "localhost";
    private static final int POP3_PORT = 110;
    private static final String SMTP_HOST = "localhost";
    private static final int SMTP_PORT = 2525;

    // Stockage temporaire des mots de passe pour l'authentification socket (POP3)
    private static final Map<String, String> sessionPasswords = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        initRmiConnection();

        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/web", Location.CLASSPATH); 
            config.enableCorsForAllOrigins();
        }).start(8080);

        // Routes API
        app.post("/api/login", MailRestController::login);
        app.get("/api/inbox", MailRestController::getInbox);
        app.get("/api/messages/{id}", ctx -> getMessage(ctx));
        app.post("/api/messages/{id}/delete", ctx -> deleteMessage(ctx));
        app.post("/api/send", ctx -> sendMessage(ctx));
        app.get("/api/stats", ctx -> getStats(ctx));

        System.out.println("====================================================");
        System.out.println("🚀 SERVEUR WEB DE MESSAGERIE DÉMARRÉ");
        System.out.println("🔗 URL : http://localhost:8080");
        System.out.println("====================================================");
    }

    private static void initRmiConnection() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            authService = (IAuthService) registry.lookup("AuthService");
            System.out.println("✅ Connecté au service d'authentification RMI.");
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
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine(); 
            out.println("USER " + username); in.readLine();
            out.println("PASS " + password);
            String passRes = in.readLine();
            
            if (!passRes.startsWith("+OK")) {
                ctx.status(500).result("{\"error\": \"Échec login POP3\"}");
                return;
            }

            out.println("LIST");
            String line = in.readLine();
            JSONArray messages = new JSONArray();
            if (line.startsWith("+OK")) {
                while (!(line = in.readLine()).equals(".")) {
                    String[] parts = line.split(" ");
                    JSONObject msg = new JSONObject();
                    msg.put("id", parts[0]);
                    msg.put("size", parts[1]);
                    messages.put(msg);
                }
            }
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

            StringBuilder content = new StringBuilder();
            while (!(line = in.readLine()).equals(".")) {
                content.append(line).append("\n");
            }

            JSONObject res = new JSONObject();
            res.put("id", msgId);
            res.put("body", content.toString());
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
            
            // On ajoute aussi des données fictives pour les graphiques temporels
            // Pour simuler une "belle" interface de données
            stats.put("weeklyActivity", new int[]{12, 19, 3, 5, 2, 3, 10});
            stats.put("storageLimit", 1024 * 1024 * 50); // 50MB limit fictive
            
            ctx.json(stats);
        } catch (Exception e) {
            ctx.status(500).result("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
