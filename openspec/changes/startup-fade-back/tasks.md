# Tasks

## 1. Comportement
- [ ] 1.1 Retour non intercepté par l'accueil pendant le fondu de sortie (D1) ; test Compose du scénario « Retour pendant le fondu de sortie » (« Couverture de l'écran de démarrage ») à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) : le test échoue sur le code actuel et passe avec le changement ; le test sans Retour laisse l'accueil au premier plan
- [ ] 1.2 Scénario « Retour sans fondu » couvert (D2) : Retour pendant l'écran de démarrage sans animation quitte le premier plan, Retour après le remplacement suit « Navigation 3 paliers » ; tests existants de la couverture sans animation adaptés

## 2. Validation
- [ ] 2.1 `./gradlew test` vert ; `openspec validate --all --strict` vert
- [ ] 2.2 Sur la TV réelle, en `assembleRelease` : Retour pendant le fondu de sortie quitte SygixOs au premier plan ; Retour avant le fondu inchangé ; Retour après le fondu ramène au héro comme avant ; sans animation, la frontière du remplacement instantané vérifiée
- [ ] 2.3 `openspec archive startup-fade-back` après `ui-tvos-polish`, `startup-splash`, `self-update`, `rc5-tv-fixes` et `rc6-polish`, puis retirer le sujet n° 3 du suivi post-release (`openspec/backlog.md`)
