package org.example;

/**
 * Interface de rappel pour la journalisation des événements des serveurs.
 * Permet de rediriger les logs vers la console ou une interface graphique.
 */
public interface ServerLogger {
    void log(String message);
}
