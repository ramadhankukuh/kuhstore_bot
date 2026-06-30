package com.kuhstore.bot;

import com.kuhstore.config.AppConfig;
import com.kuhstore.dao.MemberDAO;
import com.kuhstore.dao.TransactionDAO;
import com.kuhstore.model.Member;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Handler utama Telegram Bot (LongPolling).
 */
public class KuhStoreBot extends TelegramLongPollingBot {

    private final CommandHandler commandHandler;
    private final MessageHandler messageHandler;
    private final MemberDAO memberDAO;
    private final TransactionDAO transactionDAO;

    public KuhStoreBot() {
        this.memberDAO = new MemberDAO();
        this.transactionDAO = new TransactionDAO();
        this.commandHandler = new CommandHandler(this, memberDAO);
        this.messageHandler = new MessageHandler(this, memberDAO, transactionDAO);
    }

    @Override
    public String getBotUsername() {
        return AppConfig.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return AppConfig.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(">>> Update received: " + (update.hasMessage() ? update.getMessage().getText() : "non-text"));

        // Handle callback query (inline keyboard)
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
            return;
        }

        // Handle pesan teks
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();
            String chatName = update.getMessage().getFrom() != null
                    ? update.getMessage().getFrom().getFirstName() : "unknown";

            System.out.println(">>> Message from " + chatName + " (chatId=" + chatId + "): " + text);

            try {
                if (text.startsWith("/")) {
                    commandHandler.handle(chatId, text, update.getMessage().getFrom());
                } else {
                    messageHandler.handle(chatId, text, update.getMessage().getFrom());
                }
            } catch (Exception e) {
                System.err.println("!!! Error handling message: " + e.getMessage());
                e.printStackTrace();
                sendMessage(chatId, "⚠️ Terjadi kesalahan internal. Silakan coba lagi.");
            }
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackData = update.getCallbackQuery().getData();
        long chatId = update.getCallbackQuery().getMessage().getChatId();
        int messageId = update.getCallbackQuery().getMessage().getMessageId();

        // Proses callback data (akan di-expand seiring development)
        System.out.println("Callback: " + callbackData + " from chat " + chatId);

        // Default: forward ke messageHandler untuk sementara
        messageHandler.handleCallback(chatId, messageId, callbackData, update.getCallbackQuery().getFrom());
    }

    /**
     * Helper untuk mengirim pesan.
     */
    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("HTML");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Helper untuk mengirim pesan dengan reply keyboard markup.
     */
    public void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
    }

    /**
     * Helper untuk mengedit teks pesan yang sudah dikirim (inline keyboard).
     */
    public void editMessageText(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(String.valueOf(chatId));
        edit.setMessageId(messageId);
        edit.setText(text);
        edit.setParseMode("HTML");
        if (markup != null) {
            edit.setReplyMarkup(markup);
        }
        try {
            execute(edit);
        } catch (TelegramApiException e) {
            System.err.println("Error editing message: " + e.getMessage());
        }
    }

    /**
     * Helper untuk mengirim foto QRIS dengan caption dan tombol aksi.
     */
    public void sendQRISPhoto(long chatId, String imageUrl, String caption, InlineKeyboardMarkup markup) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(String.valueOf(chatId));
        photo.setPhoto(new InputFile(imageUrl));
        photo.setCaption(caption);
        photo.setParseMode("HTML");
        if (markup != null) {
            photo.setReplyMarkup(markup);
        }
        try {
            execute(photo);
        } catch (TelegramApiException e) {
            System.err.println("Error sending photo: " + e.getMessage());
        }
    }
}
