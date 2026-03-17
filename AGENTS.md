# AGENTS.md - Agent Coding Guidelines for Magic Academy

This file provides guidance for AI agents working on this codebase.

## Build Commands

All commands run from the `magic-academy/` directory.

```bash
# Build all modules (produces shadow JARs)
./gradlew shadowJar

# Build a single module
./gradlew :magic-spells:shadowJar

# Build and deploy to dev server (copies JARs to server/plugins/)
./gradlew shadowJar && cp */build/libs/*-all.jar server/plugins/

# Clean build
./gradlew clean shadowJar

# Run a specific task
./gradlew :magic-core:build

# Check dependencies without building
./gradlew dependencies
```

**No test infrastructure exists** - there are no unit or integration tests.

## Project Overview

Multi-module Gradle project (Kotlin DSL) targeting Paper 1.21.4 / Java 21.

```
api/              Shared interfaces, enums, records, and custom events
magic-core/       Foundation plugin: DB, player data cache, mana system, stat engine
magic-items/      Item registry and loot tables loaded from YAML
magic-spells/     Spell system: rune crafting, spell execution, loadout GUI
magic-npcs/       Custom lightweight NPC system (Villager entities)
magic-dungeons/   Instanced dungeon system with world copying
magic-hideouts/   Per-player hideout worlds with upgradeable modules
magic-academy/    Rank advancement and trial boss management
magic-world/      World zones, mana storms, world boss scheduling
server/           Dev server (Paper 1.21.4), plugins deploy here
```

**Dependency chain:** `api` → `magic-core` → `magic-items` / `magic-npcs` / `magic-hideouts` / `magic-world` → `magic-spells` → `magic-dungeons` → `magic-academy`

All modules declare `compileOnly(project(":api"))` - the `api` JAR is NOT shaded into plugin JARs to avoid classloader conflicts.

## Code Style Guidelines

### General Style

- **No comments** - Write self-explanatory code without inline comments
- **Braces** - Use K&R style (opening brace on same line)
- **Indentation** - 4 spaces (no tabs)
- **Line length** - Aim for under 120 characters per line
- **Imports** - Grouped: java.*, org.bukkit.*, other external, project imports; no wildcards
- **Blank lines** - Single blank line between logical sections, max one blank line between methods

### Package Structure

| Module | Package Root |
|--------|--------------|
| api | `gg.magic.academy.api` |
| magic-core | `gg.magic.academy.core` |
| magic-items | `gg.magic.academy.items` |
| magic-spells | `gg.magic.academy.spells` |
| magic-npcs | `gg.magic.academy.npcs` |
| magic-dungeons | `gg.magic.academy.dungeons` |
| magic-hideouts | `gg.magic.academy.hideouts` |
| magic-academy | `gg.magic.academy` |
| magic-world | `gg.magic.academy.world` |

Subpackages: `listener/`, `command/`, `menu/`, `registry/`, `manager/`, `template/`, `instance/`, `module/`, etc.

### Naming Conventions

- **Classes/Interfaces**: PascalCase (e.g., `MagicAcademyPlugin`, `SpellRegistry`)
- **Methods**: camelCase (e.g., `getPlayerData()`, `startTrial()`)
- **Fields/Variables**: camelCase, prefixed with type indicator where appropriate
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `MAX_MANA`, `TICK_RATE`)
- **Enums**: PascalCase, enum values UPPER_SNAKE_CASE

### Types and Variables

- Use explicit types; avoid `var` except for clearly readable local variable type inference
- Use records for simple data carriers (e.g., `record DiscoveryRecord(...)`)
- Prefer immutable collections when possible (`List.of()`, `Set.of()`, `Map.of()`)
- Use primitive types where applicable (`int` not `Integer` for counts)

### Error Handling

- Log errors using `plugin.getLogger().log(Level.SEVERE, "message", e)` for exceptions
- Log warnings with `Level.WARNING` for recoverable issues
- Never swallow exceptions silently - always log or rethrow appropriately
- Use try-with-resources for all closeable resources (connections, statements, result sets)
- Return safe default values on error (e.g., empty list, false, 0) when operation can continue

