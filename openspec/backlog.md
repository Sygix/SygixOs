# Suivi des anomalies et travaux reportés

Dernière revue : **7 novembre 2026**, après v0.6.0. Les sujets terminés, validés sur la TV par le propriétaire ou abandonnés sont retirés ; leur historique reste dans git et dans les changes archivés. Les numéros déjà attribués ne sont pas réutilisés.

## 18. Cohérence du design system avec la maquette

**Constat** : v0.6.0 a unifié le matériau verre (flou givré Haze partagé, bord et reflets conservés), givré le bouton du héro et branché dock, capsule, menus, bulle de suivi, listes déroulantes des réglages et panneau d'accueil sur `GlassSurface`. La comparaison complète avec la maquette Penpot n'a pas encore été faite composant par composant.

**Piste** : comparer à la maquette les cartes, les modales du suivi (`TrackingModal`) et les lignes des réglages : rayons, ombres, typographie et focus ; corriger les écarts dans le design system plutôt que composant par composant. Faire aussi la passe Penpot du menu d'À suivre pour Ajouter et les actions du compte, prévue dans l'exigence « Menu « Ouvrir avec… » » d'up-next.

## 22. Version pour Fire TV

**Constat** : SygixOs n'est publié que pour Google TV et Android TV. Le propriétaire veut aussi une version pour les Fire TV Stick d'Amazon.

**Piste** : vérifier ce que Fire OS permet à un launcher tiers (devenir l'écran d'accueil, bouton Accueil, permissions, Shizuku et ADB intégré), les apps et les intents disponibles (sources d'À suivre, programmes), puis choisir la forme de la publication : variante de build ou APK commun, sur GitHub ou sur l'Amazon Appstore.

## 23. Publication sur les stores

**Constat** : SygixOs n'est distribué que par les releases GitHub. Le propriétaire veut le publier sur les stores : Google Play (que l'on peut aussi installer avec Aurora Store), et d'autres comme F-Droid ou IzzyOnDroid.

**Piste** : relever les règles de chaque store pour un launcher TV (permissions sensibles, ADB intégré, Shizuku, mise à jour intégrée depuis GitHub, signature) et choisir une variante par store si nécessaire. Préparer les fiches (textes dans les cinq langues, captures TV, politique de confidentialité) et automatiser l'envoi dans le workflow de release.
