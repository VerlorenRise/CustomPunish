/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.projectn4.punish.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import ru.projectn4.punish.model.PlayerData;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.model.PunishmentType;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final File databaseFile;
    private final ExecutorService asyncExecutor;
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.databaseFile = new File(plugin.getDataFolder(), "punishments.db");
        this.asyncExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "PunishPlugin-DB-Worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void init() throws SQLException {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        try {
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Не найден драйвер org.sqlite.JDBC!", e);
        }
        this.getConnection();
        try (Statement statement = this.connection.createStatement();){
            statement.execute("PRAGMA journal_mode = WAL;");
            statement.execute("PRAGMA synchronous = NORMAL;");
            statement.execute("    CREATE TABLE IF NOT EXISTS punishments (\n        id INTEGER PRIMARY KEY AUTOINCREMENT,\n        target_uuid VARCHAR(36),\n        target_name VARCHAR(64) NOT NULL,\n        target_ip VARCHAR(45),\n        punish_type VARCHAR(16) NOT NULL,\n        operator_uuid VARCHAR(36) NOT NULL,\n        operator_name VARCHAR(64) NOT NULL,\n        reason TEXT NOT NULL,\n        created_at BIGINT NOT NULL,\n        expires_at BIGINT NOT NULL,\n        active INTEGER NOT NULL DEFAULT 1,\n        unpunished_by_uuid VARCHAR(36),\n        unpunished_by_name VARCHAR(64),\n        unpunish_reason TEXT,\n        unpunished_at BIGINT\n    );\n");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punish_target_uuid ON punishments (target_uuid);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punish_target_name ON punishments (target_name);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punish_target_ip ON punishments (target_ip);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_punish_active_type ON punishments (active, punish_type);");
            statement.execute("    CREATE TABLE IF NOT EXISTS player_data (\n        uuid VARCHAR(36) PRIMARY KEY,\n        name VARCHAR(64) NOT NULL,\n        last_ip VARCHAR(45) NOT NULL,\n        last_seen BIGINT NOT NULL\n    );\n");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_data_name ON player_data (name);");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_player_data_ip ON player_data (last_ip);");
        }
        this.plugin.getLogger().info("База данных SQLite успешно подключена и инициализирована.");
    }

    private synchronized Connection getConnection() throws SQLException {
        if (this.connection == null || this.connection.isClosed()) {
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
        }
        return this.connection;
    }

    public void close() {
        this.asyncExecutor.shutdown();
        if (this.connection != null) {
            try {
                if (!this.connection.isClosed()) {
                    this.connection.close();
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.WARNING, "Ошибка при закрытии базы данных", e);
            }
        }
    }

    public CompletableFuture<Void> savePlayerData(UUID uuid, String name, String ip) {
        return CompletableFuture.runAsync(() -> {
            String sql = "    INSERT INTO player_data (uuid, name, last_ip, last_seen)\n    VALUES (?, ?, ?, ?)\n    ON CONFLICT(uuid) DO UPDATE SET\n        name = excluded.name,\n        last_ip = excluded.last_ip,\n        last_seen = excluded.last_seen;\n";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setString(3, ip);
                ps.setLong(4, System.currentTimeMillis());
                ps.executeUpdate();
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка сохранения данных игрока: " + name, e);
            }
        }, this.asyncExecutor);
    }

    public CompletableFuture<Optional<PlayerData>> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE uuid = ? LIMIT 1;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return Optional.empty();
                    Optional<PlayerData> optional = Optional.of(this.mapPlayerData(rs));
                    return optional;
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка получения данных игрока: " + String.valueOf(uuid), e);
            }
            return Optional.empty();
        }, this.asyncExecutor);
    }

    public CompletableFuture<Optional<PlayerData>> getPlayerDataByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM player_data WHERE LOWER(name) = LOWER(?) ORDER BY last_seen DESC LIMIT 1;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                ps.setString(1, name);
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return Optional.empty();
                    Optional<PlayerData> optional = Optional.of(this.mapPlayerData(rs));
                    return optional;
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка получения данных игрока по имени: " + name, e);
            }
            return Optional.empty();
        }, this.asyncExecutor);
    }

    public CompletableFuture<Punishment> insertPunishment(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "    INSERT INTO punishments (\n        target_uuid, target_name, target_ip, punish_type,\n        operator_uuid, operator_name, reason, created_at,\n        expires_at, active\n    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1);\n";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql, 1)) {
                ps.setString(1, punishment.getTargetUuid() != null ? punishment.getTargetUuid().toString() : null);
                ps.setString(2, punishment.getTargetName());
                ps.setString(3, punishment.getTargetIp());
                ps.setString(4, punishment.getType().name());
                ps.setString(5, punishment.getOperatorUuid().toString());
                ps.setString(6, punishment.getOperatorName());
                ps.setString(7, punishment.getReason());
                ps.setLong(8, punishment.getCreatedAt());
                ps.setLong(9, punishment.getExpiresAt());
                ps.executeUpdate();
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        punishment.setId(generatedKeys.getInt(1));
                    }
                }
                return punishment;
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка добавления наказания в базу данных", e);
                throw new RuntimeException(e);
            }
        }, this.asyncExecutor);
    }

    public CompletableFuture<Optional<Punishment>> getActivePunishmentByUuid(UUID targetUuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE target_uuid = ? AND punish_type = ? AND active = 1 ORDER BY id DESC LIMIT 1;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                ps.setString(1, targetUuid.toString());
                ps.setString(2, type.name());
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return Optional.empty();
                    Punishment p = this.mapPunishment(rs);
                    if (p.isExpired()) {
                        this.deactivateInternal(p.getId(), null, "Консоль", "Истечение срока");
                        Optional optional = Optional.empty();
                        return optional;
                    }
                    Optional<Punishment> optional = Optional.of(p);
                    return optional;
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка получения активного наказания", e);
            }
            return Optional.empty();
        }, this.asyncExecutor);
    }

    public CompletableFuture<Optional<Punishment>> getActivePunishmentByName(String targetName, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE LOWER(target_name) = LOWER(?) AND punish_type = ? AND active = 1 ORDER BY id DESC LIMIT 1;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                ps.setString(1, targetName);
                ps.setString(2, type.name());
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return Optional.empty();
                    Punishment p = this.mapPunishment(rs);
                    if (p.isExpired()) {
                        this.deactivateInternal(p.getId(), null, "Консоль", "Истечение срока");
                        Optional optional = Optional.empty();
                        return optional;
                    }
                    Optional<Punishment> optional = Optional.of(p);
                    return optional;
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка получения наказания по никнейму", e);
            }
            return Optional.empty();
        }, this.asyncExecutor);
    }

    public CompletableFuture<Optional<Punishment>> getActiveBanByIp(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM punishments WHERE target_ip = ? AND punish_type = ? AND active = 1 ORDER BY id DESC LIMIT 1;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                ps.setString(1, ip);
                ps.setString(2, PunishmentType.BAN_IP.name());
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return Optional.empty();
                    Punishment p = this.mapPunishment(rs);
                    if (p.isExpired()) {
                        this.deactivateInternal(p.getId(), null, "Консоль", "Истечение срока");
                        Optional optional = Optional.empty();
                        return optional;
                    }
                    Optional<Punishment> optional = Optional.of(p);
                    return optional;
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка проверки IP-бана: " + ip, e);
            }
            return Optional.empty();
        }, this.asyncExecutor);
    }

    public CompletableFuture<Boolean> deactivateActivePunishments(UUID targetUuid, String targetName, PunishmentType type, UUID unpunisherUuid, String unpunisherName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = targetUuid != null ? "    UPDATE punishments\n    SET active = 0,\n        unpunished_by_uuid = ?,\n        unpunished_by_name = ?,\n        unpunish_reason = ?,\n        unpunished_at = ?\n    WHERE target_uuid = ? AND punish_type = ? AND active = 1;\n" : "    UPDATE punishments\n    SET active = 0,\n        unpunished_by_uuid = ?,\n        unpunished_by_name = ?,\n        unpunish_reason = ?,\n        unpunished_at = ?\n    WHERE LOWER(target_name) = LOWER(?) AND punish_type = ? AND active = 1;\n";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql)) {
                ps.setString(1, unpunisherUuid != null ? unpunisherUuid.toString() : null);
                ps.setString(2, unpunisherName);
                ps.setString(3, reason);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, targetUuid != null ? targetUuid.toString() : targetName);
                ps.setString(6, type.name());
                int updated = ps.executeUpdate();
                return updated > 0;
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка деактивации наказания", e);
                return false;
            }
        }, this.asyncExecutor);
    }

    public CompletableFuture<Boolean> deactivateActiveIpBans(String ip, UUID unpunisherUuid, String unpunisherName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "    UPDATE punishments\n    SET active = 0,\n        unpunished_by_uuid = ?,\n        unpunished_by_name = ?,\n        unpunish_reason = ?,\n        unpunished_at = ?\n    WHERE target_ip = ? AND punish_type = 'BAN_IP' AND active = 1;\n";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql)) {
                ps.setString(1, unpunisherUuid != null ? unpunisherUuid.toString() : null);
                ps.setString(2, unpunisherName);
                ps.setString(3, reason);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, ip);
                int updated = ps.executeUpdate();
                return updated > 0;
            } catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка деактивации IP-бана: " + ip, e);
                return false;
            }
        }, this.asyncExecutor);
    }

    private void deactivateInternal(int id, UUID unpunisherUuid, String unpunisherName, String reason) {
        String sql = "    UPDATE punishments\n    SET active = 0,\n        unpunished_by_uuid = ?,\n        unpunished_by_name = ?,\n        unpunish_reason = ?,\n        unpunished_at = ?\n    WHERE id = ?;\n";
        try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
            ps.setString(1, unpunisherUuid != null ? unpunisherUuid.toString() : null);
            ps.setString(2, unpunisherName);
            ps.setString(3, reason);
            ps.setLong(4, System.currentTimeMillis());
            ps.setInt(5, id);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Ошибка деактивации наказания ID " + id, e);
        }
    }

    public CompletableFuture<List<Punishment>> loadAllActiveMutes() {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Punishment> list = new ArrayList<Punishment>();
            String sql = "SELECT * FROM punishments WHERE punish_type = 'MUTE' AND active = 1;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);
                 ResultSet rs = ps.executeQuery();){
                while (rs.next()) {
                    Punishment p = this.mapPunishment(rs);
                    if (p.isExpired()) {
                        this.deactivateInternal(p.getId(), null, "Консоль", "Истечение срока");
                        continue;
                    }
                    list.add(p);
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка загрузки активных мутов", e);
            }
            return list;
        }, this.asyncExecutor);
    }

    public CompletableFuture<List<Punishment>> getHistory(String targetName, UUID targetUuid, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            ArrayList<Punishment> list = new ArrayList<Punishment>();
            String sql = targetUuid != null ? "SELECT * FROM punishments WHERE target_uuid = ? OR LOWER(target_name) = LOWER(?) ORDER BY id DESC LIMIT ? OFFSET ?;" : "SELECT * FROM punishments WHERE LOWER(target_name) = LOWER(?) ORDER BY id DESC LIMIT ? OFFSET ?;";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                if (targetUuid != null) {
                    ps.setString(1, targetUuid.toString());
                    ps.setString(2, targetName);
                    ps.setInt(3, limit);
                    ps.setInt(4, offset);
                } else {
                    ps.setString(1, targetName);
                    ps.setInt(2, limit);
                    ps.setInt(3, offset);
                }
                try (ResultSet rs = ps.executeQuery();){
                    while (rs.next()) {
                        list.add(this.mapPunishment(rs));
                    }
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка получения истории наказаний: " + targetName, e);
            }
            return list;
        }, this.asyncExecutor);
    }

    public CompletableFuture<Integer> getHistoryCount(String targetName, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = targetUuid != null ? "SELECT COUNT(*) FROM punishments WHERE target_uuid = ? OR LOWER(target_name) = LOWER(?);" : "SELECT COUNT(*) FROM punishments WHERE LOWER(target_name) = LOWER(?);";
            try (PreparedStatement ps = this.getConnection().prepareStatement(sql);){
                if (targetUuid != null) {
                    ps.setString(1, targetUuid.toString());
                    ps.setString(2, targetName);
                } else {
                    ps.setString(1, targetName);
                }
                try (ResultSet rs = ps.executeQuery();){
                    if (!rs.next()) return 0;
                    Integer n = rs.getInt(1);
                    return n;
                }
            }
            catch (SQLException e) {
                this.plugin.getLogger().log(Level.SEVERE, "Ошибка подсчета истории наказаний: " + targetName, e);
            }
            return 0;
        }, this.asyncExecutor);
    }

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String uuidStr = rs.getString("target_uuid");
        UUID targetUuid = uuidStr != null ? UUID.fromString(uuidStr) : null;
        String targetName = rs.getString("target_name");
        String targetIp = rs.getString("target_ip");
        PunishmentType type = PunishmentType.valueOf(rs.getString("punish_type"));
        UUID operatorUuid = UUID.fromString(rs.getString("operator_uuid"));
        String operatorName = rs.getString("operator_name");
        String reason = rs.getString("reason");
        long createdAt = rs.getLong("created_at");
        long expiresAt = rs.getLong("expires_at");
        boolean active = rs.getInt("active") == 1;
        String unpunishedByUuidStr = rs.getString("unpunished_by_uuid");
        UUID unpunishedByUuid = unpunishedByUuidStr != null ? UUID.fromString(unpunishedByUuidStr) : null;
        String unpunishedByName = rs.getString("unpunished_by_name");
        String unpunishReason = rs.getString("unpunish_reason");
        long unpunishedAtVal = rs.getLong("unpunished_at");
        Long unpunishedAt = rs.wasNull() ? null : Long.valueOf(unpunishedAtVal);
        return new Punishment(id, targetUuid, targetName, targetIp, type, operatorUuid, operatorName, reason, createdAt, expiresAt, active, unpunishedByUuid, unpunishedByName, unpunishReason, unpunishedAt);
    }

    private PlayerData mapPlayerData(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");
        String lastIp = rs.getString("last_ip");
        long lastSeen = rs.getLong("last_seen");
        return new PlayerData(uuid, name, lastIp, lastSeen);
    }
}

