/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.projectn4.punish;

import java.sql.SQLException;
import java.util.logging.Level;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.projectn4.punish.command.BanCommand;
import ru.projectn4.punish.command.BanIpCommand;
import ru.projectn4.punish.command.HistoryCommand;
import ru.projectn4.punish.command.KickCommand;
import ru.projectn4.punish.command.MuteCommand;
import ru.projectn4.punish.command.PunishReloadCommand;
import ru.projectn4.punish.command.UnbanCommand;
import ru.projectn4.punish.command.UnbanIpCommand;
import ru.projectn4.punish.command.UnmuteCommand;
import ru.projectn4.punish.config.ConfigManager;
import ru.projectn4.punish.database.DatabaseManager;
import ru.projectn4.punish.listener.ChatListener;
import ru.projectn4.punish.listener.CommandListener;
import ru.projectn4.punish.listener.LoginListener;
import ru.projectn4.punish.permission.PermissionManager;
import ru.projectn4.punish.service.PunishmentService;

public class CustomPunishPlugin
extends JavaPlugin {
    private static CustomPunishPlugin instance;
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private PermissionManager permissionManager;
    private PunishmentService punishmentService;

    public void onEnable() {
        instance = this;
        this.configManager = new ConfigManager(this);
        this.configManager.load();
        this.databaseManager = new DatabaseManager(this);
        try {
            this.databaseManager.init();
        }
        catch (SQLException e) {
            this.getLogger().log(Level.SEVERE, "КРИТИЧЕСКАЯ ОШИБКА: Не удалось инициализировать базу данных SQLite!", e);
            this.getServer().getPluginManager().disablePlugin((Plugin)this);
            return;
        }
        this.permissionManager = new PermissionManager(this.configManager);
        this.punishmentService = new PunishmentService(this, this.databaseManager, this.configManager);
        this.punishmentService.loadActiveMutes();
        this.getServer().getPluginManager().registerEvents((Listener)new LoginListener(this.databaseManager, this.punishmentService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ChatListener(this.punishmentService), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new CommandListener(this.punishmentService, this.configManager), (Plugin)this);
        this.registerCommand("ban", new BanCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("banip", new BanIpCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("unban", new UnbanCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("unbanip", new UnbanIpCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("mute", new MuteCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("unmute", new UnmuteCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("kick", new KickCommand(this.punishmentService, this.permissionManager, this.configManager));
        this.registerCommand("history", new HistoryCommand(this.databaseManager, this.permissionManager, this.configManager));
        this.registerCommand("punish", new PunishReloadCommand(this.configManager, this.punishmentService, this.permissionManager));
        this.getLogger().info("=========================================");
        this.getLogger().info("CustomPunish v" + this.getPluginMeta().getVersion() + " успешно запущен!");
        this.getLogger().info("Ядро сервера: " + this.getServer().getName() + " " + this.getServer().getMinecraftVersion());
        this.getLogger().info("База данных: SQLite (punishments.db)");
        this.getLogger().info("Поддержка лимитов LuckPerms: АКТИВНА");
        this.getLogger().info("=========================================");
    }

    public void onDisable() {
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
        this.getLogger().info("CustomPunish успешно отключен.");
        instance = null;
    }

    private void registerCommand(String name, Object executor) {
        PluginCommand cmd = this.getCommand(name);
        if (cmd != null) {
            if (executor instanceof CommandExecutor) {
                CommandExecutor ce = (CommandExecutor)executor;
                cmd.setExecutor(ce);
            }
            if (executor instanceof TabCompleter) {
                TabCompleter tc = (TabCompleter)executor;
                cmd.setTabCompleter(tc);
            }
        } else {
            this.getLogger().warning("Команда /" + name + " не найдена в plugin.yml!");
        }
    }

    public static CustomPunishPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    public PermissionManager getPermissionManager() {
        return this.permissionManager;
    }

    public PunishmentService getPunishmentService() {
        return this.punishmentService;
    }
}

