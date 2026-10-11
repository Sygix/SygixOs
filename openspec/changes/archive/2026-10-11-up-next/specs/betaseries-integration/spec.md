# Delta betaseries-integration

Décision du propriétaire (2026-10-10) : BetaSeries n'alimente plus la rangée Up Next. La rangée est spécifiée par la capability up-next et se nourrit uniquement du TV Provider. BetaSeries reviendra en P8, dans un change dédié, comme étape d'enrichissement des items (identifiants externes) qui active le niveau 5 de dédoublonnage de up-next.

## REMOVED Requirements

### Requirement: Agrégation Up Next
**Reason :** La rangée Up Next agrège les `WatchNextPrograms` publiés par toutes les apps dans le TV Provider (capability up-next) ; BetaSeries ne lui fournit plus d'items et il n'y a plus d'ordre « Jellyfin puis BetaSeries ».
**Migration :** Aucune donnée ni aucun code à migrer : aucune intégration BetaSeries n'existe dans le launcher. Le rôle de BetaSeries (enrichissement des items par des identifiants externes, niveau 5 de dédoublonnage de up-next) sera spécifié par le change P8 dédié, qui réécrira aussi le `Purpose` de la capability.
