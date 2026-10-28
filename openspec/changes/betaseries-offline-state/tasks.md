# Tasks

## 1. Contrat de données avant implémentation

- [x] 1.1 Sur la TV de référence, vérifier en lecture seule la réponse authentifiée multi-épisodes avec courant + cinq suivants diffusés, la pagination et le franchissement de saison ; si nécessaire vérifier le repli par saisons explicites décrit en D3. Consigner uniquement structure, types, compteurs et protocole retenu dans un rapport de vérification du change, sans titres, historique ni identifiants personnels ; cette preuve précède tout code de réserve.
- [x] 1.2 Réconcilier la base d'implémentation avec l'état alors livré de `betaseries-session-recovery` et `tv-resource-budget`, selon l'ordre de `proposal.md` ; vérifier par comparaison des exigences que seuls vu/non vu obtiennent la reprise automatique et qu'aucune récupération de session temporairement indisponible n'est perdue.

## 2. Stockage durable et restauration

- [x] 2.1 Créer le dépôt et le schéma Room dédiés au compte, aux contenus, réserves, intentions révisionnées et références de nettoyage, avec exclusion de sauvegarde et migrations non destructives ; vérifier sur une base de test persistée que recréation et évolution du schéma conservent intentions, statuts et dates.
- [x] 2.2 Lier durablement la session protégée au compte sans dupliquer le jeton dans Room ; restaurer le cache avant le réseau et contrôler les remplacements incomplets. Vérifier les cas sans cache, cache vide, compte différent, panne de stockage et redémarrage hors ligne avec tests de dépôt/session.
- [x] 2.3 Remplacer la liste mutable de suivi comme source de vérité par la projection transactionnelle du dépôt ; vérifier en JUnit qu'un rafraîchissement plus ancien ne masque ni intention ni confirmation récente et que les mêmes données alimentent héro/À suivre sans nouvelle lecture TV Provider.

## 3. Vu/non vu local pour épisodes et films

- [x] 3.1 Enregistrer vu/non vu avant publication et planification, avec une dernière intention par identité et révision ; tester sauvegarde impossible sans envoi ni faux succès, persistance de plusieurs épisodes isolés et vu/non vu successifs sur un même contenu pendant un envoi.
- [x] 3.2 Adapter Annuler aux demandes avant envoi, déjà parties et confirmées, en conservant fenêtre et navigation existantes ; tester l'inverse exact d'un film, l'absence de retrait/revisionnage implicite et la protection des données pendant Annuler. Le retrait différé non envoyé doit toujours être abandonné à la mort du processus.
- [x] 3.3 Appliquer les statuts effectifs à l'enrichissement et aux projections communes, y compris aux reprises locales identifiées ; vérifier avec `TrackingProjectionTest`, les tests héro et les tests de fusion adaptés que l'ancien épisode/film ne réapparaît pas et que les épisodes précédents non vus restent inchangés.

## 4. Réserve glissante et nettoyage

- [x] 4.1 Implémenter le protocole de réserve vérifié en 1.1 avec dates de diffusion persistées, pagination et concurrence bornées ; remplacer les assertions `limit=1` devenues obsolètes par fixtures de six épisodes, inter-saisons, fin avec moins de six, date absente/future et liste partielle.
- [x] 4.2 Faire avancer la projection hors ligne et compléter uniquement la série concernée en ligne, en appliquant les intentions avant le compte des candidats ; vérifier six marquages après recréation sans réseau, réserve épuisée distincte d'une série à jour et absence de chargement de suivant pour les films.
- [x] 4.3 Séparer la confirmation du statut cible du complément de réserve dans le fournisseur ; un test doit montrer qu'une confirmation acquitte la demande même si le complément échoue, puis que le complément seul reprend sans renvoi de mutation ni relecture complète du compte.
- [x] 4.4 Mettre en place le nettoyage des demandes acquittées, épisodes hors fenêtre et films devenus inutiles ; vérifier les lignes réellement supprimées de la base et les protections par nouvelle révision, Annuler, reprise locale connue, images partagées et résultat incomplet. Refaire le nettoyage à la libération des références sans constituer d'historique illimité.

