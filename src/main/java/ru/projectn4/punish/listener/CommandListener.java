/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 */
package ru.projectn4.punish.listener;

import java.util.Locale;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.service.PunishmentService;

public class CommandListener
implements Listener {
    private final PunishmentService punishmentService;
    private final ConfigManager configManager;

    public CommandListener(PunishmentService punishmentService, ConfigManager configManager) {
        this.punishmentService = punishmentService;
        this.configManager = configManager;
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Optional<Punishment> activeMute;
        Player player = event.getPlayer();
        String message = event.getMessage();
        if (message.isEmpty() || !message.startsWith("/")) {
            return;
        }
        String rawCommand = message.substring(1).trim();
        int spaceIndex = rawCommand.indexOf(32);
        String commandName = (spaceIndex != -1 ? rawCommand.substring(0, spaceIndex) : rawCommand).toLowerCase(Locale.ROOT);
        if (commandName.contains(":")) {
            commandName = commandName.substring(commandName.indexOf(58) + 1);
        }
        if (this.configManager.getBlockedCommandsOnMute().contains(commandName) && (activeMute = this.punishmentService.getActiveMute(player.getUniqueId(), player.getName())).isPresent()) {
            event.setCancelled(true);
            player.sendMessage(this.punishmentService.buildMuteBlockedMessage(activeMute.get()));
        }
    }
}

