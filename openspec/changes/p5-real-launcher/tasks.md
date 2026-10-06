# Tasks

## 1. Vérifications de plateforme et décisions préalables

- [ ] 1.1 Sur la TV de test, vérifier l'éligibilité HOME de SygixOs, l'état RoleManager et le dialogue système de demande/refus; consigner les versions/API et résultats non personnels dans le handoff, sans changer le launcher par défaut; terminé quand chaque résultat est documenté et reproductible.
- [ ] 1.2 Avant toute implémentation dépendante, intégrer les décisions de Simon sur les questions ouvertes de `design.md` et la variante de maquette choisie; ne pas re-questionner l'inclusion du rôle HOME ni de l'option de démarrage, déjà confirmée; terminé quand design, specs et tâches reflètent explicitement ses réponses.
- [ ] 1.3 Vérifier la conformité de l'usage du service d'accessibilité pour ce comportement et la politique de distribution cible; terminé quand une source Android/politique vérifiable autorise le parcours ou que le service est retiré des exigences fonctionnelles.

## 2. Rôle HOME et présentation initiale

- [ ] 2.1 Implémenter l'éligibilité au rôle HOME et la demande explicite via le mécanisme Android; vérifier avec des tests que l'acceptation, le refus et le rôle indisponible affichent chacun l'état réel et ne bloquent pas l'accueil. Le rôle HOME fait partie du périmètre confirmé; seuls le parcours de présentation et les détails de contrôle restent ouverts.
- [ ] 2.2 Si une présentation initiale et son déclenchement sont validés, ajouter le parcours retenu distinct du dialogue système; couvrir ses cas validés (dont refus/ignorance et reprise le cas échéant) par tests Compose à `@Config(qualifiers = "w960dp-h540dp-xhdpi")` et testTags stables.
- [ ] 2.3 Après sélection du mockup, réaliser les exigences visuelles de la proposition et vérifier le rendu, focus initial, navigation D-pad, Retour et restauration du focus par tests TV Compose.

## 3. Réglages P5 et recours de compatibilité

- [ ] 3.1 Ajouter la catégorie P5 et l'option de démarrage à l'allumage (périmètre confirmé); fixer la sémantique du contrôle et l'état initial selon les décisions ouvertes, puis vérifier les états système au retour des réglages par tests JUnit et Compose.
- [ ] 3.2 Si la conformité est confirmée, implémenter le service d'accessibilité selon les décisions validées; vérifier activation explicite, désactivation, arrêt/refus et absence d'événement transmis par tests contrôlés et validation sur appareil.
- [ ] 3.3 Implémenter l'option de démarrage à l'allumage TV déjà retenue, uniquement selon les mécanismes Android autorisés et après décision de son état initial; vérifier option off/on, échec OEM et reprise manuelle sur la TV de test. La disponibilité et le mécanisme exact restent conditionnés aux vérifications plateforme, pas l'inclusion de l'option.
- [ ] 3.4 Après sélection du mockup, réaliser l'UI de la catégorie P5 confirmée; vérifier chaînes françaises, testTags, navigation D-pad, focus/Retour/restauration et layout à taille TV par tests Compose.
- [ ] 3.5 Documenter au README le rôle HOME, l'option de démarrage retenue et leurs limites OEM; vérifier les instructions contre l'application livrée sans modifier la feuille de route ni la procédure ADB existante, et sans introduire de changement automatisé de composants système.

## 4. Intégration et livraison

- [ ] 4.1 Exécuter `./gradlew test` et `./gradlew assembleRelease`; critères: tests verts et APK release construit.
- [ ] 4.2 Valider l'ensemble sur la TV réelle avec l'APK release: attribution/refus HOME, retours d'apps système, recours d'accessibilité si retenu, démarrage et reprise; consigner les limites OEM observées sans donnée privée.
- [ ] 4.3 Après fusion, validation TV et mise à jour des specs courantes, archiver `up-next` avant `p5-real-launcher`; vérifier `openspec validate --all --strict` après archivage.
