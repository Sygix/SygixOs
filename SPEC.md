# SygixOs — Spec v1.0

Launcher Apple TV-style pour Google TV (TCL, Android 14 / Google TV). Remplace le launcher Google (avec pubs). Priorité absolue : **design fidèle à tvOS** et **zéro bug**.

## 1. Principes de développement
- Kotlin, **Jetpack Compose for TV**
- SOLID, KISS, DRY : petites classes à responsabilité unique, interfaces aux frontières (repos, sources de données), pas de logique dans les UI
- Commentaires : uniquement quand le code ne s'auto-décrit pas. Pas de commentaires "IA", pas de bloat
- State : uniflow (ViewModel -> StateFlow -> Compose). Pas d'état dupliqué
- Erreurs : jamais de crash silencieux ; tous les appels réseau ont fallback + état UI (loading/empty/error)
- Tests : logique métier testée (fusions Up Next, tri, mapping), UI testée à la main sur la TV

## 2. Design (copie tvOS)
- **Home = grille d'apps** : grandes icônes arrondies (superellipse), 6-7 par ligne selon densité
- **Focus** : zoom ~1.1x, ombre douce, fond de la page se change en recommandations de l'app focus (effet Top Shelf), transition floutée
- **Liquid Glass** : surfaces translucides floutées, reflets subtils sur les surfaces focus
- **Rangée Up Next** tout en haut, au-dessus de la grille : posters avec barre de progression
- Animations : 250-400ms, courbes d'easing Apple ; jamais de saut sec
- **Thème sombre uniquement** en v1 (noir pur, posters plein cadre)
- Langue : **français uniquement** (strings FR, architecture i18n prête)
- Police : type SF Pro -> sans-serif proche (Inter) embarquée

## 3. Écrans
1. **Home** : Up Next + grille d'apps + fond contextuel
2. **Settings** : page soignée — Jellyfin (URL + authentification via OAuth/mécanisme applicatif), BetaSeries (OAuth app, v1.x), screensaver, apps épinglées/ordre, à propos
3. **Screensaver** : vidéos aériennes type Aerial Apple (paysages/villes, 4K, en rotation)
4. App normale en v1 ; remplacement système en phase finale (ADB)

## 4. Sources de contenu
- **Jellyfin** (homelab) : intégration complète — Continue watching, Up Next (prochains épisodes), posters, recherche. Config via page Settings (URL + auth applicative)
- **BetaSeries** : hors v1 ; prévu en OAuth applicatif, rangée "à voir" agrégée dans Up Next (v1.x). La page Settings expose déjà le bloc BetaSeries
- **Netflix / Prime / Disney+ / Orange TV / autres** : pas d'API -> lancement par deep link, recommandations au focus seulement si données publiques accessibles, sinon fond neutre
- **Up Next** : fusion déterministe (Jellyfin d'abord, BetaSeries ensuite), tri par date d'activité ; logique isolée dans un use case testé
- **Grille d'apps** : auto-détection de toutes les apps TV installées (Leanback/LAUNCHER category), tri + épinglage manuel persisté

## 5. Interactions
- DPAD natif télécommande TCL uniquement
- Bouton Home de la télécommande -> retourne au launcher (comportement système)
- Long-press sur une app -> menu contextuel (épingler, désépingler, ouvrir, infos)

## 6. Usage
- Mono-utilisateur, pas de profils — toute la maison sur le même launcher

## 7. Architecture
```
app/
  core/        (design system Compose : focus, glass, posters, thème)
  data/        (client Jellyfin, package manager, persistance DataStore)
  domain/      (use cases : fusion Up Next, catalogue apps, ...)
  ui/          (home, settings, screensaver)
```
- Retrofit/OkHttp pour Jellyfin, kotlinx.serialization, Coil pour posters, DataStore pour réglages
- Aucun backend ; tout en local

## 8. Phases
1. **P1 — Squelette + design system** : navigation DPAD, grille home, focus/zoom/glass, apps auto-détectées -> APK installable
2. **P2 — Jellyfin** : settings, intégration Continue watching/Up Next/posters
3. **P3 — Screensaver aerial + polish animations**
4. **P4 — BetaSeries (OAuth) + fusion Up Next**
5. **P5 — Remplacement launcher** :
   - `adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx`
   - `adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith`
   - (réversible : `pm enable`)
   - Pré-requis TV : mode développeur (7x sur "Build"), débogage ADB activé

## 9. Livrables
- Repo GitHub **SygixOs** (privé, sous le compte de Sygix), origin en SSH
- CI légère : build Gradle à chaque push
- APK signé (debug d'abord) livré à chaque phase pour test sur la TV
