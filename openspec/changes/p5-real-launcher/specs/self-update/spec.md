# Spec Delta

Ce delta reprend « Redémarrage après la mise à jour » de `openspec/specs/self-update/spec.md` en entier, avec tous ses scénarios. Seuls changent la dernière phrase de l'exigence et le scénario « pas de prise de rôle » : le parcours de mise à jour ne demande toujours jamais le rôle HOME, et P5 ne le demande qu'après une action explicite de l'utilisateur.

## MODIFIED Requirements

### Requirement: Redémarrage après la mise à jour
Après une installation réussie, si SygixOs était au premier plan au moment où l'installation a été validée (installation sans confirmation lancée au premier plan, ou écran de confirmation du système effectivement affiché), le launcher SHALL demander sa réouverture sur son accueil, dans la nouvelle version, comme lors d'un démarrage à froid. La réouverture automatique dépend des autorisations du système : sur la TV de référence, la politique constructeur bloque cette relance. Cette limitation est acceptée et son traitement est reporté à P5 (remplacement du launcher système, suivi post-release sujet 9) ; dans ce cas, la nouvelle version SHALL être présente au retour manuel sur SygixOs. La demande de relance SHALL porter sur la version visée, être effacée après un refus, un échec ou l'abandon de la session, et n'avoir aucun effet si la version installée n'est pas celle visée. Si SygixOs n'était pas au premier plan, il SHALL ne pas se rouvrir et ne rien afficher : la nouvelle version est simplement présente au prochain retour sur SygixOs. Le launcher SHALL déclarer son activité principale comme candidate au rôle d'écran d'accueil d'Android, pour que le système la relance quand SygixOs est le launcher par défaut. Le parcours de mise à jour (vérification, téléchargement, installation, relance) SHALL ne jamais demander ce rôle ni rien faire pour devenir launcher par défaut : ce choix reste celui de l'utilisateur, et SygixOs ne demande le rôle qu'après une action explicite de l'utilisateur dans la présentation initiale ou dans les réglages (« Rôle d'écran d'accueil » de `launcher-shell`).

#### Scenario: installation au premier plan
- **WHEN** l'installation réussit alors que SygixOs était au premier plan
- **THEN** l'accueil de SygixOs s'affiche de nouveau dans la nouvelle version si le système autorise la relance ; si la politique constructeur la bloque, l'utilisateur rouvre SygixOs manuellement ; « À propos » affiche la nouvelle version et la pastille a disparu ; aucun message n'annonce la mise à jour

#### Scenario: installation en arrière-plan
- **WHEN** l'installation réussit alors qu'une autre app est au premier plan
- **THEN** SygixOs ne s'ouvre pas et rien ne s'affiche par-dessus l'app ; au prochain retour sur SygixOs, la nouvelle version est installée et c'est un démarrage à froid

#### Scenario: confirmation acceptée au retour
- **WHEN** l'utilisateur a pressé Home pendant le téléchargement, que la confirmation du système a été gardée puis affichée à son retour sur SygixOs, et qu'il l'accepte
- **THEN** l'accueil de SygixOs s'affiche de nouveau après l'installation, dans la nouvelle version, si le système autorise la relance ; sinon la nouvelle version est disponible au retour manuel, conformément à la limitation constructeur reportée à P5

#### Scenario: remplacement venu d'ailleurs
- **WHEN** une demande de relance a été effacée par un échec, ou vise une autre version, et que SygixOs est remplacé par un autre moyen
- **THEN** SygixOs ne se rouvre pas

#### Scenario: pas de prise de rôle
- **WHEN** SygixOs est installé ou mis à jour sur une TV dont le launcher par défaut est un autre launcher
- **THEN** ni l'installation ni la mise à jour ne demandent le rôle d'écran d'accueil ni ne modifient un réglage ; Android peut proposer un choix de launcher au prochain appui sur Home, et la réponse appartient à l'utilisateur ; la demande du rôle n'a lieu qu'après une action explicite de l'utilisateur dans la présentation initiale ou les réglages (« Rôle d'écran d'accueil » de `launcher-shell`)
