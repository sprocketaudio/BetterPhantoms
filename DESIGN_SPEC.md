# Better Phantoms - Design Specification

## Target
- Minecraft `1.21.1`
- Loader `NeoForge`
- Document type: Design specification only (no implementation details or code)

## 1. Mod Summary

Better Phantoms reworks phantoms from an Overworld insomnia punishment mob into an End-native aerial predator.

Core goals:
- Remove phantom spawning from the Overworld entirely.
- Break the link between insomnia and phantom attacks.
- Make phantoms part of the End's identity.
- Use phantoms to add pressure to the Ender Dragon fight in a controlled, escalating way.
- Keep the design readable, fair, and learnable.
- Avoid annoying mechanics such as blindness spam, cheap knockback gimmicks, or excessive tankiness.

This mod should feel like a vanilla-plus redesign of phantoms, not a total replacement of what they are.

## 2. Design Pillars

### 2.1 - Phantoms Belong to the End
Phantoms should feel like a natural predator of the End rather than an arbitrary punishment for not sleeping.

### 2.2 - Fair but Threatening
The player should be able to understand phantom behavior and learn how to respond. The mod should create pressure, not irritation.

### 2.3 - Clear Fight Escalation
During the dragon fight, phantoms should not be present immediately. They should be introduced as the player dismantles the dragon's defenses.

### 2.4 - Counterplay Matters
Players should have a valid defensive response, similar in spirit to the Enderman shelter strategy. Cover should interrupt phantom engagement, but should not trivialize them completely.

### 2.5 - No Unnecessary Complexity
Do not add multiple phantom variants, complicated status systems, or bloated mechanics in version 1. The strength of the mod is clarity and focus.

## 3. High-Level Feature Set

The mod includes three major gameplay changes.

### A) Overworld Rework
- Phantoms no longer naturally spawn in the Overworld.
- Insomnia no longer causes phantom spawning.
- Insomnia no longer functions as the vanilla phantom trigger.

### B) Dragon Fight Integration
- During the Ender Dragon fight, phantoms begin appearing only as the player destroys end crystals.
- Each destroyed crystal creates a small phantom response wave.
- When the final crystal is destroyed, all currently active fight phantoms enter a temporary frenzy state.

### C) Post-Dragon End Ecosystem
- After the dragon is defeated, phantoms naturally exist in the End.
- They spawn sparsely throughout general End areas.
- They spawn in more dangerous groups near End Cities.
- They patrol at high altitude and then commit to attacks once they choose a target.

## 4. Overworld Rules

### 4.1 - Phantom Removal from Overworld
Natural phantom spawning must be removed from Overworld biomes.

### 4.2 - Insomnia Change
The player's insomnia or sleep-deprivation state should no longer have any connection to phantom spawning.

### 4.3 - Scope for Insomnia
For version 1, keep this simple:
- Either leave insomnia with no gameplay consequence.
- Or neutralize its effect entirely if practical.

Do not add replacement penalties in version 1:
- No food drain.
- No health drain.
- No hallucinations.
- No debuffs.

Reason:
The mod identity is "phantoms are now an End threat," not "sleep system overhaul."

## 5. Dragon Fight Design

### 5.1 - Purpose
Phantoms should act as the End's response to the player dismantling the dragon's defenses.

Pacing target:
- Early fight: familiar dragon and crystal gameplay.
- Mid-fight: increasing aerial pressure as crystals are destroyed.
- Transition moment: final crystal destroyed.
- Late fight: dragon plus empowered remaining phantoms.

### 5.2 - Spawn Trigger Model
Phantoms do not spawn at the start of the dragon fight.

Instead:
- Each crystal destroyed triggers a small phantom wave.
- The wave should be associated with the tower/crystal that was destroyed.
- The last crystal destroyed triggers frenzy on active fight phantoms.

### 5.3 - Wave Spawn Location
Phantoms should spawn above or near the destroyed crystal tower area, not directly on top of the player.

Desired feel:
- The player destroys a crystal.
- A short beat occurs.
- Phantoms appear above the battlefield.
- They circle briefly, then engage.

