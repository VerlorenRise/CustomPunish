/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.projectn4.punish.service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.database.DatabaseManager;
import ru.projectn4.punish.model.PlayerData;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.model.PunishmentType;
import ru.projectn4.punish.util.MessageUtil;
import ru.projectn4.punish.util.TimeUtil;

public class PunishmentService {
    public static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final Pattern IP_PATTERN = Pattern.compile("^(\\d{1,3}\\.){3}\\d{1,3}$");
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final ConfigManager configManager;
    private final Map<UUID, Punishment> activeMutesByUuid = new ConcurrentHashMap<UUID, Punishment>();
    private final Map<String, Punishment> activeMutesByName = new ConcurrentHashMap<String, Punishment>();

    public static final long WARN_DURATION_MILLIS = 30L * 24L * 60L * 60L * 1000L;

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public PunishmentService(JavaPlugin plugin, DatabaseManager databaseManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.configManager = configManager;
    }

    public void loadActiveMutes() {
        this.activeMutesByUuid.clear();
        this.activeMutesByName.clear();
        this.databaseManager.loadAllActiveMutes().thenAccept(list -> {
            for (Punishment p : list) {
                if (p.getTargetUuid() != null) {
                    this.activeMutesByUuid.put(p.getTargetUuid(), p);
                }
                if (p.getTargetName() == null) continue;
                this.activeMutesByName.put(p.getTargetName().toLowerCase(Locale.ROOT), p);
            }
            this.plugin.getLogger().info("Загружено активных мутов в кеш: " + list.size());
        });
    }

    public void cacheMute(Punishment p) {
        if (p == null) {
            return;
        }
        if (p.getTargetUuid() != null) {
            this.activeMutesByUuid.put(p.getTargetUuid(), p);
        }
        if (p.getTargetName() != null) {
            this.activeMutesByName.put(p.getTargetName().toLowerCase(Locale.ROOT), p);
        }
    }

    public Optional<Punishment> getActiveMute(UUID uuid, String name) {
        Punishment mute = null;
        if (uuid != null) {
            mute = this.activeMutesByUuid.get(uuid);
        }
        if (mute == null && name != null) {
            mute = this.activeMutesByName.get(name.toLowerCase(Locale.ROOT));
        }
        if (mute == null) {
            try {
                Punishment p;
                Optional<Punishment> dbMute = Optional.empty();
                if (uuid != null) {
                    dbMute = this.databaseManager.getActivePunishmentByUuid(uuid, PunishmentType.MUTE).join();
                }
                if (dbMute.isEmpty() && name != null) {
                    dbMute = this.databaseManager.getActivePunishmentByName(name, PunishmentType.MUTE).join();
                }
                if (dbMute.isPresent() && !(p = (Punishment)dbMute.get()).isExpired()) {
                    this.cacheMute(p);
                    return dbMute;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            return Optional.empty();
        }
        if (mute.isExpired()) {
            if (uuid != null) {
                this.activeMutesByUuid.remove(uuid);
            }
            if (name != null) {
                this.activeMutesByName.remove(name.toLowerCase(Locale.ROOT));
            }
            this.databaseManager.deactivateActivePunishments(uuid, name, PunishmentType.MUTE, CONSOLE_UUID, "Консоль", "Истечение срока");
            return Optional.empty();
        }
        return Optional.of(mute);
    }

    public void handleMutedChat(Player player, Punishment mute, Collection<? extends CommandSender> recipients) {
        player.sendMessage(this.buildMuteBlockedMessage(mute));
        String attemptMsg = this.configManager.getMessage("chat-mute-attempt", "&8* {player} пытается что-то сказать *").replace("{player}", player.getName());
        Component comp = MessageUtil.parse(attemptMsg);
        boolean sent = false;
        if (recipients != null && !recipients.isEmpty()) {
            for (CommandSender commandSender : recipients) {
                if (commandSender == null || commandSender.equals((Object)player)) continue;
                commandSender.sendMessage(comp);
                sent = true;
            }
        }
        if (!sent) {
            double radius = this.configManager.getLocalChatRadius();
            double radiusSq = radius * radius;
            for (Player nearby : player.getWorld().getPlayers()) {
                if (nearby.equals((Object)player) || !(nearby.getLocation().distanceSquared(player.getLocation()) <= radiusSq)) continue;
                nearby.sendMessage(comp);
            }
        }
    }

    public CompletableFuture<Boolean> ban(CommandSender operator, String targetName, UUID targetUuid, String targetIp, long durationMillis, String reason, boolean silent) {
        if (targetUuid == null) {
            return this.databaseManager.getPlayerDataByName(targetName).thenCompose(optData -> {
                UUID resolvedUuid = optData.map(PlayerData::getUuid).orElse(null);
                String resolvedIp = targetIp != null ? targetIp : optData.map(PlayerData::getLastIp).orElse(null);
                return this.executeBan(operator, targetName, resolvedUuid, resolvedIp, durationMillis, reason, silent);
            });
        }
        return this.executeBan(operator, targetName, targetUuid, targetIp, durationMillis, reason, silent);
    }

    private CompletableFuture<Boolean> executeBan(CommandSender operator, String targetName, UUID targetUuid, String targetIp, long durationMillis, String reason, boolean silent) {
        UUID opUuid = (operator instanceof Player) ? ((Player) operator).getUniqueId() : CONSOLE_UUID;
        String opName = operator.getName();
        long now = System.currentTimeMillis();
        long expiresAt = durationMillis <= 0L ? -1L : now + durationMillis;
        Punishment punishment = new Punishment(targetUuid, targetName, targetIp, PunishmentType.BAN, opUuid, opName, reason, now, expiresAt);
        return this.databaseManager.insertPunishment(punishment).thenApply(inserted -> {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                Player online = targetUuid != null ? Bukkit.getPlayer((UUID)targetUuid) : Bukkit.getPlayerExact((String)targetName);
                if (online != null && online.isOnline()) {
                    online.kick(this.buildBanScreen((Punishment)inserted));
                }
            });
            this.broadcastPunishment((Punishment)inserted, silent);
            return true;
        });
    }

