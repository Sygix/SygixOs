# Spec Delta

## MODIFIED Requirements

### Requirement: Grille d'apps
Le launcher SHALL auto-détecter les apps TV et conserver la grille, les titres non focusables et l'ordre des sections de settings. Le titre À suivre SHALL rester visible en erreur ; son masquage SHALL dépendre de toutes ses sources selon up-next, pas de la seule permission TV Provider. Style et espacements existants SHALL être conservés.

#### Scenario: affichage
- **WHEN** le home s'ouvre
- **THEN** les apps TV apparaissent en cinq colonnes, tuiles 16:9 avec bannière sinon icône entière centrée ; ordre et épinglage restent persistés

#### Scenario: menu contextuel
- **WHEN** OK est maintenu sur une tuile
- **THEN** le menu propose les actions existantes d'épinglage, déplacement dans la grille et masquage ; OK valide, Retour ferme, un appui court ouvre l'app

#### Scenario: lisibilité du menu contextuel
- **WHEN** le menu s'ouvre sur des tuiles claires ou sombres
- **THEN** le voile sombre et le verre du menu rendent le texte lisible ; nom et package précèdent les actions verticales, la pilule claire focusée n'a ni zoom ni halo et les actions non focusées n'ont pas de fond

#### Scenario: navigation dans le menu
- **WHEN** le menu d'une tuile s'ouvre
- **THEN** la première action, Épingler ou Retirer du dock selon l'état, a le focus ; haut/bas parcourent les actions sans boucle, gauche/droite ne sortent pas du menu, Retour rend le focus à la tuile

#### Scenario: titres de section
- **WHEN** À suivre est réglé avant les applications
- **THEN** son titre précède sa rangée puis Applications et sa première rangée ; l'ordre inverse est utilisé avec le réglage après les applications

#### Scenario: titres jamais focusables
- **WHEN** le D-pad passe d'une section à l'autre
- **THEN** il saute les titres et garde le titre de la section d'arrivée entièrement visible au-dessus de sa première ligne

#### Scenario: section Up Next masquée
- **WHEN** la rangée est désactivée ou qu'aucune source ne fournit de contenu affichable selon up-next
- **THEN** son titre et sa place sont supprimés ; Applications devient le seul titre s'il existe des apps ; un indicateur de suivi en erreur reste accessible selon son propre contrat

#### Scenario: aucune app TV détectée
- **WHEN** aucune app TV n'est détectée
- **THEN** seul « Aucune app TV détectée » occupe l'espace des apps sans titre Applications ; À suivre suit ses propres sources

#### Scenario: titre au-dessus de la carte d'erreur
- **WHEN** la carte d'erreur locale remplace le contenu de la rangée
- **THEN** le titre À suivre reste au-dessus et appartient au bloc focusé

#### Scenario: style des titres
- **WHEN** une section est affichée
- **THEN** son titre utilise `SygixTypography.titleLarge` et `SygixColors.OnDarkSecondary`, aligné sur la première carte, séparé par `Dimens.SectionTitleGap` ; le second titre suit l'espacement `Dimens.GridRowSpacing`

**Détails.** — Détection : catégories LEANBACK_LAUNCHER et LAUNCHER. Les titres partagent le fond de la grille, défilent avec leur section et restent alignés à `Dimens.ScreenMarginH`, premier titre à `Dimens.GridTopMargin`, conformément aux maquettes 4.1/7.1/7.2/7.5 et au composant Titre de section. Les groupes vides et titres correspondants sont absents ; une erreur avec carte garde son titre. Le haut de la zone grille reste le haut du titre de sa première section.

### Requirement: Rangée Up Next
Le home SHALL afficher À suivre dans la zone grille si son réglage est actif et que ses sources indépendantes fournissent un état affichable. Sa position SHALL suivre settings ; contenu, fusion, cartes, états et ouverture SHALL suivre up-next, y compris l'apport du compte sans permission TV Provider.

#### Scenario: contenu
- **WHEN** des programmes locaux ou contenus du compte sont affichables et que le réglage est actif
- **THEN** la rangée et son titre sont affichés à la position réglée sur le fond de la grille

#### Scenario: sans contenu
- **WHEN** aucune source ne fournit de contenu affichable ni de carte d'erreur selon up-next
- **THEN** rangée et titre sont masqués ; le refus de READ_TV_LISTINGS seul ne masque pas les contenus du compte

#### Scenario: réglage désactivé
- **WHEN** Afficher À suivre est désactivé
- **THEN** rangée et titre quittent la navigation sans place réservée ; réactiver réaffiche le contenu à sa position sans redémarrage

