/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent
 *  org.bukkit.event.player.AsyncPlayerPreLoginEvent$Result
 */
package ru.projectn4.punish.listener;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import ru.projectn4.punish.database.DatabaseManager;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.model.PunishmentType;
import ru.projectn4.punish.service.PunishmentService;

public class LoginListener
implements Listener {
    private final DatabaseManager databaseManager;
    private final PunishmentService punishmentService;

    public LoginListener(DatabaseManager databaseManager, PunishmentService punishmentService) {
        this.databaseManager = databaseManager;
        this.punishmentService = punishmentService;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
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
                this.punishmentService.cacheMute((Punishment)optMute.get());
            } else {
                this.databaseManager.getActivePunishmentByName(name, PunishmentType.MUTE).thenAccept(optMuteByName -> optMuteByName.ifPresent(this.punishmentService::cacheMute));
            }
        });
    }
}

