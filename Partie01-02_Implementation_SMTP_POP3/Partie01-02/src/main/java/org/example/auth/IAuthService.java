package org.example.auth;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAuthService extends Remote {
    /**
     * Authentifie un utilisateur et retourne un token sous format JSON.
     * Exemple de retour : {"token": "1234-abcd-..."}
     */
    String authenticate(String username, String password) throws RemoteException;

    /**
     * Vérifie si un token est valide.
     */
    boolean verifyToken(String token) throws RemoteException;

    /**
     * Retourne le nom d'utilisateur associé à un token.
     */
    String getUsernameFromToken(String token) throws RemoteException;

    /**
     * Enregistre un nouvel utilisateur.
     */
    boolean registerUser(String username, String password) throws RemoteException;
}