## 5. Synchronisation persistante et isolation de session

- [x] 5.1 Ajouter WorkManager au catalogue Gradle et un worker unique par incarnation, contraint au réseau avec backoff et dépendances minimales ; vérifier par tests de worker sans Activity qu'il lit la file durable sans initialiser lecteur, images ou TV Provider, et qu'il n'entretient aucun polling sans travail restant.
- [x] 5.2 Implémenter réconciliation avant renvoi incertain, état potentiellement envoyé durable et acquittement conditionnel par compte/incarnation/révision ; tester réponse perdue, crash avant acquittement, statut déjà conforme sans renvoi, statut différent avec priorité locale, mutation en erreur et lecture de vérification en panne.
- [x] 5.3 Mutualiser déclenchement local, réessai manuel, premier plan et worker ; vérifier absence d'envoi concurrent, respect des budgets et Retry-After, pause sur accès refusé, réparation d'une planification manquée et intention arrivée pendant la fin du dernier lot.
- [x] 5.4 Invalider la session puis arrêter et purger ses travaux/données à la déconnexion ou au changement d'identité ; tester A vers B avec réponse tardive, purge interrompue puis redémarrage, reconnexion de A sans anciennes demandes et conservation des préférences indépendantes du compte.

## 6. Présentation et documentation du comportement

- [x] 6.1 Adapter notice, indicateur, détail et contrôle Synchroniser existants aux états enregistré sur la TV, en attente, incomplet, synchronisé et stockage en erreur ; conserver les testTags existants et fournir les ressources anglais, français, allemand, espagnol, italien, vérifiées par le test de complétude des traductions.
- [x] 6.2 Ajouter les tests Compose D-pad avec `@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")` pour épisode suivant, film disparu, Annuler, file restante sans carte, réglages quittés/rouverts et refus d'accès ; vérifier absence de vol de focus, bords, Retour, restauration de l'origine et repli d'un élément disparu.
- [x] 6.3 Mettre à jour le README : cache hors ligne pour films et séries, courant + cinq suivants diffusés, limites de réserve et d'images, arrière-plan Android différable, nettoyage et perte des demandes à la déconnexion ; vérifier la cohérence des textes avec les specs et les états UI livrés.

## 7. Vérification intégrée

- [x] 7.1 Exécuter `openspec-verify-change`, `openspec validate --all --strict` et `./gradlew test` ; conserver les résultats et vérifier qu'aucune tâche fonctionnelle n'est cochée sans preuve de test correspondante.
- [ ] 7.2 Construire avec `./gradlew assembleRelease` et valider sur TV Google TV sous Android 14 : premier remplissage, coupure réseau, plusieurs épisodes et un film marqués, redémarrage hors ligne, restauration du réseau pendant une autre app, confirmation et nettoyage effectifs de la base, puis déconnexion ; consigner des preuves anonymisées sans confondre demande planifiée et exécution réellement observée.
- [ ] 7.3 Vérifier sur TV la sortie de veille, la reprise après redémarrage et le cas d'arrêt forcé Android, l'absence de double envoi et le repos sans travail restant ; consigner les délais réellement observés et les restrictions Android sans promettre d'instantanéité, puis contrôler le rendu/focus en retour sur l'accueil.

## Workflow follow-up

- Après revue, merge explicitement autorisé et validation sur TV réelle, archiver avec `openspec archive betaseries-offline-state` dans l'ordre indiqué dans `proposal.md`.
- Vérifier ensuite `openspec validate --all --strict` sur les specs consolidées, notamment l'exception de reprise des seuls vu/non vu et la conservation des règles de récupération de session éventuellement livrées entre-temps.
