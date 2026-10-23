# Reprise P9 — BetaSeries

## Point de départ

- Branche : `docs/spec-p9-betaseries`.
- Change : `p9-betaseries`.
- Cette PR contient le cadrage, six deltas de capability, le design technique, les vérifications et les tâches futures. Aucun code produit n'est implémenté.
- Prochaine session prévue : passe Penpot, uniquement quand Sygix la demande. Ne pas démarrer l'implémentation depuis ce handoff.

## Lire dans cet ordre

1. `proposal.md` : périmètre et exclusions.
2. `technical-verification.md` : preuves API et limites Android, sans données personnelles.
3. `design.md` : frontières, identité, configuration web, transport et mutations.
4. `specs/watch-tracking/spec.md`, puis deltas BetaSeries, up-next, launcher-shell, settings et ui-testing.
5. `tasks.md` : validations préalables et suite d'implémentation, toutes non cochées.

## Décisions à préserver pendant les maquettes

- Une rangée À suivre, 20 cartes, tri actuel. Une seule carte par série identifiée ; reprise non vue prioritaire, prochain non vu diffusé sinon. Pas de carte pour un épisode futur.
- Héro alimenté par tout le À regarder, même au-delà des 20 cartes, avec qualité et ordre de session existants.
- Même préférence de lecteur depuis le héro et les cartes : premier choix confirmé, même si une seule offre ; mémorisation par série/film, changement via menu, nouvelle demande si app non lançable ou indisponibilité établie.
- Apps vidéo reconnues puis Autre application ; ne pas confondre source du suivi et lecteur.
- Disponibilité non confirmée si la région n'est pas prouvée ; pays confirmé/modifiable. Aucun filtre ne masque un contenu uniquement faute d'offre.
- Badge du lecteur choisi, sinon BetaSeries ; informations habituelles sans mode anti-spoilers supplémentaire.
- Compte actif unique ; déconnexion pour changer, préférences de lecteur et masquages locaux conservés sur la TV.
- Vu/non vu sur le seul épisode choisi ; Annuler non bloquant, suppression distante différée pendant sa fenêtre.
- Confirmation Terminé discrète sans preuve fiable ; accès média optionnel ; aucun marquage automatique à partir du simple lancement de l'app.
- Masquage local de la série entière toutes sources d'À suivre, et des candidats BetaSeries du héro ; archive/retrait du compte distincts. Une reprise locale peut rester après archivage.
- Panne BetaSeries : apport retiré, apps locales conservées, indicateur discret sans notification répétée ; actions tentables avec erreurs/réessai, pas de file hors ligne.

## Écrans et états à maqueter

| Parcours | États à montrer |
| --- | --- |
| Réglages Suivi de visionnage | Déconnecté, connecté, synchronisation, vide, panne serveur, accès expiré, déconnexion |
| Association TV | Préparation, adresse + code + QR, attente, code expiré/consommé, réponse serveur invalide, réessai, succès |
| Pays | Proposition non confirmée, sélection, validation, modification et disponibilité non confirmée |
| Accès média | Désactivé, à autoriser, accordé, refusé/révoqué, indisponible sur la TV ; explication du repli manuel |
| Choix du lecteur | Première sélection, préférence existante, offres incertaines, Autre application, aucune app, app disparue, ouverture limitée à fiche/recherche/app |
| Menu d'un contenu | Ouvrir avec, vu/non vu, choisir un épisode, ajouter au compte si absent, archiver/retirer si applicable, retirer localement d'À suivre |
| Épisodes | Saisons et épisodes diffusés vus/non vus, courant, chargement, vide, erreur, reconnexion |
| Retour de lecture | Terminé non bloquant, marquage en cours/succès/échec, Annuler, retrait en attente puis erreur éventuelle |
| Accueil | Carte BetaSeries sans lecteur puis avec, reprise fusionnée, héro avec source choisie, panne avec et sans cartes restantes |

## Décisions visuelles encore à valider

- Position et accès à l'indicateur, y compris quand À suivre est masqué.
- Accès D-pad à Terminé et Annuler sans prendre le focus automatiquement, avec carte d'origine disparue ou remplacée par l'épisode suivant.
- Durée et comportement de la fenêtre Annuler pendant l'interaction ; aucun retrait distant ne part avant sa fin.
- Présentation du sélecteur de saisons/épisodes et des actions de compte distinctes du masquage local.

Réutiliser le design system et les composants existants. Montrer les aperçus à Sygix, conserver les variantes non choisies comme telles et reporter les décisions dans ce change avant de déclarer l'UI prête à implémenter.

## Limites techniques à représenter honnêtement

L'association sans secret d'application a fonctionné avec la configuration récupérée du client web, mais ce mécanisme n'est pas un contrat stable. Les offres par contenu n'ont pas démontré un filtrage régional. L'ouverture ciblée et la preuve de fin restent à vérifier par app sur TV. Les maquettes doivent donc inclure les replis prévus, sans exiger des données que le service ou l'app ne fournit pas.

## Archive et suite

Ne pas archiver après le merge de la seule spec. Les tâches appareil viennent avant le code concerné, la passe Penpot avant le code UI, puis l'implémentation et sa validation font l'objet d'une autre session. Archiver seulement après merge de l'implémentation et validation TV.
