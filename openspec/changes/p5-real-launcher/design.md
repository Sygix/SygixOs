# Design

## Context

Simon a confirmé le périmètre produit P5 suivant : faire de SygixOs un vrai launcher, inclure le rôle système HOME, fournir une option de démarrage à l'allumage TV, et prévoir le recours à AccessibilityService lorsque l'OEM détourne Home, sous réserve de conformité. Ces décisions de présence fonctionnelle ne sont pas à reconfirmer. Restent ouverts la sémantique du contrôle « remplacer le launcher », l'état initial de l'option, les mécanismes Android exacts et les limites OEM. Le manifeste déclare déjà SygixOs comme cible HOME; le README décrit encore une procédure ADB manuelle qui désactive des apps système.

Les changements ouverts `up-next` et `jellyfin-tvprovider-only` ne couvrent pas P5. Le premier modifie déjà la capability `settings` et demeure actif; éviter de modifier ses exigences dans le présent change.

La voie Android documentée pour le rôle HOME est `RoleManager.ROLE_HOME`; l'application doit vérifier que le rôle est disponible et laisser l'utilisateur confirmer l'attribution. L'éligibilité repose sur les filtres HOME/DEFAULT du manifeste. Sources: [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager), [Créer une application TV](https://developer.android.com/training/tv/get-started/create).

Le service `AccessibilityService` est un accès sensible, activé explicitement par l'utilisateur dans les réglages système, et pas une permission runtime ordinaire. Android le destine à l'assistance aux personnes handicapées; son utilisation pour une substitution de touche Home nécessite validation de conformité et ne doit pas être présentée comme une capacité générale garantie. Source: [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). Des guides tiers de Projectivy rapportent un contournement via accessibilité et démarrage automatique sur certains appareils, mais cela ne garantit ni disponibilité ni comportement identique selon OEM/version.

## Goals / Non-Goals

**Goals:**
- Pour les parcours retenus, séparer l'explication SygixOs des consentements et écrans de contrôle Android.
- Actualiser les états système exposés par les contrôles retenus au retour depuis Réglages système.
- Rendre les limitations et échecs non bloquants, et la désactivation explicite.
- Spécifier le fonctionnel maintenant; différer le détail visuel à la sélection du mockup par Simon.

**Non-Goals:**
- Désactivation ADB ou automatique de composants système.
- Promesse d'interception de Home ou de démarrage automatique universelle.
- Compléter les exigences de rendu UI avant sélection d'une variante du mockup.
- Absorber la mise à jour documentaire roadmap traitée ailleurs.

## Decisions

- Le rôle HOME se demande par l'API système officielle, après une action utilisateur; aucun réglage n'est changé silencieusement.
- SygixOs relit l'état réel après chaque retour au premier plan depuis les réglages système, plutôt que de déduire un succès d'une tentative d'ouverture.
- Si une présentation propre à SygixOs est retenue, elle informe et route vers l'étape validée; elle n'est jamais présentée comme le dialogue de permission Android.
- L'accessibilité n'est jamais activée par SygixOs seule. Un éventuel événement Home ne peut être traité que si le système le transmet; l'application doit prévoir absence, arrêt et refus.
- Le rôle HOME et l'option de démarrage à l'allumage font partie du périmètre confirmé. Le contrôle de remplacement, leur présentation/état initial et leurs mécanismes précis restent à définir; graphisme, hiérarchie, testTags UI, placement et parcours D-pad restent en attente du choix d'une variante du fichier « SygixOs UI tvOS 26-png.zip ».
- Avant archivage de ce change, archiver `up-next`, qui modifie également `settings`, puis appliquer P5 à la baseline résultante. `jellyfin-tvprovider-only` est indépendant de ces capacités et n'impose pas d'ordre relatif.

## Risks / Trade-offs

- [L'usage du service d'accessibilité pourrait ne pas être approprié ou admis pour ce besoin] → Valider conformité et politique de distribution avant implémentation; ne pas déclencher l'activation sans décision éclairée.
- [Le HOME role, Home hardware key et démarrage varient par OEM] → États disponibles/inactifs/échec et comportement non bloquant; vérification sur l'appareil avant le code dépendant du comportement observé.
- [La sémantique du contrôle « remplacement du launcher » et l'état initial de l'option de démarrage ne sont pas décidés] → Ne pas présumer qu'une bascule unique contrôle HOME, accessibilité et démarrage, ni activer l'option par défaut.

## Migration Plan

Aucune migration de données existante prévue. Ne pas modifier automatiquement le choix HOME ou les réglages d'accessibilité. L'option de démarrage est dans le périmètre, mais son état initial et son mécanisme restent à décider; les mécanismes doivent être vérifiés sur appareil avant leur implémentation. Archive seulement après merge et validation sur TV réelle selon les règles du dépôt.

## Open Questions

1. Quelle sémantique donner au contrôle « remplacer le launcher »? A) indicateur/raccourci d'état HOME, options distinctes — recommandé, to confirm; B) contrôle groupé HOME + accessibilité + démarrage; C) contrôles séparés sans contrôle global.
2. Quel état initial pour l'option de démarrage déjà retenue? A) désactivée jusqu'au choix explicite — recommandé, to confirm; B) préserver le comportement système existant; C) demander le choix dans une présentation initiale.
3. Quel parcours utilisateur pour proposer l'attribution HOME et l'option de démarrage (présentation initiale, réglages, ou les deux)? A) réglages uniquement — recommandé, to confirm; B) présentation initiale ignorable puis réglages; C) décider après validation sur appareil.
4. Quels mécanismes système sont autorisés et disponibles pour le démarrage à l'allumage et quelles limites OEM s'appliquent? A) vérifier sur TV cible avant implémentation — recommandé, to confirm; B) documenter des comportements OEM distincts après vérification; C) ne livrer que les mécanismes universellement autorisés.
5. L'interception Home via `AccessibilityService` est-elle conforme au produit et à sa distribution? A) ne pas activer avant vérification de conformité — recommandé, to confirm; B) la livrer si conformité vérifiée et avec activation explicite; C) retirer ce recours si conformité non établie.

## UI Placeholder

Les exigences visuelles et de navigation D-pad de la popup initiale et de la catégorie P5 sont à compléter après choix explicite par Simon d'une variante de la maquette « SygixOs UI tvOS 26-png.zip ». Ne pas extrapoler ses images ou inventer une variante.