### Requirement: Écran initial du home
Le home SHALL s'ouvrir sur un héro plein écran sans cadre, avec les programmes locaux et contenus de suivi admissibles, sinon le repli nature, et le dock en overlay bas. Au démarrage à froid, le fondu de l'écran de démarrage SHALL révéler le héro sans attendre une connexion BetaSeries.

#### Scenario: état initial
- **WHEN** le launcher démarre
- **THEN** le héro occupe tout l'écran sans aperçu du suivant, prend le focus dès le début du fondu de sortie du démarrage et porte le dock semi-transparent en overlay bas

#### Scenario: fallback sans contenu
- **WHEN** aucune source ne fournit de programme admissible
- **THEN** les vidéos nature bouclent, précédées du dégradé sombre qui effectue un passage lent puis reste immobile à sa dernière position tant qu'elles ne jouent pas, sans écran noir ni crash ; une permission TV refusée n'écarte pas les contenus BetaSeries accessibles

### Requirement: Navigation 3 paliers
Le D-pad SHALL conserver héro, dock et grille comme trois paliers, avec une seule zone active. À suivre SHALL rester une ligne de grille. Parcours et actions de suivi SHALL restaurer leur origine sans créer de palier ; les messages non bloquants ne SHALL pas voler le focus. Défilement, positions et replis existants SHALL être conservés.

#### Scenario: descente depuis le héro
- **WHEN** bas est pressé depuis le héro
- **THEN** le premier élément du dock prend le focus, sinon la grille avec son défilement existant

#### Scenario: traversée du dock
- **WHEN** l'utilisateur descend du dock
- **THEN** héro, dock et capsule sortent ensemble par le haut pendant que la grille remonte en une animation continue ; elle retrouve sa position et son dernier élément, sinon le premier de sa première ligne ; la lecture du héro est en pause

#### Scenario: rangée Up Next affichée en tête
- **WHEN** l'utilisateur descend pour la première fois avec À suivre avant les applications
- **THEN** son titre arrive en haut, sa première carte prend le focus et bas mène aux apps sans changement de fond

#### Scenario: rangée masquée
- **WHEN** la rangée est masquée
- **THEN** titre et rangée sont sautés sans place réservée ; la grille restaure les apps selon ses règles habituelles

#### Scenario: déplacements dans la rangée Up Next
- **WHEN** gauche ou droite est pressé sur À suivre
- **THEN** le focus reste dans la rangée sans boucle aux bords

#### Scenario: retour dans la grille à la position laissée
- **WHEN** la grille est quittée puis réouverte
- **THEN** elle retrouve sa position de défilement et son dernier élément à la même place à l'écran, sans descente forcée d'un écran

#### Scenario: carte laissée disparue avant le retour
- **WHEN** la dernière carte visitée n'existe plus au retour
- **THEN** sa suivante, sinon précédente prend le focus ; si la rangée est vide le repli de rangée vide s'applique

#### Scenario: dock jamais visible en vue grille
- **WHEN** la grille est active, pendant ou après défilement
- **THEN** le dock ne recouvre jamais la grille et ne réapparaît qu'avec le héro

#### Scenario: remontée
- **WHEN** haut est pressé sur la première ligne de grille
- **THEN** le défilement inverse restaure le dock, sinon le héro ; haut depuis le dock rend le focus au héro et relance sa lecture

#### Scenario: remontée depuis les apps
- **WHEN** haut est pressé sur la première rangée d'apps
- **THEN** si À suivre est placé avant, sa dernière carte visitée, sinon sa première carte prend le focus sans défilement vers le héro ; sinon la remontée vers le dock s'applique

#### Scenario: position après les applications
- **WHEN** À suivre est placé après les apps
- **THEN** bas depuis leur dernière rangée atteint À suivre, bas à son bord ne fait rien, haut revient à la dernière rangée d'apps

#### Scenario: carte focusée disparue
- **WHEN** une carte disparaît, notamment par synchronisation du compte, panne, déconnexion ou retrait local
- **THEN** sa suivante, sinon précédente prend le focus sans changer la position verticale ; les sources restantes ne provoquent pas de retour forcé au héro

#### Scenario: rangée devenue vide
- **WHEN** la rangée devenue vide ou masquée portait le focus ou la dernière carte visitée
- **THEN** la première tuile d'app si elle était avant les apps, la dernière si elle était après reçoit le focus ; sans app le héro reprend le focus

#### Scenario: retour depuis une app
- **WHEN** l'utilisateur revient après ouverture depuis À suivre, carte ou menu
- **THEN** la position verticale de grille est restaurée et le contenu ouvert revient premier et focusé selon sa clé, même avec un autre épisode ; s'il a disparu le repli part de sa position à l'ouverture ; la rangée revient à son début pour un contenu retrouvé

