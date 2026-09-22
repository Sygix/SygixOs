# Change : p2b-settings

## Why
P2b du roadmap : réglages du launcher. Jusqu'ici le choix des apps sources du héro / Top Shelf (réservé « v1.x » dans la spec launcher-shell) n'a pas d'UI, et aucune app ne peut être masquée de la grille. Cette spec introduit l'entrée réglages (icône flottante en haut à droite du héro) et une page de réglages dédiée à la tvOS.

## What Changes
- **Icône réglages flottante** : engrenage discret en verre translucide, haut à droite du héro, accessible par appui haut depuis le héro, OK ouvre les réglages
- **Page de réglages plein écran** (style tvOS) : volet catégories à gauche (« Apps sources », « Applications cachées », « À propos »), contenu à droite, fond sombre neutre, DPAD uniquement
- **Apps sources** : toutes les apps TV installées listées (icône, nom, nombre de programmes publiés), toggle switch Apple par ligne, effet immédiat sur héro et Top Shelf sans redémarrage, persisté DataStore, défaut : toutes activées — matérialise l'exigence « Sélection des apps sources » de launcher-shell
- **Menu contextuel** : nouvelle option « Cacher » (en plus d'épingler/déplacer) ; une app cachée disparaît de la grille ET du dock
- **Applications cachées** : sous-écran plein écran listant les apps cachées, switch de réactivation par ligne + bouton « Tout réactiver »
- **À propos** : version de l'app + licences OSS
- Retour depuis les réglages : retour au héro (focus héro), comportement standard

## Impact
- specs affectées : nouvelle capability `settings` (delta ci-dessous)
- launcher-shell : l'exigence « Sélection des apps sources » devient implémentée par cette capability (pas de modification de son texte)
- persistance : nouveaux états DataStore (apps sources activées, apps cachées)
