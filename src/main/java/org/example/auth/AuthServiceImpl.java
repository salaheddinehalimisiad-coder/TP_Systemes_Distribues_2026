package org.example.auth;

import org.example.DatabaseManager;
import org.json.JSONObject;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthServiceImpl extends UnicastRemoteObject implements IAuthService {

    // Stockage des tokens en mémoire : Token -> Username
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>();
    // Stockage des mots de passe en mémoire : Token -> Password (pour le pontage POP3/SMTP)
    private final Map<String, String> activePasswords = new ConcurrentHashMap<>();

    public AuthServiceImpl() throws RemoteException {
        super();
        // Plus besoin d'initFile() car on utilise MySQL
    }

    @Override
    public String authenticate(String username, String password) throws RemoteException {
        if (DatabaseManager.authenticateUser(username, password)) {
            String token = UUID.randomUUID().toString();
            activeTokens.put(token, username);
            activePasswords.put(token, password);
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
        if (DatabaseManager.userExists(username)) {
            System.out.println("Tentative d'enregistrement échouée : l'utilisateur " + username + " existe déjà.");
            return false;
        }
        boolean saved = DatabaseManager.registerUser(username, password);
        if (saved) {
            System.out.println("Utilisateur " + username + " créé avec succès.");
        }
        return saved;
    }

    @Override
    public boolean updateUser(String username, String newPassword) throws RemoteException {
        if (!DatabaseManager.userExists(username)) {
            System.out.println("Mise à jour échouée : l'utilisateur " + username + " n'existe pas.");
            return false;
        }
        boolean saved = DatabaseManager.updatePassword(username, newPassword);
        if (saved) {
            System.out.println("Mot de passe mis à jour pour " + username);
        }
        return saved;
    }

    @Override
    public boolean deleteUser(String username) throws RemoteException {
        if (!DatabaseManager.userExists(username)) {
            return false;
        }
        boolean saved = DatabaseManager.deleteUser(username);
        if (saved) {
            System.out.println("Utilisateur " + username + " supprimé avec succès.");
            // Déconnecter s'il avait un token actif
            activeTokens.values().removeIf(val -> val.equals(username));
        }
        return saved;
    }

    @Override
    public boolean userExists(String username) throws RemoteException {
        return DatabaseManager.userExists(username);
    }

    @Override
    public String getPassword(String token) throws RemoteException {
        return activePasswords.get(token);
    }
}

