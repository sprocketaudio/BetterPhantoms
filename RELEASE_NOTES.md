# Better Phantoms - Release Notes (v0.1.0)

## Scope

This release implements the first full vertical slice of the Better Phantoms design:

- Phase 1: foundation and config baseline
- Phase 2: dragon-fight phantom wave integration
- Phase 3: post-dragon End spawning model
- Phase 4: End phantom behavior pass
- Phase 5: loot/reward balance (Elytra membrane repair scaling)
- Phase 6: stabilization and release documentation

## Gameplay Changes

### Overworld and Insomnia
- Natural phantom spawning in the Overworld is disabled by default.
- Vanilla insomnia phantom spawning is disabled by default.

### Dragon Fight
- No phantoms are spawned at fight start.
- Destroying crystals triggers delayed phantom waves near the destroyed crystal.
- Active fight-wave phantoms are capped.
- Destroying the final crystal triggers a short frenzy on active fight phantoms.
- Frenzy now speeds up lock-on/re-engage cadence and slightly extends commit persistence for the active wave phantoms.

### Post-Dragon End
- Natural End phantom spawning is gated behind dragon defeat.
- General End spawning is intentionally sparse.
- General End natural spawns are usually solos, with occasional pairs.
- End City regions receive increased spawn pressure and natural phantom packs are shaped toward small hunting groups (`3-5` total).

### Phantom Behavior
- End phantoms patrol higher before engagement.
- Engagement requires exposure and a short lock-on window.
- Phantoms stay committed for a readable combat window with re-engages.
- Solid cover breaks immediate dive paths.
- Prolonged cover use causes loiter/dispersal instead of clumping.

### Rewards and Balance
- Phantom membrane drops remain unchanged.
- Elytra repair via phantom membrane remains supported in an anvil.
- Repair effectiveness is reduced by config default (`0.6`) to prevent trivial sustain loops.

## Config Defaults Added

- `disableOverworldPhantomSpawning = true`
- `disableInsomniaPhantomSpawning = true`
- `enableDragonFightPhantomWaves = true`
- `phantomsPerCrystalWaveMin = 2`
- `phantomsPerCrystalWaveMax = 3`
- `activeFightPhantomCap = 10`
- `frenzyDurationSeconds = 12`
- `enableNaturalEndSpawningAfterDragonDeath = true`
- `generalEndSpawnWeight = 5`
- `endCitySpawnDensityMultiplier = 2.0`
- `highPatrolAltitude = 96`
- `endCityDiveSpeedBonus = 0.15`
- `elytraRepairEffectiveness = 0.6`

## Notes

- This version intentionally prioritizes behavior clarity over content breadth.
- No new phantom loot items, species variants, or status-gimmick attacks were added.
