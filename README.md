# 📧 Système de Messagerie Distribuée (SMTP / POP3 / IMAP / RMI)

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![SystemeDistribues](https://img.shields.io/badge/Systems-Distributed-007396?style=for-the-badge&logo=servers&logoColor=white)
![RMI](https://img.shields.io/badge/Architecture-RMI-4CAF50?style=for-the-badge)

Bienvenue sur le dépôt du projet de **Systèmes Distribués (2025/2026)** de l'École Militaire Polytechnique (EMP). Ce projet consiste en la reconception, l'extension et le déploiement d'un système de messagerie complet respectant les RFCs standards (SMTP, POP3, IMAP) et doté d'une architecture distribuée d'authentification (Java RMI).

---

## 💎 EMP Mail : Interface Web de Nouvelle Génération (SaaS)

Nous avons modernisé l'expérience utilisateur en intégrant une interface Web de classe mondiale, transformant ce projet technique en un produit orienté SaaS :

- **Design Premium "Marvelous"** : Interface basée sur le **Glassmorphism**, avec un **Mode Sombre** intelligent et des animations fluides (Animate.css).
- **Dashboard Analytique** : Visualisation en temps réel de l'activité du compte via des graphiques dynamiques (**Chart.js**).
- **Multilingue (i18n)** : Support complet et dynamique du **Français**, **Anglais** et **Arabe** (avec gestion automatique du mode **RTL**).
- **Expérience Utilisateur (UX)** : Skeleton screens pour le chargement, notifications toasts, et actions rapides au survol des emails.
- **Architecture REST** : Backend propulsé par **Javalin (Port 8080)** servant de pont entre le Web et le réseau distribué.

---

## 🎯 Architecture du Projet

Le projet a évolué d'une version monolithique locale vers un véritable système orienté Services (SOA) :

1. **Serveurs de Messagerie** (TCP Sockets) :
   - 📤 **SMTP** (Port `2525`) : Traitement, routage et "dot-stuffing" (RFC 5321).
   - 📥 **POP3** (Port `110`) : Récupération des emails et flags de suppression (RFC 1939).
   - 🔄 **IMAP** (Port `143`) : Mode connecté, gestion des flags persistants, `FETCH` partiel des headers (RFC 9051).
2. **Service Central d'Authentification** (Java RMI) :
   - Moteur décentralisé (Port `1099`) générant des Tokens (`UUID`) sous format `JSON`.
   - Garantit une validation de l'état (Thread-Safe) lors de l'accès simultané de plusieurs serveurs.
3. **Interfaces de Supervision (GUI) & Web** :
   - Interfaces Swing pour gérer et superviser asynchroniquement les serveurs.
   - **Portail Web Moderne (EMP Mail)** accessible via navigateur.

---

## 🚀 Démarrage Rapide

### Prérequis
- Java JDK 17 (ou supérieur).
- Apache Maven.

### Installation et Déploiement

1. **Cloner le repository** :
   ```bash
   git clone https://github.com/salaheddinehalimisiad-coder/TP_Systemes_Distribues_2026.git
   cd TP_Systemes_Distribues_2026/Partie01-02_Implementation_SMTP_POP3/Partie01-02
   ```

2. **Compiler le projet** :
   ```bash
   mvn clean package
   ```

3. **Lancer les composants (L'Ordre est Strict)** :
   - **Étape 1** : Lancer le Registre RMI : Exécuter `org.example.auth.AuthServerApp`.
   - **Étape 2** : Lancer les Serveurs Mails : `org.example.Pop3Server` et `org.example.SmtpServer`.
   - **Étape 3** : Lancer le portail Web : `org.example.web.MailRestController`.
   
4. **Accéder à l'interface** :
   Ouvrez votre navigateur sur [http://localhost:8080](http://localhost:8080).

---

## 🛠️ Partie 7 : Client Standard Jakarta Mail (Interoperabilité)

Pour valider l'interopérabilité avec les bibliothèques standards :
1. **Lancer le client graphique** : Exécuter `org.example.StandardMailClientGui`.
2. **Fonctionnalités incluses** :
   - 📤 **SMTP** : Envoi de mails formatés.
   - 📥 **POP3** : Récupération et suppression sécurisée.
   - 🔍 **IMAP** : Consultation avancée et recherche par sujet.

---

## ⚖️ Partie 8 : Scalabilité et Load Balancing

Le système supporte désormais une architecture distribuée avec répartition de charge (NGINX) :

### Lancement des nœuds backend
Vous pouvez lancer plusieurs instances du serveur Web sur des ports différents :
```bash
mvn exec:java -Dexec.mainClass="org.example.web.MailRestController" -Dexec.args="8080"
mvn exec:java -Dexec.mainClass="org.example.web.MailRestController" -Dexec.args="8081"
mvn exec:java -Dexec.mainClass="org.example.web.MailRestController" -Dexec.args="8082"
```

### Configuration NGINX
Un fichier de configuration prêt à l'emploi est disponible dans `src/main/resources/nginx.conf`. Il redirige le trafic vers les trois instances ci-dessus en mode **Round Robin**.

---

## ⚙️ Intégration Continue (CI/CD)

Ce projet est scanné et compilé automatiquement via **GitHub Actions**.  
À chaque `Push` sur la banche `main`, un agent Ubuntu valide la consistance du code et génère l'artefact de production.

---
*Projet réalisé par Halimi Mohamed Salah Eddine - SIAD (2025/2026)*
*Modernisé par Antigravity AI*
