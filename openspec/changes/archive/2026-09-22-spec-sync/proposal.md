# Change : spec-sync

Aligne deux exigences de launcher-shell sur le comportement réellement implémenté et testé sur la TV.

## Why
Relecture de fin de session : la spec courante contredit le code sur deux points, sans qu'aucun comportement ne doive changer.
- « Qualité des visuels » annonce 960 px alors que le seuil retenu avec Sygix est 1080 px (« Préchargement et mémoire » dit déjà 1080).
- « Préchargement et mémoire » annonce la validation de tous les visuels au chargement. Le TV Provider de la TCL publie environ 500 programmes : tout valider saturait le réseau et relançait les minuteurs du panneau. Le code valide les premiers visuels du héro au chargement, puis les affiches d'une app quand le focus s'y pose.

## What Changes
- Seuil de qualité porté à 1080 px dans « Qualité des visuels » (aucun changement de code)
- « Préchargement et mémoire » décrit la validation bornée et à la demande

## Impact
- specs affectées : launcher-shell (MODIFIED « Qualité des visuels », « Préchargement et mémoire »)
- aucun changement de code : documentation seule
