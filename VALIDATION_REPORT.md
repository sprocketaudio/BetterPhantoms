# Better Phantoms - Validation Report (Phase 6)

Date: `2026-04-13`

## Automated Checks Run

Commands executed:

```powershell
.\gradlew.bat compileJava --no-build-cache
.\gradlew.bat build -x test --no-build-cache
.\gradlew.bat runData --no-build-cache
.\gradlew.bat runServer --no-build-cache
```

## Results

- `compileJava`: passed.
- `build -x test`: passed.
- `runData`: passed and completed datagen initialization for the mod.
- `runServer`: dedicated server startup confirmed in `run/logs/latest.log`:
  - Mod list includes `betterphantoms`.
  - Server reached `"Done (4.017s)! For help, type \"help\""` on `2026-04-13`.

## Runtime Notes

- A benign first-run warning was observed for missing `server.properties`; server then generated defaults and continued startup.
- Intermittent Gradle `processResources` stale-output cleanup failures were observed in this environment on some invocations. Re-running after `.\gradlew.bat --stop` and using `--no-build-cache` resolved the lock issue.

## Manual Gameplay Verification Still Recommended

The following should be verified in actual gameplay sessions before tagging a public release:

- Dragon fight pacing:
  - No start-wave phantoms.
  - Crystal-triggered wave timing and cap behavior.
  - Final-crystal frenzy readability and duration.
- End post-dragon spawning:
  - No natural End phantoms before dragon defeat.
  - Sparse general End pressure after defeat.
  - Stronger pressure near End Cities.
- Behavior loop:
  - Exposure/lock-on readability.
  - Commit and re-engage cadence.
  - Cover break and anti-camping dispersal behavior.
- Elytra repair:
  - Membrane repair is reduced and predictable.
  - Config changes apply without requiring a world reset.
- Dedicated server multiplayer:
  - Multiple players in End/dragon fight scenario.
  - No desync between target selection and behavior windows.

## Release Readiness Assessment

- Build integrity: ready.
- Data/resource integrity: ready.
- Dedicated server boot path: ready.
- Full gameplay QA (singleplayer + multiplayer): pending manual pass.
