# Suivi des anomalies et travaux reportés

Dernière revue : **7 novembre 2026**, après v0.6.0. Les sujets terminés, validés sur la TV par le propriétaire ou abandonnés sont retirés ; leur historique reste dans git et dans les changes archivés. Les numéros déjà attribués ne sont pas réutilisés.

## 18. Cohérence du design system avec la maquette

**Constat** : v0.6.0 a unifié le matériau verre (flou givré Haze partagé, bord et reflets conservés), givré le bouton du héro et branché dock, capsule, menus, bulle de suivi, listes déroulantes des réglages et panneau d'accueil sur `GlassSurface`. La comparaison complète avec la maquette Penpot n'a pas encore été faite composant par composant.

**Piste** : comparer à la maquette les cartes, les modales du suivi (`TrackingModal`) et les lignes des réglages : rayons, ombres, typographie et focus ; corriger les écarts dans le design system plutôt que composant par composant. Faire aussi la passe Penpot du menu d'À suivre pour Ajouter et les actions du compte, prévue dans l'exigence « Menu « Ouvrir avec… » » d'up-next.
