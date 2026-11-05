/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ru.projectn4.punish.command;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.database.DatabaseManager;
import ru.projectn4.punish.model.Punishment;
import ru.projectn4.punish.model.PunishmentType;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.util.MessageUtil;
import ru.projectn4.punish.util.TimeUtil;

public class HistoryCommand
implements CommandExecutor,
TabCompleter {
    private static final int PAGE_SIZE = 5;
    private final DatabaseManager databaseManager;
    private final PermissionManager permissionManager;
    private final ConfigManager configManager;

    public HistoryCommand(DatabaseManager databaseManager, PermissionManager permissionManager, ConfigManager configManager) {
        this.databaseManager = databaseManager;
        this.permissionManager = permissionManager;
        this.configManager = configManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.history")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("usage.history", "&6Использование: &e/" + label + " <игрок> [страница]"));
            return true;
        }
        String targetName = args[0];
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Math.max(1, Integer.parseInt(args[1]));
            }
            catch (NumberFormatException ignored) {
                page = 1;
            }
        }
        int targetPage = page;
        Player online = Bukkit.getPlayerExact((String)targetName);
        UUID targetUuid = online != null ? online.getUniqueId() : null;
        this.databaseManager.getHistoryCount(targetName, targetUuid).thenAccept(totalCount -> {
            if (totalCount == 0) {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("history.empty", "&aУ игрока &e{player} &aнет записей в истории наказаний.").replace("{player}", targetName));
                return;
            }
            int totalPages = (int)Math.ceil((double)totalCount.intValue() / 5.0);
            int validPage = Math.min(targetPage, totalPages);
            int offset = (validPage - 1) * 5;
            this.databaseManager.getHistory(targetName, targetUuid, 5, offset).thenAccept(list -> this.sendHistoryPage(sender, targetName, (List<Punishment>)list, validPage, totalPages, (int)totalCount));
        });
        return true;
    }

    private void sendHistoryPage(CommandSender sender, String targetName, List<Punishment> list, int page, int totalPages, int totalCount) {
        MessageUtil.sendMessage(sender, "&8&m----------------------------------------");
        MessageUtil.sendMessage(sender, "&6История наказаний игрока &e" + targetName + " &7(Всего: &f" + totalCount + "&7)");
        MessageUtil.sendMessage(sender, "");
        for (Punishment p : list) {
            String typeBadge = switch (p.getType()) {
                case BAN -> "&c&l[БАН]";
                case BAN_IP -> "&4&l[IP-БАН]";
                case MUTE -> "&e&l[МУТ]";
                case KICK -> "&7&l[КИК]";
                case WARN -> "&6&l[ВАРН]";
            };
            String statusBadge = p.getType() == PunishmentType.KICK ? "&7(Кик)" : (p.isActive() ? (p.isExpired() ? "&8(Истек)" : "&a&l(АКТИВНО)") : (p.getUnpunishedAt() != null ? "&6(Снято)" : "&7(Неактивно)"));
            String durationStr = TimeUtil.formatDuration(p.getExpiresAt() <= 0L ? -1L : p.getExpiresAt() - p.getCreatedAt());
            MessageUtil.sendMessage(sender, typeBadge + " " + statusBadge + " &7#&f" + p.getId() + " &7| &f" + TimeUtil.formatDate(p.getCreatedAt()));
            MessageUtil.sendMessage(sender, "  &7Модератор: &b" + p.getOperatorName() + " &7| Срок: &e" + durationStr);
            MessageUtil.sendMessage(sender, "  &7Причина: &f" + p.getReason());
            if (p.getUnpunishedAt() != null) {
                MessageUtil.sendMessage(sender, "  &6Снял: &b" + p.getUnpunishedByName() + " &7(" + TimeUtil.formatDate(p.getUnpunishedAt()) + ") &7Причина: &f" + (p.getUnpunishReason() != null ? p.getUnpunishReason() : "Без причины"));
            }
            MessageUtil.sendMessage(sender, "");
        }
        MessageUtil.sendMessage(sender, "&7Страница &e" + page + " &7из &e" + totalPages + " &8| &7/history " + targetName + " [стр]");
        MessageUtil.sendMessage(sender, "&8&m----------------------------------------");
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.history")) {
            return List.of();
        }
        if (args.length == 1) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().toLowerCase().startsWith(args[0].toLowerCase())) continue;
                names.add(p.getName());
            }
            return names;
        }
        if (args.length == 2) {
            return List.of("1", "2", "3");
        }
        return List.of();
    }
}

