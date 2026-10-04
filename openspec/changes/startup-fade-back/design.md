# Design

## Context
Sujet n° 3 du suivi post-release : au test de la rc.7, Retour pressé pendant le fondu de sortie de l'écran de démarrage est absorbé par l'accueil. Dans le code actuel :

- `StartupHost` (`ui/home/StartupSplash.kt`) rend l'accueil interactif dès le début du fondu de sortie : `interactive = phase != StartupPhase.Splash`, donc `true` pendant `FadingOut` ;
- l'accueil composé (`ui/home/HomeScreen.kt`) traite alors `Key.Back` dans son `onPreviewKeyEvent` racin : Retour ramène le focus au héro (« Navigation 3 paliers ») et la touche n'atteint jamais le système, donc SygixOs reste au premier plan ;
- sans animation, `StartupGate` passe de `Splash` directement à `Done` (phase `FadingOut` sautée quand les animations sont désactivées) : l'accueil est interactif dès le remplacement instantané, sans période intermédiaire.

Sygix a tranché (décision actée) : le fondu de sortie appartient à l'écran de démarrage ; Retour suit le comportement normal du système (SygixOs quitte le premier plan) sur toute la durée de l'écran de démarrage, fondu de sortie compris.

## Decisions

### D1. Retour non intercepté pendant le fondu de sortie
L'état du fondu de sortie est transmis à l'accueil composé sous l'écran de démarrage : tant que le fondu n'est pas terminé, le traitement de `Key.Back` par l'accueil ne s'applique pas et la touche remonte au système, qui met l'activité en arrière-plan (comportement inchangé pour tout le reste de l'écran de démarrage, déjà couvert et testé). À la fin du fondu, l'accueil redevient entièrement interactif, Retour compris ; le héro détient le focus depuis le début du fondu et le D-pad reste actif pendant le fondu, comme aujourd'hui (scénario « focus au début du fondu » de la couverture de test, inchangé).

Alternative écartée : rendre tout l'accueil non interactif pendant le fondu (Retour compris, mais aussi D-pad et OK) — elle contredirait le scénario existant « focus au début du fondu » et la décision de Sygix ne porte que sur Retour.

### D2. Chemin sans animation inchangé
Quand les animations sont désactivées, l'écran de démarrage est remplacé instantanément (pas de fondu, donc pas de période `FadingOut`) : l'accueil est interactif dès le remplacement et Retour suit « Navigation 3 paliers ». Aucun changement de code ; un scénario « Retour sans fondu » et un scénario de test rendent cette frontière explicite pour éviter l'ambiguïté relevée dans le suivi.

## Risks / Trade-offs
- Retour pressé pendant le fondu met l'activité en arrière-plan alors que le fondu se joue : Android termine l'animation en arrière-plan ; au retour sur le launcher, l'accueil s'affiche directement sans écran de démarrage (scénario « retour sur le launcher », déjà spécifié et testé). Rien à ajouter.
- La mécanique exacte (quelle valeur est transmise à l'accueil) reste un détail d'implémentation ; la spec n'exige que le comportement observable.
