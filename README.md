# 📧 Système de Messagerie Distribuée (SMTP / POP3 / IMAP / RMI)

![Java](https://img.shields.io/badge/Java-17+-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![SystemeDistribues](https://img.shields.io/badge/Systems-Distributed-007396?style=for-the-badge&logo=servers&logoColor=white)
![RMI](https://img.shields.io/badge/Architecture-RMI-4CAF50?style=for-the-badge)

Bienvenue sur le dépôt du projet de **Systèmes Distribués (2025/2026)** de l'École Militaire Polytechnique (EMP). Ce projet consiste en la reconception, l'extension et le déploiement d'un système de messagerie complet respectant les RFCs standards (SMTP, POP3, IMAP) et doté d'une architecture distribuée d'authentification (Java RMI).

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
3. **Interfaces de Supervision (GUI)** :
   - Interfaces Swing pour gérer et superviser asynchroniquement les serveurs (Logs, Compteurs, Start/Stop).
   - Client RMI d'administration des utilisateurs.

---

## 🚀 Démarrage Rapide

### Prérequis
- Java JDK 17 (ou supérieur).
- Apache Maven (pour la résolution du package `org.json`).

### Installation et Déploiement

1. **Cloner le repository** :
   ```bash
   git clone https://github.com/salaheddinehalimisiad-coder/TP_Systemes_Distribues_2026.git
   cd TP_Systemes_Distribues_2026/Partie01-02_Implementation_SMTP_POP3/Partie01-02
   ```

2. **Compiler les dépendances (CI prête)** :
   ```bash
   mvn clean package
   ```

3. **Lancer les composants (L'Ordre est Strict)** :
   - **Étape 1** : Lancer le Registre RMI : Exécuter `org.example.auth.AuthServerApp`.
   - **Étape 2** : (Optionnel) Créer des comptes avec `org.example.auth.AdminClientGui`.
   - **Étape 3** : Lancer les Serveurs Mails avec `org.example.MailServerLauncher` et cliquer sur "Start Server" sur chaque GUI.

---

## 🧪 Scénarios de tests (via Telnet)

Une fois tous les serveurs démarrés, vous pouvez communiquer via TCP brut :

**Test SMTP :**
```bash
telnet localhost 2525
HELO test
MAIL FROM:<admin@mon-domaine.com>
RCPT TO:<salah@mon-domaine.com>
DATA
Sujet: Bonjour
Ceci est le corps du message.
.
QUIT
```

**Test POP3 :**
```bash
telnet localhost 110
USER salah
PASS admin123
LIST
RETR 1
QUIT
```

---

## ⚙️ Intégration Continue (CI/CD)

Ce projet est scanné et compilé automatiquement via **GitHub Actions**.  
À chaque `Push` ou `Pull Request` sur la banche `main`, un agent Ubuntu :
- Monte l'environnement JDK 17.
- Injecte les dépendances Maven.
- Valide la consistance du code et génère l'artefact.

---
*Projet réalisé par Halimi Mohamed Salah Eddine - SIAD (2025/2026)*
