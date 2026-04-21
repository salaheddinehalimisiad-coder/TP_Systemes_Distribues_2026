# TP_Systemes_Distribues_2026

Ce dépôt contient le code, les développements et la documentation réalisés dans le cadre du projet d'ingénierie des Systèmes Distribués (2025/2026). Le projet s'appuie sur un système de messagerie distribué préexistant développé en Java (intégrant SMTP, POP3, un serveur d'authentification RMI, une base de données MySQL et une API REST).

---

## 🏗️ Architecture du Projet

Le système est structuré autour d'une architecture distribuée classique :
- **Serveur d'Authentification (RMI)** : Centralise la gestion des comptes et la validation des sessions.
- **Serveurs Mails (Nodes)** : Implémentent les protocoles de bas niveau (SMTP/POP3) pour l'échange de messages.
- **Interface Client (Web)** : Un portail ergonomique permettant la consultation et l'envoi de mails via une API REST.

---

## 🚀 Guide de Démarrage

### Prérequis
- Java JDK 17
- Maven
- MySQL Server

### Installation
1. **Cloner le dépôt** :
   ```bash
   git clone https://github.com/votre-repo/TP_Systemes_Distribues_2026.git
   cd TP_Systemes_Distribues_2026
   ```

2. **Lancer les composants (L'Ordre est Strict)** :
   - **Étape 1** : Lancer le Registre RMI (Auth Server).
   - **Étape 2** : Lancer les Serveurs Mails (SMTP/POP3).
   - **Étape 3** : Lancer le portail Web.

---

## 🌟 Travail Supplémentaire (Modernisation & Extensions)
> **Note :** Cette section contient les fonctionnalités avancées ajoutées en bonus au-delà de l'énoncé de base pour démontrer une maîtrise complète des systèmes distribués modernes.

### 🎨 Modernisation de l'Interface (UX/UI)
- **Design Glassmorphism** : Interface "Sleek & Professional" avec cartes blanches premium et animations fluides.
- **Support Multi-langue** : Support complet du **Français**, **Anglais** et **Arabe (RTL)**.
- **Diaporama Immersif** : Page de garde interactive présentant les photos de l'Ecole Militaire Polytechnique.

### ⚡ Fonctionnalités Avancées (Extra)
- **Notifications Temps Réel** : Utilisation de **WebSockets** pour les alertes d'arrivée de messages et sons de notification.
- **Gestionnaire de Tâches** : Transformation d'emails en tâches actionnables.
- **Dossier Envoyés & Recherche** : Suivi des envois et filtrage global par mot-clé.
- **Panneau Admin Broadcast** : Envoi de messages système à l'ensemble des utilisateurs instantanément.

---
*Projet réalisé par Halimi Mohamed Salah Eddine - SIAD (2025/2026)*
