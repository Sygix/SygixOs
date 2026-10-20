# Vérification documentaire de la spec images

Ce rapport conserve les constats de la revue de spec de la PR #82. L'implémentation empilée et ses preuves sont désormais décrites dans `verification.md` ; `tasks.md` suit son avancement actuel.

## Résultat

| Dimension | Statut |
| --- | --- |
| Planning | Proposition, design, trois deltas et tâches complets ; `openspec instructions apply` indique `ready`. |
| Cohérence des deltas | 23 exigences, dont 12 MODIFIED et 11 ADDED ; les cibles existent, tous leurs scénarios d'origine sont conservés et les contrats repliés dans les descriptions/Détails. 159 scénarios décrivent la cible. |
| Qualité de rédaction | Aucune description des deltas ne dépasse 500 caractères. Les règles repliées par la PR #81 (relecture, attente/reprise, synchronisation, qualité des cartes) restent couvertes via leurs exigences parentes modifiées. |
| Projection | Sans écriture ni archivage, application du delta en mémoire : 51 exigences launcher-shell, 23 ui-testing et 16 up-next, sans doublon ni cible MODIFIED absente, avec textes normatifs et scénarios. |
| Implémentation | Au moment de la revue de spec : volontairement non réalisée. L'implémentation et ses validations automatisées sont maintenant suivies dans `verification.md`, avec deux validations finales sur la TV encore ouvertes. |

## Preuves

- Base de la spec : `282ca2918ca07bd1330c158a6cbeb899888e5cc6` (PR #81 après repli des exigences), au-dessus de la PR #80.
- `openspec validate --all --strict` : **Totals: 9 passed, 0 failed (9 items)**.
- Comparaison structurée des deltas avec les specs repliées de la base : aucune exigence MODIFIED inconnue, aucun scénario existant manquant, aucune description de plus de 500 caractères.
- Au stade de la revue documentaire, les specs courantes restent inchangées et seuls les artifacts de `hero-shuffle-image-provider` sont modifiés.
- `git diff --check` : aucune erreur.
- Au stade de la revue documentaire, code, tests, Gradle, CI et archives étaient identiques à la PR #81 ; son résultat Gradle ne prouvait pas les nouveaux comportements. Les preuves d'implémentation ultérieures sont consignées séparément dans `verification.md`.

## Limites et reprise

Les schémas content:// de Disney+ et Crunchyroll, les colonnes d'identité/expiration, les délais effectifs et les tailles d'images sur la TV restent non vérifiés. Sygix a dispensé le relevé préalable avant le code ; les constantes de concurrence et de délai restent des valeurs techniques à confirmer lors de la validation finale sur la TV.

Cette vérification documentaire seule ne permet pas l'archivage. Le point de reprise est dans `handoff.md` ; les tâches 7.2 et 7.3 restent ouvertes jusqu'à la validation réelle sur la TV, puis le change pourra être archivé après merge.
