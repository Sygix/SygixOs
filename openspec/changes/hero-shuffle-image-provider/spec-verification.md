# Vérification documentaire de la spec images

## Résultat

| Dimension | Statut |
| --- | --- |
| Planning | Proposition, design, trois deltas et tâches complets ; `openspec instructions apply` indique `ready`. |
| Cohérence des deltas | 23 exigences, dont 12 MODIFIED et 11 ADDED ; les cibles existent, tous leurs scénarios d'origine sont conservés et les contrats repliés dans les descriptions/Détails. 159 scénarios décrivent la cible. |
| Qualité de rédaction | Aucune description des deltas ne dépasse 500 caractères. Les règles repliées par la PR #81 (relecture, attente/reprise, synchronisation, qualité des cartes) restent couvertes via leurs exigences parentes modifiées. |
| Projection | Sans écriture ni archivage, application du delta en mémoire : 51 exigences launcher-shell, 23 ui-testing et 16 up-next, sans doublon ni cible MODIFIED absente, avec textes normatifs et scénarios. |
| Implémentation | Non réalisée, volontairement : 0/23 tâches d'implémentation. Aucune correction d'image ni validation sur appareil n'est déclarée acquise. |

## Preuves

- Base de la spec : `282ca2918ca07bd1330c158a6cbeb899888e5cc6` (PR #81 après repli des exigences), au-dessus de la PR #80.
- `openspec validate --all --strict` : **Totals: 9 passed, 0 failed (9 items)**.
- Comparaison structurée des deltas avec les specs repliées de la base : aucune exigence MODIFIED inconnue, aucun scénario existant manquant, aucune description de plus de 500 caractères.
- Comparaison des specs courantes à la base : inchangées ; les modifications de cette PR restent dans les artifacts de `hero-shuffle-image-provider`.
- `git diff --check` : aucune erreur.
- Code, tests, Gradle, CI et archives identiques à la PR #81, dont `./gradlew test --no-daemon` a donné **BUILD SUCCESSFUL** ; ce résultat ne teste pas les comportements futurs décrits ici.

## Limites et reprise

Les schémas content:// de Disney+ et Crunchyroll, les colonnes d'identité/expiration, les délais effectifs et les tailles d'images sur la TV restent à vérifier par la première tâche avant le code. Les constantes de concurrence et de délai sont des valeurs techniques initiales, pas des mesures déjà validées sur appareil.

Cette vérification permet la revue de la spec, pas l'archivage d'une implémentation. Le point de reprise et les décisions confirmées sont dans `handoff.md` ; les 23 tâches restent décochées pour la prochaine session.