    public CompletableFuture<Boolean> banIp(CommandSender operator, String targetInput, long durationMillis, String reason, boolean silent) {
        long expiresAt;
        UUID uUID;
        if (operator instanceof Player) {
            Player p = (Player)operator;
            uUID = p.getUniqueId();
        } else {
            uUID = CONSOLE_UUID;
        }
        UUID opUuid = uUID;
        String opName = operator.getName();
        long now = System.currentTimeMillis();
        long l = expiresAt = durationMillis <= 0L ? -1L : now + durationMillis;
        if (IP_PATTERN.matcher(targetInput).matches()) {
            Punishment punishment = new Punishment(null, targetInput, targetInput, PunishmentType.BAN_IP, opUuid, opName, reason, now, expiresAt);
            return this.databaseManager.insertPunishment(punishment).thenApply(inserted -> {
                this.kickAllWithIp(targetInput, this.buildBanScreen((Punishment)inserted));
                this.broadcastPunishment((Punishment)inserted, silent);
                return true;
            });
        }
        Player online = Bukkit.getPlayerExact((String)targetInput);
        if (online != null && online.getAddress() != null) {
            String ip = online.getAddress().getAddress().getHostAddress();
            Punishment punishment = new Punishment(online.getUniqueId(), online.getName(), ip, PunishmentType.BAN_IP, opUuid, opName, reason, now, expiresAt);
            return this.databaseManager.insertPunishment(punishment).thenApply(inserted -> {
                this.kickAllWithIp(ip, this.buildBanScreen((Punishment)inserted));
                this.broadcastPunishment((Punishment)inserted, silent);
                return true;
            });
        }
        return this.databaseManager.getPlayerDataByName(targetInput).thenCompose(optData -> {
            if (optData.isEmpty()) {
                MessageUtil.sendMessage(operator, this.configManager.getMessage("error.player-data-not-found", "&cИгрок &e{player} &cни разу не заходил на сервер, его IP неизвестен!").replace("{player}", targetInput));
                return CompletableFuture.completedFuture(false);
            }
            PlayerData data = (PlayerData)optData.get();
            Punishment punishment = new Punishment(data.getUuid(), data.getName(), data.getLastIp(), PunishmentType.BAN_IP, opUuid, opName, reason, now, expiresAt);
            return this.databaseManager.insertPunishment(punishment).thenApply(inserted -> {
                this.kickAllWithIp(data.getLastIp(), this.buildBanScreen((Punishment)inserted));
                this.broadcastPunishment((Punishment)inserted, silent);
                return true;
            });
        });
    }

