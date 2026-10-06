# Proposal

## Why

SygixOs se déclare déjà comme écran d'accueil Android, mais ne prend pas encore en charge le parcours utilisateur complet pour le choisir ni les contournements de la touche Home observés sur certains appareils. L'actuelle procédure ADB du README désactive manuellement des applications système et ne constitue pas une expérience réversible intégrée; P5 doit fournir un vrai launcher, tout en rendant explicites les limites dépendantes des appareils.

## What Changes

- Ajouter la procédure intégrée d'offre du rôle HOME au premier lancement, avec consentement explicite au dialogue système Android et distinction avec la popup explicative propre à SygixOs.
- Fournir dans Settings le contrôle du remplacement du launcher, le recours conditionnel à un service d'accessibilité pour les appareils qui détournent Home, et une option de démarrage au démarrage TV.
- Spécifier l'activation explicite de l'accessibilité par l'utilisateur, la réversibilité, les indisponibilités et échecs; ne pas promettre un comportement uniforme entre OEM/versions.
- Prévoir une section P5 dans Settings et une popup initiale inspirées de la maquette « SygixOs UI tvOS 26-png.zip »; les exigences UI détaillées restent volontairement en placeholder jusqu'au choix par Simon d'une variante du mockup.
- Ne pas automatiser la désactivation ADB du launcher Google TV ni d'autres applications système.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell`: mécanismes d'offre et d'activation du rôle HOME et comportement de remplacement.
- `settings`: options de remplacement, accessibilité et démarrage TV, ainsi que section P5.
- `ui-testing`: validation fonctionnelle des parcours et états système; exigences visuelles en attente du mockup choisi.

## Impact

Manifest et intégration Android de SygixOs, cycle de vie de l'application, persistance locale des options, service d'accessibilité Android et démarrage système selon les possibilités autorisées par la plateforme/OEM; Settings, ressources françaises, tests et README. Aucune dépendance serveur n'est prévue. Le change `up-next` est ouvert et modifie déjà la capability `settings`: archive order à respecter, voir `design.md`.

## Non-goals

- Désactiver automatiquement le launcher Google TV ou SetupWraith, notamment par ADB.
- Garantir que l'interception de Home ou le démarrage automatique fonctionnent sur tous les appareils.
- Détailler ou implémenter l'UI avant la sélection d'une variante de maquette par Simon.
