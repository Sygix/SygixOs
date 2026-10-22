# Suivi des anomalies et travaux reportés

Dernière revue : **22 octobre 2026**, sur `main` à `97d87b1`, après la publication de **v0.3.2**. Aucun change actif, aucune PR ni issue ouverte lors de cette revue. Les numéros des sujets sont conservés pour les références historiques.

Les constats et pistes historiques ci-dessous ne remplacent pas les specs courantes. Lorsqu'un sujet a été spécifié ou livré depuis, son état actualisé le précise. Les sujets sans nouvelle mesure restent ouverts, sans présumer que leur anomalie est encore reproductible.

| Sujet | État à la revue |
| --- | --- |
| 1. Fournisseurs d'images lents | Solution livrée en v0.3.2 ; nouveau signalement de disparition d'images suivi au sujet 14. |
| 2. Cadence de la mascotte | Écart accepté ; nouvelle mesure TV nécessaire. |
| 4. Tests instables | Toujours ouvert ; échec CI actuel précisé ci-dessous. |
| 5–6. Synchronisation du héro | Implémentation remaniée et tests présents ; scénarios historiques précis à revérifier sur TV. |
| 7–8. Fluidité | Mesures historiques à renouveler après les optimisations livrées. |
| 9. Relance après mise à jour | Toujours ouvert ; configuration du launcher livrée, résultat après mise à jour à vérifier. |
| 10–11. Guidage d'installation et QR | Toujours ouverts. |
| 12. Documentation post-v0.0.1 | Terminé ; détail historique conservé ci-dessous. |
| 13. Autorisation constructeur | Reporté, à reprendre seulement sur un appareil concerné. |
| 14. Disparition des images Jellyfin | Cause reproduite, correctif testé et validé sur TV ; intégration en attente. |
| 15. Configuration du launcher | Validations TV restantes à consigner. |
| 16. Validation OpenSpec stricte | Deux avertissements préexistants dans `launcher-shell` font échouer le mode strict. |

## 1. Visuels du héro servis par un fournisseur `content://` lent

**État actualisé** : traité par `archive/2026-10-21-hero-shuffle-image-provider`, révision livrée en v0.3.2 : chargement Coil commun, index Room, une lecture par app et trois au total, priorité aux demandes visibles, délai de 8 s à partir de la lecture réelle (hors attente en file), cache et reprises ciblées. Ces valeurs sont désormais spécifiées, contrairement aux questions historiques ci-dessous. L'archivage rapporte une validation TV confirmée par le propriétaire sans relevé détaillé. La disparition intermittente nouvellement signalée est suivie au sujet 14.

**Constat** : certaines apps publient leurs visuels sous forme d'URI `content://` servies par leur propre fournisseur. Avec le client Jellyfin, la vérification d'une image prend environ 5 à 6 s et son chargement environ 11 s, contre moins de 0,5 s pour les images HTTPS publiées par d'autres apps. Les visuels étant vérifiés l'un après l'autre (« Préchargement et mémoire », scénario « validation au chargement »), une app lente retarde la vérification des visuels de toutes les apps, donc le premier visuel prêt du héro, que l'écran de démarrage attend dans la limite de son plafond (« Écran de démarrage »).

**Pistes** :
- vérifications en parallèle, ou par app, avec un délai borné par visuel ;
- une URI qui dépasse ce délai n'est pas redemandée lors du même chargement, pour qu'une app lente ne pénalise pas les suivantes.

**À trancher** : valeur du délai, nombre de vérifications simultanées (compatible avec la limite de décodeurs de « Préchargement et mémoire »), sort d'une app lente (écartée du héro, ou ses visuels gardés pour plus tard) et sa durée.

## 2. Cadence de la mascotte de l'écran de démarrage

**Constat** : la mascotte est jouée à environ 30 images/s au lieu des 60 prévues (« Animation de la mascotte ») : la première composition de l'accueil, d'environ 250 ms, tombe pendant l'animation. Écart accepté pour v0.0.1.

**Pistes** :
- jouer la mascotte seule pendant la durée minimale de l'écran de démarrage, puis composer l'accueil ;
- composer l'accueil avant l'apparition de la mascotte.

**À trancher** : la piste retenue et son effet sur le délai d'affichage de l'accueil ; l'une comme l'autre modifie l'ordre de composition décrit dans « Écran de démarrage ».

## 4. Tests Robolectric des réglages instables sous forte charge

