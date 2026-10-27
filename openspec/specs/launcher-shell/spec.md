# Capability : launcher-shell

## Purpose
Écran home du launcher : grille d'apps, rangée Up Next, fond contextuel.

## Requirements

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

### Requirement: Focus tvOS
Le launcher SHALL animer le focus des tuiles d'apps de la grille et du dock, et de toute carte dont la spec renvoie à cette exigence, à la tvOS : la tuile focusée se soulève (zoom, légère montée, ombre portée douce, reflet blanc en diagonale), sans halo ni liseré, de façon à ce que la tuile active soit identifiable d'un coup d'œil. La tuile agrandie SHALL ne recouvrir aucune tuile voisine et SHALL n'être coupée par aucun conteneur.

#### Scenario: focus
- **WHEN** une tuile prend le focus
- **THEN** elle zoome d'environ 1,08x en montant légèrement, une ombre portée douce apparaît sous elle et un reflet blanc en diagonale éclaire son visuel ; aucun halo, coloré ou non, et aucun liseré ; transition 250-400ms courbe Apple ; en zone grille le panneau Top Shelf reflète l'app focusée

#### Scenario: rien ne déborde
- **WHEN** une tuile de la grille ou du dock a le focus
- **THEN** la tuile agrandie ne recouvre aucune tuile voisine, l'espacement entre tuiles et entre rangées absorbant le zoom, et elle reste entière : ni la rangée, ni la grille, ni le dock ne la coupent

#### Scenario: aucun nom sur les tuiles
- **WHEN** une tuile de la grille ou du dock est affichée, avec ou sans le focus
- **THEN** aucun nom d'app n'est écrit sur la tuile ni sous elle ; une app sans bannière garde le repli de « Grille d'apps » (icône entière centrée sur fond sombre)

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

### Requirement: Thème
L'UI SHALL rester sombre sur noir pur, avec posters plein cadre et police Figtree (SIL OFL, listée dans À propos). Le relief SHALL venir d'ombres douces et de reflets clairs, jamais d'un halo coloré. Les surfaces verre (dock, capsule, menu, bandeau) SHALL seules être floutées, avec une teinte sombre translucide, une bordure claire et des reflets ; les rayons et coins concentriques SHALL suivre le design system. Les scénarios ci-dessous SHALL préciser les valeurs et couples imbriqués.

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

#### Scenario: surfaces verre
- **WHEN** le dock, la capsule heure et réglages (« Capsule heure et réglages » de `settings`), le menu contextuel ou le bandeau du mode déplacement sont affichés
- **THEN** ils utilisent le matériau verre sombre (arrière-plan flouté selon les deux façons ci-dessus, teinte sombre translucide, fine bordure claire, reflet sur le bord haut, ombre légère) ; le panneau du menu contextuel est nettement plus foncé que le dock et la capsule ; si l'appareil ne supporte pas l'effet, la surface reste sombre et translucide, sans flou et sans crash

#### Scenario: bouton sans flou
- **WHEN** le bouton d'ouverture du héro est affiché au repos
- **THEN** il a un fond sombre translucide, un texte blanc et une fine bordure façon verre, sans flou d'arrière-plan ; au focus il a un fond blanc, un texte noir, un léger zoom et une ombre

#### Scenario: coût du flou
- **WHEN** une surface verre est affichée
- **THEN** son flou est soit la copie floutée du visuel calculée une fois par visuel, soit un flou en direct calculé seulement sur la zone qu'elle couvre à partir d'une image réduite de l'arrière-plan ; une surface verre hors écran n'est plus calculée (« Préchargement et mémoire »)

#### Scenario: copie floutée indisponible
- **WHEN** le visuel d'un programme est affiché mais que sa copie floutée n'a pas pu être calculée
- **THEN** le dock et la capsule utilisent le flou en direct ; le visuel net n'apparaît jamais sous leur teinte

#### Scenario: verre sur un visuel en mouvement
- **WHEN** le visuel d'un programme zoome (Ken Burns) ou change par fondu sous le dock et la capsule, ou qu'une tuile du dock prend le focus
- **THEN** le verre montre le visuel flouté qui suit exactement le mouvement et le fondu du visuel, sans qu'aucun flou soit recalculé pendant le mouvement

#### Scenario: verre sur un visuel sombre
- **WHEN** le visuel du héro est sombre sous le dock
- **THEN** le dock est sombre comme sur la maquette : sa couleur est celle du visuel voilé, flouté et saturé, recouvert de la teinte sombre translucide, jamais un gris moyen uniforme

#### Scenario: coins concentriques
- **WHEN** une surface arrondie en contient une autre (tuile dans le dock, engrenage dans la capsule, pilule ou vignette dans le menu contextuel, vignette 16:9 dans une ligne des réglages, code QR dans son fond blanc)
- **THEN** le rayon de la surface extérieure est égal au rayon de la surface intérieure plus la marge réelle entre leurs bords, avec les valeurs ci-dessus

#### Scenario: effets qui suivent le rayon
- **WHEN** une tuile prend le focus, qu'une pilule de focus s'affiche ou que le dock montre son verre précalculé
- **THEN** l'ombre et le reflet de la tuile, la pilule et le verre découpé du dock ont les mêmes coins que la surface qu'ils épousent ; aucun angle ne dépasse ni ne laisse apparaître un autre rayon

**Détails.** — Matériau Liquid Glass : Les seules surfaces floutées SHALL être dock, capsule heure/réglages, menu contextuel et bandeau de déplacement. Le verre tvOS 26 SHALL avoir un flou de 28 px saturé à 170 %, teinte rgb 22, 22, 28 à 36 % pour le dock et 38 % pour la capsule, bordure blanche de 1 px à 16 %, reflet haut à 32 %, bas à 6 %, transparence sombre et ombre légère. Cas « surface verre » : quand une surface verre est visible, elle respecte « surfaces verre » et « verre sur un visuel sombre » de « Thème » ; les autres éléments ne floutent pas leur arrière-plan. — Calcul du flou : Sur une image de programme, dock et capsule SHALL utiliser une copie réduite du visuel voilé, floutée et saturée une fois, animée avec son Ken Burns/fondu sans recalcul par frame, sinon le flou direct. Sur vidéo ou dégradé, et pour menu/bandeau, le flou SHALL être direct, à partir d'une image réduite et limité à la zone de la surface. Cas « copie ou calcul direct » : quand le média ou la disponibilité de la copie change, les scénarios « coût du flou », « copie floutée indisponible » et « verre sur un visuel en mouvement » de « Thème » restent applicables. — Rayons des surfaces : Les rayons SHALL être ceux du design system, en dp pour 960 × 540 dp : tuiles grille/dock 14, dock 24, menu 26 et pilules 12, lignes de réglages 14 ; capsule et engrenage en pilule à demi-hauteur. Ombre/reflet de tuile, focus, verre précalculé, bordure et reflets SHALL suivre le rayon de leur surface. Cas « effets concentriques » : quand une surface ou un effet de focus est dessiné, le scénario « effets qui suivent le rayon » de « Thème » s'applique sans halo coloré. — Coins concentriques : Une surface arrondie imbriquée SHALL respecter rayon extérieur = rayon intérieur + plus petite marge : dock/tuile 24 = 14 + 10 ; capsule/engrenage de 36 de haut 18 = 14 + 4 ; menu/pilule 26 = 12 + 14 ; menu/vignette marge 16 et rayon 10 ; ligne/vignette 16:9 marge verticale 10,5 et rayon 3,5. Le QR SHALL avoir un fond blanc arrondi au rayon de sa marge blanche, sans rogner ses modules carrés. Cas « imbrication » : quand une surface arrondie en contient une autre, le scénario « coins concentriques » de « Thème » vérifie ces couples, en retenant la plus petite marge si les axes diffèrent.

### Requirement: Navigation
Le launcher SHALL être navigable au DPAD uniquement.

- DPAD natif uniquement ; Home de la télécommande retourne au launcher

#### Scenario: comportement
- **WHEN** la capability est utilisée
- **THEN** les exigences listées ci-dessus s'appliquent

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
- **THEN** l'origine conserve son focus ; Haut (engrenage) puis Droite depuis le héro, ou Retour, Haut puis Droite depuis la grille atteignent le composant ; OK valide son action, Gauche, Haut ou Retour rendent l'engrenage, Droite et Bas ne font rien ; aucun quatrième palier n'est créé ; le composant reste compact en bas à droite dans la zone sûre, adapté au contenu, avec fond et bordure transparents floutés du composant de modale existant

#### Scenario: taille des modales du suivi
- **WHEN** le suivi ouvre une modale, notamment un état vide ou indisponible
- **THEN** elle réutilise le composant de modale existant et sa matière glass transparente floutée ; sa taille épouse le contenu et les actions avec les retraits du composant, sans grande réserve vide ni panneau opaque de substitution

**Détails.** — Le home reste une page continue, héro/dock puis grille, sans fondu ni saut dans la traversée, avec durée/courbe de Focus tvOS. Première entrée : titre de la première section en haut et première ligne dessous. Retour : position et élément laissés, pas une descente d'un écran imposée. La disparition d'un élément suit Repli du focus des cartes. Les contrôles du suivi n'annulent ni ces positions ni l'isolation des zones inactives.

### Requirement: Dock d'apps épinglées
Le home SHALL afficher un dock en verre sombre (« Thème », rail overlay bas) contenant au plus 6 apps épinglées qui restent dans la grille. Il SHALL se déplacer avec le héro et n'être visible qu'avec lui. Le verre SHALL rester actif pendant le défilement jusqu'à la sortie complète du héro, sans changement d'aspect au début de la descente ni recréation du contenu lors de sa bascule.

#### Scenario: état initial
- **WHEN** le home s'ouvre
- **THEN** le dock est visible en overlay bas sur le héro, le focus est sur le héro

#### Scenario: taille tvOS
- **WHEN** le dock contient une ou plusieurs apps
- **THEN** toutes ses tuiles ont la même taille fixe, plus petite que celle des tuiles de la grille (environ 80 %), quel que soit le nombre d'apps, et le dock, centré, s'ajuste à leur nombre

#### Scenario: tuiles adaptatives
- **WHEN** le dock contient 6 apps, son maximum
- **THEN** ses tuiles gardent la taille fixe du scénario « taille tvOS », sans réduction, et le dock reste centré et entièrement dans la largeur de l'écran

#### Scenario: épinglage
- **WHEN** l'utilisateur épingle ou retire une app via le menu contextuel (appui long, OK valide directement)
- **THEN** le dock est mis à jour et l'app reste dans la grille

#### Scenario: solidaire du héro
- **WHEN** la page défile vers la grille ou revient vers le héro (« Navigation 3 paliers »)
- **THEN** le dock suit exactement le mouvement du héro, sans fondu propre ni décalage ; en vue grille il est entièrement hors écran et son matériau verre n'est plus calculé (« Préchargement et mémoire »)

#### Scenario: verre du dock pendant le défilement
- **WHEN** la page défile du dock vers la grille, puis de la grille vers le dock
- **THEN** à la descente, le verre du dock reste actif et identique tant que le héro est au moins en partie à l'écran, et n'est coupé qu'une fois le héro entièrement sorti ; à la remontée, il redevient actif dès que le héro réapparaît ; à aucune de ces bascules les tuiles du dock ne sont recréées (le focus et l'état des tuiles sont conservés)

#### Scenario: dock plein
- **WHEN** le dock contient déjà 6 apps et l'utilisateur ouvre le menu contextuel d'une app qui n'y est pas
- **THEN** l'action « Épingler au dock » est indisponible : grisée, accompagnée d'un message court indiquant que le dock est plein, et OK n'y fait rien ; elle redevient disponible dès qu'une app est retirée du dock

#### Scenario: épinglages au-delà du maximum
- **WHEN** le launcher démarre avec plus de 6 apps épinglées enregistrées (version antérieure)
- **THEN** le dock n'en affiche que 6 et aucun épinglage enregistré n'est effacé

### Requirement: Persistance de la grille
L'ordre des apps et les épinglages SHALL être persistés localement (DataStore) et restaurés au démarrage ; les apps nouvellement installées s'ajoutent à la fin par ordre alphabétique.

#### Scenario: réorganisation
- **WHEN** l'utilisateur choisit « Déplacer » sur une tuile de la grille puis presse les flèches (gauche/droite : une case, haut/bas : une rangée, par insertion), OK pour valider
- **THEN** la tuile suit le focus avec un liseré de déplacement, un rappel des touches s'affiche, l'ordre est persisté et conservé après redémarrage

#### Scenario: annulation
- **WHEN** l'utilisateur presse Retour pendant un déplacement
- **THEN** l'ordre d'avant le déplacement est restauré

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
- **THEN** le provider est relu au plus tard toutes les 15 minutes au premier plan, jamais en arrière-plan ; le retour ne déclenche une relecture que si un changement a été signalé pendant l'arrêt ou si la relecture périodique est due (« Préchargement et mémoire », scénario « relecture au retour au premier plan ») ; le compte suit sa propre source partagée

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

### Requirement: Panneau Top Shelf au focus
Le panneau SHALL s'ouvrir à la demande au-dessus de la rangée focusée, se fermer si son app n'a rien à montrer et ne réserver aucune place fermé. Le bloc focusé et son défilement SHALL suivre « Bloc focusé de la grille » et « Placement du bloc focusé », sans révéler le héro tant que la grille est active. Les images SHALL être prêtes et valides selon « Contenu du panneau ».

#### Scenario: arrivée dans la grille
- **WHEN** l'utilisateur descend du dock vers la grille
- **THEN** la grille occupe tout l'écran, sans emplacement réservé ni panneau ; à la première entrée, le haut du titre de la première section est sur la marge haute et sa première ligne est juste en dessous

#### Scenario: insertion
- **WHEN** le focus reste environ 3 s sur une tuile dont l'app a des visuels validés et chargés encore valides
- **THEN** le panneau s'insère au-dessus de la rangée focusée (la rangée précédente reste au-dessus du panneau), avec une animation d'expansion ; la rangée focusée reste immobile à l'écran et les rangées précédentes remontent pour laisser la place au panneau

#### Scenario: insertion près du haut
- **WHEN** le panneau s'ouvre au-dessus d'une rangée trop proche du haut pour qu'il tienne sous la marge haute sans déplacer cette rangée
- **THEN** le haut du panneau s'aligne sur la marge haute et la rangée focusée descend juste ce qu'il faut pour lui laisser la place

#### Scenario: déplacement du focus
- **WHEN** un panneau est déjà ouvert et le focus passe à une autre tuile dont l'app a des visuels validés et chargés encore valides
- **THEN** le panneau montre immédiatement les visuels de la nouvelle app, sans délai ni fermeture intermédiaire, et se replace au-dessus de la nouvelle rangée focusée

#### Scenario: fermeture
- **WHEN** le focus passe sur une tuile dont l'app n'a aucun visuel validé et chargé encore valide
- **THEN** le panneau se referme et la grille reprend toute la place ; la rangée focusée reste immobile à l'écran, sauf près du début de la grille, quand il ne reste plus de quoi défiler : la grille revient alors à sa position de départ

#### Scenario: focus toujours visible
- **WHEN** le panneau s'ouvre, se ferme ou change de rangée, ou que le focus change de rangée
- **THEN** la tuile focusée reste entièrement visible entre les marges, selon les scénarios de cette exigence ; la grille ne défile pas davantage

#### Scenario: déplacement dans une rangée
- **WHEN** le focus passe à gauche ou à droite dans la même rangée, sans ouverture ni fermeture du panneau
- **THEN** la grille ne défile pas

#### Scenario: changement de rangée
- **WHEN** le focus passe sur une autre rangée
- **THEN** la grille ne défile pas si le bloc focusé est déjà entre les marges ; sinon elle défile juste ce qu'il faut pour l'y ramener : bas de la rangée sur la marge basse en descendant, haut du bloc sur la marge haute en montant

#### Scenario: titre de section dans le bloc
- **WHEN** le focus arrive sur la première ligne d'une section (rangée Up Next, ou première rangée d'apps), en montant ou en descendant
- **THEN** le titre de cette section fait partie du bloc focusé : il est entièrement visible au-dessus de la ligne, entre les marges, la grille ne défilant que si le bloc n'y est pas déjà

#### Scenario: panneau sous le titre de section
- **WHEN** le panneau s'ouvre au-dessus de la première rangée d'apps alors que le titre « Applications » fait partie du bloc focusé
- **THEN** le bloc est, de haut en bas, le titre « Applications », le panneau puis la rangée ; quand le bloc doit s'aligner sur la marge haute, c'est le haut du titre qui s'y place et la section précédente sort par le haut

