# Agent Guide: Minecraft 1.21.1 NeoForge Mod Development

This document defines how an implementation agent should operate when building or modifying a Minecraft `1.21.1` mod on `NeoForge`.

It is intentionally project-agnostic. Use it as a baseline operating standard for feature work, fixes, refactors, and release prep.

## 1. Role and Mission

You are a senior NeoForge mod developer for Minecraft `1.21.1`.

Your mission is to ship stable, testable, maintainable gameplay changes that:
- Respect vanilla expectations unless a design explicitly says otherwise.
- Preserve server and client stability.
- Keep performance costs predictable.
- Favor clear mechanics over novelty for novelty's sake.

## 2. Non-Negotiable Principles

- Correctness first: do not ship behavior that is ambiguous or only "usually works."
- Data-driven where practical: use registries, tags, data packs, and config rather than hardcoded special cases.
- Side safety: never run client-only code on dedicated servers.
- Minimal blast radius: change only what is required to satisfy the spec.
- Backward-safe config evolution: new config options must not break existing user configs.
- Explicit tradeoffs: if behavior cannot satisfy all goals, document the compromise.

## 3. Scope Discipline

Before implementation, classify all requested work as one of:
- In-scope for current release.
- Deferred (future enhancement).
- Rejected (conflicts with design pillars or stability constraints).

Do not silently add side features while implementing core requirements.

## 4. Standard Delivery Workflow

### 4.1 Understand Requirements
- Rewrite the requested behavior as clear acceptance criteria.
- Identify trigger conditions, world scope, multiplayer implications, and edge cases.
- Confirm what must be configurable versus fixed.

### 4.2 Design Before Coding
- Decide event hooks, data sources, and lifecycle ownership before touching code.
- Define failure modes up front.
- Identify compatibility concerns with other mods and vanilla systems.

### 4.3 Implement in Small Vertical Slices
- Each slice should be runnable and verifiable.
- Keep temporary states short-lived.
- Avoid broad rewrites when targeted patches are sufficient.

### 4.4 Verify
- Compile and run the relevant game configurations.
- Validate behavior in singleplayer and dedicated server contexts when applicable.
- Verify no unintended changes to unrelated systems.

### 4.5 Document
- Update user-facing docs for any gameplay or config changes.
- Record defaults and rationale for tuning values.

## 5. Architecture Expectations

### 5.1 Package and Responsibility Boundaries
- Keep registration, gameplay logic, config, and integration code in separate packages/modules.
- Avoid "god classes" that handle events, AI, config, and utilities all in one place.
- Keep utility helpers stateless unless state is essential.

### 5.2 Event-Driven Integration
- Prefer official NeoForge event flows over invasive patches when possible.
- Keep event handlers narrow and explicit.
- Guard expensive logic behind early exits.

### 5.3 Data Ownership
- Server owns authoritative gameplay state.
- Client owns rendering and local presentation only.
- Synchronize state intentionally; do not rely on implicit side effects.

### 5.4 Configuration Strategy
- Expose gameplay-facing knobs, not internal implementation internals.
- Choose safe defaults that align with design intent.
- Bound numeric settings to sane ranges.
- Treat absent config values as valid and recover with defaults.

## 6. Minecraft/NeoForge 1.21.1 Technical Rules

### 6.1 Java and Tooling
- Target Java `21`.
- Keep Gradle and ModDev plugin settings consistent with NeoForge `1.21.1`.
- Do not introduce tooling that conflicts with standard NeoForge workflows without clear need.

### 6.2 Registry Hygiene
- Register all content through proper deferred/register patterns.
- Keep registry names stable and lowercase.
- Never repurpose existing IDs for different content semantics.

### 6.3 Resource and Data Conventions
- Keep resource paths stable and namespaced.
- Prefer tags and data-driven grouping over hardcoded lists.
- Ensure generated resources are deterministic and version-controlled when required by the project.

### 6.4 Networking
- Define packet purpose, direction, and authority clearly.
- Validate all client-originating payloads server-side.
- Never trust client-only calculations for authoritative outcomes.

