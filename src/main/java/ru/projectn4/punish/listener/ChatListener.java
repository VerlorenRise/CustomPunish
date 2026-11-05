/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.papermc.paper.event.player.AsyncChatEvent
 *  net.kyori.adventure.audience.Audience
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.plugin.Plugin
 */
package ru.projectn4.punish.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import ru.projectn4.punish.CustomPunishPlugin;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.service.PunishmentService;

public class ChatListener
implements Listener {
    private final PunishmentService punishmentService;
    private final Set<Player> recentlyBlocked = ConcurrentHashMap.newKeySet();

    public ChatListener(PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=false)
    public void onLegacyChatLowest(AsyncPlayerChatEvent event) {
        this.handleLegacyChat(event);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onLegacyChatHighest(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Optional<Punishment> activeMute = this.punishmentService.getActiveMute(player.getUniqueId(), player.getName());
        if (activeMute.isPresent()) {
            event.setCancelled(true);
            event.getRecipients().clear();
            event.setMessage("");
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void handleLegacyChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Optional<Punishment> activeMute = this.punishmentService.getActiveMute(player.getUniqueId(), player.getName());
        if (activeMute.isPresent()) {
            ArrayList recipients = new ArrayList(event.getRecipients());
            event.setCancelled(true);
            event.getRecipients().clear();
            event.setMessage("");
            if (this.recentlyBlocked.add(player)) {
                try {
                    this.punishmentService.handleMutedChat(player, activeMute.get(), recipients);
                }
                finally {
                    Bukkit.getScheduler().runTask((Plugin)CustomPunishPlugin.getInstance(), () -> this.recentlyBlocked.remove(player));
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler(priority=EventPriority.LOWEST, ignoreCancelled=false)
    public void onPaperChatLowest(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Optional<Punishment> activeMute = this.punishmentService.getActiveMute(player.getUniqueId(), player.getName());
        if (activeMute.isPresent()) {
            ArrayList<CommandSender> viewers = new ArrayList<CommandSender>();
            for (Audience audience : event.viewers()) {
                if (!(audience instanceof CommandSender)) continue;
                CommandSender sender = (CommandSender)audience;
                viewers.add(sender);
            }
            event.setCancelled(true);
            event.viewers().clear();
            if (this.recentlyBlocked.add(player)) {
                try {
                    this.punishmentService.handleMutedChat(player, activeMute.get(), viewers);
                }
                finally {
                    Bukkit.getScheduler().runTask((Plugin)CustomPunishPlugin.getInstance(), () -> this.recentlyBlocked.remove(player));
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onPaperChatHighest(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Optional<Punishment> activeMute = this.punishmentService.getActiveMute(player.getUniqueId(), player.getName());
        if (activeMute.isPresent()) {
            event.setCancelled(true);
            event.viewers().clear();
        }
    }
}

