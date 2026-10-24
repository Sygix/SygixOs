## MODIFIED Requirements

### Requirement: Page de réglages
Les réglages SHALL conserver deux volets plein écran, leur fond et leurs pilules sans zoom, accueil masqué sans perdre son état. Une catégorie Suivi de visionnage SHALL accueillir le compte et ses réglages. Une catégorie Langue SHALL précéder À propos. Les panneaux ouverts SHALL recevoir Retour avant la page ; navigation et restauration SHALL conserver les comportements existants.

#### Scenario: structure
- **WHEN** les réglages s'ouvrent
- **THEN** les catégories sont Apps sources, Applications cachées, Écran d'accueil, Suivi de visionnage, Langue et À propos ; la première a le focus et son contenu est à droite

#### Scenario: changement de catégorie
- **WHEN** haut/bas est pressé à gauche puis droite
- **THEN** la catégorie active et son contenu changent ; droite atteint le premier contrôle du volet, sauf la règle existante de première ligne d'Applications cachées ; gauche revient à la catégorie sauf panneau ou liste actif

#### Scenario: retour
- **WHEN** Retour est pressé
- **THEN** une liste ou un panneau actif se ferme d'abord avec restauration de son contrôle ; sinon les réglages ferment sur le héro focusé et sa lecture reprend à la position conservée

#### Scenario: accueil masqué
- **WHEN** les réglages sont affichés
- **THEN** aucun élément du home n'est dessiné ni focusable dessous ; panneaux et listes utilisent les réglages comme arrière-plan, selon leur matériau

#### Scenario: retour immédiat
- **WHEN** les réglages sont quittés par Retour ou Home au premier plan
- **THEN** visuel, titre et bouton du héro reviennent sans mascotte, écran noir ou fondu d'entrée ; sa vidéo reprend sans rechargement sauf libération pendant un passage en arrière-plan

#### Scenario: fond
- **WHEN** les réglages s'ouvrent
- **THEN** leur fond reste celui de la zone grille

#### Scenario: focus d'une ligne
- **WHEN** une ligne prend le focus
- **THEN** elle devient une pilule claire à texte et icônes sombres sans zoom, halo ou verre, sans changer son rectangle ; les lignes au repos n'ont pas de fond, sauf Tout réactiver qui garde son fond discret

#### Scenario: catégorie active, focus à droite
- **WHEN** le focus est à droite
- **THEN** la catégorie active garde sa pilule grise et son texte blanc, les autres catégories sans fond

#### Scenario: vignette des lignes d'apps
- **WHEN** une ligne d'app est affichée
- **THEN** elle garde la vignette 16:9, bannière sinon icône centrée ; le fournisseur de suivi n'est pas ajouté à la liste des applications

**Détails.** — Le focus des catégories, lignes, contenus, dépliages, contrôles et actions reprend la pilule claire sans zoom, halo ou verre, et l'état marqué de la catégorie active. Les parcours de connexion et pays respectent cette base et leur navigation propre ; les autres catégories ne changent pas de règles.

## ADDED Requirements

### Requirement: Langue de l'interface
Tous les textes de SygixOs SHALL s'afficher dans la langue de l'interface : français, anglais, allemand, espagnol ou italien. Sans choix explicite, SygixOs SHALL suivre la langue de la TV : la première langue du système qui est traduite, sinon l'anglais. Un texte cité par une spec désigne le texte du catalogue français ; les autres langues en portent la traduction. Les données externes (titres, résumés, services) restent dans la langue de leur source.

#### Scenario: TV dans une langue traduite
- **WHEN** aucune langue n'est choisie et la langue de la TV est l'allemand
- **THEN** l'accueil, les menus, les réglages, la configuration de l'écran d'accueil et les messages s'affichent en allemand

#### Scenario: TV dans une langue non traduite
- **WHEN** aucune langue n'est choisie et la TV est réglée dans une langue non traduite, sans autre langue traduite dans sa liste
- **THEN** SygixOs s'affiche en anglais

