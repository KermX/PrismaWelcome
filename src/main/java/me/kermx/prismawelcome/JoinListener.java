package me.kermx.prismawelcome;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
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
import java.util.Set;
import java.util.stream.Collectors;

public class JoinListener implements Listener {
    private final WelcomeRewardSystem welcomeRewardSystem;
    private final ActionBarReminder actionBarReminder;
    private final GenUtil genUtil;
    private Player newPlayer;
    private final PrismaWelcome plugin;
    private static final Random random = new Random();

    public JoinListener() {
        plugin = PrismaWelcome.getPlugin(PrismaWelcome.class);
        genUtil = new GenUtil();
        welcomeRewardSystem = new WelcomeRewardSystem(
                plugin,
                plugin.getConfigUtil().WRTimeLimit,
                plugin.getConfigUtil().WRAcceptedWelcomes,
                plugin.getConfigUtil().WRRewardMode,
                plugin.getConfigUtil().WRWelcomeRewards
        );
        this.actionBarReminder = new ActionBarReminder(plugin, welcomeRewardSystem);
    }

    private void runWithDelay(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
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
        event.setJoinMessage(null);
        runWithDelay(() -> {
            Player player = event.getPlayer();
            int currentOnlinePlayers = Bukkit.getOnlinePlayers().size();

            if (shouldSilenceJoinLeave(currentOnlinePlayers, player)) {
                return;
            }

            if (!player.hasPlayedBefore()) {
                handleFirstJoin(player, event, currentOnlinePlayers);
            } else {
                handleRegularJoin(player, event, currentOnlinePlayers);
            }
        }, 5L );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
        int currentOnlinePlayers = Bukkit.getOnlinePlayers().size();

        if (shouldSilenceJoinLeave(currentOnlinePlayers, event.getPlayer())) {
            return;
        }

        handlePlayerEvent(event.getPlayer(), event, "leave");
    }

    private boolean shouldSilenceJoinLeave(int currentOnlinePlayers, Player player) {
        boolean silenceAbovePlayerCount = plugin.getConfigUtil().enableSJLAPC &&
                currentOnlinePlayers > plugin.getConfigUtil().playerCountSJLAPC;
        boolean hasSilentPermission = player.hasPermission(plugin.getConfigUtil().silenceJoinLeavePermission);

        boolean shouldSilence = silenceAbovePlayerCount || hasSilentPermission;


        if (plugin.getConfigUtil().enableCheckVanish && player.hasPermission(plugin.getConfigUtil().checkVanishPermission)) {
            boolean hasVanishMetadata = plugin.getConfigUtil().vanishJoinMetadata.stream()
                    .anyMatch(key -> player.hasMetadata(key) && player.getMetadata(key).stream().anyMatch(MetadataValue::asBoolean));
            if (hasVanishMetadata || plugin.getConfigUtil().vanishJoinPlaceholders.stream().anyMatch(player::hasPermission)) {
                shouldSilence = true;
            }
        }
        if (plugin.getConfigUtil().worldBlacklist.contains(player.getWorld().getName())) {
            shouldSilence = true;
        }

        if (plugin.getConfigUtil().enableNotifyVanishJoiner) {
            player.sendMessage(plugin.getConfigUtil().vanishJoinNotificationMessage);
        }

        return shouldSilence;
    }

