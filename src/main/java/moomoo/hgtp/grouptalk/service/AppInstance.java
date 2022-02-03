package moomoo.hgtp.grouptalk.service;

import moomoo.hgtp.grouptalk.config.ConfigManager;
import moomoo.hgtp.grouptalk.util.CnameGenerator;
import org.apache.commons.net.ntp.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;


public class AppInstance {

    private static final Logger log = LoggerFactory.getLogger(AppInstance.class);

    public static final String ALGORITHM = "MD5";
    public static final String MD5_REALM = "HGTP_SERVICE";
    public static final String MD5_HASH_KEY = "950817";

    public static final short MAGIC_COOKIE = 0x4853; // HS
    public static final int SERVER_MODE = 0;
    public static final int CLIENT_MODE = 1;
    public static final int PROXY_MODE = 2;
    public static final int USER_ID_SIZE = 8;
    public static final int ROOM_ID_SIZE = 12;
    public static final int SEQ_INCREMENT = 1;
    public static final long SERVER_SESSION_ID = 84;

    private static AppInstance appInstance = null;

    // 프로그램 모드 init (-1) , server (0) , client (1) , proxy (2)
    private int mode = -1;

    private ConfigManager configManager = null;

    // only client
    private String userId = "";

    // only server
    private String serverNonce = "";

    public AppInstance() {
    }

    public static AppInstance getInstance() {
        if (appInstance == null) {
            appInstance = new AppInstance();
        }
        return appInstance;
    }

    private void initServerInstance(){
        try {
            // Decoding nonce -> realm
            MessageDigest messageDigestNonce = MessageDigest.getInstance(ALGORITHM);
            messageDigestNonce.update(AppInstance.MD5_REALM.getBytes(StandardCharsets.UTF_8));
            messageDigestNonce.update(AppInstance.MD5_HASH_KEY.getBytes(StandardCharsets.UTF_8));
            byte[] digestNonce = messageDigestNonce.digest();
            messageDigestNonce.reset();
            messageDigestNonce.update(digestNonce);

            serverNonce = new String(messageDigestNonce.digest());
        } catch (Exception e) {
            log.error("AppInstance.initServerInstance ", e);
            System.exit(1);
        }
    }

    private void initClientInstance() {
        userId = CnameGenerator.generateCnameUserId();
    }

    public int getMode() {return mode;}

    public boolean setMode(int mode) {
        if (this.mode != -1) { return false; }
        switch (mode){
            case SERVER_MODE:
                this.mode = SERVER_MODE;
                initServerInstance();
                break;
            case CLIENT_MODE:
                this.mode = CLIENT_MODE;
                initClientInstance();
                break;
            case PROXY_MODE:
                this.mode = PROXY_MODE;
                break;
            default:
                return false;
        }
        return true;
    }

    public ConfigManager getConfigManager() {return configManager;}

    public void setConfigManager(String configPath) {
        this.configManager = new ConfigManager(configPath);
    }

    public long getTimeStamp() { return TimeStamp.getCurrentTime().getSeconds();}

    // only server
    public String getServerNonce() {return serverNonce;}

    // only client
    public String getUserId() {return userId;}
}
