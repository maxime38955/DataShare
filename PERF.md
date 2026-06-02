# 🚀 Rapport de Performance (PERF.md) - DataShare MVP

Ce document synthétise les résultats des campagnes de tests de performance menées sur l'API backend (Spring Boot) du prototype DataShare. L'objectif est de valider la robustesse de l'architecture logicielle avant son déploiement et de s'assurer du respect de nos contrats de service (SLAs).

---

## 1. Méthodologie et Outillage (DevPerf)

Pour s'inscrire dans une démarche moderne et reproductible, nous avons opté pour le framework **Grafana K6** (tests de performance "as code" en JavaScript). 

**Type de test réalisé : Test de Charge (Load Testing)**
L'objectif n'est pas de faire crasher le serveur (Stress Test), mais de simuler un comportement utilisateur normal et continu avec une charge définie.
Le test a été configuré avec des paliers progressifs :
* **Ramp-up :** 10 secondes (Montée progressive jusqu'à 20 utilisateurs simultanés - VUs).
* **Plateau :** 30 secondes (Maintien de la charge cible).
* **Ramp-down :** 10 secondes (Déconnexion progressive).

---

## 2. Scénario Critiques Testés

Le scénario K6 a été conçu pour tester les fonctionnalités les plus consommatrices en ressources processeur (CPU) du cycle d'authentification et de sécurité :
1. **Étape A : Authentification (`POST /api/v1/user/login`).** Vérification de l'empreinte cryptographique Bcrypt en base de données (PostgreSQL) et génération d'un jeton JWT signé.
2. **Étape B : Accès protégé (`GET /api/v1/files/user/files`).** Interception de la requête par les filtres `Spring Security`, validation de la signature du JWT, extraction de l'utilisateur, et requête transactionnelle en base de données pour récupérer l'historique.

---

## 3. Résultats et KPIs (Indicateurs Clés)

Avant l'exécution, deux seuils d'exigence (Thresholds) stricts ont été fixés dans le script K6 pour valider ou invalider le test de manière automatisée :
* Un temps de réponse au 95ème centile (p95) inférieur à 500 ms.
* Un taux d'erreur HTTP inférieur à 1 %.

**📊 Synthèse des résultats (Test en environnement local) :**

| Métrique | Seuil d'exigence (Contrat) | Résultat obtenu | Statut |
| :--- | :--- | :--- | :--- |
| **Taux d'erreur HTTP** | `< 1 %` | **0.00 %** (0 échec sur 1468 requêtes) | ✅ Succès |
| **Temps de réponse p(95)**| `< 500 ms` | **119.93 ms** (Moyenne : 54 ms) | ✅ Succès |
| **Volume de requêtes** | N/A | **~ 30 requêtes / seconde** | ℹ️ Info |

---

## 4. Analyse Architecturale (Diagnostic Senior)

Les résultats obtenus sont excellents pour cette phase de MVP. 
Avec 100% de réussite sous une charge constante de 20 utilisateurs virtuels tournant en boucle, **l'API démontre une parfaite stabilité.** Le temps de réponse p(95) de 120 ms prouve que la logique métier centrale (la configuration Spring Security, les filtres JWT personnalisés, et l'ORM Hibernate) est hautement optimisée. La base de données PostgreSQL gère parfaitement les connexions concurrentes sans créer de goulot d'étranglement sur ces routes de lecture.

*(Note : Ces tests ayant été exécutés en local (localhost), ils excluent la latence du réseau internet. Ils valident purement la puissance de traitement du code Java).*

---

## 5. Limites et Roadmap d'Amélioration (Phase 2)

En tant que Référent Technique, il convient de souligner que ce test de charge initial n'est pas exhaustif. Pour garantir la pérennité du produit DataShare en production, la roadmap technique de la Phase 2 devra inclure les campagnes suivantes :

1. **Test sur les flux binaires (Heavy Upload/Download) :**
   * Le test actuel simule des échanges JSON légers. Le véritable défi de DataShare réside dans l'upload de fichiers volumineux (jusqu'à 1 Go). Il faudra tester le comportement des *Streams* et de la mémoire vive (RAM) du serveur (Heap Space) lorsque plusieurs utilisateurs envoient des fichiers simultanément.
2. **Stress Testing :**
   * Pousser le système au-delà de 20 utilisateurs (ex: pics à 500 VUs) pour identifier le point de rupture (Breakpoint) de notre serveur Tomcat embarqué ou du pool de connexions HikariCP (Postgres).
3. **Soak Testing (Test d'endurance) :**
   * Laisser tourner un scénario K6 à charge moyenne pendant 12 à 24 heures pour s'assurer de l'absence de fuites de mémoire (Memory Leaks) dans l'application Java.