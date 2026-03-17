# Magic Academy — Custom Items Reference

---

## Materials (crafting ingredients)

Drop from dungeons and world content. Spent to upgrade hideout modules.

| Item | What it's for |
|------|--------------|
| **Mana Crystal** | Core currency — used in almost every hideout upgrade (Spell Lab, Rune Forge, Artifact Display) |
| **Void Mushroom** | Mid-tier material — Alchemy Lab and early Rune Forge upgrades |
| **Phoenix Feather** | High-tier material — Tier 3 module upgrades |
| **Ghost Dust** | High-tier material — pairs with phoenix feather in top-tier upgrades |

**Necromancy Rune** and **Dark Spell Tome** are listed as items but have no defined use yet — placeholders for future content.

---

## Runes (spell crafting)

Players combine **one of each type** at the Rune Forge to craft spells.
Recipe format: `element + shape + effect = spell`

### Element Runes — what the spell is made of
| Item | Effect |
|------|--------|
| **Fire Rune** | Fire spells |
| **Ice Rune** | Ice spells |
| **Arcane Rune** | Arcane spells |

### Shape Runes — how the spell travels
| Item | Effect |
|------|--------|
| **Projectile Rune** | Flies forward |
| **Wave Rune** | Spreads in a wave |
| **Nova Rune** | Bursts in all directions |

### Effect Runes — extra modifier
| Item | Effect |
|------|--------|
| **Slow Rune** | Adds slow on hit |
| **Shield Break Rune** | Breaks shields |
| **Null Rune** | No effect (pure form) |

### Known Recipes
| Combination | Spell |
|-------------|-------|
| Fire + Projectile + Null | **Firebolt** |
| Arcane + Nova + Shield Break | **Mana Shield** |
| Arcane + Wave + Null | **Arcane Push** |
| Ice + Projectile + Slow | **Ice Spike** |

Other combinations are discoverable by experimentation — not listed in-game.

---

## Spells

Cast by pressing **F (swap hands)** while holding a spell in hotbar slots 0–3. Each spell has 4 tiers — upgrade by holding the spell and running `/spellupgrade`, which consumes the required materials from your inventory. Spells are created by combining 3 runes at a **vanilla crafting table**.

### Firebolt
`Fire + Projectile + Null`

| Tier | Description | Mana | Cooldown | Upgrade Cost |
|------|-------------|------|----------|--------------|
| 1 | Basic bolt of fire flies toward target | 10 | 1.5s | — |
| 2 | Explodes on impact, dealing area damage | 14 | 1.6s | 5x Void Mushroom, 3x Mana Crystal |
| 3 | Leaves burning ground where it lands | 18 | 1.8s | 2x Phoenix Feather, 8x Mana Crystal |
| 4 | Chains between up to 3 nearby enemies | 24 | 2.2s | 5x Ghost Dust, 5x Phoenix Feather |

### Mana Shield
`Arcane + Nova + Shield Break`

| Tier | Description | Mana | Cooldown | Upgrade Cost |
|------|-------------|------|----------|--------------|
| 1 | Barrier absorbs the next hit | 20 | 8s | — |
| 2 | Absorbs 2 hits before breaking | 25 | 7s | 5x Mana Crystal |
| 3 | Reflects 25% of absorbed damage back at attacker | 30 | 6.5s | 12x Mana Crystal, 4x Void Mushroom |
| 4 | Shield detonates on break, stunning nearby enemies | 38 | 6s | 20x Mana Crystal, 3x Ghost Dust |

### Arcane Push
`Arcane + Wave + Null`

| Tier | Description | Mana | Cooldown | Upgrade Cost |
|------|-------------|------|----------|--------------|
| 1 | Burst of arcane energy pushes enemies away | 12 | 3s | — |
| 2 | Wider wave hits enemies in a wider arc | 16 | 2.8s | 4x Mana Crystal |
| 3 | Pushed enemies take fall damage on landing | 20 | 2.6s | 10x Mana Crystal, 3x Void Mushroom |
| 4 | Creates a persistent shockwave ring that lingers | 28 | 3.5s | 18x Mana Crystal, 3x Phoenix Feather |

### Ice Spike
`Ice + Projectile + Slow`

| Tier | Description | Mana | Cooldown | Upgrade Cost |
|------|-------------|------|----------|--------------|
| 1 | Spike of ice slows the target on hit | 11 | 2s | — |
| 2 | Spike shatters on impact into 3 shards | 15 | 2.2s | 4x Mana Crystal, 2x Void Mushroom |
| 3 | Target is frozen in place for 2 seconds | 20 | 2.5s | 10x Mana Crystal, 6x Void Mushroom |
| 4 | Blizzard of spikes erupts from the impact point | 28 | 3s | 18x Mana Crystal, 4x Ghost Dust |

---

## Artifacts (passive stat relics)

Found in dungeons, boss drops, and world puzzles. Placed in your hideout's Artifact Display to grant permanent passive bonuses.

| Item | Rarity | Source | Bonus |
|------|--------|--------|-------|
| **Ember Shard** | Common | Dungeon secret room | +spell damage (minor) |
| **Void Fragment** | Common | World puzzle | +mana regen (minor) |
| **Frost Crystal** | Rare | Boss drop | +max mana (minor) |
| **Arcane Prism** | Rare | Dungeon secret room | Balanced stats |
| **Lich Heartstone** | Epic | Boss drop | +max mana (major) |
| **Storm Core** | Epic | Boss drop | +spell damage (major) |
| **Ancient Wellspring** | Legendary | World puzzle | +mana regen (major) |

---

## Spell Tier Icons & UI Items

**Spell Tier I–IV** — display icons shown in the loadout GUI, not held by players.

**ui_spellbook_bg / ui_spellbook_divider / ui_hideout_bg** — invisible GUI background panels.
