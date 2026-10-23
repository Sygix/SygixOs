# Proposal

## Why

Le TV Provider ne représente qu'une partie des séries et films à regarder et ne fournit pas un suivi de visionnage commun aux applications. P9 complète l'accueil avec le compte BetaSeries, sans doublons ni nouvelle rangée, et mutualise le choix du lecteur et les actions de suivi entre le héro et À suivre.

## What Changes

- Connecter un compte BetaSeries par association OAuth sur téléphone, code et QR depuis les réglages ; récupérer automatiquement la configuration du client web selon le contrat vérifié, protéger localement le jeton et gérer les changements de configuration.
- Introduire une frontière de fournisseurs de suivi indépendante des lecteurs ; BetaSeries est le seul fournisseur exposé, Trakt pourra être ajouté par un change ultérieur.
- Fusionner séries et films à regarder avec les programmes des applications : une carte par série identifiée, reprise non vue prioritaire, prochain épisode diffusé sinon, mêmes règles de tri et limite de 20 cartes.
- Alimenter le héro avec l'ensemble des contenus à regarder, sans limite de 20 candidats ; conserver ses règles de qualité, d'ordre et de repli.
- Choisir une application au premier lancement, même avec une seule offre connue, puis mémoriser ce choix par série ou film ; proposer les apps vidéo reconnues et « Autre application » ; mutualiser les préférences entre héro et À suivre.
- Présenter honnêtement les offres dont la disponibilité régionale n'est pas confirmée ; choisir et confirmer le pays dans les réglages, sans déduire une disponibilité d'une app installée.
- Ajouter vu/non vu, annulation non bloquante, choix d'un épisode diffusé, ajout au compte depuis un contenu existant, retrait/archivage et masquage local.
- Proposer un accès Android optionnel aux sessions média ; n'automatiser le marquage qu'avec une identité et une fin de lecture fiables, sinon demander discrètement au retour.
- En cas d'échec BetaSeries, retirer sa contribution et conserver les sources locales, avec un indicateur discret, détail et réessai ; aucune file de mutations hors ligne.
- Préparer les parcours et états nécessaires à une future passe Penpot, sans produire ni approuver de maquette dans ce change de cadrage.

## Capabilities

### New Capabilities

- `watch-tracking` : compte de suivi actif, identité partagée, sélection et mémorisation des lecteurs, mutations et annulation, preuves de visionnage et parcours communs.

### Modified Capabilities

- `betaseries-integration` : remplacer le contrat OAuth embryonnaire par l'association TV, le contrat API, les données et les limites vérifiées.
- `up-next` : fusion du suivi avec les sources locales, résolution des conflits, identité, ouverture, menu, badges et états multi-sources.
- `launcher-shell` : candidats BetaSeries du héro, déclenchement du parcours commun, indépendance des sources, navigation et indicateur de suivi.
- `settings` : accès au compte, au pays et à l'autorisation optionnelle ; réaffichage des contenus retirés également apportés par le suivi.
- `ui-testing` : contrats et scénarios déterministes de connexion, navigation, fusion, mutations, repli et retour d'une autre application.

## Impact

Points existants concernés : `UpNextItem`, `HeroItem`, `UpNextFeed`, `UpNextContentKey`, `UpNextController`, `UpNextOpener`, `HeroRepository`, `HomeViewModel`, `AppLauncher`, réglages et préférences, pipeline partagé des images et manifeste Android. Le fournisseur réseau, le stockage protégé, la résolution des lecteurs et l'observation optionnelle des sessions sont de nouvelles frontières ; les composables ne portent pas leur logique.

L'accès API et les vérifications de contrats sont détaillés dans `technical-verification.md`. Les endpoints authentifiés, les intents et les signaux sur TV ne sont considérés vérifiés qu'avec leur preuve ; les limites ne sont pas transformées en promesses d'ouverture ou de suivi universels.

## Dependencies

Aucun change ouvert au début du cadrage. P9 part des capabilities déjà livrées et archivées (P6, P7 et chargement partagé des images). Aucune dépendance à P8 Recherche : l'ajout au suivi part d'un contenu existant ; aucune recherche globale de catalogue n'est incluse.

## Non-goals

- P8 Recherche, recommandations, découverte et fonctions sociales : hors P9.
- Trakt et connexion simultanée de plusieurs fournisseurs : futur change de suivi, phase non fixée.
- Backend, collecte des mots de passe et lecteurs vidéo intégrés : hors périmètre.
- Garantie d'ouverture ciblée ou de suivi automatique pour toute app, accès aux bibliothèques personnelles et abonnements réels : non fournis par le simple lien BetaSeries.
- Passe visuelle Penpot et valeurs géométriques des nouveaux parcours : étape suivante sur instruction de Sygix, avant implémentation de l'UI.

## Questions ouvertes

Les limites et validations restantes sont centralisées dans `design.md`, section « Questions ouvertes », et dans `technical-verification.md`. Les règles produit déjà tranchées ne sont pas conditionnées à l'existence d'un catalogue régional exhaustif.