### Plugin Architecture

- Each module has a main class extending `JavaPlugin`
- Main class implements `Listener` for event handlers
- Use singleton pattern with static `instance` field for plugin access
- Register events in `onEnable()` using `getServer().getPluginManager().registerEvents()`
- Register commands using `getCommand(...).setExecutor(...)`
- Always null-check before using nullable returns from Bukkit APIs

### Database Operations

- Use HikariCP for connection pooling
- Use SQLite in dev, MySQL in production (configurable via `config.yml`)
- Always use PreparedStatement with parameter binding to prevent SQL injection
- Use try-with-resources for all database resources
- Log SQL errors with context (e.g., "Failed to load player " + uuid)

### YAML Configuration

- Content is YAML-driven where possible
- Config files in `server/plugins/MagicAcademy/`:
  - `spells/`, `runes/` - spell and rune definitions
  - `items/defaults.yml`, `loot_tables/` - item configs
  - `dungeons/`, `dungeon_worlds/<dungeonId>/` - dungeon configs
  - `npcs/`, `dialogues/` - NPC configs
  - `hideouts/modules.yml` - hideout upgrades
  - `academy/ranks.yml` - rank definitions

### Event-Driven Design

- Prefer custom events for cross-module communication
- Custom events: `SpellCastEvent`, `RuneDiscoveryEvent`, `RankAdvanceEvent`, `DungeonCompleteEvent`, `NpcDungeonPortalEvent`, `NpcRankTrialEvent`, `DialogueQuestDispatchEvent`
- Fire events using `Bukkit.getServer().getPluginManager().callEvent(new MyEvent(...))`

### Key Design Constraints

- **NPCs**: Custom Villager-based NPCs with AI disabled, identified by PDC key `magic:npc_id`. Do NOT use Citizens.
- **Spell casting**: Player presses F (swap hands) while holding an item in hotbar slots 0-3
- **Dungeon instances**: Template world folder copied → loaded as Bukkit world → deleted after completion
- **Hideout worlds**: Lazy-loaded per player, named `hideout_<uuid_short>`, auto-unloaded after 10 min idle
- **MythicMobs**: Used only for mob AI/abilities via reflection and soft-depend
- **LuckPerms**: Used for rank group assignment via reflection/soft-depend
- **Item identity**: Custom items identified by PDC tag `magic:item_id`, not display names

### API Enums / Records Quick Reference

- `AcademyRank`: STUDENT → APPRENTICE → MAGE → MASTER_MAGE → ARCHMAGE
- `Rarity`: COMMON / RARE / EPIC / LEGENDARY
- `Element`: FIRE / ICE / ARCANE / LIGHTNING / SHADOW
- `SpellShape`: PROJECTILE / BEAM / WAVE / NOVA / TRAP
- `SpellEffect`: NONE / EXPLOSION / CHAIN / SLOW / SHIELD_BREAK / LIFESTEAL
- `RuneType`: ELEMENT / SHAPE / EFFECT

### Import Order Example

```java
package gg.magic.academy.spells;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import gg.magic.academy.api.spell.SpellTemplate;
import gg.magic.academy.core.player.PlayerDataManager;
```

### Getter/Setter Style

```java
public class MyPlugin extends JavaPlugin {
    private static MyPlugin instance;
    private Manager manager;

    public static MyPlugin get() { return instance; }
    public Manager getManager() { return manager; }
}
```

### Common Pitfalls

- Always use `JavaPlugin.getPlugin(Class.class)` or store instance statically
- Use Bukkit scheduler for async operations, not raw threads
- Never access player data synchronously during player connect events
- Remember Minecraft uses 1/20 second ticks (20 ticks = 1 second)
- Entity UUIDs are not stable across restarts for some entity types
- Use PersistentDataContainer (PDC) for item/entity metadata, not display names or lore
