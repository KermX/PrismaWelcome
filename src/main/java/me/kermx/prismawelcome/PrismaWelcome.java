package me.kermx.prismawelcome;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaWelcome extends JavaPlugin {

    private ConfigUtil configUtil;

    @Override
    public void onEnable() {
        setupConfig();
        loadConfigurations();
        registerEvents();
        registerCommands();

        getLogger().info("==============================================");
        getLogger().info("PrismaWelcome Plugin has been enabled!");
        getLogger().info("Author: KermX");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("==============================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("==============================================");
        getLogger().info("PrismaWelcome Plugin has been disabled!");
        getLogger().info("==============================================");
    }

    private void setupConfig() {
        saveDefaultConfig();
    }

    private void loadConfigurations(){
        configUtil = new ConfigUtil();
        configUtil.loadConfig();
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
    }

    private void registerCommands() {
        Command command = new Command(this);
        getCommand("prismawelcome").setExecutor(command);
    }

    public FileConfiguration getPluginConfig() {
        return getConfig();
    }

    public ConfigUtil getConfigUtil(){
        return configUtil;
    }
}
