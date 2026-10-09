# Maquettes P5

## Statut

**v1 et v2 sont remplacées** (décision du propriétaire du 2026-10-10) : elles ne sont ni une référence visuelle ni une exigence. Elles plaçaient l'action HOME dans « Apps sources », ce que la décision 8 de `design.md` écarte : les contrôles P5 vont dans la catégorie « Écran d'accueil » créée par `up-next`.

Les nouvelles maquettes seront faites dans Penpot et fournies aux agents d'implémentation :
- la présentation initiale (rôle HOME, choix du démarrage à l'allumage, service d'accessibilité) ;
- les contrôles P5 dans la catégorie « Écran d'accueil » des réglages (« Remplacer le launcher », démarrage à l'allumage, service d'accessibilité), avec leurs états.

Jusqu'à leur réception, le rendu, les libellés définitifs, la place des contrôles et la navigation D-pad détaillée restent en attente (`design.md`, « Maquettes » et question ouverte 5).

## Fichiers remplacés

- `v1-settings-home-choice.png` / `.html` — « Apps sources » avec une ligne d'action HOME focalisée, juste avant l'ouverture du dialogue système. Remplacée.
- `v2-settings-home-return.png` / `.html` — même écran au retour dans SygixOs, résultat non confirmé. Remplacée.

Ces deux planches ne dessinaient que des écrans SygixOs, aucun écran Android, constructeur ni d'application tierce.

## Composants existants à reprendre

Repères dans le code pour les nouvelles maquettes (le rendu suit les exigences de `settings`, « Page de réglages », sans en recopier les valeurs) :
- `ui/settings/SettingsScreen.kt` — `SettingsCategory`, volets catégories et contenu, navigation D-pad entre volets.
- `ui/settings/SettingsContent.kt` — lignes de réglage et pilule de focus (`focusPill`, `animatedPillColors`).
- `ui/settings/AboutContent.kt` — `AboutActionRow` (titre et détail), modèle d'une ligne d'action avec état.
- `core/designsystem/Theme.kt`, `FocusPill.kt`, `Motion.kt` — couleurs, focus et dimensions du design system.
