# Design

## Context

Voir `proposal.md` pour le besoin et les capacités touchées. Le manifeste déclare déjà SygixOs comme cible HOME; le README décrit encore une procédure ADB manuelle qui désactive des apps système. Les changements ouverts `up-next` et `jellyfin-tvprovider-only` ne couvrent pas P5. Le premier modifie déjà la capability `settings` et demeure actif; éviter de modifier ses exigences dans le présent change.

La voie Android documentée pour le rôle HOME est `RoleManager.ROLE_HOME`; l'application doit vérifier que le rôle est disponible et laisser l'utilisateur confirmer l'attribution. L'éligibilité repose sur les filtres HOME/DEFAULT du manifeste. Sources: [RoleManager](https://developer.android.com/reference/android/app/role/RoleManager), [Créer une application TV](https://developer.android.com/training/tv/get-started/create).

Le service `AccessibilityService` est un accès sensible, activé explicitement par l'utilisateur dans les réglages système, et pas une permission runtime ordinaire. Android le destine à l'assistance aux personnes handicapées; son utilisation pour une substitution de touche Home nécessite validation de conformité et ne doit pas être présentée comme une capacité générale garantie. Source: [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService). Des guides tiers de Projectivy rapportent un contournement via accessibilité et démarrage automatique sur certains appareils, mais cela ne garantit ni disponibilité ni comportement identique selon OEM/version.

## Goals / Non-Goals

**Goals:**
- Séparer l'explication SygixOs des consentements et écrans de contrôle Android.
- Actualiser les états HOME et accessibilité au retour depuis Réglages système.
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
- La popup propre à SygixOs informe et route vers l'étape concernée; elle n'est jamais présentée comme le dialogue de permission Android.
- L'accessibilité n'est jamais activée par SygixOs seule. Un éventuel événement Home ne peut être traité que si le système le transmet; l'application doit prévoir absence, arrêt et refus.
- Les réglages sont une catégorie P5, conformément à la demande, mais son graphisme, sa hiérarchie précise, les testTags UI, le placement et le parcours D-pad sont explicitement en attente du choix d'une variante du fichier « SygixOs UI tvOS 26-png.zip ».
- Avant archivage de ce change, archiver `up-next`, qui modifie également `settings`, puis appliquer P5 à la baseline résultante. `jellyfin-tvprovider-only` est indépendant de ces capacités et n'impose pas d'ordre relatif.

## Risks / Trade-offs

- [L'usage du service d'accessibilité pourrait ne pas être approprié ou admis pour ce besoin] → Valider conformité et politique de distribution avant implémentation; ne pas déclencher l'activation sans décision éclairée.
- [Le HOME role, Home hardware key et démarrage varient par OEM] → États disponibles/inactifs/échec et comportement non bloquant; vérification sur l'appareil avant le code dépendant du comportement observé.
- [La bascule « remplacement du launcher » est ambiguë par rapport aux sous-options HOME, accessibilité et démarrage] → Décision produit demandée ci-dessous; ne pas présumer qu'une bascule unique contrôle ou active le service d'accessibilité.

## Migration Plan

Aucune migration de données existante prévue. La politique d'état initiale des options reste à décider; ne pas modifier automatiquement le choix HOME ou les réglages d'accessibilité. Les étapes d'implémentation dépendent des décisions ouvertes et de la validation sur appareil. Archive seulement après merge et validation sur TV réelle selon les règles du dépôt.

## Open Questions

1. Quel périmètre donner au contrôle « remplacer le launcher système »? A) simple raccourci vers demande/état HOME, options distinctes — recommandé, to confirm; B) contrôle groupé HOME + accessibilité + démarrage; C) le retirer au profit de contrôles séparés.
2. Quelle politique d'état initial appliquer aux options P5? A) non activées jusqu'au choix explicite de l'utilisateur — recommandé, to confirm; B) conserver les états système existants et demander séparément les capacités manquantes; C) proposer le choix dans l'accueil initial.
3. À quel moment demander l'activation du service d'accessibilité? A) uniquement après action volontaire dans P5 — recommandé, to confirm; B) dans le parcours initial avec étape de consentement distincte; C) ne pas livrer ce service avant confirmation de conformité Android/distribution.
4. Après refus de HOME ou d'accessibilité, quand reproposer? A) uniquement depuis la catégorie P5 — recommandé, to confirm; B) proposer une seule fois au prochain démarrage; C) réafficher à chaque lancement jusqu'à activation.
5. L'interception Home via `AccessibilityService` est-elle autorisée pour ce produit et sa distribution? A) ne pas activer avant vérification de conformité — recommandé, to confirm; B) la livrer comme compatibilité expérimentale si conforme; C) exclure le service et ne proposer que le rôle HOME.

## UI Placeholder

Les exigences visuelles et de navigation D-pad de la popup initiale et de la catégorie P5 sont à compléter après choix explicite par Simon d'une variante de la maquette « SygixOs UI tvOS 26-png.zip ». Ne pas extrapoler ses images ou inventer une variante.