This should feel intentional and thematic rather than cheap or gamey.

### 5.4 - Wave Size
Initial target values:
- Per crystal destroyed: `2 to 3` phantoms.
- Never allow fight waves to scale infinitely.
- Maintain a strict cap on active fight phantoms.

Recommended active cap:
- `8 to 10` active fight phantoms maximum.

### 5.5 - Crystal Progression Scaling
Wave behavior should escalate slightly as more crystals are removed.

Design intent:
- Early crystal destroys feel like warnings.
- Mid crystal destroys create steady aerial pressure.
- Final crystal destroy creates a clear phase shift.

Scaling should be subtle. Do not dramatically increase health, damage, or wave size from one crystal to the next.

### 5.6 - Final Crystal Frenzy
When the final crystal is destroyed:
- All currently active fight phantoms enter Frenzy.
- Frenzy lasts for a short duration.
- Frenzy does not summon a huge new swarm by itself.
- Frenzy empowers the phantoms already in play.

Frenzy effects:
- Increased movement speed.
- Faster attack re-engagement.
- Faster dive commitment.
- Potentially slightly more aggressive target persistence.

Recommended duration:
- `10 to 15` seconds.

Recommended presentation:
- Distinct audio cue and/or subtle visual cue.
- The player should understand a phase change has happened.

Frenzy should feel dangerous, not chaotic nonsense. Do not combine frenzy with major extra spawn spam.

### 5.7 - What Not to Do in the Dragon Fight
Do not:
- Add blindness.
- Add screen-obscuring effects.
- Add forced edge knockback gimmicks.
- Make phantoms extremely tanky.
- Make them spawn directly on the player.
- Give them highly random behavior that feels unreadable.

The fight should be harder because of aerial pressure and timing, not unfair crowd-control.

## 6. Post-Dragon End Spawning

### 6.1 - Unlock Condition
After the dragon has been defeated, phantoms should exist as normal natural spawns in the End.

This should feel like:
"The dragon is gone, but the End is still dangerous."

### 6.2 - Spawn Distribution
Phantoms should not spawn equally everywhere.

Desired distribution:
- Sparse ambient spawning across general End terrain.
- Higher threat density near End Cities.

Regional identity:
- General End: tense travel.
- End Cities: real aerial danger.

### 6.3 - General End Spawning
Outside special areas, phantoms should:
- Spawn infrequently.
- Usually appear alone or in pairs.
- Create tension, not constant harassment.

Target feel:
Players notice them in the sky and know they may become a problem, but End travel is not nonstop combat.

### 6.4 - End City Spawning
Near End Cities, phantoms should:
- Spawn in small groups.
- Be more active and dangerous.
- Have slightly stronger dive pressure than general End phantoms.

Recommended group size near End Cities:
- `3 to 5`.

Recommended behavior boost near End Cities:
- Faster dive speed.
- Faster lock-on.
- Slightly shorter re-engagement delay.

Do not make End City phantoms a different mob type. Keep the same phantom with contextual behavior tuning.

### 6.5 - Spawn Pacing
The End should feel hostile but not exhausting.

Design target:
- Careful travelers encounter phantoms often enough to remember they exist.
- End City looters feel actively hunted.
- Players are not forced into constant skywatching everywhere.

## 7. Phantom Behavior Design

### 7.1 - Identity
End phantoms are aerial predators. They should feel observant, deliberate, and committed once they attack.

They should not feel like random buzzing pests.

### 7.2 - Idle Altitude
Phantoms should idle noticeably higher than vanilla behavior.

Target concept:
- High enough that players can see them watching from above.
- Not so high that they become irrelevant or visually meaningless.

Atmosphere outcome:
The player sees them before they strike.

### 7.3 - Detection Philosophy
Aggro should not be a simple "close enough = instant attack" rule.

A phantom should prefer to engage when:
- The player is exposed to open sky.
- The player is within meaningful horizontal range.
- The player remains in the open long enough to be considered a valid target.

This should feel like hunters choosing a moment, not missiles triggered by proximity.

