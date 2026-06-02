# 📁 DataShare - Plateforme de partage de fichiers sécurisés (MVP)

DataShare est une application web permettant aux utilisateurs de téléverser, gérer et partager des fichiers volumineux (jusqu'à 1 Go) de manière sécurisée via des liens temporaires.

## 🛠️ Stack Technique
* **Backend :** Java 17, Spring Boot 3, Spring Security, Hibernate (JPA)
* **Frontend :** Angular, TypeScript, HTML/CSS
* **Base de données :** PostgreSQL
* **Outils d'analyse & tests :** Grafana K6, SonarCloud, Dependabot

---

## ⚙️ 1. Prérequis Système (Environnement)

Pour installer et exécuter ce projet en local, les outils suivants doivent être installés sur votre machine :

* **Java JDK 17+** (Pour la compilation et l'exécution du Backend Spring Boot)
* **Node.js v18+ et npm** (Pour l'exécution du Frontend Angular)
* **PostgreSQL** (Installé en local ou exécuté via un conteneur **Docker**)
* **Maven** (Inclus dans la plupart des IDE, pour la gestion des dépendances Java)
* *(Optionnel)* **K6** (Pour lancer les tests de charge locaux)

---

## 🔐 2. Variables d'environnement

La sécurité de l'application repose sur l'injection de variables d'environnement. **Aucun mot de passe n'est versionné sur Git.** Avant de lancer l'application, configurez les variables suivantes dans votre environnement local, ou éditez le fichier `backend/src/main/resources/application.yml` pour vos tests locaux :

| Variable | Description | Exemple de valeur locale |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | URL de la base PostgreSQL | `jdbc:postgresql://localhost:5432/datashare` |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur de la BDD | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe de la BDD | `votre_mot_de_passe` |
| `JWT_SECRET` | Clé de signature des tokens | `cle_secrete_256_bits_minimum_pour_le_jwt` |

*(Assurez-vous d'avoir créé une base de données nommée `datashare` dans votre instance PostgreSQL avant le lancement).*

---

## 🚀 3. Processus d'installation et d'exécution

Le projet est composé de deux modules distincts nécessitant chacun leur propre terminal.

### Étape A : Lancer le Backend (Spring Boot)
Ouvrez un terminal à la racine du dossier `backend/` :

```bash
# 1. Installer les dépendances et compiler le projet
mvn clean install

# 2. Démarrer le serveur
mvn spring-boot:run
```
L'API Backend sera accessible sur : **`http://localhost:8080/api/v1`**

### Étape B : Lancer le Frontend (Angular)
Ouvrez un second terminal à la racine du dossier `frontend/` :

```bash
# 1. Installer les dépendances Node modules
npm install

# 2. Démarrer le serveur de développement Angular
ng serve
```
L'interface utilisateur sera accessible sur : **`http://localhost:4200`**

---

## 📚 4. Documentation Annexe et Qualité

Ce dépôt contient l'ensemble de la documentation technique justifiant les choix d'architecture et les audits réalisés sur le MVP :

* **Documentation API :** Les routes exposées sont documentées au format OpenAPI.
* **`PERF.md` :** Rapport complet des tests de performance et de charge (Grafana K6).
    * *Commande pour rejouer les tests :* `k6 run performance-test.js`
* **`SECURITY.md` :** Audit DevSecOps, analyse SonarCloud et gestion des vulnérabilités OWASP.
* **`MAINTENANCE.md` :** Stratégie de mise à jour des dépendances, figeage des versions et prévention de la dette technique.