package me.kermx.prismawelcome;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JoinListener implements Listener {

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Random random = new Random();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerEvent(event.getPlayer(), event, "join");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerEvent(event.getPlayer(), event, "leave");
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

        for (String key : config.getConfigurationSection("prisma-welcome").getKeys(false)) {
            String permissionNode = config.getString("prisma-welcome." + key + ".permission");
            List<String> conditionList = config.getStringList("prisma-welcome." + key + ".placeholders");

            // Check if permissionNode is not specified or the player has the permission
            boolean hasPermission = permissionNode == null || permissionNode.isEmpty() || player.hasPermission(permissionNode);

            // Check if conditionList is empty or player meets the conditions
            boolean shouldIncludeMessages = hasPermission && evaluateConditions(player, conditionList);

            if (shouldIncludeMessages) {
                List<String> messagesToAdd = config.getStringList("prisma-welcome." + key + ".messages." + messageType);
                customMessages.addAll(messagesToAdd);
            }
        }
        return customMessages;
    }

    private boolean evaluateConditions(Player player, List<String> conditionList) {
        Bukkit.getLogger().info("Executing evaluateConditions method");
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

                if (evaluateCondition(placeholderValue, operator, valueValue) || evaluateCondition(valueValue, operator, placeholderValue)) {
                    return true;
                }
            }
        }
        return false;
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
            return "this should never happen";
        }
        int index = random.nextInt(messages.size());
        return messages.get(index);
    }
}