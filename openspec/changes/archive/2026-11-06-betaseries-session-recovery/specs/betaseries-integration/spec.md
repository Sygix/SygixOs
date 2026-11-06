## MODIFIED Requirements

### Requirement: Fin de session BetaSeries
La déconnexion SHALL supprimer immédiatement le jeton et les données de compte locales et arrêter leurs travaux. La révocation distante SHALL viser uniquement le jeton de cette session et rester au mieux possible si le réseau manque ; le changement de compte SHALL ne jamais conserver une mutation différée. Une panne temporaire du stockage ou du service SHALL conserver la session et permettre sa restauration sans nouvelle association.

#### Scenario: déconnexion en ligne
- **WHEN** l'utilisateur se déconnecte
- **THEN** le jeton courant est révoqué si le service répond et le compte n'alimente plus l'accueil ; les préférences de lecteurs et masquages de la TV restent conservés

#### Scenario: déconnexion hors ligne
- **WHEN** la révocation distante ne peut pas aboutir
- **THEN** l'effacement local et l'arrêt du suivi sont effectifs, l'échec distant est distingué et aucune reconnexion automatique du compte n'a lieu

#### Scenario: stockage indisponible
- **WHEN** le stockage protégé ne peut pas être lu ou déchiffré
- **THEN** le fichier est conservé et relu au prochain retour au premier plan ou sur Réessayer, sauf erreur définitive de déchiffrement ou clé Keystore confirmée absente ou définitivement invalidée ; une exception de récupération de clé dont la cause reste temporaire ou inconnue ne permet pas sa suppression ; aucune donnée secrète n'est exposée

#### Scenario: remplacement de session
- **WHEN** un nouveau jeton est reçu pendant une association
- **THEN** il est validé avant remplacement atomique de la session ; un jeton refusé à la validation est révoqué au mieux sans afficher « Révocation distante non confirmée » ; fermeture du panneau et annulation de l'appelant ne laissent ni session bloquée ni jeton orphelin ; stockage et révocation terminent leur séquence malgré cette annulation

#### Scenario: identité indisponible au démarrage
- **WHEN** un jeton a été restauré mais que le chargement de l'identité échoue sans refus explicite du jeton
- **THEN** la session reste conservée ; Réessayer et la reprise au premier plan selon « Chargement du compte BetaSeries » réutilisent ce jeton, sans association OAuth ni révocation ; une réussite charge ensuite les contenus du compte

#### Scenario: absence de session confirmée
- **WHEN** aucune session n'est enregistrée ou que son déchiffrement est définitivement impossible
- **THEN** la restauration se termine sans rester en chargement et une nouvelle association est proposée ; une panne temporaire ne suffit jamais à conclure à cette absence

### Requirement: Bornes réseau et délais
Les appels BetaSeries SHALL avoir des bornes fixes et annuler leurs ressources au dépassement. Les lectures idempotentes SHALL autoriser au plus deux reprises ; aucune mutation ne SHALL être rejouée automatiquement. Une expiration du compte SHALL exiger un refus explicite du jeton par le service, pas seulement un statut HTTP générique.

#### Scenario: budgets
- **WHEN** un appel réseau s'exécute
- **THEN** les bornes sont connexion 5 s, lecture 10 s, tentative 15 s, lecture avec reprises 60 s, corps API 2 Mio, compte et mutation avec relecture 90 s, identification locale 30 s ; 502/503/504, panne et non-JSON transitoire autorisent des reprises croissantes et Retry-After dans le budget restant, un délai serveur supérieur à ce reste échouant aussitôt ; un JSON invalide est une erreur de protocole

#### Scenario: refus HTTP sans preuve d'expiration
- **WHEN** une lecture reçoit 401/403 sans code BetaSeries établissant le refus du jeton, notamment avec un corps non JSON
- **THEN** une indisponibilité réessayable est exposée sans effacer le jeton, sans bloquer les prochains rafraîchissements et sans retirer les contenus précédemment valides ; un refus explicite de configuration conserve son renouvellement distinct

#### Scenario: jeton explicitement refusé
- **WHEN** le service retourne un code BetaSeries établissant le refus du jeton
- **THEN** la reconnexion est proposée et l'apport du compte retiré selon « Chargement du compte BetaSeries » ; aucun réessai automatique de mutation ni boucle de renouvellement de configuration n'est lancé

#### Scenario: révocation au mieux
- **WHEN** la déconnexion révoque le jeton
- **THEN** une seule tentative est faite dans l'échéance d'appel ; son échec affiche « Révocation distante non confirmée », sans échec du compte ni réessai ni blocage de l'effacement local
