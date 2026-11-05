package ru.projectn4.punish.command;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;

public class UnwarnCommand implements CommandExecutor, TabCompleter {
    private final PunishmentService punishmentService;
    private final PermissionManager permissionManager;
    private final ConfigManager configManager;

    public UnwarnCommand(PunishmentService punishmentService, PermissionManager permissionManager, ConfigManager configManager) {
        this.punishmentService = punishmentService;
        this.permissionManager = permissionManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.unwarn")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("usage.unwarn", "&6Использование: &e/" + label + " <игрок> [ID | причина]"));
            return true;
        }
        String targetName = args[0];
        Integer warnId = null;
        String reason = "Снятие предупреждения";
        if (args.length > 1) {
            try {
                warnId = Integer.parseInt(args[1]);
                if (args.length > 2) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        if (i > 2) sb.append(" ");
                        sb.append(args[i]);
                    }
                    reason = sb.toString();
                }
            } catch (NumberFormatException e) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    if (i > 1) sb.append(" ");
                    sb.append(args[i]);
                }
                reason = sb.toString();
            }
        }
        final Integer finalWarnId = warnId;
        this.punishmentService.unwarn(sender, targetName, warnId, reason).thenAccept(success -> {
            if (Boolean.TRUE.equals(success)) {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("success.unwarn-issued", "&aПредупреждение с игрока &e{player} &aуспешно снято!").replace("{player}", targetName));
            } else {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("error.unwarn-not-found", "&cУ игрока &e{player} &cнет активных предупреждений" + (finalWarnId != null ? " с ID #" + finalWarnId : "") + "!").replace("{player}", targetName));
            }
        });
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.unwarn")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    list.add(p.getName());
                }
            }
            return list;
        }
        return List.of();
    }
}
