# Design : p2b-settings

## Décisions

### Point d'entrée : icône flottante, pas de bouton dans le dock
L'icône réglages est un élément overlay du héro, en haut à droite, en verre translucide faible opacité (cohérent avec les surfaces verre du thème). Elle est atteinte par **appui haut depuis le héro** : la nav 3 paliers (héro → dock → grille) est étendue en héro → (haut) réglages / (bas) dock. L'icône est hors des paliers dock/grille ; depuis la grille, on n'y accède pas directement (Retour → dock → héro → haut).

### Page réglages : deux volets tvOS
Plein écran, fond sombre neutre (pas de héro derrière — la lecture est mise en pause). Volet gauche : catégories focusables (une à la fois, comme les paliers du home). Volet droit : liste verticale de lignes. Gauche/droite basculent le focus entre volets ; seul le volet actif est focusable (même règle que le home).

### Switch Apple
Composant switch unique réutilisé partout (apps sources, apps cachées) : OK bascule, animation courbe Apple, état persisté en DataStore immédiatement (écriture à chaque bascule, pas de bouton « Enregistrer »).

### Masquage vs apps sources : deux axes indépendants
- « Cacher » (menu contextuel) = visibilité de l'app dans la grille et le dock
- « Apps sources » (réglages) = contribution des programmes de l'app au héro / Top Shelf

Une app cachée reste comptée dans les apps sources et vice versa. Le masquage survit à la réinstallation de l'app.

### Licences OSS et version
Plugin Gradle `aboutlibraries` : les licences sont générées automatiquement depuis les dépendances réelles du build, affichées dans « À propos » (liste navigable DPAD). La licence GPL-3.0 du repo est également présentée dans cette section. La version affichée provient du versionName de l'app.

### Nombre de programmes publiés
Affiché sous le nom dans la ligne « Apps sources » (comptage TV Provider par app, requête légère au chargement de la catégorie, cache accepté) — utile pour diagnostiquer une app qui ne publie rien.

## Non-objectifs (v1 des réglages)
- Pas de réglages du héro (durée du diaporama, lecture auto) — P2c+
- Pas de réglages screensaver / Up Next — P3/P4
- Pas de recherche dans les listes d'apps
