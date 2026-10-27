## MODIFIED Requirements

### Requirement: Héro du suivi
Le héro SHALL recevoir tous les contenus à regarder admissibles du fournisseur, pas seulement les 20 cartes retenues dans À suivre. Identité, épisode retenu, état effectif local durable et lecteur SHALL être partagés ; ordre, qualité d'image, préchargement, fondu et fallback SHALL suivre les contrats du héro existants. Une panne temporaire SHALL conserver les candidats connus du compte lié.

#### Scenario: compte plus grand que la rangée
- **WHEN** le compte contient plus de 20 contenus admissibles
- **THEN** tous sont candidats au héro, chargés par pages et préparés selon les bornes existantes, sans décoder toutes leurs images à l'avance ; la réserve d'épisodes ne crée pas plusieurs candidats pour une même série

#### Scenario: présentation et ouverture
- **WHEN** un contenu de suivi est affiché
- **THEN** les textes habituels du héro sont utilisés avec métadonnées connues ; l'en-tête identifie l'app choisie, sinon BetaSeries ; son bouton utilise le parcours commun sans prétendre qu'une offre non confirmée est disponible

#### Scenario: masquage local
- **WHEN** une série ou un film est retiré localement d'À suivre
- **THEN** les candidats BetaSeries de ce contenu quittent aussi le héro ; les programmes locaux non issus du suivi gardent leurs règles existantes

#### Scenario: source en attente ou vide
- **WHEN** le compte charge ou réussit sans contenu admissible
- **THEN** les programmes locaux et le cache du compte encore admissible restent utilisables ; sans candidat local ni cache affichable, le fallback existe sans prolonger le démarrage pour attendre le compte

#### Scenario: panne ou déconnexion
- **WHEN** le compte subit une panne temporaire, perd son autorisation ou est déconnecté
- **THEN** la panne conserve ses candidats issus de l'état local durable avec indicateur ; refus d'accès et déconnexion les retirent sans réapparition depuis le cache d'images ; le programme local correspondant garde son contrat, sinon le héro passe au candidat suivant ou au fallback

#### Scenario: retour et ordre
- **WHEN** le compte se rafraîchit au retour au premier plan
- **THEN** les candidats conservés ne sont pas remélangés ; ajouts et retraits suivent Ordre aléatoire du héro et l'identité stable évite un doublon du même contenu identifié

#### Scenario: progression et film hors ligne
- **WHEN** un épisode ou film est marqué vu localement sans réseau
- **THEN** le héro utilise le même état effectif que À suivre : épisode suivant déjà enregistré ou retrait du candidat film, sans barre de lecture inventée ; une réserve épuisée ne crée pas de suite et le focus suit les replis existants

#### Scenario: images hors ligne
- **WHEN** un contenu persistant ne dispose plus d'une image locale utilisable
- **THEN** les politiques d'images et replis existants s'appliquent sans prolonger leurs échéances ; les métadonnées persistées ne promettent pas le téléchargement des médias

### Requirement: Indicateur d'indisponibilité du suivi
Une panne du fournisseur connecté ou des intentions locales en attente SHALL avoir un indicateur sans vol de focus ni notification répétée, avec cause et Réessayer ou Reconnecter. Le détail SHALL rester accessible même sans carte À suivre ; il SHALL distinguer sauvegarde locale et confirmation distante, puis disparaître après résolution des états signalés ou déconnexion.

#### Scenario: panne partielle
- **WHEN** le compte échoue et que les programmes locaux ou le cache du compte restent disponibles
- **THEN** ils restent utilisables ; l'indicateur ne prend ni carte ni focus, utilise TextStyles.RowSecondary sans dépasser la hauteur du titre, aligné à droite sans suffixe « · Réglages » ; seul le trajet depuis l'engrenage donne accès au détail

#### Scenario: accès sans carte depuis le héro
- **WHEN** aucune notification de suivi n'est affichée, qu'une panne ou attente est signalée et que le focus est sur l'engrenage
- **THEN** Droite ouvre le détail (état, Réessayer sinon Reconnecter) ; fermer rend l'engrenage ; ce trajet reste identique quand À suivre est masqué

#### Scenario: synchronisation en cours
- **WHEN** une synchronisation affiche un statut dans l'en-tête d'À suivre
- **THEN** ce statut reprend le placement discret à droite et n'agrandit pas l'en-tête ni ne prend la place des cartes ; une intention durable en attente n'est pas présentée comme déjà confirmée sur BetaSeries

#### Scenario: aucune carte restante
- **WHEN** la panne sans cache utilisable ou le traitement local des marquages retire les dernières cartes du suivi
- **THEN** le repli de focus de rangée vide s'applique et la cause ou l'attente ainsi que le réessai restent accessibles depuis les réglages de suivi, sans dépendre d'une carte disparue

#### Scenario: détail et fermeture
- **WHEN** le détail est ouvert
- **THEN** Réessayer, sinon Reconnecter a le focus ; haut/bas parcourent ses actions sans boucle, gauche/droite restent dans le panneau, Retour restitue l'origine ou son repli ; une action disparue au rétablissement restitue le contexte sans focus perdu

#### Scenario: confirmation partielle de la file
- **WHEN** certaines demandes sont confirmées mais que d'autres intentions ou compléments restent en attente
- **THEN** l'indicateur reflète le travail restant ; aucune notification répétée ni succès global ne masque ces états, même si une notice individuelle a été fermée
