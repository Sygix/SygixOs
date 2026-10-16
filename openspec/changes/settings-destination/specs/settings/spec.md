# Spec Delta

## ADDED Requirements

### Requirement: Destination de réglages exclusive
Les réglages SHALL être une destination distincte, exclusive de l'accueil dans l'arbre d'interface actif. Pendant leur affichage, le contenu de l'accueil SHALL être démonté et non simplement masqué derrière un panneau. Le rendu, le focus initial, les bords D-pad et Retour restent ceux de « Page de réglages » ; aucune nouvelle préférence utilisateur n'est introduite.

#### Scenario: ouverture
- **WHEN** l'utilisateur ouvre les réglages depuis l'engrenage
- **THEN** la page plein écran remplace le contenu d'accueil ; la première catégorie a le focus et aucun élément d'accueil ne reste composé derrière elle

#### Scenario: retour et ancre conservée
- **WHEN** l'utilisateur quitte les réglages par Retour, sans liste déroulante ni panneau prioritaire ouvert
- **THEN** le héro revient affiché et focusé selon « Page de réglages », sans nouvel écran de démarrage ; l'ancre de grille mémorisée reste disponible pour une descente ultérieure

#### Scenario: liste déroulante et panneau système
- **WHEN** la liste « Position d'À suivre » ou le panneau du launcher système est ouvert puis l'utilisateur presse Retour
- **THEN** le contrat propre à la liste ou au panneau s'applique d'abord ; les réglages restent la destination active quand ce contrat l'exige, sans recomposer l'accueil

#### Scenario: retour d'une autre app
- **WHEN** le launcher passe en arrière-plan depuis les réglages puis revient sans demande Home au premier plan
- **THEN** la destination et la catégorie des réglages sont conservées ; les états système sont relus et la fermeture de la liste déroulante suit son contrat existant

#### Scenario: Home au premier plan
- **WHEN** un intent Home valide est reçu au premier plan pendant les réglages, avec ou sans panneau superposé
- **THEN** « Touche Home avec SygixOs au premier plan » de `launcher-shell` s'applique ; les réglages se ferment et le héro redevient la destination active et focusée

#### Scenario: données modifiées dans les réglages
- **WHEN** l'utilisateur modifie sources, visibilité, position d'À suivre ou apps cachées
- **THEN** l'état persiste selon les exigences de ces contrôles ; au retour, l'accueil reflète les valeurs courantes sans redémarrage ni réinitialisation de ses autres données

#### Scenario: données indisponibles
- **WHEN** catalogue, compteurs ou autres données nécessaires aux réglages chargent, sont vides ou échouent, avec source absente ou permission refusée
- **THEN** les états et replis de leurs exigences restent affichés et navigables ; l'accueil n'est pas recomposé pour les produire et aucune seconde observation des compteurs n'est créée

#### Scenario: compteurs au premier plan
- **WHEN** les réglages sont actifs et que le launcher reste au premier plan
- **THEN** l'unique observation des compteurs de « Apps sources » reste utilisable ; au passage en arrière-plan puis au retour, elle suit son contrat de pause et de rafraîchissement
