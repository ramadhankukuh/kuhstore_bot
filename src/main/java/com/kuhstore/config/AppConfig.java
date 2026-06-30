package com.kuhstore.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Konstanta konfigurasi global KuhStore Bot.
 * Dibaca dari file config.properties di classpath.
 */
public class AppConfig {

    private static final Properties PROPS = loadProperties();

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException(
                    "config.properties tidak ditemukan di classpath! " +
                    "Salin config.properties.example menjadi config.properties " +
                    "dan isi credential kamu."
                );
            }
            props.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        return props;
    }

    private static String get(String key) {
        return PROPS.getProperty(key);
    }

    // ========== Database ==========
    public static final String DB_HOST = get("db.host");
    public static final String DB_PORT = get("db.port");
    public static final String DB_NAME = get("db.name");
    public static final String DB_USER = get("db.user");
    public static final String DB_PASS = get("db.pass");
    public static final String DB_URL  = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
            + "?useSSL=false&serverTimezone=Asia/Jakarta&allowPublicKeyRetrieval=true";

    // ========== Telegram Bot ==========
    public static final String BOT_TOKEN   = get("bot.token");
    public static final String BOT_USERNAME = get("bot.username");

    // ========== H2H API ==========
    public static final String H2H_BASE_URL  = get("h2h.base_url");
    public static final String H2H_MEMBER_ID = get("h2h.member_id");
    public static final String H2H_PIN       = get("h2h.pin");
    public static final String H2H_PASSWORD  = get("h2h.password");

    // ========== Midtrans Sandbox ==========
    public static final String MIDTRANS_SERVER_KEY = get("midtrans.server_key");
    public static final String MIDTRANS_CLIENT_KEY = get("midtrans.client_key");
    public static final String MIDTRANS_BASE_URL   = get("midtrans.base_url");
}
