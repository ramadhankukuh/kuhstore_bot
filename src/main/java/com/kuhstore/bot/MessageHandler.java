package com.kuhstore.bot;

import com.kuhstore.api.H2HClient;
import com.kuhstore.dao.KeywordDAO;
import com.kuhstore.dao.MemberDAO;
import com.kuhstore.dao.ProductDAO;
import com.kuhstore.dao.TransactionDAO;
import com.kuhstore.model.Keyword;
import com.kuhstore.model.Member;
import com.kuhstore.model.Product;
import com.kuhstore.model.Transaction;
import java.util.HashMap;
import java.util.Map;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle pesan teks biasa & keyword auto-reply.
 */
public class MessageHandler {

    private final KuhStoreBot bot;
    private final MemberDAO memberDAO;
    private final TransactionDAO transactionDAO;
    private final ProductDAO productDAO;
    private final KeywordDAO keywordDAO;

    // Checkout state: chatId -> {stage, productId, gameKey}
    private static final Map<Long, Map<String, String>> checkoutState = new HashMap<>();

    public MessageHandler(KuhStoreBot bot, MemberDAO memberDAO, TransactionDAO transactionDAO) {
        this.bot = bot;
        this.memberDAO = memberDAO;
        this.transactionDAO = transactionDAO;
        this.productDAO = new ProductDAO();
        this.keywordDAO = new KeywordDAO();
    }

    // Daftar tombol menu utama yang bisa diakses kapan saja
    private static final java.util.Set<String> MENU_BUTTONS = java.util.Set.of(
        "🎮 Top-Up Game",
        "📱 Pulsa & Paket Data",
        "⚡ Token PLN",
        "💳 E-Wallet",
        "📊 Cek Transaksi"
    );

    public void handle(long chatId, String text, User from) {
        // Jika user menekan tombol menu saat sedang checkout, batalkan checkout
        if (checkoutState.containsKey(chatId)) {
            if (MENU_BUTTONS.contains(text) || text.startsWith("/")) {
                checkoutState.remove(chatId);
                // Lanjutkan ke proses menu di bawah
            } else {
                handleCheckoutInput(chatId, text);
                return;
            }
        }

        // Cek keyword auto-reply terlebih dahulu
        try {
            Keyword keyword = keywordDAO.findByKeyword(text.toLowerCase().trim());
            if (keyword != null) {
                bot.sendMessage(chatId, keyword.getResponse());
                return;
            }
        } catch (Exception e) {
            System.err.println("Error checking keyword: " + e.getMessage());
        }

        // Handle menu keyboard
        switch (text) {
            case "🎮 Top-Up Game":
                showGameList(chatId);
                break;
            case "📱 Pulsa & Paket Data":
                showPulsaCategories(chatId);
                break;
            case "⚡ Token PLN":
                handlePLN(chatId);
                break;
            case "💳 E-Wallet":
                showEWallet(chatId);
                break;
            case "📊 Cek Transaksi":
                showTransactionHistory(chatId, from);
                break;
            default:
                bot.sendMessage(chatId, "Silakan pilih menu yang tersedia. Ketik /menu untuk melihat menu.");
        }
    }

    public void handleCallback(long chatId, int messageId, String callbackData, User from) {
        System.out.println("Callback data: " + callbackData);
        try {
            if (callbackData.startsWith("game_")) {
                String gameKey = callbackData.substring(5);
                showGameProducts(chatId, messageId, gameKey);
            } else if (callbackData.startsWith("product_")) {
                int productId = Integer.parseInt(callbackData.substring(8));
                showCheckoutConfirmation(chatId, messageId, productId);
            } else if (callbackData.startsWith("checkout_yes_")) {
                int productId = Integer.parseInt(callbackData.substring(13));
                Product product = productDAO.findById(productId);
                if (product == null) {
                    bot.sendMessage(chatId, "❌ Produk tidak ditemukan.");
                    return;
                }

                // Determine game key from product name
                String prodName = product.getName().toLowerCase();
                String gameKey = "";
                if (prodName.contains("free fire") || prodName.contains("ff")) {
                    gameKey = "free-fire";
                } else if (prodName.contains("mobile legend") || prodName.contains("ml")) {
                    gameKey = "mobile-legends";
                }

                // Set checkout state
                Map<String, String> state = new HashMap<>();
                state.put("productId", String.valueOf(productId));
                state.put("stage", "awaiting_id");
                if (!gameKey.isEmpty()) {
                    state.put("gameKey", gameKey);
                }
                checkoutState.put(chatId, state);

                // Build checkout form
                String name = product.getName().toLowerCase();
                boolean needsZone = name.contains("mobile legend") || name.contains("ml");

                String checkoutText = "🛒 <b>Checkout</b>\n\n"
                    + "Produk: " + product.getName() + "\n"
                    + "Harga: Rp" + String.format("%,.0f", product.getSellingPrice()) + "\n\n"
                    + "📝 Masukkan <b>User ID</b> kamu:";
                if (needsZone) {
                    checkoutText += "\n\n💡 Untuk Mobile Legends, masukkan User ID dulu (contoh: 123456789)";
                } else if (gameKey.equals("free-fire")) {
                    checkoutText += "\n\n💡 Masukkan User ID Free Fire kamu (contoh: 12345678)";
                }

                // Edit pesan konfirmasi jadi form input User ID
                bot.editMessageText(chatId, messageId, checkoutText, null);
            } else if (callbackData.startsWith("checkout_no_")) {
                bot.editMessageText(chatId, messageId, "⬅️ <b>Menu Utama</b>\n\nSilakan ketik /menu untuk melihat menu utama.", null);
            } else if (callbackData.startsWith("pay_confirm_")) {
                int transactionId = Integer.parseInt(callbackData.substring(12));
                confirmPaymentAndProcess(chatId, transactionId);
            } else {
                bot.sendMessage(chatId, "⏳ Fitur sedang dalam pengembangan...");
            }
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan.");
            System.err.println("Error handleCallback: " + e.getMessage());
        }
    }

