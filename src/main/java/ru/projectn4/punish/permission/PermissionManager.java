/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.luckperms.api.LuckPerms
 *  net.luckperms.api.LuckPermsProvider
 *  net.luckperms.api.cacheddata.CachedMetaData
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.permissions.PermissionAttachmentInfo
 */
package ru.projectn4.punish.permission;

import java.util.Locale;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.model.PunishmentType;
import ru.projectn4.punish.permission.LimitResult;
import ru.projectn4.punish.util.TimeUtil;

public class PermissionManager {
    private final ConfigManager configManager;
    private boolean luckPermsPresent = false;

    public PermissionManager(ConfigManager configManager) {
        this.configManager = configManager;
        try {
            this.luckPermsPresent = Bukkit.getPluginManager().isPluginEnabled("LuckPerms");
        }
        catch (Throwable ignored) {
            this.luckPermsPresent = false;
        }
    }

    public boolean isExempt(CommandSender target) {
        if (target == null) {
            return false;
        }
        return target.hasPermission("punish.exempt");
    }

    public boolean canBypassExempt(CommandSender operator) {
        if (!(operator instanceof Player)) {
            return true;
        }
        return operator.hasPermission("punish.exempt.bypass") || operator.isOp();
    }

    public boolean hasPermission(CommandSender sender, String node) {
        if (!(sender instanceof Player)) {
            return true;
        }
        return sender.hasPermission(node) || sender.hasPermission("punish.admin") || sender.isOp();
    }

