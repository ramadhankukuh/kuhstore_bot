package com.kuhstore.api;

import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utility HTTP GET/POST menggunakan OkHttp.
 */
public class HttpHelper {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * HTTP GET request.
     */
    public static String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * HTTP POST request with JSON body.
     */
    public static String post(String url, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * HTTP POST request with JSON body and Basic Auth header.
     */
    public static String postWithAuth(String url, String jsonBody, String serverKey) throws IOException {
        String credential = Credentials.basic(serverKey, "");
        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", credential)
                .header("Content-Type", "application/json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }

    /**
     * HTTP GET request with Basic Auth header.
     */
    public static String getWithAuth(String url, String serverKey) throws IOException {
        String credential = Credentials.basic(serverKey, "");
        Request request = new Request.Builder()
                .url(url)
                .get()
                .header("Authorization", credential)
                .build();
        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "";
        }
    }
}
