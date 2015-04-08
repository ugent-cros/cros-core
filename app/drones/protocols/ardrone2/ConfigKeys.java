package drones.protocols.ardrone2;

/**
 * Created by brecht on 4/6/15.
 */
public enum ConfigKeys {
    // "general" configs
    GEN_NUM_VERSION_CONFIG("general", "num_version_config"),
    GEN_NUM_VERSION_MB("general", "num_version_mb"),
    GEN_NUM_VERSION_SOFT("general", "num_version_soft"),
    GEN_DRONE_SERIAL("general", "drone_serial"),
    GEN_SOFT_BUILD_DATE("general", "soft_build_date"),
    GEN_MOTOR1_SOFT("general", "motor1_soft"),
    GEN_MOTOR1_HARD("general", "motor1_hard"),
    GEN_MOTOR1_SUPPLIER("general", "motor1_supplier"),
    GEN_MOTOR2_SOFT("general", "motor2_soft"),
    GEN_MOTOR2_HARD("general", "motor2_hard"),
    GEN_MOTOR2_SUPPLIER("general", "motor2_supplier"),
    GEN_MOTOR3_SOFT("general", "motor3_soft"),
    GEN_MOTOR3_HARD("general", "motor3_hard"),
    GEN_MOTOR3_SUPPLIER("general", "motor3_supplier"),
    GEN_MOTOR4_SOFT("general", "motor4_soft"),
    GEN_MOTOR4_HARD("general", "motor4_hard"),
    GEN_MOTOR4_SUPPLIER("general", "motor4_supplier"),
    GEN_ARDRONE_NAME("general", "ardrone_name"),
    GEN_FLYING_TIME("general", "flying_time"),
    GEN_NAVDATA_DEMO("general", "navdata_demo"),
    GEN_NAVDATA_OPTIONS("general", "navdata_options"),
    GEN_COM_WATCHDOG("general", "com_watchdog"),
    GEN_VIDEO_ENABLE("general", "video_enable"),
    GEN_VISION_ENABLE("general", "vision_enable"),
    GEN_VBAT_MIN("general", "vbat_min"),
    // control configs
    CONTROL_CONTROL_LEVEL("control","control_level"),
    CONTROL_EULER_ANGLE_MAX("control","euler_angle_max"),
    CONTROL_ALTITUDE_MAX("control","altitude_max"),
    CONTROL_ALTITUDE_MIN("control","altitude_min"),
    CONTROL_CONTROL_VZ_MAX("control","control_vz_max"),
    CONTROL_CONTROL_YAW("control","control_yaw"),
    CONTROL_OUTDOOR("control","outdoor"),
    CONTROL_FLIGHT_WITHOUT_SHELL("control","flight_without_shell"),
    CONTROL_FLYING_MODE("control","flying_mode"),
    // custom configs
    CUSTOM_APPLICATION_ID("custom","application_id"),
    CUSTOM_PROFILE_ID("custom","profile_id"),
    CUSTOM_SESSION_ID("custom","session_id");

    private String configClass;
    private String key;

    private ConfigKeys(String configClass, String key) {
        this.configClass = configClass;
        this.key = key;
    }

    public String getKey() {
        return String.format("%s:%s", configClass, key);
    }
}
