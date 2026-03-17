# Magic Academy RPG -- Build Progress

**Last updated:** 2026-03-17 (session 5)
**Server target:** Paper 1.21.4, Java 21
**Build system:** Gradle 8.11.1 multi-module, all JARs produced via Shadow (GradleUp Shadow plugin)
**Root project:** `magic-academy/` (this folder)

---

## Context

Solo-first Magic Academy RPG Minecraft server. Design docs live at:
- `../yes.md` -- full game design spec (spells, dungeons, ranks, hideouts, relics)
- `../implementation_plan.md` -- phased build roadmap

**Key design rule:** Players must always have something meaningful to grind alone.

**Architecture decisions already locked in:**
- Custom Java plugins for all gameplay systems (no Citizens, no Mythic Dungeons, no ItemsAdder)
- MythicMobs free tier used only for mob AI/abilities -- all lifecycle management is custom
- SQLite for dev / MySQL for prod via a single `config.yml` flag in `magic-core`
- All content (spells, dungeons, NPCs, runes, loot tables) defined in YAML -- no code changes needed to add content
- Spell casting input: player holds spell in hotbar slot 0-3 and presses F (swap hands) to cast
- NPC system: Villager entities with AI disabled, identified by PersistentDataContainer key `magic:npc_id`
- Dungeon instances: copy template world folder -> load as new Bukkit world -> delete on complete
- Hideout worlds: lazy-loaded per player, named `hideout_<uuid_short>`, unloaded after 10 min idle
- Stat bonuses from hideout modules flow through `StatEngine` via `StatModifier` interface (no hard dependencies between modules)

---

## Completed

### Gradle Project Structure
- [settings.gradle.kts](settings.gradle.kts) -- all 9 submodules registered
- [build.gradle.kts](build.gradle.kts) -- root build uses GradleUp Shadow plugin 9.1.0, Paper repo, Java 21
- [gradle/wrapper/gradle-wrapper.properties](gradle/wrapper/gradle-wrapper.properties) -- Gradle 8.7 (wrapper JAR still missing)
- [.gitignore](.gitignore)
- Each module has its own `build.gradle.kts` with correct inter-module deps

### Dev Server Setup (local)
- `magic-academy/server/paper-1.21.4-232.jar` downloaded (valid size ~51 MB)
- `magic-academy/server/run.bat` launches Paper using local JDK 21
- `magic-academy/server/eula.txt` set to `eula=true`
- Plugins copied to `magic-academy/server/plugins/` (only `*-SNAPSHOT-all.jar`)
- WorldEdit installed (`worldedit-bukkit-7.3.14.jar`)
- ViaVersion + ViaBackwards installed for backwards compatibility
- ViaVersion config blocks clients older than 1.21 (`block-versions: ["<1.21"]`)
- Lobby schematic copied to `magic-academy/server/schematics/lobby.schematic`

### Build / Dependency Fixes
- Switched inter-module deps to `implementation` so API classes are shaded into each plugin JAR
- Fixed classloader issues: all plugins use `getInstance()` instead of static `get()` to get plugin instances via Bukkit PluginManager
- Created `MagicCoreAPI` interface in api module for cross-plugin communication
- Added HikariCP relocation in build.gradle.kts to avoid conflicts
- Removed SQLite relocation (causes UnsatisfiedLinkError with native libraries)
- Fixed resource-pack-prompt JSON format in server.properties
- build-deploy.ps1 now cleans old JARs before deploying, does not copy api JAR

### Module: `api` (shared, no plugin.yml)
Location: [api/src/main/java/gg/magic/academy/api/](api/src/main/java/gg/magic/academy/api/)

All interfaces and data types other modules import from here:

