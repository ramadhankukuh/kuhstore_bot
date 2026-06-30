package com.kuhstore.model;

import java.sql.Timestamp;

/**
 * Model untuk tabel keywords (keyword & jawaban otomatis).
 */
public class Keyword {
    private int id;
    private String keyword;
    private String response;
    private Timestamp createdAt;

    public Keyword() {}

    public Keyword(int id, String keyword, String response, Timestamp createdAt) {
        this.id = id;
        this.keyword = keyword;
        this.response = response;
        this.createdAt = createdAt;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "Keyword{id=" + id + ", keyword='" + keyword + "'}";
    }
}
