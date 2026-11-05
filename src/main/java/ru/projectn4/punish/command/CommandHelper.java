/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 */
package ru.projectn4.punish.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.util.MessageUtil;
import ru.projectn4.punish.util.TimeUtil;

public final class CommandHelper {
    private CommandHelper() {
    }

    public static ParsedArgs parseTimedArgs(String[] args, ConfigManager configManager) {
        String reason;
        if (args.length == 0) {
            return null;
        }
        String target = args[0];
        boolean silent = false;
        ArrayList<String> remaining = new ArrayList<String>();
        for (int i = 1; i < args.length; ++i) {
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
                continue;
            }
            remaining.add(args[i]);
        }
        long durationMillis = -1L;
        if (remaining.isEmpty()) {
            reason = configManager.getMessage("default-reason", "Причина не указана");
        } else {
            String firstArg = (String)remaining.get(0);
            if (TimeUtil.isValidDuration(firstArg)) {
                durationMillis = TimeUtil.parseDuration(firstArg);
                remaining.remove(0);
                reason = remaining.isEmpty() ? configManager.getMessage("default-reason", "Причина не указана") : String.join((CharSequence)" ", remaining);
            } else {
                reason = String.join((CharSequence)" ", remaining);
            }
        }
        return new ParsedArgs(target, durationMillis, reason, silent);
    }

    public static ParsedArgs parseUntimedArgs(String[] args, ConfigManager configManager) {
        if (args.length == 0) {
            return null;
        }
        String target = args[0];
        boolean silent = false;
        ArrayList<String> remaining = new ArrayList<String>();
        for (int i = 1; i < args.length; ++i) {
            if (args[i].equalsIgnoreCase("-s")) {
                silent = true;
                continue;
            }
            remaining.add(args[i]);
        }
        String reason = remaining.isEmpty() ? configManager.getMessage("default-reason", "Причина не указана") : String.join((CharSequence)" ", remaining);
        return new ParsedArgs(target, 0L, reason, silent);
    }

    public static boolean checkExempt(CommandSender sender, String targetName, PermissionManager permissionManager, ConfigManager configManager) {
        Player targetPlayer = Bukkit.getPlayerExact((String)targetName);
        if (targetPlayer != null && permissionManager.isExempt((CommandSender)targetPlayer) && !permissionManager.canBypassExempt(sender)) {
            MessageUtil.sendMessage(sender, configManager.getMessage("error.target-exempt", "&cВы не можете наказать игрока &e{player}&c, так как он защищен от наказаний!").replace("{player}", targetPlayer.getName()));
            return true;
        }
        return false;
    }

    public static List<String> completeTimed(String[] args, ConfigManager configManager) {
        if (args.length == 1) {
            ArrayList<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getName().toLowerCase().startsWith(args[0].toLowerCase())) continue;
                names.add(p.getName());
            }
            return names;
        }
        if (args.length == 2) {
            List<String> suggestions = Arrays.asList("15m", "30m", "1h", "2h", "1d", "3d", "7d", "10d", "30d", "permanent", "-s");
            ArrayList<String> res = new ArrayList<String>();
            for (String s : suggestions) {
                if (!s.toLowerCase().startsWith(args[1].toLowerCase())) continue;
                res.add(s);
            }
            return res;
        }
        if (args.length == 3) {
            ArrayList<String> reasons = new ArrayList<String>(configManager.getDefaultReasons());
            reasons.add("-s");
            ArrayList<String> res = new ArrayList<String>();
            for (String r : reasons) {
                if (!r.toLowerCase().startsWith(args[2].toLowerCase())) continue;
                res.add(r);
            }
            return res;
        }
        return List.of();
    }

    public static class ParsedArgs {
        private final String target;
        private final long durationMillis;
        private final String reason;
        private final boolean silent;

        public ParsedArgs(String target, long durationMillis, String reason, boolean silent) {
            this.target = target;
            this.durationMillis = durationMillis;
            this.reason = reason;
            this.silent = silent;
        }

        public String getTarget() {
            return this.target;
        }

        public long getDurationMillis() {
            return this.durationMillis;
        }

        public String getReason() {
            return this.reason;
        }

        public boolean isSilent() {
            return this.silent;
        }
    }
}