| File | Purpose |
|---|---|
| `MagicCoreAPI.java` | Interface implemented by MagicCore for cross-plugin access to database, player data, stat engine, artifact effects |
| `PluginAPI.java` | Generic interface for plugin instance access |
| `PluginAccess.java` | Utility class for safely getting plugin instances via Bukkit PluginManager |
| `gui/SimpleGui.java` | Custom lightweight inventory GUI - implements InventoryHolder, supports click handlers per slot |
| `gui/PagedGui.java` | Paginated GUI (5 rows, 36 item slots, navigation row), built on SimpleGui |
| `gui/GuiUtil.java` | Utility for building GUI items (name, lore, filler, glow) |
| `AcademyRank.java` | Enum: STUDENT->APPRENTICE->MAGE->MASTER_MAGE->ARCHMAGE with `.next()` and `.level()` |
| `Rarity.java` | Enum: COMMON/RARE/EPIC/LEGENDARY with Adventure TextColor |
| `element/Element.java` | Enum: FIRE/ICE/ARCANE/LIGHTNING/SHADOW |
| `spell/SpellShape.java` | Enum: PROJECTILE/BEAM/WAVE/NOVA/TRAP |
| `spell/SpellEffect.java` | Enum: NONE/EXPLOSION/CHAIN/SLOW/SHIELD_BREAK/LIFESTEAL |
| `spell/SpellTier.java` | Record: tier number, description, manaCost, cooldownMs, mythicSkillId, upgradeCost map |
| `spell/SpellTemplate.java` | Record: full spell definition loaded from YAML, with `tier(n)` and `maxTier()` helpers |
| `rune/RuneType.java` | Enum: ELEMENT/SHAPE/EFFECT |
| `rune/RuneTemplate.java` | Record: rune id, name, type, powerTier, customModelData |
| `artifact/ArtifactSource.java` | Enum: DUNGEON_SECRET_ROOM/WORLD_PUZZLE/BOSS_DROP |
| `artifact/ArtifactTemplate.java` | Record: artifact definition |
| `player/MagicPlayerData.java` | Mutable player state: mana, rank, spell tiers, loadout (4 slots), module levels, artifacts, cooldowns |
| `event/SpellCastEvent.java` | Cancellable -- fired before spell executes |
| `event/RuneDiscoveryEvent.java` | Fired when a player discovers a new spell combination for the first time |
| `event/RankAdvanceEvent.java` | Fired when a player advances rank |
| `event/DungeonCompleteEvent.java` | Fired when all rooms in a dungeon are cleared |

### Module: `magic-core`
Location: [magic-core/src/main/java/gg/magic/academy/core/](magic-core/src/main/java/gg/magic/academy/core/)

| File | Purpose |
|---|---|
| `MagicCore.java` | Main plugin class, wires all core systems |
| `database/DatabaseManager.java` | HikariCP pool, SQLite/MySQL, all DB operations. Tables: `players`, `player_spells`, `player_loadout`, `player_modules`, `player_artifacts`, `spell_discoveries`, `dungeon_clears`, `crafting_rate_limit` |
| `player/PlayerDataManager.java` | Cache (UUID->MagicPlayerData), async load on join, async save on quit, auto-save every 5 min |
| `mana/ManaSystem.java` | Mana regen tick (5/sec), `consumeMana()` with action bar feedback, mana bar display |
| `stat/StatEngine.java` | Aggregates `StatModifier` implementations. Methods: `computeMaxMana()`, `computeSpellDamageMultiplier()`, `computeManaRegenBonus()` |
| `stat/StatModifier.java` | Interface for other modules to inject stat bonuses without hard deps |
| `gui/GuiListener.java` | Global listener for InventoryClickEvent/InventoryDragEvent - routes to SimpleGui instances |
| `resources/config.yml` | Database mode, mana regen config, crafting rate limit |

### Module: `magic-items`
Location: [magic-items/src/main/java/gg/magic/academy/items/](magic-items/src/main/java/gg/magic/academy/items/)

| File | Purpose |
|---|---|
| `MagicItems.java` | Plugin main class |
| `registry/ItemRegistry.java` | Loads `plugins/MagicAcademy/items/*.yml`, builds `ItemStack` templates with custom_model_data and PDC tag `magic:item_id`. Static helper `getItemId(ItemStack)` |
| `loot/LootEntry.java` | Record: itemId, minAmount, maxAmount, weight |
| `loot/LootTable.java` | Weighted random roll, returns `List<ItemStack>` |
| `loot/LootTableRegistry.java` | Loads `plugins/MagicAcademy/loot_tables/*.yml` |

