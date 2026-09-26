# Delta up-next

## ADDED Requirements

### Requirement: Modèle provider-agnostic
La rangée Up Next SHALL être alimentée par des providers derrière une interface commune, chaque item étant un modèle canonique indépendant de l'app d'origine.

#### Scenario: constitution
- **WHEN** la rangée Up Next est chargée
- **THEN** chaque item expose série, saison, épisode, type (épisode ou film), titre d'affichage, poster, progression optionnelle, date d'activité, IDs externes quand le programme en expose et app provider ; les IDs externes manquants écartent les apps qui en dépendent (ex. Stremio sans IMDb)

#### Scenario: extension P4
- **WHEN** un nouveau provider (BetaSeries) est ajouté
- **THEN** il s'agit d'un nouvel adapter de l'interface provider, sans modification du modèle canonique ni de la rangée

### Requirement: Fusion Continue Watching + Next Up
La rangée SHALL fusionner les épisodes et films en cours avec les épisodes à suivre, dédoublonnés et triés par date d'activité décroissante.

#### Scenario: fusion
- **WHEN** le TV Provider contient des programmes Jellyfin en cours ou à suivre
- **THEN** la rangée affiche les items en cours (épisodes et films, avec barre de progression) et les épisodes à suivre, limités à 20 items

#### Scenario: dédoublonnage
- **WHEN** un même épisode apparaît à la fois comme en cours et comme à suivre
- **THEN** seul l'item en cours est retenu (progression et date de reprise conservées)

#### Scenario: tri
- **WHEN** la rangée est affichée
- **THEN** les items sont triés par date d'activité décroissante (date de reprise pour les items en cours, date de sortie sinon)

### Requirement: Résolution de l'app de lecture
La cible de lecture de chaque item SHALL être résolue par un score combinant l'ordre de préférence des apps persisté et la disponibilité, l'override par série étant prioritaire.

#### Scenario: résolution automatique
- **WHEN** un item n'a pas d'override
- **THEN** la cible est l'apps candidate au meilleur score (rang dans l'ordre de préférence × disponibilité) ; les apps non installées sont écartées ; à égalité, l'ordre de préférence tranche

#### Scenario: priorité de l'override
- **WHEN** la série d'un item possède un override vers une app installée
- **THEN** la cible est cette app, indépendamment du score

#### Scenario: disponibilité Stremio
- **WHEN** un item n'a pas d'ID IMDb
- **THEN** Stremio n'est pas considéré disponible pour cet item

### Requirement: Ouverture de lecture avec fallback
La lecture SHALL s'ouvrir via l'adapter de la cible résolue, avec fallback borné si l'ouverture échoue.

#### Scenario: Jellyfin
- **WHEN** la cible est Jellyfin
- **THEN** un intent `ACTION_VIEW` (data = ID de l'item, catégorie `LEANBACK_LAUNCHER`, extra `source=30`) est envoyé à l'activity de démarrage du client Jellyfin ; si l'intent échoue, l'app Jellyfin est lancée seule

#### Scenario: Stremio
- **WHEN** la cible est Stremio
- **THEN** le deep link `stremio:///detail/series/{imdb}/{imdb}:{saison}:{épisode}?autoPlay=true` (épisodes) ou `stremio:///detail/movie/{imdb}/{imdb}` (films) est ouvert ; si le lien échoue, l'app Stremio est lancée seule

#### Scenario: app sans deep link
- **WHEN** la cible est une app sans deep link supporté
- **THEN** l'app est lancée seule

#### Scenario: échec en cascade
- **WHEN** l'ouverture échoue pour la cible et son fallback
- **THEN** l'app suivante de l'ordre de préférence est tentée ; si aucune ne répond, un toast discret est affiché, sans crash

### Requirement: Override par série
Le menu contextuel d'une carte Up Next SHALL permettre de forcer l'app de lecture d'une série, avec persistance par exceptions.

#### Scenario: menu contextuel
- **WHEN** l'utilisateur fait un appui long sur une carte
- **THEN** un menu contextuel s'ouvre : « Toujours ouvrir avec » (apps supportées installées + « Automatique ») et « Retirer l'override » si un override existe pour la série

#### Scenario: application
- **WHEN** l'utilisateur choisit une app dans « Toujours ouvrir avec »
- **THEN** la persistance enregistre l'exception pour l'ID externe de la série, la cible de toutes les cartes de cette série est re-résolue et le badge se met à jour sans rechargement

#### Scenario: retrait
- **WHEN** l'utilisateur choisit « Retirer l'override »
- **THEN** l'exception est supprimée de la persistance et la série redevient « Automatique »

#### Scenario: sans override ajouté
- **WHEN** une série sans override apparaît dans la rangée
- **THEN** elle est résolue automatiquement, sans action requise

### Requirement: Badge app cible
Chaque carte Up Next SHALL afficher le logo de l'app de lecture résolue, sans texte.

#### Scenario: affichage
- **WHEN** une carte Up Next est affichée
- **THEN** un petit logo de l'app cible résolue est visible sur la carte, sans texte

#### Scenario: mise à jour
- **WHEN** la cible d'une série change (override ou préférence)
- **THEN** le badge des cartes concernées reflète la nouvelle cible sans rechargement de la rangée

### Requirement: États de la rangée
La rangée SHALL couvrir les états TV Provider sans contenu Jellyfin, chargement, erreur et vide, sans jamais bloquer le reste du home.

#### Scenario: aucun contenu Jellyfin
- **WHEN** aucun programme Jellyfin n'est présent dans le TV Provider (client absent, non authentifié ou rien en cours)
- **THEN** la rangée Up Next est absente du home, sans invitation à configurer quoi que ce soit

#### Scenario: chargement
- **WHEN** les données sont en cours de récupération
- **THEN** un squelette de cartes est affiché, sans flash de contenu

#### Scenario: erreur
- **WHEN** la requête vers le TV Provider échoue
- **THEN** une carte d'état focusable (message + « Réessayer ») remplace la rangée, sans crash

#### Scenario: vide
- **WHEN** aucun item n'est retourné
- **THEN** la rangée est masquée

#### Scenario: non-blocage
- **WHEN** la rangée est dans n'importe quel état
- **THEN** la navigation DPAD du home (héro, dock, grille) reste fonctionnelle
