# DataShare
# 🛠️ Documentation de Maintenance - DataShare (MVP)

Ce document définit les procédures de maintenance applicative de DataShare. L'objectif est de prévenir l'accumulation de dette technique, de garantir la compatibilité entre les différentes briques logicielles, et d'assurer une stabilité continue en production.

---

## 1. Procédures de mise à jour des dépendances

Le maintien à jour des frameworks centraux (Spring Boot, Angular) et de leurs bibliothèques associées est réalisé selon une procédure stricte en 3 étapes :

1. **Veille technique et audit :** Vérification régulière des versions obsolètes via les outils natifs :
   * Backend : `mvn versions:display-dependency-updates`
   * Frontend : `npm outdated`
2. **Analyse d'impact :** Lecture systématique des notes de version (*Changelogs* et *Release Notes*) des bibliothèques ciblées avant toute modification pour repérer les changements d'API.
3. **Mise à jour isolée :** Les mises à jour sont effectuées sur une branche Git de maintenance dédiée (ex: `chore/update-dependencies`), suivies d'une exécution complète des suites de tests avant fusion (Merge).

---

## 2. Fréquence des mises à jour

Nous adoptons un rythme basé sur le versionnage sémantique (SemVer) des dépendances :

* **Patchs (Corrections de bugs) : Hebdomadaire ou immédiate**
  * *Objectif :* Intégrer les corrections d'anomalies fonctionnelles et les optimisations de performance fournies par les éditeurs.
* **Versions Mineures (Nouvelles fonctionnalités) : Mensuelle (Fin de sprint)**
  * *Objectif :* Bénéficier des nouvelles API et améliorations non-bloquantes (rétrocompatibles).
* **Versions Majeures (Changement de Framework) : Bi-annuelle**
  * *Objectif :* Montée de version de l'écosystème cœur (ex: passage à une nouvelle version majeure de Java ou d'Angular). Planifiée comme un ticket technique spécifique nécessitant une allocation de temps dédiée.

---

## 3. Risques liés aux mises à jour et Mitigations

La modification de l'arbre des dépendances introduit des risques pour la stabilité de l'application. Voici comment DataShare s'en prémunit :

### 3.1. Risque de Régression et de "Breaking Changes"
* **Le risque :** Une bibliothèque modifie ou supprime une méthode que notre code utilise, provoquant un plantage de l'application lors de la compilation ou, pire, à l'exécution (Runtime).
* **La mitigation :** 
  * Interdiction d'utiliser le symbole `*` ou `^` de manière aveugle dans le `package.json` ou `pom.xml`.
  * La validation d'une mise à jour nécessite obligatoirement que la compilation passe, et que les tests de performance K6 garantissent qu'aucune régression de temps de réponse n'a été introduite.

### 3.2. Risque de conflits inter-dépendances (Dependency Hell)
* **Le risque :** Mettre à jour une dépendance "A" casse la dépendance "B", car la dépendance "B" exigeait l'ancienne version de "A" pour fonctionner.
* **La mitigation :** 
  * Utilisation stricte des fichiers de verrouillage (`package-lock.json` en Front). 
  * Délégation de la compatibilité croisée au parent Spring Boot (`spring-boot-starter-parent` gère lui-même les versions compatibles de ses sous-librairies pour éviter les conflits).

### 3.3. Risque d'obsolescence d'API (Deprecation)
* **Le risque :** Utiliser des méthodes marquées comme `@Deprecated` qui seront supprimées dans la version suivante, transformant la future mise à jour en un chantier de refactoring massif.
* **La mitigation :** 
  * Le compilateur Java et les linters Angular (ESLint) sont configurés pour remonter des avertissements (Warnings) sévères si du code déprécié est utilisé. Ces avertissements doivent être traités avant la clôture du ticket.