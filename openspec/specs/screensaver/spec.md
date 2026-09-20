# Capability : screensaver

## Purpose
Screensaver du launcher : diffuser des vidéos aériennes 4K type Aerial Apple quand la TV est inactive, avec cache local et configuration depuis Settings.

## Requirements
### Requirement: Lecture
Le screensaver SHALL diffuser des vidéos aériennes 4K (paysages/villes) en rotation, avec enchaînement fluide et cache local.

#### Scenario: déclenchement
- **WHEN** la TV est inactive au-delà du délai configuré
- **THEN** le screensaver démarre et boucle des vidéos aériennes

### Requirement: Configuration
Le screensaver SHALL être activable/désactivable et son délai réglable depuis Settings.

#### Scenario: réglage
- **WHEN** l'utilisateur modifie le réglage screensaver
- **THEN** la préférence est persistée et appliquée à la prochaine inactivité