#### Scenario: plusieurs langues sur la TV
- **WHEN** la liste des langues de la TV commence par une langue non traduite suivie du français
- **THEN** SygixOs s'affiche en français

#### Scenario: langue de la TV modifiée
- **WHEN** la langue de la TV change pendant que SygixOs tourne et qu'aucune langue n'est choisie
- **THEN** SygixOs passe à la nouvelle langue dès son retour au premier plan

#### Scenario: formats suivant la langue
- **WHEN** l'interface est dans une langue donnée
- **THEN** les dates courtes, l'heure, les noms de pays et leur ordre alphabétique suivent cette langue

#### Scenario: écrans d'une autre application
- **WHEN** un texte décrit un bouton d'Android ou de Tsuyu
- **THEN** il cite le libellé de ce bouton dans la langue de l'interface : celui d'Android pour ses écrans, celui que Tsuyu affiche dans cette langue pour Tsuyu

### Requirement: Catégorie « Langue »
La catégorie « Langue » SHALL afficher l'en-tête des autres catégories (titre « Langue », description « Choisissez la langue de SygixOs. Par défaut, il suit la langue de la TV. ») puis une ligne « Langue de la TV » et une ligne par langue, nommée dans sa propre langue (Deutsch, English, Español, Français, Italiano). La ligne retenue SHALL porter une coche ; les lignes SHALL garder la forme et le focus des lignes de réglages.

#### Scenario: contenu
- **WHEN** la catégorie « Langue » est affichée
- **THEN** « Langue de la TV » vient en premier avec, en texte secondaire, le nom de la langue qu'elle donne, puis Deutsch, English, Español, Français et Italiano ; seule la ligne retenue a une coche

#### Scenario: langue de la TV non traduite
- **WHEN** la langue de la TV n'est pas traduite
- **THEN** le texte secondaire de « Langue de la TV » indique qu'elle n'est pas traduite et que l'anglais est utilisé

#### Scenario: navigation D-pad
- **WHEN** droite est pressé sur la catégorie « Langue »
- **THEN** le focus va sur la ligne retenue ; haut et bas parcourent les lignes sans boucler aux bords ; gauche revient à la catégorie ; Retour ferme les réglages selon « Page de réglages »

#### Scenario: retour d'une autre app
- **WHEN** l'utilisateur revient dans SygixOs après l'avoir quitté depuis ce volet
- **THEN** le volet et la ligne focalisée sont restaurés selon les règles des réglages

### Requirement: Choix de la langue
OK sur une ligne SHALL appliquer ce choix à SygixOs seul, sans changer la langue de la TV ni celle des autres applications : une langue la fixe, « Langue de la TV » revient au suivi de la TV. Le choix SHALL être conservé par le système après un redémarrage, une mise à jour ou l'arrêt de SygixOs. Choisir la ligne déjà retenue SHALL ne rien faire.

#### Scenario: choisir une langue
- **WHEN** OK est pressé sur Italiano alors que « Langue de la TV » est retenue
- **THEN** SygixOs s'affiche en italien, les réglages restent ouverts sur la catégorie « Langue » avec le focus sur Italiano, qui porte la coche

#### Scenario: revenir à la langue de la TV
- **WHEN** OK est pressé sur « Langue de la TV » alors qu'une langue est fixée
- **THEN** SygixOs reprend la langue de la TV et la coche passe sur « Langue de la TV »

#### Scenario: même choix
- **WHEN** OK est pressé sur la ligne qui porte déjà la coche
- **THEN** rien ne change et l'interface ne se recharge pas

#### Scenario: choix conservé
- **WHEN** la TV redémarre ou SygixOs est mis à jour après le choix d'une langue
- **THEN** SygixOs s'affiche dans la langue choisie, quelle que soit la langue de la TV

#### Scenario: autres applications
- **WHEN** une langue est choisie dans SygixOs
- **THEN** les menus Android et les autres applications gardent la langue de la TV
