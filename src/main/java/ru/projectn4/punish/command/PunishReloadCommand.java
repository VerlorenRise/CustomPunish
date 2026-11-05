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

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;

public class PunishReloadCommand
implements CommandExecutor,
TabCompleter {
    private final ConfigManager configManager;
    private final PunishmentService punishmentService;
    private final PermissionManager permissionManager;

    public PunishReloadCommand(ConfigManager configManager, PunishmentService punishmentService, PermissionManager permissionManager) {
        this.configManager = configManager;
        this.punishmentService = punishmentService;
        this.permissionManager = permissionManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.reload")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            this.configManager.load();
            this.punishmentService.loadActiveMutes();
            MessageUtil.sendMessage(sender, this.configManager.getMessage("success.reload", "&aКонфигурация плагина наказаний успешно перезагружена!"));
            return true;
        }
        MessageUtil.sendMessage(sender, "&6&lCustomPunish &7— Система наказаний для Purpur 26.2");
        MessageUtil.sendMessage(sender, "&e/ban <игрок> [срок] [причина] [-s] &7— Заблокировать игрока");
        MessageUtil.sendMessage(sender, "&e/banip <игрок|IP> [срок] [причина] [-s] &7— Заблокировать по IP");
        MessageUtil.sendMessage(sender, "&e/unban <игрок> [причина] &7— Разблокировать игрока");
        MessageUtil.sendMessage(sender, "&e/unbanip <IP> [причина] &7— Разблокировать IP");
        MessageUtil.sendMessage(sender, "&e/mute <игрок> [срок] [причина] [-s] &7— Замутить игрока");
        MessageUtil.sendMessage(sender, "&e/unmute <игрок> [причина] &7— Снять мут");
        MessageUtil.sendMessage(sender, "&e/kick <игрок> [причина] [-s] &7— Исключить с сервера");
        MessageUtil.sendMessage(sender, "&e/history <игрок> [стр] &7— Просмотр истории наказаний");
        MessageUtil.sendMessage(sender, "&e/punish reload &7— Перезагрузить конфиг");
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = List.of("reload", "help");
            ArrayList<String> res = new ArrayList<String>();
            for (String s : list) {
                if (!s.toLowerCase().startsWith(args[0].toLowerCase())) continue;
                res.add(s);
            }
            return res;
        }
        return List.of();
    }
}

