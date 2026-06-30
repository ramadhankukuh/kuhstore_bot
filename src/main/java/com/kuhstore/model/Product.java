package com.kuhstore.model;

import java.sql.Timestamp;

/**
 * Model untuk tabel products (produk dari H2H).
 */
public class Product {
    private int id;
    private String h2hCode;
    private String name;
    private String category;  // pulsa, voucher_game, pln, e_wallet, dll
    private double price;
    private double sellingPrice;
    private String status;    // OPEN / CLOSED
    private Timestamp updatedAt;

    public Product() {}

    public Product(int id, String h2hCode, String name, String category, double price,
                   double sellingPrice, String status, Timestamp updatedAt) {
        this.id = id;
        this.h2hCode = h2hCode;
        this.name = name;
        this.category = category;
        this.price = price;
        this.sellingPrice = sellingPrice;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getH2hCode() { return h2hCode; }
    public void setH2hCode(String h2hCode) { this.h2hCode = h2hCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", code='" + h2hCode + "', name='" + name + "', price=" + sellingPrice + "}";
    }
}
