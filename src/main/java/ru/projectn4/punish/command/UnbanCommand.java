/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ru.projectn4.punish.command;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.projectn4.punish.command.CommandHelper;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;

public class UnbanCommand
implements CommandExecutor,
TabCompleter {
    private final PunishmentService punishmentService;
    private final PermissionManager permissionManager;
    private final ConfigManager configManager;

    public UnbanCommand(PunishmentService punishmentService, PermissionManager permissionManager, ConfigManager configManager) {
        this.punishmentService = punishmentService;
        this.permissionManager = permissionManager;
        this.configManager = configManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.unban")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("usage.unban", "&6Использование: &e/" + label + " <игрок> [причина]"));
            return true;
        }
        CommandHelper.ParsedArgs parsed = CommandHelper.parseUntimedArgs(args, this.configManager);
        if (parsed == null) {
            return true;
        }
        this.punishmentService.unban(sender, parsed.getTarget(), parsed.getReason()).thenAccept(success -> {
            if (success.booleanValue()) {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("success.unban-done", "&aИгрок &e{player} &aуспешно разблокирован!").replace("{player}", parsed.getTarget()));
            } else {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("error.not-banned", "&cУ игрока &e{player} &cнет активного бана!").replace("{player}", parsed.getTarget()));
            }
        });
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return List.of();
    }
}

