# Change : toolchain-glass

Montée de version de la chaîne de build (Kotlin, Compose, AGP, Gradle, compileSdk) pour passer à Haze 2 et son vrai verre : réfraction, spéculaire, aberration chromatique.

## Why
Le Liquid Glass actuel (Haze 1.3.1) se limite à un flou d'arrière-plan teinté : pas de réfraction des bords, pas de reflet spéculaire réel. Haze 2 (`haze-glass`, `Modifier.hazeGlass` + `GlassStyle`) apporte le matériau complet façon visionOS/tvOS 26, mais exige Kotlin 2.4 et Compose 1.12, donc une montée de la chaîne complète. Sygix a validé la montée de version, la cible restant Android 14+.

## What Changes
- Kotlin 2.4.20, Compose BOM 2026.09.00 (Compose 1.12.x, Material3 1.4), AGP et Gradle alignés, `compileSdk`/`targetSdk` relevés au niveau exigé par Compose 1.12 ; `minSdk` inchangé (34, Android 14+)
- `kotlinOptions` remplacé par `compilerOptions` (DSL courant), dépendances de test (Robolectric, androidx.test) alignées
- `GlassSurface` passe à `Modifier.hazeGlass` + `GlassStyle` : réfraction des bords, reflet spéculaire, teinte et flou du style `regular` ajustés pour le dock, le bouton du héro et le menu contextuel ; repli translucide inchangé si l'effet est indisponible
- Repli automatique : si l'appareil ne supporte pas l'effet, la surface reste translucide sans flou (comportement actuel)

## Impact
- specs affectées : launcher-shell (MODIFIED « Thème » : le Liquid Glass devient un vrai matériau verre)
- build : Gradle wrapper, AGP, Kotlin, Compose BOM, compileSdk ; conteneur de build à compléter (plateforme et build-tools de la nouvelle cible)
- risque : dépendance `haze-glass` marquée expérimentale par son auteur ; en cas de régression visuelle ou de performance sur la TCL, retour à `hazeBlur` sans toucher au reste de la montée de version
