package me.kermx.prismawelcome;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.MetadataValue;

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
        boolean silenceAbovePlayerCount = config.getBoolean("silenceJoinLeaveAbovePlayerCount.enable", false)
                && currentOnlinePlayers > config.getInt("silenceJoinLeaveAbovePlayerCount.playerCount", 25);
        boolean hasSilentPermission = player.hasPermission(config.getString("silenceJoinLeavePermission", "prismawelcome.silent"));

        boolean shouldSilence = silenceAbovePlayerCount || hasSilentPermission;

        if (config.getBoolean("checkVanish.enable", false) && player.hasPermission(config.getString("checkVanish.permission", "prismawelcome.staff"))) {
            List<String> placeholders = config.getStringList("checkVanish.placeholders");
            if (evaluateConditions(player, placeholders)) {
                    shouldSilence = true;
            }
            for (MetadataValue meta : player.getMetadata(config.getString("checkVanish.metadata", "vanished"))){
                if (meta.asBoolean()){
                    shouldSilence = true;
                }
            }
        }
         List<String> worldBlacklist = config.getStringList("worldBlacklist");
        if (worldBlacklist.contains(player.getWorld().getName())){
            shouldSilence = true;
        }
        if (shouldSilence || config.getBoolean("checkVanish.notifyJoiner", false)){
            player.sendMessage(config.getString("checkVanish.notificationMessage", "You are vanished!"));
        }
        return shouldSilence;
    }

    private void handleMessage(Player player, FileConfiguration config, String messageType, String formattedMessage){

        List<String> hoverText = config.getStringList("hoverMessages." + messageType + ".hoverText");
        String clickAction = config.getString("hoverMessages." + messageType + ".clickAction");
        String clickValue = config.getString("hoverMessages." + messageType + ".clickValue");

        BaseComponent[] messageComponent = TextComponent.fromLegacyText(formattedMessage);

        if (config.getBoolean("hoverMessages." + messageType + ".enable", false) && hoverText != null && !hoverText.isEmpty()){

            BaseComponent[] hoverComponentBuilder = new BaseComponent[hoverText.size() * 2 - 1];

            for (int i = 0; i < hoverText.size(); i++) {
                String line = parsePlaceholders(player, hoverText.get(i));
                line = parseHexColorCodes(parseColorCodes(line));
                hoverComponentBuilder[i * 2] = new TextComponent(TextComponent.fromLegacyText(line));
                if (i < hoverText.size() - 1) {
                    hoverComponentBuilder[i * 2 + 1] = new TextComponent("\n");
                }
            }

            if (clickAction != null && !clickAction.equals("NONE")) {
                ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.valueOf(clickAction.toUpperCase()), clickValue);
                for (BaseComponent component : messageComponent){
                    component.setClickEvent(clickEvent);
                }
            }
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverComponentBuilder));
            for (BaseComponent component : messageComponent){
                component.setHoverEvent(hoverEvent);
            }
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()){
            onlinePlayer.spigot().sendMessage(messageComponent);
        }
    }

    private String parsePlaceholders(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
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
            handleMessage(player, config, "firstJoin", formattedMessage);

            executeCommands(player, config);

            playSounds(config);
        }
    }

    private void executeCommands(Player player, FileConfiguration config) {
        List<String> commands = config.getStringList("firstJoinMessage.commands");
        String commandMode = config.getString("firstJoinMessage.commandMode", "all").toLowerCase();

        if (!commands.isEmpty()) {
            if (commandMode.equals("all")) {
                for (String command : commands) {
                    command = command.replace("%player_name%", player.getName());

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            } else if (commandMode.equals("random")) {
                Random random = new Random();
                String randomCommand = commands.get(random.nextInt(commands.size()));
                randomCommand = randomCommand.replace("%player_name%", player.getName());

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), randomCommand);
            } else {
                plugin.getLogger().warning("Invalid commandMode specified in configuration: " + commandMode);
            }
        }
    }

    private void playSounds(FileConfiguration config){
        String soundName = config.getString("firstJoinMessage.sound.name");
        float volume = (float) config.getDouble("firstJoinMessage.sound.volume", 1.0);
        float pitch = (float) config.getDouble("firstJoinMessage.sound.pitch", 1.0);

        Sound sound;
        try {
            sound = Sound.valueOf(soundName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound name specified in configuration: " + soundName);
            return;
        }
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.playSound(onlinePlayer.getLocation(), sound, volume, pitch);
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
            ((PlayerJoinEvent) event).setJoinMessage(null);
            handleMessage(player, config, "join", formattedMessage);
        } else if (event instanceof PlayerQuitEvent) {
            ((PlayerQuitEvent) event).setQuitMessage(null);
            handleMessage(player, config, "leave", formattedMessage);
        }
    }


    private String parseMessages(List<String> messages, Player player) {
        String message = getRandomMessage(messages);
        return parseHexColorCodes(parseColorCodes(PlaceholderAPI.setPlaceholders(player, message)));
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