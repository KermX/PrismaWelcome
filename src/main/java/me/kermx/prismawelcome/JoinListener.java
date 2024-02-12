package me.kermx.prismawelcome;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JoinListener implements Listener {

    private final WelcomeRewardSystem welcomeRewardSystem;
    private final ActionBarReminder actionBarReminder;
    private Player newPlayer;
    private final PrismaWelcome plugin;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Random random = new Random();

    public JoinListener() {
        plugin = PrismaWelcome.getPlugin(PrismaWelcome.class);
        FileConfiguration config = plugin.getPluginConfig();
        welcomeRewardSystem = loadWelcomeRewardSystem(config);
        actionBarReminder = new ActionBarReminder(plugin, welcomeRewardSystem);
    }

    private WelcomeRewardSystem loadWelcomeRewardSystem(FileConfiguration config) {
        int timeLimit = config.getInt("welcomeRewarding.timeLimit", 30);
        List<String> acceptedWelcomes = config.getStringList("welcomeRewarding.acceptedWelcomes");
        String rewardMode = config.getString("welcomeRewarding.rewardMode", "random");
        List<String> welcomeRewards = config.getStringList("welcomeRewarding.welcomeRewards");

        return new WelcomeRewardSystem(plugin, timeLimit, acceptedWelcomes, rewardMode, welcomeRewards);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player welcomingPlayer = event.getPlayer();
        String chatMessage = event.getMessage();

        if (newPlayer != null && newPlayer != welcomingPlayer) {
            welcomeRewardSystem.processChatMessage(newPlayer, welcomingPlayer, chatMessage);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FileConfiguration config = plugin.getPluginConfig();
        int currentOnlinePlayers = Bukkit.getOnlinePlayers().size();

        if (shouldSilenceJoinLeave(config, currentOnlinePlayers, player)) {
            event.setJoinMessage(null);
            return;
        }

        if (!player.hasPlayedBefore()) {
            handleFirstJoin(player, event, config, currentOnlinePlayers);
        } else {
            handleRegularJoin(player, event, config, currentOnlinePlayers);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FileConfiguration config = plugin.getPluginConfig();
        int currentOnlinePlayers = Bukkit.getOnlinePlayers().size();

        if (shouldSilenceJoinLeave(config, currentOnlinePlayers, event.getPlayer())) {
            event.setQuitMessage(null);
            return;
        }

        handlePlayerEvent(event.getPlayer(), event, "leave");
    }

    private boolean shouldSilenceJoinLeave(FileConfiguration config, int currentOnlinePlayers, Player player) {
        return config.getBoolean("silenceJoinLeaveAbovePlayerCount.enable", false)
                && currentOnlinePlayers > config.getInt("silenceJoinLeaveAbovePlayerCount.playerCount", 25)
                || player.hasPermission(config.getString("silenceJoinLeavePermission", "prismawelcome.silent"));
    }

    private void handleFirstJoin(Player player, PlayerJoinEvent event, FileConfiguration config, int currentOnlinePlayers) {
        List<String> firstJoinMessage = config.getStringList("firstJoinMessage.messages");

        if (!firstJoinMessage.isEmpty()) {
            handleSilentJoinLeave(event, config, currentOnlinePlayers);

            String formattedMessage = parseMessages(firstJoinMessage, player);
            event.setJoinMessage(null);

            handleMOTD(player, config.getStringList("firstJoinMOTD.message"));

            newPlayer = player;
            Bukkit.getScheduler().runTaskLater(plugin, () -> newPlayer = null, 20L * welcomeRewardSystem.getTimeLimit());

            if (config.getBoolean("welcomeRewarding.actionBarEnabled", false)) {
                List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p != newPlayer)
                        .collect(Collectors.toList());

                actionBarReminder.displayActionBar(onlinePlayers, newPlayer, config);
            }

            List<String> hoverText = config.getStringList("hoverMessages.firstJoin.hoverText");
            String clickAction = config.getString("hoverMessages.firstJoin.clickAction");
            String clickValue = config.getString("hoverMessages.firstJoin.clickValue");

            TextComponent welcomeMessage = new TextComponent(parseHexColorCodes(parseColorCodes(formattedMessage)));
            if (config.getBoolean("hoverMessages.firstJoin.enable", true) && hoverText != null && !hoverText.isEmpty()) {
                // this part is dogshit but I just want to see how to make it work before implementing in its own method
                TextComponent hoverComponent = new TextComponent();

                for (int i = 0; i < hoverText.size(); i++) {
                    String line = hoverText.get(i);
                    hoverComponent.addExtra(new TextComponent(parseHexColorCodes(parseColorCodes(line))));

                    if (i < hoverText.size() - 1) {
                        hoverComponent.addExtra("\n");
                    }
                }

                if (!(clickAction == null) && !clickAction.equals("NONE")) {
                    welcomeMessage.setClickEvent(new ClickEvent(ClickEvent.Action.valueOf(clickAction.toUpperCase()), clickValue));
                }
                BaseComponent[] hoverComponentBuilder = new ComponentBuilder(hoverComponent).create();
                welcomeMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponentBuilder)));
            }

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()){
                onlinePlayer.spigot().sendMessage(welcomeMessage);
            }
        }
    }

    private void handleRegularJoin(Player player, PlayerJoinEvent event, FileConfiguration config, int currentOnlinePlayers) {
        handleSilentJoinLeave(event, config, currentOnlinePlayers);
        handlePlayerEvent(player, event, "join");
        handleRandomCustomMOTD(player, config);
    }

    private void handleSilentJoinLeave(PlayerEvent event, FileConfiguration config, int currentOnlinePlayers) {
        if (shouldSilenceJoinLeave(config, currentOnlinePlayers, event.getPlayer())) {
            if (event instanceof PlayerJoinEvent) {
                ((PlayerJoinEvent) event).setJoinMessage(null);
            } else if (event instanceof PlayerQuitEvent) {
                ((PlayerQuitEvent) event).setQuitMessage(null);
            }
        }
    }

    private String parseColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

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

    private void handlePlayerEvent(Player player, PlayerEvent event, String eventType) {
        PrismaWelcome plugin = PrismaWelcome.getPlugin(PrismaWelcome.class);
        FileConfiguration config = plugin.getPluginConfig();

        List<String> customMessages = getCustomMessages(player, config, eventType);
        String formattedMessage = parseMessages(customMessages, player);

        if (event instanceof PlayerJoinEvent) {
            ((PlayerJoinEvent) event).setJoinMessage(parseHexColorCodes(formattedMessage));
        } else if (event instanceof PlayerQuitEvent) {
            ((PlayerQuitEvent) event).setQuitMessage(parseHexColorCodes(formattedMessage));
        }
    }


    private String parseMessages(List<String> messages, Player player) {
        String message = getRandomMessage(messages);
        return parseColorCodes(PlaceholderAPI.setPlaceholders(player, message));
    }


    private List<String> getCustomMessages(Player player, FileConfiguration config, String messageType) {
        List<String> customMessages = new ArrayList<>();

        for (String key : config.getConfigurationSection("customMessages").getKeys(false)) {
            String permissionNode = config.getString("customMessages." + key + ".permission");
            List<String> conditionList = config.getStringList("customMessages." + key + ".placeholders");

            // Check if permissionNode is not specified or the player has the permission
            boolean hasPermission = permissionNode == null || permissionNode.isEmpty() || player.hasPermission(permissionNode);

            // Check if conditionList is empty or player meets the conditions
            boolean shouldIncludeMessages = hasPermission && evaluateConditions(player, conditionList);

            if (shouldIncludeMessages) {
                List<String> messagesToAdd = config.getStringList("customMessages." + key + ".messages." + messageType);
                customMessages.addAll(messagesToAdd);
            }
        }
        if (customMessages.isEmpty()) {
            customMessages.addAll(config.getStringList("defaultMessage.messages." + messageType));
        }
        return customMessages;
    }

    private void handleRandomCustomMOTD(Player player, FileConfiguration config) {
        List<String> accessibleMOTDs = getAccessibleCustomMOTDs(player, config);
        if (accessibleMOTDs.isEmpty()) {
            handleMOTD(player, config.getStringList("defaultMOTD.message"));
        } else {
            String randomMOTDKey = accessibleMOTDs.get(new Random().nextInt(accessibleMOTDs.size()));
            List<String> motdMessages = config.getStringList("customMOTDs." + randomMOTDKey + ".message");
            handleMOTD(player, motdMessages);
        }
    }

    private List<String> getAccessibleCustomMOTDs(Player player, FileConfiguration config) {
        List<String> accessibleMOTDs = new ArrayList<>();

        for (String key : config.getConfigurationSection("customMOTDs").getKeys(false)) {
            String permissionNode = config.getString("customMOTDs." + key + ".permission");
            List<String> conditionList = config.getStringList("customMOTDs." + key + ".placeholders");

            boolean hasPermission = permissionNode == null || permissionNode.isEmpty() || player.hasPermission(permissionNode);

            boolean shouldDisplayMOTD = hasPermission && evaluateConditions(player, conditionList);

            if (shouldDisplayMOTD) {
                accessibleMOTDs.add(key);
            }
        }
        return accessibleMOTDs;
    }

    private void handleMOTD(Player player, List<String> motdMessages) {
        for (String line : motdMessages) {
            player.sendMessage(parseHexColorCodes(parseColorCodes(PlaceholderAPI.setPlaceholders(player, line))));
        }
    }

    private boolean evaluateConditions(Player player, List<String> conditionList) {
        if (conditionList == null || conditionList.isEmpty()) {
            return true;
        }

        for (String condition : conditionList) {
            String[] parts = condition.split(" ", 3);

            if (parts.length == 3) {
                String placeholder = parts[0];
                String operator = parts[1];
                String value = parts[2];

                String placeholderValue = PlaceholderAPI.setPlaceholders(player, placeholder);
                String valueValue = PlaceholderAPI.setPlaceholders(player, value);

                if (!evaluateCondition(placeholderValue, operator, valueValue)) {
                    return false; // If any condition is not satisfied, return false
                }
            }
        }
        return true; // All conditions are satisfied
    }

    private boolean evaluateCondition(String placeholder, String operator, String value) {

        switch (operator) {
            case "==":
                return placeholder.equals(value);
            case ">=":
                return compareNumbers(placeholder, value) >= 0;
            case "<=":
                return compareNumbers(placeholder, value) <= 0;
            case ">":
                return compareNumbers(placeholder, value) > 0;
            case "<":
                return compareNumbers(placeholder, value) < 0;
            case "matches":
                return placeholder.matches(value);
            case "contains":
                return placeholder.contains(value);
            default:
                return false;
        }
    }

    private int compareNumbers(String placeholder, String value) {
        try {
            double placeholderValue = Double.parseDouble(placeholder);
            double valueValue = Double.parseDouble(value);
            return Double.compare(placeholderValue, valueValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private String getRandomMessage(List<String> messages) {
        if (messages.isEmpty()) {
            return ChatColor.RED + "Join Message Error!";
        }
        int index = random.nextInt(messages.size());
        return messages.get(index);
    }
}