    public CompletableFuture<Boolean> mute(CommandSender operator, String targetName, UUID targetUuid, long durationMillis, String reason, boolean silent) {
        if (targetUuid == null) {
            return this.databaseManager.getPlayerDataByName(targetName).thenCompose(optData -> {
                UUID resolvedUuid = optData.map(PlayerData::getUuid).orElse(null);
                return this.executeMute(operator, targetName, resolvedUuid, durationMillis, reason, silent);
            });
        }
        return this.executeMute(operator, targetName, targetUuid, durationMillis, reason, silent);
    }

    private CompletableFuture<Boolean> executeMute(CommandSender operator, String targetName, UUID targetUuid, long durationMillis, String reason, boolean silent) {
        UUID opUuid = (operator instanceof Player) ? ((Player) operator).getUniqueId() : CONSOLE_UUID;
        String opName = operator.getName();
        long now = System.currentTimeMillis();
        long expiresAt = durationMillis <= 0L ? -1L : now + durationMillis;
        Punishment punishment = new Punishment(targetUuid, targetName, null, PunishmentType.MUTE, opUuid, opName, reason, now, expiresAt);
        return this.databaseManager.insertPunishment(punishment).thenApply(inserted -> {
            if (targetUuid != null) {
                this.activeMutesByUuid.put(targetUuid, (Punishment)inserted);
            }
            this.activeMutesByName.put(targetName.toLowerCase(Locale.ROOT), (Punishment)inserted);
            Player online = targetUuid != null ? Bukkit.getPlayer((UUID)targetUuid) : Bukkit.getPlayerExact((String)targetName);
            if (online != null && online.isOnline()) {
                this.sendMuteNotification(online, (Punishment)inserted);
            }
            this.broadcastPunishment((Punishment)inserted, silent);
            return true;
        });
    }

