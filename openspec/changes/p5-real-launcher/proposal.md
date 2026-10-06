# Proposal

## Why

SygixOs se déclare déjà comme écran d'accueil Android, mais ne prend pas encore en charge le parcours utilisateur complet pour le choisir ni les contournements de la touche Home observés sur certains appareils. L'actuelle procédure ADB du README désactive manuellement des applications système et ne constitue pas une expérience réversible intégrée; P5 doit fournir un vrai launcher, tout en rendant explicites les limites dépendantes des appareils.

## What Changes

- Inclure dans P5 un vrai launcher, le rôle HOME et une option de démarrage à l'allumage TV, conformément aux décisions confirmées de Simon; la sémantique du contrôle, l'état initial et le parcours de présentation restent ouverts.
- Prévoir le recours à AccessibilityService lorsque l'OEM détourne Home, sous réserve de validation de conformité et de politique de distribution; ne pas activer le service sans action explicite de l'utilisateur.
- Pour tout parcours retenu, distinguer l'explication SygixOs des écrans système, respecter l'action explicite de l'utilisateur et traiter les indisponibilités sans promettre un comportement uniforme entre OEM/versions.
- Si ces interfaces sont retenues, leur UI s'inspirera de la maquette « SygixOs UI tvOS 26-png.zip »; les exigences visuelles et de navigation restent en placeholder jusqu'à la sélection explicite d'une variante par Simon.
- Ne pas automatiser la désactivation ADB du launcher Google TV ni d'autres applications système.

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell`: mécanismes d'offre et d'activation du rôle HOME et comportement de remplacement.
- `settings`: section P5, avec option de démarrage à l'allumage confirmée; la sémantique du contrôle et son état initial restent à décider.
- `ui-testing`: validation fonctionnelle des parcours et états système; exigences visuelles en attente du mockup choisi.

## Impact

Manifest et intégration Android de SygixOs, cycle de vie de l'application, persistance locale des options, service d'accessibilité Android et démarrage système selon les possibilités autorisées par la plateforme/OEM; Settings, ressources françaises, tests et README. Aucune dépendance serveur n'est prévue. Le change `up-next` est ouvert et modifie déjà la capability `settings`: archive order à respecter, voir `design.md`.

## Non-goals

- Désactiver automatiquement le launcher Google TV ou SetupWraith, notamment par ADB.
- Garantir que l'interception de Home ou le démarrage automatique fonctionnent sur tous les appareils.
- Détailler ou implémenter l'UI avant la sélection d'une variante de maquette par Simon.
