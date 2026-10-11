# Spec Delta

## MODIFIED Requirements

### Requirement: Contenu héro TV Provider
Le héro SHALL afficher les programmes publiés dans le TV Provider système par les apps installées (watch next + preview programs), sans autre configuration que l'octroi de la permission système de lecture des programmes TV. Toute colonne autre que l'identifiant et le package SHALL être traitée comme facultative : une colonne absente ou vide retire seulement l'élément qu'elle alimente. Le remplissage réel du type, de la saison, de l'épisode et de la durée par les apps de la TV de référence est une hypothèse à vérifier sur l'appareil.

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
SygixOs SHALL ouvrir les images `content://` publiées par d'autres applications (visuels du héro, du Top Shelf et d'À suivre) par une référence instable au fournisseur de contenu, avec un délai borné, de sorte que l'arrêt ou le blocage du processus de ce fournisseur ne puisse jamais arrêter SygixOs. Une image dont le fournisseur meurt, ne répond pas dans le délai ou renvoie une erreur SHALL être traitée comme une image en échec (espace réservé), sans plantage.

#### Scenario: fournisseur bloqué puis arrêté par le système
- **WHEN** le fournisseur d'images d'une application ne répond pas (serveur de l'application injoignable) et que le système arrête cette application pour ANR pendant qu'une de ses images est en cours d'ouverture
- **THEN** SygixOs reste lancé ; l'image concernée passe en échec et son espace réservé reste affiché

#### Scenario: délai dépassé
- **WHEN** l'ouverture d'une image `content://` d'une autre application dépasse le délai
- **THEN** l'ouverture est annulée, la référence au fournisseur et le descripteur sont libérés et l'image passe en échec

#### Scenario: image disponible
- **WHEN** le fournisseur répond dans le délai
- **THEN** l'image s'affiche comme aujourd'hui