**État actualisé** : plusieurs corrections de synchronisation et d'isolation ont été livrées depuis ce constat. La dernière CI de `main` n'est toutefois pas verte : [run 39695972888](https://github.com/Sygix/SygixOs/actions/runs/39695972888), 1 échec sur 1 090 tests, `HomeViewModelSourceToggleTest` / `re-enabling a source re-prepares the shelf of the focused app on the recomputed hero` (`TimeoutCancellationException`, puis `UncaughtExceptionsBeforeTest`). Le run précédent passait ; cela ne suffit pas à établir la cause ni à classer ce nouvel échec comme simple instabilité de charge.

**Vérification locale du 22 octobre** : la suite complète après le correctif du sujet 14 passe avec 1 093 tests, aucun échec ni test ignoré, dont ce test de réactivation. Sa cause d'échec en CI reste à déterminer.

**Constat** : sous forte charge de la machine de build, des tests des réglages échouent par dépassement de délai (`HiddenPaneTest`, `SettingsNavigationTest`, `SettingsViewModelTest`). Ils passent de façon stable en CI.

**Piste** : rendre ces tests déterministes, sans dépendance au temps réel ni à la vitesse de la machine (horloge de test, attente d'un état plutôt que d'un délai).

**À trancher** : périmètre (ces trois classes seulement, ou un audit de tous les tests Compose) et critère de stabilité attendu.

## 5. Bouton du héro en avance sur le visuel au changement de programme

**État actualisé** : le bouton fait maintenant partie du `SequentialFade` des textes ; le héro dispose d'un programme affiché distinct de la cible et de tests de synchronisation (`HeroVisualSyncTest`). Le constat rc.7 ne décrit donc plus exactement le code actuel. Le clic pendant le fondu reste à revérifier sur appareil avant clôture de ce sujet.

**Constat** : au test TV de la rc.7, au début d'un changement de programme, le libellé et la cible du bouton du héro changent d'un coup, 100 à 170 ms avant le nouveau visuel. Pendant environ 0,6 s, l'ancien titre s'affiche avec le verbe du programme suivant (« Ouvrir » ou « Reprendre »), et un appui sur le bouton ouvre alors le programme suivant (2 passages sur 12). « Diaporama héro » prévoit que le bouton change avec le visuel, jamais avant, et ouvre le programme dont le visuel est affiché. Cause probable : dans `HeroOverlay` (`ui/hero/HeroStage.kt`), `HeroOpenButton` est hors du `SequentialFade` qui porte le titre et suit directement le programme cible du changement.

**Piste** : le bouton (libellé et cible de l'ouverture) suit le même fondu et la même source de vérité que le titre, pour qu'un appui ouvre toujours le programme dont le titre est affiché.

**À trancher** : comportement du bouton pendant le fondu des textes (libellé qui suit le fondu du titre ou qui change à un instant précis ; appui pendant le fondu qui ouvre le programme sortant, le programme entrant ou qui est ignoré), et le test qui couvrira ce cas.

## 6. Textes du héro avant le visuel au premier lancement après installation

**État actualisé** : l'attente des images, les replis et le retour au premier plan ont été remaniés en v0.3.2 ; tests dans `HeroVisualSyncTest`, `HeroVisualReadyTest` et `HomeRefreshTest`. Le rapport archivé confirme des parcours de premier lancement sur émulateur, mais pas ce scénario temporel précis sur TV réelle. À recontrôler avant clôture.

**Constat** : au test TV de la rc.7, au premier lancement qui suit l'installation, le titre, la ligne d'infos et le bouton du héro s'affichent environ 0,9 s sur un fond vide avant le visuel. Non reproduit aux démarrages suivants. « Diaporama héro » prévoit des textes qui changent avec le visuel, et « Écran de démarrage » attend le premier visuel du héro, dans la limite de 2 s, avant de révéler l'accueil.

**Pistes** :
- établir la séquence de ce premier lancement (demande de permission `READ_TV_LISTINGS`, plafond de 2 s atteint, cache d'images vide, code pas encore compilé) pour savoir quelle étape retarde le visuel ;
- n'afficher les textes du héro qu'avec son premier visuel, ou avec l'état de repli quand aucun visuel n'est prêt.

**À trancher** : ce que montre le héro quand l'accueil est révélé sans visuel prêt (textes seuls, repli seul, ou repli puis textes avec le visuel), et si ce cas mérite un scénario dédié dans « Écran de démarrage ».

## 7. Première image en retard après une pause en navigation lente

**Constat** : au test TV de la rc.7, dans la grille et dans les réglages, en navigation lente (une touche toutes les 0,35 à 0,8 s environ), la première image après chaque pause est en retard de 3 à 8 ms : 5 à 6 % des images sont en retard, contre 0,2 à 1,4 % en navigation continue. C'est sous l'objectif de moins de 10 % (« Préchargement et mémoire »), mais l'écart avec la navigation continue suggère un coût propre à la reprise après une pause.

**Pistes** :
- identifier le travail fait au premier déplacement après une pause (reprise du rendu, travail différé à la pose du focus qui tombe sur l'image suivante, ramasse-miettes) ;
- sortir ce travail de la première image d'un déplacement.

**À trancher** : objectif visé en navigation lente (le même seuil de 10 % ou un seuil plus serré) et protocole de mesure (cadence des touches, durée, APK de release), pour comparer les mesures d'une version à l'autre.

## 8. Image longue au début du fondu de sortie de l'écran de démarrage

**Constat** : au test TV de la rc.7, une image d'environ 65 ms apparaît au début du fondu de sortie de l'écran de démarrage, soit environ quatre images manquées à 60 images/s. Le scénario « fluidité de l'écran de démarrage » (« Écran de démarrage ») vise 60 images par seconde et moins de 10 % d'images en retard pour la mascotte et le fondu de sortie ; une image isolée ne remet pas ce taux en cause.

**Pistes** :
- identifier ce qui démarre au début du fondu : le Ken Burns, la lecture d'une vidéo d'aperçu et la prise du focus par le héro (« Écran de démarrage », « Écran initial du home ») ;
- étaler ces démarrages sur les premières images du fondu, ou en préparer une partie avant son début, sous l'écran de démarrage.

**À trancher** : ce qui peut être décalé sans contredire « Écran de démarrage » (Ken Burns et lecture qui ne commencent qu'au fondu de sortie, focus du héro dès son début), et le lien avec le sujet 2 (cadence de la mascotte), qui touche au même ordre de composition.

## 9. Relance de SygixOs après la mise à jour intégrée bloquée par le constructeur

**Constat** : au test réel de la mise à jour intégrée (v0.0.1-rc.7 vers v0.0.1) sur la TV TCL de référence, l'installation aboutit (profil `.dm` appliqué, données conservées), mais SygixOs ne se relance pas : le gestionnaire d'auto-démarrage du constructeur ignore la diffusion `MY_PACKAGE_REPLACED` vers les récepteurs statiques (journal « Skipping delivery of static … for auto run »), y compris le récepteur de statut d'installation. L'utilisateur reste sur l'écran système « Installation d'applis inconnues ». Le scénario « installation au premier plan » de « Redémarrage après la mise à jour » (`self-update`) n'est donc pas tenu sur cette TV.

**Pistes** :
- relance assurée par le système quand SygixOs est le launcher par défaut (phase de remplacement du launcher système) ;
- statut de la session d'installation transmis à une activité plutôt qu'à un récepteur ;
- accueil d'origine désactivé (SygixOs seul écran d'accueil) : la tâche 1.3 de `p7-default-launcher` vérifie si la relance a lieu quand SygixOs est l'écran d'accueil effectif.

**Décision du propriétaire** : attendre P5 (remplacement du launcher système) avant de traiter ce point. La spec courante `self-update` décrit la demande de relance sous réserve de l'autorisation du système et la réouverture manuelle lorsque le constructeur la bloque ; le change est synchronisé et archivé (sujet 12). La limitation n'est pas résolue par cet archivage.

**Suite P5 (2026-10-11)** : P5 est livré dans v0.1.0-rc.2 et archivé sans relevé de la relance après mise à jour avec le rôle HOME (tâche 1.4 de `p5-real-launcher`, laissée ouverte) ; aucun nouveau mécanisme de relance (décision 31). Le sujet reste ouvert.

**État actualisé** : la configuration par ADB local et le parcours Tsuyu sont livrés et archivés (`2026-10-17-p7-default-launcher`, `2026-10-18-tsuyu-home-setup`). Attendre la livraison du remplacement du launcher n'est plus une prochaine étape ; il reste à mesurer la relance après une mise à jour intégrée avec la configuration effective de la TV.

## 10. Guidage vers l'autorisation « Installer des applis inconnues »

**Constat** : sur la TV TCL de référence, l'écran système ouvert par `MANAGE_UNKNOWN_APP_SOURCES` affiche la liste générale des apps et non la page de SygixOs ; la liste paraît vide une à quatre secondes, puis le focus tombe sur l'interrupteur d'une autre app (une pression réflexe sur OK change l'autorisation de cette autre app).

**Pistes** :
- adapter le texte provisoire d'aide (D7/D9 de `self-update`) avec le chemin réel des réglages de la TV ;
- signaler à l'utilisateur de chercher SygixOs dans la liste.

**À trancher** : texte final et emplacement de l'aide.

## 11. Liserés dans le code QR des notes de version

**Constat** : le QR de « À propos » se lit sans erreur, mais des liserés clairs séparent les modules noirs adjacents (anticrénelage de chaque module dessiné séparément).

**Piste** : dessiner les modules sans anticrénelage ou en un seul chemin fusionné.

**À trancher** : rien, correctif cosmétique.

## 12. Suites de documentation après v0.0.1

**Constat initial** : v0.0.1 est publiée (même commit que v0.0.1-rc.7). Les changes livrés ne sont pas encore archivés ; avant ce lot, la roadmap du README gardait l'ancienne numérotation (P2c, P4, P5).

**État du lot documentaire** :
- roadmap du README et contexte `openspec/config.yaml` renumérotés : P3 logo et écran de démarrage (livré), P4 mises à jour intégrées (livré avec la limitation du sujet 9), P5 remplacement du launcher système, P6 Up Next, P7 recherche, P8 BetaSeries (numérotation d'alors, avant l'insertion de P7, écran d'accueil par défaut) ;
- change actif `p2c-upnext` renommé `up-next` ; références actives mises à jour, archives historiques inchangées ; les questions ouvertes restent dans `up-next/proposal.md` ;
- synchronisation vérifiée puis archivage, dans cet ordre : `ui-tvos-polish` → `startup-splash` → `self-update` → `rc5-tv-fixes` → `rc6-polish` → `startup-fade-back` ; archives sous `openspec/changes/archive/2026-10-05-<nom>/` (date du système d'archivage) ; les quatre specs courantes concernées sont `launcher-shell`, `settings`, `ui-testing` et la nouvelle capability `self-update` ;
- `up-next` et `jellyfin-tvprovider-only` restent actifs, sans implémentation d'Up Next ni synchronisation anticipée ;
- mise à jour de `AGENTS.md` faite par l'orchestratrice avec l'autorisation explicite du propriétaire pour ce change (roadmap P3–P8, renvoi Up Next, conservation de la numérotation historique des archives) ;
- sujet 3 retiré après synchronisation et archivage de `startup-fade-back` ; les autres sujets restent ouverts.

**Acceptation historique** : le 6 octobre 2026, Sygix a confirmé « J’ai aussi validé ces anciens changes sur la TV » pour le polish, le démarrage, les mises à jour intégrées et rc5/rc6 ; sa confirmation antérieure couvre `startup-fade-back`. Il s'agit d'une acceptation rapportée par le propriétaire, pas de tests exécutés par l'agente, ni de nouvelles mesures. Les tâches historiques exigeant un relevé ou une consignation détaillée restent non cochées dans les archives, avec cette réserve explicite. La cadence de la mascotte reste un écart accepté (sujet 2) ; la relance après mise à jour reste reportée (sujet 9).

**État actualisé : terminé.** Les étapes ci-dessus décrivent le lot historique. Up Next et `jellyfin-tvprovider-only` ont depuis été livrés et archivés le 11 octobre ; la configuration du launcher et son parcours Tsuyu le sont également. Le README reflète v0.3.2 et sa roadmap n'utilise plus de numéros de phase. Aucun change actif ne reste dans `openspec/changes/` à la date de revue.

## 13. Autorisation de démarrage du constructeur réinitialisée au démarrage

**Constat** (vérifications de `p7-default-launcher`, 2026-10-12/13, TV de référence TCL, Android 14) : le gestionnaire d'auto-démarrage du constructeur lit une politique (`AppBootPolicy`, fournisseur de contenu du constructeur) dont les listes `autoStartWhiteList` et `assocStartWhiteList` peuvent être modifiées par le shell (`content update`, deux-points échappés) et persistent après un redémarrage ; y inscrire SygixOs n'a pourtant aucun effet : sans l'opération `AUTO_START`, la liaison du service d'accessibilité et la fin du démarrage restent refusées. Sur cette TV, `AUTO_START` accordée par le shell persiste après un redémarrage complet. Des retours publics signalent d'autres firmwares TCL où `AUTO_START` revient à `ignore` à chaque démarrage.

**Pistes** :
- sur une TV qui réinitialise `AUTO_START`, vérifier si l'inscription dans les listes blanches de la politique devient utile, avec sauvegarde de la valeur d'origine et relecture ;
- sinon, réaccorder `AUTO_START` à chaque configuration relancée, ce que P7 permet déjà.

**Décision du propriétaire** : retiré de P7 le 2026-10-13 ; à reprendre seulement si une TV réinitialise `AUTO_START`.

## 14. Images Jellyfin affichées puis retirées

**Signalement du 22 octobre 2026** : des images apparaissent dans « À suivre », puis disparaissent alors que le serveur répond. Les autres zones utilisant le même chargement doivent être vérifiées. La TV de référence exécute v0.3.2.

**Cause confirmée** : les épisodes Jellyfin observés ne publient pas de `series_id`. Le même épisode est représenté par plusieurs identités de publication (carte « À suivre », programme Watch Next du héro, programme Preview), qui étaient comptées comme autant de films ou séries distincts en l'absence de `sharedIdentity`. Au troisième comptage des mêmes octets, l'image était rejetée comme générique et un événement `QUALITY` retirait les visuels déjà affichés. La lecture directe du fournisseur a pourtant confirmé des images de 1 280 × 720 à 3 840 × 2 160 pour les quatre cartes concernées.

**Correctif vérifié** : seules les identités de film ou de série renseignées contribuent au seuil de trois ; une identité de publication inconnue ne prouve pas une série distincte. Les rejets déjà persistés sans cette preuve sont relus immédiatement si leurs dimensions passent une politique, puis validés normalement avant réaffichage. Les vrais génériques associés à trois identités connues restent rejetés. Aucun effacement global du cache ni des données utilisateur.

**Preuves automatisées** : `ArtworkRepositoryTest.publicationsWithoutASeriesIdDoNotProveDistinctSeries` et `previouslyMisclassifiedPublicationsRecoverWithoutWaitingForGenericRetry` échouent avant le correctif et passent après. `UpNextArtworkRenderTest.jellyfinStyleEpisodeStaysVisibleWhenHeroAndPreviewUseTheSameArtworkWithoutSeriesId` couvre le rendu réel de la carte pendant les résolutions des deux autres publications. Suite complète : **1 093 tests réussis** ; `assembleRelease` réussi.

**Validation sur TV Google TV sous Android 14** : avant remplacement, deux des quatre cartes Jellyfin avaient perdu leur image. Après installation de la release corrigée par-dessus v0.3.2, les quatre affichent leur image sans réinitialisation. Vérification répétée plusieurs heures après, après navigation vers le Top Shelf Jellyfin, retour des réglages système et redémarrage du processus de SygixOs : les quatre images restent visibles. Le Top Shelf affiche et fait tourner des visuels Jellyfin ; une des images auparavant absentes s'affiche également dans le héro. Aucun journal `ProgramArtwork` ou `AndroidRuntime` n'a été relevé pour le nouveau processus pendant le dernier parcours. Ces essais ne constituent pas une mesure de fluidité ni une validation de démarrage à froid de la TV.

**État** : correctif validé localement et sur appareil, intégration en attente.

## 15. Validations TV du parcours de configuration du launcher

**État** : `p7-default-launcher` et `tsuyu-home-setup` sont livrés et archivés ; `AGENTS.md` conserve explicitement les validations sur la TV de référence en attente. Les résultats sur émulateur ne remplacent pas ces essais.

**À consigner** : configuration via l'ADB local et via Tsuyu, retour à l'accueil par la télécommande, persistance après redémarrage et réparation sur symptôme. Le test de mise à jour intégrée et de relance reste suivi séparément au sujet 9.

## 16. Avertissements bloquant la validation OpenSpec stricte

**Constat du 22 octobre 2026** : `openspec validate --all --strict` valide sept specs sur huit. `launcher-shell` échoue à cause de deux textes d'exigence de plus de 500 caractères (`requirements[9]` et `requirements[14]`). Ces textes sont déjà présents dans `main` à `97d87b1` et ne sont pas modifiés par le correctif du sujet 14.

**Suite** : reprendre leur rédaction via le workflow OpenSpec, en plaçant les détails dans des scénarios ou en séparant les comportements sans modifier le contrat.