### 7.4 - Lock-On Window
After deciding to attack, a phantom should have a short lock-on period.

Target feel:
- Small recognition window for the player.
- Clear transition from patrol to aggression.
- Readable attack startup.

The window should be short enough to preserve threat, but long enough to remain understandable.

### 7.5 - Commit Behavior
This is a core requirement.

Once a phantom chooses a player:
- It descends from idle altitude into a lower combat band.
- It remains engaged for a period of time.
- It does not instantly abandon the attack after one weak pass.

Desired combat loop:
- Idle high.
- Detect target.
- Lock on.
- Descend.
- Make repeated attack passes.
- Pull back to a mid altitude.
- Re-engage.
- Eventually disengage if the player breaks line/exposure.

This keeps high-altitude phantoms from becoming ignorable.

### 7.6 - Retreat and Re-Engage
After a dive pass, phantoms should not always return all the way to idle height.

Instead:
- Pull back to a mid-combat altitude.
- Reposition.
- Attack again after a short delay.

This keeps combat active and readable.

### 7.7 - Cover Counterplay
Players should be able to break phantom aggression by moving under solid cover.

This is intentional and desirable, similar to sheltering against Endermen.

Cover should not be a perfect reset.

Desired behavior when the player takes cover:
- Phantom loses immediate attack path.
- Phantom remains nearby for a short period.
- Phantom loiters above or near the area.
- If the player immediately steps back into open sky, threat resumes quickly.

This keeps cover useful without making phantoms trivial.

### 7.8 - Anti-Camping Behavior
If the player remains under cover for too long, phantoms should not stack endlessly overhead.

Desired behavior:
- Some drift wider.
- Some resume nearby patrol space.
- Local threat persists without one giant clump over one block.

This avoids awkward behavior and helps system stability.

## 8. Combat Tuning

### 8.1 - Health
Do not significantly increase phantom health.

Reason:
Flying enemies become frustrating quickly when too durable. Threat should come from movement, pressure, and timing.

Recommended direction:
- Keep health close to vanilla.
- Small increase only if absolutely necessary.
- Default recommendation: unchanged health.

### 8.2 - Damage
Do not dramatically increase base damage.

Recommended direction:
- Vanilla or near-vanilla damage.
- Danger should mostly come from repeated attack windows and aerial pressure.

### 8.3 - Speed
Speed is the best tuning lever.

Use speed to distinguish contexts:
- Normal End phantoms: controlled predator speed.
- End City phantoms: slightly more aggressive.
- Frenzy phantoms: clearly faster for a short phase.

### 8.4 - Attack Cadence
Attack cadence matters more than raw damage.

Design target:
- Reasonable time between passes.
- Faster in danger zones.
- Fastest during frenzy.
- Never instant spam with no breathing room.

## 9. End City Pressure Model

### 9.1 - Why End Cities Matter
End Cities are a major loot destination. Phantoms should reinforce that danger theme.

### 9.2 - Behavior Near End Cities
Near End Cities:
- More phantoms at once.
- Faster commitment to attack.
- Faster dives.
- More active overhead circling.

### 9.3 - Intended Feel
Approaching an End City should feel like entering an aerial hunting ground.

Player interpretation:
"This place is worth looting, but it is more dangerous here."

### 9.4 - What Not to Do
Do not:
- Add special phantom variants for End Cities in version 1.
- Add debuff spam.
- Add random teleporting or phase gimmicks.
- Add extreme damage spikes.

End City danger should come from numbers and behavior quality, not surprise mechanics.

## 10. Player Experience Targets

### 10.1 - First Dragon Fight Experience
The player should notice:
- No phantoms at fight start.
- Aerial pressure rises as crystals fall.
- Final crystal is a memorable phase shift.
- The fight feels more dramatic but still understandable.

### 10.2 - First Post-Dragon End Exploration
The player should notice:
- The End now has a native flying predator.
- General travel has tension.
- End Cities are significantly more dangerous.

### 10.3 - Learnability
Within a few encounters, a player should understand:
- Open sky makes you vulnerable.
- Cover breaks attacks.
- End Cities mean more phantoms.
- Final crystal in dragon fight triggers a danger spike.