**Item identification:** All custom items tagged with `magic:item_id` (String) in PDC. Use `ItemRegistry.getItemId(stack)` anywhere to get the item's logical ID.

### Module: `magic-spells`
Location: [magic-spells/src/main/java/gg/magic/academy/spells/](magic-spells/src/main/java/gg/magic/academy/spells/)

| File | Purpose |
|---|---|
| `MagicSpells.java` | Plugin main class |
| `registry/SpellRegistry.java` | Loads `plugins/MagicAcademy/spells/*.yml` -> `SpellTemplate` objects |
| `registry/RuneRegistry.java` | Loads `plugins/MagicAcademy/runes/*.yml` and `runes/recipes.yml`. `resolveRecipe(element, shape, effect)` -> Optional spellId |
| `crafting/RuneCraftingHandler.java` | Intercepts WORKBENCH InventoryClickEvent on result slot. Validates 3-rune combo (1 ELEMENT + 1 SHAPE + 1 EFFECT). Checks rate limit (10/min per player). Known recipe -> grant spell. Unknown -> fire `RuneDiscoveryEvent` + server announce. Consumes runes on use. |
| `executor/SpellExecutor.java` | Listens for `PlayerSwapHandItemsEvent` (F key). Hotbar slots 0-3 = spell slots. Checks cooldown, calls `ManaSystem.consumeMana()`, fires `SpellCastEvent`. Dispatches to MythicMobs API via reflection (falls back to vanilla particles if MM not installed). |
| `upgrade/SpellUpgradeManager.java` | Validates item costs in player inventory, consumes items, increments spell tier in `MagicPlayerData` |
| `loadout/SpellLoadoutMenu.java` | 54-slot inventory GUI. Top row: 4 equipped slots. Rows below: all owned spells. Click to equip. |

**Spell casting flow:** Player presses F -> `SpellExecutor` -> cooldown check -> mana drain -> `SpellCastEvent` -> MythicMobs `castSkill()` via reflection -> fallback particle effect if MM unavailable.

### Module: `magic-npcs`
Location: [magic-npcs/src/main/java/gg/magic/academy/npcs/](magic-npcs/src/main/java/gg/magic/academy/npcs/)

**No Citizens dependency.** NPCs are Villager entities with AI disabled.

| File | Purpose |
|---|---|
| `MagicNpcs.java` | Plugin main class. Spawns NPCs 1 tick after enable. |
| `npc/NpcDefinition.java` | Record: id, name, world, location, yaw, InteractionType (DIALOGUE/VENDOR_MENU/DUNGEON_PORTAL/RANK_TRIAL), interactionTarget |
| `npc/NpcManager.java` | Reads `plugins/MagicAcademy/npcs/*.yml`. Spawns Villager at defined location with PDC tag `magic:npc_id`. Right-click -> routes to DialogueEngine, fires `NpcDungeonPortalEvent`, or fires `NpcRankTrialEvent`. |
| `npc/NpcDungeonPortalEvent.java` | Event carrying dungeonId -- listened to by `magic-dungeons` |
| `npc/NpcRankTrialEvent.java` | Event carrying trialId -- listened to by `magic-academy` |
| `dialogue/DialogueEngine.java` | Loads `plugins/MagicAcademy/dialogues/*.yml`. Branching tree: nodeId -> text + options. Options rendered as clickable chat via `ClickEvent.runCommand("/magic_dialogue <id> <node>")`. Supports `action: quest_dispatch:<questId>` which fires `DialogueQuestDispatchEvent`. |
| `dialogue/DialogueNode.java` | Record: id, text, List<DialogueOption>, action string |
| `dialogue/DialogueQuestDispatchEvent.java` | Fired by DialogueEngine when a quest action node is reached |

### Module: `magic-dungeons`
Location: [magic-dungeons/src/main/java/gg/magic/academy/dungeons/](magic-dungeons/src/main/java/gg/magic/academy/dungeons/)