#### Scenario: retour depuis la grille
- **WHEN** Retour est pressé dans la grille
- **THEN** la page revient au héro en une seule animation continue et le héro reprend focus et lecture

#### Scenario: continuité de l'animation
- **WHEN** haut, bas ou Retour est pressé pendant un défilement
- **THEN** l'animation repart de sa position courante vers la nouvelle cible sans saut

#### Scenario: cohabitation avec la Top Shelf
- **WHEN** À suivre a le focus
- **THEN** aucun Top Shelf n'est ouvert ; il reste lié aux apps

#### Scenario: couches inactives
- **WHEN** une zone ou un contexte est inactif
- **THEN** ses éléments ne prennent pas le focus ; gauche/droite restent dans la zone active et une surcouche de suivi active isole son propre focus selon watch-tracking

#### Scenario: retour depuis le héro
- **WHEN** une app ouverte par le parcours de suivi depuis le héro rend la main
- **THEN** le héro retrouve son contexte et le programme encore admissible, sinon son voisin ou le fallback ; aucune restauration ne force la grille

#### Scenario: proposition non bloquante
- **WHEN** Terminé ou Annuler est proposé au retour
- **THEN** l'origine conserve son focus ; l'action reste accessible au D-pad selon le parcours complété dans la passe Penpot avant implémentation, sans quatrième palier

**Détails.** — Le home reste une page continue, héro/dock puis grille, sans fondu ni saut dans la traversée, avec durée/courbe de Focus tvOS. Première entrée : titre de la première section en haut et première ligne dessous. Retour : position et élément laissés, pas une descente d'un écran imposée. La disparition d'un élément suit Repli du focus des cartes. Les contrôles du suivi n'annulent ni ces positions ni l'isolation des zones inactives.

### Requirement: Contenu héro TV Provider
Le héro SHALL conserver les programmes watch next et preview encore publiés des apps installées sous READ_TV_LISTINGS, avec leurs champs facultatifs et empreintes indépendantes de ligne/progression. L'apport du compte SHALL suivre « Héro du suivi », indépendant de cette permission ; le fallback SHALL s'appliquer seulement sans candidat admissible de l'une ou l'autre source.

#### Scenario: permission
- **WHEN** le launcher démarre sans READ_TV_LISTINGS
- **THEN** la demande système existante s'affiche ; son octroi recharge les programmes sans redémarrage et son refus laisse fonctionner l'apport du compte, sinon le fallback

#### Scenario: programmes disponibles
- **WHEN** des apps publient des programmes
- **THEN** vidéo puis images selon les contrats partagés, titre, source, type, saison, épisode, durée et progression disponibles sont conservés, sans liste d'apps codée en dur ; le type affiché provient de `watch_next_type` pour Watch Next, du type preview sinon, et les numéros de `season_display_number` / `episode_display_number` ; les requêtes sans clause de sélection sont filtrées côté launcher et l'ordre suit Ordre aléatoire du héro

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme et qu'aucun contenu de suivi admissible n'existe
- **THEN** le fond nature puis neutre assure le repli sans écran vide

#### Scenario: colonnes facultatives
- **WHEN** type, saison, épisode, durée ou position ne sont pas publiés
- **THEN** seuls ces éléments manquent, selon Diaporama héro

#### Scenario: rafraîchissement automatique
- **WHEN** un changement du TV Provider est signalé au premier plan ou que sa relecture périodique est due
- **THEN** le provider est relu au plus tard toutes les 15 minutes au premier plan, jamais en arrière-plan ; le retour déclenche une relecture et le compte suit sa propre source partagée

#### Scenario: programme retiré
- **WHEN** une app retire un programme
- **THEN** il quitte héro et Top Shelf à la prochaine lecture ; s'il était affiché, média et textes changent par le fondu existant et ses visuels quittent l'ensemble vérifié

#### Scenario: programme modifié
- **WHEN** titre ou visuel d'un programme change
- **THEN** la nouvelle version est reflétée dans le héro et le Top Shelf à la prochaine lecture ; seule son image nouvelle est vérifiée et récupérée

#### Scenario: programme republié ou progression mise à jour
- **WHEN** seule la ligne, la progression, l'activité ou la durée change
- **THEN** place, image et état sont conservés sans récupération ni revalidation ; seuls les textes concernés évoluent

#### Scenario: source en erreur ou absente
- **WHEN** une table du provider échoue, manque ou dépasse son délai
- **THEN** erreur et vide restent distincts ; seuls ses anciens programmes encore valides restent affichables ; le compte et les autres tables continuent, sinon les replis existants s'appliquent

