package org.example.auth;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AuthServerApp {
    public static void main(String[] args) {
        try {
            int port = 1099;
            
            // Création du registre RMI sur le port 1099
            Registry registry = LocateRegistry.createRegistry(port);
            
            // Instanciation de l'implémentation du service
            IAuthService authService = new AuthServiceImpl();
            
            // Enregistrement du service sous le nom "AuthService"
            registry.rebind("AuthService", authService);
            
            System.out.println(" Serveur d'Authentification RMI démarré sur le port " + port);
            System.out.println("En attente de connexions RMI...");
            
        } catch (Exception e) {
            System.err.println(" Erreur au démarrage du serveur d'authentification : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
