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
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;

public class KickCommand
implements CommandExecutor,
TabCompleter {
    private final PunishmentService punishmentService;
    private final PermissionManager permissionManager;
    private final ConfigManager configManager;

    public KickCommand(PunishmentService punishmentService, PermissionManager permissionManager, ConfigManager configManager) {
        this.punishmentService = punishmentService;
        this.permissionManager = permissionManager;
        this.configManager = configManager;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.kick")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("usage.kick", "&6Использование: &e/" + label + " <игрок> [причина] [-s]"));
            return true;
        }
        CommandHelper.ParsedArgs parsed = CommandHelper.parseUntimedArgs(args, this.configManager);
        if (parsed == null) {
            return true;
        }
        if (parsed.isSilent() && !this.permissionManager.hasPermission(sender, "punish.silent")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.silent-no-permission", "&cУ вас нет прав на использование тихого режима (-s)!"));
            return true;
        }
        Player target = Bukkit.getPlayerExact((String)parsed.getTarget());
        if (target == null || !target.isOnline()) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.player-not-online", "&cИгрок &e{player} &cне в сети!").replace("{player}", parsed.getTarget()));
            return true;
        }
        if (this.permissionManager.isExempt((CommandSender)target) && !this.permissionManager.canBypassExempt(sender)) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.target-exempt", "&cВы не можете наказать игрока &e{player}&c, так как он защищен от наказаний!").replace("{player}", target.getName()));
            return true;
        }
        this.punishmentService.kick(sender, target, parsed.getReason(), parsed.isSilent()).thenAccept(success -> {
            if (success.booleanValue()) {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("success.kick-issued", "&aИгрок &e{player} &aуспешно исключен!").replace("{player}", target.getName()));
            }
        });
        return true;
    }

    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.kick")) {
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
            ArrayList<String> reasons = new ArrayList<String>(this.configManager.getDefaultReasons());
            reasons.add("-s");
            ArrayList<String> res = new ArrayList<String>();
            for (String r : reasons) {
                if (!r.toLowerCase().startsWith(args[1].toLowerCase())) continue;
                res.add(r);
            }
            return res;
        }
        return List.of();
    }
}