If players cannot learn these rules quickly, the design has failed.

## 11. Configuration Design

Version 1 should include a simple config with gameplay-facing options only.

Recommended config options:
- Disable Overworld phantom spawning (default `true`).
- Disable insomnia phantom spawning (default `true`).
- Enable dragon fight phantom waves (default `true`).
- Phantoms per crystal wave (default `2 to 3`).
- Active fight phantom cap (default `8 to 10`).
- Frenzy duration (default `10 to 15` seconds).
- Enable natural End spawning after dragon death (default `true`).
- General End spawn weight (configurable).
- End City spawn bonus weight or density multiplier (configurable).
- High patrol altitude tuning (configurable).
- End City dive speed bonus (configurable).
- Elytra repair effectiveness from Phantom Membrane (configurable; reduced vs vanilla baseline).

Avoid exposing dozens of tiny behavior settings in version 1. The mod should feel curated, not like a balance spreadsheet.

## 12. Non-Goals for Version 1

Do not include in the first release:
- Multiple phantom species or subclasses.
- Blindness or screen-distortion attacks.
- Heavy insomnia overhaul systems.
- Knock-off-edge specialist AI.
- Complex loot progression.
- New crafting materials unless there is a very clear use case.
- Large-scale visual overhauls or custom models unless already easy to support.
- Big dragon fight redesign beyond phantom integration.

Reason:
The clean version of this concept is stronger than an overloaded one.

## 13. Future Expansion Ideas

Future ideas (not part of version 1):
- Optional insomnia rework that does something non-phantom-related.
- Special phantom loot tied to the End.
- Phantom membrane alternatives or expanded recipe usage.
- Optional post-dragon world difficulty escalation for the End.
- Distinct ambient sounds and improved audio telegraphing.
- Optional phantom nesting or roosting behaviors around End structures.

These should be explored only after the core design is proven fun.

## 14. Success Criteria

The mod succeeds if:
- Players stop seeing phantoms as an Overworld annoyance.
- Dragon fights feel more dramatic without feeling unfair.
- The End feels more alive and threatening.
- Players can understand and predict phantom behavior.
- End Cities become meaningfully more tense.
- The mod remains lightweight, focused, and easy to explain.

## 15. One-Sentence Mod Identity

Better Phantoms turns phantoms from an insomnia punishment into a true End predator, using crystal-triggered dragon-fight pressure and post-dragon aerial hunting to make the End feel more dangerous and alive.

## 16. Loot and Rewards

### 16.1 - Design Intent
Phantoms must provide a practical reward so engaging with them feels worthwhile, especially in the End where they are a persistent threat.

This reward must:
- Fit within vanilla expectations.
- Avoid adding new items or systems in version 1.
- Not interfere with other mods that rely on Phantom Membrane.
- Avoid creating infinite or trivial Elytra sustain.

The goal is to make phantoms useful but not abusable.

### 16.2 - Phantom Membrane (Unchanged Drop)
Phantoms continue to drop Phantom Membrane as in vanilla.

Changes:
- No change to drop rate.
- No change to drop quantity.
- No additional loot is introduced in version 1.

Reason:
This maintains compatibility with modded environments where Phantom Membrane may already be used in recipes or other systems.

### 16.3 - Elytra Repair Adjustment
Phantom Membrane can be used to repair Elytra in an anvil.

However, repair efficiency is reduced compared to standard repair materials.

Rules:
- Each Phantom Membrane restores reduced durability compared to vanilla expectations.
- Recommended baseline: about `50% to 70%` of standard repair effectiveness.
- The exact value should be configurable (see Section 11).

Design intent:
- Keep Phantom Membrane relevant and useful.
- Prevent trivial infinite Elytra sustain from End farming.
- Preserve balance in both vanilla-like and modded environments.

### 16.4 - No Additional Rewards (Version 1)
The following are explicitly not included in version 1:
- No enchanted book drops.
- No rare or unique phantom loot.
- No new crafting materials.
- No Elytra upgrades or modifiers.
- No biome-specific loot variations.

