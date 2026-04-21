# 📧 EMP Mail - Système de Messagerie Distribué Moderne

![Version](https://img.shields.io/badge/version-2.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)
![Build](https://img.shields.io/badge/build-docker--compose-orange)

**EMP Mail** est une plateforme de communication sécurisée et moderne développée pour l'**Ecole Militaire Polytechnique**. Ce système repose sur une architecture distribuée robuste alliant Java RMI pour la communication inter-noeuds et une interface web ultra-moderne.

---

## ✨ Fonctionnalités Clés

### 👤 Interface Utilisateur (UX/UI)
- **Design Premium** : Look "Sleek & Professional" avec des effets de glassmorphism et des animations fluides (Animate.css).
- **Mode Sombre** : Basculement dynamique entre les thèmes clair et sombre.
- **Diaporama Immersif** : Page d'accueil avec défilement fluide d'images architecturales de l'EMP.
- **Recherche Globale** : Filtrage en temps réel des messages par sujet, expéditeur ou contenu.

### 🌐 Internationalisation (i18n)
- **Support Multi-langue** : Support complet du **Français**, **Anglais** et **Arabe**.
- **Gestion RTL** : Inversion complète de l'interface pour la langue arabe (Right-to-Left).

### 🛠 Fonctions Avancées
- **Dossier Envoyés** : Suivi complet des messages expédiés.
- **Notifications Temps Réel** : Alertes sonores et visuelles instantanées via **WebSockets**.
- **Analyse IA** : Module de résumé automatique des emails par intelligence artificielle.
- **Gestionnaire de Tâches** : Possibilité de transformer un email en tâche à faire directement dans le panneau latéral.

### 🛡 Administration
- **Dashboard Admin** : Vue d'ensemble de l'utilisation du stockage et du nombre de messages par utilisateur.
- **Diffusion Globale (Broadcast)** : Envoi instantané de messages système à tous les utilisateurs de la plateforme.

---

## 🏗 Architecture Technique

Le système est conçu pour être scalable et distribué :
- **Backend** : Java (Javalin Framework) pour les API REST et WebSockets.
- **Communication** : Java RMI pour la synchronisation entre les différents noeuds de messagerie.
- **Base de données** : PostgreSQL / SQLite pour la persistance des données.
- **Frontend** : JavaScript Vanilla (ES6+), HTML5, CSS3, Chart.js pour les graphiques.
- **Conteneurisation** : Docker et Docker-Compose pour un déploiement simplifié en cluster.

---

## 🚀 Installation & Lancement

### Prérequis
- Docker & Docker Compose
- Java 17+ (pour le développement local)

### Déploiement avec Docker
1. Clonez le dépôt :
   ```bash
   git clone https://github.com/salaheddinehalimisiad-coder/TP_Systemes_Distribues_2026.git
   cd TP_Systemes_Distribues_2026
   ```

2. Lancez le cluster :
   ```bash
   docker-compose up -d --build
   ```

3. Accédez à l'application :
   - Interface Utilisateur : `http://localhost`
   - Interface Administration : `http://localhost/admin.html`

---

## 👤 Identifiants de Test
- **Utilisateur Standard** : `salah` / `salah123`
- **Administrateur** : `admin` / `admin123`

---

## 👨‍💻 Développé par
**Halimisiad Salaheddine** - *TP Systèmes Distribués 2026*

---
© 2026 Ecole Militaire Polytechnique. Tous droits réservés.
