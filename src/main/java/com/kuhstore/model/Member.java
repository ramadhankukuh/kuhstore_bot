package com.kuhstore.model;

import java.sql.Timestamp;

/**
 * Model untuk tabel members (pengguna Telegram).
 */
public class Member {
    private int id;
    private long telegramId;
    private String username;
    private String fullName;
    private String phone;
    private boolean isVerified;
    private double balance;
    private Timestamp joinedAt;

    public Member() {}

    public Member(int id, long telegramId, String username, String fullName, String phone,
                  boolean isVerified, double balance, Timestamp joinedAt) {
        this.id = id;
        this.telegramId = telegramId;
        this.username = username;
        this.fullName = fullName;
        this.phone = phone;
        this.isVerified = isVerified;
        this.balance = balance;
        this.joinedAt = joinedAt;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTelegramId() { return telegramId; }
    public void setTelegramId(long telegramId) { this.telegramId = telegramId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isVerified() { return isVerified; }
    public void setVerified(boolean verified) { isVerified = verified; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public Timestamp getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Timestamp joinedAt) { this.joinedAt = joinedAt; }

    @Override
    public String toString() {
        return "Member{id=" + id + ", telegramId=" + telegramId + ", name='" + fullName + "'}";
    }
}
