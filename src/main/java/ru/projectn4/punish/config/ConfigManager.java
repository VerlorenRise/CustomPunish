/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.projectn4.punish.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.projectn4.punish.config.ConfigLimitGroup;
import ru.projectn4.punish.util.TimeUtil;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, ConfigLimitGroup> banLimits = new LinkedHashMap<String, ConfigLimitGroup>();
    private final Map<String, ConfigLimitGroup> muteLimits = new LinkedHashMap<String, ConfigLimitGroup>();
    private final List<String> blockedCommandsOnMute = new ArrayList<String>();
    private final List<String> defaultReasons = new ArrayList<String>();
    private long defaultBanLimit = 1800000L;
    private boolean defaultBanCanPermanent = false;
    private long defaultMuteLimit = 1800000L;
    private boolean defaultMuteCanPermanent = false;
    private double localChatRadius = 100.0;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        ConfigurationSection muteSection;
        this.plugin.saveDefaultConfig();
        this.plugin.reloadConfig();
        FileConfiguration config = this.plugin.getConfig();
        this.banLimits.clear();
        this.muteLimits.clear();
        this.blockedCommandsOnMute.clear();
        this.defaultReasons.clear();
        ConfigurationSection banSection = config.getConfigurationSection("limits.ban");
        if (banSection != null) {
            for (Object key : banSection.getKeys(false)) {
                String perm = banSection.getString((String)key + ".permission", "punish.limit.ban." + (String)key);
                String maxDurStr = banSection.getString((String)key + ".max-duration", "1d");
                boolean canPermanent = banSection.getBoolean((String)key + ".can-permanent", false);
                long maxMillis = canPermanent ? -1L : this.parseDurationSafe(maxDurStr, 86400000L);
                this.banLimits.put(((String)key).toLowerCase(Locale.ROOT), new ConfigLimitGroup((String)key, perm, maxMillis, canPermanent));
            }
        }
        if ((muteSection = config.getConfigurationSection("limits.mute")) != null) {
            for (String key : muteSection.getKeys(false)) {
                String perm = muteSection.getString(key + ".permission", "punish.limit.mute." + key);
                String maxDurStr = muteSection.getString(key + ".max-duration", "1d");
                boolean canPermanent = muteSection.getBoolean(key + ".can-permanent", false);
                long maxMillis = canPermanent ? -1L : this.parseDurationSafe(maxDurStr, 86400000L);
                this.muteLimits.put(key.toLowerCase(Locale.ROOT), new ConfigLimitGroup(key, perm, maxMillis, canPermanent));
            }
        }
        String defBanStr = config.getString("default-limits.ban.max-duration", "30m");
        this.defaultBanCanPermanent = config.getBoolean("default-limits.ban.can-permanent", false);
        this.defaultBanLimit = this.defaultBanCanPermanent ? -1L : this.parseDurationSafe(defBanStr, 1800000L);
        String defMuteStr = config.getString("default-limits.mute.max-duration", "1h");
        this.defaultMuteCanPermanent = config.getBoolean("default-limits.mute.can-permanent", false);
        this.defaultMuteLimit = this.defaultMuteCanPermanent ? -1L : this.parseDurationSafe(defMuteStr, 3600000L);
        List<String> blocked = config.getStringList("blocked-commands-on-mute");
        if (blocked.isEmpty()) {
            this.blockedCommandsOnMute.addAll(Arrays.asList("msg", "tell", "w", "r", "reply", "whisper", "me"));
        } else {
            for (String cmd : blocked) {
                this.blockedCommandsOnMute.add(cmd.toLowerCase(Locale.ROOT).replace("/", ""));
            }
        }
        List<String> reasons = config.getStringList("suggested-reasons");
        if (reasons.isEmpty()) {
            this.defaultReasons.addAll(Arrays.asList("Читы", "Оскорбления", "Спам", "Неадекватное поведение", "Реклама", "Помеха игре"));
        } else {
            this.defaultReasons.addAll(reasons);
        }
        this.localChatRadius = config.getDouble("local-chat-radius", 100.0);
    }

    private long parseDurationSafe(String str, long fallback) {
        try {
            return TimeUtil.parseDuration(str);
        }
        catch (Exception e) {
            return fallback;
        }
    }

    public Map<String, ConfigLimitGroup> getBanLimits() {
        return Collections.unmodifiableMap(this.banLimits);
    }

    public Map<String, ConfigLimitGroup> getMuteLimits() {
        return Collections.unmodifiableMap(this.muteLimits);
    }

    public long getDefaultBanLimit() {
        return this.defaultBanLimit;
    }

    public boolean isDefaultBanCanPermanent() {
        return this.defaultBanCanPermanent;
    }

    public long getDefaultMuteLimit() {
        return this.defaultMuteLimit;
    }

    public boolean isDefaultMuteCanPermanent() {
        return this.defaultMuteCanPermanent;
    }

    public List<String> getBlockedCommandsOnMute() {
        return Collections.unmodifiableList(this.blockedCommandsOnMute);
    }

    public double getLocalChatRadius() {
        return this.localChatRadius;
    }

    public List<String> getDefaultReasons() {
        return Collections.unmodifiableList(this.defaultReasons);
    }

    public String getMessage(String path, String def) {
        return this.plugin.getConfig().getString("messages." + path, def);
    }

    public List<String> getMessageList(String path, List<String> def) {
        List list = this.plugin.getConfig().getStringList("messages." + path);
        return list != null && !list.isEmpty() ? list : def;
    }
}

