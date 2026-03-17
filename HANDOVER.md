# Handover Document - Magic Academy RPG

**Last updated:** 2026-03-17
**Maintainer:** Sam

---

## Quick Start

```powershell
# Build and deploy to dev server
cd magic-academy
powershell -ExecutionPolicy Bypass -File build-deploy.ps1

# Start dev server
cd server
run.bat
```

---

## Project Structure

```
magic-academy/
├── api/                    # Shared interfaces, enums, events
├── magic-core/             # Database, player data, mana, stat engine
├── magic-items/            # Item registry, loot tables
├── magic-spells/           # Spell system, rune crafting, loadout GUI
├── magic-npcs/             # Villager NPCs, dialogue system
├── magic-dungeons/         # Instanced dungeons, party system
├── magic-hideouts/         # Per-player hideout worlds
├── magic-academy/          # Ranks, trials, contracts
├── magic-world/            # Zones, mana storms, world bosses
└── server/                # Paper server, plugins, configs
```

---

## Key Systems

### Dependencies
- **Paper 1.21.4** + Java 21
- **MythicMobs** - mob AI (soft depend)
- **Oraxen** - custom item textures (soft depend)
- **LuckPerms** - rank groups (soft depend)
- **PlaceholderAPI** - placeholders (soft depend)
- **ViaVersion + ViaBackwards** - version compatibility (ViaBackwards must be 5.4.0+)

### Custom GUI System
The project uses a lightweight custom GUI system (no external dependencies):
- `SimpleGui` - Basic inventory GUI with click handlers per slot
- `PagedGui` - Paginated GUI (36 items per page + navigation)
- `GuiUtil` - Item builder utilities
- All menus use these classes from the api module

### Content is YAML-Driven
All gameplay content lives in `server/plugins/MagicAcademy/`:
- `spells/*.yml` - spell definitions
- `runes/*.yml` - rune definitions  
- `dungeons/*.yml` - dungeon configs
- `npcs/*.yml` - NPC spawn locations
- `dialogues/*.yml` - dialogue trees
- `hideouts/modules.yml` - hideout upgrades
- `academy/ranks.yml` - rank gates

### Spell Casting
- Hold spell in hotbar slots 0-3
- Press F (swap hands) to cast

### NPC System
- Villager entities with AI disabled
- Identified by PDC tag `magic:npc_id`
- Interaction types: DIALOGUE, DUNGEON_PORTAL, RANK_TRIAL, VENDOR_MENU

---

## Commands

| Command | Description |
|---------|-------------|
| `/spellbook` | Open spell loadout |
| `/discoveries` | View discovered spells |
| `/contracts` | Open contract board |
| `/hideout` | Visit your hideout |
| `/hideout leave` | Return to hub |
| `/npcs list` | List all NPCs |
| `/npcs spawn <id>` | Spawn single NPC |
| `/npcs tp <id>` | Teleport to NPC |
| `/npc setspawn <id>` | Move NPC to your position |
| `/dungeon <id>` | Enter dungeon |
| `/party invite <player>` | Invite to party |
| `/magic_dialogue <id> <node>` | Advance dialogue |

---

## Build System

```bash
# Using local Gradle (recommended)
.tools/gradle-8.11.1/bin/gradle.bat shadowJar

# Deploys to server/plugins/
```

All modules use **Shadow JAR** except `api` (plain JAR).

---

## Known Issues / TODOs

1. **ViaBackwards version** - Must use ViaBackwards 5.4.0+ to match ViaVersion 5.4.0. Download from https://viaversion.com/downloads/

2. **Dungeon template worlds** - Currently generates flat worlds. Need pre-built world folders in `plugins/MagicAcademy/dungeon_worlds/`

3. **academy_trials world** - Trial manager teleports here. World needs to exist (can use Multiverse or create manually)

4. **Resource pack** - Oraxen generates automatically. Players will be prompted to download

---

## Adding New Content

### New Spell
1. Create `server/plugins/MagicAcademy/spells/my_spell.yml`
2. Define tiers, mana cost, cooldown, effects
3. Add recipe to `runes/recipes.yml` if craftable

### New NPC
1. Edit `server/plugins/MagicAcademy/npcs/academy_npcs.yml`
2. Set location, name, interaction type
3. Use `/npc setspawn <id>` to position

### New Dungeon
1. Create `server/plugins/MagicAcademy/dungeons/my_dungeon.yml`
2. Define rooms, bosses, loot tables
3. Add NPC that triggers `DUNGEON_PORTAL`

---

## Files of Interest

- `AGENTS.md` - AI agent guidelines
- `PROGRESS.md` - Detailed build progress
- `CLAUDE.md` - Original design doc
- `build-deploy.ps1` - Build script

---

## Contact

For questions, check:
1. Design doc at `../yes.md`
2. Implementation plan at `../implementation_plan.md`
3. Code comments in respective modules
