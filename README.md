# Better Phantoms

Better Phantoms is a NeoForge mod for Minecraft `1.21.1` that moves phantoms out of the Overworld and turns them into an End-focused threat.

## Features

- Disables natural Overworld phantom spawning.
- Disables insomnia-triggered phantom spawning.
- Adds crystal-triggered phantom waves during the Ender Dragon fight.
- Applies a short frenzy to active fight phantoms when the final crystal is destroyed.
- Enables natural End phantom spawning only after the dragon has been defeated.
- Increases phantom pressure near End Cities.
- Keeps Phantom Membrane drops unchanged.
- Reduces Elytra repair efficiency from Phantom Membrane (configurable).

## Requirements

- Minecraft `1.21.1`
- NeoForge `21.1.x`
- Java `21`

## Configuration

Common config file:

`run/config/betterphantoms-common.toml`

Main options include:

- Overworld spawn disable toggle
- Insomnia spawn disable toggle
- Dragon fight wave toggle, size range, cap, and frenzy duration
- Post-dragon End spawning toggle and weights
- Patrol altitude and End City dive pressure tuning
- Elytra repair effectiveness

## Development

PowerShell:

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
.\gradlew.bat build
```
