# Suivi des anomalies et travaux reportés

Dernière revue : **24 octobre 2026**, après v0.4.0 (retours du propriétaire sur la TV, pour une prochaine version corrective). Les sujets terminés, validés sur la TV par le propriétaire ou abandonnés sont retirés ; leur historique reste dans git et dans les changes archivés. Les numéros déjà attribués ne sont pas réutilisés.

## 17. Doublons entre BetaSeries et une app dans À suivre

**Constat** : une série suivie sur BetaSeries et en cours dans Jellyfin apparaît deux fois dans À suivre, une carte par source, au lieu d'une seule carte fusionnée.

**Piste** : la série publiée par l'app sous son titre localisé n'est pas identifiée. Sur la TV, `shows/search` avec le titre français a renvoyé une liste vide alors que la série existe sous son titre original. Utiliser d'abord les identifiants externes publiés quand il y en a, puis chercher aussi par titre original ou par alias ; un test doit couvrir un titre localisé.

## 18. Cohérence du design system avec la maquette

**Constat** : le bouton du héro n'a pas de flou d'arrière-plan (verre), contrairement à la maquette.

**Piste** : vérifier tous les composants (héro, dock, capsule, cartes, menus, modales et notifications du suivi, réglages) par rapport à la maquette et entre eux : matériau verre, rayons, ombres, typographie, focus ; corriger les écarts dans le design system plutôt que composant par composant.

## 19. Ordre d'À suivre avec le suivi BetaSeries

**Constat** : les films du compte sont ajoutés seulement après les séries, et les contenus publiés par les apps perdent leur place : seules 2 des 4 séries en cours dans Jellyfin restent visibles dans la rangée.

**Piste** : un seul tri pour séries et films ; les contenus publiés par les apps (en cours, épisode suivant) passent avant les contenus apportés seulement par le compte, dans la limite de 20 cartes ; mettre à jour l'exigence d'ordre d'up-next et ajouter un test.

