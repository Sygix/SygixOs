# Maquettes P5 ancrées aux écrans SygixOs

Ces deux maquettes remplacent les variantes conceptuelles rejetées (`v1-settings-distinct`, `v2-home-first`,
`v3-onboarding-optional`). Elles montrent **uniquement des écrans in-app SygixOs**; aucun écran, menu ou dialogue
Android/OEM ni UI d'application tierce n'est dessiné. Chaque aperçu est en 1920 × 1080 et son HTML autonome est la
source de rendu.

- [État 1 — avant l'action système](v1-settings-home-choice.png) ([HTML](v1-settings-home-choice.html)) — catégorie
  « Apps sources » sélectionnée, ligne d'action HOME focalisée. Android prend ensuite la main: la maquette s'arrête
  juste avant le transfert et **n'illustre pas** l'interface système.
- [État 2 — retour dans SygixOs](v2-settings-home-return.png) ([HTML](v2-settings-home-return.html)) — même écran
  relu au retour, focus conservé sur la ligne d'action; le résultat du choix HOME est présenté comme non confirmé.

## Ancrage et relevé de comparaison aux composants source

Aucune maquette n'a été dessinée de mémoire: chaque élément reprend un composant réel du dépôt (HEAD
`90003d918b3ddabb053b3e4f427cde7282471665`). Table de correspondance (mesures source en `dp`, valeurs maquette en `px`) :

| Élément maquette | Composant source | Mesure source | Valeur maquette |
| --- | --- | --- | --- |
| Pane catégories (colonne gauche) | `SettingsScreen` `CategoryPaneWidth` | 340 dp | `grid-template-columns: 340px` |
| Gouttière entre panes | `SettingsScreen` `PaneGap` | 24 dp | `gap: 24px` |
| Marge haute des panes | `SettingsScreen` `PaneTop` | 48 dp | `padding: 48px` |
| Catégories listées | `SettingsCategory` (`SOURCES`, `HIDDEN`, `ABOUT`) et `strings.xml` | Apps sources / Applications cachées / À propos | mêmes trois libellés, aucun ajout |
| Ligne catégorie | `SettingsCategoryRow` (`CategoryHeight`, `RowPadding`) | 38 dp / 14 dp | `.nav{height:52px;padding:0 14px}` |
| Ligne de contenu (focus-pill) | `SettingsContent` + `focusPill`/`animatedPillColors` | `SettingsRowHeight` 48 dp, coin 14 dp, `RowVerticalPadding` 6 dp, `RowGap` 12 dp | `.row{min-height:72px;border-radius:14px}` |
| Couleur du focus | `SygixColors.PillFocus` `0xFFF2F2F5` / `OnPill` `0xFF0B0B0F` | #F2F2F5 / #0B0B0F | `.row.focus{background:#f2f2f5;color:#17171b}` |
| Surface vitrée | `SygixColors.GlassTint` `(22,22,28)`, `GlassBorder` blanc 0.16, `GlassShadow` noir 0.32 | — | `rgba(22,22,28,.88)` + bord `rgba(255,255,255,.08)` |
| Actions avec détail | `AboutContent` `AboutActionRow` (titre + détail, `heightIn(min = RowHeight)`) | 48 dp | `.row` titre + `.sub` |
| Navigation D-pad | `SettingsScreen` lignes 118-130 | haut/bas = catégories, droite = contenu, gauche = catégories, Retour | pied de page `↑/↓ · → contenu · ← catégories` |

Vérification couleur des rendus (échantillonnage PNG, `python3` sur grille): les deux images contiennent bien
`#f2f2f5` (pill de focus, 2145 échantillons chacune) et un fond vitré `#14141a`–`#15151c` cohérent avec
`GlassTint` sur fond sombre. Les deux PNG diffèrent (l'état 2 change le bloc d'état), donc ce ne sont pas deux copies.

### Écarts assumés faute de composant in-app