| File | Purpose |
|---|---|
| `MagicDungeons.java` | Plugin main class. Listens for `NpcDungeonPortalEvent` -> calls `DungeonInstanceManager.enterDungeon()` |
| `template/RoomConfig.java` | Record: RoomType (PUZZLE/MOB_WAVE/MINIBOSS/BOSS/TREASURE), mythicmobs refs, loot table id |
| `template/DungeonTemplate.java` | Record: id, name, theme, difficulty, room list, HP scale values, required rank |
| `template/DungeonTemplateLoader.java` | Loads `plugins/MagicAcademy/dungeons/*.yml` |
| `instance/DungeonInstance.java` | Runtime state: instanceId, template, party (UUID list), world, phase (LOADING/IN_PROGRESS/COMPLETE/FAILED), currentRoomIndex, startTime. `getHpScalar()` for party scaling. |
| `instance/DungeonInstanceManager.java` | `enterDungeon()`: rank check -> async world copy -> Bukkit `createWorld()` -> teleport -> start room sequence. `completeDungeon()`: record clears, fire `DungeonCompleteEvent`, return players to hub after 10s, delete world. |
| `instance/RoomController.java` | Drives a single room: PUZZLE (auto-complete placeholder + 5s delay), MOB_WAVE (spawn via MythicMobs API, advance waves), MINIBOSS/BOSS (spawn MM mob, complete on boss death; fallback timer if spawn fails), TREASURE (roll loot table, give items). |

**World copy strategy:** Template worlds stored in `plugins/MagicAcademy/dungeon_worlds/<dungeonId>/`. Copied to server world container as `dungeon_<instanceId_short>`. Deleted after dungeon ends. If no template exists, a blank flat world is generated (acceptable for dev).

**Boss completion:** Uses `EntityDeathEvent` to match active boss UUID and complete the room (works with MythicMobs or vanilla fallback).

### Module: `magic-hideouts`
Location: [magic-hideouts/src/main/java/gg/magic/academy/hideouts/](magic-hideouts/src/main/java/gg/magic/academy/hideouts/)

| File | Purpose |
|---|---|
| `MagicHideouts.java` | Plugin main, registers `HideoutBuffProvider` with `MagicCore.getStatEngine()` |
| `module/ModuleTier.java` | Record: tier, description, cost map, maxManaBonus, spellDamageBonus, manaRegenBonus |
| `module/HideoutModule.java` | Record: id, name, description, List<ModuleTier> |
| `module/ModuleRegistry.java` | Loads `plugins/MagicAcademy/hideouts/modules.yml` |
| `module/HideoutBuffProvider.java` | Implements `StatModifier`. Reads player's module levels from `MagicPlayerData`, sums bonuses from `ModuleRegistry`. Registered with `StatEngine` under key `"hideout_buffs"`. |
| `manager/HideoutManager.java` | `visitHideout(player)`: lazy-loads world named `hideout_<uuid_short>`. `upgradeModule(player, moduleId)`: validates and consumes items, increments level in `MagicPlayerData`. Idle unload task runs every 10 min. |

### Module: `magic-academy`
Location: [magic-academy/src/main/java/gg/magic/academy/](magic-academy/src/main/java/gg/magic/academy/)

| File | Purpose |
|---|---|
| `MagicAcademyPlugin.java` | Plugin main. Listens for `NpcRankTrialEvent` -> calls `TrialManager.startTrial()` |
| `rank/RankGate.java` | Record: targetRank, requiredDungeonClears list, requiredSpells list, requiredDiscoveries count, trialId |
| `rank/RankManager.java` | Loads `plugins/MagicAcademy/academy/ranks.yml`. `meetsPreTrialRequirements()` checks DB for dungeon clears and `MagicPlayerData` for spells. `advanceRank()` updates data, tries LuckPerms group update via reflection (soft dep), fires `RankAdvanceEvent`, broadcasts server-wide. |
| `trial/TrialManager.java` | `startTrial()`: checks pre-reqs, teleports to `academy_trials` world, spawns MythicMobs boss. `completeTrial()`: advances rank via `RankManager`, teleports back to academy after 3s. Active trials tracked in `Map<UUID, UUID>` (playerUUID -> bossEntityUUID). |

