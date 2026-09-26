# Design : p2c-upnext

## Décisions

### Modèle canonique provider-agnostic
L'unité de la rangée est un `UpNextItem` unique, produit par une interface `UpNextProvider` (une implémentation Jellyfin en p2c, une BetaSeries en P4). Champs : IDs interne et externes (IMDb, TVDB si fournis), type (épisode / film), série, saison, épisode, titre d'affichage, poster, progression optionnelle (0–1), timestamp d'activité (reprise ou date de sortie), provider. Les providers retournent des items **stérilisés en IDs externes dès l'extraction** (Jellyfin : `ProviderIds` sur l'épisode et la série) : c'est ce qui permet à P4 de fusionner sans refonte et au routage de matcher une série Jellyfin avec sa contrepartie Stremio.

### Fusion Continue Watching + Next Up (Jellyfin)
Deux appels API : `/Users/{userId}/Items/Resume?MediaTypes=Video&IncludeItemTypes=Episode,Movie` et `/Shows/NextUp?UserId=…`. Fusion locale :
- Dédup : un même épisode présent dans les deux listes → **l'item en cours gagne** (progression + date de reprise plus fraîche) ; un film n'existe que côté Resume
- Tri final par **date d'activité décroissante** : timestamp de reprise pour les items en cours, date de sortie ajoutée sinon (spec launcher-shell : « tri par date d'activité »)
- Limite : 20 items fusionnés max

### Routage de lecture : score, pas if/else
`PlaybackTargetResolver` prend un `UpNextItem` et retourne une cible résolue (app + mode d'ouverture). Résolution :
1. **Override par série** (ID externe) → app imposée, court-circuite le score
2. Sinon, pour chaque app candidate : score = rang dans l'ordre de préférence persisté × disponibilité catalogue. Disponibilité Jellyfin : item issu du serveur Jellyfin → toujours « dans le catalogue » par nature (pas de vérification supplémentaire). Dispo Stremio : IDs IMDb présents (Stremio indexe par IMDb) — Stremio n'est jamais considéré disponible sans IMDb ID
3. Plus haut score gagne ; égalité → l'ordre de préférence tranche

L'ordre de préférence est persisté comme **liste ordonnée d'apps supportées installées** (défaut : Jellyfin, Stremio) ; apps non installées écartées à la résolution. Chaque app est un `PlaybackAdapter` avec ses modes :
- **Jellyfin** : `ACTION_VIEW` data=item ID + catégorie `LEANBACK_LAUNCHER` + extra `source=30` sur `org.jellyfin.androidtv/.ui.startup.StartupActivity` — intent **non documenté** (discussions jellyfin#3452, issue #3170 : pas de deep link officiel). Fallback : lancement de l'app. Le commentaire de code pointera vers les discussions amont.
- **Stremio** : deep link `stremio:///detail/series/{imdbId}/{imdbId}:{season}:{episode}?autoPlay=true` (SDK officiel, `autoPlay` supporté sur l'app Android TV) ; films : `stremio:///detail/movie/{imdbId}/{imdbId}`. Fallback : lancement de l'app.
- **Fallback générique** : `LeanbackLauncher` intent du package — l'app seule, sobre (pas de GLOBAL_SEARCH handoff : dépendant du device/Google TV, écarté).

Si l'adapter primaire échoue à l'exécution (`ActivityNotFoundException`, app désinstallée entre-temps), le résolveur retente **uniquement en mode fallback app** de la même app, puis passe à l'app suivante de l'ordre de préférence — jamais de crash silencieux, toast discret si aucune app ne répond.

### Badge app cible
Petit logo de l'app résolue en bas-droite de la carte (icônes des apps cibles, vectorielles embarquées — pas de scraping dynamique), sans texte. Re-rendu dynamique : le badge reflète la cible résolue, donc l'override change visuellement le badge sans recharger la rangée.

### Override par série : menu contextuel
Long-press OK sur une carte (pattern existant du menu contextuel de la grille) ouvre un menu : « Toujours ouvrir avec » (liste des apps supportées installées + « Automatique ») et « Retirer l'override » (affiché seulement si un override existe pour la série). Persistance : **map série → app**, jamais un snapshot de l'état résolu — une série sans override ajoutée plus tard reste « Automatique » sans action. Clé = ID externe (IMDb/TVDB), pas l'ID interne Jellyfin, pour survivre au changement de serveur et matcher BetaSeries en P4.

### États de la rangée
Sous le héro, au-dessus de la grille (position de launcher-shell). États :
- **TV Provider sans contenu** : rangée masquée (client Jellyfin absent, non authentifié ou rien en cours) — pas d'invitation à configurer quoi que ce soit, l'intégration n'a aucune config
- **Loading** : squelette discret de N cartes (même matériau que les rangées de la grille), jamais de flash de contenu
- **Erreur** : carte unique d'état (icône + message court + « Réessayer » focusable), pas d'erreur dans une snackbar volatile
- **Vide** : rangée masquée (convention empty-state du repo : pas de bouton grisé, rien à afficher)
Timeouts réseau bornés (10 s), retry manuel via la carte d'état ; pas d'auto-refresh silencieux en p2c.

### Source Jellyfin : TV Provider (décision)
Décision prise en amont : **pas d'API directe au serveur Jellyfin** pour le moment. Le client Jellyfin officiel publie déjà les items en cours (épisodes + films, position incluse) et les épisodes Next Up dans `WatchNextPrograms` (synchro WorkManager périodique côté client) — poster, titre, progression et intent de reprise sont portés par le programme. p2c lit ça via la permission `READ_TV_LISTINGS` (déjà demandée au premier lancement) : zéro réseau, zéro credentials, zéro écran de config. Conséquences : pas d'écran « non configuré » (état vide standard à la place) et la **recherche** reste hors scope tant qu'aucune API directe n'est introduite.

## Notes de test
- Fusion + dédup + tri : tests unitaires JVM purs (mappeurs JSON → items canoniques → fusion)
- Résolution de cible : tests unitaires (score, override, absence d'IMDb → Stremio indisponible, fallback execution)
- UI : Robolectric + Compose test rules (focus rangée, DPAD, badge, menu contextuel d'override), assertions sémantiques uniquement, timing constants respectés (pas de sleeps)
