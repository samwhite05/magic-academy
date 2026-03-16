# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All commands run from the `magic-academy/` directory.

```bash
# Build all modules (produces shadow JARs)
./gradlew shadowJar

# Build a single module
./gradlew :magic-spells:shadowJar

# Build and deploy to dev server (copy JARs to server/plugins/)
./gradlew shadowJar && cp */build/libs/*-all.jar server/plugins/

# Clean build
./gradlew clean shadowJar
```

No test infrastructure exists — there are no unit or integration tests.

## Project Structure

Multi-module Gradle project (Kotlin DSL) targeting Paper 1.21.4 / Java 21.

```
api/              Shared interfaces, enums, records, and custom events — no plugin.yml
magic-core/       Foundation plugin: DB, player data cache, mana system, stat engine
magic-items/      Item registry and loot tables loaded from YAML
magic-spells/     Spell system: rune crafting, spell execution, loadout GUI
magic-npcs/       Custom lightweight NPC system (Villager entities, no Citizens)
magic-dungeons/   Instanced dungeon system with world copying
magic-hideouts/   Per-player hideout worlds with upgradeable modules
magic-academy/    Rank advancement and trial boss management
magic-world/      World zones, mana storms, world boss scheduling
server/           Dev server (Paper 1.21.4), plugins deploy here
```

**Dependency chain:** `api` → `magic-core` → `magic-items` / `magic-npcs` / `magic-hideouts` / `magic-world` → `magic-spells` → `magic-dungeons` → `magic-academy`

All modules declare `compileOnly(project(":api"))` — the `api` JAR is NOT shaded into plugin JARs to avoid classloader conflicts.

## Architecture

### Data Layer (magic-core)
- **DatabaseManager**: HikariCP pool, SQLite (dev) or MySQL (prod) toggled by `config.yml`. Tables: `players`, `player_spells`, `player_loadout`, `player_modules`, `player_artifacts`, `spell_discoveries`, `dungeon_clears`, `crafting_rate_limit`.
- **PlayerDataManager**: UUID → `MagicPlayerData` in-memory cache. Async load on join, async save on quit, auto-save every 5 min.
- **ManaSystem**: Regen tick at 5 mana / 20 ticks, shown in action bar.
- **StatEngine**: Aggregates `StatModifier` implementations at runtime. Hideout buffs plug in without hard module dependencies.

### Content is YAML-driven
All game content lives under `server/plugins/MagicAcademy/`:
- `spells/` — spell definitions; `runes/` — rune definitions + `recipes.yml`
- `items/defaults.yml`, `loot_tables/`
- `dungeons/`, `dungeon_worlds/<dungeonId>/` (world template folders)
- `npcs/`, `dialogues/`
- `hideouts/modules.yml`, `academy/ranks.yml`
- `zones/`, `world_bosses.yml`

Adding new spells, dungeons, NPCs, etc. requires only YAML — no code changes.

### Key Design Decisions
- **NPC system**: Custom Villager-based NPCs with AI disabled, identified by PDC key `magic:npc_id`. **Do not introduce Citizens as a dependency.**
- **Spell casting input**: Player presses F (swap hands) while holding an item in hotbar slots 0–3.
- **Dungeon instances**: Template world folder is copied → loaded as a Bukkit world → deleted after completion.
- **Hideout worlds**: Lazy-loaded per player, named `hideout_<uuid_short>`, auto-unloaded after 10 min idle.
- **MythicMobs integration**: Used only for mob AI/abilities via reflection and soft-depend. All lifecycle management (spawning, death tracking) is custom.
- **LuckPerms integration**: Also via reflection / soft-depend in `magic-academy` for rank group assignment.
- **Item identity**: Custom items identified by PDC tag `magic:item_id`, not display names.

### Event-Driven Module Communication
Custom events decouple modules: `SpellCastEvent`, `RuneDiscoveryEvent`, `RankAdvanceEvent`, `DungeonCompleteEvent`, `NpcDungeonPortalEvent`, `NpcRankTrialEvent`, `DialogueQuestDispatchEvent`. Prefer firing/listening to these events over direct cross-module method calls.

## Package Roots

| Module | Package |
|---|---|
| api | `gg.magic.academy.api` |
| magic-core | `gg.magic.academy.core` |
| magic-items | `gg.magic.academy.items` |
| magic-spells | `gg.magic.academy.spells` |
| magic-npcs | `gg.magic.academy.npcs` |
| magic-dungeons | `gg.magic.academy.dungeons` |
| magic-hideouts | `gg.magic.academy.hideouts` |
| magic-academy | `gg.magic.academy` |
| magic-world | `gg.magic.academy.world` |

## API Enums / Records Quick Reference

- `AcademyRank`: STUDENT → APPRENTICE → MAGE → MASTER_MAGE → ARCHMAGE
- `Rarity`: COMMON / RARE / EPIC / LEGENDARY
- `Element`: FIRE / ICE / ARCANE / LIGHTNING / SHADOW
- `SpellShape`: PROJECTILE / BEAM / WAVE / NOVA / TRAP
- `SpellEffect`: NONE / EXPLOSION / CHAIN / SLOW / SHIELD_BREAK / LIFESTEAL
- `RuneType`: ELEMENT / SHAPE / EFFECT (rune crafting requires 1 of each)
