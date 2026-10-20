# Reprise de l'implémentation

## Point de départ

La première implémentation a été livrée en v0.3.1 et a régressé sur la TV de référence (causes dans `design.md`, « Régression de la première implémentation »). La spec a été révisée pour v0.3.2. L'implémentation part de `main` après merge de cette révision, en une seule PR, et suit la section 8 de `tasks.md`. Le nettoyage des avertissements de compilation fait l'objet d'une PR séparée.

## Décisions du propriétaire (révision v0.3.2)

- Coil reste le moteur : intercepteur pour la candidate et les clés, fetcher unique pour les octets HTTP(S) et `content://`, cache disque de Coil.
- Une lecture à la fois par app, 3 au total, pour toutes les sources. Seule la requête a un délai (8 s depuis son début effectif) ; aucun délai d'attente en file ni délai empilé.
- Index Room des programmes, candidates, images et associations, sans limite de nombre ; migrations écrites et testées, repli par recréation de l'index.
- Mises à jour par événements du TV Provider et par échéance d'image : seule l'image concernée change ; aucune invalidation globale ni effacement au retour au premier plan. Empreinte de programme sans identifiant de ligne ni progression.
- Top Shelf à 640 px paysage. Héro limité aux programmes dont un visuel paysage acceptable ou une vidéo est prêt ; programme passé tant que son image n'est pas prête ; repli nature seulement après échec de la requête.
- Images génériques traitées comme absentes : refus par toutes les politiques, mêmes octets pour au moins 3 contenus distincts d'une app, ressource d'une autre app ; nouvelle tentative après une heure.
- Clip nature continu pendant les programmes en repli consécutifs, clip suivant à sa fin ; après un programme avec image, le clip repart du début.
- Cache vidéo jusqu'à 300 Mo selon l'espace libre, lecture en flux sans erreur si le cache est impossible. Clips 2K conservés.
- Shuffle du héro une fois par processus, stable pendant la session, nouveaux programmes insérés au hasard après le courant, programme republié à sa place.

## Contrats repliés dans leurs exigences parentes

La PR #81 a replié « Données facultatives du héro » et « Relecture des programmes du héro » dans « Contenu héro TV Provider », « Synchronisation du héro » et « Visuel du héro en attente » dans « Diaporama héro », « Reprise des visuels du héro » dans « Préchargement et mémoire », et « Qualité des cartes » dans « Carte Up Next ». Ce change modifie ces exigences parentes et leurs paragraphes **Détails.**.

## Vérification attendue

Suivre la section 8 de `tasks.md`, puis `openspec validate --all --strict`, `./gradlew test`, `assembleRelease`, l'émulateur, et la validation sur la TV (8.12) avant l'archivage.
