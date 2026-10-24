## MODIFIED Requirements

### Requirement: Tests UI sans capture d'image
La suite UI SHALL tester l'écran home par assertions sémantiques (Robolectric + Compose), sans dépendance de capture d'image ni de comparaison visuelle.

#### Scenario: indépendance des tests
- **WHEN** la suite UI s'exécute
- **THEN** tous les tests tournent sur JVM via Robolectric, sans émulateur ni génération de PNG, et font partie de la task `test` standard

**Détails.** — Taille des tests TV : Les tests Compose désignés à taille TV dans cette capability SHALL utiliser `@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")`, jamais la taille Robolectric par défaut ; les autres tests Robolectric tournent aussi en français, langue des textes cités par les specs. Cas « configuration des tests Compose » : quand un scénario de cette capability demande un test à taille TV, ses dimensions et sa densité sont celles du qualifier, y compris pour le démarrage, les mises à jour et la configuration automatique.

## ADDED Requirements

### Requirement: Couverture des langues
Les langues de l'interface SHALL être couvertes par la task `test` : tests JUnit de la résolution de la langue (langue de la TV, liste de plusieurs langues, repli sur l'anglais, choix explicite), test de complétude qui échoue si une langue manque d'un texte ou n'a pas les mêmes paramètres que l'anglais, et tests Compose TV de la catégorie « Langue ».

#### Scenario: résolution
- **WHEN** la suite JUnit s'exécute
- **THEN** une langue de la TV non traduite donne l'anglais, une liste qui contient une langue traduite donne la première traduite, et un choix explicite l'emporte sur la TV

#### Scenario: complétude des traductions
- **WHEN** un texte est ajouté à l'anglais sans être traduit dans une autre langue, ou traduit avec d'autres paramètres
- **THEN** le test de complétude échoue en nommant le texte et la langue

#### Scenario: catégorie « Langue »
- **WHEN** les tests Compose TV parcourent la catégorie « Langue »
- **THEN** ils vérifient l'ordre des lignes, la coche, le focus initial sur la ligne retenue, les bords sans boucle, gauche vers la catégorie, l'application d'un choix une seule fois et le texte de la langue de la TV non traduite
