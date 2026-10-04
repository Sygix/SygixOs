# Suivi : sujets reportés après v0.0.1

Sujets constatés pendant la préparation de v0.0.1 et reportés au prochain lot pour ne pas bloquer la release publique. Aucun n'est spécifié ni tranché : chacun deviendra un change OpenSpec (`openspec/changes/<id>/`) ou sera rattaché à un change existant au moment de son traitement. Les pistes ci-dessous sont des hypothèses de travail, pas des décisions.

## 1. Visuels du héro servis par un fournisseur `content://` lent

**Constat** : certaines apps publient leurs visuels sous forme d'URI `content://` servies par leur propre fournisseur. Avec le client Jellyfin, la vérification d'une image prend environ 5 à 6 s et son chargement environ 11 s, contre moins de 0,5 s pour les images HTTPS publiées par d'autres apps. Les visuels étant vérifiés l'un après l'autre (« Préchargement et mémoire », scénario « validation au chargement »), une app lente retarde la vérification des visuels de toutes les apps, donc le premier visuel prêt du héro, que l'écran de démarrage attend dans la limite de son plafond (« Écran de démarrage »).

**Pistes** :
- vérifications en parallèle, ou par app, avec un délai borné par visuel ;
- une URI qui dépasse ce délai n'est pas redemandée lors du même chargement, pour qu'une app lente ne pénalise pas les suivantes.

**À trancher** : valeur du délai, nombre de vérifications simultanées (compatible avec la limite de décodeurs de « Préchargement et mémoire »), sort d'une app lente (écartée du héro, ou ses visuels gardés pour plus tard) et sa durée.

## 2. Cadence de la mascotte de l'écran de démarrage

**Constat** : la mascotte est jouée à environ 30 images/s au lieu des 60 prévues (« Animation de la mascotte ») : la première composition de l'accueil, d'environ 250 ms, tombe pendant l'animation. Écart accepté pour v0.0.1.

**Pistes** :
- jouer la mascotte seule pendant la durée minimale de l'écran de démarrage, puis composer l'accueil ;
- composer l'accueil avant l'apparition de la mascotte.

**À trancher** : la piste retenue et son effet sur le délai d'affichage de l'accueil ; l'une comme l'autre modifie l'ordre de composition décrit dans « Écran de démarrage ».

## 3. Retour pendant le fondu de sortie de l'écran de démarrage

**Constat** : Retour pressé pendant le fondu de sortie de l'écran de démarrage est absorbé par l'accueil : SygixOs reste au premier plan. « Écran de démarrage » prévoit le comportement normal du système (SygixOs quitte le premier plan) « pendant l'écran de démarrage », sans dire si le fondu de sortie en fait partie.

**À trancher** : le fondu de sortie appartient-il à l'écran de démarrage (Retour quitte le premier plan) ou à l'accueil (Retour suit « Navigation 3 paliers ») ? La réponse sera écrite dans un scénario dédié.

## 4. Tests Robolectric des réglages instables sous forte charge

**Constat** : sous forte charge de la machine de build, des tests des réglages échouent par dépassement de délai (`HiddenPaneTest`, `SettingsNavigationTest`, `SettingsViewModelTest`). Ils passent de façon stable en CI.

**Piste** : rendre ces tests déterministes, sans dépendance au temps réel ni à la vitesse de la machine (horloge de test, attente d'un état plutôt que d'un délai).

**À trancher** : périmètre (ces trois classes seulement, ou un audit de tous les tests Compose) et critère de stabilité attendu.
