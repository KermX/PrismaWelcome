package me.kermx.prismawelcome;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaWelcome extends JavaPlugin{
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new JoinListener(), this);

        Command command = new Command(this);
        getCommand("prismawelcome").setExecutor(command);

        getLogger().info("==============================================");
        getLogger().info("PrismaWelcome Plugin has been enabled!");
        getLogger().info("Author: KermX");
        getLogger().info("Version: " + getDescription().getVersion());
        getLogger().info("==============================================");
    }
    public FileConfiguration getPluginConfig(){
        return getConfig();
    }

    @Override
    public void onDisable() {
        getLogger().info("==============================================");
        getLogger().info("PrismaWelcome Plugin has been disabled!");
        getLogger().info("==============================================");
    }
}
