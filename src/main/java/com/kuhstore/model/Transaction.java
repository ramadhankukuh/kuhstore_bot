package com.kuhstore.model;

import java.sql.Timestamp;

/**
 * Model untuk tabel transactions.
 */
public class Transaction {
    private int id;
    private String refId;
    private int memberId;
    private int productId;
    private String destination;    // nomor HP / ID game / meter PLN
    private double amount;
    private String status;         // pending / success / failed
    private String serialNumber;   // SN/token dari H2H
    private String h2hInvoice;
    private String paymentMethod;
    private String midtransOrderId;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Join fields (optional)
    private String memberName;
    private String productName;

    public Transaction() {}

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRefId() { return refId; }
    public void setRefId(String refId) { this.refId = refId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }

    public String getH2hInvoice() { return h2hInvoice; }
    public void setH2hInvoice(String h2hInvoice) { this.h2hInvoice = h2hInvoice; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getMidtransOrderId() { return midtransOrderId; }
    public void setMidtransOrderId(String midtransOrderId) { this.midtransOrderId = midtransOrderId; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", refId='" + refId + "', status='" + status + "'}";
    }
}
