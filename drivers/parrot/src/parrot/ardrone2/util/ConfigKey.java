package parrot.ardrone2.util;

/**
 * Created by brecht on 4/6/15.
 */
public enum ConfigKey {
    // "general" configs
    GEN_NUM_VERSION_CONFIG("num_version_config"),
    GEN_NUM_VERSION_MB("num_version_mb"),
    GEN_NUM_VERSION_SOFT("num_version_soft"),
    GEN_DRONE_SERIAL("drone_serial"),
    GEN_SOFT_BUILD_DATE("soft_build_date"),
    GEN_MOTOR1_SOFT("motor1_soft"),
    GEN_MOTOR1_HARD("motor1_hard"),
    GEN_MOTOR1_SUPPLIER("motor1_supplier"),
    GEN_MOTOR2_SOFT("motor2_soft"),
    GEN_MOTOR2_HARD("motor2_hard"),
    GEN_MOTOR2_SUPPLIER("motor2_supplier"),
    GEN_MOTOR3_SOFT("motor3_soft"),
    GEN_MOTOR3_HARD("motor3_hard"),
    GEN_MOTOR3_SUPPLIER("motor3_supplier"),
    GEN_MOTOR4_SOFT("motor4_soft"),
    GEN_MOTOR4_HARD("motor4_hard"),
    GEN_MOTOR4_SUPPLIER("motor4_supplier"),
    GEN_ARDRONE_NAME("ardrone_name"),
    GEN_FLYING_TIME("flying_time"),
    GEN_NAVDATA_DEMO("navdata_demo"),
    GEN_NAVDATA_OPTIONS("navdata_options"),
    GEN_COM_WATCHDOG("com_watchdog"),
    GEN_VIDEO_ENABLE("video_enable"),
    GEN_VISION_ENABLE("vision_enable"),
    GEN_VBAT_MIN("vbat_min"),
    // control configs
    CONTROL_CONTROL_LEVEL("control_level"),
    CONTROL_EULER_ANGLE_MAX("euler_angle_max"),
    CONTROL_ALTITUDE_MAX("altitude_max"),
    CONTROL_ALTITUDE_MIN("altitude_min"),
    CONTROL_CONTROL_VZ_MAX("control_vz_max"),
    CONTROL_CONTROL_YAW("control_yaw"),
    CONTROL_OUTDOOR("outdoor"),
    CONTROL_FLIGHT_WITHOUT_SHELL("flight_without_shell"),
    CONTROL_FLYING_MODE("flying_mode"),
    // custom configs
    CUSTOM_APPLICATION_ID("application_id"),
    CUSTOM_PROFILE_ID("profile_id"),
    CUSTOM_SESSION_ID("session_id"),
    // video configs
    VIDEO_BITRATE("bitrate"),
    VIDEO_BITRATE_MAX("max_bitrate"),
    VIDEO_CODEC("video_codec"),
    VIDEO_CHANNEL("video_channel"),
    VIDEO_ON_USB("video_on_usb"),
    VIDEO_BITRATE_CTRL_MODE("bitrate_ctrl_mode");

    private static final String GENERAL = "general";
    private static final String CONTROL = "control";
    private static final String CUSTOM = "custom";
    private static final String VIDEO = "video";

    static {
        GEN_NUM_VERSION_CONFIG.configClass = GENERAL;
        GEN_NUM_VERSION_MB.configClass = GENERAL;
        GEN_NUM_VERSION_SOFT.configClass = GENERAL;
        GEN_DRONE_SERIAL.configClass = GENERAL;
        GEN_SOFT_BUILD_DATE.configClass = GENERAL;
        GEN_MOTOR1_SOFT.configClass = GENERAL;
        GEN_MOTOR1_HARD.configClass = GENERAL;
        GEN_MOTOR1_SUPPLIER.configClass = GENERAL;
        GEN_MOTOR2_SOFT.configClass = GENERAL;
        GEN_MOTOR2_HARD.configClass = GENERAL;
        GEN_MOTOR2_SUPPLIER.configClass = GENERAL;
        GEN_MOTOR3_SOFT.configClass = GENERAL;
        GEN_MOTOR3_HARD.configClass = GENERAL;
        GEN_MOTOR3_SUPPLIER.configClass = GENERAL;
        GEN_MOTOR4_SOFT.configClass = GENERAL;
        GEN_MOTOR4_HARD.configClass = GENERAL;
        GEN_MOTOR4_SUPPLIER.configClass = GENERAL;
        GEN_ARDRONE_NAME.configClass = GENERAL;
        GEN_FLYING_TIME.configClass = GENERAL;
        GEN_NAVDATA_DEMO.configClass = GENERAL;
        GEN_NAVDATA_OPTIONS.configClass = GENERAL;
        GEN_COM_WATCHDOG.configClass = GENERAL;
        GEN_VIDEO_ENABLE.configClass = GENERAL;
        GEN_VISION_ENABLE.configClass = GENERAL;
        GEN_VBAT_MIN.configClass = GENERAL;
        // control configs
        CONTROL_CONTROL_LEVEL.configClass = CONTROL;
        CONTROL_EULER_ANGLE_MAX.configClass = CONTROL;
        CONTROL_ALTITUDE_MAX.configClass = CONTROL;
        CONTROL_ALTITUDE_MIN.configClass = CONTROL;
        CONTROL_CONTROL_VZ_MAX.configClass = CONTROL;
        CONTROL_CONTROL_YAW.configClass = CONTROL;
        CONTROL_OUTDOOR.configClass = CONTROL;
        CONTROL_FLIGHT_WITHOUT_SHELL.configClass = CONTROL;
        CONTROL_FLYING_MODE.configClass = CONTROL;
        // custom configs
        CUSTOM_APPLICATION_ID.configClass = CUSTOM;
        CUSTOM_PROFILE_ID.configClass = CUSTOM;
        CUSTOM_SESSION_ID.configClass = CUSTOM;
        // video configs
        VIDEO_BITRATE.configClass = VIDEO;
        VIDEO_BITRATE_MAX.configClass = VIDEO;
        VIDEO_CODEC.configClass = VIDEO;
        VIDEO_CHANNEL.configClass = VIDEO;
        VIDEO_ON_USB.configClass = VIDEO;
        VIDEO_BITRATE_CTRL_MODE.configClass = VIDEO;
    }

    private String configClass;
    private String key;

    private ConfigKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return String.format("%s:%s", configClass, key);
    }
}
