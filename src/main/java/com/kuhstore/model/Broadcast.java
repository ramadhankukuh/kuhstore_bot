package com.kuhstore.model;

import java.sql.Timestamp;

/**
 * Model untuk tabel broadcasts.
 */
public class Broadcast {
    private int id;
    private String title;
    private String content;
    private int sentBy;
    private Timestamp sentAt;
    private int recipientCount;

    // Join field
    private String sentByName;

    public Broadcast() {}

    public Broadcast(int id, String title, String content, int sentBy, Timestamp sentAt, int recipientCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.sentBy = sentBy;
        this.sentAt = sentAt;
        this.recipientCount = recipientCount;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getSentBy() { return sentBy; }
    public void setSentBy(int sentBy) { this.sentBy = sentBy; }

    public Timestamp getSentAt() { return sentAt; }
    public void setSentAt(Timestamp sentAt) { this.sentAt = sentAt; }

    public int getRecipientCount() { return recipientCount; }
    public void setRecipientCount(int recipientCount) { this.recipientCount = recipientCount; }

    public String getSentByName() { return sentByName; }
    public void setSentByName(String sentByName) { this.sentByName = sentByName; }

    @Override
    public String toString() {
        return "Broadcast{id=" + id + ", title='" + title + "', recipients=" + recipientCount + "}";
    }
}
