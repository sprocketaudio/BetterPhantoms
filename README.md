# Better Phantoms

Better Phantoms is a NeoForge mod that moves phantoms out of the Overworld and turns them into an End-focused threat.

## Features

- Disables natural Overworld phantom spawning.
- Disables insomnia-triggered phantom spawning.
- Adds crystal-triggered phantom waves during the Ender Dragon fight.
- Applies a short frenzy to active fight phantoms when the final crystal is destroyed.
- Enables natural End phantom spawning only after the dragon has been defeated.
- Increases phantom pressure near End Cities.
- Keeps Phantom Membrane drops unchanged.
- Reduces Elytra repair efficiency from Phantom Membrane (configurable).

## Supported Versions

This repository keeps both supported development tracks in one place:

- `mc-1.21.1/` targets Minecraft `1.21.1`, NeoForge `21.1.x`, and Java `21`.
- `mc-26.1.2/` targets Minecraft `26.1.2`, NeoForge `26.1.2.x`, and Java `25`.

## Requirements

- A Java runtime matching the version folder you are working in.
- NeoForge for the matching Minecraft target.

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

## Repository Layout

- `mc-1.21.1/` contains the `1.21.1` implementation.
- `mc-26.1.2/` contains the `26.1.2` implementation.
- Root-level docs and CI are shared across both version folders.
