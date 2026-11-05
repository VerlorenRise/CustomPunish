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
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;
import ru.projectn4.punish.util.MessageUtil;

public class WarnCommand implements CommandExecutor, TabCompleter {
    private final PunishmentService punishmentService;
    private final PermissionManager permissionManager;
    private final ConfigManager configManager;

    public WarnCommand(PunishmentService punishmentService, PermissionManager permissionManager, ConfigManager configManager) {
        this.punishmentService = punishmentService;
        this.permissionManager = permissionManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.warn")) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("error.no-permission", "&cУ вас нет прав на использование этой команды!"));
            return true;
        }
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, this.configManager.getMessage("usage.warn", "&6Использование: &e/" + label + " <игрок> [причина] [-s]"));
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
        if (CommandHelper.checkExempt(sender, parsed.getTarget(), this.permissionManager, this.configManager)) {
            return true;
        }
        Player onlineTarget = Bukkit.getPlayerExact(parsed.getTarget());
        UUID targetUuid = onlineTarget != null ? onlineTarget.getUniqueId() : null;
        String targetName = onlineTarget != null ? onlineTarget.getName() : parsed.getTarget();

        this.punishmentService.warn(sender, targetName, targetUuid, parsed.getReason(), parsed.isSilent()).thenAccept(success -> {
            if (success) {
                MessageUtil.sendMessage(sender, this.configManager.getMessage("success.warn-issued", "&aПредупреждение игроку &e{player} &aуспешно выдано!").replace("{player}", targetName));
            }
        });
        return true;
    }

    @Override
    @Nullable
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!this.permissionManager.hasPermission(sender, "punish.warn")) {
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
        if (args.length == 2) {
            List<String> list = new ArrayList<>(this.configManager.getDefaultReasons());
            list.add("-s");
            List<String> res = new ArrayList<>();
            for (String s : list) {
                if (s.toLowerCase().startsWith(args[1].toLowerCase())) {
                    res.add(s);
                }
            }
            return res;
        }
        return List.of();
    }
}
