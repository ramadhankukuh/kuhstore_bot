package com.kuhstore.api;

import com.kuhstore.config.AppConfig;
import org.json.JSONObject;

/**
 * Integrasi API H2H.id untuk layanan PPOB dan top-up game.
 */
public class H2HClient {

    private static final String BASE_URL = AppConfig.H2H_BASE_URL;
    private static final String MEMBER_ID = AppConfig.H2H_MEMBER_ID;
    private static final String PIN = AppConfig.H2H_PIN;
    private static final String PASSWORD = AppConfig.H2H_PASSWORD;

    /**
     * Generate refID unik.
     */
    public static String generateRefId(int memberId) {
        return "KUHSTORE_" + System.currentTimeMillis() + "_" + memberId;
    }

    /**
     * Cek saldo H2H.
     */
    public static JSONObject checkBalance() throws Exception {
        String url = BASE_URL + "/balance"
                + "?memberID=" + MEMBER_ID
                + "&pin=" + PIN
                + "&password=" + PASSWORD;
        String response = HttpHelper.get(url);
        return new JSONObject(response);
    }

    /**
     * Mendapatkan daftar produk (pricelist).
     */
    public static JSONObject getPricelist(String type) throws Exception {
        String url = BASE_URL + "/pricelist"
                + "?type=" + type
                + "&memberID=" + MEMBER_ID
                + "&pin=" + PIN
                + "&password=" + PASSWORD;
        String response = HttpHelper.get(url);
        return new JSONObject(response);
    }

    /**
     * Order produk (pulsa, voucher game, dll).
     */
    public static JSONObject orderProduct(String productCode, String destination, String refId) throws Exception {
        String url = BASE_URL + "/"
                + "?product=" + productCode
                + "&dest=" + destination
                + "&refID=" + refId
                + "&memberID=" + MEMBER_ID
                + "&pin=" + PIN
                + "&password=" + PASSWORD;
        String response = HttpHelper.get(url);
        return new JSONObject(response);
    }

    /**
     * Cek status transaksi berdasarkan refID.
     */
    public static JSONObject checkStatus(String refId) throws Exception {
        String url = BASE_URL + "/status"
                + "?refID=" + refId
                + "&memberID=" + MEMBER_ID
                + "&pin=" + PIN
                + "&password=" + PASSWORD;
        String response = HttpHelper.get(url);
        return new JSONObject(response);
    }

    /**
     * Validasi akun game.
     */
    public static JSONObject checkGameAccount(String game, String userId, String zoneId) throws Exception {
        String url = "https://api.h2h.id/api/game/check";
        JSONObject body = new JSONObject();
        body.put("game", game);
        body.put("user_id", userId);
        if (zoneId != null && !zoneId.isEmpty()) {
            body.put("zone_id", zoneId);
        }
        String response = HttpHelper.post(url, body.toString());
        return new JSONObject(response);
    }

    /**
     * Validasi meter PLN.
     */
    public static JSONObject checkPLNMeter(String meterId) throws Exception {
        String url = "https://api.h2h.id/api/pln/check";
        JSONObject body = new JSONObject();
        body.put("meter_id", meterId);
        String response = HttpHelper.post(url, body.toString());
        return new JSONObject(response);
    }
}