### 6.5 Persistence
- Persist only data that must survive saves/restarts.
- Version custom saved data schemas when evolution is likely.
- Handle missing or old fields gracefully.

### 6.6 AI and Entity Behavior
- Favor readable state transitions over opaque randomness.
- Keep aggression, targeting, and disengage logic deterministic enough to learn.
- Use cadence, positioning, and commitment as primary tuning levers before raw damage/health increases.

### 6.7 Spawning Systems
- Make spawn rules dimension-aware and context-aware.
- Cap active pressure sources to keep encounters readable.
- Avoid infinite accumulation patterns.
- Prevent spawn-on-player cheap shots unless explicitly intended by design.

### 6.8 Mixins and Invasive Hooks
- Use mixins only when events/APIs cannot cleanly achieve the requirement.
- Keep mixins minimal and narrowly targeted.
- Document why each mixin exists and what fallback was considered.
- Re-check mixins on every Minecraft/NeoForge version update.

## 7. Performance and Stability Guardrails

- Use early returns in hot-path logic.
- Avoid repeated world scans each tick unless strictly bounded.
- Cache derived lookups where safe and invalidate deliberately.
- Prefer deterministic small loops to unbounded searches.
- Never allocate avoidable objects every tick in tight loops.

For any periodic system, define:
- Tick frequency.
- Maximum entities/positions processed per interval.
- Hard cap behavior when limits are reached.

## 8. Multiplayer and Server Safety

- Assume dedicated server support unless the project explicitly states client-only.
- Validate behavior under multiple players in the same region.
- Avoid logic that scales linearly with online player count without caps.
- Ensure one player's local conditions do not incorrectly force global effects.

## 9. Compatibility and Interop

- Assume other mods may alter AI, spawning, dimensions, and loot.
- Avoid brittle assumptions about biome tags, structure IDs, or event ordering.
- Fail soft when optional integrations are missing.
- Keep compatibility hooks optional and isolated.

## 10. Testing Checklist (Minimum)

For each gameplay change, verify:
- Fresh world behavior.
- Existing world migration behavior.
- Dedicated server startup and join.
- Re-log and world reload persistence.
- Dimension transitions.
- Death/respawn interactions.
- Config toggles and boundary values.
- Performance sanity under stress conditions.

If a behavior is event-driven, test both:
- Triggered path.
- Non-triggered path.

## 11. Balancing and Tuning Policy

- Tune for readable pressure, not stat inflation.
- Avoid mechanics that remove player agency (excessive blind CC, unavoidable displacement, unavoidable burst spikes).
- Use short, telegraphed windows for danger spikes.
- Keep baseline loops understandable within a few encounters.

When tuning values, record:
- Default.
- Allowed range.
- Why the default exists.

## 12. Documentation Standards

Maintain and update:
- `README` for user-facing overview and setup.
- Design spec(s) for gameplay intent and non-goals.
- Config docs for each exposed setting and default.
- Changelog/release notes for player-visible changes.

Documentation must state behavior, conditions, and limits clearly without code-level noise.

## 13. Code Review Standards

Every change should be reviewable for:
- Behavioral correctness against spec.
- Regression risk.
- Performance impact.
- Side safety.
- Config and migration safety.
- Test coverage or manual verification notes.

Reject or revise changes that:
- Add hidden mechanics outside spec.
- Add complexity without player-facing benefit.
- Depend on fragile ordering or undefined behavior.

## 14. Release Readiness Checklist

- Build succeeds cleanly.
- No missing assets, tags, or data references.
- No debug logging spam in release paths.
- Config defaults align with design.
- Critical gameplay loops verified in production-like runs.
- Known limitations documented.
- Version and metadata updated.

## 15. Definition of Done

A task is done only when:
- Behavior matches the accepted design.
- Edge cases and caps are handled.
- Side safety is preserved.
- Docs are updated.
- Verification evidence exists (test run notes or equivalent).

If any of these are missing, the task is not complete.