    private void showGameList(long chatId) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null || !member.isVerified()) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar. Ketik /daftar untuk mendaftar.");
                return;
            }

            List<Product> products = productDAO.findByCategory("voucher_game");
            if (products.isEmpty()) {
                bot.sendMessage(chatId, "🎮 Daftar game belum tersedia. Silakan hubungi admin.");
                return;
            }

            // Extract unique game names from product names
            List<String> gameNames = new ArrayList<>();
            for (Product p : products) {
                String name = p.getName().toLowerCase();
                String game = "";
                if (name.contains("free fire") || name.contains("ff")) game = "Free Fire";
                else if (name.contains("mobile legend")) game = "Mobile Legends";
                else if (name.contains("pubg")) game = "PUBG Mobile";
                else if (name.contains("valorant")) game = "Valorant";
                else if (name.contains("genshin")) game = "Genshin Impact";
                else game = p.getName().replaceAll("\\d+\\s*Diamond\\s*", "").trim();
                if (!gameNames.contains(game) && !game.isEmpty()) {
                    gameNames.add(game);
                }
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (String game : gameNames) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText("🎮 " + game);
                String gameKey = game.toLowerCase().replaceAll("[^a-z]", "");
                btn.setCallbackData("game_" + gameKey);
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(btn);
                rows.add(row);
            }

            markup.setKeyboard(rows);
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🎮 <b>Top-Up Game</b>\n\nPilih game:");
            msg.setReplyMarkup(markup);
            bot.sendMessage(msg);

        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Gagal memuat daftar game.");
            System.err.println("Error showGameList: " + e.getMessage());
        }
    }

    private void showGameProducts(long chatId, int messageId, String gameKey) {
        try {
            List<Product> allProducts = productDAO.findByCategory("voucher_game");
            List<Product> filtered = new ArrayList<>();

            for (Product p : allProducts) {
                String name = p.getName().toLowerCase();
                boolean match;
                switch (gameKey) {
                    case "freefire":      match = name.contains("free fire") || name.contains("ff"); break;
                    case "mobilelegends": match = name.contains("mobile legend"); break;
                    case "pubgmobile":    match = name.contains("pubg"); break;
                    case "valorant":      match = name.contains("valorant"); break;
                    case "genshinimpact": match = name.contains("genshin"); break;
                    default:              match = true; break;
                }
                if (match) filtered.add(p);
            }

            if (filtered.isEmpty()) {
                bot.sendMessage(chatId, "❌ Tidak ada produk untuk game ini.");
                return;
            }

            String gameDisplay;
            switch (gameKey) {
                case "freefire":      gameDisplay = "Free Fire"; break;
                case "mobilelegends": gameDisplay = "Mobile Legends"; break;
                case "pubgmobile":    gameDisplay = "PUBG Mobile"; break;
                case "valorant":      gameDisplay = "Valorant"; break;
                case "genshinimpact": gameDisplay = "Genshin Impact"; break;
                default:              gameDisplay = gameKey; break;
            }
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (Product p : filtered) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(p.getName() + " - Rp" + String.format("%,.0f", p.getSellingPrice()));
                btn.setCallbackData("product_" + p.getId());
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(btn);
                rows.add(row);
            }

            markup.setKeyboard(rows);
            // Edit pesan sebelumnya (daftar game) jadi daftar nominal
            bot.editMessageText(chatId, messageId, "🎮 <b>" + gameDisplay + "</b>\n\nPilih nominal diamond:", markup);

        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Gagal memuat produk.");
            System.err.println("Error showGameProducts: " + e.getMessage());
        }
    }

    /**
     * Menampilkan konfirmasi checkout dengan tombol Checkout dan Batal.
     * Mengganti pesan (edit) yang berisi daftar nominal dengan ringkasan pesanan.
     */
    private void showCheckoutConfirmation(long chatId, int messageId, int productId) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar.");
                return;
            }

            Product product = productDAO.findById(productId);
            if (product == null) {
                bot.sendMessage(chatId, "❌ Produk tidak ditemukan.");
                return;
            }

            String detailMsg = "🛒 <b>Konfirmasi Pesanan</b>\n\n"
                + "🎮 " + product.getName() + "\n"
                + "💰 Harga: Rp" + String.format("%,.0f", product.getSellingPrice()) + "\n\n"
                + "Silakan pilih:\n"
                + "✅ <b>Checkout</b> — Lanjutkan ke pembayaran\n"
                + "❌ <b>Batal</b> — Kembali ke daftar game";

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton btnCheckout = new InlineKeyboardButton();
            btnCheckout.setText("✅ Checkout");
            btnCheckout.setCallbackData("checkout_yes_" + productId);

            InlineKeyboardButton btnBatal = new InlineKeyboardButton();
            btnBatal.setText("❌ Batal");
            btnBatal.setCallbackData("checkout_no_" + productId);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(btnCheckout);
            row.add(btnBatal);
            rows.add(row);

            markup.setKeyboard(rows);

            // Edit pesan yang sudah ada (ganti tombol nominal jadi konfirmasi)
            bot.editMessageText(chatId, messageId, detailMsg, markup);

        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan.");
            System.err.println("Error showCheckoutConfirmation: " + e.getMessage());
        }
    }

    private void handleProductSelection(long chatId, int productId) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar.");
                return;
            }

            Product product = productDAO.findById(productId);
            if (product == null) {
                bot.sendMessage(chatId, "❌ Produk tidak ditemukan.");
                return;
            }

            // Simpan state checkout
            Map<String, String> state = new HashMap<>();
            state.put("productId", String.valueOf(productId));
            state.put("stage", "awaiting_id");
            checkoutState.put(chatId, state);

            // Deteksi game untuk request zone ID
            String name = product.getName().toLowerCase();
            boolean needsZone = name.contains("mobile legend") || name.contains("ml");

            String msg = "🛒 <b>Checkout</b>\n\n"
                    + "Produk: " + product.getName() + "\n"
                    + "Harga: Rp" + String.format("%,.0f", product.getSellingPrice()) + "\n\n"
                    + "📝 Masukkan <b>User ID</b> kamu:";
            if (needsZone) {
                msg += "\n\n💡 Untuk Mobile Legends, masukkan User ID dulu (contoh: 123456789)";
            }
            bot.sendMessage(chatId, msg);

        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan.");
            System.err.println("Error handleProductSelection: " + e.getMessage());
        }
    }

    private void handleCheckoutInput(long chatId, String text) {
        Map<String, String> state = checkoutState.get(chatId);
        String stage = state.get("stage");

        // Handle cancel
        if (text.equalsIgnoreCase("/cancel") || text.equalsIgnoreCase("batal")) {
            checkoutState.remove(chatId);
            bot.sendMessage(chatId, "❌ Checkout dibatalkan. Ketik /menu untuk kembali.");
            return;
        }

        try {
            if ("awaiting_id".equals(stage)) {
                String input = text.trim();
                if (!input.matches("[\\d.]+")) {
                    bot.sendMessage(chatId, "❌ ID Player harus berupa angka. Masukkan ulang:");
                    return;
                }

                int productId = Integer.parseInt(state.get("productId"));
                Product product = productDAO.findById(productId);
                String name = product.getName().toLowerCase();

                // Cek apakah user input dengan format userId.zoneId (khusus ML)
                if (input.contains(".") && (name.contains("mobile legend") || name.contains("ml"))) {
                    String[] parts = input.split("\\.", 2);
                    if (parts.length == 2 && parts[0].matches("\\d+") && parts[1].matches("\\d+")) {
                        state.put("userId", parts[0]);
                        state.put("zoneId", parts[1]);
                        state.put("gameKey", "mobile-legends");
                        createPayment(chatId, state, parts[1]);
                        return;
                    } else {
                        bot.sendMessage(chatId, "❌ Format salah. Gunakan format: UserID.ZoneID (contoh: 12345678.1234)");
                        return;
                    }
                }

                state.put("userId", input);

                if (name.contains("mobile legend") || name.contains("ml")) {
                    state.put("stage", "awaiting_zone");
                    state.put("gameKey", "mobile-legends");
                    bot.sendMessage(chatId, "📝 Masukkan <b>Zone ID</b> kamu (contoh: 1234):");
                } else if (name.contains("free fire") || name.contains("ff")) {
                    state.put("gameKey", "free-fire");
                    state.put("zoneId", "");
                    createPayment(chatId, state, "");
                } else {
                    // Unknown game, skip validation
                    state.put("zoneId", "");
                    createPayment(chatId, state, "");
                }
            } else if ("awaiting_zone".equals(stage)) {
                String zoneId = text.trim();
                if (!zoneId.matches("\\d+")) {
                    bot.sendMessage(chatId, "❌ Zone ID harus berupa angka. Masukkan ulang:");
                    return;
                }
                state.put("zoneId", zoneId);
                createPayment(chatId, state, zoneId);
            } else {
                // Stage tidak dikenal — bersihkan state
                checkoutState.remove(chatId);
                bot.sendMessage(chatId, "⏳ Sesi checkout sudah berakhir. Ketik /menu untuk memulai ulang.");
            }
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan: " + e.getMessage());
            checkoutState.remove(chatId);
            System.err.println("Error handleCheckoutInput: " + e.getMessage());
        }
    }

    /**
     * Validasi ID game via H2H API, lalu lanjut ke pembayaran jika sukses.
     */
    private void validateAndProcessGameId(long chatId, Map<String, String> state) {
        String gameKey = state.get("gameKey");
        String userId = state.get("userId");
        String zoneId = state.getOrDefault("zoneId", "");

        try {
            JSONObject result = H2HClient.checkGameAccount(gameKey, userId, zoneId);
            System.out.println("Game check result: " + result);

            boolean success = result.optBoolean("success", false);

            if (success) {
                JSONObject data = result.optJSONObject("data");
                String username = "";
                if (data != null) {
                    username = data.optString("username", "");
                }
                if (username.isEmpty()) {
                    username = userId;
                }

                state.put("username", username);
                String finalZoneId = zoneId;

                // Proceed to payment (langsung invoice)
                createPayment(chatId, state, finalZoneId);
            } else {
                bot.sendMessage(chatId, "⚠️ User ID kamu salah.\n\n"
                    + "Masukkan kembali User ID kamu dengan benar: ");

                // Reset ke stage awaiting_id untuk input ulang
                state.put("stage", "awaiting_id");
                state.remove("zoneId");
                checkoutState.put(chatId, state);
            }
        } catch (Exception e) {
            // Buat tombol CS
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("💬 Hubungi Customer Service");
            btn.setUrl("https://t.me/ramadhankukuh");
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(btn);
            rows.add(row);
            markup.setKeyboard(rows);

            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setParseMode("HTML");
            msg.setText("Gagal mengecek User ID kamu. Hubungi Customer Service.");
            msg.setReplyMarkup(markup);
            bot.sendMessage(msg);

            checkoutState.remove(chatId);
            System.err.println("Error validateAndProcessGameId: " + e.getMessage());
        }
    }

    private void createPayment(long chatId, Map<String, String> state, String zoneId) throws Exception {
        int productId = Integer.parseInt(state.get("productId"));
        String userId = state.get("userId");

        Member member = memberDAO.findByTelegramId(chatId);
        Product product = productDAO.findById(productId);

        // Buat transaksi di database
        String refId = H2HClient.generateRefId(member.getId());
        String destination = userId + (zoneId.isEmpty() ? "" : "." + zoneId);
        String orderId = "KUHSTORE_" + System.currentTimeMillis();
        Transaction transaction = new Transaction();
        transaction.setRefId(refId);
        transaction.setMidtransOrderId(orderId);
        transaction.setMemberId(member.getId());
        transaction.setProductId(productId);
        transaction.setDestination(destination);
        transaction.setAmount(product.getSellingPrice());
        transaction.setStatus("pending");
        transactionDAO.insert(transaction);

        // Simpan transaction ID ke state
        state.put("transactionId", String.valueOf(transaction.getId()));
        state.put("orderId", orderId);
        state.put("destination", destination);

        // Buat pembayaran QRIS via Midtrans Core API
        try {
            JSONObject midtransResponse = com.kuhstore.api.MidtransClient.createQRISCharge(
                orderId,
                (long) product.getSellingPrice()
            );

            String qrisImageUrl = com.kuhstore.api.MidtransClient.extractQRISImageUrl(midtransResponse);

            if (!qrisImageUrl.isEmpty()) {
                state.put("paymentUrl", qrisImageUrl);
                state.put("stage", "awaiting_payment");

                String caption = "🧾 <b>INVOICE PESANAN</b> 🧾\n\n"
                    + "<b>- ID:</b> " + userId + (zoneId.isEmpty() ? "" : " (Zone " + zoneId + ")") + "\n"
                    + "<b>- Produk:</b> " + product.getName() + "\n"
                    + "<b>- Total Bayar:</b> Rp" + String.format("%,.0f", product.getSellingPrice()) + "\n"
                    + "<b>- Order ID:</b> <code>" + orderId + "</code>\n\n"
                    + "💳 Scan QRIS di atas untuk membayar.\n"
                    + "⏳ Bot akan otomatis mendeteksi pembayaran...";

                // Kirim gambar QRIS
                bot.sendQRISPhoto(chatId, qrisImageUrl, caption, null);

                // Log URL QRIS ke console untuk testing
                System.out.println("🔗 URL QRIS untuk order " + orderId + ": " + qrisImageUrl);

                // Mulai polling otomatis cek status pembayaran
                int transactionId = transaction.getId();
                Product finalProduct = product;
                String finalDestination = destination;
                new Thread(() -> pollPaymentStatus(chatId, orderId, transactionId, finalProduct, finalDestination)).start();
            } else {
                // Gagal generate QRIS — jangan proses order
                bot.sendMessage(chatId, "⚠️ Gagal membuat pembayaran QRIS. Silakan coba lagi nanti.");
                transactionDAO.updateStatus(transaction.getId(), "failed", "QRIS generation failed");
            }

        } catch (Exception e) {
            System.err.println("Midtrans QRIS error: " + e.getMessage());
            // Gateway error — jangan proses order, beri tahu user
            bot.sendMessage(chatId, "⚠️ Gateway pembayaran sedang bermasalah. Silakan coba lagi nanti.");
            try {
                transactionDAO.updateStatus(transaction.getId(), "failed", "Midtrans error: " + e.getMessage());
            } catch (Exception ex) {
                System.err.println("Error updating transaction: " + ex.getMessage());
            }
        }

        checkoutState.put(chatId, state);
    }

    private void processOrderToH2H(long chatId, Transaction transaction, Product product, String destination) {
        new Thread(() -> {
            try {
                JSONObject result = H2HClient.orderProduct(product.getH2hCode(), destination, transaction.getRefId());
                System.out.println("H2H order result: " + result);

                if (!result.optBoolean("status", false)) {
                    String apiMsg = result.optString("message", "Gagal");
                    transactionDAO.updateStatus(transaction.getId(), "failed", "");
                    sendOrderResult(chatId, false, transaction, product, destination, "", apiMsg);
                    return;
                }

                // Cek apakah response langsung sukses (ada SN)
                String sn = checkH2HResponseForSuccess(result);
                if (sn != null) {
                    System.out.println("H2H: Order langsung sukses, sn=" + sn);
                    transactionDAO.updateStatus(transaction.getId(), "success", sn);
                    sendOrderResult(chatId, true, transaction, product, destination, sn, null);
                    return;
                }

                // Cek apakah response langsung failed
                String failMsg = checkH2HResponseForFailed(result);
                if (failMsg != null) {
                    System.out.println("H2H: Order langsung gagal: " + failMsg);
                    transactionDAO.updateStatus(transaction.getId(), "failed", "");
                    sendOrderResult(chatId, false, transaction, product, destination, "", failMsg);
                    return;
                }
                // Fallback: brute force scan semua field
                JSONObject initialData = result.optJSONObject("data");
                if (initialData != null) {
                    String bruteMsg = bruteForceCheckFailed(initialData);
                    if (bruteMsg != null) {
                        System.out.println("H2H: Order gagal (brute): " + bruteMsg);
                        transactionDAO.updateStatus(transaction.getId(), "failed", "");
                        sendOrderResult(chatId, false, transaction, product, destination, "", bruteMsg);
                        return;
                    }
                }
                // Status pending — mulai polling status dari H2H
                pollH2HStatus(chatId, transaction, product, destination);

            } catch (Exception e) {
                System.err.println("H2H error: " + e.getMessage());
                sendOrderResult(chatId, false, transaction, product, destination, "", "Order dalam antrian, silahkan hubungi Customer Service.");
            }
        }).start();

        checkoutState.remove(chatId);
    }

    /**
     * Cek apakah response H2H mengindikasikan sukses.
     * Return SN jika sukses, null jika tidak.
     */
    private String checkH2HResponseForSuccess(JSONObject result) {
        JSONObject data = result.optJSONObject("data");
        if (data != null) {
            // Cek transaction_status (field utama H2H)
            String txStatus = data.optString("transaction_status", "");
            if (txStatus.matches("(?i)success|sukses|berhasil")) {
                String sn = data.optString("serial_number", "");
                if (!sn.isEmpty()) return sn;
                return "✓";
            }

            // Cek status_label (contoh: "Sukses")
            String statusLabel = data.optString("status_label", "");
            if (statusLabel.matches("(?i)success|sukses|berhasil")) {
                String sn = data.optString("serial_number", "");
                if (!sn.isEmpty()) return sn;
                return "✓";
            }

            // Cek status (fallback untuk endpoint lain)
            String orderStatus = data.optString("status", "");
            if (orderStatus.matches("(?i)success|sukses|berhasil")) return "✓";

            String rc = data.optString("rc", "");
            if ("00".equals(rc) || "0".equals(rc)) {
                String sn = data.optString("serial_number", "");
                if (!sn.isEmpty()) return sn;
                return "✓";
            }
        }

        return null;
    }

    /**
     * Cek apakah response H2H mengindikasikan gagal.
     * Return pesan error jika gagal, null jika tidak.
     */
    private String checkH2HResponseForFailed(JSONObject result) {
        JSONObject data = result.optJSONObject("data");
        
        // Cek top-level result dulu
        String topMsg = result.optString("message", "");
        if (topMsg.matches("(?i).*gagal.*|.*fail.*|.*error.*")) {
            return sanitizeMessage(topMsg);
        }
        
        if (data != null) {
            String txStatus = data.optString("transaction_status", "");
            String statusLabel = data.optString("status_label", "");
            String statusDesc = data.optString("status_description", "");
            String msg = data.optString("message", "");

            // Cek dari transaction_status
            if (txStatus.matches("(?i)failed|error|gagal|fail")) {
                return getFailMessage(data, msg);
            }
            // Cek dari status_label
            if (statusLabel.matches("(?i)failed|error|gagal|fail")) {
                return getFailMessage(data, msg);
            }
            // Cek dari status_description
            if (statusDesc.matches("(?i).*gagal.*|.*fail.*|.*error.*")) {
                return getFailMessage(data, msg);
            }
            // Cek dari message
            if (msg.matches("(?i).*gagal.*|.*fail.*|.*error.*|.*invalid.*|.*salah.*")) {
                return getFailMessage(data, msg);
            }
            // Cek dari status
            String orderStatus = data.optString("status", "");
            if (orderStatus.matches("(?i)failed|error|gagal|fail")) {
                return getFailMessage(data, msg);
            }
            // Cek dari rc
            String rc = data.optString("rc", "");
            if (!rc.isEmpty() && !"00".equals(rc) && !"0".equals(rc)) {
                return getFailMessage(data, msg);
            }
        }
        return null;
    }

    /**
     * Brute force: scan semua field string di data untuk cari kata gagal/fail.
     */
    private String bruteForceCheckFailed(JSONObject data) {
        String worstMatch = null;
        for (String key : data.keySet()) {
            Object val = data.opt(key);
            if (val instanceof String) {
                String str = (String) val;
                if (str.matches("(?i).*gagal.*|.*fail.*|.*error.*|.*invalid.*")) {
                    // Cari yang paling deskriptif (bukan cuma "Gagal")
                    String clean = sanitizeMessage(str);
                    if (!clean.isEmpty() && !clean.equalsIgnoreCase("gagal")) {
                        return clean; // langsung return yang ada detailnya
                    }
                    if (worstMatch == null) {
                        worstMatch = clean;
                    }
                }
            }
        }
        return worstMatch;
    }

    /** Ambil pesan error dari data response, tanpa info saldo */
    private String getFailMessage(JSONObject data, String defaultMsg) {
        // Prioritas: reason → message → status_description → provider_message
        String raw = data.optString("reason", "");
        if (raw.isEmpty()) raw = defaultMsg;
        if (raw.isEmpty()) raw = data.optString("status_description", "");
        if (raw.isEmpty()) raw = data.optString("provider_message", "");
        if (raw.isEmpty()) return "Gagal";
        return sanitizeMessage(raw);
    }

    /** Hapus info saldo/refund dari pesan H2H */
    private String sanitizeMessage(String msg) {
        // Hapus bagian "[REFUND] Saldo ..."
        msg = msg.replaceAll("\\s*\\[REFUND\\][^@]*(@\\d{2}/\\d{2}\\s*\\d{2}:\\d{2})?", "");
        // Hapus "Saldo ..." dari message
        msg = msg.replaceAll("\\s*Saldo\\s+[\\d.,]+\\s*-\\s*[\\d.,]+\\s*=\\s*[\\d.,]+", "");
        msg = msg.replaceAll("\\s*Saldo\\s+[\\d.,]+", "");
        // Hapus trailing " @tanggal"
        msg = msg.replaceAll("\\s+@\\d{2}/\\d{2}\\s*\\d{2}:\\d{2}", "");
        // Bersihkan spasi ganda
        msg = msg.replaceAll("\\s+", " ").trim();
        // Hapus titik di akhir
        while (msg.endsWith(".") || msg.endsWith(" ")) {
            msg = msg.substring(0, msg.length() - 1).trim();
        }
        return msg.isEmpty() ? "Gagal" : msg;
    }

    /**
     * Polling status H2H terus sampai dapat respon sukses/gagal.
     */
    private void pollH2HStatus(long chatId, Transaction transaction, Product product, String destination) {
        String refId = transaction.getRefId();
        int pollCount = 0;

        while (true) {
            try {
                Thread.sleep(15000);
                pollCount++;

                JSONObject statusResult = H2HClient.checkStatus(refId);
                System.out.println("H2H status poll #" + pollCount + ": " + statusResult);

                // Debug: log semua field di data
                JSONObject data = statusResult.optJSONObject("data");
                if (data != null) {
                    StringBuilder keys = new StringBuilder("H2H poll data keys: ");
                    for (String key : data.keySet()) {
                        keys.append(key).append(", ");
                    }
                    System.out.println(keys.toString());
                }

                // Cek sukses (hanya jika top-level status true)
                if (statusResult.optBoolean("status", false)) {
                    String sn = checkH2HResponseForSuccess(statusResult);
                    if (sn != null) {
                        System.out.println("H2H poll #" + pollCount + ": SUCCESS");
                        transactionDAO.updateStatus(transaction.getId(), "success", sn);
                        sendOrderResult(chatId, true, transaction, product, destination, sn, null);
                        return;
                    }
                }

                // Cek gagal — scan SEMUA field (data + top-level)
                String failMsg = checkH2HResponseForFailed(statusResult);
                if (failMsg == null && data != null) {
                    failMsg = bruteForceCheckFailed(data);
                }
                if (failMsg == null) {
                    String topMsg = statusResult.optString("message", "");
                    if (topMsg.matches("(?i).*gagal.*|.*fail.*|.*error.*|.*invalid.*")) {
                        failMsg = sanitizeMessage(topMsg);
                    }
                }
                
                if (failMsg != null) {
                    System.out.println("H2H poll #" + pollCount + ": FAILED: " + failMsg);
                    transactionDAO.updateStatus(transaction.getId(), "failed", "");
                    sendOrderResult(chatId, false, transaction, product, destination, "", failMsg);
                    return;
                }
                
                System.out.println("H2H poll #" + pollCount + ": masih pending, lanjut...");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("H2H poll error: " + e.getMessage());
                // Lanjut polling terus
            }
        }
    }

    /**
     * Helper untuk kirim hasil order (sukses/gagal) dengan format baru + tombol CS.
     */
    private void sendOrderResult(long chatId, boolean isSuccess, Transaction transaction,
                                  Product product, String destination, String sn, String errorMsg) {
        String waktu = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
        String harga = "Rp" + String.format("%,.0f", product.getSellingPrice());
        String refId = transaction.getRefId();
        String tujuan = destination;
        String produk = product.getName();
        // Jika error "Saldo tidak cukup", ganti dengan "Mohon hubungi Customer Service"
        if (errorMsg != null && errorMsg.toLowerCase().contains("saldo tidak cukup")) {
            errorMsg = "Mohon hubungi Customer Service";
        }

        String text;
        if (isSuccess) {
            String snDisplay = (sn != null && !sn.isEmpty()) ? sn : "-";
            text = "✅ <b>PESANAN BERHASIL</b> ✅\n\n"
                + "<b>- Nomor Tujuan:</b> " + tujuan + "\n"
                + "<b>- Produk:</b> " + produk + "\n"
                + "<b>- Harga:</b> " + harga + "\n"
                + "<b>- Waktu:</b> " + waktu + "\n"
                + "<b>- Ref ID:</b> <code>" + refId + "</code>\n"
                + "<b>- SN:</b> <code>" + snDisplay + "</code>\n\n"
                + "Pesanan telah berhasil diproses dan dikirim ke akun kamu. "
                + "Terima kasih telah berbelanja di <b>KuhStore</b> 🙏";
        } else {
            String snFailed = (errorMsg != null && !errorMsg.isEmpty()) ? errorMsg : "-";
            text = "❌ <b>PESANAN GAGAL</b> ❌\n\n"
                + "<b>- Nomor Tujuan:</b> " + tujuan + "\n"
                + "<b>- Produk:</b> " + produk + "\n"
                + "<b>- Harga:</b> " + harga + "\n"
                + "<b>- Waktu:</b> " + waktu + "\n"
                + "<b>- Ref ID:</b> <code>" + refId + "</code>\n"
                + "<b>- SN:</b> <code>" + snFailed + "</code>\n\n"
                + "Silahkan hubungi Customer Service untuk bantuan.";
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText("💬 Hubungi Customer Service");
        btn.setUrl("https://t.me/ramadhankukuh");
        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(btn);
        rows.add(row);
        markup.setKeyboard(rows);

        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setParseMode("HTML");
        msg.setText(text);
        msg.setReplyMarkup(markup);
        bot.sendMessage(msg);
    }

    /**
     * Polling otomatis cek status pembayaran Midtrans.
     * Cek setiap 15 detik, maksimal 5 menit (20 kali).
     */
    private void pollPaymentStatus(long chatId, String orderId, int transactionId,
                                    Product product, String destination) {
        int maxAttempts = 20;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(15000); // 15 detik

                JSONObject status = com.kuhstore.api.MidtransClient.checkStatus(orderId);
                String transactionStatus = status.optString("transaction_status", "");

                if ("settlement".equals(transactionStatus) || "capture".equals(transactionStatus)) {
                    // Pembayaran berhasil!
                    bot.sendMessage(chatId, "✅ <b>Pembayaran diterima!</b> Memproses pesanan, mohon tunggu...");
                    Transaction t = transactionDAO.findById(transactionId);
                    if (t != null) {
                        processOrderToH2H(chatId, t, product, destination);
                    }
                    return;
                } else if ("expire".equals(transactionStatus) || "cancel".equals(transactionStatus)
                        || "deny".equals(transactionStatus)) {
                    // Pembayaran gagal/kadaluarsa — bersihkan state checkout
                    checkoutState.remove(chatId);
                    transactionDAO.updateStatus(transactionId, "failed", "");
                    bot.sendMessage(chatId, "❌ <b>Pembayaran gagal atau kadaluarsa.</b>\n"
                            + "Silakan order ulang. Ref: " + orderId);
                    return;
                }
                // pending → lanjut polling
                System.out.println("Polling #" + (i + 1) + " for " + orderId + ": " + transactionStatus);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("Polling error for " + orderId + ": " + e.getMessage());
                // Lanjut polling, mungkin error sementara
            }
        }

        // Timeout setelah 5 menit — bersihkan state checkout
        checkoutState.remove(chatId);
        bot.sendMessage(chatId, "⏰ <b>Waktu pembayaran habis.</b>\n"
                + "QRIS sudah kadaluarsa. Silakan order ulang.");
        try {
            transactionDAO.updateStatus(transactionId, "failed", "");
        } catch (Exception e) {
            System.err.println("Error updating expired transaction: " + e.getMessage());
        }
    }

    private void confirmPaymentAndProcess(long chatId, int transactionId) {
        try {
            checkoutState.remove(chatId);

            Transaction transaction = transactionDAO.findById(transactionId);
            if (transaction == null) {
                bot.sendMessage(chatId, "❌ Transaksi tidak ditemukan.");
                return;
            }

            Product product = productDAO.findById(transaction.getProductId());
            String dest = transaction.getDestination();

            bot.sendMessage(chatId, "🔍 Mengecek status pembayaran...");

            // Cek status pembayaran ke Midtrans
            try {
                JSONObject status = com.kuhstore.api.MidtransClient.checkStatus(transaction.getMidtransOrderId());
                String transactionStatus = status.optString("transaction_status", "");

                if ("settlement".equals(transactionStatus) || "capture".equals(transactionStatus)
                        || "success".equals(transactionStatus)) {
                    // Bayar berhasil, proses ke H2H
                    transactionDAO.updateStatus(transaction.getId(), "pending", "");
                    bot.sendMessage(chatId, "✅ Pembayaran diterima! Memproses pesanan, mohon tunggu...");
                    processOrderToH2H(chatId, transaction, product, dest);
                } else if ("pending".equals(transactionStatus) || "deny".equals(transactionStatus)) {
                    bot.sendMessage(chatId, "⏳ Pembayaran belum dikonfirmasi. Silakan bayar dulu melalui link yang dikirim.\n\n"
                            + "Ketik /menu untuk kembali.");
                } else if ("expire".equals(transactionStatus) || "cancel".equals(transactionStatus)) {
                    transactionDAO.updateStatus(transaction.getId(), "failed", "");
                    bot.sendMessage(chatId, "❌ Pembayaran kadaluarsa/dibatalkan. Silakan order ulang.");
                } else {
                    // Jika gagal cek, langsung coba proses
                    bot.sendMessage(chatId, "ℹ️ Memproses pesanan...");
                    processOrderToH2H(chatId, transaction, product, dest);
                }
            } catch (Exception e) {
                // Jika Midtrans error, tetap coba proses ke H2H
                System.err.println("Midtrans check error: " + e.getMessage());
                bot.sendMessage(chatId, "ℹ️ Memproses pesanan langsung...");
                processOrderToH2H(chatId, transaction, product, dest);
            }

        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Error: " + e.getMessage());
        }
    }

    private void showPulsaCategories(long chatId) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null || !member.isVerified()) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar. Ketik /daftar untuk mendaftar.");
                return;
            }
            bot.sendMessage(chatId, "📱 <b>Pulsa & Paket Data</b>\n\n"
                    + "Fitur ini sedang dalam pengembangan. Silakan hubungi admin untuk informasi lebih lanjut.");
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan.");
        }
    }

    private void handlePLN(long chatId) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null || !member.isVerified()) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar. Ketik /daftar untuk mendaftar.");
                return;
            }
            bot.sendMessage(chatId, "⚡ <b>Token PLN</b>\n\n"
                    + "Fitur ini sedang dalam pengembangan. Silakan hubungi admin untuk informasi lebih lanjut.");
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan.");
        }
    }

    private void showEWallet(long chatId) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null || !member.isVerified()) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar. Ketik /daftar untuk mendaftar.");
                return;
            }
            bot.sendMessage(chatId, "💳 <b>E-Wallet</b>\n\n"
                    + "Fitur ini sedang dalam pengembangan. Silakan hubungi admin.");
        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Terjadi kesalahan.");
        }
    }

    private void showTransactionHistory(long chatId, User from) {
        try {
            Member member = memberDAO.findByTelegramId(chatId);
            if (member == null) {
                bot.sendMessage(chatId, "⛔ Anda belum terdaftar. Ketik /daftar untuk mendaftar.");
                return;
            }

            var transactions = transactionDAO.findByMemberId(member.getId());
            if (transactions.isEmpty()) {
                bot.sendMessage(chatId, "📊 Belum ada transaksi.");
                return;
            }

            StringBuilder sb = new StringBuilder("📊 <b>Riwayat Transaksi</b>\n\n");
            int count = 0;
            for (var t : transactions) {
                if (count >= 10) break; // max 10 transaksi terakhir
                String statusIcon;
                switch (t.getStatus()) {
                    case "success": statusIcon = "✅"; break;
                    case "failed":  statusIcon = "❌"; break;
                    default:        statusIcon = "⏳"; break;
                }
                sb.append(statusIcon).append(" <b>").append(t.getProductName() != null ? t.getProductName() : "Produk").append("</b>\n");
                sb.append("   Ref: ").append(t.getRefId()).append("\n");
                sb.append("   Tujuan: ").append(t.getDestination()).append("\n");
                sb.append("   Status: ").append(t.getStatus()).append("\n\n");
                count++;
            }
            bot.sendMessage(chatId, sb.toString());

        } catch (Exception e) {
            bot.sendMessage(chatId, "⚠️ Gagal memuat riwayat transaksi.");
            System.err.println("Error showTransactionHistory: " + e.getMessage());
        }
    }

}
