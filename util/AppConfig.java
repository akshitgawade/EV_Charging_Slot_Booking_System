package com.evcharging.util;

public class AppConfig {

    public static String getDbUrl() {
        return System.getenv().getOrDefault(
            "DB_URL",
            "jdbc:mysql://localhost:3306/ev_charging_system?useSSL=false&serverTimezone=Asia/Kolkata"
        );
    }

    public static String getDbUser() {
        return System.getenv().getOrDefault("DB_USER", "root");
    }

    public static String getDbPassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }
}
