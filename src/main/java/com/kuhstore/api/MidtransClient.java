package com.kuhstore.api;

import com.kuhstore.config.AppConfig;
import org.json.JSONObject;

/**
 * Integrasi Midtrans Sandbox untuk pembayaran via QRIS.
 */
public class MidtransClient {

    private static final String CORE_API_URL = AppConfig.MIDTRANS_BASE_URL;
    private static final String SERVER_KEY = AppConfig.MIDTRANS_SERVER_KEY;

    /**
     * Membuat transaksi QRIS via Core API Midtrans.
     * Response berisi actions[] dengan URL gambar QRIS.
     *
     * @param orderId Order ID unik
     * @param amount  Jumlah pembayaran
     * @return JSONObject response dari Midtrans
     */
    public static JSONObject createQRISCharge(String orderId, long amount) throws Exception {
        JSONObject body = new JSONObject();
        body.put("payment_type", "qris");

        JSONObject transactionDetails = new JSONObject();
        transactionDetails.put("order_id", orderId);
        transactionDetails.put("gross_amount", amount);
        body.put("transaction_details", transactionDetails);

        String url = CORE_API_URL + "charge";
        String response = HttpHelper.postWithAuth(url, body.toString(), SERVER_KEY);
        return new JSONObject(response);
    }

    /**
     * Mengambil URL gambar QRIS dari response createQRISCharge.
     */
    public static String extractQRISImageUrl(JSONObject response) {
        if (response.has("actions")) {
            var actions = response.getJSONArray("actions");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                if ("generate-qr-code".equals(action.optString("name"))) {
                    return action.optString("url", "");
                }
            }
        }
        return "";
    }

    /**
     * Cek status transaksi Midtrans via Core API.
     *
     * @param orderId Order ID
     * @return JSONObject response
     */
    public static JSONObject checkStatus(String orderId) throws Exception {
        String url = CORE_API_URL + orderId + "/status";
        String response = HttpHelper.getWithAuth(url, SERVER_KEY);
        return new JSONObject(response);
    }
}
