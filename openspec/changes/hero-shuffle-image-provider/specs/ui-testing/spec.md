# Spec Delta

## MODIFIED Requirements

### Requirement: Couverture des infos du héro
Les infos du programme dans le héro (« Diaporama héro » de `launcher-shell`) SHALL être couvertes par des tests JUnit (choix du libellé de l'en-tête, éléments de la ligne d'infos, arrondis de la durée et du temps restant, lecture des colonnes du TV Provider) et par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) avec une source d'icônes de test, sans capture d'image et avec des titres fictifs uniquement.

#### Scenario: types de programme
- **WHEN** le héro affiche un programme `CONTINUE`, `NEXT`, `NEW`, `WATCHLIST`, un preview program et un watch next sans type
- **THEN** « hero-header-label » vaut respectivement « Continuer dans X », « Épisode suivant dans X », « Nouveau dans X », « À regarder dans X », « X » et « X »

#### Scenario: données manquantes
- **WHEN** le nom de l'app, son icône, la saison, l'épisode, la durée ou la position manquent
- **THEN** seuls les éléments disponibles existent (« hero-source-icon », « hero-header-label », « hero-details », « hero-progress-bar », « hero-remaining »), « hero-header » n'existe pas sans icône ni libellé, et sans ligne d'infos le bas du titre est le bas de « hero-metadata »

#### Scenario: textes avec le visuel
- **WHEN** l'image du programme suivant est retenue par un chargeur de test, puis réussit dans un premier cas et échoue par délai de requête dans un second
- **THEN** pendant l'attente, les textes et la cible de « hero-open » restent ceux du visuel précédent et le défilement passe ce programme ; au succès, il entre dans le diaporama avec son visuel ; à l'échec, il s'affiche avec son repli nature puis neutre si nécessaire ; aucune récupération en cours n'est dupliquée et une réponse tardive ne désynchronise pas les textes

#### Scenario: vidéo d'aperçu lente
- **WHEN** une vidéo d'aperçu d'un lecteur de test ne rend pas sa première image dans la seconde qui suit son changement
- **THEN** le héro passe au programme suivant sans afficher les textes de la vidéo ; la vidéo reste préparée, en pause, sans être préparée une seconde fois ; quand elle est prête et qu'on y revient, elle est lue avec ses textes

#### Scenario: héro immobile
- **WHEN** un programme avec en-tête, ligne d'infos et temps restant est affiché pendant le Ken Burns
- **THEN** des états Compose sont modifiés (zoom) mais aucune recomposition n'a lieu (`IdleFrameTest`)

## ADDED Requirements

### Requirement: Couverture du chargement partagé des images
Le contrat d'images partagé de launcher-shell SHALL être couvert par des tests déterministes avec sources, horloge et échéancier contrôlés. Les tests SHALL compter les récupérations et analyses de dimensions, distinguer les usages et vérifier les limites de concurrence sans réseau ni fournisseur tiers réel.

#### Scenario: récupération commune aux trois zones
- **WHEN** le héro, le préchargement du Top Shelf, son affichage et une carte demandent simultanément la même version d'image
- **THEN** une seule récupération et une seule analyse d'en-tête ont lieu ; les usages partagent le résultat, les décodages demandent leurs tailles propres et le départ d'un abonné n'annule pas le travail d'un autre

#### Scenario: politiques de dimensions indépendantes
- **WHEN** la même image est soumise aux politiques héro, Top Shelf et À suivre à des tailles d'affichage contrôlées, aux valeurs juste sous, égales et juste au-dessus de leurs seuils
- **THEN** les verdicts suivent leurs exigences respectives, une image refusée pour le héro ou le panneau peut être acceptée dans la carte, le portrait ne passe que dans la carte, et les verdicts ne déclenchent aucune nouvelle analyse de la même version

#### Scenario: séquentiel par app et parallèle entre apps
- **WHEN** plusieurs images de quatre apps sont demandées avec des récupérations contrôlées
- **THEN** au plus la constante globale d'apps est active, aucune app n'a deux récupérations physiques à la fois, une app lente n'empêche pas les autres de terminer, et une place disponible fait progresser une autre app sans attendre la fin du lot ; trente images d'une même app sont toutes lues sans qu'aucune n'expire pendant son attente, celles d'une zone affichée en premier

