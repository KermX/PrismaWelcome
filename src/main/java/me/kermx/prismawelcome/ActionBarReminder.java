package me.kermx.prismawelcome;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActionBarReminder {

    private final PrismaWelcome plugin;
    private final WelcomeRewardSystem welcomeRewardSystem;
    private BukkitTask actionBarTask;

    public ActionBarReminder(PrismaWelcome plugin, WelcomeRewardSystem welcomeRewardSystem) {
        this.plugin = plugin;
        this.welcomeRewardSystem = welcomeRewardSystem;
    }

    public void displayActionBar(List<Player> players, Player newPlayer, FileConfiguration config) {
        if (config.getBoolean("welcomeRewarding.actionBarEnabled", false) && newPlayer != null) {
            String reminderMessage = config.getString("welcomeRewarding.actionBarReminder", "");
            if (!reminderMessage.isEmpty()) {
                // Schedule a repeating task to update the action bar reminder every second
                actionBarTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                    if (newPlayer != null) {
                        int remainingTime = welcomeRewardSystem.getTimeLimit() - (int) ((System.currentTimeMillis() - newPlayer.getFirstPlayed()) / 1000);

                        // Check if there is remaining time
                        if (remainingTime > 0) {
                            for (Player player : players) {
                                if (player != newPlayer) {  // Exclude the newPlayer
                                    String formattedMessage = reminderMessage
                                            .replace("%player_name%", newPlayer.getName())
                                            .replace("%remaining_time%", String.valueOf(remainingTime));

                                    formattedMessage = parseHexColorCodes(formattedMessage);

                                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(parseColorCodes(formattedMessage)));
                                }
                            }
                        } else {
                            cancelActionBarTask();
                        }
                    } else {
                        cancelActionBarTask();
                    }
                }, 0L, 20L); // repeat every 20 ticks
            }
        }
    }
    public void cancelActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }
    }

    private String parseColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private String parseHexColorCodes(String message) {
        Matcher matcher = HEX_COLOR_PATTERN.matcher(message);

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String hexColor = matcher.group(1);
            ChatColor color = ChatColor.of("#" + hexColor);
            matcher.appendReplacement(result, color.toString());
        }
        matcher.appendTail(result);

        return result.toString();
    }
}

