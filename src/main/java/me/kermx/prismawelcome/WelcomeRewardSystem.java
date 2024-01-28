package me.kermx.prismawelcome;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class WelcomeRewardSystem {
    private final Set<String> rewardedPlayers;
    private final int timeLimit;
    private final List<String> acceptedWelcomes;
    private final String rewardMode;
    private final List<String> welcomeRewards;
    private final Plugin plugin;

    public WelcomeRewardSystem(Plugin plugin, int timeLimit, List<String> acceptedWelcomes, String rewardMode, List<String> welcomeRewards) {
        this.plugin = plugin;
        this.timeLimit = timeLimit;
        this.acceptedWelcomes = acceptedWelcomes;
        this.rewardMode = rewardMode;
        this.welcomeRewards = welcomeRewards;
        this.rewardedPlayers = new HashSet<>();
    }

    public void processChatMessage(Player newPlayer, Player welcomingPlayer, String message) {
        long joinTime = newPlayer.getFirstPlayed() / 1000;
        String welcomingPlayerName = welcomingPlayer.getName();
        if (!rewardedPlayers.contains(welcomingPlayerName) && System.currentTimeMillis() / 1000 - joinTime <= timeLimit && containsAcceptedWord(message)) {
            executeRewards(welcomingPlayer);
            rewardedPlayers.add(welcomingPlayerName);

            Bukkit.getScheduler().runTaskLater(plugin, () -> rewardedPlayers.remove(welcomingPlayerName), 20L * timeLimit);
        }
    }

    private boolean containsAcceptedWord(String message) {
        String lowerCaseMessage = message.toLowerCase();
        return acceptedWelcomes.stream().anyMatch(acceptedWelcome -> lowerCaseMessage.contains(acceptedWelcome.toLowerCase()));
    }

    private void executeRewards(Player player) {
        if ("random".equalsIgnoreCase(rewardMode)) {
            executeRandomReward(player);
        } else if ("all".equalsIgnoreCase(rewardMode)) {
            executeAllRewards(player);
        }
    }

    private void executeRandomReward(Player player) {
        welcomeRewards.stream()
                .findAny()
                .ifPresent(randomCommand -> executeCommand(player, randomCommand));
    }

    private void executeAllRewards(Player player) {
        welcomeRewards.forEach(command -> executeCommand(player, command));
    }

    private void executeCommand(Player player, String command) {
        String finalCommand = command.replace("%player_name%", player.getName());

        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
    }

    public int getTimeLimit() {
        return timeLimit;
    }
}
