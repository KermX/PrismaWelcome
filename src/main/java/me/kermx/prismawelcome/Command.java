package me.kermx.prismawelcome;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class Command implements CommandExecutor {

    private final PrismaWelcome plugin;

    public Command(PrismaWelcome plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("prismawelcome")){
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")){
                if (sender.hasPermission("prismawelcome.reload")){
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "PrismaWelcome config reloaded!");
                    return true;
                }
                sender.sendMessage(ChatColor.RED + "You don't have permission!");
                return false;
            }
            sender.sendMessage(ChatColor.RED + "Invalid syntax");
            return false;
        }
        return false;
    }
}
