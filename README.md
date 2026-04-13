# Better Phantoms

Better Phantoms is a Minecraft `1.21.1` NeoForge mod that repositions phantoms as an End-focused threat instead of an Overworld insomnia punishment.

## Project Docs

- [Design Specification](./DESIGN_SPEC.md)
- [Agent Guide (Project-Agnostic NeoForge 1.21.1)](./AGENTS.md)
- [Release Notes](./RELEASE_NOTES.md)
- [Validation Report](./VALIDATION_REPORT.md)

## Development Setup

### Requirements

- Java `21`
- Gradle wrapper included in repo

### Common Commands

Windows (PowerShell):

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat build
.\gradlew.bat runData
```

Linux/macOS:

```bash
./gradlew runClient
./gradlew runServer
./gradlew build
./gradlew runData
```

## Notes

- The mod targets NeoForge for Minecraft `1.21.1`.
- Generated data/resources are produced via the `runData` task.
- Gameplay direction and non-goals are documented in `DESIGN_SPEC.md`.