**Trial completion:** Uses `EntityDeathEvent` to match boss UUID in `activeTrials` and complete the trial (works with MythicMobs or vanilla fallback).

### Module: `magic-world`
Location: [magic-world/src/main/java/gg/magic/academy/world/](magic-world/src/main/java/gg/magic/academy/world/)

| File | Purpose |
|---|---|
| `MagicWorld.java` | Plugin main, starts storm and boss schedulers |
| `zone/WorldZone.java` | Record: id, name, world, bounding box (min/max x/y/z), enterMessage, mobTable, ambientEffect |
| `zone/ZoneManager.java` | Loads `plugins/MagicAcademy/zones/*.yml`. Listens `PlayerMoveEvent` (block-change only). Detects zone transitions, sends enter message. No WorldGuard dependency -- pure AABB check. |
| `event/ManaStormController.java` | Random interval 1-2 hrs, 5 min duration. Sets world weather to storm/thunder. Server-wide broadcast on start/end. Reschedules itself. |
| `boss/WorldBossScheduler.java` | Loads `plugins/MagicAcademy/world_bosses.yml`. Per-boss interval scheduling. Announces 5 min before, then spawns via MythicMobs API (Elder Guardian fallback). Reschedules after spawn. |

### Starter YAML Content
All under [server/plugins/MagicAcademy/](server/plugins/MagicAcademy/)

| Path | Contents |
|---|---|
| `spells/firebolt.yml` | 4-tier fire projectile, upgrade costs defined |
| `spells/mana_shield.yml` | 4-tier arcane nova shield |
| `spells/arcane_push.yml` | 4-tier arcane wave knockback |
| `spells/ice_spike.yml` | 4-tier ice projectile with slow |
| `runes/fire_element.yml` | Fire element rune (CMD 1001) |
| `runes/ice_element.yml` | Ice element rune (CMD 1002) |
| `runes/arcane_element.yml` | Arcane element rune (CMD 1003) |
| `runes/projectile_shape.yml` | Projectile shape rune (CMD 1010) |
| `runes/wave_shape.yml` | Wave shape rune (CMD 1011) |
| `runes/nova_shape.yml` | Nova shape rune (CMD 1012) |
| `runes/slow_effect.yml` | Slow effect rune (CMD 1020) |
| `runes/shield_break_effect.yml` | Shield break effect rune (CMD 1021) |
| `runes/none_effect.yml` | Null effect rune (CMD 1022) |
| `runes/recipes.yml` | 4 known recipes -> 4 starter spells |
| `items/defaults.yml` | All ingredient items + all rune items as ItemRegistry entries |
| `dungeons/wizard_ruins.yml` | T1 dungeon, STUDENT rank, 5 rooms, Lich boss |
| `dungeons/crystal_caverns.yml` | T2 dungeon, APPRENTICE rank, 6 rooms, Crystal Dragonling boss |
| `loot_tables/ruins_treasure.yml` | 4 rolls, 0.85 chance, 6 entry types |
| `loot_tables/crystal_treasure.yml` | 4 rolls, 0.9 chance, 6 entry types |
| `npcs/academy_npcs.yml` | 4 NPCs near spawn: Headmaster (0,64,3), Rune Master (4,64,0), Dungeon Keeper (-4,64,0), Trialmaster (0,64,-3) |
| `dialogues/academy_dialogues.yml` | Headmaster + Rune Master branching dialogue trees |
| `hideouts/modules.yml` | 4 modules: Spell Lab (T3), Alchemy Lab (T3), Rune Forge (T3), Artifact Display (T2) |
| `academy/ranks.yml` | Gates for APPRENTICE/MAGE/MASTER_MAGE/ARCHMAGE |
| `zones/world_zones.yml` | 3 zones: Forbidden Forest, Ancient Ruins, Mana Storm Crater |

---

## Not Yet Built (Prioritized)

### Priority 1 -- Required to Compile and Run

