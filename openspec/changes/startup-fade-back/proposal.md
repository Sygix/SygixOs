# Change : startup-fade-back

## Why
Au test de la rc.7 sur la TV de référence, la touche Retour pressée pendant le fondu de sortie de l'écran de démarrage est absorbée par l'accueil : SygixOs reste au premier plan (sujet n° 3 du suivi post-release, `openspec/backlog.md`). « Écran de démarrage » prévoit que Retour suit le comportement normal du système (SygixOs quitte le premier plan) « pendant l'écran de démarrage », sans dire si le fondu de sortie en fait partie ; l'implémentation rend l'accueil interactif dès le début du fondu, et son traitement de Retour (retour au héro, « Navigation 3 paliers ») intercepte la touche avant le système.

## What Changes
- **Le fondu de sortie appartient à l'écran de démarrage** (décision de Sygix, actée) : Retour pressé entre le début et la fin du fondu de sortie suit le comportement normal du système — SygixOs quitte le premier plan, sans crash — au lieu d'être traité par l'accueil. La touche Retour reste non interceptée pendant tout l'écran de démarrage, fondu de sortie compris.
- « Écran de démarrage » gagne un scénario dédié « Retour pendant le fondu de sortie » qui spécifie ce comportement ; le scénario existant « Retour pendant l'écran de démarrage » est repris sans changement de comportement hors fondu ; les autres scénarios de l'exigence et les exigences « Écran de démarrage sans animation » et « Couverture de l'écran de démarrage » sont reprises sans changement de comportement.

## Capabilities

### New Capabilities
Aucune.

### Modified Capabilities
- `launcher-shell` : MODIFIED « Écran de démarrage » (version de `rc6-polish`), « Écran de démarrage sans animation » (version de `rc5-tv-fixes`).
- `ui-testing` : MODIFIED « Couverture de l'écran de démarrage » (version de `rc6-polish`).
- `settings` : non touchée.

## Dépendances et chevauchements
- Ce change reprend des exigences portées par des changes non archivés ; chaque delta MODIFIED part de leur dernier texte et garde tous leurs scénarios : « Écran de démarrage » vient de `startup-splash`, reprise par `rc5-tv-fixes` puis `rc6-polish` (ce change part du texte de `rc6-polish`) ; « Écran de démarrage sans animation » vient de `startup-splash`, reprise par `rc5-tv-fixes`, non touchée par `rc6-polish` (ce change part du texte de `rc5-tv-fixes`) ; « Couverture de l'écran de démarrage » vient de `startup-splash`, reprise par `rc5-tv-fixes` puis `rc6-polish` (ce change part du texte de `rc6-polish`).
- Les autres changes ouverts ne touchent aucune de ces exigences.
- **Ordre d'archivage** : après `ui-tvos-polish`, `startup-splash`, `self-update`, `rc5-tv-fixes` et `rc6-polish`. Archiver ce change avant eux est refusé par `openspec archive` (exigences cibles absentes).

## Impact
- `ui/home/StartupSplash.kt` : le fondu de sortie en cours est transmis à l'accueil composé sous l'écran de démarrage.
- `ui/home/HomeScreen.kt` : le traitement de la touche Retour par l'accueil (retour au héro) ne s'applique qu'après la fin du fondu de sortie ; les autres touches restent traitées comme aujourd'hui pendant le fondu (héro focusable, D-pad actif).
- `app/src/test/java/fr/sygix/sygixos/ui/home/` : tests Compose existants (`StartupSplashTest` et apparentés) complétés pour le scénario « Retour pendant le fondu de sortie » et la variante sans animation ; aucun nouveau fichier de production.
- Le sujet n° 3 du suivi post-release (`openspec/backlog.md`) est traité ; sa piste est retirée du suivi au moment de l'archivage.

## Non-goals
- Le traitement des autres touches (D-pad, OK) pendant le fondu de sortie : inchangé, déjà couvert par « Écran de démarrage » (scénario « touches pendant l'écran de démarrage » : aucun effet avant le fondu ; héro focusable dès le début du fondu).
- Les sujets n° 2 (cadence de la mascotte) et n° 8 (image longue au début du fondu) du suivi post-release : autres constats du même test, hors périmètre de ce change.
- Retour pressé avant ou après le fondu de sortie : comportements inchangés (avant : comportement système, déjà couvert ; après : « Navigation 3 paliers »).

## Questions ouvertes
Aucune : Sygix a tranché — le fondu de sortie appartient à l'écran de démarrage, Retour suit le comportement normal du système pendant toute sa durée.
