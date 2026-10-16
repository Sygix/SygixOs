# Design

## Context

Voir `proposal.md`. `LauncherHome` utilise `Column.verticalScroll`, `HomeGrid` découpe le catalogue puis compose chaque rangée, et `HomeViewModel` précharge tout le catalogue. `UpNextRow` est déjà lazy horizontalement. `GridScroll`, `GridSections`, `HomePage` et `FocusFallback` portent des règles réutilisables ; leurs géométries et identités ne doivent pas être remplacées par des positions accidentelles de composition.

Sources vérifiées : `Catalog` fournit les listes dock/grille de `TvApp` (package, libellé, activité) ; le package est l'identité métier. `HeroState` fournit les programmes et ensembles de visuels contrôlés ; aucune colonne TV nouvelle n'est requise. Les tailles et animations viennent de `Dimens` et `Motion`. Les états de permission, erreur et vide des sources restent chez leurs adaptateurs existants.

## Goals / Non-Goals

**Goals:** coût de composition lié au viewport ; artworks bornés ; restauration d'une cible non composée ; mêmes géométries et interactions.

**Non-Goals:** second scroll vertical, changement de tri, de style, de nombre de colonnes, de matière ou de contrat P7.

## Decisions

### D1 — une seule liste verticale et un en-tête résident borné

Employer une `LazyColumn` comme autorité de scroll de la page, avec un espace d'en-tête et des items de sections/rangées. Garder le héro/dock/capsule dans un en-tête résident de coût constant, dont la translation est dérivée du même scroll et bornée à sa hauteur : cela conserve les tags géométriques et évite de recréer les tuiles du dock pendant les bascules de verre. Ce résident n'existe que pendant la destination accueil ; ses effets hors écran restent coupés selon les corrections de visibilité.

Alternative écartée : imbriquer une grille lazy verticale dans le `verticalScroll` existant, qui donnerait deux autorités et des contraintes de hauteur non bornées. Un simple item héro recyclable imposerait de changer les garanties existantes de continuité et de non-recréation du dock ; le résident constant n'empêche pas de virtualiser les nombreuses rangées.

### D2 — géométrie et animation pilotées par un coordinateur unique

Décrire les sections et rangées par métadonnées stables et calculer leurs ancres dans le domaine à partir du viewport, des dimensions et de la hauteur animée du shelf. Traduire une position cible en index/offset lazy, sans mesurer toutes les rangées. Une transition interrompue repart de la position courante, avec `Motion` ; aucune téléportation interne d'une animation lazy longue ne peut remplacer la continuité exigée.

Les clés identifient contenu et section, pas seulement un index. Les items de rangée gardent des clés d'apps stables ; un changement de ligne dû à une insertion ne transfère ni artwork ni focus à une autre app. Le shelf demeure associé au bloc logique focusé et ne crée pas de place quand il est fermé.

### D3 — révéler puis focaliser

Conserver les clés et ancres logiques hors des éléments recyclables. Résoudre la cible/repli par les règles existantes, révéler sa rangée via le coordinateur, attendre son attachement par observation d'état/layout, puis demander le focus et vérifier le résultat. Annuler une demande dépassée par une touche plus récente ou une disparition de cible. Le D-pad, le mode déplacement et le retour d'app empruntent ce même chemin, sans registre de `FocusRequester` détachés conservé pour tout le catalogue.

### D4 — préparation d'artworks limitée et différée

Remplacer le préchargement exhaustif par le dock et les rangées visibles avec une rangée voisine de prélecture de chaque côté. Ce budget technique est interne, non persisté. La composition lazy reste gérée par Compose ; sa borne est vérifiée séparément du budget d'images.

Ne pas faire de `load` immédiat dans chaque nouvelle tuile composée pendant le mouvement : le préparateur attend le repos selon la politique de focus existante, reste sur `BackgroundDecoding`, réutilise le cache et annule le travail non commencé d'une fenêtre dépassée. Un décodage synchrone déjà parti peut finir ; son résultat n'est appliqué qu'à une cible encore correspondante. La frontière de préparation doit accepter un double déterministe ; un petit contrat dédié est préférable à un faux PackageManager ou à un framework d'injection.

Les tuiles gardent les replis existants en attente/échec. La vérification des affiches Top Shelf reste indépendante, avec son délai existant ; aucune nouvelle validation n'est déclenchée par le simple préchargement de bannières.

### D5 — preuves de gain distinctes des invariants

Tests JVM : catalogues synthétiques de 20, 200 et 2 000 apps, mêmes viewport et états, comptes d'attachement/composition et appels de préparation. Vérifier à la fois les cibles visibles, l'absence initiale des éloignées et leur accessibilité ultérieure ; ne pas rendre les tests verts en cachant toutes les apps.

Profilage : hôte de fixture dans le source set `perf`, sans changement du launcher de production ni données personnelles, avec sources/artworks contrôlés et aucune action réelle d'ouverture d'app. Vérifier son absence du manifest release. Rejouer le même scénario avant/après sous les mêmes ressources, deux échauffements puis au moins cinq passages ; garder les échantillons CPU, allocations, mémoire et traces. Les nombres de recompositions ne remplacent pas une mesure de frame ; aucun seuil de temps réel n'entre en CI. La TV reste la validation finale par le propriétaire après release.

## Risks / Trade-offs

- Ancre modifiée par le shelf → couvrir ouverture/fermeture près du haut et en profondeur, titres et limites de viewport.
- Clipping du zoom au bord d'un item lazy → conserver les marges et vérifier les rectangles agrandis, sans changer le design system.
- Lecture des offsets animés en composition → isoler les lectures en layout/draw quand possible et conserver les garde-fous de repos.
- Prélecture qui masque encore un travail O(catalogue) → compter séparément création de lignes et décodages ; les métadonnées du catalogue peuvent rester complètes en mémoire.

## Migration Plan

Une PR d'implémentation après validation, basée sur les corrections listées dans la proposal. Conserver les tests de navigation et adapter seulement leurs attentes d'accès aux éléments hors fenêtre. Retour arrière par réversion de cette famille, sans migration DataStore.

Archive : après implémentation, merge et validation TV. Aucun chevauchement de blocs modifiés avec `p7-default-launcher` ou `hero-shuffle-image-provider` ; leur archivage reste indépendant. `settings-destination` conserve les ancres décrites ici sans dépendre du type de scroll.

## Open Questions

Aucune décision produit ouverte. Les mesures préciseront les limites observées de précomposition interne de Compose ; elles ne changent pas le budget explicite du préparateur ni les invariants de navigation.
