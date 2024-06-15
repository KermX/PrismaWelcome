package me.kermx.prismawelcome;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenUtil {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public boolean evaluateConditions(Player player, List<String> conditionList) {
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

    public boolean evaluateCondition(String placeholder, String operator, String value) {

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

    public int compareNumbers(String placeholder, String value) {
        try {
            double placeholderValue = Double.parseDouble(placeholder);
            double valueValue = Double.parseDouble(value);
            return Double.compare(placeholderValue, valueValue);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String parseColorCodes(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String parseHexColorCodes(String message) {
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
