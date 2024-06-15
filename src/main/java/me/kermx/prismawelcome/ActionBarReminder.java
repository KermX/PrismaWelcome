package me.kermx.prismawelcome;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class ActionBarReminder {

    private final PrismaWelcome plugin;
    private final WelcomeRewardSystem welcomeRewardSystem;
    private BukkitTask actionBarTask;
    private final GenUtil genUtil;

    public ActionBarReminder(PrismaWelcome plugin, WelcomeRewardSystem welcomeRewardSystem) {
        this.plugin = plugin;
        this.welcomeRewardSystem = welcomeRewardSystem;
        genUtil = new GenUtil();
    }

    public void displayActionBar(List<Player> players, Player newPlayer) {
        if (!plugin.getConfigUtil().WRActionBarEnabled || newPlayer == null) {
            return;
        }

        String reminderMessage = plugin.getConfigUtil().WRActionBarMessage;
        if (reminderMessage.isEmpty()) {
            return;
        }

        // Schedule a repeating task to update the action bar reminder every second
        actionBarTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            int remainingTime = getRemainingTime(newPlayer);

            // Check if there is remaining time
            if (remainingTime > 0) {
                sendActionBarReminder(players, newPlayer, reminderMessage, remainingTime);
            } else {
                cancelActionBarTask();
            }
        }, 0L, 20L); // repeat every 20 ticks
    }

    private int getRemainingTime(Player newPlayer) {
        return welcomeRewardSystem.getTimeLimit() - (int) ((System.currentTimeMillis() - newPlayer.getFirstPlayed()) / 1000);
    }

    private void sendActionBarReminder(List<Player> players, Player newPlayer, String reminderMessage, int remainingTime) {
        for (Player player : players) {
            if (player != newPlayer) {  // Exclude the newPlayer
                String formattedMessage = reminderMessage
                        .replace("%player_name%", newPlayer.getName())
                        .replace("%remaining_time%", String.valueOf(remainingTime));

                formattedMessage = genUtil.parseHexColorCodes(formattedMessage);

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(genUtil.parseColorCodes(formattedMessage)));
            }
        }
    }

    public void cancelActionBarTask() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }
    }
}

