# Spec Delta

## MODIFIED Requirements

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes publiés dans le TV Provider système par les apps installées (watch next + preview programs), sans autre configuration que l'octroi de la permission système de lecture des programmes TV. Toute colonne autre que l'identifiant et le package SHALL être traitée comme facultative : une colonne absente ou vide retire seulement l'élément qu'elle alimente. Le remplissage réel du type, de la saison, de l'épisode et de la durée par les apps de la TV de référence est une hypothèse à vérifier sur l'appareil. Le héro et le Top Shelf SHALL ne montrer que des programmes encore publiés : le launcher relit le provider quand celui-ci signale un changement et au plus tard toutes les 15 minutes tant que l'accueil est au premier plan, jamais en arrière-plan ; une relecture sans changement ne republie rien.

#### Scenario: permission
- **WHEN** le launcher démarre sans `android.permission.READ_TV_LISTINGS`
- **THEN** la demande système s'affiche ; dès l'octroi le héro se recharge sans redémarrage ; en cas de refus le héro reste sur le fallback sans erreur visible

#### Scenario: programmes disponibles
- **WHEN** une ou plusieurs apps installées publient des programmes dans le TV Provider
- **THEN** le héro les affiche : visuel (previewVideoUri, sinon posterArtUri, sinon thumbnailUri), titre, app source, type du programme (`watch_next_type` d'un watch next ; preview program sinon), numéros de saison et d'épisode affichés (`season_display_number`, `episode_display_number`), durée et progression quand disponibles (« Diaporama héro »), ordre mélangé au hasard une fois par lancement de SygixOs (« Ordre aléatoire du héro »), toutes apps confondues, sans liste d'apps codée en dur ; les requêtes au provider ne portent aucune clause de sélection, le filtrage par app est fait côté launcher

#### Scenario: fallback
- **WHEN** aucune app ne publie de programme dans le TV Provider
- **THEN** le héro utilise le fond vidéo de secours, sans crash ni écran vide

#### Scenario: colonnes facultatives
- **WHEN** un programme ne publie pas son type, sa saison, son épisode, sa durée ou sa position
- **THEN** il reste affiché ; seuls les éléments correspondants manquent, selon « Diaporama héro »

#### Scenario: rafraîchissement automatique
- **WHEN** l'accueil est au premier plan et que le TV Provider signale un changement, ou que 15 minutes se sont écoulées depuis la dernière relecture
- **THEN** le héro relit le provider ; aucune relecture n'a lieu tant que le launcher est en arrière-plan, et le retour au premier plan en déclenche une

#### Scenario: programme retiré
- **WHEN** une app retire un programme publié pendant que l'accueil est affiché
- **THEN** il disparaît du héro et du Top Shelf à la relecture suivante, sans redémarrage ; s'il était affiché dans le héro, le héro passe par fondu au programme qui prend sa place, ses textes ne changeant qu'avec le visuel (« Diaporama héro ») ; ses visuels sortent des ensembles de visuels vérifiés

#### Scenario: programme modifié
- **WHEN** une app modifie le titre ou le visuel d'un programme publié
- **THEN** le héro et le Top Shelf montrent le nouveau titre et le nouveau visuel à la relecture suivante ; un nouveau visuel est vérifié comme un visuel inconnu (« Qualité des visuels »)

## ADDED Requirements

### Requirement: Ordre aléatoire du héro
L'ordre des programmes du héro SHALL être tiré au hasard une fois par lancement de SygixOs (démarrage du processus) et SHALL rester stable pendant la session : un retour au premier plan ou un rechargement du TV Provider ne remélange pas les programmes déjà présents. Un programme apparu pendant la session SHALL être inséré à une position tirée au hasard après le programme affiché, sans déplacer les autres ; un programme disparu est retiré sans changer l'ordre relatif des autres.

#### Scenario: nouveau lancement
- **WHEN** SygixOs est relancé (nouveau processus) avec les mêmes programmes publiés
- **THEN** l'ordre du héro est tiré de nouveau au hasard ; le premier programme n'est pas toujours le même

#### Scenario: retour au premier plan
- **WHEN** l'utilisateur revient sur SygixOs pendant la même session, avec ou sans rechargement du TV Provider
- **THEN** les programmes déjà présents gardent leur ordre

#### Scenario: programme ajouté pendant la session
- **WHEN** une app publie un nouveau programme pendant la session
- **THEN** il est inséré à une position tirée au hasard après le programme affiché, sans réordonner les autres

### Requirement: Images d'autres applications sans dépendance forte
SygixOs SHALL charger les images `content://` des apps tierces via une référence instable et un pool borné, de sorte qu'un fournisseur bloqué ne bloque pas le rendu ni n'arrête SygixOs. Les erreurs ou dépassements de délai SHALL produire un échec d'image et un espace réservé.

#### Scenario: fournisseur bloqué puis arrêté par le système
- **WHEN** le fournisseur d'images d'une application ne répond pas (serveur de l'application injoignable) et que le système arrête cette application pour ANR pendant qu'une de ses images est en cours d'ouverture
- **THEN** SygixOs reste lancé ; l'image concernée passe en échec et son espace réservé reste affiché

#### Scenario: délai dépassé
- **WHEN** l'ouverture d'une image `content://` d'une autre application dépasse le délai
- **THEN** la requête Coil reçoit un résultat d'échec au plus tard à l'échéance, le signal d'annulation est déclenché hors du worker bloqué, et l'image passe en échec sans bloquer le rendu

#### Scenario: appel Binder bloqué et résultat tardif
- **WHEN** `openTypedAssetFile` reste bloqué après l'échéance malgré la demande d'annulation, puis retourne un descripteur
- **THEN** le résultat du chargement a déjà échoué, l'image de remplacement reste affichée, le descripteur tardif et le client sont fermés dès la sortie du worker, et ce résultat n'est jamais transmis à Coil

#### Scenario: pool de lecture saturé
- **WHEN** tous les workers bornés sont encore bloqués dans des appels de fournisseurs
- **THEN** une nouvelle requête échoue rapidement en espace réservé, sans créer de thread ou de file d'attente non bornés et sans bloquer le rendu

#### Scenario: image disponible
- **WHEN** le fournisseur répond dans le délai
- **THEN** l'image s'affiche comme aujourd'hui
