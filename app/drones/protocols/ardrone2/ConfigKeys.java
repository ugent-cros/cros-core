package drones.protocols.ardrone2;

/**
 * Created by brecht on 4/6/15.
 */
public enum ConfigKeys {
    // general configs
    gen_num_version_config("general", "num_version_config"),
    gen_num_version_mb("general", "num_version_mb"),
    gen_num_version_soft("general", "num_version_soft"),
    gen_drone_serial("general", "drone_serial"),
    gen_soft_build_date("general", "soft_build_date"),
    gen_motor1_soft("general", "motor1_soft"),
    gen_motor1_hard("general", "motor1_hard"),
    gen_motor1_supplier("general", "motor1_supplier"),
    gen_motor2_soft("general", "motor2_soft"),
    gen_motor2_hard("general", "motor2_hard"),
    gen_motor2_supplier("general", "motor2_supplier"),
    gen_motor3_soft("general", "motor3_soft"),
    gen_motor3_hard("general", "motor3_hard"),
    gen_motor3_supplier("general", "motor3_supplier"),
    gen_motor4_soft("general", "motor4_soft"),
    gen_motor4_hard("general", "motor4_hard"),
    gen_motor4_supplier("general", "motor4_supplier"),
    gen_ardrone_name("general", "ardrone_name"),
    gen_flying_time("general", "flying_time"),
    gen_navdata_demo("general", "navdata_demo"),
    gen_navdata_options("general", "navdata_options"),
    gen_com_watchdog("general", "com_watchdog"),
    gen_video_enable("general", "video_enable"),
    gen_vision_enable("general", "vision_enable"),
    gen_vbat_min("general", "vbat_min"),
    // control configs
    control_control_level("control","control_level"),
    control_euler_angle_max("control","euler_angle_max"),
    control_altitude_max("control","altitude_max"),
    control_altitude_min("control","altitude_min"),
    control_control_vz_max("control","control_vz_max"),
    control_control_yaw("control","control_yaw"),
    control_outdoor("control","outdoor"),
    control_flight_without_shell("control","flight_without_shell"),
    control_flying_mode("control","flying_mode"),
    // custom configs
    custom_application_id("custom","application_id"),
    custom_profile_id("custom","profile_id"),
    custom_session_id("custom","session_id");

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
