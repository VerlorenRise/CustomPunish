package ru.projectn4.punish.listener;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.database.DatabaseManager;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.model.PunishmentType;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;
import ru.projectn4.punish.util.TimeUtil;

public class LoginListener implements Listener {
    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final PunishmentService punishmentService;
    private final ConfigManager configManager;

    public LoginListener(JavaPlugin plugin, DatabaseManager databaseManager, PunishmentService punishmentService, ConfigManager configManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.punishmentService = punishmentService;
        this.configManager = configManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ip = event.getAddress().getHostAddress();

        Optional<Punishment> ipBan = this.databaseManager.getActiveBanByIp(ip).join();
        if (ipBan.isPresent()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, this.punishmentService.buildBanScreen(ipBan.get()));
            return;
        }

        Optional<Punishment> uuidBan = this.databaseManager.getActivePunishmentByUuid(uuid, PunishmentType.BAN).join();
        if (uuidBan.isPresent()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, this.punishmentService.buildBanScreen(uuidBan.get()));
            return;
        }

        Optional<Punishment> nameBan = this.databaseManager.getActivePunishmentByName(name, PunishmentType.BAN).join();
        if (nameBan.isPresent()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, this.punishmentService.buildBanScreen(nameBan.get()));
            return;
        }

        this.databaseManager.savePlayerData(uuid, name, ip);

        this.databaseManager.getActivePunishmentByUuid(uuid, PunishmentType.MUTE).thenAccept(optMute -> {
            if (optMute.isPresent()) {
                this.punishmentService.cacheMute(optMute.get());
            } else {
                this.databaseManager.getActivePunishmentByName(name, PunishmentType.MUTE).thenAccept(optMuteByName ->
                    optMuteByName.ifPresent(this.punishmentService::cacheMute)
                );
            }
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        this.databaseManager.getUnnotifiedOfflinePunishments(player.getUniqueId(), player.getName()).thenAccept(list -> {
            if (list == null || list.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                for (Punishment p : list) {
                    String typeStr = (p.getType() == PunishmentType.WARN) ? "варн" : "мут";
                    String durationStr = TimeUtil.formatDuration(p.getRemainingMillis());
                    String template = this.configManager.getMessage(
                        "notification.offline-punishment",
                        "&cАдминистратор &b{operator} &cвыдал вам в оффлайне &e{type}&c Причина: &f{reason}&c. Истекает через: &e{remaining}"
                    );
                    String msg = template
                        .replace("{operator}", p.getOperatorName())
                        .replace("{type}", typeStr)
                        .replace("{reason}", p.getReason())
                        .replace("{remaining}", durationStr)
                        .replace("{duration}", durationStr);
                    player.sendMessage(MessageUtil.parse(msg));
                    this.databaseManager.markPunishmentNotified(p.getId());
                }
            });
        });
    }
}
