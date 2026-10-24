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
- Proposer uniquement la confirmation manuelle « Terminé ? » au retour d'un lecteur, sans accès aux notifications ni sessions média.
- Conserver la liste du compte 15 minutes et patcher uniquement le contenu muté après l'avoir relu ; mettre en cache l'identité de chaque contenu local. Une lecture complète intervient à la connexion ou au retour au premier plan après 15 minutes, sans minuterie de synchronisation.
- En cas de page ou fiche en échec, conserver les autres contenus valides ; retirer l'apport du compte si l'accès est refusé. Détail et réessai restent accessibles ; aucune file hors ligne.
- Conserver les parcours et états de la passe Penpot ainsi que les décisions visuelles confirmées dans le design et les scénarios ; les planches ne valent ni implémentation ni validation sur TV.

## Capabilities

### New Capabilities

- `watch-tracking` : compte actif, identité partagée, lecteurs, mutations, annulation et confirmation manuelle au retour.

### Modified Capabilities

- `betaseries-integration` : remplacer le contrat OAuth embryonnaire par l'association TV, le contrat API, les données et les limites vérifiées.
- `up-next` : fusion du suivi avec les sources locales, résolution des conflits, identité, ouverture, menu, badges et états multi-sources.
- `launcher-shell` : candidats BetaSeries du héro, déclenchement du parcours commun, indépendance des sources, navigation et indicateur de suivi.
- `settings` : compte, pays et réaffichage des contenus retirés également apportés par le suivi.
- `ui-testing` : contrats et scénarios déterministes de connexion, navigation, fusion, mutations, repli et retour d'une autre application.

## Impact

Points concernés : modèles, fusion et mémoire À suivre, héro, ouverture, réglages, images et stockage protégé. Les ViewModels pilotent les intents et le focus ; les composables rendent leur état.

Preuves : `technical-verification.md`.

## Dependencies

Aucun change ouvert au début du cadrage. P9 part des capabilities déjà livrées et archivées (P6, P7 et chargement partagé des images). Aucune dépendance à P8 Recherche : l'ajout au suivi part d'un contenu existant ; aucune recherche globale de catalogue n'est incluse.

## Non-goals

- P8 Recherche, recommandations, découverte et fonctions sociales : hors P9.
- Trakt et connexion simultanée de plusieurs fournisseurs : futur change de suivi, phase non fixée.
- Backend, collecte des mots de passe et lecteurs vidéo intégrés : hors périmètre.
- Garantie d'ouverture ciblée ou de suivi automatique pour toute app, accès aux bibliothèques personnelles et abonnements réels : non fournis par le simple lien BetaSeries.
- Preuve automatique de fin via sessions média : futur change, après validation d'adaptateurs.

## Questions ouvertes

Aucune question produit ouverte dans ce périmètre ; les validations appareil restantes sont les tâches 1.x et 8.2. Demande d'une clé API officielle à BetaSeries : à trancher par Sygix (voir design, Risks).