    private void handleMessage(Player player, String messageType, String formattedMessage){

        List<String> hoverText = null;
        String clickAction = null;
        String clickValue = null;

        switch (messageType){
            case "firstJoin":
                hoverText = plugin.getConfigUtil().FJHMText;
                clickAction = plugin.getConfigUtil().FJHMClickAction;
                clickValue = plugin.getConfigUtil().FJHMClickValue;
                break;
            case "join":
                hoverText = plugin.getConfigUtil().JHMText;
                clickAction = plugin.getConfigUtil().JHMClickAction;
                clickValue = plugin.getConfigUtil().JHMClickValue;
                break;
            case "leave":
                hoverText = plugin.getConfigUtil().LHMText;
                clickAction = plugin.getConfigUtil().LHMClickAction;
                clickValue = plugin.getConfigUtil().LHMClickValue;
                break;
            default:
                break;
        }

        BaseComponent[] messageComponent = TextComponent.fromLegacyText(formattedMessage);

        if (hoverText != null && !hoverText.isEmpty()){
            BaseComponent[] hoverComponentBuilder = new BaseComponent[hoverText.size() * 2 - 1];

            for (int i = 0; i < hoverText.size(); i++) {
                String line = parsePlaceholders(player, hoverText.get(i));
                line = genUtil.parseHexColorCodes(genUtil.parseColorCodes(line));
                hoverComponentBuilder[i * 2] = new TextComponent(TextComponent.fromLegacyText(line));
                if (i < hoverText.size() - 1) {
                    hoverComponentBuilder[i * 2 + 1] = new TextComponent("\n");
                }
            }

            if (!"NONE".equals(clickAction)) {
                ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.valueOf(clickAction.toUpperCase()), parsePlaceholders(player,clickValue));
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

    private void handleFirstJoin(Player player, PlayerJoinEvent event, int currentOnlinePlayers) {
        List<String> firstJoinMessage = plugin.getConfigUtil().FJMessages;

        if (!firstJoinMessage.isEmpty()) {
            handleSilentJoinLeave(event, currentOnlinePlayers);

            String formattedMessage = parseMessages(firstJoinMessage, player);
            event.setJoinMessage(null);

            handleMOTD(player, plugin.getConfigUtil().FJMOTD);

            newPlayer = player;
            Bukkit.getScheduler().runTaskLater(plugin, () -> newPlayer = null, 20L * welcomeRewardSystem.getTimeLimit());

            if (plugin.getConfigUtil().WRActionBarEnabled) {
                List<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p != newPlayer)
                        .collect(Collectors.toList());

                actionBarReminder.displayActionBar(onlinePlayers, newPlayer);
            }
            handleMessage(player, "firstJoin", formattedMessage);

            executeCommands(player);

            playSounds();
        }
    }

    private void executeCommands(Player player) {
        List<String> commands = plugin.getConfigUtil().FJCommands;
        String commandMode = plugin.getConfigUtil().FJCommandMode;

        if (!commands.isEmpty()) {
            if ("all".equalsIgnoreCase(commandMode)) {
                for (String command : commands) {
                    command = command.replace("%player_name%", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                }
            } else if ("random".equalsIgnoreCase(commandMode)) {
                String randomCommand = commands.get(random.nextInt(commands.size()));
                randomCommand = randomCommand.replace("%player_name%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), randomCommand);
            } else {
                plugin.getLogger().warning("Invalid commandMode specified in configuration: " + commandMode);
            }
        }
    }

    private void playSounds() {
        String soundName = plugin.getConfigUtil().FJSoundName;
        float volume = plugin.getConfigUtil().FJSoundVolume;
        float pitch = plugin.getConfigUtil().FJSoundPitch;

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

    private void handleRegularJoin(Player player, PlayerJoinEvent event, int currentOnlinePlayers) {
        handleSilentJoinLeave(event, currentOnlinePlayers);
        handlePlayerEvent(player, event, "join");
        handleRandomCustomMOTD(player);
    }

    private void handleSilentJoinLeave(PlayerEvent event, int currentOnlinePlayers) {
        if (shouldSilenceJoinLeave(currentOnlinePlayers, event.getPlayer())) {
            if (event instanceof PlayerJoinEvent) {
                ((PlayerJoinEvent) event).setJoinMessage(null);
            } else if (event instanceof PlayerQuitEvent) {
                ((PlayerQuitEvent) event).setQuitMessage(null);
            }
        }
    }

    private void handlePlayerEvent(Player player, PlayerEvent event, String eventType) {
        List<String> customMessages = getCustomMessages(player, eventType);
        String formattedMessage = parseMessages(customMessages, player);

        if (event instanceof PlayerJoinEvent) {
            ((PlayerJoinEvent) event).setJoinMessage(null);
            handleMessage(player, "join", formattedMessage);
        } else if (event instanceof PlayerQuitEvent) {
            ((PlayerQuitEvent) event).setQuitMessage(null);
            handleMessage(player, "leave", formattedMessage);
        }
    }


    private String parseMessages(List<String> messages, Player player) {
        String message = getRandomMessage(messages);
        return genUtil.parseHexColorCodes(genUtil.parseColorCodes(PlaceholderAPI.setPlaceholders(player, message)));
    }


    private List<String> getCustomMessages(Player player, String messageType) {
        List<String> customMessages = new ArrayList<>();
        Set<String> keys = plugin.getConfigUtil().getCustomMessagesKeys();

        for (String key : keys) {
            String permissionNode = plugin.getConfigUtil().getPermissionNodeForCustomMessage(key);
            List<String> conditionList = plugin.getConfigUtil().getPlaceholdersForCustomMessage(key);

            // Check if permissionNode is not specified or the player has the permission
            boolean hasPermission = permissionNode == null || permissionNode.isEmpty() || player.hasPermission(permissionNode);

            // Check if conditionList is empty or player meets the conditions
            boolean shouldIncludeMessages = hasPermission && genUtil.evaluateConditions(player, conditionList);

            if (shouldIncludeMessages) {
                List<String> messagesToAdd = plugin.getConfigUtil().getCustomMessages(key, messageType);
                customMessages.addAll(messagesToAdd);
            }
        }
        if (customMessages.isEmpty()) {
            customMessages.addAll(plugin.getConfigUtil().getDefaultMessages(messageType));
        }
        return customMessages;
    }

    private void handleRandomCustomMOTD(Player player) {
        List<String> accessibleMOTDs = getAccessibleCustomMOTDs(player);
        if (accessibleMOTDs.isEmpty()) {
            handleMOTD(player, plugin.getConfigUtil().getDefaultMOTD());
        } else {
            String randomMOTDKey = accessibleMOTDs.get(new Random().nextInt(accessibleMOTDs.size()));
            List<String> motdMessages = plugin.getConfigUtil().getCustomMOTD(randomMOTDKey);
            handleMOTD(player, motdMessages);
        }
    }

    private List<String> getAccessibleCustomMOTDs(Player player) {
        List<String> accessibleMOTDs = new ArrayList<>();
        Set<String> keys = plugin.getConfigUtil().getCustomMOTDsKeys();

        for (String key : keys) {
            String permissionNode = plugin.getConfigUtil().getPermissionNodeForCustomMOTD(key);
            List<String> conditionList = plugin.getConfigUtil().getPlaceholdersForCustomMOTD(key);

            boolean hasPermission = permissionNode == null || permissionNode.isEmpty() || player.hasPermission(permissionNode);

            boolean shouldDisplayMOTD = hasPermission && genUtil.evaluateConditions(player, conditionList);

            if (shouldDisplayMOTD) {
                accessibleMOTDs.add(key);
            }
        }
        return accessibleMOTDs;
    }

    private void handleMOTD(Player player, List<String> motdMessages) {
        for (String line : motdMessages) {
            player.sendMessage(genUtil.parseHexColorCodes(genUtil.parseColorCodes(PlaceholderAPI.setPlaceholders(player, line))));
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