    public LimitResult calculateBanLimit(CommandSender sender) {
        long parsed;
        Object bestSource;
        if (!(sender instanceof Player)) {
            return LimitResult.unlimited();
        }
        Player player = (Player)sender;
        if (player.hasPermission("punish.admin") || player.hasPermission("punish.ban.unlimited") || player.isOp()) {
            return LimitResult.unlimited();
        }
        boolean canPermanent = player.hasPermission("punish.ban.permanent") || player.hasPermission("punish.limit.ban.permanent");
        long maxDuration = 0L;
        boolean hasAnyLimit = canPermanent;
        Object object = bestSource = canPermanent ? "Право на перманентный бан" : null;
        if (this.luckPermsPresent) {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                CachedMetaData metaData = lp.getPlayerAdapter(Player.class).getMetaData(player);
                String banMeta = metaData.getMetaValue("max-ban");
                if (banMeta == null) {
                    banMeta = metaData.getMetaValue("ban-limit");
                }
                if (banMeta != null && !banMeta.trim().isEmpty()) {
                    hasAnyLimit = true;
                    String val = banMeta.trim().toLowerCase(Locale.ROOT);
                    if (val.equals("permanent") || val.equals("perm") || val.equals("-1") || val.equals("навсегда")) {
                        canPermanent = true;
                        bestSource = "LuckPerms Meta (max-ban: " + banMeta + ")";
                    } else if (TimeUtil.isValidDuration(val) && (parsed = TimeUtil.parseDuration(val)) > maxDuration) {
                        maxDuration = parsed;
                        bestSource = "LuckPerms Meta (max-ban: " + banMeta + ")";
                    }
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (!pai.getValue()) continue;
            String perm = pai.getPermission().toLowerCase(Locale.ROOT);
            String timePart = null;
            if (perm.startsWith("punish.limit.ban.")) {
                timePart = perm.substring("punish.limit.ban.".length());
            } else if (perm.startsWith("punish.ban.limit.")) {
                timePart = perm.substring("punish.ban.limit.".length());
            }
            if (timePart == null) continue;
            hasAnyLimit = true;
            if (timePart.equals("permanent") || timePart.equals("perm") || timePart.equals("навсегда")) {
                canPermanent = true;
                bestSource = "Право " + pai.getPermission();
                continue;
            }
            if (!TimeUtil.isValidDuration(timePart) || (parsed = TimeUtil.parseDuration(timePart)) <= maxDuration) continue;
            maxDuration = parsed;
            bestSource = "Право " + pai.getPermission();
        }
        if (!hasAnyLimit) {
            return LimitResult.notConfigured();
        }
        return LimitResult.limited(canPermanent, maxDuration, (String)bestSource);
    }

    public LimitResult calculateMuteLimit(CommandSender sender) {
        long parsed;
        Object bestSource;
        if (!(sender instanceof Player)) {
            return LimitResult.unlimited();
        }
        Player player = (Player)sender;
        if (player.hasPermission("punish.admin") || player.hasPermission("punish.mute.unlimited") || player.isOp()) {
            return LimitResult.unlimited();
        }
        boolean canPermanent = player.hasPermission("punish.mute.permanent") || player.hasPermission("punish.limit.mute.permanent");
        long maxDuration = 0L;
        boolean hasAnyLimit = canPermanent;
        Object object = bestSource = canPermanent ? "Право на перманентный мут" : null;
        if (this.luckPermsPresent) {
            try {
                LuckPerms lp = LuckPermsProvider.get();
                CachedMetaData metaData = lp.getPlayerAdapter(Player.class).getMetaData(player);
                String muteMeta = metaData.getMetaValue("max-mute");
                if (muteMeta == null) {
                    muteMeta = metaData.getMetaValue("mute-limit");
                }
                if (muteMeta != null && !muteMeta.trim().isEmpty()) {
                    hasAnyLimit = true;
                    String val = muteMeta.trim().toLowerCase(Locale.ROOT);
                    if (val.equals("permanent") || val.equals("perm") || val.equals("-1") || val.equals("навсегда")) {
                        canPermanent = true;
                        bestSource = "LuckPerms Meta (max-mute: " + muteMeta + ")";
                    } else if (TimeUtil.isValidDuration(val) && (parsed = TimeUtil.parseDuration(val)) > maxDuration) {
                        maxDuration = parsed;
                        bestSource = "LuckPerms Meta (max-mute: " + muteMeta + ")";
                    }
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (!pai.getValue()) continue;
            String perm = pai.getPermission().toLowerCase(Locale.ROOT);
            String timePart = null;
            if (perm.startsWith("punish.limit.mute.")) {
                timePart = perm.substring("punish.limit.mute.".length());
            } else if (perm.startsWith("punish.mute.limit.")) {
                timePart = perm.substring("punish.mute.limit.".length());
            }
            if (timePart == null) continue;
            hasAnyLimit = true;
            if (timePart.equals("permanent") || timePart.equals("perm") || timePart.equals("навсегда")) {
                canPermanent = true;
                bestSource = "Право " + pai.getPermission();
                continue;
            }
            if (!TimeUtil.isValidDuration(timePart) || (parsed = TimeUtil.parseDuration(timePart)) <= maxDuration) continue;
            maxDuration = parsed;
            bestSource = "Право " + pai.getPermission();
        }
        if (!hasAnyLimit) {
            return LimitResult.notConfigured();
        }
        return LimitResult.limited(canPermanent, maxDuration, (String)bestSource);
    }

    public String validatePunishmentDuration(CommandSender sender, PunishmentType type, long requestedDurationMillis) {
        LimitResult limit;
        LimitResult limitResult = limit = type == PunishmentType.BAN || type == PunishmentType.BAN_IP ? this.calculateBanLimit(sender) : this.calculateMuteLimit(sender);
        if (limit.isUnlimited()) {
            return null;
        }
        if (!limit.isConfigured()) {
            return this.configManager.getMessage("error.no-limit-configured", "&cДля вашей роли в LuckPerms не задан лимит времени наказания! Обратитесь к администратору.");
        }
        if (requestedDurationMillis <= 0L) {
            if (!limit.canPermanent()) {
                String maxStr = TimeUtil.formatDuration(limit.getMaxDurationMillis());
                return this.configManager.getMessage("error.permanent-forbidden", "&cВам запрещено выдавать навсегда! Ваш максимальный лимит: &e{max_duration}").replace("{max_duration}", maxStr);
            }
            return null;
        }
        if (limit.getMaxDurationMillis() > 0L && requestedDurationMillis > limit.getMaxDurationMillis()) {
            String maxStr = TimeUtil.formatDuration(limit.getMaxDurationMillis());
            String reqStr = TimeUtil.formatDuration(requestedDurationMillis);
            return this.configManager.getMessage("error.duration-exceeded", "&cУказанный срок (&e{duration}&c) превышает ваш лимит (&e{max_duration}&c)!").replace("{duration}", reqStr).replace("{max_duration}", maxStr);
        }
        return null;
    }
}

