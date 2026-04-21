# 📧 EMP Mail - Projet Systèmes Distribués (TP 2026)

Ce projet implémente un système de messagerie distribué complet basé sur les protocoles **SMTP** et **POP3**, avec une architecture robuste utilisant **Java RMI** pour l'authentification et **Docker Cluster** pour le déploiement.

---

## 🏛️ Architecture du Système
L'application est composée de plusieurs services conteneurisés :
- **Mail Server (Node Cluster)** : Noeuds gérant les protocoles SMTP/POP3.
- **Auth Service (RMI)** : Service centralisé de gestion des utilisateurs et sessions.
- **Database (PostgreSQL)** : Persistance des messages et des comptes.
- **Web Frontend** : Interface client moderne servie via Nginx/Javalin.

### Protocoles Supportés :
- **SMTP (Port 2525)** : Envoi de messages avec conformité RFC.
- **POP3 (Port 110)** : Récupération et gestion de la boîte de réception.
- **RMI** : Couche d'authentification distribuée.

---

## 🚀 Installation Rapide (Docker)
Assurez-vous d'avoir Docker et Docker Compose installés :
```bash
docker-compose up -d --build
```
*L'interface est alors accessible sur [http://localhost](http://localhost).*

---

## 🌟 Travail Supplémentaire (Modernisation & Extensions)
> **Note :** Cette section contient les fonctionnalités avancées ajoutées en bonus au-delà de l'énoncé de base pour démontrer une maîtrise complète des systèmes distribués modernes.

### 🎨 Modernisation de l'Interface (UX/UI)
- **Design Glassmorphism** : Interface "Sleek & Professional" avec cartes blanches premium et animations fluides.
- **Support Multi-langue Dynamique** : Support complet du **Français**, **Anglais** et **Arabe (avec gestion RTL)**.
- **Diaporama Immersif** : Page de garde interactive présentant les photos de l'Ecole Militaire Polytechnique.

### ⚡ Fonctionnalités Avancées (Extra)
- **Notifications Temps Réel** : Utilisation de **WebSockets** pour les alertes d'arrivée de messages et sons de notification.
- **Gestionnaire de Tâches Intégré** : Panel de productivité permettant de transformer un email en tâche actionnable.
- **Dossier Envoyés & Recherche** : Implémentation du backend/frontend pour le suivi des envois et la recherche globale par mot-clé.
- **Panneau Admin Broadcast** : Interface administrative permettant d'envoyer des messages système à l'ensemble des utilisateurs instantanément.
- **Sécurité UX** : Bascule de visibilité des mots de passe et animations de chargement "shimmer".

---

## 👨‍💻 Crédits
**Réalisé par :** Halimi Mohamed Salah Eddine - SIAD (2025/2026)  
**Accompagnement :** Antigravity AI (Modernisation distribuée)

---
© 2026 Ecole Militaire Polytechnique.