**Détails.** — Les colonnes facultatives ne sont pas inventées ; leur remplissage réel reste vérifiable sur appareil. Le héro et le Top Shelf conservent le contrat de Validité des programmes publiés pour les tables Android. Un succès inchangé renouvelle leur confirmation sans republication ; un échec ne vaut pas liste vide. Le compte possède un état et une validité propres et ne fournit aucun contenu au Top Shelf.

### Requirement: Clic programme publié
Un programme local non associé au suivi SHALL conserver l'ouverture par intent publié, sinon app source. Un contenu associé au suivi SHALL utiliser le même parcours de lecteur que À suivre, avec préférences partagées et restauration depuis le héro.

#### Scenario: ouverture
- **WHEN** un programme local non associé est ouvert depuis le héro
- **THEN** son intent ouvre la fiche ou le contenu selon l'app, sinon l'app est lancée

#### Scenario: progression
- **WHEN** une position de lecture locale est connue
- **THEN** le héro conserve sa barre de progression ; un statut BetaSeries seul ne crée aucune position

#### Scenario: ouverture suivie
- **WHEN** un contenu associé au suivi est ouvert depuis le héro
- **THEN** choix du lecteur, replis et observation suivent watch-tracking, avec les mêmes préférences que sa carte ; l'origine héro reste conservée

## ADDED Requirements

### Requirement: Héro du suivi
Le héro SHALL recevoir tous les contenus à regarder admissibles du fournisseur, pas seulement les 20 cartes retenues dans À suivre. Identité, épisode retenu et lecteur SHALL être partagés ; ordre, qualité d'image, préchargement, fondu et fallback SHALL suivre les contrats du héro existants.

#### Scenario: compte plus grand que la rangée
- **WHEN** le compte contient plus de 20 contenus admissibles
- **THEN** tous sont candidats au héro, chargés par pages et préparés selon les bornes existantes, sans décoder toutes leurs images à l'avance

#### Scenario: présentation et ouverture
- **WHEN** un contenu de suivi est affiché
- **THEN** les textes habituels du héro sont utilisés avec métadonnées connues ; l'en-tête identifie l'app choisie, sinon BetaSeries ; son bouton utilise le parcours commun sans prétendre qu'une offre non confirmée est disponible

#### Scenario: masquage local
- **WHEN** une série ou un film est retiré localement d'À suivre
- **THEN** les candidats BetaSeries de ce contenu quittent aussi le héro ; les programmes locaux non issus du suivi gardent leurs règles existantes

#### Scenario: source en attente ou vide
- **WHEN** le compte charge ou réussit sans contenu admissible
- **THEN** les programmes locaux restent utilisables ; sans candidat local, le fallback existe sans prolonger le démarrage pour attendre le compte

#### Scenario: panne ou déconnexion
- **WHEN** le compte devient indisponible ou est déconnecté
- **THEN** ses candidats sont retirés sans réapparition depuis le cache d'images ; le programme local correspondant peut rester, sinon le héro passe au candidat suivant ou au fallback

#### Scenario: retour et ordre
- **WHEN** le compte se rafraîchit au retour au premier plan
- **THEN** les candidats conservés ne sont pas remélangés ; ajouts et retraits suivent Ordre aléatoire du héro et l'identité stable évite un doublon du même contenu identifié

### Requirement: Indicateur d'indisponibilité du suivi
Une panne du fournisseur connecté SHALL avoir un indicateur discret donnant accès à la cause et à Réessayer ou Reconnecter, sans notification répétée à chaque retour. Il SHALL rester accessible même sans carte À suivre et disparaître après rétablissement confirmé ou déconnexion.

#### Scenario: panne partielle
- **WHEN** le compte échoue et que les programmes locaux restent disponibles
- **THEN** ils restent utilisables et l'indicateur n'occupe pas une carte de la limite de 20, ne vole pas le focus et ne remplace pas la rangée

#### Scenario: aucune carte restante
- **WHEN** la panne retire les dernières cartes du suivi
- **THEN** le repli de focus de rangée vide s'applique et la cause/réessai restent accessibles depuis les réglages de suivi, sans dépendre d'une carte disparue

#### Scenario: détail et fermeture
- **WHEN** le détail est ouvert
- **THEN** Réessayer, sinon Reconnecter a le focus ; haut/bas parcourent ses actions sans boucle, gauche/droite restent dans le panneau, Retour restitue l'origine ou son repli ; une action disparue au rétablissement restitue le contexte sans focus perdu
