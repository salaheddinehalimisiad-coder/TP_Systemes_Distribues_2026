package org.example.auth;

import java.io.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    // Fichier stockant les utilisateurs "username:password"
    private static final String USERS_FILE = "users.txt";

    // Stockage des tokens en mémoire : Token -> Username
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();

    public AuthServiceImpl() throws RemoteException {
        super();
        initFile();
    }

    private synchronized void initFile() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            try {
                file.createNewFile();
                // Utilisateur par défaut pour les tests
                registerUser("admin", "admin123");
            } catch (IOException e) {
                System.err.println("Erreur de création du fichier: " + e.getMessage());
            }
        }
    }

    @Override
    public String authenticate(String username, String password) throws RemoteException {
        if (checkUserCredentials(username, password)) {
            String token = UUID.randomUUID().toString();
            activeTokens.put(token, username);
            System.out.println("Utlisateur " + username + " authentifié. Token généré : " + token);
            // On retourne la réponse format JSON
            return "{\"token\": \"" + token + "\"}";
        }
        System.out.println("Échec d'authentification pour l'utilisateur " + username);
        return "{\"error\": \"Identifiants invalides\"}";
    }

    @Override
    public boolean verifyToken(String token) throws RemoteException {
        return activeTokens.containsKey(token);
    }

    @Override
    public String getUsernameFromToken(String token) throws RemoteException {
        return activeTokens.get(token);
    }

    @Override
    public boolean registerUser(String username, String password) throws RemoteException {
        if (userExists(username)) {
            System.out.println("Tentative d'enregistrement échouée : l'utilisateur " + username + " existe déjà.");
            return false;
        }
        try (FileWriter fw = new FileWriter(USERS_FILE, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(username + ":" + password);
            System.out.println("Utilisateur " + username + " créé avec succès.");
            return true;
        } catch (IOException e) {
            System.err.println("Erreur lors de l'enregistrement de l'utilisateur: " + e.getMessage());
            return false;
        }
    }

    private boolean userExists(String username) {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].equals(username)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur de lecture: " + e.getMessage());
        }
        return false;
    }

    private boolean checkUserCredentials(String username, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException e) {
            System.err.println("Erreur de lecture: " + e.getMessage());
        }
        return false;
    }
}
