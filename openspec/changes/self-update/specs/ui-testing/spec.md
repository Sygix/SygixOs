# Delta ui-testing

## ADDED Requirements

### Requirement: Couverture des mises à jour
La mise à jour depuis les releases (`self-update`) SHALL être couverte sans accès au réseau réel ni au vrai installateur : la logique (conversion des tags, choix de la version proposée, échéance quotidienne, politique HTTPS, enchaînement des vérifications du fichier, correspondance des statuts d'installation) par des tests JUnit avec des sources factices ; le transport HTTP par des tests contre un serveur local lancé par le test ; les lignes d'« À propos » et la pastille par des tests Compose à la taille d'une TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`), avec les key events physiques du D-pad et des testTags stables : « update-check », « update-install », « update-prereleases », « update-prereleases-switch », « update-badge-gear », « update-badge-about ».

#### Scenario: préversion contre version finale
- **WHEN** les tests de conversion et de choix s'exécutent
- **THEN** `v0.0.1-rc.4` donne 164, `v0.0.1` donne 199, `v1.2.3-beta.5` donne 100200335 ; une version finale bat les préversions du même numéro ; une préversion n'est proposée que si la préférence l'autorise ; une version inférieure ou égale à la version installée n'est jamais proposée ; les tags non conformes sont ignorés

#### Scenario: réponses de l'API
- **WHEN** le transport factice renvoie une liste vide, une release sans asset, un asset sans empreinte, un JSON illisible, une erreur 404, une erreur 403 avec la limite atteinte et son heure de réessai, une absence de réseau, un délai dépassé
- **THEN** chaque cas donne l'état attendu par « Source des versions » et « Versions comparées », sans exception non gérée

#### Scenario: empreinte invalide
- **WHEN** le contenu téléchargé ne correspond pas à l'empreinte publiée
- **THEN** l'installateur factice n'est jamais appelé, le fichier temporaire n'existe plus et l'état indique un fichier corrompu

#### Scenario: certificat différent
- **WHEN** l'inspection factice de l'APK renvoie un certificat différent de celui de l'app installée
- **THEN** l'installateur factice n'est jamais appelé, le fichier temporaire n'existe plus et l'état indique une signature différente

#### Scenario: transport réel
- **WHEN** le transport HTTP réel interroge un serveur local du test
- **THEN** il renvoie le corps et les en-têtes utiles, respecte les délais (un serveur qui ne répond pas provoque une erreur de délai), interrompt un corps plus long que la taille attendue, et n'envoie ni en-tête d'autorisation ni cookie

#### Scenario: politique HTTPS
- **WHEN** une URL d'asset ou une URL finale après redirection est en HTTP
- **THEN** elle est refusée et aucun téléchargement n'est transmis à l'installation

#### Scenario: navigation dans À propos
- **WHEN** le test ouvre « À propos » et presse droite, puis bas et haut
- **THEN** « update-check » prend le focus en premier ; bas parcourt les lignes de mise à jour puis les licences dans l'ordre affiché ; haut depuis « update-check » et bas depuis la dernière licence ne bougent pas le focus ; gauche rend le focus à la catégorie « À propos »

#### Scenario: ligne Mettre à jour
- **WHEN** l'état passe de « à jour » à « version disponible » puis revient à « à jour » alors que « update-install » a le focus
- **THEN** « update-install » apparaît sans voler le focus, puis à sa disparition le focus passe à « update-check »

#### Scenario: préversions
- **WHEN** le test bascule « update-prereleases » avec des versions connues contenant une préversion plus récente
- **THEN** l'état de « update-prereleases-switch », la version annoncée et la présence de « update-badge-gear » et « update-badge-about » changent aussitôt, sans appel au transport factice

#### Scenario: pastille sans effet sur le focus
- **WHEN** une version est proposée et le test parcourt héro, engrenage et retour au héro
- **THEN** « update-badge-gear » est affichée, n'est jamais focusée, et la position et la taille de la capsule et de l'engrenage sont identiques à celles d'un test sans version proposée