- [ ] **Gradle wrapper JAR** -- `gradle/wrapper/gradle-wrapper.jar` is missing, so `./gradlew` won't work. Use local Gradle 8.11.1 (`.tools/gradle-8.11.1/bin/gradle.bat`) or regenerate the wrapper.
- [x] **`/magic_dialogue` command handler** -- Implemented in `magic-npcs` (`DialogueCommand`).
- [x] **`SpellLoadoutMenu` click handler** -- Implemented in `magic-spells` (`SpellLoadoutListener`).

### Priority 2 -- Gameplay Gaps (Placeholder to Real)

- [x] **Boss death detection** -- `EntityDeathEvent` listeners wired in `magic-dungeons` and `magic-academy`; boss UUID -> completion (MythicMobs or vanilla fallback).
- [x] **Spell cooldown tracking bug** -- Cooldown check uses `MagicPlayerData.isOnCooldown(...)`; remaining time derived from stored timestamps.
- [x] **Hideout `/visit` command** -- `/hideout` command implemented; teleports to hideout and opens upgrade menu.
- [x] **Module upgrade command/GUI** -- `ModuleUpgradeMenu` + `ModuleUpgradeListener` wired to `HideoutManager.upgradeModule()`.
- [x] **`/magic_dialogue` permission** -- Permission defaults to true (no LuckPerms setup required).

### Priority 3 -- Phase 2 Content Systems

- [x] **Magical Research tracking** -- `/discoveries` command opens paginated GUI (`DiscoveriesMenu`) in `magic-spells`. Shows all spells with first-discoverer name and date. Read-only, all clicks blocked.
- [x] **Relic/Artifact system** -- `ArtifactEffectHandler` (api), `ArtifactEffectRegistry` + `ArtifactStatProvider` (magic-core), `ArtifactRegistry` + `ArtifactPickupListener` (magic-items), `ArtifactDisplayMenu` + listener (magic-hideouts). 7 starter artifacts in `artifacts/starter_artifacts.yml`. `/hideout artifacts` to manage display.
- [x] **Contract / Quest system** -- `ContractManager`, `ContractBoardMenu`, `ContractBoardListener`, `ContractCommand` in `magic-academy`. Tracks KILL_MOB / COMPLETE_DUNGEON / CAST_SPELL objectives. `/contracts` to open board. `DialogueQuestDispatchEvent` listened to. 6 starter contracts in `contracts/starter_contracts.yml`.
- [x] **Party dungeon system** -- `PartyManager`, `Party`, `PartyCommand` in `magic-dungeons`. `/party invite|accept|decline|leave|kick|disband|info`. All party members teleported into dungeon instance together with HP scaling.
- [x] **PlaceholderAPI expansion** -- `MagicPlaceholderExpansion` in `magic-core`. `%magic_mana%`, `%magic_max_mana%`, `%magic_rank%`, `%magic_spell_1-4%`. Registered on startup if PlaceholderAPI is installed.

### Lobby Hotbar System (session 2)

- `magic-core/hotbar/LobbyHotbarManager.java` -- Gives 5 hotbar items on join and hub world entry. Items can't be dropped (PDC tag `magic:hotbar_action`).
  - Slot 0: Spellbook → opens Spell Loadout
  - Slot 1: Discoveries → opens Discoveries Browser
  - Slot 2: Contract Board → opens Contract Board
  - Slot 3: Hideout → visits hideout
  - Slot 8: Profile → prints rank/mana/spell/artifact summary to chat
- Per-plugin right-click handlers: `SpellHotbarListener` (magic-spells), `HideoutHotbarListener` (magic-hideouts), `AcademyHotbarListener` (magic-academy)

---

### Priority 4 -- Infrastructure