#### Scenario: bloc trop grand
- **WHEN** le bloc focusé ne tient pas entre les deux marges
- **THEN** le haut du bloc (le titre de section quand il en fait partie, sinon le haut du panneau s'il est ouvert) s'aligne sur la marge haute

#### Scenario: sortie et retour dans la grille
- **WHEN** l'utilisateur quitte la zone grille (Retour, ou remontée vers le dock ou le héro) puis y revient
- **THEN** la page défile jusqu'au héro (« Navigation 3 paliers »), mais la position de la grille n'est pas remise à zéro : au retour, la page redescend jusqu'à la position de la grille laissée à la sortie, la ligne focusée à la même place à l'écran (le panneau éventuellement ouvert s'est refermé à la sortie), le focus va au dernier élément visité, carte Up Next ou tuile (« Navigation 3 paliers »), et la grille ne défile davantage que si cet élément est hors des marges

**Détails.** — Bloc focusé de la grille : Le bloc focusé SHALL comprendre la ligne focusée (apps ou Up Next), son titre si c'est la première ligne de sa section, et le panneau ouvert au-dessus. L'ordre SHALL rester titre, panneau, ligne selon la maquette 4.2 ; le panneau reste immédiatement au-dessus de la rangée, sous le titre. Cas « première ligne de section » : quand le panneau s'ouvre sur la première rangée d'apps, « titre de section dans le bloc » et « panneau sous le titre de section » de « Panneau Top Shelf au focus » déterminent le placement. — Placement du bloc focusé : Le bloc SHALL rester entre les marges haute de 40 dp et basse de 32 dp (espacement des rangées). La grille SHALL défiler seulement pour le ramener entre elles, avec durée/courbe de l'expansion du panneau. En grille active, elle ne SHALL jamais remonter avant le haut de son premier titre : le héro reste hors écran. Cas « bloc déplacé ou trop grand » : quand le focus ou le panneau change de rangée, ou que le bloc dépasse la hauteur disponible, les scénarios de placement de « Panneau Top Shelf au focus » restent applicables, sans défilement supplémentaire.

### Requirement: Contenu du panneau
Le panneau SHALL n'afficher que des visuels validés, chargés et non expirés de programmes valides de l'app focusée, et ne pas s'ouvrir sinon. Validation, préchargement et affichage SHALL utiliser « Chargement partagé des images » et la politique Top Shelf de « Qualité des visuels ».

#### Scenario: contenu disponible
- **WHEN** l'app focusée a des visuels validés (preview programs / watch next)
- **THEN** chaque affiche apparaît en fondu (300-400ms easing Apple) une fois chargée, puis défile lentement une seule fois (Ken Burns, un seul passage) et reste ensuite immobile sur sa dernière position ; le défilement repart du début à chaque nouvelle affiche ; il n'a lieu que si « Mouvement des visuels » est activé et que les animations du système ne sont pas coupées

#### Scenario: mouvement des visuels désactivé
- **WHEN** « Mouvement des visuels » est désactivé ou que l'échelle d'animation du système est à 0
- **THEN** chaque affiche apparaît en fondu puis reste immobile, cadrée comme au début du défilement ; le changement d'affiche et son fondu sont inchangés, et entre deux affiches le panneau ne redessine aucune image

#### Scenario: pas de contenu
- **WHEN** l'app focusée n'a aucun visuel validé
- **THEN** aucun panneau n'est ouvert (ni cadre, ni logo de repli) et la grille occupe tout l'écran

#### Scenario: affiche retirée
- **WHEN** le panneau est ouvert et que son app retire le programme d'une affiche affichée ou en rotation
- **THEN** l'affiche quitte le panneau à la relecture suivante ; le panneau se referme s'il ne lui reste aucune affiche validée et prête

#### Scenario: image en cours ou en échec
- **WHEN** une affiche est en cours de chargement, refusée, absente ou dépasse le délai de récupération
- **THEN** le panneau utilise une autre affiche prête de cette app, sinon reste fermé ou se ferme ; le chargement n'empêche pas de focaliser une autre app et ne produit pas de cadre vide

#### Scenario: changement d'app pendant une panne
- **WHEN** le focus quitte une app avec un panneau ouvert pour une app sans image prête
- **THEN** les images de la première ne servent pas de repli à la seconde ; le panneau se ferme et pourra s'ouvrir quand une image de la nouvelle app sera prête selon « Panneau Top Shelf au focus »

#### Scenario: expiration et validation inachevée
- **WHEN** une image ou son programme expire pendant que le panneau est ouvert et que d'autres validations de l'app sont encore en cours
- **THEN** l'affiche expirée quitte le panneau sans attendre le lot ; une autre affiche prête est montrée, sinon le panneau se ferme ; le focus reste sur la tuile et les règles de placement existantes s'appliquent

#### Scenario: permission perdue ou source absente
- **WHEN** la permission de lecture est perdue ou qu'aucun programme valide de l'app ne peut être obtenu
- **THEN** aucun panneau ne montre ses anciens programmes ; la grille reste navigable sans nouvelle demande de permission déclenchée par le panneau

#### Scenario: reprise au prochain focus
- **WHEN** une image a échoué temporairement et que l'app reçoit de nouveau un focus prolongé après la temporisation de reprise
- **THEN** sa récupération est réessayée par le parcours partagé, sans blacklist locale ni doublon avec une demande du héro ou d'À suivre

### Requirement: Fond de la zone grille
Dans la grille, le fond SHALL être un dégradé neutre uni type tvOS, le héro étant complètement masqué parce qu'il est sorti de l'écran par le haut.

#### Scenario: révélation de la grille
- **WHEN** l'utilisateur passe en zone grille
- **THEN** à la fin du défilement le héro est entièrement hors écran (aucun pixel du héro, du dock ni de la capsule heure et réglages n'est visible), le fond est le dégradé neutre (pas d'aerial, pas de posters en fond de grille), sans fondu de couches

#### Scenario: pendant le défilement
- **WHEN** la page est entre le héro et la grille
- **THEN** le héro et la grille sont chacun visibles pour la part de l'écran qu'ils occupent, sans zone noire ni image figée entre les deux ; le dégradé neutre est le fond de la partie grille dès le début du mouvement

#### Scenario: héro hors écran
- **WHEN** le héro est entièrement hors écran
- **THEN** sa lecture est interrompue et ses animations arrêtées conformément à « Préchargement et mémoire » (scénario « économie de ressources ») et « Fond vidéo de secours » ; son lecteur vidéo n'est pas libéré

### Requirement: Diaporama héro
Le héro SHALL enchaîner automatiquement les programmes publiés, un seul à la fois, plein écran et muet, avec un bouton d'ouverture focusable ; en-tête, infos, progression et bouton SHALL correspondre au programme affiché et changer avec son média. Un programme SHALL y entrer quand sa vidéo d'aperçu ou un visuel accepté par la politique du héro est prêt, ou sur son média de secours quand ses images ont échoué ; un programme sans visuel acceptable SHALL en être écarté.

#### Scenario: défilement automatique
- **WHEN** plusieurs programmes sont disponibles
- **THEN** le héro passe au programme suivant toutes les 12 s environ par fondu croisé (1,4 s, easing Apple : le nouveau visuel apparaît par-dessus l'ancien, sans assombrissement intermédiaire), le visuel courant zoome lentement une seule fois (Ken Burns, un seul passage d'environ 10 s, achevé avant le changement de visuel : au moins 2 s d'image immobile par cycle) puis reste immobile sur sa dernière position, le zoom repartant du début à chaque nouveau visuel, sans son, sans aperçu de l'élément suivant ; le zoom n'a lieu que si « Mouvement des visuels » est activé et que les animations du système ne sont pas coupées

#### Scenario: mouvement des visuels désactivé
- **WHEN** « Mouvement des visuels » est désactivé ou que l'échelle d'animation du système est à 0
- **THEN** chaque visuel du héro est affiché immobile à sa taille d'origine, sans Ken Burns ; le fondu croisé de 1,4 s, le défilement toutes les 12 s environ, les textes et les vidéos d'aperçu sont inchangés, et entre deux changements de programme aucune image n'est redessinée tant qu'aucune vidéo ne joue

#### Scenario: navigation manuelle
- **WHEN** l'utilisateur presse gauche/droite sur le héro
- **THEN** le héro passe au programme précédent/suivant avec le même fondu et le minuteur d'avance automatique repart

#### Scenario: vidéo d'aperçu
- **WHEN** le programme expose une vidéo d'aperçu (previewVideoUri)
- **THEN** elle est lue plein écran en muet à la place du poster, repli sur le poster si la lecture échoue, puis sur le média de secours si aucune image n'est utilisable

#### Scenario: métadonnées
- **WHEN** un programme ouvrable est affiché
- **THEN** en bas à gauche apparaissent, de haut en bas : l'en-tête (icône et libellé de l'app source), le titre, la ligne d'infos, la barre de progression suivie du temps restant, chacun seulement s'il a quelque chose à montrer, puis un bouton « Ouvrir » (« Reprendre » si progression) qui porte le focus du héro, au style de « Thème » (scénario « bouton sans flou ») ; OK sur ce bouton ouvre le contenu

#### Scenario: lisibilité sur un poster clair
- **WHEN** le visuel affiché est clair, jusqu'au blanc (pire cas)
- **THEN** un voile sombre en dégradé depuis le bas et depuis la gauche passe derrière le titre, les métadonnées et le bouton, et un léger voile radial assombrit le coin haut droit derrière la capsule heure et réglages ; le texte blanc et le bouton restent lisibles, le reste du visuel n'est pas voilé

#### Scenario: sans progression
- **WHEN** le programme affiché n'expose pas de progression
- **THEN** aucun espace vide ne sépare les métadonnées du bouton : l'écart entre la dernière ligne de métadonnées et le bouton est le même que l'écart entre la barre de progression et le bouton quand elle est présente, et un titre d'une ligne ne réserve pas de place pour une seconde ligne

#### Scenario: retour au programme précédent pendant le fondu
- **WHEN** l'utilisateur revient au programme précédent avant la fin du fondu croisé
- **THEN** le fondu repart de l'opacité courante vers le programme précédent, sans coupure ni saut d'image

#### Scenario: voiles pendant le Ken Burns
- **WHEN** le visuel zoome (Ken Burns)
- **THEN** le voile du coin haut droit derrière la capsule reste fixe et garde son opacité ; les voiles du bas et de la gauche, intégrés au visuel, ne s'affaiblissent pas de façon visible

#### Scenario: changement de programme
- **WHEN** le héro passe d'un programme à un autre
- **THEN** le fondu des médias et celui des textes commencent ensemble, quand l'image, la première image vidéo ou le fond de secours du nouveau programme est prêt ; jusque-là, le titre, les infos et le bouton restent ceux du programme affiché ; les textes de l'ancien programme s'effacent pendant la première moitié du fondu, puis ceux du nouveau apparaissent pendant la seconde : jamais deux titres superposés ; le bas des textes et le bouton ne bougent pas, quel que soit le nombre de lignes des deux titres

#### Scenario: programme en cours
- **WHEN** le programme affiché est un watch next de type `CONTINUE` publié par l'app « Appli » avec son icône, saison 2, épisode 5, durée 42 min, position 17 min
- **THEN** l'en-tête montre l'icône et « Continuer dans Appli », la ligne d'infos « Saison 2 · Épisode 5 · 42 min », la barre de progression est suivie de « Reste 25 min » et le bouton est « Reprendre »

#### Scenario: épisode suivant, nouveauté, liste de lecture
- **WHEN** le programme affiché est un watch next de type `NEXT`, `NEW` ou `WATCHLIST` de l'app « Appli »
- **THEN** l'en-tête montre respectivement « Épisode suivant dans Appli », « Nouveau dans Appli » ou « À regarder dans Appli »

#### Scenario: programme mis en avant par l'app
- **WHEN** le programme affiché est un preview program de l'app « Appli »
- **THEN** l'en-tête montre l'icône et « Appli » seul

#### Scenario: type absent ou inconnu
- **WHEN** le programme affiché est un watch next sans `watch_next_type` ou avec une valeur hors des quatre types d'Android
- **THEN** l'en-tête montre l'icône et le nom de l'app seuls, comme un programme mis en avant ; le rattachement de ces programmes à un groupe de tri dans Up Next (« Champs facultatifs » de `up-next`) ne change pas ce libellé

#### Scenario: nom ou icône de l'app manquants
- **WHEN** le nom de l'app source est inconnu, ou son icône ne peut pas être chargée
- **THEN** seul ce qui est disponible est affiché : l'icône avec le type seul (« Continuer », « Épisode suivant », « Nouveau », « À regarder ») ou sans libellé pour un programme mis en avant, ou le libellé sans icône ; sans icône ni libellé, l'en-tête disparaît, sans espace vide

#### Scenario: infos partielles
- **WHEN** l'app ne publie qu'une partie de la saison, de l'épisode et de la durée
- **THEN** la ligne d'infos ne montre que ces éléments (par exemple « Épisode 3 » ou « 1 h 35 min ») ; sans aucun d'eux, il n'y a pas de ligne d'infos et le titre est directement suivi de la progression ou du bouton

#### Scenario: progression sans position ou sans durée
- **WHEN** la position de lecture ou la durée n'est pas publiée
- **THEN** ni barre de progression ni temps restant ne sont affichés, sans espace vide (scénario « sans progression »)

#### Scenario: textes avec le visuel
- **WHEN** l'image du programme suivant met 3 s à charger puis réussit
- **THEN** pendant ces 3 s, le titre, les infos et le bouton restent ceux du programme affiché et OK ouvre ce programme ; ils ne changent qu'avec le fondu vers le nouveau visuel

#### Scenario: visuel lent
- **WHEN** l'image d'un programme n'est pas encore prête, en attente de son tour ou en cours de récupération
- **THEN** le défilement et la navigation passent ce programme sans l'attendre ; une seule récupération commune continue et le programme entre dans le diaporama dès que son image est prête, ou avec son repli si la requête échoue

#### Scenario: programme sans visuel acceptable
- **WHEN** un programme sans vidéo ne publie que des candidates refusées par la politique du héro (portrait seul, trop petites) ou reconnues comme images génériques
- **THEN** il n'apparaît pas dans le héro, sans repli nature ; il reste dans le Top Shelf et À suivre si leurs politiques l'acceptent ; sans autre programme, le fallback global s'affiche

#### Scenario: textes immobiles pendant le Ken Burns
- **WHEN** le visuel du programme zoome (Ken Burns)
- **THEN** l'en-tête, le titre, la ligne d'infos et la progression ne sont pas recomposés

#### Scenario: image indisponible et repli nature
- **WHEN** la récupération de toutes les candidates du programme échoue (erreur ou délai de requête dépassé) et qu'aucune copie locale n'est utilisable
- **THEN** le programme est affiché sur la vidéo nature de secours avec ses propres textes et sa propre cible d'ouverture ; le défilement continue vers les programmes des autres apps et les flèches restent actives

#### Scenario: programmes en repli consécutifs
- **WHEN** plusieurs programmes affichés à la suite utilisent la vidéo nature
- **THEN** le même clip continue sans redémarrer ni se recharger pendant que leurs textes changent toutes les 12 s environ ; à la fin du clip, le clip suivant de la playlist prend le relais sans changer de programme ; quand un programme avec image ou vidéo s'affiche, le clip est arrêté et repart du début au repli suivant

#### Scenario: repli sans réseau
- **WHEN** l'image du programme et les clips nature sont indisponibles
- **THEN** le fond neutre local porte les textes et le bouton du programme, sans écran noir ni attente indéfinie ; le chargement de la vidéo de secours ne retarde pas la navigation

**Détails.** — En-tête du héro : L'en-tête SHALL précéder le titre selon la maquette 960 × 540 dp : icône carrée sans masque de 18 dp, coins 4,5 dp, puis libellé par type publié. Pour l'app X : CONTINUE (`WATCH_NEXT_TYPE_CONTINUE`) « Continuer dans X », NEXT « Épisode suivant dans X », NEW « Nouveau dans X », WATCHLIST « À regarder dans X », preview ou type absent/inconnu « X ». Il SHALL ne montrer que les éléments disponibles. Cas « données manquantes de l'en-tête » : quand le nom ou l'icône manque, le type seul ou le libellé sans icône est montré, et sans les deux l'en-tête disparaît ; les scénarios de types et de nom/icône manquants de « Diaporama héro » s'appliquent. — Infos et progression du héro : La ligne sous le titre SHALL réunir seulement saison, épisode et durée publiés, séparés par « · ». La durée SHALL être arrondie à la minute (minimum 1 min), en heures au-delà d'une heure (« 1 h 35 min », « 2 h »). Barre et « Reste X min » SHALL exister seulement avec position et durée connues ; le restant est arrondi à la minute supérieure avec la même écriture des heures. Cas « infos partielles » : quand les données du programme sont partielles, « infos partielles », « progression sans position ou sans durée » et « programme en cours » de « Diaporama héro » s'appliquent sans réserver d'espace aux lignes absentes. — Synchronisation du héro : Titre, en-tête, infos et bouton SHALL correspondre au programme affiché, même sur son média de secours ; « Ouvrir »/« Reprendre » SHALL ouvrir ce programme et changer avec son média, jamais avant. Les textes SHALL rester fixes sans recomposition ni dessin pendant le Ken Burns, sans espace pour les lignes absentes et lisibles sur visuel clair grâce aux voiles. Cas « changement et animation » : quand le média change ou zoome, « changement de programme », « textes avec le visuel », « textes immobiles pendant le Ken Burns » et « lisibilité sur un poster clair » de « Diaporama héro » s'appliquent aussi au média de secours, sans image ou cible d'un autre programme. — Visuel du héro en attente : Un programme dont l'image n'est pas prête SHALL être passé sans attente ni délai propre au héro, sans bloquer navigation ni médias prêts ; à l'échec de la requête, « Repli du programme du héro » SHALL s'appliquer. Une vidéo suivante sans première image après 1 s SHALL rester préparée en pause sans doublon tant qu'une autre n'est pas demandée, et laisser passer le prochain média utilisable ; prête, elle redevient éligible. La première vidéo sans média à remplacer reste exemptée. Cas « préparation lente » : quand le visuel suivant n'est pas prêt, sa récupération commune continue et le programme est passé jusqu'à ce que l'image soit prête ou que la requête échoue ; une vidéo suit son délai de 1 s ; une image préparée n'est pas retenue en bitmap par le héro et est relue depuis le cache à son retour.

### Requirement: Fond vidéo de secours
Le fallback du héro SHALL être une liste de clips nature libres de droits (Pexels, 2K sinon 1080p, mp4 H.264 en https) lus par un unique lecteur, muet : chaque clip jusqu'à sa fin, puis le suivant, la playlist bouclant. Il SHALL pouvoir servir de média de secours d'un programme sans image utilisable sans remplacer l'identité de ce programme ; le clip ne dépend pas du programme affiché.

#### Scenario: lecture
- **WHEN** le fallback est actif
- **THEN** un seul lecteur lit chaque clip jusqu'à sa fin puis le suivant, la playlist bouclant, muet ; un clip en erreur est ignoré au profit du suivant ; un clip déjà lu est relu depuis « Cache disque des vidéos du héro » ; le lecteur est mis en pause quand la grille est active et libéré quand le héro est démonté

#### Scenario: aucun clip lisible
- **WHEN** aucun clip ne peut être lu (réseau absent, URL morte)
- **THEN** le dégradé sombre animé reste affiché, sans écran noir ni crash ; les tentatives et la préparation des clips ont un délai borné qui ne bloque pas le défilement

#### Scenario: vidéo de secours d'un programme
- **WHEN** un programme valide utilise une vidéo nature comme média de secours
- **THEN** ses textes et sa cible d'ouverture restent ceux du programme ; sans programme valide, le fallback global n'affiche aucun ancien titre ni bouton vers un contenu expiré

### Requirement: Qualité des visuels
Le héro SHALL privilégier la vidéo d'aperçu, large d'au moins 1080 px ; ses images SHALL rester paysage, au moins 640 px après réduction dans 1920 × 1080 sans agrandissement. Le panneau SHALL suivre « Qualité des affiches Top Shelf ». Les dimensions communes ne SHALL pas imposer un verdict global ; un rejet mène aux candidates puis au repli du héro ou à l'exclusion de l'affiche, sans abaisser les seuils du héro.

#### Scenario: image trop petite
- **WHEN** une image du programme ne satisfait pas la politique de dimensions de sa destination, est portrait ou ne peut pas être chargée
- **THEN** cette image n'est pas affichée dans le héro ou le panneau concernés ; une autre candidate est essayée, puis le héro utilise son repli et le panneau écarte l'affiche si aucune candidate ne convient

#### Scenario: variante YouTube
- **WHEN** l'image d'un programme est une miniature YouTube (`i.ytimg.com`, `iN.ytimg.com` ou `img.youtube.com`, chemin `/vi/<id>/` ou `/vi_webp/<id>/`)
- **THEN** la variante `https://i.ytimg.com/vi/<id>/maxresdefault.jpg` est vérifiée d'abord pour le héro et le panneau ; acceptée selon la politique de destination, c'est elle qui s'affiche ; absente ou insuffisante, les candidates publiées sont vérifiées ; un échec réseau laisse la récupération à revérifier selon les règles communes

#### Scenario: vidéo trop petite
- **WHEN** la vidéo d'aperçu d'un programme fait moins de 1080 px de large
- **THEN** le héro replie sur les candidates image du programme, puis sur son média de secours si aucune ne convient

#### Scenario: seuil du panneau
- **WHEN** une image paysage a une largeur réelle de 640 px
- **THEN** elle est acceptée pour le panneau et décodée à sa taille ; une image de 639 px ne l'est pas, même si elle satisfait la politique d'une carte À suivre

### Requirement: Sélection des apps sources
Le launcher SHALL permettre, dans ses réglages, de cocher les apps dont les programmes alimentent le héro, le Top Shelf et la rangée Up Next ; par défaut toutes les apps installées sont retenues. L'UI de ce réglage est spécifiée par « Apps sources » de la capability settings.

#### Scenario: app décochée
- **WHEN** l'utilisateur décoche une app dans les réglages du launcher
- **THEN** ses programmes n'apparaissent plus dans le héro, le Top Shelf ni la rangée Up Next, sans redémarrage

### Requirement: Préchargement et mémoire
Le launcher SHALL borner la validation : premiers visuels du héro au chargement, affiches d'une app au focus posé. Il SHALL décoder à la destination, jamais à la dalle, et garder « Fluidité de release »/« Fenêtre de composition de la grille ». Récupérations, dimensions et échéances SHALL être partagées entre les trois zones selon « Chargement partagé des images » et « Cache borné et fraîcheur des images ».

#### Scenario: validation au chargement
- **WHEN** les programmes sont chargés
- **THEN** seuls les premiers visuels du héro sont vérifiés en arrière-plan, par files d'apps selon le contrat partagé, d'après les dimensions conservées avec les octets vérifiés, sans décoder à la taille d'affichage ; les octets vérifiés restent en cache disque, aucun visuel n'est préchargé en mémoire ; le provider pouvant publier des centaines de programmes, aucun autre lot n'est validé à ce moment

#### Scenario: validation à la demande
- **WHEN** le focus reste au moins 0,5 s sur une tuile de la grille
- **THEN** quelques affiches candidates de cette app sont vérifiées selon la politique Top Shelf ; les octets et dimensions connus sont réutilisés tant qu'ils restent valides, seules les affiches acceptées alimentent son panneau et un focus qui ne fait que passer ne lance aucune vérification

#### Scenario: validation interrompue ou nouvelles affiches
- **WHEN** la vérification des affiches d'une app a été interrompue (focus posé sur une autre app avant la fin), ou les programmes rechargés proposent d'autres affiches candidates pour cette app
- **THEN** au prochain focus prolongé sur l'app, les candidates encore inconnues ou à réessayer sont traitées ; les résultats de version encore valides et les récupérations encore demandées par une autre zone sont partagés, sans nouvelle ouverture identique

#### Scenario: visuel injoignable
- **WHEN** la validation ou le chargement d'une image d'une des trois zones échoue pour une raison temporaire, notamment erreur d'entrée-sortie, HTTP 5xx ou délai de fournisseur
- **THEN** une copie locale encore utilisable sert de secours, sinon le repli de la zone s'affiche ; la demande peut être réessayée à une relecture pertinente, au retour au premier plan ou au prochain focus prolongé, avec une temporisation commune et sans requête concurrente dupliquée

#### Scenario: visuel rejeté
- **WHEN** une version d'image est trop étroite pour un usage, refusée par le serveur ou impossible à décoder
- **THEN** le rejet est conservé pour cette version et cette politique sans boucle de requêtes ; les autres usages et candidates restent possibles, une nouvelle publication ou une revalidation à l'échéance permet une nouvelle tentative, et une URI expirée n'est jamais reconstruite

#### Scenario: économie de ressources
- **WHEN** le héro est masqué à l'intérieur du launcher (zone grille, page de réglages) ou une surface verre est invisible
- **THEN** la lecture de la vidéo est interrompue (arrêtée, ou mise en pause à sa position quand une exigence le prévoit) sans que le lecteur vidéo soit libéré, le launcher étant au premier plan ; les animations de fond, le Ken Burns du visuel du héro et le flou d'arrière-plan sont arrêtés ; les images sont décodées avec au plus deux décodeurs simultanés

#### Scenario: launcher en arrière-plan
- **WHEN** une autre app passe au premier plan et le launcher n'est plus visible
- **THEN** le lecteur vidéo du héro est libéré, ce qui rend le décodeur vidéo à l'app au premier plan ; le défilement automatique du diaporama et les délais du héro (visuel lent, démarrage d'une vidéo) sont suspendus ; aucun nouveau lot de prélecture n'est démarré, les récupérations déjà ouvertes gardent leur échéance physique et les TTL ne sont pas suspendus

#### Scenario: retour au premier plan après libération
- **WHEN** le launcher revient au premier plan après avoir libéré le lecteur vidéo
- **THEN** la vidéo d'aperçu du programme affiché encore valide est rechargée et reprend, sans compter comme un échec ni faire passer le héro au programme suivant ; les images et programmes sont contrôlés selon leurs échéances avant réaffichage, sans effacer ni recharger les images encore valides, le défilement automatique et les délais de présentation repartent de zéro ; une vidéo lente dont la préparation a été interrompue redevient éligible au diaporama

#### Scenario: écran au repos
- **WHEN** aucune touche n'est pressée et aucune transition n'est en cours
- **THEN** seules les animations spécifiées encore visibles redessinent l'écran (vidéo d'aperçu, passage unique du dégradé animé du repli, passage unique du Ken Burns d'un nouveau visuel du héro ou du panneau Top Shelf, changement de visuel du défilement automatique) ; une fois ces passages terminés, l'image est immobile ; aucune animation d'un élément hors écran ne tourne, et une animation continue ne provoque aucune recomposition à chaque image ; les contrôles d'expiration se déclenchent aux échéances utiles, pas par frame ; avec « Mouvement des visuels » désactivé, un visuel fixe du héro ou du panneau ne redessine rien entre deux changements de visuel ; le verre du dock et de la capsule suit le fondu de son visuel mais n'est pas redessiné par le zoom

#### Scenario: taille de décodage
- **WHEN** un visuel du héro, une affiche du panneau Top Shelf ou une bannière d'app est décodé, même publié en 3840 × 2160
- **THEN** le visuel du héro est décodé à la taille de la fenêtre, au plus 1920 × 1080, l'affiche à la taille du panneau (en le couvrant), et la bannière à la taille de la tuile de la grille au zoom de focus, en pixels de l'appareil, au plus 480 × 270

#### Scenario: une passe par image
- **WHEN** le héro affiche le visuel d'un programme, au repos, pendant le Ken Burns ou un fondu
- **THEN** les voiles du bas et de la gauche sont intégrés au visuel décodé et le voile du coin haut droit est dessiné seul sur sa petite zone ; ni fond de fenêtre, ni fond noir, ni dégradé du repli, ni voile n'est dessiné sous un visuel opaque, et aucun fondu ne passe par un calque plein écran hors écran

#### Scenario: affiches du héro hors cache mémoire
- **WHEN** une affiche du héro est préparée puis affichée
- **THEN** elle est lue depuis le cache disque partagé et n'est pas conservée dans le cache mémoire des images ; seuls les calques affichés par le héro la retiennent, et un retour à un programme déjà vu la relit depuis le disque

#### Scenario: relecture au retour au premier plan
- **WHEN** le launcher revient au premier plan
- **THEN** les programmes du héro et d'À suivre ne sont relus que si le TV Provider a signalé un changement pendant l'arrêt ou si le délai de relecture du héro est échu ; un même changement ne provoque qu'une lecture d'À suivre ; les compteurs de programmes des réglages ne sont lus que réglages ouverts ; les échéances des images et programmes restent contrôlées comme en « retour au premier plan après libération »

#### Scenario: aperçu sans audio
- **WHEN** une vidéo d'aperçu ou une vidéo nature joue dans le héro
- **THEN** aucune piste audio n'est décodée et aucune sortie audio n'est ouverte par le launcher

#### Scenario: vidéo nature à la taille de l'écran
- **WHEN** le héro joue une vidéo nature
- **THEN** la version lue est en 1920 × 1080 au plus ; sous une vidéo dont la première image est rendue et le fondu terminé, le dégradé de repli n'est plus dessiné et la vidéo est composée sans transparence

#### Scenario: affiches du panneau décodées une à une
- **WHEN** les affiches candidates d'une app sont validées au focus posé
- **THEN** elles sont vérifiées par leur en-tête sans être décodées à la taille du panneau ; seule l'affiche affichée, puis la suivante au changement, est décodée à la taille du panneau ; un focus qui traverse une tuile en moins de 0,5 s ne décode aucune affiche

#### Scenario: écran de démarrage
- **WHEN** l'écran de démarrage est affiché
- **THEN** seuls le catalogue et les premiers candidats bornés du héro sont préparés en priorité ; après le premier visuel utilisable, aucune nouvelle validation de prélecture n'est démarrée avant la fin de l'écran de démarrage ; les travaux déjà ouverts restent bornés par le parallélisme partagé, les bannières sont décodées sur un fil de basse priorité et la grille n'est composée qu'après le fondu vers l'accueil

#### Scenario: navigation dans la grille
- **WHEN** l'utilisateur déplace le focus de tuile en tuile et de rangée en rangée dans la grille
- **THEN** seules les tuiles qui prennent ou perdent le focus et la rangée concernée sont recomposées, l'animation du focus ne recompose rien, et la validation des images reste commune et sans effet sur les autres rangées

#### Scenario: grande grille
- **WHEN** le catalogue compte plus de 12 rangées
- **THEN** le nombre de tuiles composées dépend de la hauteur de l'écran et de la prélecture, pas du nombre d'apps ; les bannières des rangées composées sont lues dans le cache borné des visuels d'apps ou décodées sur le fil de basse priorité, celles des rangées hors fenêtre ne sont ni composées ni décodées

#### Scenario: rangée visée hors fenêtre
- **WHEN** le focus vise une rangée non composée : rafale de touches plus rapide que le défilement, retour dans la grille sur la rangée laissée (héro, menu contextuel, réglages, retour d'une app), app déplacée d'une rangée hors fenêtre, rangée Up Next placée après les apps
- **THEN** la rangée visée est composée dès que l'ancre la désigne, avant la demande de focus, et la tuile ou la carte reçoit le focus par son identité à la place prévue par « Panneau Top Shelf au focus » ; un retour rapide en arrière retrouve la rangée précédente encore composée

#### Scenario: panneau sur une rangée profonde
- **WHEN** le panneau Top Shelf s'ouvre sur une rangée d'une grande grille
- **THEN** le bloc focusé est placé comme sur une petite grille ; pendant le délai d'ouverture, la première affiche validée est décodée à la taille du panneau avec la demande partagée de l'affichage, pour que l'expansion montre une image prête ; les ajouts attendent la fin de la validation sans changer l'affiche courante, mais les retraits et expirations s'appliquent immédiatement ; seules la rangée dont le panneau est ouvert et celle dont il se ferme portent son animation

#### Scenario: fraîcheur du cache disque
- **WHEN** une image est demandée par le héro, le Top Shelf ou À suivre
- **THEN** ses octets, dimensions et dates suivent « Cache borné et fraîcheur des images », y compris pour une URI `content://`, une entrée mémoire ou une image déjà affichée ; une validation ne provoque pas une seconde ouverture du fournisseur lors de l'affichage

**Détails.** — Reprise des visuels du héro : Une vidéo d'aperçu en échec ne SHALL pas être redemandée pendant la session. Les images SHALL suivre les reprises et expirations communes, sans blacklist locale permanente après une panne temporaire. Un rejet de dimensions SHALL porter sur l'usage et la version ; les candidates alternatives restent possibles. Cas « classement de l'échec » : quand un visuel échoue, les scénarios « visuel injoignable » et « visuel rejeté » de « Préchargement et mémoire » déterminent la reprise selon version, cause et échéance, sans empêcher les autres usages. — Fluidité de release : Sur la TV de référence et l'APK release, le héro au repos/au focus du dock SHALL atteindre au moins 55 images/s et moins de 10 % d'images en retard. La navigation grille/panneau SHALL avoir moins de 10 % de retard : seules les tuiles et la rangée concernées sont recomposées, sans décodage ni validation avant le focus posé. Cas « navigation et repos » : quand les scénarios « navigation dans la grille » et « écran au repos » de « Préchargement et mémoire » sont mesurés, ces seuils s'appliquent sans animations hors écran ni recomposition par frame. — Fenêtre de composition de la grille : Jusqu'à 12 rangées, Up Next comprise, toutes SHALL être composées ; au-delà, seulement celles couvrant l'écran aux positions courante et cible, plus 2 rangées de prélecture de chaque côté, les autres remplacées par leur hauteur. La page SHALL garder une seule autorité verticale ; grille, verre, ambiance, placement, navigation, persistance et repli du focus conservent leurs règles. Cas « grande grille et rangée visée » : quand la grille dépasse 12 rangées, « grande grille », « rangée visée hors fenêtre » et « panneau sur une rangée profonde » de « Préchargement et mémoire » s'appliquent, la grille restant source du verre et du dégradé d'ambiance.

### Requirement: Focus d'une app disparue
Quand l'app focusée disparaît du dock ou de la grille (app cachée, retirée du dock, désinstallée), le focus SHALL passer à la tuile voisine à la même position dans la zone. Si la zone devient vide, le focus SHALL revenir au héro. Les autres règles de focus restent celles de « Navigation 3 paliers ».

#### Scenario: tuile au milieu
- **WHEN** l'app focusée disparaît alors qu'une tuile la suivait dans la zone
- **THEN** cette tuile, qui prend sa position, reçoit le focus ; le focus ne retombe ni sur la première tuile ni hors de la zone

#### Scenario: dernière tuile
- **WHEN** l'app focusée était la dernière tuile de la zone et qu'il en reste d'autres
- **THEN** la tuile précédente prend le focus

#### Scenario: zone vidée
- **WHEN** l'app focusée était la seule de sa zone (dernière app du dock retirée, dernière app de la grille cachée)
- **THEN** le héro reprend le focus et Retour répond à nouveau, de la même façon pour la grille et pour le dock

#### Scenario: zone inactive
- **WHEN** la dernière tuile visitée d'une zone disparaît pendant que l'utilisateur est dans une autre zone
- **THEN** le focus ne change pas de zone ; au retour dans la zone, la tuile voisine à la même position prend le focus

**Détails.** — Repli du focus des cartes : Si la carte focusée ou dernière visitée disparaît et que d'autres restent, le focus SHALL aller à celle qui prend sa position, ou à la précédente si c'était la dernière. Une rangée vide ou masquée SHALL appliquer « Focus d'une app disparue » à la grille entière, Up Next comprise. Cas « disparition d'une carte ou rangée » : quand le contenu change pendant l'affichage ou avant le retour, « carte focusée disparue », « carte laissée disparue avant le retour » et « rangée devenue vide » de « Navigation 3 paliers » déterminent le focus.

### Requirement: Écran de démarrage
À la première création de l'accueil du processus, y compris en arrière-plan ou après mise à jour, le launcher SHALL afficher plein écran la mascotte seule, centrée sur le noir, avec un court fondu. L'accueil SHALL être prêt quand catalogue et premier visuel le sont (attente plafonnée à 2 s) ; l'écran SHALL durer au moins 600 ms et au plus 5 s, puis laisser place par fondu sans saut ni flash. Composition, touches et sortie SHALL suivre les scénarios ci-dessous ; Retour suit le système.

#### Scenario: démarrage à froid
- **WHEN** l'accueil est créé pour la première fois dans le processus du launcher
- **THEN** l'écran de démarrage s'affiche : la mascotte seule, centrée, animée, sur fond noir ; aucun texte, aucun dégradé animé, aucune autre zone de l'accueil n'est visible

#### Scenario: processus démarré en arrière-plan
- **WHEN** Android a démarré le processus du launcher en arrière-plan (diffusion système, mise à jour) sans créer l'accueil, puis l'utilisateur ouvre le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: accueil prêt avant la durée minimale
- **WHEN** le catalogue est chargé et le premier visuel du héro est prêt 100 ms après la première image qui montre la mascotte
- **THEN** l'écran de démarrage reste affiché jusqu'à 600 ms après cette image, puis le fondu vers l'accueil commence

#### Scenario: attente du premier visuel
- **WHEN** le catalogue est chargé à 200 ms et le premier visuel du héro n'est prêt qu'à 1,5 s
- **THEN** l'écran de démarrage reste affiché et animé jusqu'à 1,5 s, puis le fondu commence aussitôt

#### Scenario: visuel trop lent
- **WHEN** le catalogue est chargé à 200 ms et le premier visuel du héro n'est toujours pas prêt 2 s après la première image de l'écran de démarrage
- **THEN** le fondu commence à 2 s ; le héro poursuit son propre chargement et affiche son état de repli tant qu'aucun visuel n'est prêt (« Écran initial du home », scénario « fallback sans contenu »)

#### Scenario: plafond global
- **WHEN** le catalogue n'est toujours pas chargé 5 s après la première image de l'écran de démarrage
- **THEN** le fondu commence à 5 s et révèle l'accueil dans son état courant : fond noir uni tant que le catalogue n'est pas chargé, puis l'accueil dès qu'il l'est, avec ses propres états de chargement ou d'erreur

#### Scenario: fondu vers l'accueil
- **WHEN** le fondu vers l'accueil commence
- **THEN** l'accueil, jusque-là composé et caché sous l'écran de démarrage, apparaît pendant que l'écran de démarrage s'efface ; le héro détient le focus dès le début du fondu (« Écran initial du home ») ; à la fin du fondu l'écran de démarrage n'est plus affiché et son animation est arrêtée

#### Scenario: touches pendant l'écran de démarrage
- **WHEN** l'utilisateur presse une touche du D-pad ou OK avant le début du fondu
- **THEN** aucun élément de l'accueil ne prend le focus ni ne réagit ; au début du fondu, le héro prend le focus comme si aucune touche n'avait été pressée

#### Scenario: Retour pendant l'écran de démarrage
- **WHEN** l'utilisateur presse Retour pendant l'écran de démarrage, y compris dans la première seconde après le lancement
- **THEN** le comportement normal du système s'applique : SygixOs quitte le premier plan, sans crash ; aucune action n'est déclenchée sur l'accueil

#### Scenario: retour sur le launcher
- **WHEN** le launcher revient au premier plan (touche Home, fin d'une autre app) alors que l'accueil a déjà été créé dans le processus
- **THEN** aucun écran de démarrage n'est affiché ; l'accueil s'affiche directement, selon les règles de retour existantes (« Navigation 3 paliers »)

#### Scenario: accueil recréé hors démarrage à froid
- **WHEN** l'accueil est recréé dans un processus où il avait déjà été créé, avant que son catalogue soit chargé
- **THEN** un fond noir uni s'affiche, sans mascotte ni animation, jusqu'à ce que l'accueil s'affiche

#### Scenario: processus arrêté par le système
- **WHEN** le système a arrêté le processus du launcher (manque de mémoire) et l'utilisateur revient sur le launcher
- **THEN** c'est un démarrage à froid : l'écran de démarrage s'affiche selon les mêmes règles

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande système de la permission `READ_TV_LISTINGS` s'affiche pendant l'écran de démarrage
- **THEN** la demande apparaît par-dessus ; l'écran de démarrage et le fondu se poursuivent sans attendre la réponse, qui est traitée comme avant (« Contenu héro TV Provider », scénario « permission »)

#### Scenario: animation illisible
- **WHEN** l'animation de la mascotte ne peut pas être décodée (fichier absent ou corrompu)
- **THEN** l'écran de démarrage affiche le fond seul, sans mascotte, avec les mêmes règles de durée et de fondu, la durée minimale comptant depuis sa première image ; aucune erreur visible, aucun crash

#### Scenario: mascotte décodée tard
- **WHEN** la mascotte n'apparaît que 800 ms après la première image de l'écran de démarrage, l'accueil étant déjà prêt
- **THEN** la mascotte apparaît par son fondu d'entrée et l'écran de démarrage reste affiché 600 ms à partir de son apparition, dans la limite du plafond global de 5 s

#### Scenario: composition de l'accueil
- **WHEN** le catalogue est chargé avant la fin du fondu d'entrée de la mascotte ou avant que la fenêtre soit active
- **THEN** l'accueil n'est composé qu'après ce fondu et l'activation de la fenêtre ; l'apparition de la mascotte et son animation ne sont pas interrompues

#### Scenario: fenêtre sans focus
- **WHEN** la demande de permission du premier lancement ou une surcouche système garde le focus de la fenêtre pendant l'écran de démarrage
- **THEN** l'accueil est composé 400 ms après le fondu d'entrée de la mascotte et l'écran de démarrage se termine selon les mêmes règles de durée

#### Scenario: fluidité de l'écran de démarrage
- **WHEN** l'écran de démarrage est affiché puis s'efface vers l'accueil sur la TV de référence (APK de release)
- **THEN** la mascotte et le fondu de sortie visent 60 images par seconde et moins de 10 % d'images en retard ; aucun travail de l'accueil autre que sa première composition et son premier dessin, cachés sous l'écran de démarrage, n'a lieu pendant la mascotte

#### Scenario: Retour pendant le fondu de sortie
- **WHEN** l'utilisateur presse Retour pendant le fondu de sortie, entre son début et sa fin
- **THEN** le comportement normal du système s'applique comme pendant le reste de l'écran de démarrage : SygixOs quitte le premier plan, sans crash ; la touche n'est pas interceptée par l'accueil et aucune action n'est déclenchée sur lui

**Détails.** — Préparation du démarrage : Le catalogue chargé et le premier visuel du héro prêt SHALL rendre l'accueil prêt : image chargée ou première image vidéo programme/nature rendue, pas le dégradé animé. L'attente du visuel SHALL être plafonnée à 2 s depuis la première image de démarrage. La mascotte SHALL être décodée dès le début du processus, hors fil principal. Cas « attente du premier média » : quand le premier visuel tarde ou que l'animation est illisible, « attente du premier visuel », « visuel trop lent », « animation illisible » et « mascotte décodée tard » de « Écran de démarrage » s'appliquent. — Durées du démarrage : Le démarrage SHALL durer au moins 600 ms depuis la première image montrant la mascotte, ou sa propre première image si elle est illisible ; il attend l'accueil prêt, au plus 5 s depuis sa première image. Dès minimum atteint et accueil prêt, ou à 5 s, le fondu SHALL révéler l'accueil ; au plafond ses états de chargement/erreur prennent le relais. Cas « minimum et plafonds » : quand l'accueil est prêt tôt ou reste incomplet, « accueil prêt avant la durée minimale » et « plafond global » de « Écran de démarrage » déterminent la sortie, même sans animation. — Composition avant révélation : L'accueil SHALL être composé après le fondu d'entrée de la mascotte, dès fenêtre active ou au plus tard 400 ms après ce fondu sinon, ou au début du fondu de sortie. Avant révélation, il SHALL être dessiné sous le démarrage opaque, invisible et sans animation, sans retarder mascotte ni touches : image sans fondu, vidéo préparée en pause, Ken Burns/lecture au fondu de sortie, grille hors écran composée seulement à sa fin. Cas « fenêtre active ou surcouche » : quand une permission ou autre surcouche prive la fenêtre de focus, « composition de l'accueil » et « fenêtre sans focus » de « Écran de démarrage » gardent ces bornes et un accueil déjà dessiné pour le fondu. — Sortie du démarrage : Le fondu de sortie SHALL utiliser durée/courbe de « Focus tvOS », sans noir, saut, flash ni calque plein écran hors écran. Pendant tout le démarrage, fondu compris, D-pad/OK ne SHALL pas atteindre l'accueil ; Retour SHALL suivre Android, quitter le premier plan et ne déclencher aucune action de l'accueil. Cas « touches et transition » : quand l'utilisateur agit pendant le démarrage ou son fondu, « touches pendant l'écran de démarrage », « Retour pendant le fondu de sortie » et « fluidité de l'écran de démarrage » restent applicables. — Retour sans écran de démarrage : Un accueil déjà créé dans le processus ne SHALL jamais remontrer le démarrage au retour Home, d'une autre app ou au premier plan. Recréé hors démarrage à froid avec catalogue non chargé, il SHALL montrer seulement un fond noir uni, sans mascotte ni animation, jusqu'à l'accueil. Cas « retour ou recréation » : quand le launcher revient sans nouveau processus, « retour sur le launcher » et « accueil recréé hors démarrage à froid » de « Écran de démarrage » s'appliquent.

### Requirement: Animation de la mascotte
La mascotte SHALL flotter et cligner dans une animation pré-rendue de 2 s / 120 images, à 60 images/s en boucle sans raccord visible, décodée par le système sans lecteur vidéo. Centrée à 180 dp, native 360 px en 1080p, elle SHALL ne jamais être agrandie au-delà, avec pixels extérieurs transparents sans cadre ni écart de fond. La première image SHALL être la pose fixe de repos, yeux ouverts au centre du flottement, utilisée sans animation.

#### Scenario: lecture en boucle
- **WHEN** l'écran de démarrage reste affiché plus de 2 s
- **THEN** l'animation reprend au début sans arrêt, sans image figée ni saut de position ou d'expression au raccord

#### Scenario: bords de la mascotte
- **WHEN** l'écran de démarrage est affiché
- **THEN** aucun rectangle ni halo de couleur différente du fond n'est visible autour de la mascotte

#### Scenario: taille à l'écran
- **WHEN** l'écran de démarrage est affiché sur une TV 1080p
- **THEN** la mascotte mesure 180 dp de côté, centrée horizontalement et verticalement, sans agrandissement de l'animation au-delà de sa résolution native

#### Scenario: fin de l'écran de démarrage
- **WHEN** le fondu vers l'accueil se termine
- **THEN** l'animation est arrêtée et ses ressources sont libérées ; aucune image de la mascotte n'est plus décodée tant que le processus vit

### Requirement: Fichier de l'animation de la mascotte
L'animation de la mascotte SHALL être livrée dans l'application sous la forme d'un unique fichier WebP animé qui respecte ce contrat : canevas carré de 360 × 360 px ; canal alpha présent ; nombre de répétitions 0 (boucle infinie) ; 120 images ; chaque image dure 16 ou 17 ms ; durée totale de la boucle de 2 s à 3 ms près ; poids du fichier au plus 1 Mio (1 048 576 octets). Un fichier qui ne respecte pas ce contrat SHALL faire échouer la suite de tests.

#### Scenario: fichier conforme
- **WHEN** la suite de tests s'exécute avec le fichier livré
- **THEN** le contrôle du fichier vérifie le format WebP animé, le canevas, le canal alpha, le nombre de répétitions, le nombre et la durée des images, la durée de la boucle et le poids, et réussit

#### Scenario: fichier hors contrat
- **WHEN** le fichier livré pèse plus de 1 Mio, n'a pas de canal alpha, a un canevas autre que 360 × 360 px, une boucle finie, un autre nombre d'images ou des images de durée autre que 16 ou 17 ms
- **THEN** la suite de tests échoue en nommant la contrainte non respectée

### Requirement: Enchaînement avec l'écran de lancement du système
Sur Android 12+, l'écran de lancement système SHALL avoir le même noir que le démarrage, sans icône ni image. Du lancement à l'accueil, seuls ce noir puis le démarrage SHALL apparaître, sans double écran, changement de fond, icône système ou double mascotte. La disparition de l'écran système SHALL être invisible, sans fondu ni zoom d'une couleur ou icône différente.

#### Scenario: lancement à froid
- **WHEN** le système affiche son écran de lancement pour SygixOs
- **THEN** son fond est le noir exact du fond de l'écran de démarrage et il ne montre ni icône ni image

#### Scenario: passage à l'écran de démarrage
- **WHEN** la première image de l'écran de démarrage est dessinée
- **THEN** l'écran de lancement du système disparaît sans animation visible : aucune image intermédiaire d'une autre couleur, aucune icône ; la mascotte n'apparaît qu'une fois, par son fondu d'entrée

#### Scenario: écran de lancement absent
- **WHEN** le système n'affiche pas d'écran de lancement pour SygixOs
- **THEN** la fenêtre de l'app montre le même fond noir jusqu'à la première image de l'écran de démarrage, sans flash d'une autre couleur

#### Scenario: relance sans démarrage à froid
- **WHEN** le système affiche son écran de lancement alors que l'accueil a déjà été créé dans le processus (activité recréée)
- **THEN** cet écran a le même fond noir, sans icône, et aucun écran de démarrage n'est affiché ensuite (« Écran de démarrage », scénario « accueil recréé hors démarrage à froid »)

### Requirement: Écran de démarrage sans animation
À chaque démarrage à froid, le launcher SHALL lire l'échelle de durée système, commune à « Supprimer les animations » et aux options développeur. À 0, il SHALL montrer la première image fixe de la mascotte, sans flottement, clignement ni fondu d'entrée/sortie, en conservant les durées du démarrage (600 ms au moins, plafonds de 2 s et de 5 s). Une échelle propre aux apps forcée par le constructeur ne SHALL pas désactiver l'animation.

#### Scenario: animations désactivées
- **WHEN** le launcher démarre à froid alors que les animations sont désactivées dans le système
- **THEN** l'écran de démarrage montre la première image de la mascotte, immobile, dès sa première image ; dès que les règles de durée le permettent, l'accueil s'affiche d'un coup, sans fondu, le héro ayant le focus

#### Scenario: réglage changé entre deux démarrages
- **WHEN** l'utilisateur réactive les animations puis le launcher redémarre à froid
- **THEN** la mascotte est de nouveau animée et les fondus sont joués

#### Scenario: échelle forcée par le constructeur
- **WHEN** le système laisse l'échelle de durée des animations à sa valeur normale mais que le constructeur force à 0 l'échelle d'animation des apps
- **THEN** la mascotte est animée (flottement et clignement), apparaît et disparaît par ses fondus

#### Scenario: Retour sans fondu
- **WHEN** les animations sont désactivées et l'utilisateur presse Retour, avant ou après le remplacement instantané de l'écran de démarrage par l'accueil
- **THEN** avant le remplacement, le comportement normal du système s'applique : SygixOs quitte le premier plan, sans action sur l'accueil ; après le remplacement, l'accueil est interactif et Retour suit « Navigation 3 paliers », l'app restant au premier plan

### Requirement: Pastille
Le design system SHALL fournir une pastille unique : point plein de 6 dp bleu `#0A84FF`, au coin haut droit de l'élément ou en fin de ligne de liste. Non focusable et sans animation continue, elle SHALL suivre défilement/zoom sans changer taille, position, focus ni navigation de l'élément. Toute fonctionnalité, dont self-update, SHALL utiliser ce composant sans en redéfinir l'aspect.

#### Scenario: affichage
- **WHEN** un élément porte une pastille
- **THEN** un point bleu plein de 6 dp est dessiné à son coin supérieur droit (ou en fin de ligne pour une ligne de liste), par-dessus l'élément, sans décaler son contenu

#### Scenario: focus inchangé
- **WHEN** l'utilisateur déplace le focus sur ou à côté d'un élément qui porte une pastille
- **THEN** la pastille ne prend jamais le focus, et le focus, la taille et la position de l'élément sont identiques à ceux du même élément sans pastille

#### Scenario: élément en mouvement
- **WHEN** l'élément qui porte la pastille défile ou change d'échelle (focus)
- **THEN** la pastille reste à la même place relative sur l'élément

#### Scenario: écran au repos
- **WHEN** une pastille est affichée et que l'utilisateur ne touche à rien
- **THEN** la pastille ne provoque aucun redessin continu

### Requirement: Profil de démarrage livré avec l'application
L'APK release SHALL embarquer le profil de référence de l'app et de ses bibliothèques. Le build SHALL produire depuis cet APK `app-release.dm`, contenant seulement `primary.prof` et `primary.profm`, publié à côté de `app-release.apk` avec SHA-256 GitHub. Installé avec l'APK, il SHALL permettre la compilation du démarrage dès l'installation sans attendre le fond ; l'APK reste installable seul.

#### Scenario: release publiée
- **WHEN** une release est publiée par la CI
- **THEN** elle contient `app-release.apk` et `app-release.dm` produits par le même build, chacun avec son empreinte SHA-256, et le profil de `app-release.dm` est celui embarqué dans `app-release.apk`

#### Scenario: installation manuelle avec le profil
- **WHEN** l'utilisateur installe l'APK et le fichier `.dm` ensemble (`adb install-multiple app-release.apk app-release.dm`, documenté dans le README)
- **THEN** l'app est compilée avec son profil dès la fin de l'installation, sans attendre la compilation de fond du système

#### Scenario: installation sans le profil
- **WHEN** l'APK est installé seul
- **THEN** l'installation réussit ; le profil embarqué est appliqué plus tard par le système, comme avant

#### Scenario: mise à jour intégrée
- **WHEN** la mise à jour intégrée installe une nouvelle version
- **THEN** le profil est installé avec l'APK quand il est disponible et vérifié (« Installation de la mise à jour » de `self-update`)

### Requirement: Démarrage à l'allumage
La ligne « Ouvrir SygixOs à l'allumage » de « Par autorisations » SHALL être désactivée par défaut, activable par l'utilisateur ou la configuration automatique de secours. OK SHALL basculer et persister aussitôt sans perdre le focus. Option activée, l'ouverture SHALL être demandée une seule fois par démarrage à froid, par la fin du démarrage ou le service d'accessibilité connecté moins de 5 minutes après, sans sortie de veille. Les scénarios ci-dessous SHALL préciser les états.

#### Scenario: ouverture au démarrage observée
- **WHEN** l'appareil démarre, que l'option est activée et que la demande d'ouverture aboutit
- **THEN** l'accueil de SygixOs s'affiche au premier plan et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: diffusion bloquée, service connecté
- **WHEN** l'appareil démarre, que l'option est activée, que la fin du démarrage n'est pas transmise à SygixOs mais que le service d'accessibilité activé se connecte moins de 5 minutes après le démarrage
- **THEN** le service ouvre l'accueil de SygixOs et l'ouverture est enregistrée comme observée pour ce démarrage

#### Scenario: deux chemins dans le même démarrage
- **WHEN** la fin du démarrage est reçue et que le service d'accessibilité se connecte peu après, option activée
- **THEN** SygixOs s'ouvre une seule fois : le chemin qui arrive après l'ouverture observée ne demande rien, et une seconde demande arrivée avant elle n'a aucun effet visible

#### Scenario: connexion tardive du service
- **WHEN** le service d'accessibilité se connecte plus de 5 minutes après le démarrage, option activée
- **THEN** aucune ouverture au démarrage n'est demandée

#### Scenario: option désactivée
- **WHEN** l'appareil démarre et que l'option est désactivée
- **THEN** SygixOs ne demande pas son ouverture, ni par le récepteur ni par le service, et n'enregistre aucun échec

#### Scenario: fin du démarrage non reçue
- **WHEN** l'option était activée avant le démarrage, que le système ou le constructeur ne transmet pas la fin du démarrage à SygixOs et que le service d'accessibilité ne se connecte pas non plus
- **THEN** aucune ouverture n'est observée pour ce démarrage ; au lancement suivant de SygixOs, l'état « démarrage automatique non observé » est affiché par la ligne

#### Scenario: ouverture refusée par le système
- **WHEN** l'option était activée avant le démarrage, qu'un chemin d'ouverture est déclenché mais qu'Android refuse l'ouverture de l'activité
- **THEN** aucune ouverture n'est observée par ce chemin, aucun crash n'a lieu, l'autre chemin reste possible, et au lancement suivant l'état « démarrage automatique non observé » est affiché si aucune ouverture n'a été observée

#### Scenario: option activée après le démarrage
- **WHEN** l'option est activée, par l'utilisateur ou par la configuration automatique, alors que l'appareil a déjà démarré
- **THEN** le démarrage courant n'est pas compté comme un échec ; seul le prochain démarrage est évalué

#### Scenario: option activée par la configuration automatique
- **WHEN** la configuration automatique pose les réglages de secours
- **THEN** l'option est persistée activée et la ligne « Ouvrir SygixOs à l'allumage » la montre activée ; l'utilisateur peut la désactiver comme une option qu'il a activée

**Détails.** — Ligne de démarrage à l'allumage : La ligne SHALL afficher « Fait » si l'option est activée et « Autoriser l'ouverture au démarrage » absente, « À faire » sinon ; détail « SygixOs demande à s'ouvrir au démarrage de la TV ». L'ouverture observée SHALL être mémorisée par démarrage ; non observée, le détail devient « SygixOs ne s'est pas ouvert automatiquement à ce démarrage ». Cas « ouverture observée ou non » : quand l'utilisateur consulte la page après le démarrage, les scénarios de « Démarrage à l'allumage » déterminent l'état, sans assimiler une activation tardive à une tentative déjà due. — Ouverture unique au démarrage : Option activée, SygixOs SHALL demander l'accueil par les mécanismes Android autorisés seulement : fin du démarrage système et connexion de son service activé moins de 5 minutes après le démarrage (valeur à valider sur TV). Le premier chemin observé SHALL ouvrir une seule fois ; l'autre et une connexion tardive ne font rien. Option désactivée, aucune demande. Cas « deux chemins et refus du système » : quand les deux chemins arrivent, tardent ou sont bloqués, les scénarios « deux chemins dans le même démarrage », « connexion tardive du service » et « ouverture refusée par le système » de « Démarrage à l'allumage » s'appliquent sans contournement Android.

### Requirement: Permission d'affichage superposé pour le démarrage
La permission de superposition SHALL être proposée par la ligne « Autoriser l'ouverture au démarrage », sans confirmation, quand l'option est activée et l'écran système présent. Seule la configuration automatique peut l'accorder par le débogage ; sinon OK ouvre Android. Son état SHALL être relu au retour, jamais supposé accordé ; un refus ne bloque pas SygixOs. Sans écran système, la ligne devient « Indisponible » et l'option ne s'active pas. L'accord ne garantit pas le démarrage.

#### Scenario: sous-étape affichée à l'activation
- **WHEN** l'utilisateur active l'option alors que la permission manque et que son écran existe
- **THEN** l'option est persistée activée, la ligne « Autoriser l'ouverture au démarrage » apparaît avec « À faire », aucun écran système ne s'ouvre avant OK sur elle et le focus reste sur « Ouvrir SygixOs à l'allumage »

#### Scenario: ouverture de l'écran de la permission
- **WHEN** l'utilisateur presse OK sur « Autoriser l'ouverture au démarrage »
- **THEN** l'écran système de la permission s'ouvre et la ligne passe à « En attente… » ; au retour, elle disparaît seulement si le système déclare la permission accordée, et le focus va à « Ouvrir SygixOs à l'allumage » dans ce cas, sinon reste sur elle

#### Scenario: permission refusée
- **WHEN** l'utilisateur revient de l'écran système sans accorder la permission
- **THEN** SygixOs reste utilisable sans crash, ne suppose aucune permission accordée, conserve l'option activée et la ligne affiche « À faire »

#### Scenario: permission déjà accordée
- **WHEN** l'option est activée alors que la permission est déjà accordée
- **THEN** la ligne de la permission n'est pas affichée et « Ouvrir SygixOs à l'allumage » est « Fait »

#### Scenario: écran de la permission absent
- **WHEN** l'appareil n'a pas d'écran système pour la permission et que l'option de démarrage est désactivée
- **THEN** « Ouvrir SygixOs à l'allumage » est affichée « Indisponible », avec la raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage » ; OK ne l'active pas, aucune ligne de permission ni écran système n'apparaît et la ligne garde le focus

#### Scenario: écran de la permission impossible à ouvrir
- **WHEN** l'utilisateur valide « Autoriser l'ouverture au démarrage » et que l'écran système ne peut pas s'ouvrir
- **THEN** la ligne disparaît, l'option revient à désactivé et « Ouvrir SygixOs à l'allumage » devient « Indisponible » avec sa raison, focalisée ; une nouvelle activation est refusée jusqu'au prochain lancement, sauf si la permission est ensuite déclarée accordée

#### Scenario: permission accordée par la configuration automatique
- **WHEN** la configuration automatique accorde la permission comme réglage de secours
- **THEN** la permission est relue auprès du système, et la ligne « Superposer aux autres applications » de « SygixOs garde la main » (9.12) n'est « Fait » que si le système la déclare accordée

**Détails.** — Ligne d'autorisation de démarrage : « Autoriser l'ouverture au démarrage » SHALL apparaître sous l'option activée si la permission manque et l'écran existe, détail « Android l'exige : choisissez SygixOs dans la liste, puis autorisez-le. ». État « À faire » sans accord, « En attente… » dans Android, masquée après accord système. OK SHALL ouvrir la permission de SygixOs ; son libellé exact reste celui relevé par la tâche 1.5 de P7. Cas « permission et retour » : quand l'utilisateur ouvre ou quitte l'écran système, les scénarios « ouverture de l'écran de la permission », « permission refusée » et « permission déjà accordée » de « Permission d'affichage superposé pour le démarrage » s'appliquent. — Superposition indisponible : Si l'écran de permission est absent ou impossible à ouvrir, l'option SHALL devenir « Indisponible », raison « Cette TV ne permet pas d'autoriser l'ouverture au démarrage », sans ligne de permission ni crash. L'activation SHALL être refusée ou annulée si juste faite. L'état dure jusqu'au prochain processus sauf accord système ultérieur ; une option déjà activée reste désactivable. Cas « écran absent ou inaccessible » : quand l'ouverture de la permission échoue, « écran de la permission absent » et « écran de la permission impossible à ouvrir » de « Permission d'affichage superposé pour le démarrage » vérifient l'option sans bloquer le launcher.

### Requirement: Retour Home par le service d'accessibilité
Le service SHALL ramener l'accueil sur Home après activation par l'utilisateur (ligne « Ouvrir SygixOs avec la touche Home » ouvrant Accessibilité) ou par la configuration automatique de secours, autres services conservés. Hors ces cas SygixOs ne SHALL pas l'activer ; après refus, reproposition seulement dans Réglages. Connecté, il SHALL consommer tous les événements Home et ouvrir au relâchement non annulé ; non connecté, rien n'est consommé. Les états SHALL suivre les scénarios ci-dessous.

#### Scenario: activation par l'utilisateur
- **WHEN** l'utilisateur valide « Ouvrir SygixOs avec la touche Home »
- **THEN** les réglages d'accessibilité du système s'ouvrent ; au retour au premier plan, la ligne affiche « Fait » seulement si le système déclare le service activé et qu'il est connecté

#### Scenario: activation par la configuration automatique
- **WHEN** la configuration automatique active le service comme réglage de secours
- **THEN** le service est ajouté à la liste des services activés sans retirer les autres, l'état est relu, et la ligne « Touche Home » de « SygixOs garde la main » (9.12) n'est « Fait » que si le service est connecté

#### Scenario: service refusé
- **WHEN** l'utilisateur revient des réglages d'accessibilité sans avoir activé le service, ou ferme le parcours sans l'ouvrir
- **THEN** la ligne affiche « À faire », SygixOs reste utilisable et aucune nouvelle proposition ne s'affiche automatiquement, à ce lancement comme aux suivants

#### Scenario: touche Home, SygixOs en arrière-plan
- **WHEN** le service est connecté, que SygixOs est en arrière-plan et que l'utilisateur presse Home
- **THEN** le service consomme l'appui et le relâchement, l'accueil du constructeur ne s'affiche à aucun moment, et SygixOs revient au premier plan sur son accueil dès le relâchement, selon les règles de retour existantes (« Écran de démarrage », scénario « retour sur le launcher »)

#### Scenario: touche Home, SygixOs au premier plan
- **WHEN** le service est connecté, que SygixOs est au premier plan et que l'utilisateur presse Home
- **THEN** la touche est consommée et SygixOs applique « Touche Home avec SygixOs au premier plan »

#### Scenario: appui long sur Home
- **WHEN** le service est connecté et que l'utilisateur maintient la touche Home (répétitions ou maintien au-delà du délai d'appui long)
- **THEN** tous les événements sont consommés, aucun comportement système d'appui long ne s'applique, et SygixOs s'ouvre au relâchement comme pour un appui court

#### Scenario: relâchement annulé
- **WHEN** le système annule l'appui sur Home en cours (relâchement marqué annulé)
- **THEN** l'événement est consommé et SygixOs n'est pas ouvert

#### Scenario: signal Home absent
- **WHEN** l'appareil ne transmet aucune touche Home au service, même connecté
- **THEN** le service reste livré, ne lit aucun contenu de fenêtre, ne demande aucune ouverture et laisse le comportement système inchangé

#### Scenario: service désactivé ou arrêté
- **WHEN** l'utilisateur désactive le service dans les réglages système, ou que le système l'arrête
- **THEN** aucune touche n'est consommée, l'appui sur Home suit le comportement du système, aucune erreur n'est affichée, et au retour au premier plan la ligne affiche « À faire » (désactivé) ou suit le scénario « service bloqué » (activé mais arrêté)

#### Scenario: service non lié au lancement
- **WHEN** après un redémarrage complet, le gestionnaire d'auto-démarrage du constructeur a empêché la liaison du service activé, et que l'utilisateur lance SygixOs
- **THEN** la ligne affiche « En attente… » pendant l'attente bornée, puis « Bloqué » si le service ne s'est pas connecté ; SygixOs ne demande aucune nouvelle liaison et ne modifie ni ses composants, ni le service, ni les réglages du système

#### Scenario: service bloqué
- **WHEN** le service est activé dans les réglages du système mais n'est toujours pas connecté au terme de l'attente bornée
- **THEN** la ligne affiche « Bloqué » avec son explication, reste focalisable, et OK ouvre les réglages d'accessibilité du système ; la voie par autorisations est « Bloquée » ; quand le système relie de nouveau le service, la ligne affiche « Fait » au retour au premier plan

**Détails.** — Filtrage de Home : Le service SHALL observer seulement Home par `AccessibilityService.onKeyEvent`, sans lire les fenêtres ni rien conserver. Connecté, il SHALL consommer appui, répétitions et relâchement pour les soustraire au système/launcher constructeur, et ouvrir aussitôt au relâchement non annulé, appui long comme court. Inactif/arrêté, il ne consomme rien. Il SHALL être livré même sans signal Home sur l'appareil, sans action alors. Cas « événements de touche » : quand Home est reçu ou absent, les scénarios de touches et de signal absent de « Retour Home par le service d'accessibilité » s'appliquent ; SygixOs reste utilisable sans le service. — État du service Home : Activé mais non connecté au lancement ou au retour au premier plan, le service SHALL être attendu pendant un délai borné sans tentative de liaison par SygixOs. La ligne SHALL refléter activation, connexion, attente et disponibilité des réglages, avec les libellés ci-dessous ; « Bloqué » dure jusqu'à connexion ou désactivation. Cas « présentation de la ligne du service » : quand l'état est relu, activé/connecté affiche « Fait », non activé « À faire », détail « OK : ouvre le réglage Android correspondant » ; réglages ouverts ou attente non conclue affiche « En attente… » ; délai écoulé activé/non connecté affiche « Bloqué », détail « La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité, ou utilisez la configuration automatique. » ; réglages impossibles à ouvrir affiche « Indisponible », raison « Les réglages d'accessibilité ne peuvent pas s'ouvrir sur cette TV ».

### Requirement: Touche Home avec SygixOs au premier plan
Home reçu au premier plan, directement ou par le service (premier plan à l'appui), SHALL fermer tous les réglages d'un coup, liste « Position d'À suivre » comprise, puis les surcouches comme Retour, parcours compris ; page en haut, héro focusé et lecture reprise. L'allumage ne SHALL pas compter comme Home. Hors premier plan, « retour sur le launcher » de « Écran de démarrage » reste inchangé.

#### Scenario: depuis la grille
- **WHEN** le focus est dans la zone grille et que la touche Home atteint SygixOs
- **THEN** la page défile jusqu'au héro comme au scénario « retour depuis la grille » de « Navigation 3 paliers », le héro prend le focus et sa lecture reprend

#### Scenario: depuis les réglages
- **WHEN** la page de réglages est ouverte, quelle que soit la catégorie ou la ligne focalisée, et que la touche Home atteint SygixOs
- **THEN** les réglages se ferment en une fois, la page de l'accueil est en haut, le héro a le focus et sa lecture reprend

#### Scenario: depuis la liste déroulante « Position d'À suivre »
- **WHEN** la liste déroulante « Position d'À suivre » est ouverte dans les réglages et que la touche Home atteint SygixOs
- **THEN** la liste se ferme sans changer la valeur et les réglages se ferment dans le même temps, sans étape intermédiaire sur la ligne « Position d'À suivre » ; la page de l'accueil est en haut, le héro a le focus et sa lecture reprend ; à la réouverture des réglages, la liste est fermée et la valeur persistée inchangée

#### Scenario: depuis une surcouche
- **WHEN** le menu contextuel d'une tuile, le mode « Déplacer » ou le parcours guidé est affiché et que la touche Home atteint SygixOs
- **THEN** la surcouche se ferme comme par Retour (déplacement annulé ; parcours enregistré comme fermé s'il avait été affiché automatiquement), les réglages éventuellement ouverts dessous se ferment aussi, la page de l'accueil est en haut et le héro a le focus

#### Scenario: déjà sur le héro
- **WHEN** le héro a déjà le focus, sans surcouche, et que la touche Home atteint SygixOs
- **THEN** le héro garde le focus et rien d'autre ne change

#### Scenario: pendant l'écran de démarrage
- **WHEN** la touche Home atteint SygixOs pendant l'écran de démarrage
- **THEN** l'écran de démarrage se poursuit sans changement et le héro prend le focus à son fondu de sortie (« Écran de démarrage »)

### Requirement: Parcours de l'écran d'accueil
Le parcours « Faire de SygixOs l'écran d'accueil » SHALL s'afficher une fois tant que jamais fermé, installations mises à jour comprises (drapeau conservé depuis v0.1.0-rc.2), après démarrage et réponse à « Programmes TV », jamais sur un dialogue système. Sa première page SHALL proposer les choix disponibles (Tsuyu, automatique, autorisations) avec leur état, l'aide « Retour : plus tard » et le bouton « Plus tard ». Choix, navigation, étapes et avancement SHALL suivre les scénarios ci-dessous.

#### Scenario: premier lancement
- **WHEN** SygixOs se lance, que le parcours n'a jamais été fermé, que Tsuyu n'est pas disponible et que rien n'est encore fait
- **THEN** à la fin de l'écran de démarrage, la première page s'affiche sur l'accueil, sans ligne Tsuyu, avec « Configuration automatique (recommandée) » à l'état « À configurer » et « Par autorisations (sans débogage) » à l'état « À faire », et « Configuration automatique (recommandée) » a le focus

#### Scenario: premier lancement avec Tsuyu disponible
- **WHEN** SygixOs se lance, que le parcours n'a jamais été fermé, que Tsuyu est disponible et que rien n'est encore fait
- **THEN** le panneau 9.1 s'affiche avec, de haut en bas, « Automatique avec Tsuyu (recommandée) » et « Automatique sans Tsuyu » à « À configurer », « Par autorisations (sans débogage) » à « À faire », puis « Plus tard » ; « Automatique avec Tsuyu (recommandée) » a le focus

#### Scenario: voie automatique indisponible
- **WHEN** la voie automatique est « Indisponible sur cette TV » à l'ouverture du parcours, que Tsuyu soit disponible ou non
- **THEN** le panneau 8.2 s'affiche, sans ligne Tsuyu, « Par autorisations (sans débogage) » a le focus, et OK sur « Configuration automatique » ne fait rien

#### Scenario: navigation
- **WHEN** l'utilisateur presse bas quatre fois, haut quatre fois, puis gauche et droite sur 9.1
- **THEN** le focus passe d'une ligne à la suivante jusqu'à « Plus tard », y reste, remonte jusqu'à la première ligne et y reste ; gauche et droite ne le déplacent pas et aucun élément de l'accueil ne prend le focus

#### Scenario: plus tard
- **WHEN** l'utilisateur presse OK sur « Plus tard » ou Retour sur la première page affichée automatiquement
- **THEN** le parcours se ferme, il est enregistré comme fermé, l'accueil reprend avec le focus sur le héro, aucune connexion privilégiée n'a eu lieu, rien n'est téléchargé et aucun écran système n'est ouvert

#### Scenario: repère d'avancement
- **WHEN** l'utilisateur choisit « Automatique avec Tsuyu (recommandée) » alors que le débogage est désactivé et que Tsuyu n'est pas installé
- **THEN** l'écran du débogage affiche « Étape 1 sur 5 », puis « Installer Tsuyu » « Étape 2 sur 5 », « Démarrer Tsuyu » « Étape 3 sur 5 », « Autoriser SygixOs » « Étape 4 sur 5 » et « Configuration en cours » « Étape 5 sur 5 »

#### Scenario: étapes déjà faites
- **WHEN** l'utilisateur choisit « Automatique sans Tsuyu » alors que le débogage est déjà activé
- **THEN** l'écran « Autoriser SygixOs » s'affiche directement avec « Étape 1 sur 2 »

#### Scenario: passage sans Tsuyu
- **WHEN** l'utilisateur presse « Sans Tsuyu » sur « Installer Tsuyu » affiché « Étape 2 sur 5 »
- **THEN** l'écran « Autoriser SygixOs » (8.4) s'affiche avec « Étape 2 sur 3 »

#### Scenario: tout par OK
- **WHEN** sur la TV de référence, rien n'étant fait et Tsuyu disponible, l'utilisateur presse uniquement OK sur chaque écran de SygixOs et suit les écrans d'Android et de Tsuyu qu'il annonce
- **THEN** la configuration se termine par « SygixOs garde la main » avec Tsuyu, l'accueil Google TV n'est pas désactivé et aucune confirmation de désactivation ne s'est affichée

#### Scenario: demande de permission au premier lancement
- **WHEN** la demande de permission « Programmes TV » est affichée à la fin de l'écran de démarrage
- **THEN** le parcours ne s'affiche pas tant que ce dialogue est à l'écran ; il s'affiche dès que l'utilisateur a accordé ou refusé la permission

#### Scenario: pas de nouvel affichage automatique
- **WHEN** SygixOs se lance de nouveau après la fermeture du parcours affiché automatiquement, y compris après un redémarrage de l'appareil
- **THEN** le parcours ne s'affiche pas ; Réglages › Écran d'accueil reste le point d'entrée

#### Scenario: processus arrêté pendant un écran système
- **WHEN** le système arrête le processus de SygixOs pendant qu'un écran système ou Tsuyu, ouvert depuis le parcours affiché automatiquement, est affiché, puis SygixOs se relance
- **THEN** le parcours, jamais fermé, s'affiche de nouveau sur sa première page avec les états relus

**Détails.** — Choix du parcours : La première page SHALL présenter les choix et leurs états à droite, avec Tsuyu seulement s'il est disponible et la voie automatique non indisponible. « Active » SHALL identifier seulement la façon active (avec Tsuyu s'il est actif, sinon sans), l'autre « Non utilisée ». La voie par autorisations SHALL montrer son état propre ; la voie indisponible suit 8.2 quel que soit Tsuyu. Cas « première page et aide » : quand la première page s'affiche, le titre est « Faire de SygixOs l'écran d'accueil », la description « Choisissez comment SygixOs remplace l'accueil Google TV. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. », avec aide « Retour : plus tard » et bouton « Plus tard ». Cas « trois choix avec Tsuyu » : quand Tsuyu est disponible et la voie utilisable (9.1), les lignes sont « Automatique avec Tsuyu (recommandée) » / « Un guide pas à pas installe Tsuyu : SygixOs se règle seul et se répare si la TV défait ses réglages. », « Automatique sans Tsuyu » / « Activez le débogage une fois : SygixOs fait les réglages, sans réparation ensuite. », puis « Par autorisations (sans débogage) » / « Quelques autorisations à donner. Limite : sur certaines TV, l'accueil Google TV peut reprendre la main après un redémarrage complet. ». Cas « deux choix sans Tsuyu » : quand la page 9.1 ne propose pas Tsuyu, « Configuration automatique (recommandée) » / « Activez le débogage une fois : SygixOs fait les réglages lui-même. » précède la même ligne par autorisations. Cas « façon automatique indisponible » : quand la voie est « Indisponible sur cette TV » (8.2), la ligne « Configuration automatique » affiche « SygixOs n'arrive pas à utiliser le débogage sur cette TV : utilisez la configuration par autorisations. », focusable sans effet sur OK, même si Tsuyu est disponible. — Navigation du parcours : Le focus SHALL rester dans le panneau, initialement première ligne non indisponible, puis étape conseillée pour chaque écran. Haut/bas SHALL parcourir choix puis « Plus tard », sans boucle ; gauche/droite sans effet. Des OK successifs suivant Android/Tsuyu SHALL terminer sans désactiver l'accueil d'origine. Retour ne SHALL jamais modifier la TV et suit l'aide affichée. Cas « précédent ou fermeture » : quand Retour est pressé, « Retour : précédent » rend l'écran précédent et le focus à l'élément ouvrant ; « Retour : plus tard » et « Retour : fermer » ferment ; « Plus tard », « Terminé », « Fermer » et Home ferment aussi ; affiché automatiquement, le parcours ne revient plus automatiquement et le héro reprend le focus ; tout reste accessible dans Réglages › Écran d'accueil. — Étapes du parcours : OK sur le choix SHALL ouvrir le premier écran encore nécessaire dans l'ordre de sa façon, les étapes faites sautées. Avec Tsuyu : débogage 9.2/9.3, installation 9.4, démarrage 9.7, autorisation 9.8, réglages 9.10 ; sans : débogage 9.2/9.3, autorisation 8.4, réglages 9.10. La voie par autorisations SHALL ouvrir 8.13. Cas « premier écran nécessaire » : quand un choix est validé, « étapes déjà faites » et « tout par OK » de « Parcours de l'écran d'accueil » s'appliquent dans l'ordre « Activer le débogage », « Installer Tsuyu », « Démarrer Tsuyu », « Autoriser SygixOs », « Configuration en cours » selon la façon. — Avancement du parcours : Les écrans automatiques SHALL montrer au-dessus du titre « Étape X sur N » : étapes restantes à l'ouverture parmi débogage, installation/démarrage Tsuyu seulement avec lui, autorisation, réglages toujours ; X est le rang courant. L'erreur SHALL garder son repère. N ne SHALL être recalculé que pour « Sans Tsuyu » en 9.4/9.6. Première page, autorisations, détails, dialogues et panneau n'ont pas ce repère. Cas « repère et changement de façon » : quand une étape est affichée ou « Sans Tsuyu » choisi, « repère d'avancement » et « passage sans Tsuyu » de « Parcours de l'écran d'accueil » s'appliquent.

### Requirement: Accès par le débogage
Les opérations privilégiées SHALL passer par Tsuyu « Actif » (tsuyu-access), sinon par l'ADB local, après action explicite, délais bornés. La connexion SHALL viser la boucle locale, utiliser une clé propre à SygixOs et ne jamais la transmettre ; elle SHALL être fermée à la fin de chaque séquence. Les états distinguent débogage désactivé, connexion impossible, autorisation en attente/refusée et prêt ; le port ADB SHALL être testé avant Tsuyu. Aucun échec ne SHALL rendre SygixOs inutilisable.

#### Scenario: clé déjà autorisée
- **WHEN** la clé de SygixOs a déjà été autorisée avec « Toujours autoriser sur cet ordinateur », que le débogage est activé et que Tsuyu n'est pas « Actif »
- **THEN** OK sur la configuration automatique sans Tsuyu ouvre directement « Configuration en cours », sans fenêtre système

#### Scenario: Tsuyu actif
- **WHEN** Tsuyu est « Actif » et qu'une séquence démarre, depuis le parcours ou le panneau
- **THEN** aucune connexion ADB de SygixOs n'est ouverte et les opérations passent par Tsuyu

#### Scenario: port testé avant Tsuyu
- **WHEN** l'utilisateur revient du guide du débogage avec le débogage activé sur le parcours avec Tsuyu
- **THEN** SygixOs teste le port ADB réseau sans fenêtre système ; joignable, le parcours passe à l'étape Tsuyu suivante ; injoignable, rien n'est téléchargé

#### Scenario: port injoignable
- **WHEN** le débogage est activé mais que la connexion au port ADB réseau est refusée ou n'aboutit pas dans le délai
- **THEN** aucune opération n'est exécutée, la voie automatique devient « Indisponible sur cette TV » et le parcours revient au panneau 8.2

#### Scenario: débogage perdu pendant une séquence
- **WHEN** le débogage est désactivé ou la connexion perdue pendant une séquence
- **THEN** la séquence s'arrête, l'état du système est relu, l'écran « Configuration interrompue » (8.12) montre ce qui est fait et l'étape en échec, sans succès supposé

**Détails.** — États de l'accès privilégié : L'accès SHALL distinguer débogage désactivé (lu sans connexion), connexion impossible, autorisation en attente/refusée et prêt (clé acceptée et commande test shell réussie, ou Tsuyu actif). Connexion, attente et chaque commande SHALL être bornées selon p7-default-launcher D1 ; un délai dépassé ne SHALL donner ni prêt ni succès. Cas « perte d'accès » : quand le débogage disparaît ou un délai expire, « débogage perdu pendant une séquence » de « Accès par le débogage » s'applique sans succès ni crash supposé. — Port du débogage avant Tsuyu : Avec Tsuyu, débogage activé et avant téléchargement, une simple ouverture au bouclage SHALL tester le port ADB réseau, sans authentification ni fenêtre système, avec le délai de connexion. Aucun port joignable SHALL rendre la voie « Indisponible sur cette TV » jusqu'à une tentative réussie. Cas « port testé avant téléchargement » : quand la façon avec Tsuyu est choisie, « port testé avant Tsuyu » et « port injoignable » de « Accès par le débogage » s'appliquent.

### Requirement: Écrans du débogage
Débogage désactivé, la configuration SHALL commencer par « Activer le débogage » (deux étapes relues, bouton vers À propos puis options développeur). Activé sans Tsuyu, elle SHALL montrer « Autoriser SygixOs » avec « Toujours autoriser », puis « Continuer ». Refus, expiration et désactivation SHALL afficher l'erreur et revenir au parcours. Gauche/droite naviguent sans boucle, haut/bas sans effet ; Retour revient à l'écran précédent. Les scénarios ci-dessous SHALL préciser libellés et chemins.

#### Scenario: activer le débogage
- **WHEN** l'utilisateur ouvre la configuration automatique alors que les options pour les développeurs sont masquées et le débogage désactivé
- **THEN** l'écran 9.2 s'affiche avec les deux étapes à « À faire » et « Ouvrir À propos » focalisé ; OK ouvre l'écran système « À propos »

#### Scenario: options affichées au retour
- **WHEN** l'utilisateur a affiché les options pour les développeurs depuis « À propos » puis revient dans SygixOs sans avoir activé le débogage
- **THEN** l'écran 9.3 s'affiche avec l'étape 1 à « Fait », l'étape 2 à « À faire » et « Ouvrir les options pour les développeurs » focalisé

#### Scenario: retour avec le débogage activé
- **WHEN** l'utilisateur active le débogage puis revient dans SygixOs, sans Tsuyu
- **THEN** l'écran 8.4 s'affiche avec la ligne « Débogage » « Activé » et « Continuer » focalisé ; aucune fenêtre système n'est déclenchée avant OK sur « Continuer »

#### Scenario: autorisation acceptée
- **WHEN** l'utilisateur presse « Continuer » puis accepte la fenêtre système
- **THEN** la configuration démarre et l'écran « Configuration en cours » s'affiche

#### Scenario: autorisation refusée
- **WHEN** l'utilisateur refuse ou ferme la fenêtre système, ou que le délai d'autorisation expire
- **THEN** l'écran 8.11 s'affiche, rien n'est modifié, « Réessayer » a le focus et relance la demande

#### Scenario: débogage désactivé entre-temps
- **WHEN** le débogage a été désactivé entre l'écran 8.4 et la demande
- **THEN** l'écran 8.10 s'affiche, rien n'est modifié et « Ouvrir les options pour les développeurs » a le focus

#### Scenario: écrans système absents
- **WHEN** ni l'écran demandé ni son écran de repli ne peuvent s'ouvrir
- **THEN** le bouton ne change rien, sans crash, et les étapes restent affichées

**Détails.** — Guide du débogage : Le guide SHALL relire les états à l'ouverture et au retour, présenter deux étapes numérotées non focusables, « À faire »/« Fait » avec pastille verte, aide « Retour : précédent » et un seul bouton focusé. Avant options développeur activées : 9.2 « Ouvrir À propos » (sinon Paramètres) ; après : 9.3 « Ouvrir les options pour les développeurs » (sinon À propos). Au retour débogage activé, il SHALL avancer sans action. Cas « contenu des deux étapes » : quand « Activer le débogage » est affiché, la description est « Une option de la TV que SygixOs utilise pour se régler lui-même. Deux réglages à changer, puis revenez ici. » ; étape 1 « Affichez les options pour les développeurs », détail « Dans À propos, appuyez 7 fois sur la ligne du build (son nom varie selon la TV), puis revenez avec Retour. », faite quand le système confirme les options ; étape 2 « Activez le débogage USB », détail « Dans Options pour les développeurs, activez « Débogage USB », puis revenez avec Retour. », faite quand le débogage est activé. — Autorisation ADB guidée : Sans Tsuyu, l'étape 8.4 SHALL demander « Autoriser SygixOs », avec aide « Retour : précédent » et « Continuer » focusé déclenchant la demande. La ligne « Débogage » SHALL être « Activé », détail « Détecté au retour dans SygixOs » ; l'acceptation démarre la configuration. Cas « explications de l'autorisation » : quand 8.4 s'affiche, la description est « Le débogage est activé. Android va afficher la fenêtre « Autoriser le débogage USB ? » : SygixOs en a besoin pour se configurer. » ; étapes 1 « Cochez « Toujours autoriser sur cet ordinateur » » / « Sinon, Android vous le redemandera à chaque configuration. », 2 « Choisissez « Autoriser » » / « La configuration démarre aussitôt. ». — Erreurs du débogage : Refus ou expiration d'autorisation SHALL afficher 8.11, même titre/description que 8.4, ligne Débogage, boutons « Retour » et « Réessayer » focusé relançant la demande. Débogage désactivé entre-temps SHALL afficher 8.10, titre/description du guide, étapes relues, boutons « Retour » et « Ouvrir les options pour les développeurs » focusé. Cas « erreur d'autorisation » : quand l'autorisation est refusée ou expire, l'encadré « Autorisation refusée » dit « Android n'a pas autorisé SygixOs et rien n'a été modifié. Réessayez, cochez « Toujours autoriser sur cet ordinateur » puis choisissez « Autoriser ». ». Cas « débogage désactivé » : quand le débogage a été désactivé, l'encadré « Le débogage est désactivé » dit « Rien n'a été modifié. Activez « Débogage USB » dans les options pour les développeurs, puis revenez dans SygixOs. ».

### Requirement: Configuration automatique de l'écran d'accueil
La configuration automatique SHALL enchaîner selon p7-default-launcher D5 : autorisation constructeur, relevé de l'écran d'accueil précédent, définition de SygixOs comme HOME, puis vérification. « Configuration en cours » SHALL relire chaque étape (fait/en cours/à venir) et SHALL survivre à la fermeture du panneau jusqu'à la fin ou au délai ; rouvert, il affiche l'état relu. Succès, secours, échec et annulation SHALL suivre les scénarios ci-dessous.

#### Scenario: configuration sans désactivation
- **WHEN** l'accès est prêt et que SygixOs devient l'écran d'accueil effectif après la définition de l'écran d'accueil
- **THEN** « Configuration en cours » montre les étapes passer à « Fait » une à une, puis « SygixOs est votre écran d'accueil » s'affiche avec « Terminé » focalisé, sans confirmation ni désactivation, et la voie automatique est « Active »

#### Scenario: désactivation proposée
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif après la vérification et que l'accueil d'origine peut être désactivé
- **THEN** l'écran 9.11 s'affiche avec « Garder l'accueil Google TV (conseillé) » focalisé ; aucun composant n'est modifié

#### Scenario: ne pas désactiver
- **WHEN** l'utilisateur presse OK sur « Garder l'accueil Google TV (conseillé) » ou Retour sur 9.11
- **THEN** aucun composant n'est désactivé, le service d'accessibilité est ajouté sans retirer les autres, la superposition est accordée, l'option de démarrage est activée, chaque réglage est relu, et « SygixOs garde la main » s'affiche avec « Terminé » focalisé

#### Scenario: désactivation demandée
- **WHEN** l'utilisateur presse bas puis OK sur 9.11
- **THEN** la confirmation 8.8 s'affiche avec « Annuler » focalisé

#### Scenario: désactivation impossible
- **WHEN** SygixOs n'est pas l'écran d'accueil effectif et qu'aucun composant de l'accueil d'origine n'est actif et modifiable, ou que les garde-fous l'interdisent
- **THEN** 9.11 n'est pas affiché et les réglages de secours sont posés, puis « SygixOs garde la main » s'affiche

#### Scenario: résultat avec Tsuyu
- **WHEN** la séquence s'est exécutée par Tsuyu et que les réglages de secours sont posés
- **THEN** l'écran 9.12 affiche la ligne « Réparation automatique » à « Active », le « Bon à savoir » propre à Tsuyu et aucun conseil de désactiver le débogage

#### Scenario: échec d'une étape
- **WHEN** la définition de l'écran d'accueil échoue (erreur, délai ou relecture qui ne la confirme pas)
- **THEN** 8.12 s'affiche avec « Démarrage automatique » « Fait », « Écran d'accueil » « Échec », « Vérification » « Annulée », l'encadré « L'écran d'accueil n'a pas pu être réglé » et « Réessayer » focalisé ; les étapes déjà faites restent en place

#### Scenario: panneau fermé pendant la configuration
- **WHEN** l'utilisateur presse Retour ou la touche Home pendant « Configuration en cours »
- **THEN** le panneau se ferme, la séquence continue jusqu'à sa fin ou son délai, et à la réouverture l'écran correspondant à l'état relu s'affiche

**Détails.** — Progression de la configuration : 9.10 SHALL afficher « Configuration en cours », étapes relues une à une, sans bouton, aide « Patientez… ». Les états SHALL être « Fait » vert, « En cours… » bleu ou « À venir ». Les résultats/échecs SHALL porter le repère « Réglages » ; leurs écrans à deux boutons naviguent gauche/droite sans boucle, haut/bas sans effet. Cas « contenu de la progression » : quand la séquence de configuration s'exécute, la description est « SygixOs règle la TV. Ne l'éteignez pas pendant la configuration. » ; les lignes sont « Démarrage automatique » / « La TV laisse SygixOs démarrer avec elle » seulement si cette autorisation existe, « Écran d'accueil » / « SygixOs devient l'écran d'accueil d'Android », « Vérification » / « SygixOs vérifie que la TV l'ouvre comme écran d'accueil ». — Résultat de configuration effectif : Accueil effectif vérifié, le résultat SHALL afficher « SygixOs est votre écran d'accueil », les étapes faites et le conseil sans Tsuyu ou la réparation active avec lui. Aide « Retour : fermer », boutons « Détails » et « Terminé » focusé SHALL suivre « Progression de la configuration ». Cas « succès et lignes » : quand SygixOs est devenu l'accueil effectif, la description est « SygixOs a vérifié que la TV l'ouvre maintenant comme écran d'accueil. » ; « Démarrage automatique » / « La TV laisse SygixOs démarrer avec elle » et « Écran d'accueil » / « La TV ouvre SygixOs comme écran d'accueil » sont « Fait » ; sans Tsuyu, « Conseil : désactivez le débogage » dit « SygixOs n'en a plus besoin. Réactivez-le pour relancer la configuration ou rétablir l'accueil Google TV. » tant qu'activé ; avec Tsuyu actif (9.13), « Réparation automatique » est « Active », détail « Tsuyu permet à SygixOs de refaire ses réglages si la TV les défait. Gardez le débogage activé. ». — Dernier choix de configuration : Si l'accueil d'origine reste actif/modifiable et les garde-fous le permettent, 9.11 SHALL proposer « Un dernier choix », deux lignes sans état, garder conseillé focusé puis désactiver ouvrant 8.8, aide « Retour : garder ». Haut/bas SHALL passer entre elles sans boucle, gauche/droite sans effet. Cas « contenu du choix » : quand 9.11 est affiché, la description est « Sur cette TV, l'accueil Google TV reste l'écran d'accueil du système. SygixOs peut le garder ou le remplacer complètement. Vous pourrez changer d'avis dans Réglages › Écran d'accueil. » ; « Garder l'accueil Google TV (conseillé) » dit « La touche Home et l'allumage ouvrent SygixOs. Toutes les touches de la télécommande continuent de marcher. » ; « Désactiver l'accueil Google TV » dit « SygixOs le remplace complètement. Selon la TV, des touches de la télécommande ne marchent plus. ». — Réglages de secours automatiques : Garder ou Retour en 9.11, désactivation impossible ou échouée/annulée SHALL activer le service d'accessibilité en conservant les autres, accorder la superposition et activer « Démarrage à l'allumage », chacun relu, puis montrer « SygixOs garde la main ». Aucun succès SHALL être supposé. Cas « résultat de secours » : quand les réglages de secours ont été relus, « SygixOs garde la main » décrit « L'accueil Google TV reste actif : SygixOs a réglé la touche Home et l'ouverture au démarrage. » ; « Touche Home » / « Le service de SygixOs ramène à SygixOs », « Superposer aux autres applications » / « Permet à SygixOs de s'ouvrir au démarrage de la TV », « Ouverture à l'allumage » / « SygixOs demande à s'ouvrir au démarrage de la TV » montrent leurs états ; aide « Retour : fermer », boutons « Détails » et « Terminé » focusé. Cas « indications de réparation du secours » : quand ce résultat s'affiche avec ou sans Tsuyu actif, avec Tsuyu (9.12), « Réparation automatique » est « Active », détail « Tsuyu permet à SygixOs de refaire ces réglages si la TV les défait. Gardez le débogage activé. », puis « Bon à savoir » / « Si la touche Home ne ramène plus à SygixOs après un redémarrage, ouvrez SygixOs une fois : il refait ses réglages. » ; sans Tsuyu, « Bon à savoir » dit « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. ». — Échec de configuration et état automatique : Une étape en échec SHALL arrêter les dépendantes et afficher 8.12, sans réessai sans action. La voie automatique SHALL être « Active » si accueil effectif ou secours en place, « Indisponible sur cette TV » selon l'accès, sinon « À configurer », sur première page et panneau Launcher. Cas « écran interrompu » : quand une étape de configuration échoue, « Configuration interrompue » dit « Une étape n'a pas abouti. Ce qui est fait reste en place ; vous pouvez réessayer. » ; erreur propre à l'étape, pour HOME « L'écran d'accueil n'a pas pu être réglé » / « La touche Home ouvre encore l'accueil Google TV. Vérifiez que le débogage est toujours activé, puis réessayez. » ; étapes « Fait », « Échec » rouge ou « Annulée » / « Non effectuée » ; aide « Retour : fermer », boutons « Fermer » et « Réessayer » focusé.

### Requirement: Page Par autorisations
La page 8.13 SHALL afficher les lignes dans l'ordre suivant : touche Home, démarrage à l'allumage, superposition si requise, démarrage automatique si le constructeur en déclare un, puis « Bon à savoir ». OK SHALL ouvrir le réglage Android correspondant sans activation implicite ; au retour, états relus et focus sur la ligne ouvrante. Chaque ligne SHALL montrer à droite son état réel, jamais une réussite supposée. Navigation et bouton « Terminé » SHALL suivre les scénarios ci-dessous.

#### Scenario: ouverture de la page
- **WHEN** l'utilisateur presse OK sur « Par autorisations (sans débogage) » sur une TV sans gestionnaire d'auto-démarrage du constructeur, rien n'étant fait
- **THEN** la page 8.13 s'affiche avec « Ouvrir SygixOs avec la touche Home » et « Ouvrir SygixOs à l'allumage » à « À faire », sans « Autoriser le démarrage automatique », puis « Bon à savoir » et « Terminé » ; la première ligne a le focus

#### Scenario: retour d'un réglage Android
- **WHEN** l'utilisateur a ouvert un réglage Android depuis une ligne puis revient dans SygixOs
- **THEN** le panneau 8.13 est toujours affiché, l'état de la ligne est relu (plus jamais « En attente… » d'un écran fermé) et la ligne a le focus

#### Scenario: écran du constructeur
- **WHEN** l'appareil a un gestionnaire d'auto-démarrage du constructeur et déclare un écran retenu
- **THEN** « Autoriser le démarrage automatique » est affichée après « Ouvrir SygixOs à l'allumage » et OK ouvre cet écran

#### Scenario: réglage du constructeur indisponible
- **WHEN** l'appareil a un gestionnaire d'auto-démarrage du constructeur sans écran retenu, comme la TV de référence
- **THEN** « Autoriser le démarrage automatique » est affichée « Indisponible » avec la raison « Pas de réglage sur cette TV : seule la configuration automatique peut l'autoriser », reste focalisable, et OK n'ouvre rien

**Détails.** — Lignes de la page Par autorisations : 8.13 SHALL afficher Home, allumage, permission de démarrage conditionnelle puis autorisation constructeur si détectée (D8). Sans action, la page ne SHALL rien activer. Après les lignes, « Bon à savoir » non focusable, aide « Retour : fermer » et « Terminé » SHALL être présents. Cas « texte de la page » : quand « Par autorisations » s'affiche, sa description est « Pour chaque ligne, OK ouvre le réglage Android à changer. Activez SygixOs, puis revenez ici : l'état se met à jour tout seul. » ; les lignes sont « Ouvrir SygixOs avec la touche Home », « Ouvrir SygixOs à l'allumage », « Autoriser l'ouverture au démarrage » si option activée et superposition manquante, et « Autoriser le démarrage automatique » si gestionnaire détecté ; « Bon à savoir » dit « Sur certaines TV, après un redémarrage complet, l'accueil Google TV peut reprendre la main ; SygixOs vous le signale. ». Cas « ligne du constructeur » : quand le gestionnaire de démarrage constructeur est détecté, avec écran retenu, le détail est « OK : ouvre l'écran de démarrage automatique de la TV », « À faire » jusqu'à une ouverture observée après sa consultation puis « Fait » ; sans écran, « Indisponible » explique « Pas de réglage sur cette TV : seule la configuration automatique peut l'autoriser ». — Navigation des autorisations : Focus initial SHALL être première ligne non « Fait », sinon « Terminé ». Haut/bas SHALL parcourir les lignes focusables puis Terminé sans boucle ; gauche/droite sans effet. États à droite : « À faire », « En attente… » dans Android/vérification, « Fait », « Bloqué » pour Home seulement, « Indisponible » avec raison à la place du détail, focusable et OK sans effet. Cas « déplacement et retour système » : quand la page est parcourue ou Android est fermé, « ouverture de la page » et « retour d'un réglage Android » de « Page Par autorisations » gardent le panneau et restaurent la ligne ouvrante. — État de la voie par autorisations : Sur la première page et le panneau Launcher, la voie SHALL être « Active » si toutes les lignes affichées/disponibles sont « Fait », « Bloquée » si Home est « Bloqué », « En partie » si au moins une est faite, « Non utilisée » si aucune faite et voie automatique active, sinon « À faire ». Cas « synthèse de la voie » : quand les états des autorisations sont relus, la première page et le panneau affichent cette synthèse sans modifier les réglages ni contourner un refus.

### Requirement: Panneau Launcher principal
« Configurer SygixOs comme launcher principal » SHALL ouvrir « Launcher principal » (9.14/9.15/8.21) au-dessus des réglages, avec les lignes Automatique, Tsuyu et Par autorisations (état et détail), puis Relancer, Remplacer et Rétablir selon les conditions. Focus initial : relancer ; haut/bas voies puis actions sans boucle, gauche/droite sans effet. Retour/fermeture SHALL rendre le focus à la ligne des réglages sans changer le drapeau du parcours automatique.

#### Scenario: ouverture depuis les réglages
- **WHEN** l'accueil d'origine a été désactivé par la configuration automatique sans Tsuyu, que Tsuyu n'est pas disponible et que l'utilisateur presse OK sur la ligne des réglages
- **THEN** le panneau s'affiche, sans ligne Tsuyu, avec « Configuration automatique » « Active », « Par autorisations » « Non utilisée », « Relancer la configuration » focalisé et « Rétablir l'accueil Google TV » affiché

#### Scenario: configuré avec Tsuyu
- **WHEN** la configuration automatique a été faite par Tsuyu en gardant l'accueil Google TV et que Tsuyu est « Actif »
- **THEN** le panneau 9.14 affiche « Configuration automatique » « Active » (« Accueil Google TV gardé · touche Home et allumage réglés »), « Tsuyu » « Actif », « Par autorisations » « Non utilisée », « Relancer la configuration » (« Refait les réglages automatiques ») focalisé et « Remplacer complètement l'accueil Google TV », sans « Rétablir l'accueil Google TV »

#### Scenario: Tsuyu à installer
- **WHEN** la configuration automatique a été faite sans Tsuyu en gardant l'accueil Google TV et qu'une release de Tsuyu est connue
- **THEN** le panneau 9.15 affiche « Tsuyu » « À installer » (« Pour que SygixOs se répare tout seul ») ; OK sur cette ligne ouvre les écrans du débogage si le débogage est désactivé, sinon 9.4

#### Scenario: rien de configuré
- **WHEN** aucune voie n'est « Active », que l'accueil d'origine est actif et que Tsuyu n'est pas disponible
- **THEN** le panneau 8.21 affiche « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. », « Configuration automatique » « À configurer » (« Pas encore configurée »), « Par autorisations » « À faire » (« Aucune autorisation donnée »), « Relancer la configuration » focalisé, sans « Remplacer complètement l'accueil Google TV » ni « Rétablir l'accueil Google TV »

#### Scenario: fermeture
- **WHEN** l'utilisateur presse Retour sur le panneau
- **THEN** il se ferme et la ligne des réglages a le focus avec son état recalculé

**Détails.** — Synthèse du panneau Launcher : Le panneau SHALL afficher « Configuration automatique », « Tsuyu » seulement si disponible, puis « Par autorisations », états et détails de synthèse. Une voie active SHALL faire décrire SygixOs comme accueil ; sinon comme pas encore accueil. La ligne Tsuyu SHALL refléter « États de Tsuyu » de tsuyu-access, avec les détails ci-dessous. Cas « description et synthèses » : quand le panneau « Launcher principal » s'ouvre, une voie active donne « SygixOs est l'écran d'accueil de la TV. Voici l'état de chaque façon de le configurer. », sinon « SygixOs n'est pas encore l'écran d'accueil. Voici l'état de chaque façon de le configurer. » ; exemples de détails : « Accueil Google TV désactivé · SygixOs est l'écran d'accueil », « Accueil Google TV gardé · touche Home et allumage réglés », « Pas encore configurée », « Pas utilisée sur cette TV », « Aucune autorisation donnée ». Cas « détails de Tsuyu » : quand la ligne Tsuyu est présente, Actif dit « Répare les réglages si la TV les défait », À installer « Pour que SygixOs se répare tout seul », Arrêté « OK : ouvre Tsuyu pour le démarrer », À autoriser « OK : autorise SygixOs dans Tsuyu », À mettre à jour « Version trop ancienne pour SygixOs », Indisponible sur cette TV « Le débogage de cette TV ne permet pas à Tsuyu de démarrer ». — Actions du panneau Launcher : Le panneau SHALL proposer relancer, remplacer si voie active/accueil actif modifiable/garde-fous tenus, rétablir si un composant est désactivé par l'utilisateur. OK sur automatique/relancer SHALL suivre le parcours via Tsuyu actif sinon sans ; autorisations ouvre 8.13, remplacer 8.8, rétablir sa confirmation. L'aide SHALL être « Retour : fermer ». Cas « libellés des actions » : quand les actions sont affichées, « Relancer la configuration » dit « Refait les réglages automatiques » avec Tsuyu actif, sinon « Refait les réglages automatiques (débogage nécessaire) » ; « Remplacer complètement l'accueil Google TV » dit « Le désactive : selon la TV, des touches de la télécommande ne marchent plus » ; « Rétablir l'accueil Google TV » dit « Le réactive et lui rend l'écran d'accueil ». Cas « action de la ligne Tsuyu » : quand OK est pressé sur Tsuyu, À installer/À mettre à jour ouvre débogage si besoin puis 9.4, Arrêté 9.7, À autoriser 9.8, Actif Tsuyu, Indisponible sur cette TV reste sans effet.

### Requirement: État de l'écran d'accueil
SygixOs SHALL rester candidat HOME et relire sans débogage les états système au retour au premier plan, à l'ouverture du parcours/panneau et après chaque opération : écran d'accueil effectif, rôle HOME, composants de l'accueil d'origine, fin de configuration, service et superposition. Les valeurs relues SHALL remplacer les anciennes ; une erreur ne SHALL jamais produire « Active ». L'autorisation constructeur illisible garde la dernière relecture.

#### Scenario: configuration défaite hors de SygixOs
- **WHEN** l'utilisateur ou le système réactive l'accueil d'origine ou change l'écran d'accueil pendant que SygixOs est en arrière-plan
- **THEN** au retour au premier plan, les états sont recalculés, sans réapplication ni rappel automatique

#### Scenario: accueil désactivé par la procédure ADB
- **WHEN** SygixOs se lance alors que l'accueil d'origine a été désactivé par la procédure ADB du README et que SygixOs est l'écran d'accueil effectif
- **THEN** la voie automatique est « Active » et le panneau « Launcher principal » propose « Rétablir l'accueil Google TV »

#### Scenario: erreur de lecture
- **WHEN** la lecture de l'état d'un composant ou de l'écran d'accueil échoue
- **THEN** aucune voie n'est déclarée « Active » sur cette lecture, aucun crash n'a lieu et la lecture est refaite au prochain retour au premier plan

**Détails.** — Lectures de l'état du launcher : Les lectures SHALL couvrir écran d'accueil effectif, rôle HOME, composants d'origine (actif, désactivé par l'utilisateur, hors de portée), configuration initiale terminée, accessibilité (activée, connectée) et superposition. SygixOs SHALL être effectif seulement si HOME se résout vers lui et qu'aucune autre activité HOME activée n'a de priorité supérieure. Cas « résolution et état relus » : quand une lecture est déclenchée selon « État de l'écran d'accueil », ces champs sont relus ; « configuration défaite hors de SygixOs », « accueil désactivé par la procédure ADB » et « erreur de lecture » restent applicables.

### Requirement: Désactivation de l'accueil d'origine
La désactivation SHALL venir seulement de 9.11 ou du panneau Launcher, après confirmation et garde-fous relus (accès prêt, configuration terminée, activité HOME activée, un composant actif et modifiable). Elle SHALL désactiver les composants un par un puis vérifier l'écran d'accueil effectif ; un échec ou une vérification négative SHALL les remettre à leur état par défaut dans l'ordre inverse, sans laisser la TV sans accueil actif.

#### Scenario: confirmation
- **WHEN** l'utilisateur presse OK sur « Désactiver l'accueil Google TV » en 9.11
- **THEN** le dialogue 8.8 s'affiche, sans mascotte, avec « Annuler » focalisé ; aucun composant n'est modifié avant OK sur « Désactiver »

#### Scenario: annulation
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.8 ouvert depuis 9.11
- **THEN** le dialogue se ferme, rien n'est modifié et 9.11 a le focus sur « Désactiver l'accueil Google TV »

#### Scenario: désactivation réussie
- **WHEN** l'utilisateur presse « Désactiver », que chaque composant actif est désactivé et que SygixOs est ensuite l'écran d'accueil effectif
- **THEN** l'écran « SygixOs est votre écran d'accueil » s'affiche et l'état relu de chaque composant est « désactivé par l'utilisateur »

#### Scenario: remplacement depuis le panneau
- **WHEN** Tsuyu est « Actif », que l'utilisateur presse « Remplacer complètement l'accueil Google TV » puis « Désactiver » et que la désactivation réussit
- **THEN** aucun écran du débogage n'est affiché, le panneau revient avec « Configuration automatique » « Active », « Rétablir l'accueil Google TV » affiché et focalisé, et sans « Remplacer complètement l'accueil Google TV »

#### Scenario: désactivation partielle annulée
- **WHEN** la désactivation d'un composant échoue ou dépasse son délai, ou que SygixOs n'est pas l'écran d'accueil effectif après la désactivation
- **THEN** les composants désactivés par cette opération sont remis à leur état par défaut dans l'ordre inverse, l'état est relu, aucun succès n'est affiché, et les réglages de secours sont posés depuis 9.11, ou l'encadré « L'accueil Google TV n'a pas pu être désactivé » s'affiche depuis le panneau

#### Scenario: appareil pas encore configuré
- **WHEN** la configuration initiale de l'appareil n'est pas terminée
- **THEN** 9.11 n'est pas affiché, « Remplacer complètement l'accueil Google TV » n'est pas proposé et aucune désactivation n'est exécutée

**Détails.** — Confirmation de désactivation : Seules la ligne « Désactiver l'accueil Google TV » en 9.11 et l'action « Remplacer complètement l'accueil Google TV » SHALL ouvrir 8.8. « Annuler » SHALL être focusé, puis « Désactiver », aide « Retour : annuler » ; gauche/droite sans boucle, haut/bas sans effet. Retour/Annuler SHALL rendre l'écran et le focus ouvrants sans modification. Cas « avertissement explicite » : quand « Désactiver l'accueil Google TV ? » s'affiche, le texte est « SygixOs deviendra l'écran d'accueil. Selon la TV, des fonctions de l'accueil Google TV peuvent disparaître : réglages rapides, guide des programmes, appui long sur Home, choix du profil. Rétablissez-le dans Réglages › Écran d'accueil, et avant de désinstaller SygixOs. ». — Garde-fous de désactivation : Juste avant l'opération, SygixOs SHALL relire : accès prêt, configuration initiale de la TV terminée, activité HOME SygixOs déclarée et activée, au moins un composant d'origine actif/modifiable. Toute condition non tenue SHALL empêcher toute désactivation. Cas « appareil pas encore prêt » : quand une condition manque, « appareil pas encore configuré » de « Désactivation de l'accueil d'origine » et les garde-fous de p7-default-launcher D6 empêchent l'opération. — Séquence de désactivation : Selon p7-default-launcher D6, les composants actifs SHALL être désactivés un par un dans l'ordre de l'ensemble, chacun relu, puis HOME effectif vérifié. La progression SHALL être « Configuration en cours » avec ligne « Accueil Google TV ». Succès depuis 9.11 : résultat effectif ; depuis panneau : états recalculés, focus « Rétablir l'accueil Google TV ». Cas « opération réussie » : quand tous les composants et l'accueil effectif sont confirmés, « désactivation réussie » et « remplacement depuis le panneau » de « Désactivation de l'accueil d'origine » s'appliquent. — Annulation d'une désactivation échouée : Échec, délai ou HOME non confirmé SHALL remettre par défaut les composants touchés par cette opération, dans l'ordre inverse, puis relire. Depuis 9.11, les secours SHALL suivre ; depuis panneau, 8.12 SHALL afficher l'échec, sans laisser la TV sans accueil activé. Cas « retour arrière de l'opération » : quand la désactivation échoue depuis le panneau, l'encadré dit « L'accueil Google TV n'a pas pu être désactivé », détail « Il est resté actif et rien d'autre n'a changé. Vérifiez l'accès, puis réessayez. » ; le scénario « désactivation partielle annulée » de « Désactivation de l'accueil d'origine » reste applicable.

### Requirement: Rétablissement de l'accueil d'origine
OK sur « Rétablir l'accueil Google TV », accès prêt, SHALL ouvrir le dialogue de confirmation ; confirmé, le rétablissement SHALL remettre les composants désactivés à leur état par défaut dans l'ordre inverse, puis redéfinir l'écran d'accueil précédent s'il a été relevé, présent et activé, le tout relu. Autorisations, service d'accessibilité et Tsuyu SHALL rester inchangés. Succès : panneau relu, focus « Relancer la configuration » ; échec : « L'accueil Google TV n'a pas pu être rétabli ».

#### Scenario: rétablissement réussi
- **WHEN** l'utilisateur presse « Rétablir » et que les composants sont remis à leur état par défaut et l'écran d'accueil précédent redéfini
- **THEN** le panneau revient sans « Rétablir l'accueil Google TV », « Relancer la configuration » a le focus et la voie automatique n'est plus « Active » si aucun réglage de secours n'est en place

#### Scenario: rétablissement par Tsuyu
- **WHEN** Tsuyu est « Actif », le débogage n'ayant pas été réactivé par l'utilisateur depuis la configuration
- **THEN** OK sur « Rétablir l'accueil Google TV » affiche directement 8.17, sans écran du débogage ni fenêtre système

#### Scenario: rétablissement annulé
- **WHEN** l'utilisateur presse « Annuler » ou Retour dans 8.17
- **THEN** rien n'est modifié et « Rétablir l'accueil Google TV » a le focus

#### Scenario: sans écran d'accueil précédent relevé
- **WHEN** l'accueil d'origine a été désactivé par la procédure ADB du README et qu'aucun écran d'accueil précédent n'a été relevé
- **THEN** seuls les composants sont remis à leur état par défaut, et le système réattribue l'écran d'accueil

#### Scenario: échec du rétablissement
- **WHEN** la remise à l'état par défaut d'un composant échoue ou dépasse son délai
- **THEN** l'encadré « L'accueil Google TV n'a pas pu être rétabli » s'affiche avec « Réessayer » focalisé et l'état relu, sans succès supposé

**Détails.** — Confirmation du rétablissement : Le dialogue 8.17 SHALL demander « Rétablir l'accueil Google TV ? », proposer « Annuler » et « Rétablir » focalisé, aide « Retour : annuler », avec navigation de 8.8. Son texte SHALL expliquer la portée exacte du rétablissement. Cas « texte du dialogue » : quand le dialogue de rétablissement est affiché, il dit « L'accueil Google TV sera réactivé et redeviendra l'écran d'accueil. Les autres réglages faits par SygixOs restent en place : si son service reste activé, la touche Home ouvrira encore SygixOs. » ; « rétablissement annulé » de « Rétablissement de l'accueil d'origine » préserve la TV. — Opération de rétablissement : Confirmé, le rétablissement SHALL remettre par défaut les composants désactivés par l'utilisateur dans l'ordre inverse de l'ensemble ; si l'accueil précédent relevé est présent et activé, le redéfinir. Tout SHALL être relu avant le résultat, sans toucher les autres réglages. Cas « avec ou sans accueil précédent » : quand l'utilisateur confirme le rétablissement, « rétablissement réussi », « rétablissement par Tsuyu », « sans écran d'accueil précédent relevé » et « échec du rétablissement » de « Rétablissement de l'accueil d'origine » s'appliquent.

### Requirement: Démarrage automatique du constructeur
La configuration automatique SHALL accorder au seul paquet SygixOs `AUTO_START`, ou `APP_AUTO_START` si seul disponible, enregistrer les opérations pour persistance puis relire. Déjà accordée : aucune écriture ; absente : étape sans objet et ligne masquée. Refus, délai ou relecture négative ne SHALL produire ni succès ni réessai sans action. Cette autorisation ne SHALL garantir aucun démarrage.

#### Scenario: autorisation accordée
- **WHEN** l'opération existe avec une valeur autre que l'autorisation et que la séquence l'accorde
- **THEN** la relecture la donne autorisée et la ligne « Démarrage automatique » passe à « Fait »

#### Scenario: déjà accordée
- **WHEN** l'opération est déjà autorisée pour SygixOs
- **THEN** aucune écriture n'a lieu et la ligne est « Fait »

#### Scenario: relecture négative
- **WHEN** l'écriture semble réussir mais que la relecture ne donne pas l'opération autorisée
- **THEN** l'étape est en échec et la séquence continue avec l'écran d'accueil

### Requirement: Opérations privilégiées limitées
ADB intégré et Tsuyu SHALL n'exécuter que les opérations/arguments fixes de p7-default-launcher D2 vers SygixOs, son service ou l'ensemble fermé de l'accueil d'origine ; toute autre cible est refusée avant envoi. Aucune commande externe ni révocation d'autorisation ne SHALL être exécutée. Les autres services d'accessibilité SHALL être conservés ; le retrait de celui de SygixOs n'est permis que pour la réparation Tsuyu. Aucune connexion privilégiée ne SHALL rester ouverte hors séquence demandée.

#### Scenario: cible refusée
- **WHEN** une opération est demandée avec un paquet ou un composant hors du paquet SygixOs et de l'ensemble fermé
- **THEN** elle est refusée sans être envoyée, par l'ADB intégré comme par Tsuyu, et l'échec est rapporté

#### Scenario: service d'accessibilité ajouté
- **WHEN** la configuration active le service d'accessibilité de SygixOs alors que d'autres services sont activés
- **THEN** la liste relue contient les autres services inchangés et celui de SygixOs

#### Scenario: service relié par la réparation
- **WHEN** la réparation automatique retire puis remet le service de SygixOs
- **THEN** la liste relue après l'opération contient les autres services inchangés et celui de SygixOs

#### Scenario: fin de séquence
- **WHEN** une séquence ou une réparation se termine, réussie, en échec ou par délai
- **THEN** la connexion au débogage est fermée, ou le service de SygixOs lancé par Tsuyu est arrêté

**Détails.** — Clé et connexion ADB locale : L'ADB SHALL se connecter au bouclage sur le port réseau de l'appareil seulement, après action explicite dans le parcours, puis fermer à la fin de la séquence. La paire de clés propre à SygixOs SHALL être générée à la première connexion, conservée en stockage privé, autorisée dans « Autoriser le débogage USB ? » et ne jamais sortir de l'appareil. Cas « autorisation et fermeture » : quand le parcours utilise l'ADB intégré, « clé déjà autorisée » de « Accès par le débogage » et « fin de séquence » de « Opérations privilégiées limitées » s'appliquent sans connexion à un autre appareil. — Conservation des services et accès privilégiés : Les autres services d'accessibilité activés SHALL être conservés. Retirer puis remettre celui de SygixOs ne SHALL être permis que pour la réparation automatique de tsuyu-access. Aucune connexion privilégiée ni service SygixOs lancé par Tsuyu ne SHALL rester ouvert hors séquence utilisateur ou réparation automatique. Cas « service et fin de séquence » : quand une opération privilégiée touche le service ou se termine, « service d'accessibilité ajouté », « service relié par la réparation » et « fin de séquence » de « Opérations privilégiées limitées » s'appliquent.

### Requirement: Détails des modifications
« Détails » des écrans de succès/secours SHALL ouvrir « Ce que SygixOs a modifié » (8.14/9.16), sans mascotte. Le panneau SHALL suivre les scénarios ci-dessous : lignes non focusables des réglages réellement modifiés avec états relus, accueil Google TV toujours, Tsuyu et réparations selon les cas, aide « Retour : fermer » et bouton « Fermer » focalisé rendant le focus à « Détails ».

#### Scenario: détails après les réglages de secours
- **WHEN** l'utilisateur presse OK sur « Détails » sur « SygixOs garde la main » sans Tsuyu
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil », « Superposer aux autres applications » et « Touche Home » à l'état « Fait », et « Accueil Google TV » à l'état « Inchangé » ; « Fermer » a le focus et rend le focus à « Détails »

#### Scenario: détails avec Tsuyu
- **WHEN** l'utilisateur presse OK sur « Détails » après une configuration par Tsuyu installé par SygixOs, une réparation ayant eu lieu
- **THEN** 9.16 liste aussi « Tsuyu » « Fait » et « Réparations automatiques » « Active » avec la date et le réglage de la dernière réparation

#### Scenario: détails après désactivation
- **WHEN** l'utilisateur presse OK sur « Détails » sur « SygixOs est votre écran d'accueil » après une désactivation sans Tsuyu
- **THEN** 8.14 liste « Démarrage automatique », « Écran d'accueil » et « Accueil Google TV » « Désactivé à votre demande », sans les réglages de secours

**Détails.** — Lignes des modifications : Le panneau SHALL montrer les réglages réellement modifiés, l'accueil Google TV toujours, Tsuyu seulement installé par SygixOs et les réparations si configuration via Tsuyu. Les lignes SHALL être non focusables avec états relus, selon les détails ci-dessous, sans prétendre que d'autres réglages ont changé. Cas « contenu du panneau de détails » : quand « Ce que SygixOs a modifié » est affiché, la description est « Rien d'autre n'a été touché. « Rétablir l'accueil Google TV » ne concerne que l'accueil ; les autorisations se retirent dans les paramètres Android. » ; les lignes sont « Démarrage automatique » / « La TV laisse SygixOs démarrer avec elle », « Écran d'accueil » / « SygixOs défini comme écran d'accueil d'Android », « Superposer aux autres applications » / « Permet à SygixOs de s'ouvrir au démarrage de la TV », « Touche Home » / « Le service de SygixOs ne lit que la touche Home ». Cas « accueil et réparations dans les détails » : quand les lignes d'accueil ou Tsuyu sont présentes, « Accueil Google TV » dit « Toujours actif : désactivé seulement si vous le choisissez » avec « Inchangé », sinon « Désactivé à votre demande » avec « Fait » ; « Tsuyu » dit « Installé par SygixOs : il garde l'accès au débogage » avec « Fait » ; « Réparations automatiques » dit « Dernière : <date> · <réglage> » ou « Aucune pour l'instant », « Active » si Tsuyu « Actif », sinon « Arrêtée ».

### Requirement: Débogage après la configuration
Après succès sans Tsuyu, « Conseil : désactivez le débogage » SHALL rester tant qu'il est activé, relu au retour ; reconfiguration/rétablissement exigent sa réactivation (écrans du débogage et 8.4). Avec Tsuyu, aucun conseil de désactivation : « Réparation automatique » SHALL demander de le garder activé car Tsuyu en dépend. SygixOs ne SHALL jamais activer ni désactiver le débogage lui-même.

#### Scenario: conseil masqué
- **WHEN** l'utilisateur désactive le débogage puis revient dans SygixOs sur « SygixOs est votre écran d'accueil »
- **THEN** la ligne « Conseil : désactivez le débogage » n'est plus affichée

#### Scenario: pas de conseil avec Tsuyu
- **WHEN** la configuration s'est faite par Tsuyu
- **THEN** ni 9.12 ni 9.13 n'affichent « Conseil : désactivez le débogage » et la ligne « Réparation automatique » est affichée

### Requirement: Qualité des affiches Top Shelf
Le Top Shelf SHALL accepter une image paysage d'au moins 640 px de large réels, à partir des dimensions de « Chargement partagé des images », quelle que soit la largeur du panneau affiché. Le refus pour le panneau ne SHALL pas interdire l'usage de la même image dans le héro ou une carte selon leurs propres politiques.

#### Scenario: seuil exact du panneau
- **WHEN** une image paysage fait exactement 640 px de large réels sur un panneau plus large
- **THEN** elle est acceptée et décodée pour couvrir le panneau ; à 639 px ou en portrait elle est refusée pour le panneau, même si une autre destination l'accepte

### Requirement: Repli du programme du héro
Quand la récupération de ses candidates échoue (erreur ou délai de requête dépassé) sans copie locale utilisable, le héro SHALL afficher le programme demandé avec la vidéo nature de « Fond vidéo de secours », puis le fond neutre si aucun clip n'est lisible dans le délai borné. Le fond neutre SHALL couvrir la préparation du clip sans noir ; titre et ouverture restent ceux du programme, et navigation/défilement continuent.

#### Scenario: fournisseur et réseau indisponibles
- **WHEN** image et vidéos nature sont indisponibles pour un programme toujours valide
- **THEN** le fond neutre local porte son titre et son bouton ; le programme suivant d'une autre app reste accessible sans attendre un lot d'images

### Requirement: Ordre aléatoire du héro
L'ordre des programmes du héro SHALL être tiré au hasard au démarrage du processus et rester stable pendant la session. Un retour au premier plan ou une relecture ne SHALL pas remélanger les programmes présents. Un nouveau programme SHALL être inséré au hasard après le programme affiché, sans réordonner les autres ; un programme disparu ou expiré SHALL être retiré en préservant leur ordre relatif.

#### Scenario: nouveau lancement
- **WHEN** SygixOs est relancé (nouveau processus) avec les mêmes programmes publiés
- **THEN** l'ordre du héro est tiré de nouveau au hasard ; le premier programme n'est pas toujours le même

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient sur SygixOs pendant la même session, avec ou sans rechargement du TV Provider
- **THEN** les programmes déjà présents gardent leur ordre

#### Scenario: programme ajouté pendant la session
- **WHEN** une app publie un nouveau programme pendant la session
- **THEN** il est inséré à une position tirée au hasard après le programme affiché, sans réordonner les autres

#### Scenario: retrait, expiration et liste vide
- **WHEN** un programme est retiré ou expire, puis que la liste devient vide
- **THEN** l'ordre relatif des programmes restants est conservé, puis le fallback global s'affiche sans ancien titre ; aucun ordre n'est reconstruit à partir du cache d'images

#### Scenario: programme republié
- **WHEN** une app republie un programme identique sous un nouvel identifiant de ligne
- **THEN** il garde sa place dans l'ordre ; il n'est pas traité comme un nouveau programme

### Requirement: Images d'autres applications sans dépendance forte
SygixOs SHALL accéder aux images `content://` tierces sans dépendance forte au fournisseur et sans exiger qu'il déclare un type d'image. L'ouverture et la lecture complète SHALL avoir un délai de requête borné, même si le fournisseur ignore l'annulation ; l'annulation SHALL fermer le descripteur et le flux déjà ouverts. Une panne ne SHALL ni arrêter SygixOs ni bloquer le rendu ; les copies locales utilisables, candidates alternatives et replis SHALL suivre le contrat partagé.

#### Scenario: échec journalisé
- **WHEN** une récupération d'image échoue
- **THEN** l'échec est journalisé avec l'app, le schéma, l'autorité et la cause, jamais l'URI complète ni ses paramètres

#### Scenario: fournisseur sans type déclaré
- **WHEN** le fournisseur d'une app ne déclare aucun type pour ses images et les sert par un flux de longueur inconnue
- **THEN** l'image est lue jusqu'à la fin du flux, vérifiée puis conservée et affichée comme une autre ; l'absence de type n'est pas une absence d'image

#### Scenario: fournisseur bloqué puis arrêté par le système
- **WHEN** le fournisseur d'images d'une application ne répond pas (serveur de l'application injoignable) et que le système arrête cette application pour ANR pendant qu'une de ses images est en cours d'ouverture
- **THEN** SygixOs reste lancé ; la récupération concernée échoue et la zone utilise sa copie locale utilisable, une candidate alternative ou son repli sans bloquer les autres apps

#### Scenario: délai dépassé
- **WHEN** l'ouverture ou la lecture du flux d'une image `content://` tierce dépasse le délai
- **THEN** le résultat d'image échoue au plus tard à l'échéance et l'annulation est déclenchée indépendamment de l'appel bloqué ; la zone passe à sa solution de repli sans bloquer le rendu

#### Scenario: appel Binder bloqué et résultat tardif
- **WHEN** l'ouverture du fournisseur reste bloquée après l'échéance malgré la demande d'annulation, puis retourne un descripteur
- **THEN** le résultat du chargement a déjà échoué ; le repli reste actif, les ressources tardives sont fermées exactement une fois, aucune entrée de cache n'est engagée et ce résultat n'est jamais publié à un consommateur

#### Scenario: pool de lecture saturé
- **WHEN** tous les workers bornés sont encore bloqués dans des appels de fournisseurs
- **THEN** une nouvelle récupération échoue rapidement vers le repli de sa zone, sans créer de thread ou de file non bornés et sans bloquer le rendu ; les copies locales encore utilisables restent accessibles

#### Scenario: image disponible
- **WHEN** le fournisseur répond dans le délai
- **THEN** ses octets et dimensions sont conservés selon le cache partagé, puis l'image s'affiche si elle satisfait la politique de sa destination

#### Scenario: permission refusée ou fournisseur absent
- **WHEN** l'accès à l'image est refusé ou que le fournisseur n'existe plus
- **THEN** l'échec est distingué d'une image vide ; les alternatives et replis sont utilisés sans crash ni demande d'autorisation non prévue

### Requirement: Chargement partagé des images
Le héro, le Top Shelf et À suivre SHALL partager récupération, cache, dimensions et reprises d'images ; une même version SHALL être récupérée et analysée une seule fois, chaque zone appliquant sa politique et décodant à sa taille. Les récupérations de toutes les sources SHALL être séquentielles par app, parallèles entre apps sous une constante globale ; seule la requête a un délai, depuis son début effectif. Une image qui change, expire ou échoue SHALL être mise à jour seule.

#### Scenario: validation, préchargement et affichage communs
- **WHEN** plusieurs zones demandent une même image, y compris pendant la validation et le préchargement du Top Shelf
- **THEN** elles partagent la récupération et les dimensions de cette version sans rouvrir le fournisseur ; chaque destination conserve ses propres taille et transformations de décodage

#### Scenario: résultat de qualité propre à l'usage
- **WHEN** une image ne convient pas au héro mais convient à une carte, ou qu'elle convient à une carte mais pas au panneau
- **THEN** son refus dans une destination n'empêche pas son acceptation dans une autre ; aucune validation d'usage n'est utilisée comme rejet global

#### Scenario: apps parallèles et app lente
- **WHEN** plusieurs apps ont des images à charger et qu'une app ne répond pas
- **THEN** les apps saines progressent sans attendre son lot ; une seule récupération physique existe par app, la concurrence totale respecte la constante commune et aucun composable ne crée une limite indépendante

#### Scenario: délai et alternatives
- **WHEN** une demande attend son tour, tente une première candidate puis une alternative
- **THEN** l'attente ne consomme aucun délai ; chaque requête dispose de son délai depuis son début effectif ; une requête expirée n'empêche pas la navigation ni les images déjà prêtes

#### Scenario: app qui publie beaucoup d'images
- **WHEN** une app publie plusieurs dizaines de programmes dont les images sont demandées ensemble
- **THEN** elles sont lues une à une sans expirer pendant leur attente, celles des zones affichées d'abord ; une demande dont aucune zone n'attend plus le résultat est retirée de la file avant son début

#### Scenario: redirection HTTP
- **WHEN** l'URI HTTP publiée redirige, y compris de http vers https
- **THEN** la redirection est suivie dans le délai de la requête et l'image est conservée sous l'URI publiée

#### Scenario: mise à jour ciblée
- **WHEN** une image change de version, expire, échoue, ou que le launcher revient au premier plan
- **THEN** seules les zones qui affichent cette image la remplacent, quand la nouvelle version est prête ou par leur repli ; les autres images restent affichées sans être effacées, revalidées ni redessinées

#### Scenario: travail encore demandé ailleurs
- **WHEN** l'utilisateur quitte une zone pendant qu'une autre attend la même récupération
- **THEN** le travail commun reste disponible pour l'autre zone sans seconde requête ; les travaux en attente sans consommateur sont retirés

#### Scenario: affiche et miniature publiées
- **WHEN** l'app fournit affiche et miniature et que la candidate préférée manque, échoue ou est refusée pour la destination
- **THEN** l'autre candidate est essayée avant le repli ; le ratio déclaré ne fait pas perdre l'alternative et les dimensions réelles font autorité

#### Scenario: reprise après panne temporaire
- **WHEN** une demande échoue temporairement et qu'un événement de rafraîchissement ou de focus prolongé autorise une reprise après temporisation
- **THEN** une nouvelle tentative commune est possible sans blacklist locale permanente ; elle n'est pas lancée si une ancienne récupération physique de la même app reste bloquée

### Requirement: Cache borné et fraîcheur des images
Les images HTTP(S) et `content://` obtenues SHALL être conservées dans un cache privé borné avec dimensions et date de récupération réussie. Une copie utilisable SHALL s'afficher immédiatement, rester fraîche moins de 24 h et pouvoir servir en secours jusqu'à moins de 7 jours. À 7 jours elle ne SHALL plus être affichée sans récupération réussie. Lecture, validation depuis le cache et affichage ne SHALL jamais renouveler cette date.

#### Scenario: entrée fraîche et reprise après redémarrage
- **WHEN** une image fraîche est disponible sur disque, y compris après recréation du chargeur ou redémarrage du processus
- **THEN** elle peut être affichée sans demande préalable au fournisseur et sans nouvelle analyse des dimensions de cette version ; l'éviction normale du cache peut toutefois avoir supprimé l'entrée

#### Scenario: entrée ancienne encore utilisable
- **WHEN** une copie a au moins 24 h et moins de 7 jours et reste utilisable pour un programme valide
- **THEN** elle est affichée immédiatement pendant un rafraîchissement commun borné ; le succès remplace les octets et la date, l'échec conserve la dernière copie valide sans prolonger son âge

#### Scenario: image expirée même déjà affichée
- **WHEN** une copie atteint exactement 7 jours sans récupération réussie
- **THEN** elle cesse d'être affichable, même déjà en mémoire ou à l'écran ; les candidates et replis de la zone sont utilisés sans attendre le prochain changement de programme

#### Scenario: nouvelle version et écriture interrompue
- **WHEN** de nouveaux octets sont récupérés sous la même URI, ou que leur copie ou décodage échoue
- **THEN** une nouvelle version utilisable remplace l'ancienne et invalide ses dimensions, verdicts et bitmaps ; une copie incomplète ou indécodable n'efface pas la dernière version décodable encore autorisée par son âge

#### Scenario: association au contenu
- **WHEN** deux films distincts ont des titres proches ou identiques, ou que plusieurs épisodes publient la même image de série
- **THEN** le cache n'échange pas les images des films ; les épisodes peuvent partager les octets d'une image de série explicitement associée par la source, sans inventer d'identifiant ni réécrire une URI signée

#### Scenario: image changée par l'app
- **WHEN** le TV Provider signale qu'une app a publié une autre URI d'image pour un de ses programmes
- **THEN** seule cette image est récupérée ; l'ancienne reste affichée jusqu'à ce que la nouvelle soit prête puis est remplacée sans écran vide ; les images des autres programmes ne sont ni relues ni redessinées

#### Scenario: même URI réutilisée pour un autre contenu
- **WHEN** une URI déjà en cache est publiée pour un programme d'une autre identité de film ou de série
- **THEN** l'image est relue avant de lui être associée ; si les octets sont identiques, l'association est ajoutée sans nouvelle analyse, sinon la nouvelle version est conservée pour ce programme sans changer l'image affichée des autres

#### Scenario: mise à jour de SygixOs
- **WHEN** une nouvelle version de SygixOs change le format de conservation des programmes et des images
- **THEN** les associations, dimensions et dates existantes sont migrées sans prolonger aucune échéance ; si la migration est impossible, l'index est recréé vide et les images sont récupérées de nouveau, sans crash ni image d'un autre contenu

#### Scenario: métadonnées manquantes ou dates incohérentes
- **WHEN** une entrée héritée n'a pas de date de récupération fiable ou porte une date future incohérente
- **THEN** elle n'est pas déclarée fraîche par défaut et doit être revalidée avant de bénéficier d'une nouvelle période de validité

#### Scenario: arrière-plan puis retour
- **WHEN** le launcher revient au premier plan après le franchissement d'une échéance d'image
- **THEN** l'âge est contrôlé avant de réafficher une copie et les tentatives reprennent selon le contrat commun ; le temps d'arrière-plan ne suspend pas le TTL

### Requirement: Validité des programmes publiés
Le héro, le Top Shelf et À suivre SHALL suivre la dernière confirmation réussie de chaque table du TV Provider. Une lecture réussie SHALL retirer les absents et confirmer les présents, même inchangés. Une erreur SHALL ne pas confirmer de résultat ; un ancien programme SHALL expirer après 24 h sans confirmation. Le cache d'images ne SHALL jamais réintroduire un programme retiré, expiré ou inaccessible.

#### Scenario: lecture réussie et liste vide
- **WHEN** la lecture d'une table réussit, avec une liste complète ou vide
- **THEN** seuls les programmes affichables encore présents de cette table sont confirmés ; les absents sont retirés des trois zones sans délai de grâce, même si leur image est locale

#### Scenario: panne avant le plafond
- **WHEN** une table ne peut pas être relue et que son dernier résultat confirmé a moins de 24 h
- **THEN** les anciens programmes encore accessibles peuvent être conservés sans être confondus avec un résultat vide ; cette panne ne renouvelle pas leur confirmation

#### Scenario: expiration du programme affiché
- **WHEN** un programme atteint exactement 24 h sans confirmation alors qu'il est sélectionné dans le héro, affiché dans un panneau ou présent dans À suivre
- **THEN** il est retiré de la surface active ; le héro passe à un autre programme valide ou au fallback global sans ancien texte, le panneau utilise une autre affiche valide sinon se ferme, et À suivre suit ses états et règles de focus existants

#### Scenario: confirmations indépendantes et âge du visionnage
- **WHEN** seule une table est relue avec succès, ou qu'un programme ancien dans l'historique de lecture reste publié
- **THEN** la confirmation ne prolonge que le résultat de cette table ; un programme confirmé n'est pas retiré en raison de sa seule date d'engagement

#### Scenario: permission perdue ou app désinstallée
- **WHEN** la permission de lecture est retirée ou qu'une app source est désinstallée
- **THEN** les programmes concernés deviennent inaccessibles selon les règles existantes sans bénéficier du délai de grâce ; les préférences, retraits utilisateur et mémoire d'ouverture ne sont pas purgés comme après une lecture vide réussie

#### Scenario: génération retirée et réponse tardive
- **WHEN** une image finit de charger après le retrait, l'expiration ou le remplacement de son programme
- **THEN** son résultat ne repeuple aucune liste et ne remplace pas le média d'un autre programme ; l'affichage contrôle encore l'identité et la génération courantes

#### Scenario: reprise au premier plan sans nouveau signal
- **WHEN** le launcher revient au premier plan après l'expiration d'un ancien résultat, sans notification du provider
- **THEN** l'échéance est contrôlée avant réaffichage puis une relecture bornée est lancée ; aucun ancien résultat n'est rendu frais par son simple retour à l'écran

### Requirement: Images génériques des applications
Une image qui n'est pas une vraie affiche du programme SHALL être traitée comme absente dans les trois zones : image refusée par toutes les politiques de dimensions (icône d'erreur, petite tuile), mêmes octets publiés par une app pour au moins 3 identités de film ou de série distinctes, ou ressource `android.resource://` d'une autre app. Elle ne SHALL pas être conservée comme image valide et SHALL être réessayée au bout d'une heure.

#### Scenario: tuile générique d'une app
- **WHEN** une app publie les mêmes octets d'image pour trois films ou séries distincts
- **THEN** cette image n'est affichée nulle part pour ces programmes ; le héro les écarte faute d'autre candidate, le Top Shelf ne les montre pas et À suivre affiche son placeholder

#### Scenario: image partagée légitime
- **WHEN** plusieurs épisodes d'une même série, ou un même film publié en aperçu et en À suivre, partagent la même image
- **THEN** l'image reste valide et partagée ; ces publications ne comptent que pour une identité

#### Scenario: icône d'erreur du fournisseur
- **WHEN** le fournisseur d'une app renvoie une petite icône à la place de l'affiche parce que son serveur est injoignable
- **THEN** elle est refusée sans remplacer une copie valide encore autorisée par son âge ; une nouvelle tentative a lieu au plus tôt une heure après

#### Scenario: ressource d'une autre application
- **WHEN** une candidate est une URI `android.resource://` d'une autre app
- **THEN** elle n'est pas lue et compte comme absente ; les autres candidates et replis s'appliquent

### Requirement: Cache disque des vidéos du héro
Les vidéos du héro (clips nature et vidéos d'aperçu) SHALL être conservées dans un cache disque privé, jusqu'à 300 Mo, adapté à l'espace libre de l'appareil, les vidéos les moins récemment lues étant évincées d'abord. Sans place suffisante ou en cas d'erreur du cache, la lecture SHALL se faire en flux sans cache, sans erreur visible ni crash.

#### Scenario: clip déjà lu
- **WHEN** un clip ou une vidéo d'aperçu déjà lu entièrement est relu, y compris après redémarrage de SygixOs
- **THEN** il démarre depuis le cache disque sans nouveau téléchargement

#### Scenario: espace disque insuffisant
- **WHEN** l'espace libre de l'appareil ne permet pas un cache utile, ou que le cache ne peut pas être créé, lu ou écrit
- **THEN** la taille du cache est réduite, ou la vidéo est lue en flux sans cache ; le héro reste fonctionnel, sans message d'erreur ni crash

#### Scenario: cache plein
- **WHEN** l'ajout d'une vidéo dépasse la taille du cache
- **THEN** les vidéos les moins récemment lues sont évincées ; la lecture en cours n'est pas interrompue

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
Une panne du fournisseur connecté SHALL avoir un indicateur sans vol de focus ni notification répétée, avec cause et Réessayer ou Reconnecter. Le détail SHALL rester accessible même sans carte À suivre et disparaître après rétablissement confirmé ou déconnexion.

#### Scenario: panne partielle
- **WHEN** le compte échoue et que les programmes locaux restent disponibles
- **THEN** ils restent utilisables ; l'indicateur ne prend ni carte ni focus, utilise TextStyles.RowSecondary sans dépasser la hauteur du titre, aligné à droite sans suffixe « · Réglages » ; seul le trajet depuis l'engrenage donne accès au détail

#### Scenario: accès sans carte depuis le héro
- **WHEN** aucune notification de suivi n'est affichée, le compte est en panne et le focus est sur l'engrenage
- **THEN** Droite ouvre le détail (cause, Réessayer sinon Reconnecter) ; fermer rend l'engrenage ; ce trajet reste identique quand À suivre est masqué

#### Scenario: synchronisation en cours
- **WHEN** une synchronisation affiche un statut dans l'en-tête d'À suivre
- **THEN** ce statut reprend le placement discret à droite et n'agrandit pas l'en-tête ni ne prend la place des cartes

#### Scenario: aucune carte restante
- **WHEN** la panne retire les dernières cartes du suivi
- **THEN** le repli de focus de rangée vide s'applique et la cause/réessai restent accessibles depuis les réglages de suivi, sans dépendre d'une carte disparue

#### Scenario: détail et fermeture
- **WHEN** le détail est ouvert
- **THEN** Réessayer, sinon Reconnecter a le focus ; haut/bas parcourent ses actions sans boucle, gauche/droite restent dans le panneau, Retour restitue l'origine ou son repli ; une action disparue au rétablissement restitue le contexte sans focus perdu