    public CompletableFuture<Boolean> kick(CommandSender operator, Player target, String reason, boolean silent) {
        UUID uUID;
        if (operator instanceof Player) {
            Player p = (Player)operator;
            uUID = p.getUniqueId();
        } else {
            uUID = CONSOLE_UUID;
        }
        UUID opUuid = uUID;
        String opName = operator.getName();
        long now = System.currentTimeMillis();
        Punishment punishment = new Punishment(target.getUniqueId(), target.getName(), target.getAddress() != null ? target.getAddress().getAddress().getHostAddress() : null, PunishmentType.KICK, opUuid, opName, reason, now, 0L);
        punishment.setActive(false);
        return this.databaseManager.insertPunishment(punishment).thenApply(inserted -> {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (target.isOnline()) {
                    target.kick(this.buildKickScreen((Punishment)inserted));
                }
            });
            this.broadcastPunishment((Punishment)inserted, silent);
            return true;
        });
    }

    public CompletableFuture<Boolean> unban(CommandSender operator, String targetName, String reason) {
        UUID uUID;
        if (operator instanceof Player) {
            Player p = (Player)operator;
            uUID = p.getUniqueId();
        } else {
            uUID = CONSOLE_UUID;
        }
        UUID opUuid = uUID;
        String opName = operator.getName();
        return this.databaseManager.getPlayerDataByName(targetName).thenCompose(optData -> {
            UUID targetUuid = optData.map(PlayerData::getUuid).orElse(null);
            return this.databaseManager.deactivateActivePunishments(targetUuid, targetName, PunishmentType.BAN, opUuid, opName, reason).thenApply(unbanned -> {
                if (unbanned.booleanValue()) {
                    String msg = this.configManager.getMessage("broadcast.unban", "&a[Наказания] &b{operator} &fразблокировал игрока &e{player}&f. Причина: &7{reason}").replace("{operator}", opName).replace("{player}", targetName).replace("{reason}", reason);
                    Bukkit.broadcast((Component)MessageUtil.parse(msg));
                }
                return unbanned;
            });
        });
    }

    public CompletableFuture<Boolean> unbanIp(CommandSender operator, String ip, String reason) {
        UUID uUID;
        if (operator instanceof Player) {
            Player p = (Player)operator;
            uUID = p.getUniqueId();
        } else {
            uUID = CONSOLE_UUID;
        }
        UUID opUuid = uUID;
        String opName = operator.getName();
        return this.databaseManager.deactivateActiveIpBans(ip, opUuid, opName, reason).thenApply(unbanned -> {
            if (unbanned.booleanValue()) {
                String msg = this.configManager.getMessage("broadcast.unbanip", "&a[Наказания] &b{operator} &fразблокировал IP &e{ip}&f. Причина: &7{reason}").replace("{operator}", opName).replace("{ip}", ip).replace("{reason}", reason);
                Bukkit.broadcast((Component)MessageUtil.parse(msg));
            }
            return unbanned;
        });
    }

    public CompletableFuture<Boolean> unmute(CommandSender operator, String targetName, String reason) {
        UUID uUID;
        if (operator instanceof Player) {
            Player p = (Player)operator;
            uUID = p.getUniqueId();
        } else {
            uUID = CONSOLE_UUID;
        }
        UUID opUuid = uUID;
        String opName = operator.getName();
        Player online = Bukkit.getPlayerExact((String)targetName);
        UUID targetUuid = online != null ? online.getUniqueId() : null;
        return this.databaseManager.deactivateActivePunishments(targetUuid, targetName, PunishmentType.MUTE, opUuid, opName, reason).thenApply(unmuted -> {
            if (unmuted.booleanValue()) {
                if (targetUuid != null) {
                    this.activeMutesByUuid.remove(targetUuid);
                }
                this.activeMutesByName.remove(targetName.toLowerCase(Locale.ROOT));
                if (online != null && online.isOnline()) {
                    String notify = this.configManager.getMessage("notification.unmuted", "&aС вас был снят мут! Теперь вы можете общаться в чате.");
                    MessageUtil.sendMessage((CommandSender)online, notify);
                }
                String msg = this.configManager.getMessage("broadcast.unmute", "&a[Наказания] &b{operator} &fснял мут с игрока &e{player}&f. Причина: &7{reason}").replace("{operator}", opName).replace("{player}", targetName).replace("{reason}", reason);
                Bukkit.broadcast((Component)MessageUtil.parse(msg));
            }
            return unmuted;
        });
    }

    public CompletableFuture<Boolean> warn(CommandSender operator, String targetName, UUID targetUuid, String reason, boolean silent) {
        if (targetUuid == null) {
            return this.databaseManager.getPlayerDataByName(targetName).thenCompose(optData -> {
                UUID resolvedUuid = optData.map(PlayerData::getUuid).orElse(null);
                return this.executeWarn(operator, targetName, resolvedUuid, reason, silent);
            });
        }
        return this.executeWarn(operator, targetName, targetUuid, reason, silent);
    }

    private CompletableFuture<Boolean> executeWarn(CommandSender operator, String targetName, UUID targetUuid, String reason, boolean silent) {
        UUID opUuid = (operator instanceof Player) ? ((Player) operator).getUniqueId() : CONSOLE_UUID;
        String opName = operator.getName();
        long now = System.currentTimeMillis();
        long expiresAt = now + WARN_DURATION_MILLIS;

        Punishment punishment = new Punishment(targetUuid, targetName, null, PunishmentType.WARN, opUuid, opName, reason, now, expiresAt);
        return this.databaseManager.insertPunishment(punishment).thenCompose(inserted -> {
            return this.databaseManager.getActiveWarnsCount(targetUuid, targetName).thenCompose(warnCount -> {
                Player online = targetUuid != null ? Bukkit.getPlayer(targetUuid) : Bukkit.getPlayerExact(targetName);
                if (online != null && online.isOnline()) {
                    String playerMsg = this.configManager.getMessage("notification.warn-received-chat",
                        "&cВы получили варн &e[{count}/3]&c от &b{operator}&c. Причина: &f{reason}")
                        .replace("{count}", String.valueOf(warnCount))
                        .replace("{operator}", opName)
                        .replace("{reason}", reason);
                    online.sendMessage(MessageUtil.parse(playerMsg));
                }

                this.broadcastWarn(punishment, warnCount, silent);

                if (warnCount >= 3) {
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        String autoBanReason = "Превышен лимит предупреждений (3/3): " + reason;
                        String targetIp = (online != null && online.getAddress() != null) ?
                            online.getAddress().getAddress().getHostAddress() : null;

                        this.ban(operator, targetName, targetUuid, targetIp, -1L, autoBanReason, false).thenAccept(banned -> {
                            this.databaseManager.deactivateAllActiveWarns(targetUuid, targetName, CONSOLE_UUID, "Консоль", "Погашено баном (3/3 варнов)");
                        });
                    });
                }
                return CompletableFuture.completedFuture(true);
            });
        });
    }

    public CompletableFuture<Boolean> unwarn(CommandSender operator, String targetName, Integer warnId, String reason) {
        UUID opUuid = (operator instanceof Player) ? ((Player) operator).getUniqueId() : CONSOLE_UUID;
        String opName = operator.getName();

        return this.databaseManager.getPlayerDataByName(targetName).thenCompose(optData -> {
            UUID targetUuid = optData.map(PlayerData::getUuid).orElse(null);
            CompletableFuture<Boolean> future = (warnId != null) ?
                this.databaseManager.deactivatePunishmentById(warnId, opUuid, opName, reason) :
                this.databaseManager.deactivateLastWarn(targetUuid, targetName, opUuid, opName, reason);

            return future.thenApply(unwarned -> {
                if (unwarned) {
                    String msg = this.configManager.getMessage("broadcast.unwarn",
                        "&a[Наказания] &b{operator} &fснял варн с игрока &e{player}&f. Причина: &7{reason}")
                        .replace("{operator}", opName)
                        .replace("{player}", targetName)
                        .replace("{reason}", reason);
                    Bukkit.broadcast(MessageUtil.parse(msg));

                    Player online = targetUuid != null ? Bukkit.getPlayer(targetUuid) : Bukkit.getPlayerExact(targetName);
                    if (online != null && online.isOnline()) {
                        String notify = this.configManager.getMessage("notification.unwarned",
                            "&aС вас было снято предупреждение. Причина: &7{reason}")
                            .replace("{reason}", reason);
                        online.sendMessage(MessageUtil.parse(notify));
                    }
                }
                return unwarned;
            });
        });
    }

    private void broadcastWarn(Punishment punishment, int count, boolean silent) {
        String templateKey = silent ? "broadcast.silent.warn" : "broadcast.warn";
        String def = silent ?
            "&7[Тихо] &e{player} &7получил варн &e[{count}/3]&7 от &b{operator}&7. Причина: &f{reason}" :
            "&c[Наказания] &e{player} &fполучил варн &e[{count}/3]&f от модератора &b{operator}&f. Причина: &e{reason}";
        String template = this.configManager.getMessage(templateKey, def)
            .replace("{count}", String.valueOf(count));
        String formatted = this.applyPlaceholders(template, punishment, "30 дн.");
        Component comp = MessageUtil.parse(formatted);

        if (silent) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("punish.notifications")) continue;
                player.sendMessage(comp);
            }
            Bukkit.getConsoleSender().sendMessage(comp);
        } else {
            Bukkit.broadcast(comp);
        }
    }

    private void kickAllWithIp(String ip, Component reasonComponent) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getAddress() == null || !ip.equals(p.getAddress().getAddress().getHostAddress())) continue;
                p.kick(reasonComponent);
            }
        });
    }

    private void broadcastPunishment(Punishment punishment, boolean silent) {
        String typeKey = punishment.getType().name().toLowerCase(Locale.ROOT).replace("_", "");
        String durationStr = TimeUtil.formatDuration(punishment.getExpiresAt() <= 0L ? -1L : punishment.getExpiresAt() - punishment.getCreatedAt());
        if (silent) {
            String template = this.configManager.getMessage("broadcast.silent." + typeKey, "&7[Тихо] &e{player} &7был {action} модератором &b{operator}&7. Причина: &f{reason} &7({duration})");
            String formatted = this.applyPlaceholders(template, punishment, durationStr);
            Component comp = MessageUtil.parse(formatted);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("punish.notifications")) continue;
                player.sendMessage(comp);
            }
            Bukkit.getConsoleSender().sendMessage(comp);
        } else {
            String template = this.configManager.getMessage("broadcast." + typeKey, "&c[Наказания] &e{player} &fбыл {action} модератором &b{operator}&f. Причина: &e{reason} &7({duration})");
            String formatted = this.applyPlaceholders(template, punishment, durationStr);
            Bukkit.broadcast((Component)MessageUtil.parse(formatted));
        }
    }

    private void sendMuteNotification(Player player, Punishment p) {
        String durationStr = TimeUtil.formatDuration(p.getExpiresAt() <= 0L ? -1L : p.getExpiresAt() - p.getCreatedAt());
        List<String> lines = this.configManager.getMessageList("notification.mute-received", List.of("&c&m----------------------------------------", "&cВам был заблокирован чат!", "&fВыдал: &b{operator}", "&fПричина: &e{reason}", "&fСрок: &e{duration}", "&c&m----------------------------------------"));
        for (String line : lines) {
            player.sendMessage(MessageUtil.parse(this.applyPlaceholders(line, p, durationStr)));
        }
    }

    public Component buildBanScreen(Punishment p) {
        String durationStr = TimeUtil.formatDuration(p.getExpiresAt() <= 0L ? -1L : p.getExpiresAt() - p.getCreatedAt());
        List<String> lines = this.configManager.getMessageList("screens.ban", List.of("&c&lВЫ ЗАБЛОКИРОВАНЫ НА СЕРВЕРЕ!", "", "&fПричина: &e{reason}", "&fЗаблокировал: &b{operator}", "&fСрок: &e{duration}", "&fИстекает: &7{expires_at}", "", "&7Если вы не согласны с наказанием, создайте апелляцию:", "&bvk.com/your_server"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            sb.append(this.applyPlaceholders(lines.get(i), p, durationStr));
            if (i >= lines.size() - 1) continue;
            sb.append("\n");
        }
        return MessageUtil.parse(sb.toString());
    }

    public Component buildKickScreen(Punishment p) {
        List<String> lines = this.configManager.getMessageList("screens.kick", List.of("&c&lВЫ БЫЛИ ИСКЛЮЧЕНЫ С СЕРВЕРА!", "", "&fПричина: &e{reason}", "&fИсключил: &b{operator}"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            sb.append(this.applyPlaceholders(lines.get(i), p, ""));
            if (i >= lines.size() - 1) continue;
            sb.append("\n");
        }
        return MessageUtil.parse(sb.toString());
    }

    public Component buildMuteBlockedMessage(Punishment p) {
        String remainingStr = TimeUtil.formatDuration(p.getRemainingMillis());
        List<String> lines = this.configManager.getMessageList("screens.mute-chat", List.of("&cВы не можете отправлять сообщения в чат!", "&fПричина: &e{reason} &7| &fОсталось: &e{remaining}"));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            sb.append(this.applyPlaceholders(lines.get(i), p, "").replace("{remaining}", remainingStr));
            if (i >= lines.size() - 1) continue;
            sb.append("\n");
        }
        return MessageUtil.parse(sb.toString());
    }

    private String applyPlaceholders(String text, Punishment p, String durationStr) {
        return text.replace("{player}", p.getTargetName() != null ? p.getTargetName() : (p.getTargetIp() != null ? p.getTargetIp() : "Игрок")).replace("{ip}", p.getTargetIp() != null ? p.getTargetIp() : "-").replace("{operator}", p.getOperatorName()).replace("{reason}", p.getReason()).replace("{duration}", durationStr).replace("{action}", p.getType().getActionName()).replace("{type}", p.getType().getDisplayName()).replace("{created_at}", TimeUtil.formatDate(p.getCreatedAt())).replace("{expires_at}", p.isPermanent() ? "Никогда (навсегда)" : TimeUtil.formatDate(p.getExpiresAt()));
    }
}

