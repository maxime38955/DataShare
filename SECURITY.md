# 🛡️ Politique et Analyse de Sécurité - DataShare (MVP)

Ce document répertorie l'état actuel de la sécurité du projet DataShare (MVP), les outils d'analyse DevSecOps mis en place, ainsi que les vulnérabilités identifiées et leur plan de remédiation.

## 1. Approche et Outils DevSecOps
Pour garantir la qualité et la sécurité du code tout au long du cycle de vie du développement, nous avons intégré les outils d'analyse automatisés suivants :
* **SonarCloud (SAST) :** Analyse statique du code source pour identifier les failles, les bugs et les "code smells".
* **GitHub Dependabot & npm audit (SCA) :** Analyse continue des dépendances tierces (Frontend et Backend) pour détecter les vulnérabilités connues (CVE).

---

## 2. Analyse Statique du Code (Rapport SonarCloud)

L'analyse SonarCloud a identifié 4 alertes de sécurité mineures/moyennes. Aucune faille critique ou bloquante n'est présente dans le code métier.

### 2.1. Backend (Spring Boot) - 2 Alertes "Medium"
* **Fichier concerné :** `backend/src/main/resources/application.yml`
* **Description :** SonarCloud lève une alerte concernant la limite de taille des requêtes HTTP (définie à 1 Go / `1073741824 bytes` et 2 Go / `2147483648 bytes`), qui est supérieure à la limite de sécurité standard de 8 Mo.
* **Analyse du risque :** Une limite très haute expose théoriquement le serveur à des attaques par déni de service (DoS) par épuisement de la mémoire.
* **Justification / Remédiation :** Ce comportement est **intentionnel** et correspond à la spécification métier (US01) qui exige de pouvoir uploader des fichiers volumineux (jusqu'à 1 Go). Ce risque est accepté pour le MVP. Pour la version de production future, le traitement des fichiers se fera par flux (streaming) directement vers un espace Cloud (ex: AWS S3) pour ne pas surcharger la mémoire RAM du serveur.

### 2.2. Frontend (Angular) - 2 Alertes "Low"
* **Fichier concerné :** `frontend/src/index.html`
* **Description :** L'attribut d'intégrité des ressources (Subresource Integrity - SRI) est manquant sur certaines balises `<script>` ou `<link>`.
* **Analyse du risque :** Faible. Si un script externe (CDN) est compromis, le navigateur l'exécutera.
* **Justification / Remédiation :** Une tâche technique sera créée pour ajouter les attributs `integrity` contenant les hashs cryptographiques des ressources externes chargées dans le fichier `index.html`.

---

## 3. Analyse des Dépendances (Rapport Dependabot & npm audit)

L'audit des dépendances Frontend a relevé **5 vulnérabilités de sévérité modérée**.

### 3.1. Détail des vulnérabilités
* **`uuid` (< 11.1.1) :** Absence de vérification des limites de buffer (Missing buffer bounds check).
* **`webpack-dev-server` :** Vulnérabilité à l'exposition du code source cross-origin.

### 3.2. Analyse Architecturale (Faux Positifs en Production)
L'arbre des dépendances (`npm audit`) démontre que le paquet vulnérable `uuid` est requis en cascade par `sockjs` -> `webpack-dev-server` -> `@angular-devkit`. 

**Diagnostic Senior :** Ces dépendances sont **strictement liées à l'environnement de développement** (devDependencies) et au processus de compilation d'Angular. Elles ne sont **jamais incluses dans le bundle final de production** (dossier `/dist`). 
Par conséquent, ces failles ne représentent **aucune menace de sécurité pour les utilisateurs finaux** ou pour le serveur de production.

**Plan d'action :** Aucune action critique immédiate requise. Les paquets seront mis à jour automatiquement lors de la prochaine montée de version mineure du framework Angular (via `@angular/cli`).

---

## 4. Mesures de Sécurité Globales Implémentées

Afin de répondre aux standards de l'OWASP Top 10, l'architecture globale inclut nativement les protections suivantes :

1.  **Protection des accès (Stateless) :** Utilisation de JSON Web Tokens (JWT) signés cryptographiquement, éliminant le besoin de sessions côté serveur (et justifiant la désactivation de la protection CSRF de Spring).
2.  **Confidentialité des mots de passe :** Hachage systématique des mots de passe en base de données à l'aide d'algorithmes robustes.
3.  **Prévention des Injections SQL :** Utilisation exclusive de l'ORM Spring Data JPA (Hibernate) paramétrant nativement toutes les requêtes SQL.
4.  **Sécurité des fichiers partagés :** Génération de tokens d'accès uniques (UUID non prédictibles) pour chaque fichier uploadé, complétée par une purge automatique à expiration pour garantir le droit à l'oubli.