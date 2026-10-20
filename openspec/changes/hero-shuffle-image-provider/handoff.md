# Reprise de l'implémentation

## Point de départ

Branche de spec : `docs/spec-hero-image-loading`, empilée sur `docs/spec-compact-requirements` (PR #81), elle-même sur l'archivage Tsuyu (PR #80). L'implémentation doit partir de cette spec validée ou de `main` après intégration de la pile, pas de l'ancienne proposition limitée au shuffle.

Change implémenté : `hero-shuffle-image-provider`, sur `feat/shared-artwork-loading`, empilé sur la branche de spec. `tasks.md` distingue les vérifications automatisées et les validations finales sur la TV encore en attente. Le relevé préalable a été explicitement dispensé par Sygix ; il n'est pas présenté comme réalisé.

Commande de reprise :

```text
/opsx-verify hero-shuffle-image-provider
```

## Décisions confirmées

- Périmètre : héro, Top Shelf au focus d'une app et À suivre, y compris validation, préchargement et affichage.
- Une récupération et une analyse de dimensions par version partagées ; politiques de qualité propres aux destinations et bitmaps décodés à leurs tailles.
- Récupérations séquentielles par app, apps parallèles sous constante globale ; une app bloquée ne monopolise pas un lot collectif. Valeurs techniques initiales à confirmer sur TV : 3 apps et budget de résultat de 8 s, file et alternatives comprises.
- Accès aux fournisseurs tiers instable, timeout couvrant ouverture/MIME/lecture et annulation n'empêchant pas les échéances suivantes ; worker bloqué quarantiné, pas de deuxième récupération physique de cette app.
- Cache disque des octets HTTP(S) et content://, immédiat si utilisable : frais moins de 24 h, secours jusqu'à moins de 7 jours ; lecture/affichage ne prolongent pas les dates. Les programmes doivent rester valides.
- Programmes : chaque table confirme ses présents sur succès et retire ses absents ; une erreur ne confirme rien, ancien résultat utilisable moins de 24 h. Perte de permission/désinstallation et réponses tardives ne maintiennent pas les programmes.
- Qualité : héro inchangé ; image paysage Top Shelf à 1× la largeur du panneau ; À suivre à 1× la largeur de carte, portrait accepté. Largeurs en pixels hors zoom.
- Affiche et miniature conservées et essayées comme alternatives ; aucune image d'un autre film. Une image de série explicitement partagée peut être réutilisée entre épisodes.
- Héro : vidéo nature puis fond neutre sans perdre le bon titre/bouton, navigation et défilement continuant. Top Shelf : seulement images prêtes de la bonne app, fermé sinon. À suivre : placeholder en dernier recours.
- Shuffle du héro une fois par processus, stable pendant la session, nouveaux programmes insérés au hasard après le courant, retraits sans réordonner les autres.

## Relevé préalable dispensé

Sygix a demandé d'ignorer cette étape et de poursuivre le code. Les schémas d'URI, dimensions, erreurs et identifiants réellement publiés par Jellyfin, Disney+ et Crunchyroll restent non vérifiés sur la TV. Les validations 7.2 et 7.3 restent ouvertes ; n'en conserver ensuite que les structures, sans URI brute, serveur, titre ni historique personnel. Le schéma content:// de Disney+ et Crunchyroll reste une hypothèse.

## Contrats repliés dans leurs exigences parentes

La version finale de la PR #81 a replié « Données facultatives du héro » et « Relecture des programmes du héro » dans « Contenu héro TV Provider », « Synchronisation du héro » et « Visuel du héro en attente » dans « Diaporama héro », « Reprise des visuels du héro » dans « Préchargement et mémoire », et « Qualité des cartes » dans « Carte Up Next ». Ce change modifie donc ces exigences parentes, rétablit leurs paragraphes **Détails.** avec les règles mises à jour et ne cible plus les contrats disparus ; les anciennes règles 1 s et 2× ne subsistent nulle part.

Les règles de géométrie, focus, animation, fluidité et fenêtre de composition restent celles de la base ; les listes de contenus retirés et la mémoire utilisateur ne sont pas purgées comme des programmes périmés.

## Vérification attendue

Suivre `tasks.md` : tests de partage et comptage, seuils par usage, concurrence/timeout/annulation bloqués, cache après recréation, frontières 24 h/7 jours, expiration déjà à l'écran, générations tardives, focus et replis des trois zones. Puis validation OpenSpec, suite Gradle et release sur la TV. La validation de ces documents ne prouve pas encore la correction sur appareil.
