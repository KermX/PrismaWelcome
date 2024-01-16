package me.kermx.prismawelcome;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PrismaWelcome extends JavaPlugin{

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
    }

    public FileConfiguration getPluginConfig(){
        return getConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
