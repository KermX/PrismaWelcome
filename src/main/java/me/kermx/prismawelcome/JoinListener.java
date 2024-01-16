package me.kermx.prismawelcome;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
        handlePlayerJoin(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        handlePlayerLeave(event.getPlayer(), event);
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

    private String parseMessages(List<String> messages, Player player, FileConfiguration config, String messageType) {
        if (messages.isEmpty()) {
            String defaultMessage = getDefaultMessages(config, messageType);
            return parseColorCodes(defaultMessage.replace("%player%", player.getName()));
        }

        String randomMessage = getRandomMessage(messages);
        return parseColorCodes(randomMessage.replace("%player%", player.getName()));
    }

    private void handlePlayerJoin(Player player, PlayerJoinEvent event) {
        PrismaWelcome plugin = PrismaWelcome.getPlugin(PrismaWelcome.class);
        FileConfiguration config = plugin.getPluginConfig();

        List<String> joinMessages = getCustomMessages(player, config, "join");
        String formattedMessage = parseMessages(joinMessages, player, config, "join");

        event.setJoinMessage(parseHexColorCodes(formattedMessage));
    }

    private void handlePlayerLeave(Player player, PlayerQuitEvent event) {
        PrismaWelcome plugin = PrismaWelcome.getPlugin(PrismaWelcome.class);
        FileConfiguration config = plugin.getPluginConfig();

        List<String> leaveMessages = getCustomMessages(player, config, "leave");
        String formattedMessage = parseMessages(leaveMessages, player, config, "leave");

        event.setQuitMessage(parseHexColorCodes(formattedMessage));
    }

    private String getDefaultMessages(FileConfiguration config, String messageType){
        List<String> defaultMessages = config.getStringList("prisma-welcome.default.messages." + messageType);
        return String.join("", defaultMessages);
    }

    private List<String> getCustomMessages(Player player, FileConfiguration config, String messageType) {
        List<String> customMessages = new ArrayList<>();

        for (String key : config.getConfigurationSection("prisma-welcome").getKeys(false)) {
            if (player.hasPermission("welcome." + key)) {
                customMessages.addAll(config.getStringList("prisma-welcome." + key + ".messages." + messageType));
            }
        }

        return customMessages;
    }

    private String getRandomMessage(List<String> messages) {
        if (messages.isEmpty()) {
            return "this should never happen"; // Handle the case when there are no messages
        }

        int index = random.nextInt(messages.size());
        return messages.get(index);
    }
}
