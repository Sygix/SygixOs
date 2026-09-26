# Change : remove-screensaver

## Why
Google TV garde son propre économiseur d'écran (mode Ambiant), géré par le système et non par le launcher. Un launcher ne le remplace pas : le screensaver de SygixOs n'aurait pas sa place. Décision de Sygix : retirer la capability. Aucun code n'existe pour elle.

## What Changes
- Suppression de la capability `screensaver` et de ses deux exigences (« Lecture », « Configuration »)
- Retrait de la phase P3 de la roadmap du README

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `screensaver` : REMOVED « Lecture », REMOVED « Configuration »

## Impact
Aucun code, aucune autre spec ne référence le screensaver. Le repli nature du héro (clips Pexels) n'est pas concerné.