- La **structure exacte de la catégorie P5** (contrôle de remplacement, contrôle de démarrage, contrôle
  d'accessibilité) **n'existe pas** dans `SettingsCategory`; ces lignes sont des **placeholders visuels**, pas des
  exigences. Les libellés, états (inactif/actif/indisponible) et destinations restent à trancher par Simon.
- La **hauteur de ligne catégorie** rendue (52 px) est une mise à l'échelle lisible de `CategoryHeight` (38 dp) sur
  une planche 1920 × 1080; aucune correspondance `dp→px` stricte n'est affirmée.
- Le **halo/ombre** du focus est une simplification statique; `focusPill`/`animatedPillColors` animent le focus en
  Compose, ce que la planche ne reproduit pas.

## Limites de vérification (explicites)

- Les **captures de référence fournies par Simon** (`SygixOs UI tvOS 26` et captures d'écrans) **n'étaient pas
  accessibles** dans ce runtime: fait vérifié par `test -r` sur les chemins du cache, et aucun outil de vision
  n'est disponible pour lire ces fichiers ici. La comparaison côte à côte à ces captures **n'a donc pas pu être
  faite**; l'ancrage ci-dessus porte sur **les composants et tokens du dépôt**, pas sur les captures.
- Aucune comparaison émulateur/TV n'a été faite. Espacements au pixel fin, tailles typographiques, reflets de verre,
  animation de focus et comportement au Retour doivent être reconfirmés sur émulateur/TV.
- `AccessibilityService` n'est **ni activé ni garanti**; aucune maquette ne le présente comme acquis.
- Le **réglage de démarrage à l'allumage** est **hors périmètre** de ces deux vues (non dessiné), conformément à la
  restriction du brief.

## Commandes de rendu et résultats

```
m-shot v1-settings-home-choice.html 1920x1080
m-shot v2-settings-home-return.html  1920x1080
file v1-settings-home-choice.png v2-settings-home-return.png
# → PNG image data, 1920 x 1080, 8-bit/color RGB, non-interlaced (les deux)
python3 /tmp/check_png.py v1-settings-home-choice.png v2-settings-home-return.png
# → #f2f2f5 présent (2145 éch.), fond vitré #14141a/#15151c, images distinctes
```

## Références de code consultées

- `app/src/main/java/fr/sygix/sygixos/ui/settings/SettingsScreen.kt` — `SettingsCategory`, navigation
  catégorie/contenu, focus D-pad, pane 340 dp / gap 24 dp / top 48 dp, `CategoryHeight` 38 dp.
- `app/src/main/java/fr/sygix/sygixos/ui/settings/SettingsContent.kt` — `focusPill`, `animatedPillColors`,
  `RowHeight`, `RowVerticalPadding`, `RowGap`, `RowShape` (coin 14 dp).
- `app/src/main/java/fr/sygix/sygixos/ui/settings/AboutContent.kt` — `AboutActionRow` (titre + détail + focus).
- `app/src/main/java/fr/sygix/sygixos/core/designsystem/Theme.kt` — `SygixColors` (GlassTint, PillFocus, OnPill,
  OnPillSecondary), palette.
- `app/src/main/java/fr/sygix/sygixos/core/designsystem/FocusPill.kt` — `focusPill`.
- `app/src/main/java/fr/sygix/sygixos/core/designsystem/Motion.kt` — `Dimens` (SettingsRowHeight 48 dp,
  SettingsRowCorner 14 dp, SettingsRowPadding 14 dp, thumb 48 × 27 dp), proportions.
- `app/src/main/res/values/strings.xml` — `settings_title`, `settings_category_sources/hidden/about`.

Ces maquettes sont des supports de revue. Elles ne modifient pas la spec fonctionnelle et ne tranchent aucun choix
produit (onboarding, emplacement, hiérarchie, sémantique des contrôles restent ouverts).
