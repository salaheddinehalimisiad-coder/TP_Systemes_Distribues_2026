import re

path = 'src/main/java/org/example/web/MailRestController.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix the messy end of MailRestController.java
# 1. Find the first 'private static void getAdminUsers'
# 2. Re-structure correctly.

new_tail = """    private static void getAdminUsers(Context ctx) throws Exception {
        String token = ctx.header("Authorization");
        if (token == null || !authService.verifyToken(token)) {
            ctx.status(401).result("{\\"error\\": \\"Non autoris\u00e9\\"}");
            return;
        }

        String username = authService.getUsernameFromToken(token);
        if (!"admin".equals(username)) {
            ctx.status(403).result("{\\"error\\": \\"Acc\u00e8s refus\u00e9. N\u00e9cessite des droits d'administrateur.\\"}");
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
"""

# Replace from 'private static void getAdminUsers' or 'private static void getClusterStatus' to end
m = re.search(r'private static void (getAdminUsers|getClusterStatus)', content)
if m:
    content = content[:m.start()] + new_tail
else:
    print("Error: Could not find method to replace")

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Cleaned up MailRestController.java")
