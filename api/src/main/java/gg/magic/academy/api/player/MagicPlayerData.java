package gg.magic.academy.api.player;

import gg.magic.academy.api.AcademyRank;

import java.util.*;

/**
 * All mutable player state for the Magic Academy.
 * Loaded from DB on join, saved on quit and periodically.
 */
public class MagicPlayerData {

    private final UUID uuid;
    private int mana;
    private int maxMana;
    private AcademyRank rank;

    /** spellId -> current tier (1-based). Only contains spells the player owns. */
    private final Map<String, Integer> spellTiers = new HashMap<>();

    /** Ordered list of equipped spell IDs. Max 4 slots. Null entries = empty slot. */
    private final List<String> equippedSpells = new ArrayList<>(Arrays.asList(null, null, null, null));

    /** moduleId -> current tier (0 = not built). */
    private final Map<String, Integer> moduleLevels = new HashMap<>();

    /** Artifact IDs in the player's possession. */
    private final Set<String> ownedArtifacts = new HashSet<>();

    /** Displayed (active) artifact IDs from the Artifact Display module. */
    private final Set<String> activeArtifacts = new HashSet<>();

    /** spellId -> System.currentTimeMillis() of last cast. */
    private final Map<String, Long> cooldowns = new HashMap<>();

    public MagicPlayerData(UUID uuid) {
        this.uuid = uuid;
        this.mana = 100;
        this.maxMana = 100;
        this.rank = AcademyRank.STUDENT;
    }

    // ── Mana ──────────────────────────────────────────────────────────────────

    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public void setMana(int mana) { this.mana = Math.max(0, Math.min(mana, maxMana)); }
    public void setMaxMana(int maxMana) { this.maxMana = Math.max(1, maxMana); }

    public boolean drainMana(int amount) {
        if (mana < amount) return false;
        mana -= amount;
        return true;
    }

    public void restoreMana(int amount) {
        mana = Math.min(mana + amount, maxMana);
    }

    // ── Rank ──────────────────────────────────────────────────────────────────

    public AcademyRank getRank() { return rank; }
    public void setRank(AcademyRank rank) { this.rank = rank; }

    // ── Spells ────────────────────────────────────────────────────────────────

    public boolean hasSpell(String spellId) { return spellTiers.containsKey(spellId); }
    public int getSpellTier(String spellId) { return spellTiers.getOrDefault(spellId, 0); }
    public void grantSpell(String spellId) { spellTiers.putIfAbsent(spellId, 1); }
    public void setSpellTier(String spellId, int tier) { spellTiers.put(spellId, tier); }
    public Map<String, Integer> getSpellTiers() { return Collections.unmodifiableMap(spellTiers); }

    // ── Loadout ───────────────────────────────────────────────────────────────

    public List<String> getEquippedSpells() { return Collections.unmodifiableList(equippedSpells); }

    public boolean equipSpell(int slot, String spellId) {
        if (slot < 0 || slot >= 4) return false;
        if (!hasSpell(spellId)) return false;
        equippedSpells.set(slot, spellId);
        return true;
    }

    public void clearSlot(int slot) {
        if (slot >= 0 && slot < 4) equippedSpells.set(slot, null);
    }

    // ── Cooldowns ─────────────────────────────────────────────────────────────

    public boolean isOnCooldown(String spellId, long cooldownMs) {
        Long last = cooldowns.get(spellId);
        if (last == null) return false;
        return System.currentTimeMillis() - last < cooldownMs;
    }

    public long getCooldownRemainingMs(String spellId, long cooldownMs) {
        Long last = cooldowns.get(spellId);
        if (last == null) return 0L;
        long remaining = cooldownMs - (System.currentTimeMillis() - last);
        return Math.max(0L, remaining);
    }

    public void setCooldown(String spellId) {
        cooldowns.put(spellId, System.currentTimeMillis());
    }

    // ── Hideout Modules ───────────────────────────────────────────────────────

    public int getModuleLevel(String moduleId) { return moduleLevels.getOrDefault(moduleId, 0); }
    public void setModuleLevel(String moduleId, int level) { moduleLevels.put(moduleId, level); }
    public Map<String, Integer> getModuleLevels() { return Collections.unmodifiableMap(moduleLevels); }

    // ── Artifacts ─────────────────────────────────────────────────────────────

    public boolean hasArtifact(String artifactId) { return ownedArtifacts.contains(artifactId); }
    public void grantArtifact(String artifactId) { ownedArtifacts.add(artifactId); }
    public Set<String> getOwnedArtifacts() { return Collections.unmodifiableSet(ownedArtifacts); }

    public Set<String> getActiveArtifacts() { return Collections.unmodifiableSet(activeArtifacts); }
    public void activateArtifact(String artifactId) { activeArtifacts.add(artifactId); }
    public void deactivateArtifact(String artifactId) { activeArtifacts.remove(artifactId); }

    // ── Identity ──────────────────────────────────────────────────────────────

    public UUID getUuid() { return uuid; }
}
