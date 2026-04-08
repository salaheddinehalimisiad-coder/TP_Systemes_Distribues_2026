package org.example.auth;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    // Fichier stockant les utilisateurs en JSON
    private static final String USERS_FILE = "users.json";

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
                JSONObject initialData = new JSONObject();
                initialData.put("admin", "admin123");
                Files.write(Paths.get(USERS_FILE), initialData.toString(4).getBytes());
                System.out.println("Fichier users.json créé avec l'utilisateur par défaut 'admin'.");
            } catch (IOException e) {
                System.err.println("Erreur de création du fichier JSON: " + e.getMessage());
            }
        }
    }

    private synchronized JSONObject loadUsers() {
        try {
            if (!new File(USERS_FILE).exists()) {
                initFile();
            }
            String content = new String(Files.readAllBytes(Paths.get(USERS_FILE)));
            return new JSONObject(content);
        } catch (Exception e) {
            System.err.println("Erreur au chargement des utilisateurs: " + e.getMessage());
            return new JSONObject();
        }
    }

    private synchronized boolean saveUsers(JSONObject users) {
        try {
            Files.write(Paths.get(USERS_FILE), users.toString(4).getBytes());
            return true;
        } catch (IOException e) {
            System.err.println("Erreur à la sauvegarde des utilisateurs: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String authenticate(String username, String password) throws RemoteException {
        if (checkUserCredentials(username, password)) {
            String token = UUID.randomUUID().toString();
            activeTokens.put(token, username);
            System.out.println("Utilisateur " + username + " authentifié. Token généré : " + token);
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
        JSONObject users = loadUsers();
        if (users.has(username)) {
            System.out.println("Tentative d'enregistrement échouée : l'utilisateur " + username + " existe déjà.");
            return false;
        }
        users.put(username, password);
        boolean saved = saveUsers(users);
        if (saved) {
            System.out.println("Utilisateur " + username + " créé avec succès.");
        }
        return saved;
    }

    @Override
    public boolean updateUser(String username, String newPassword) throws RemoteException {
        JSONObject users = loadUsers();
        if (!users.has(username)) {
            System.out.println("Mise à jour échouée : l'utilisateur " + username + " n'existe pas.");
            return false;
        }
        users.put(username, newPassword);
        boolean saved = saveUsers(users);
        if (saved) {
            System.out.println("Mot de passe mis à jour pour " + username);
        }
        return saved;
    }

    @Override
    public boolean deleteUser(String username) throws RemoteException {
        JSONObject users = loadUsers();
        if (!users.has(username)) {
            return false;
        }
        users.remove(username);
        boolean saved = saveUsers(users);
        if (saved) {
            System.out.println("Utilisateur " + username + " supprimé avec succès.");
            // Déconnecter s'il avait un token actif
            activeTokens.values().removeIf(val -> val.equals(username));
        }
        return saved;
    }

    @Override
    public boolean userExists(String username) throws RemoteException {
        JSONObject users = loadUsers();
        return users.has(username);
    }

    private boolean checkUserCredentials(String username, String password) {
        JSONObject users = loadUsers();
        if (users.has(username)) {
            return users.getString(username).equals(password);
        }
        return false;
    }
}