#### Scenario: ouverture et lecture bloquées
- **WHEN** un double bloque successivement dans l'ouverture du fournisseur puis dans la lecture du flux, en ignorant l'annulation
- **THEN** la requête échoue à son délai depuis l'échéancier indépendant ; le descripteur et le flux ouverts sont fermés à l'annulation ; le worker est quarantiné jusqu'à sa sortie, aucune seconde requête de cette app n'est démarrée, les autres apps progressent sur les places restantes et les ressources tardives sont fermées exactement une fois sans publication ni engagement du cache

#### Scenario: fournisseurs et transports réels
- **WHEN** un fournisseur de test ne déclare aucun type et sert un flux de longueur inconnue, qu'une URI HTTP redirige de http vers https, ou qu'une candidate est une ressource d'une autre app
- **THEN** les deux premiers cas donnent une image conservée et affichable ; la ressource d'une autre app compte comme absente sans être lue

#### Scenario: saturation bornée
- **WHEN** tous les workers physiques sont bloqués puis de nouvelles demandes arrivent
- **THEN** elles échouent dans le délai contractuel sans augmenter le nombre borné de threads, de travaux ni la file ; les hits de cache restent servis et le rendu ne bloque pas

#### Scenario: alternatives et erreurs
- **WHEN** la première candidate manque, échoue ou est trop petite, puis qu'une alternative publiée réussit, ou que toutes échouent par refus de permission, fournisseur absent ou panne de transport
- **THEN** la candidate exploitable est retenue pour l'usage, ou son repli est exposé avec la cause ; une candidate non publiée n'est jamais inventée et chaque requête garde son propre délai

#### Scenario: récupération après panne et nouvelle version
- **WHEN** une image échoue temporairement, puis réussit après un événement de reprise autorisé, ou que le fournisseur publie de nouveaux octets sous la même URI
- **THEN** aucune blacklist locale ne bloque la reprise ; une seule tentative est commune aux abonnés, les nouvelles dimensions et les bitmaps remplacent ceux de la version précédente, et un verdict d'ancienne version n'est pas réutilisé

### Requirement: Couverture des échéances de cache et de programmes
Les expirations de « Cache borné et fraîcheur des images » et « Validité des programmes publiés » SHALL être testées avec une horloge contrôlée, y compris aux frontières exactes, après recréation du chargeur et pendant un affichage continu. Un cache consulté ne SHALL pas renouveler ses dates.

#### Scenario: cache disque après redémarrage du chargeur
- **WHEN** un chargeur obtient une image `content://` puis est recréé avec le même cache disque et un fournisseur indisponible
- **THEN** une demande autorisée par l'âge de l'entrée reçoit les octets locaux sans ouverture préalable du fournisseur, avec la même date de récupération ; les dimensions restent communes aux usages

#### Scenario: image fraîche puis ancienne utilisable
- **WHEN** l'âge depuis récupération réussie passe de moins de 24 h à 24 h, avec un rafraîchissement contrôlé en échec
- **THEN** avant la frontière aucune récupération n'est nécessaire ; à partir de la frontière la copie est immédiatement disponible pendant un rafraîchissement unique, et l'échec préserve ses octets et sa date

#### Scenario: plafond de sept jours
- **WHEN** une image atteint exactement 7 jours, alors qu'elle est présente sur disque, en mémoire et affichée, ou que le chargeur reprend après un passage en arrière-plan
- **THEN** aucune ancienne copie n'est encore montrée sans récupération réussie ; le repli de chaque zone est observable sans attendre un nouveau changement de programme

#### Scenario: confirmation des programmes
- **WHEN** une lecture de table réussit sans un programme auparavant affiché, puis qu'une autre lecture échoue ou retourne une table vide avec succès
- **THEN** le retrait réussi est immédiat, l'erreur reste distincte du vide et conserve seulement les programmes non expirés ; la table vide retire ses programmes sans les réintroduire depuis les images locales

#### Scenario: plafond de vingt-quatre heures et tables indépendantes
- **WHEN** un résultat non confirmé atteint exactement 24 h pendant qu'une autre table est relue avec succès
- **THEN** seul le résultat expiré est retiré ; le succès de l'autre table ne prolonge pas sa confirmation et une image locale ne maintient pas son programme

#### Scenario: résultat tardif et identité
- **WHEN** un résultat d'image arrive après le retrait ou le remplacement de son programme, ou que deux films ont le même titre mais des identités distinctes
- **THEN** il ne réintroduit aucun programme et ne s'affiche pas sur un autre film ; les épisodes ne partagent que des images de série dont l'association est fiable