Reason:
The mod's strength is its focused redesign of phantom behavior and role. Additional loot systems would introduce unnecessary complexity and dilute that focus.

## 17. Minor Balance Clarifications

### 17.1 - End Membrane Availability
Because phantoms exist naturally in the End:
- Phantom Membranes will be more accessible than in vanilla.
- This is intentional.

Balance is controlled via:
- Reduced Elytra repair efficiency (Section 16.3).
- Spawn pacing and encounter design (Sections 6 and 7).

No drop rate adjustments should be made.

### 17.2 - Design Consistency Reminder
All reward and balance decisions must follow these rules:
- Do not reduce Phantom Membrane drop rates.
- Do not introduce rarity-based frustration.
- Do not add reward systems that require farming optimization to understand.
- Keep all rewards intuitive and immediately understandable by players.

## 18. Documentation Fixes and Clarifications

### 18.1 - Explicit Loot Behavior
Clarify that:
- Phantom loot remains unchanged except for Elytra repair interaction.
- No hidden or undocumented drops exist.

This prevents ambiguity for both players and implementation agents.

### 18.2 - Implementation Expectation Note (Non-Technical)
Any implementation must ensure:
- Elytra repair behavior is consistent across client and server.
- Modified repair values are predictable and not random.
- Config changes are respected without requiring a world reset.

No code-level detail is required here; this is a behavioral requirement only.

## 19. Summary of Additions

This update introduces:
- A clear, simple reward loop (Phantom Membrane to Elytra repair).
- A balance mechanism (reduced repair efficiency).
- Explicit constraints to prevent scope creep.

This preserves the mod identity:
"Better Phantoms improves gameplay through behavior and encounter design, not through adding new items or reward systems."

## 20. Implementation Phase Plan

This section defines a staged delivery plan so the mod can be developed, tested, and tuned in controlled milestones.

### 20.1 - Phase 1: Foundation
Goals:
- Finalize config schema and defaults for version 1.
- Disable Overworld phantom natural spawning.
- Remove insomnia-based phantom spawning linkage.

Exit criteria:
- Overworld no longer produces natural phantoms.
- Insomnia no longer triggers phantom attacks.
- Config defaults match design intent.

### 20.2 - Phase 2: Dragon Fight Core
Goals:
- Add crystal-destroyed phantom wave triggers.
- Enforce active fight phantom cap.
- Add final-crystal frenzy phase and duration behavior.

Exit criteria:
- No phantoms spawn at dragon-fight start.
- Crystal progression creates readable pressure.
- Final crystal reliably triggers a short danger spike.

### 20.3 - Phase 3: Post-Dragon End Spawning
Goals:
- Enable natural End phantom spawning after dragon defeat.
- Implement sparse general End distribution.
- Implement increased End City density/pressure.

Exit criteria:
- General End travel has intermittent aerial threat.
- End Cities feel distinctly more dangerous.
- Spawn pressure remains readable and bounded.

### 20.4 - Phase 4: Behavior Pass
Goals:
- Tune high patrol altitude and detection logic.
- Implement lock-on, descent, and re-engage combat loop.
- Implement cover counterplay and anti-camping dispersal behavior.

Exit criteria:
- Phantoms are threatening but learnable.
- Cover is useful without creating a full threat reset.
- No large persistent overhead clumping during long cover use.

### 20.5 - Phase 5: Loot and Reward Balance
Goals:
- Keep phantom membrane drops unchanged.
- Apply reduced Elytra repair effectiveness from membranes.
- Ensure repair effectiveness remains configurable.

Exit criteria:
- Reward loop is useful but not abusable.
- Elytra sustain is moderated without changing drop rates.
- Behavior is consistent and predictable across client/server contexts.

### 20.6 - Phase 6: Stabilization and Release
Goals:
- Run multiplayer and dedicated-server validation.
- Verify performance and edge-case behavior.
- Finalize documentation and release notes.

Exit criteria:
- No critical regressions in core gameplay loops.
- Performance remains acceptable under expected encounter density.
- Player-facing docs fully match shipped behavior.
