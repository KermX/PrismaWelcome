package me.kermx.prismawelcome;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigUtil {
    private final PrismaWelcome plugin;
    private final FileConfiguration config;

    //Silence Messages Settings
    // SJLAPC = Silence Join Leave Above Player Count
    public boolean enableSJLAPC;
    public int playerCountSJLAPC;

    public String silenceJoinLeavePermission;

    // Check for players that join in vanish mode
    public boolean enableCheckVanish;
    public boolean enableNotifyVanishJoiner;
    public String checkVanishPermission;
    public String vanishJoinNotificationMessage;
    public List<String> vanishJoinMetadata;
    public List<String> vanishJoinPlaceholders;

    // World blacklist
    public List<String> worldBlacklist;

    //Welcome Reward System
    public boolean WRActionBarEnabled;
    public String WRActionBarMessage;
    public int WRTimeLimit;
    public List<String> WRAcceptedWelcomes;
    public String WRRewardMode;
    public List<String> WRWelcomeRewards;

    //Hover Messages
    // HM = hover message
    // FJ = first join
    // J = join
    // L = leave
    public boolean enableFJHM;
    public List<String> FJHMText;
    public String FJHMClickAction;
    public String FJHMClickValue;

    public boolean enableJHM;
    public List<String> JHMText;
    public String JHMClickAction;
    public String JHMClickValue;

    public boolean enableLHM;
    public List<String> LHMText;
    public String LHMClickAction;
    public String LHMClickValue;

    //First Join Message Settings
    public List<String> FJMessages;
    public String FJSoundName;
    public float FJSoundVolume;
    public float FJSoundPitch;
    public String FJCommandMode;
    public List<String> FJCommands;

    //Default Join Leave
    public List<String> DefaultJoinMessage;
    public List<String> DefaultLeaveMessage;

    //First Join MOTD
    public List<String> FJMOTD;

    //Default MOTD
    public List<String> DefaultMOTD;

    //Custom Messages & MOTDs
    private Map<String, List<String>> customMessagesMap = new HashMap<>();
    private Map<String, Map<String, List<String>>> customMessagesByTypeMap = new HashMap<>();
    private Map<String, List<String>> customMOTDsMap = new HashMap<>();
    private Map<String, String> customMessagesPermissions = new HashMap<>();
    private Map<String, List<String>> customMessagesPlaceholders = new HashMap<>();
    private Map<String, String> customMOTDsPermissions = new HashMap<>();
    private Map<String, List<String>> customMOTDsPlaceholders = new HashMap<>();

    public ConfigUtil() {
        plugin = PrismaWelcome.getPlugin(PrismaWelcome.class);
        config = plugin.getPluginConfig();
    }

    public void loadConfig() {
        loadCustomMessages(config);
        loadCustomMOTDs(config);

        // Silencing Messages Settings
        enableSJLAPC = config.getBoolean("silenceJoinLeaveAbovePlayerCount.enable", false);
        playerCountSJLAPC = config.getInt("silenceJoinLeaveAbovePlayerCount.playerCount", 25);
        silenceJoinLeavePermission = config.getString("silenceJoinLeavePermission", "prismawelcome.silent");

        // Check for players that join in vanish mode
        enableCheckVanish = config.getBoolean("checkVanish.enable", false);
        enableNotifyVanishJoiner = config.getBoolean("checkVanish.notifyJoiner", false);
        checkVanishPermission = config.getString("checkVanish.permission", "prismawelcome.staff");
        vanishJoinNotificationMessage = config.getString("checkVanish.notificationMessage", "You are vanished!");
        vanishJoinMetadata = config.getStringList("checkVanish.metadata");
        vanishJoinPlaceholders = config.getStringList("checkVanish.placeholders");

        // World Blacklist
        worldBlacklist = config.getStringList("worldBlacklist");

        // Welcome Reward System
        WRActionBarEnabled = config.getBoolean("welcomeRewarding.actionBarEnabled", true);
        WRActionBarMessage = config.getString("welcomeRewarding.actionBarReminder", "Welcome new player %player_name% in the next %remaining_time% to get a reward!");
        WRTimeLimit = config.getInt("welcomeRewarding.timeLimit", 30);
        WRAcceptedWelcomes = config.getStringList("welcomeRewarding.acceptedWelcomes");
        WRRewardMode = config.getString("welcomeRewarding.rewardMode", "random");
        WRWelcomeRewards = config.getStringList("welcomeRewarding.welcomeRewards");

        // Hover Messages Settings
        enableFJHM = config.getBoolean("hoverMessages.firstJoin.enable", false);
        FJHMText = config.getStringList("hoverMessages.firstJoin.hoverText");
        FJHMClickAction = config.getString("hoverMessages.firstJoin.clickAction", "NONE");
        FJHMClickValue = config.getString("hoverMessages.firstJoin.clickValue", "");

        enableJHM = config.getBoolean("hoverMessages.join.enable", false);
        JHMText = config.getStringList("hoverMessages.join.hoverText");
        JHMClickAction = config.getString("hoverMessages.join.clickAction", "NONE");
        JHMClickValue = config.getString("hoverMessages.join.clickValue", "");

        enableLHM = config.getBoolean("hoverMessages.leave.enable", false);
        LHMText = config.getStringList("hoverMessages.leave.hoverText");
        LHMClickAction = config.getString("hoverMessages.leave.clickAction", "NONE");
        LHMClickValue = config.getString("hoverMessages.leave.clickValue", "");

        // First Join Message Settings
        FJMessages = config.getStringList("firstJoinMessage.messages");
        FJSoundName = config.getString("firstJoinMessage.sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        FJSoundVolume = (float) config.getDouble("firstJoinMessage.sound.volume", 1.0);
        FJSoundPitch = (float) config.getDouble("firstJoinMessage.sound.pitch", 1.0);
        FJCommandMode = config.getString("firstJoinMessage.commandMode", "all");
        FJCommands = config.getStringList("firstJoinMessage.commands");

        // Default Join/Leave Messages Settings
        DefaultJoinMessage = config.getStringList("defaultMessage.messages.join");
        DefaultLeaveMessage = config.getStringList("defaultMessage.messages.leave");

        // First Join MOTD Settings
        FJMOTD = config.getStringList("firstJoinMOTD.message");

        // Default MOTD Settings
        DefaultMOTD = config.getStringList("defaultMOTD.message");
    }

    private void loadCustomMessages(FileConfiguration config) {
        for (String key : config.getConfigurationSection("customMessages").getKeys(false)) {
            customMessagesPermissions.put(key, config.getString("customMessages." + key + ".permission"));
            customMessagesPlaceholders.put(key, config.getStringList("customMessages." + key + ".placeholders"));
            customMessagesByTypeMap.put(key, new HashMap<>());
            for (String messageType : config.getConfigurationSection("customMessages." + key + ".messages").getKeys(false)) {
                customMessagesByTypeMap.get(key).put(messageType, config.getStringList("customMessages." + key + ".messages." + messageType));
            }
        }
    }

    private void loadCustomMOTDs(FileConfiguration config) {
        for (String key : config.getConfigurationSection("customMOTDs").getKeys(false)) {
            customMOTDsPermissions.put(key, config.getString("customMOTDs." + key + ".permission"));
            customMOTDsPlaceholders.put(key, config.getStringList("customMOTDs." + key + ".placeholders"));
            customMOTDsMap.put(key, config.getStringList("customMOTDs." + key + ".message"));
        }
    }

    public List<String> getDefaultMessages(String messageType){
        if (messageType == "join"){
            return DefaultJoinMessage;
        }
        if (messageType == "leave"){
            return DefaultLeaveMessage;
        }
        return null;
    }

    public List<String> getCustomMessages(String key, String messageType) {
        return customMessagesByTypeMap.getOrDefault(key, new HashMap<>()).getOrDefault(messageType, new ArrayList<>());
    }

    public List<String> getCustomMOTD(String key) {
        return customMOTDsMap.getOrDefault(key, new ArrayList<>());
    }

    public List<String> getDefaultMOTD() {
        return DefaultMOTD;
    }

    public String getPermissionNodeForCustomMessage(String key) {
        return customMessagesPermissions.get(key);
    }

    public List<String> getPlaceholdersForCustomMessage(String key) {
        return customMessagesPlaceholders.get(key);
    }

    public Set<String> getCustomMessagesKeys() {
        return customMessagesByTypeMap.keySet();
    }

    public String getPermissionNodeForCustomMOTD(String key) {
        return customMOTDsPermissions.get(key);
    }

    public List<String> getPlaceholdersForCustomMOTD(String key) {
        return customMOTDsPlaceholders.get(key);
    }

    public Set<String> getCustomMOTDsKeys() {
        return customMOTDsMap.keySet();
    }
}