### Requirement: Couverture des replis des trois zones
Les replis du héro, du Top Shelf et d'À suivre SHALL être testés en Compose à la taille TV, avec les testTags réels existants et « hero-fallback-nature » / « hero-fallback-neutral », sans captures d'écran. Le D-pad SHALL rester fonctionnel pendant les pannes et expirations.

#### Scenario: héro sur repli puis programme sain
- **WHEN** l'image d'une app dépasse son délai, que le lecteur nature réussit puis échoue dans deux cas de test, et que le programme suivant d'une autre app est prêt
- **THEN** les replis nature puis neutre portent le bon titre et la bonne cible de « hero-open » ; droite et le défilement automatique montrent le programme sain sans attente collective, et le repli n'ajoute aucun arrêt de focus

#### Scenario: Top Shelf d'une app indisponible
- **WHEN** le focus passe d'une app avec affiche prête à une app dont les images ne répondent pas, puis à une troisième app saine
- **THEN** « shelf-panel » ne montre jamais une affiche de l'ancienne app pour la nouvelle ; il se ferme faute d'affiche prête, s'ouvre pour l'app saine selon les règles existantes et le focus reste sur la bonne tuile

#### Scenario: expiration pendant un panneau ouvert
- **WHEN** une affiche ou son programme expire pendant que le Top Shelf est ouvert et qu'une autre validation reste en cours
- **THEN** l'affiche expirée disparaît sans attendre la fin du lot, une autre prête est utilisée sinon le panneau se ferme, et la tuile garde le focus et sa visibilité

#### Scenario: carte et focus après expiration
- **WHEN** l'image d'une carte expire puis que son programme expire, d'abord au milieu de la rangée puis en dernière position
- **THEN** l'image devient le placeholder sans changer le focus ni la géométrie ; le retrait du programme applique « carte focusée disparue » ou « rangée devenue vide », sans effacer les contenus retirés ni la mémoire d'ouverture

### Requirement: Couverture des mises à jour ciblées et des vidéos
Les mises à jour d'images pilotées par le TV Provider et les échéances, les images génériques, la conservation entre versions et les vidéos du héro SHALL être couvertes par des tests déterministes qui comptent récupérations, analyses et rechargements par image, avec provider, horloge, stockage et lecteur vidéo de test.

#### Scenario: un seul programme change
- **WHEN** le provider de test signale un changement où un seul programme publie une nouvelle URI, puis un autre où les programmes sont supprimés et republiés identiques sous de nouveaux identifiants avec une autre progression
- **THEN** une seule récupération a lieu dans le premier cas et aucune dans le second ; aucune autre image n'est effacée, revalidée ni recomposée dans les trois zones, et l'ordre du héro est conservé

#### Scenario: échéance et retour au premier plan
- **WHEN** une image affichée atteint 24 h, puis que le launcher revient au premier plan
- **THEN** seule cette image est rafraîchie et reste affichée jusqu'à l'arrivée de la nouvelle version ; le retour au premier plan n'efface ni ne recharge aucune image valide

#### Scenario: images génériques
- **WHEN** une app publie les mêmes octets pour trois identités distinctes, puis pour des épisodes d'une même série
- **THEN** le premier cas est traité comme absence d'image avec nouvelle tentative après une heure ; le second reste une image valide partagée

#### Scenario: conservation entre versions
- **WHEN** l'index des programmes et des images est ouvert depuis le schéma de la version précédente, puis depuis un schéma sans migration connue
- **THEN** la migration conserve associations, dimensions et dates sans les prolonger ; sans migration, l'index est recréé vide sans crash et les images sont récupérées de nouveau

#### Scenario: clip nature continu
- **WHEN** trois programmes en repli se suivent, puis le clip se termine, puis un programme avec image s'affiche avant un nouveau repli
- **THEN** le lecteur de test ne prépare le clip qu'une fois pendant les trois programmes, passe au clip suivant à sa fin sans changer de programme, et repart du début du clip au repli suivant

#### Scenario: cache vidéo indisponible
- **WHEN** le cache vidéo de test est plein, en erreur d'écriture ou impossible à créer faute d'espace
- **THEN** les vidéos plus anciennes sont évincées dans le premier cas ; dans les autres, la lecture se fait en flux sans erreur visible et le héro reste fonctionnel
