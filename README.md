# 📧 Système de Messagerie Distribuée Pro (SMTP / POP3 / RMI / Web)

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containers-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![SystemeDistribues](https://img.shields.io/badge/Systems-Distributed-007396?style=for-the-badge&logo=servers&logoColor=white)

Bienvenue sur le dépôt du projet de **Systèmes Distribués (2025/2026)**. Ce projet est un système de messagerie complet, hautement disponible et distribué, doté d'une interface SaaS moderne et de fonctionnalités d'entreprise.

---

## 💎 EMP Mail : SaaS "Production-Ready"

Nous avons transformé ce TP technique en une application SaaS robuste avec des fonctionnalités professionnelles :

- **⚡ Notifications Temps Réel (WebSockets)** : Réception instantanée d'emails sans rafraîchissement. Alerte sonore et visuelle dès l'arrivée d'un message.
- **📁 Boîte de réception Intelligente** : Tri automatique des emails en catégories (**Principale**, **Réseaux sociaux**, **Promotions**) pour une meilleure organisation.
- **🛡️ Panneau d'Administration Avancé** :
    - **Gestion des Utilisateurs** : Statistiques globales de messages et utilisation du stockage par compte.
    - **Cluster Monitor** : Surveillance en temps réel de l'état de santé (Health Check) et de la latence de chaque nœud du cluster (`mail-node-1`, `2`, `3`).
- **🛡️ Sécurité & Filtrage** :
    - **Anti-Spam Intelligent** : Moteur de filtrage par mots-clés au niveau du serveur SMTP.
    - **Authentification RMI** : Service centralisé de validation de tokens pour une sécurité renforcée.
- **✨ UX Exceptionnelle** :
    - **"Undo Send" (Annuler l'envoi)** : Délai de grâce de 5 secondes pour annuler une erreur d'envoi.
    - **Résumé par IA** : Synthèse automatique du contenu des longs emails via un moteur d'analyse sémantique.
    - **Contacts Pro** : Carnet d'adresses synchronisé avec initiales dynamiques.
- **📱 PWA (Progressive Web App)** : Installable sur mobile et bureau pour un accès hors-ligne et natif.

---

## 🏗️ Architecture Distribuée (Cluster & Docker)

Le système est conçu pour la haute disponibilité via une architecture en micro-services conteneurisée :

1.  **Load Balancer (NGINX)** : Répartition de charge intelligente entre les multiples nœuds de l'application.
2.  **App Nodes (3 Nœuds)** : Trois instances répliquées du serveur Web pour garantir la résilience.
3.  **Base de Données (MySQL)** : Persistance centralisée pour les emails et les utilisateurs.
4.  **Messaging Infrastructure** :
    -   📤 **SMTP** (2525) : Serveur d'expédition avec notifications WebSocket transversales.
    -   📥 **POP3** (110) : Serveur de récupération compatible avec les clients standards.
5.  **Service RMI** (1099) : Cluster d'authentification centralisé.

---

## 🚀 Démarrage Ultra-Rapide (Docker)

La méthode la plus simple pour lancer l'infrastructure complète est d'utiliser Docker Compose :

```bash
# Compiler le code Java localement
mvn clean package

# Lancer tout le cluster (DB, Web Nodes, Nginx)
docker-compose up -d --build
```
*L'interface est alors accessible sur [http://localhost](http://localhost).*

---

## 🛠️ Développement Local & Hot-Reload

Pour accélérer le développement, le projet supporte le **Hot-Reloading** des assets Web (HTML/JS/CSS) via un montage de volume Docker automatique. Toute modification dans `src/main/resources/web` est répercutée instantanément.

### Comptes de Test (Pré-configurés) :
- **Utilisateur Standard** : `salah` / `salah123`
- **Administrateur** : `admin` / `admin123` (Donne accès à l'onglet "Administration")

---

## ⚙️ Détails Techniques RFC
- **SMTP** : Implémentation du "Dot-stuffing" et des codes de statut RFC 5321.
- **POP3** : Support des commandes `USER`, `PASS`, `STAT`, `LIST`, `RETR`, `DELE`, `QUIT`.
- **RMI** : Interface `IAuthService` pour le découplage authentification/logique métier.

---
*Projet réalisé par Halimi Mohamed Salah Eddine - SIAD (2025/2026)*  
*Modernisé par Antigravity AI (Expert Distributed Systems)*
