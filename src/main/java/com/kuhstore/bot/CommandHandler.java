package com.kuhstore.bot;

import com.kuhstore.dao.MemberDAO;
import com.kuhstore.model.Member;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle command Telegram (/start, /menu, /daftar, dll).
 */
public class CommandHandler {

    private final KuhStoreBot bot;
    private final MemberDAO memberDAO;

    public CommandHandler(KuhStoreBot bot, MemberDAO memberDAO) {
        this.bot = bot;
        this.memberDAO = memberDAO;
    }

    public void handle(long chatId, String text, User from) {
        switch (text.toLowerCase()) {
            case "/start":
                handleStart(chatId, from);
                break;
            case "/menu":
                showMainMenu(chatId);
                break;
            case "/daftar":
                handleDaftar(chatId, from);
                break;
            case "/bantuan":
                handleBantuan(chatId);
                break;
            default:
                bot.sendMessage(chatId, "❓ Perintah tidak dikenal. Ketik /menu untuk melihat menu.");
        }
    }

    private void handleStart(long chatId, User from) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member != null) {
                bot.sendMessage(chatId, "👋 Selamat datang kembali, " + getDisplayName(from) + "!\n\n"
                        + "Ketik /menu untuk melihat menu utama.");
            } else {
                bot.sendMessage(chatId, "👋 Halo! Selamat datang di <b>KuhStore</b> 🛒\n\n"
                        + "Kami menyediakan layanan:\n"
                        + "🎮 Top-Up Game\n"
                        + "📱 Pulsa & Paket Data\n"
                        + "⚡ Token PLN\n"
                        + "💳 E-Wallet\n\n"
                        + "Untuk mulai, silakan ketik /daftar untuk mendaftar.\n"
                        + "Atau ketik /menu untuk melihat menu.");
            }
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan. Silakan coba lagi.");
            System.err.println("Error handleStart: " + e.getMessage());
        }
    }

    private void handleDaftar(long chatId, User from) {
        try {
            Member existing = memberDAO.findByTelegramId(chatId);
            if (existing != null) {
                bot.sendMessage(chatId, "✅ Anda sudah terdaftar! Ketik /menu untuk melihat menu.");
                return;
            }

            // Daftarkan member baru
            Member member = new Member();
            member.setTelegramId(chatId);
            member.setUsername(from.getUserName());
            member.setFullName(from.getFirstName() + (from.getLastName() != null ? " " + from.getLastName() : ""));
            member.setVerified(true);
            member.setBalance(0);
            memberDAO.insert(member);

            bot.sendMessage(chatId, "✅ Pendaftaran berhasil! Selamat datang, " + getDisplayName(from) + "!\n\n"
                    + "Ketik /menu untuk melihat menu utama.");
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Gagal mendaftar. Silakan coba lagi.");
            System.err.println("Error handleDaftar: " + e.getMessage());
        }
    }

    private void showMainMenu(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("🏪 <b>Menu Utama KuhStore</b>\n\nSilakan pilih layanan:");

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎮 Top-Up Game"));
        row1.add(new KeyboardButton("📱 Pulsa & Paket Data"));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⚡ Token PLN"));
        row2.add(new KeyboardButton("💳 E-Wallet"));
        rows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📊 Cek Transaksi"));
        rows.add(row3);

        keyboard.setKeyboard(rows);
        message.setReplyMarkup(keyboard);

        bot.sendMessage(message);
    }

    private void handleBantuan(long chatId) {
        bot.sendMessage(chatId, "📖 <b>Bantuan KuhStore</b>\n\n"
                + "/start - Memulai bot\n"
                + "/menu - Tampilkan menu utama\n"
                + "/daftar - Mendaftar sebagai member\n"
                + "/bantuan - Bantuan ini\n\n"
                + "Hubungi admin jika ada masalah.");
    }

    private String getDisplayName(User from) {
        return from.getFirstName() != null ? from.getFirstName() : "User";
    }
}
