# Tasks

## Acceptation historique rapportée par le propriétaire
Le 6 octobre 2026, Sygix confirme : « J’ai aussi validé ces anciens changes sur la TV ». Cette acceptation couvre le rendu et le comportement livrés, sans constituer un relevé chiffré ni un test exécuté par l'agente. Les cases historiques non cochées restent ouvertes lorsqu'elles demandent une mesure, une consignation ou un scénario individuel non détaillé dans cette confirmation générale ; l'archivage demandé conserve ces réserves documentaires. La cadence de mascotte reste l'écart accepté du backlog, sujet 2 ; la relance automatique bloquée par le constructeur reste reportée à P5 (sujet 9), avec réouverture manuelle.

## 1. Comportement
- [x] 1.1 Retour non intercepté par l'accueil pendant le fondu de sortie (D1) ; test Compose du scénario « Retour pendant le fondu de sortie » (« Couverture de l'écran de démarrage ») à la taille TV (`@Config(qualifiers = "w960dp-h540dp-xhdpi")`) : le test échoue sur le code actuel et passe avec le changement ; le test sans Retour laisse l'accueil au premier plan
- [x] 1.2 Scénario « Retour sans fondu » couvert (D2) : Retour pendant l'écran de démarrage sans animation quitte le premier plan, Retour après le remplacement suit « Navigation 3 paliers » ; tests existants de la couverture sans animation adaptés

## 2. Validation
- [x] 2.1 `./gradlew test` vert ; `openspec validate --all --strict` vert
- [x] 2.2 Sur la TV réelle, en `assembleRelease` : Retour pendant le fondu de sortie quitte SygixOs au premier plan ; Retour avant le fondu inchangé ; Retour après le fondu ramène au héro comme avant ; sans animation, la frontière du remplacement instantané vérifiée — validation déclarée par Sygix lors du lot documentaire post-release (« pour le retour c'est déjà traité et j'ai testé tu peux archiver et sync la spec ») ; aucun test TV exécuté par l'agente pendant ce lot, aucune validation générale des autres changes n'en est déduite
- [x] 2.3 Synchronisation des deltas vérifiée et change archivé dans le lot documentaire, après ses prédécesseurs ; acceptation TV rapportée par le propriétaire, avec les réserves ci-dessus. Le sujet 3 est retiré du backlog après le dernier archivage.
