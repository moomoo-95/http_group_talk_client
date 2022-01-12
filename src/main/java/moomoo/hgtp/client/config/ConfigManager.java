package moomoo.hgtp.client.config;

public class ConfigManager {

    public static final int USER_ID_SIZE = 8;
    public static final int ROOM_ID_SIZE = 12;

    // SECTION
    private static final String SECTION_COMMON = "COMMON";
    private static final String SECTION_NETWORK = "NETWORK";
    private static final String SECTION_HGTP = "HGTP";

    private static ConfigManager configManager = null;

    public ConfigManager() {
        // nothing
    }

    public static ConfigManager getInstance() {
        if (configManager == null) {
            configManager = new ConfigManager();
        }
        return configManager;
    }
}