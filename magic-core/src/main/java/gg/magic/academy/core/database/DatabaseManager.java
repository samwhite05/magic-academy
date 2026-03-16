package gg.magic.academy.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.magic.academy.api.AcademyRank;
import gg.magic.academy.api.player.MagicPlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        FileConfiguration cfg = plugin.getConfig();
        String mode = cfg.getString("database.mode", "sqlite");

        HikariConfig hikari = new HikariConfig();

        if ("mysql".equalsIgnoreCase(mode)) {
            String host = cfg.getString("database.host", "localhost");
            int port = cfg.getInt("database.port", 3306);
            String db = cfg.getString("database.name", "magic_academy");
            String user = cfg.getString("database.user", "root");
            String pass = cfg.getString("database.password", "");
            hikari.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&characterEncoding=utf8");
            hikari.setUsername(user);
            hikari.setPassword(pass);
            hikari.setDriverClassName("com.mysql.cj.jdbc.Driver");
        } else {
            File dbFile = new File(plugin.getDataFolder(), "data.db");
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setDriverClassName("org.sqlite.JDBC");
            hikari.setMaximumPoolSize(1); // SQLite is single-writer
        }

        hikari.setPoolName("MagicAcademy-DB");
        dataSource = new HikariDataSource(hikari);

        createTables();
    }

    private void createTables() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    mana INTEGER NOT NULL DEFAULT 100,
                    max_mana INTEGER NOT NULL DEFAULT 100,
                    rank TEXT NOT NULL DEFAULT 'STUDENT'
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_spells (
                    uuid TEXT NOT NULL,
                    spell_id TEXT NOT NULL,
                    tier INTEGER NOT NULL DEFAULT 1,
                    PRIMARY KEY (uuid, spell_id)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_loadout (
                    uuid TEXT NOT NULL,
                    slot INTEGER NOT NULL,
                    spell_id TEXT,
                    PRIMARY KEY (uuid, slot)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_modules (
                    uuid TEXT NOT NULL,
                    module_id TEXT NOT NULL,
                    level INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, module_id)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_artifacts (
                    uuid TEXT NOT NULL,
                    artifact_id TEXT NOT NULL,
                    active INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid, artifact_id)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS spell_discoveries (
                    spell_id TEXT NOT NULL,
                    discoverer_uuid TEXT NOT NULL,
                    discoverer_name TEXT NOT NULL,
                    discovered_at INTEGER NOT NULL,
                    PRIMARY KEY (spell_id)
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dungeon_clears (
                    uuid TEXT NOT NULL,
                    dungeon_id TEXT NOT NULL,
                    cleared_at INTEGER NOT NULL,
                    duration_ms INTEGER NOT NULL
                )
            """);
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS crafting_rate_limit (
                    uuid TEXT NOT NULL,
                    window_start INTEGER NOT NULL,
                    count INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY (uuid)
                )
            """);
            createContractTables(stmt);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
        }
    }

    // ── Player persistence ────────────────────────────────────────────────────

    public MagicPlayerData loadPlayer(UUID uuid) {
        MagicPlayerData data = new MagicPlayerData(uuid);
        try (Connection conn = dataSource.getConnection()) {
            // Base stats
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT mana, max_mana, rank FROM players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    data.setMana(rs.getInt("mana"));
                    data.setMaxMana(rs.getInt("max_mana"));
                    data.setRank(AcademyRank.valueOf(rs.getString("rank")));
                }
            }
            // Spells
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT spell_id, tier FROM player_spells WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    data.grantSpell(rs.getString("spell_id"));
                    data.setSpellTier(rs.getString("spell_id"), rs.getInt("tier"));
                }
            }
            // Loadout
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT slot, spell_id FROM player_loadout WHERE uuid = ? ORDER BY slot")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String spellId = rs.getString("spell_id");
                    if (spellId != null) data.equipSpell(rs.getInt("slot"), spellId);
                }
            }
            // Modules
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT module_id, level FROM player_modules WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    data.setModuleLevel(rs.getString("module_id"), rs.getInt("level"));
                }
            }
            // Artifacts
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT artifact_id, active FROM player_artifacts WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    data.grantArtifact(rs.getString("artifact_id"));
                    if (rs.getInt("active") == 1) data.activateArtifact(rs.getString("artifact_id"));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player " + uuid, e);
        }
        return data;
    }

    public void savePlayer(MagicPlayerData data) {
        String uuid = data.getUuid().toString();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Upsert base row
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO players (uuid, mana, max_mana, rank) VALUES (?,?,?,?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET mana=excluded.mana, max_mana=excluded.max_mana, rank=excluded.rank")) {
                    ps.setString(1, uuid);
                    ps.setInt(2, data.getMana());
                    ps.setInt(3, data.getMaxMana());
                    ps.setString(4, data.getRank().name());
                    ps.executeUpdate();
                }
                // Spells
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_spells WHERE uuid=?")) {
                    del.setString(1, uuid); del.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_spells (uuid, spell_id, tier) VALUES (?,?,?)")) {
                    for (var entry : data.getSpellTiers().entrySet()) {
                        ps.setString(1, uuid);
                        ps.setString(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                // Loadout
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_loadout WHERE uuid=?")) {
                    del.setString(1, uuid); del.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_loadout (uuid, slot, spell_id) VALUES (?,?,?)")) {
                    List<String> loadout = data.getEquippedSpells();
                    for (int i = 0; i < loadout.size(); i++) {
                        ps.setString(1, uuid);
                        ps.setInt(2, i);
                        ps.setString(3, loadout.get(i));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                // Modules
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_modules WHERE uuid=?")) {
                    del.setString(1, uuid); del.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_modules (uuid, module_id, level) VALUES (?,?,?)")) {
                    for (var entry : data.getModuleLevels().entrySet()) {
                        ps.setString(1, uuid);
                        ps.setString(2, entry.getKey());
                        ps.setInt(3, entry.getValue());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                // Artifacts
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM player_artifacts WHERE uuid=?")) {
                    del.setString(1, uuid); del.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO player_artifacts (uuid, artifact_id, active) VALUES (?,?,?)")) {
                    for (String id : data.getOwnedArtifacts()) {
                        ps.setString(1, uuid);
                        ps.setString(2, id);
                        ps.setInt(3, data.getActiveArtifacts().contains(id) ? 1 : 0);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player " + uuid, e);
        }
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

    public boolean isDiscovered(String spellId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM spell_discoveries WHERE spell_id = ?")) {
            ps.setString(1, spellId);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Discovery check failed", e);
            return false;
        }
    }

    public void recordDiscovery(String spellId, UUID discoverer, String discovererName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR IGNORE INTO spell_discoveries (spell_id, discoverer_uuid, discoverer_name, discovered_at) VALUES (?,?,?,?)")) {
            ps.setString(1, spellId);
            ps.setString(2, discoverer.toString());
            ps.setString(3, discovererName);
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to record discovery", e);
        }
    }

    // ── Rate limiting for crafting ─────────────────────────────────────────────

    /** Returns true if allowed (and increments counter). Returns false if over limit. */
    public boolean checkCraftingRateLimit(UUID uuid, int maxPerMinute) {
        String id = uuid.toString();
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        try (Connection conn = dataSource.getConnection()) {
            // Clean stale window
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE crafting_rate_limit SET window_start=?, count=1 WHERE uuid=? AND window_start<?")) {
                ps.setLong(1, now); ps.setString(2, id); ps.setLong(3, windowStart);
                ps.executeUpdate();
            }
            // Insert if not present
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO crafting_rate_limit (uuid, window_start, count) VALUES (?,?,1)")) {
                ps.setString(1, id); ps.setLong(2, now);
                ps.executeUpdate();
            }
            // Check count
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT count FROM crafting_rate_limit WHERE uuid=?")) {
                ps.setString(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt("count") >= maxPerMinute) return false;
            }
            // Increment
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE crafting_rate_limit SET count=count+1 WHERE uuid=?")) {
                ps.setString(1, id); ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Rate limit check failed, allowing", e);
            return true;
        }
    }

    // ── Dungeon clears ────────────────────────────────────────────────────────

    public void recordDungeonClear(UUID uuid, String dungeonId, long durationMs) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO dungeon_clears (uuid, dungeon_id, cleared_at, duration_ms) VALUES (?,?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dungeonId);
            ps.setLong(3, System.currentTimeMillis());
            ps.setLong(4, durationMs);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to record dungeon clear", e);
        }
    }

    public int getDungeonClearCount(UUID uuid, String dungeonId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COUNT(*) FROM dungeon_clears WHERE uuid=? AND dungeon_id=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, dungeonId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to count dungeon clears", e);
            return 0;
        }
    }

    // ── Discoveries (all) ─────────────────────────────────────────────────────

    public record DiscoveryRecord(String spellId, String discovererName, long discoveredAt) {}

    public List<DiscoveryRecord> getAllDiscoveries() {
        List<DiscoveryRecord> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT spell_id, discoverer_name, discovered_at FROM spell_discoveries ORDER BY discovered_at ASC")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new DiscoveryRecord(rs.getString("spell_id"), rs.getString("discoverer_name"), rs.getLong("discovered_at")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load all discoveries", e);
        }
        return list;
    }

    // ── Contracts ─────────────────────────────────────────────────────────────

    private void createContractTables(Statement stmt) throws SQLException {
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_contracts (
                uuid TEXT NOT NULL,
                contract_id TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                started_at INTEGER NOT NULL,
                PRIMARY KEY (uuid, contract_id)
            )
        """);
        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS player_contract_progress (
                uuid TEXT NOT NULL,
                contract_id TEXT NOT NULL,
                obj_index INTEGER NOT NULL,
                progress INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (uuid, contract_id, obj_index)
            )
        """);
    }

    public void startContract(UUID uuid, String contractId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT OR IGNORE INTO player_contracts (uuid, contract_id, status, started_at) VALUES (?,?,'ACTIVE',?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, contractId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to start contract", e);
        }
    }

    public void completeContract(UUID uuid, String contractId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE player_contracts SET status='COMPLETED' WHERE uuid=? AND contract_id=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, contractId);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to complete contract", e);
        }
    }

    /** Returns a list of active contract IDs for this player. */
    public List<String> getActiveContracts(UUID uuid) {
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT contract_id FROM player_contracts WHERE uuid=? AND status='ACTIVE'")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getString("contract_id"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load active contracts", e);
        }
        return ids;
    }

    /** Returns a list of completed contract IDs for this player. */
    public List<String> getCompletedContracts(UUID uuid) {
        List<String> ids = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT contract_id FROM player_contracts WHERE uuid=? AND status='COMPLETED'")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getString("contract_id"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load completed contracts", e);
        }
        return ids;
    }

    /** Returns obj_index -> progress map for a given contract. */
    public Map<Integer, Integer> getContractProgress(UUID uuid, String contractId) {
        Map<Integer, Integer> map = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT obj_index, progress FROM player_contract_progress WHERE uuid=? AND contract_id=?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, contractId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) map.put(rs.getInt("obj_index"), rs.getInt("progress"));
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load contract progress", e);
        }
        return map;
    }

    public void setContractProgress(UUID uuid, String contractId, int objIndex, int progress) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO player_contract_progress (uuid, contract_id, obj_index, progress) VALUES (?,?,?,?) " +
                     "ON CONFLICT(uuid, contract_id, obj_index) DO UPDATE SET progress=excluded.progress")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, contractId);
            ps.setInt(3, objIndex);
            ps.setInt(4, progress);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update contract progress", e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    public HikariDataSource getDataSource() { return dataSource; }
}
