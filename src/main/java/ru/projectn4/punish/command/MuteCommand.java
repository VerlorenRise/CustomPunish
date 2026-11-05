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
import ru.projectn4.punish.command.CommandHelper;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.model.PunishmentType;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;

public class MuteCommand
implements CommandExecutor,
TabCompleter {
    private final PunishmentService punishmentService;
    private final PermissionManager permissionManager;
    private final ConfigManager configManager;

    public MuteCommand(PunishmentService punishmentService, PermissionManager permissionManager, ConfigManager configManager) {
        this.punishmentService = punishmentService;
        this.permissionManager = permissionManager;
        this.configManager = configManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.mute")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("usage.mute", "&6Использование: &e/" + label + " <игрок> [срок] [причина] [-s]"));
            return true;
        }
        CommandHelper.ParsedArgs parsed = CommandHelper.parseTimedArgs(args, this.configManager);
        if (parsed == null) {
            return true;
        }
        if (parsed.isSilent() && !this.permissionManager.hasPermission(sender, "punish.silent")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.silent-no-permission", "&cУ вас нет прав на использование тихого режима (-s)!"));
            return true;
        }
        if (CommandHelper.checkExempt(sender, parsed.getTarget(), this.permissionManager, this.configManager)) {
            return true;
        }
        String limitError = this.permissionManager.validatePunishmentDuration(sender, PunishmentType.MUTE, parsed.getDurationMillis());
        if (limitError != null) {
            MessageUtil.sendMessage(sender, limitError);
            return true;
        }
        Player onlineTarget = Bukkit.getPlayerExact((String)parsed.getTarget());
        if (onlineTarget == null) {
            onlineTarget = Bukkit.getPlayer((String)parsed.getTarget());
        }
        UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : null;
        String targetName = onlineTarget != null ? onlineTarget.getName() : parsed.getTarget();
        this.punishmentService.mute(sender, targetName, targetUuid, parsed.getDurationMillis(), parsed.getReason(), parsed.isSilent()).thenAccept(success -> {
            if (success.booleanValue()) {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("success.mute-issued", "&aИгрок &e{player} &aуспешно замучен!").replace("{player}", targetName));
            }
        });
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.mute")) {
            return List.of();
        }
        return CommandHelper.completeTimed(args, this.configManager);
    }
}

