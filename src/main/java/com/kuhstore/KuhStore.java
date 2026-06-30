/*
 * KuhStore — Aplikasi Bot Telegram untuk Top-Up Game & Produk Digital
 * PBO UAS Genap 2025/2026 UDINUS
 * Dosen: Ajib Susanto, M.Kom
 */
package com.kuhstore;

import com.kuhstore.bot.BroadcastService;
import com.kuhstore.bot.KuhStoreBot;
import com.kuhstore.config.AppConfig;
import com.kuhstore.gui.LoginFrame;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import javax.swing.*;

/**
 * Entry point utama KuhStore.
 * Menjalankan Telegram Bot (background) + Admin GUI (Swing).
 */
public class KuhStore {

    private static KuhStoreBot bot;
    private static BroadcastService broadcastService;

    public static void main(String[] args) {
        // Set look and feel sistem
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Failed to set look and feel: " + e.getMessage());
        }

        // Jalankan Telegram Bot di thread terpisah
        startTelegramBot();

        // Buka GUI Admin
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }

    /**
     * Memulai Telegram Bot menggunakan LongPolling.
     */
    private static void startTelegramBot() {
        new Thread(() -> {
            try {
                // Delete webhook dulu agar long polling bisa jalan
                String deleteUrl = "https://api.telegram.org/bot" + AppConfig.BOT_TOKEN + "/deleteWebhook?drop_pending_updates=true";
                java.net.URL url = new java.net.URL(deleteUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int responseCode = conn.getResponseCode();
                System.out.println("deleteWebhook response: " + responseCode);
                conn.disconnect();

                // Register bot
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
                bot = new KuhStoreBot();
                botsApi.registerBot(bot);
                broadcastService = new BroadcastService(bot);
                System.out.println("✅ KuhStoreBot started successfully! @" + AppConfig.BOT_USERNAME);
            } catch (Exception e) {
                System.err.println("❌ Failed to start Telegram Bot: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(null,
                        "Gagal menjalankan Telegram Bot:\n" + e.getMessage(),
                        "Bot Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    /**
     * Mendapatkan instance BroadcastService untuk digunakan dari GUI.
     */
    public static BroadcastService getBroadcastService() {
        return broadcastService;
    }

    /**
     * Mendapatkan instance bot.
     */
    public static KuhStoreBot getBot() {
        return bot;
    }
}