- [x] **Custom GUI System** -- Replaced triumph-gui with lightweight custom implementation in api module: SimpleGui, PagedGui, GuiUtil. GuiListener in magic-core handles click events globally.
- [x] **MythicMobs YAML for all boss IDs** -- Created `server/plugins/MythicMobs/mobs.yml` and `skills.yml` with all dungeon and trial bosses
- [x] **triumph-gui Migration** -- Replaced manual inventory handling with triumph-gui library for all menus
- [x] **NPC Command Upgrades** -- Enhanced `/npcs` command with list, info, spawn, tp, setspawn subcommands
- [x] **Build/Deploy Script** -- Created `build-deploy.ps1` for one-command build and deploy
- [x] **Oraxen Integration** -- Set up Oraxen config for custom spell icons (`magic_spell_1-4`)
- [x] **GUI Custom Textures** -- Updated SpellLoadoutMenu and DiscoveriesMenu to use Oraxen item API
- [ ] **Resource pack** -- Custom model data IDs are assigned (1001-1030 for runes, 2000-2003 for ingredients). Need JSON model files in `server/resource_pack/assets/minecraft/models/item/`. Server must be configured to serve the resource pack.
- [ ] **`academy_trials` world** -- `TrialManager` teleports to a world named `academy_trials`. This world must exist on the server (hand-built arena or Multiverse-generated).
- [ ] **Dungeon template worlds** -- `DungeonInstanceManager` copies from `plugins/MagicAcademy/dungeon_worlds/<dungeonId>/`. These prefab world folders need to be built by map builders and placed there.
- [x] **`world_bosses.yml`** -- Created with 2 starter world bosses: `RuinedColossus` (90 min) and `StormWarden` (120 min).
- [x] **Auto-save call** -- `PlayerDataManager.startAutoSave()` is defined but never called. Should be called in `MagicCore.onEnable()`.

---

## Plugin Load Order

Plugins must load in this dependency order (Paper handles this via `plugin.yml` `depend:`):

```
MagicCore
  --- MagicItems
        |-- MagicSpells
        |-- MagicNpcs
        |     |-- MagicDungeons
        |     --- MagicAcademy (also depends on MagicSpells)
        --- MagicHideouts
MagicWorld (depends on MagicCore + MagicItems)
```

---

## How to Build

```bash
cd "magic-academy"

# First time: need Gradle wrapper JAR (see Priority 1 above)
gradle wrapper   # if Gradle is installed globally

# Build all JARs (local Gradle 8.11.1)
.tools/gradle-8.11.1/bin/gradle.bat -p "magic-academy" shadowJar

# Or, if wrapper is fixed:
./gradlew shadowJar

# JARs produced at:
# api/build/libs/api-1.0.0-SNAPSHOT.jar               (no plugin.yml, not deployed)
# magic-core/build/libs/magic-core-1.0.0-SNAPSHOT.jar
# magic-items/build/libs/magic-items-1.0.0-SNAPSHOT.jar
# magic-spells/build/libs/magic-spells-1.0.0-SNAPSHOT.jar
# magic-npcs/build/libs/magic-npcs-1.0.0-SNAPSHOT.jar
# magic-dungeons/build/libs/magic-dungeons-1.0.0-SNAPSHOT.jar
# magic-hideouts/build/libs/magic-hideouts-1.0.0-SNAPSHOT.jar
# magic-academy/build/libs/magic-academy-1.0.0-SNAPSHOT.jar
# magic-world/build/libs/magic-world-1.0.0-SNAPSHOT.jar
```

Deploy all `*-SNAPSHOT.jar` files (except `api`) into `server/plugins/`.

---

## Phase 1 Smoke Test (when all Priority 1+2 items are done)

1. Fresh player joins -> spawns at academy -> sees 4 NPCs
2. Talk to Rune Master -> dialogue tree works, options clickable
3. Obtain fire_element + projectile_shape + none_effect runes -> combine at workbench -> Firebolt granted, discovery recorded, announced server-wide
4. Equip Firebolt in slot 0 via loadout menu -> hold hotbar slot 0 -> press F -> spell fires
5. Talk to Dungeon Keeper -> enter Wizard Ruins -> room sequence plays -> boss dies -> loot received -> return to academy
6. Upgrade Firebolt to T2 using collected materials (hold slot 0 and run `/spellupgrade`)
7. Upgrade Spell Laboratory in hideout -> mana cap increases
8. Talk to Trialmaster -> complete Apprentice trial -> rank advances, LuckPerms group updated

