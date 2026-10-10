# Proposal

## Why

P5 fait de SygixOs un launcher candidat et lui fait demander le rôle HOME, mais ce rôle peut ne pas suffire sur Google TV : sur l'émulateur Android TV (Android 14) utilisé pour valider P5, le rôle attribué à SygixOs ne redirige pas la touche Home, le launcher fourni avec l'image garde la priorité et le système le rouvre juste après l'acceptation du rôle ; seule sa désactivation a fait de SygixOs l'écran d'accueil effectif. La TV de référence reste à vérifier (tâches 1.x). Cette désactivation n'est aujourd'hui possible que par la procédure ADB manuelle du README. [Shizuku](https://shizuku.rikka.app/) permet à une application d'utiliser les droits du shell ADB (ou de root avec Sui) une fois démarré par l'utilisateur : P7 l'utilise pour proposer cette procédure depuis SygixOs, de façon réversible, après une action explicite de l'utilisateur.

Terme : l'**accueil d'origine** désigne l'écran d'accueil fourni avec la TV (launcher Google TV ou Android TV) et le composant système qui le réactive.

## What Changes

- SygixOs détecte Shizuku (et Sui sur un appareil rooté) et en expose l'état : en cours de lecture, absent, version non prise en charge, arrêté, autorisation non accordée, autorisation refusée définitivement, prêt.
- Une nouvelle ligne de la catégorie « Écran d'accueil », après les lignes P5 (placement exact et libellés : Question ouverte 3 ; rendu : maquettes à faire), permet, Shizuku prêt et après une confirmation explicite, de désactiver l'accueil d'origine comme la procédure ADB du README ; SygixOs devient alors l'écran d'accueil effectif de l'appareil.
- La même ligne rétablit l'accueil d'origine (composants remis dans leur état par défaut), y compris après une désactivation partielle ou faite par ADB, Shizuku prêt et après confirmation. Traitement du rôle HOME au rétablissement : Question ouverte 1.
- L'état de l'accueil d'origine (actif, partiellement désactivé, désactivé, indisponible) est relu auprès du système à chaque retour au premier plan, sans Shizuku : il reste exact après un redémarrage, quand Shizuku est arrêté ou si le système réactive l'accueil d'origine.
- Garde-fous : rien n'est désactivé avant la fin de la configuration initiale de l'appareil ni si SygixOs n'est pas un écran d'accueil activé ; une désactivation partielle, ou qui ne fait pas de SygixOs l'écran d'accueil effectif, est annulée ; opérations bornées dans le temps ; aucune commande arbitraire ; aucun processus privilégié qui survit à l'opération ou à SygixOs.
- La ligne P7 prolonge la navigation de « Réglages du launcher système » (P5) : la dernière ligne de la catégorie n'est plus « Retour à l'accueil » mais la ligne P7.
- Vérifications sur la TV de référence : démarrage de Shizuku, composants de l'accueil d'origine, effets de leur désactivation (touche Home, démarrage, sortie de veille, boutons de la télécommande, rôle HOME, relance après mise à jour, sujet 9 du suivi) et du rétablissement. Moment de ces essais : Question ouverte 6.
- Feuille de route : Shizuku devient P7 ; la recherche passe en P8 et BetaSeries en P9 (`AGENTS.md`, README, contexte `openspec/config.yaml` et références des changes actifs ; les archives et l'historique du suivi gardent leur numérotation).

## Capabilities

### New Capabilities

- Aucune.

### Modified Capabilities

- `launcher-shell` : état de Shizuku, autorisation, état, désactivation et rétablissement de l'accueil d'origine, garde-fous, opérations privilégiées limitées (ADDED).
- `settings` : ligne de l'accueil d'origine, confirmation, ligne pendant une opération, états affichés (ADDED) ; « Réglages du launcher système » (MODIFIED : la navigation et l'ordre de la catégorie se prolongent jusqu'à la ligne P7).
- `ui-testing` : couverture des parcours Shizuku et sélecteurs de l'accueil d'origine (ADDED) ; « Sélecteurs du launcher système » (MODIFIED : ordre et navigation de la catégorie avec la ligne P7).

### Dépendances

- `p5-real-launcher` (change ouvert) : porte « Réglages du launcher système » et « Sélecteurs du launcher système », modifiées par ce change ; à archiver avant celui-ci (`openspec validate` signale que l'archivage refuserait ces deltas MODIFIED tant que P5 n'est pas archivé).
- `up-next` (change ouvert) : crée la catégorie « Écran d'accueil » ; archivé avant `p5-real-launcher`.

## Impact

Nouvelle dépendance : Shizuku API (`dev.rikka.shizuku:api` et `dev.rikka.shizuku:provider`, licence MIT, compatible AGPL-3.0 ; dernière version publiée en 2023, Question ouverte 10). Manifeste (fournisseur `ShizukuProvider`, déclarations de visibilité des composants de l'accueil d'origine, de Shizuku et de l'intent `HOME`), règles R8 du service privilégié, service exécuté avec les droits du shell via Shizuku (opérations fixes), contrôleur et état dans le domaine, catégorie « Écran d'accueil » des réglages, chaînes françaises, tests JUnit et Compose, README (procédure, risques, rétablissement, procédure ADB conservée). Aucune dépendance réseau ni serveur. Documents de feuille de route renumérotés.

## Non-goals

- Accorder la permission d'affichage superposé, activer le service d'accessibilité ou attribuer le rôle HOME par Shizuku : hors P7, sans phase prévue, sauf décision contraire (Question ouverte 1) ; les exigences de P5 qui l'interdisent restent inchangées.
- Exécuter des commandes arbitraires, proposer un terminal ou d'autres opérations privilégiées : sans phase prévue.
- Désactiver d'autres applications système (publicités, services Google) : sans phase prévue.
- Réappliquer automatiquement la désactivation quand le système réactive l'accueil d'origine : sans phase prévue (Question ouverte 8 pour un éventuel rappel).
- Garantir le comportement sur tous les appareils : P7 décrit le comportement sur les appareils vérifiés et affiche « Indisponible » ailleurs.
- Recherche : P8. BetaSeries : P9.

## Questions ouvertes

Liste unique dans `design.md`, section « Questions ouvertes », reprise telle quelle dans la description de la PR.
