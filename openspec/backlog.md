# Suivi : sujets reportés après v0.0.1

Sujets constatés pendant la préparation de v0.0.1 et reportés au prochain lot pour ne pas bloquer la release publique. Aucun n'est spécifié ni tranché : chacun deviendra un change OpenSpec (`openspec/changes/<id>/`) ou sera rattaché à un change existant au moment de son traitement. Les pistes ci-dessous sont des hypothèses de travail, pas des décisions.

## 1. Visuels du héro servis par un fournisseur `content://` lent

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

## 3. Retour pendant le fondu de sortie de l'écran de démarrage

**Constat** : Retour pressé pendant le fondu de sortie de l'écran de démarrage est absorbé par l'accueil : SygixOs reste au premier plan. « Écran de démarrage » prévoit le comportement normal du système (SygixOs quitte le premier plan) « pendant l'écran de démarrage », sans dire si le fondu de sortie en fait partie.

**Résolu côté comportement** : le fondu appartient à l'écran de démarrage ; `startup-fade-back` est implémenté (PR #35) et Sygix a déclaré l'avoir testé sur la TV. Aucun test TV n'a été exécuté par l'agente pendant le lot documentaire. Cette entrée reste présente jusqu'à la synchronisation et à l'archivage de `startup-fade-back`, après ses cinq prédécesseurs (sujet 12).

## 4. Tests Robolectric des réglages instables sous forte charge

**Constat** : sous forte charge de la machine de build, des tests des réglages échouent par dépassement de délai (`HiddenPaneTest`, `SettingsNavigationTest`, `SettingsViewModelTest`). Ils passent de façon stable en CI.

**Piste** : rendre ces tests déterministes, sans dépendance au temps réel ni à la vitesse de la machine (horloge de test, attente d'un état plutôt que d'un délai).

**À trancher** : périmètre (ces trois classes seulement, ou un audit de tous les tests Compose) et critère de stabilité attendu.

## 5. Bouton du héro en avance sur le visuel au changement de programme

**Constat** : au test TV de la rc.7, au début d'un changement de programme, le libellé et la cible du bouton du héro changent d'un coup, 100 à 170 ms avant le nouveau visuel. Pendant environ 0,6 s, l'ancien titre s'affiche avec le verbe du programme suivant (« Ouvrir » ou « Reprendre »), et un appui sur le bouton ouvre alors le programme suivant (2 passages sur 12). « Diaporama héro » prévoit que le bouton change avec le visuel, jamais avant, et ouvre le programme dont le visuel est affiché. Cause probable : dans `HeroOverlay` (`ui/hero/HeroStage.kt`), `HeroOpenButton` est hors du `SequentialFade` qui porte le titre et suit directement le programme cible du changement.

**Piste** : le bouton (libellé et cible de l'ouverture) suit le même fondu et la même source de vérité que le titre, pour qu'un appui ouvre toujours le programme dont le titre est affiché.

**À trancher** : comportement du bouton pendant le fondu des textes (libellé qui suit le fondu du titre ou qui change à un instant précis ; appui pendant le fondu qui ouvre le programme sortant, le programme entrant ou qui est ignoré), et le test qui couvrira ce cas.

## 6. Textes du héro avant le visuel au premier lancement après installation

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
- statut de la session d'installation transmis à une activité plutôt qu'à un récepteur.

**À trancher** : le propriétaire a choisi d'attendre la phase de remplacement du launcher système avant de traiter ce point ; la spec active `self-update` est reformulée en conséquence dans ce lot documentaire. Sa synchronisation et son archivage restent différés (sujet 12).

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
- roadmap du README et contexte `openspec/config.yaml` renumérotés : P3 logo et écran de démarrage (livré), P4 mises à jour intégrées (livré avec la limitation du sujet 9), P5 remplacement du launcher système, P6 Up Next, P7 recherche, P8 BetaSeries ;
- change actif `p2c-upnext` renommé `up-next` ; références actives mises à jour, archives historiques inchangées ; les questions ouvertes restent dans `up-next/proposal.md` ;
- ordre de synchronisation et d'archivage conservé : `ui-tvos-polish` → `startup-splash` → `self-update` → `rc5-tv-fixes` → `rc6-polish` → `startup-fade-back` ; aucun de ces changes n'est encore synchronisé ni archivé dans ce lot, faute de confirmation sur les vérifications TV historiques encore ouvertes ;
- `up-next` et `jellyfin-tvprovider-only` restent actifs, sans implémentation d'Up Next ni synchronisation anticipée ;
- mise à jour de `AGENTS.md` encore à autoriser : l'outil a refusé l'écriture du fichier protégé, aucun contournement effectué.

**À confirmer avant archivage** : les tâches TV non cochées de `ui-tvos-polish` (4.1, 6.2, 9.1–9.3, 11.6, 11.13), `startup-splash` (1.1, 7.1–7.4), `self-update` (1.1, 7.1–7.2, 9.1–9.6), `rc5-tv-fixes` (3.2, 7.3–7.4), `rc6-polish` (1.4). Les PR #18, #26 et #29 consignent des mesures ou des tests partiels, pas une validation exhaustive. La cadence de la mascotte reste un écart accepté (sujet 2) ; la relance après mise à jour reste reportée (sujet 9). Une confirmation peut accepter explicitement un écart ou un report, mais ne doit pas transformer un test absent en test réussi. La validation TV de `startup-fade-back` est déclarée par Sygix seulement pour ce change.

**À trancher** : aucun nouveau choix produit ; confirmer les vérifications ou accepter explicitement les reports ci-dessus pour terminer le lot.
