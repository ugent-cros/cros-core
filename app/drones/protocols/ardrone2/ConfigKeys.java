package drones.protocols.ardrone2;

/**
 * Created by brecht on 4/6/15.
 */
public enum ConfigKeys {
    gen_num_version_config("GENERAL", "num_version_config"),
    gen_num_version_mb("GENERAL", "num_version_mb"),
    gen_num_version_soft("GENERAL", "num_version_soft"),
    gen_drone_serial("GENERAL", "drone_serial"),
    gen_soft_build_date("GENERAL", "soft_build_date"),
    gen_motor1_soft("GENERAL", "motor1_soft"),
    gen_motor1_hard("GENERAL", "motor1_hard"),
    gen_motor1_supplier("GENERAL", "motor1_supplier"),
    gen_motor2_soft("GENERAL", "motor2_soft"),
    gen_motor2_hard("GENERAL", "motor2_hard"),
    gen_motor2_supplier("GENERAL", "motor2_supplier"),
    gen_motor3_soft("GENERAL", "motor3_soft"),
    gen_motor3_hard("GENERAL", "motor3_hard"),
    gen_motor3_supplier("GENERAL", "motor3_supplier"),
    gen_motor4_soft("GENERAL", "motor4_soft"),
    gen_motor4_hard("GENERAL", "motor4_hard"),
    gen_motor4_supplier("GENERAL", "motor4_supplier"),
    gen_ardrone_name("GENERAL", "ardrone_name"),
    gen_flying_time("GENERAL", "flying_time"),
    gen_navdata_demo("GENERAL", "navdata_demo"),
    gen_navdata_options("GENERAL", "navdata_options"),
    gen_com_watchdog("GENERAL", "com_watchdog"),
    gen_video_enable("GENERAL", "video_enable"),
    gen_vision_enable("GENERAL", "vision_enable"),
    gen_vbat_min("GENERAL", "vbat_min");

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
