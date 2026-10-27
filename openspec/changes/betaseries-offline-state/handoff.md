# Relais d'implémentation

## Point d'entrée

- Branche de spécification : `docs/spec-betaseries-offline-state`.
- Change : `betaseries-offline-state`.
- Lire `proposal.md`, `design.md`, `specs/` puis `tasks.md` dans ce dossier.
- Démarrer l'implémentation par `openspec-apply-change` après demande explicite ; les documents de cette session ne constituent pas une implémentation.

## Décisions validées

- État local durable par fournisseur et compte ; restauration de l'accueil même après redémarrage sans réseau.
- Vu/non vu optimiste pour épisodes **et films**, avec sauvegarde durable avant publication. Ajout/retrait/archivage ne deviennent pas des actions hors ligne.
- Séries : prochain diffusé non vu + au moins cinq suivants diffusés, saisons traversées si nécessaire ; tous ceux disponibles s'il en reste moins. Réserve épuisée hors ligne : conserver les demandes et attendre le réseau pour la suite.
- Synchronisation automatique **même en arrière-plan**, selon les possibilités d'Android ; intention locale en attente prioritaire, vérification avant renvoi après incertitude.
- Nettoyage explicite : supprimer demandes acquittées et épisodes consommés hors réserve devenus inutiles ; même principe pour films sans notion de suivant. Protéger demandes en attente, Annuler et preuves utiles à une reprise locale connue, puis purger à leur libération.
- Déconnexion/changement de compte : effacer les données et intentions de l'ancien compte. Les préférences indépendantes du compte restent conservées.

## Points techniques à ne pas manquer

- Première tâche : preuve en lecture seule sur appareil du contrat réel de réserve multi-épisodes/inter-saisons, sans publier de données du compte. Ce contrat n'a pas été vérifié dans la session de spécification.
- Base Room dédiée, non destructivement migrée ; ne pas mettre les intentions dans la base évictable des images.
- Confirmation de mutation séparée du complément de réserve : une panne du suivant ne justifie pas un second marquage déjà confirmé.
- Révision locale et incarnation de session : un ancien résultat ne supprime ni nouvelle intention ni données du nouveau compte.
- Worker autonome, sans Activity ni parcours d'initialisation complet de l'accueil ; pas de service permanent ni polling sans travail.
- Vérifier les changes livrés depuis la base avant d'implémenter, particulièrement `betaseries-session-recovery` sur une branche locale distincte. S'il est livré, l'archiver et réconcilier ses exigences avant ce change ; ne pas remplacer ses corrections de récupération de session par les anciennes formulations copiées depuis la base actuelle.
- `tv-resource-budget` modifie le rafraîchissement du TV Provider : conserver sa séparation des sources. Ce change ne touche pas son exigence de rafraîchissement.

## Vérifications restantes

Les quatre artefacts de planification sont complets ; `openspec validate --all --strict` réussit avec 11 éléments valides et aucun échec. Toutes les tâches d'implémentation restent à faire ; tests Gradle, API sur appareil et exécution WorkManager réelle réservés à l'implémentation. Avant PR de feature : `openspec-verify-change` et `./gradlew test`. Validation finale en `assembleRelease` sur TV réelle puis archivage après merge autorisé.
