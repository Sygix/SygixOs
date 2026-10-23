# Reprise P9 — BetaSeries

## Point de départ

- Branche de cadrage : `docs/spec-p9-betaseries`, PR #93 ; après sa fusion, reprendre depuis `main` dans une nouvelle branche d'implémentation.
- Change : `p9-betaseries`.
- Cette PR contient le cadrage, six deltas de capability, le design technique, les vérifications et les tâches futures. Aucun code produit n'est implémenté.
- Maquette Penpot validée par Sygix après les retours visuels reportés ci-dessous, y compris la fenêtre d'annulation de 10 secondes. Ne pas démarrer l'implémentation depuis ce handoff sans instruction.

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
- Une carte locale identifiée absente du compte connecté propose Ajouter au suivi, mais garde son ouverture locale avant l'ajout ; l'appartenance confirmée, pas la simple correspondance catalogue, active le lecteur partagé.
- Vu/non vu sur le seul épisode choisi ; Annuler non bloquant, suppression distante différée pendant sa fenêtre.
- Confirmation Terminé discrète sans preuve fiable ; accès média activé et vérifié automatiquement via ADB/Tsuyu déjà utilisable, accès Android manuel optionnel en repli ; aucun marquage automatique à partir du simple lancement de l'app.
- Masquage local de la série entière toutes sources d'À suivre, et des candidats BetaSeries du héro ; archive/retrait du compte distincts. Une reprise locale peut rester après archivage.
- Panne BetaSeries : apport retiré, apps locales conservées, indicateur discret sans notification répétée ; actions tentables avec erreurs/réessai, pas de file hors ligne.

## Écrans et états à maqueter

| Parcours | États à montrer |
| --- | --- |
| Réglages Suivi de visionnage | Déconnecté, connecté, synchronisation, vide, panne serveur, accès expiré, déconnexion |
| Association TV | Préparation, adresse + code + QR, attente, code expiré/consommé, réponse serveur invalide, réessai, succès |
| Pays | Dropdown natif : proposition non confirmée, sélection, validation/annulation, modification et disponibilité non confirmée ; sans « · confirmé » après la valeur |
| Accès média | Activation automatique ADB/Tsuyu en cours, accès vérifié, échec et repli ; désactivé, à autoriser manuellement, accordé, refusé/révoqué, indisponible sur la TV |
| Choix du lecteur | Première sélection, préférence existante, offres incertaines, Autre application, aucune app, app disparue, ouverture limitée à fiche/recherche/app |
| Menu d'un contenu | Ouvrir avec, vu/non vu, choisir un épisode, ajouter au compte si absent, archiver/retirer si applicable, retirer localement d'À suivre |
| Épisodes | Saisons et épisodes diffusés vus/non vus, courant, chargement, vide, erreur, reconnexion |
| Retour de lecture | Terminé non bloquant, marquage en cours/succès/échec, Annuler, retrait en attente puis erreur éventuelle |
| Accueil | Carte BetaSeries sans lecteur puis avec, reprise fusionnée, héro avec source choisie, panne avec et sans cartes restantes |

## Décisions visuelles confirmées

- Association : texte et QR portent la même `verification_url` complète avec son chemin, par exemple `https://www.betaseries.com/device`, et non le domaine seul.
- Pays : composant dropdown des réglages, comme la position d'À suivre, sans modale ni « · confirmé ».
- Modales : réutiliser le composant de modale de la maquette, fond et bordure glass transparents floutés, dimensions adaptées au contenu ; notamment état 15.5 sans grande réserve vide.
- Retour de lecture : composant réutilisable Notification de suivi compact en bas à droite dans la zone sûre, largeur adaptée au contenu, même matière glass que les modales, sans vol du focus.
- Statut de synchronisation : petit, aligné à droite du titre À suivre, sans « · Réglages ».
- Annuler : 10 secondes actives, suspendues pendant l'interaction puis reprises avec le temps restant ; aucun retrait distant avant échéance.

Référence partagée : [SygixOs Maquette](https://design.penpot.app/#/workspace?team-id=52a5041f-08a1-80f1-8008-c3c379c88195&file-id=17193a1f-939a-80b5-8008-c3fe694e0481), pages P9 et composants existants. Les images statiques ne prouvent ni le trajet D-pad ni l'accès Android réel.

## Navigation encore à valider avant code UI

- Position et accès à l'indicateur, y compris quand À suivre est masqué.
- Accès D-pad à Terminé et Annuler sans prendre le focus automatiquement, avec carte d'origine disparue ou remplacée par l'épisode suivant.
- Navigation du sélecteur de saisons/épisodes et des actions de compte distinctes du masquage local.

Réutiliser le design system et les composants existants. Les aperçus et retours visuels ne déclarent pas l'UI prête à implémenter tant que les parcours de navigation ci-dessus ne sont pas complétés et validés.

## Limites techniques à représenter honnêtement

L'association sans secret d'application a fonctionné avec la configuration récupérée du client web, mais ce mécanisme n'est pas un contrat stable. Les offres par contenu n'ont pas démontré un filtrage régional. L'ouverture ciblée et la preuve de fin restent à vérifier par app sur TV. Les maquettes doivent donc inclure les replis prévus, sans exiger des données que le service ou l'app ne fournit pas.

## Archive et suite

Ne pas archiver après le merge de la seule spec. Les tâches appareil viennent avant le code concerné, la passe Penpot avant le code UI, puis l'implémentation et sa validation font l'objet d'une autre session. Archiver seulement après merge de l'implémentation et validation TV.
