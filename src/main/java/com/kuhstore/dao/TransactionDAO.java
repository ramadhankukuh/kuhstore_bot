package com.kuhstore.dao;

import com.kuhstore.model.Transaction;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    public Transaction findById(int id) throws SQLException {
        String sql = "SELECT t.*, m.full_name AS member_name, p.name AS product_name "
                + "FROM transactions t "
                + "LEFT JOIN members m ON t.member_id = m.id "
                + "LEFT JOIN products p ON t.product_id = p.id "
                + "WHERE t.id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Transaction findByRefId(String refId) throws SQLException {
        String sql = "SELECT t.*, m.full_name AS member_name, p.name AS product_name "
                + "FROM transactions t "
                + "LEFT JOIN members m ON t.member_id = m.id "
                + "LEFT JOIN products p ON t.product_id = p.id "
                + "WHERE t.ref_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, refId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Transaction> findAll() throws SQLException {
        return findByStatus(null);
    }

    public List<Transaction> findByStatus(String status) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, m.full_name AS member_name, p.name AS product_name "
                + "FROM transactions t "
                + "LEFT JOIN members m ON t.member_id = m.id "
                + "LEFT JOIN products p ON t.product_id = p.id ";
        if (status != null && !status.isEmpty()) {
            sql += "WHERE t.status = ? ";
        }
        sql += "ORDER BY t.created_at DESC";

        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            if (status != null && !status.isEmpty()) {
                ps.setString(1, status);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Transaction> findByMemberId(int memberId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = "SELECT t.*, m.full_name AS member_name, p.name AS product_name "
                + "FROM transactions t "
                + "LEFT JOIN members m ON t.member_id = m.id "
                + "LEFT JOIN products p ON t.product_id = p.id "
                + "WHERE t.member_id = ? ORDER BY t.created_at DESC";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int countToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE DATE(created_at) = CURDATE()";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM transactions WHERE status = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public double sumTodayRevenue() throws SQLException {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE DATE(created_at) = CURDATE() AND status = 'success'";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0;
    }

    public void insert(Transaction transaction) throws SQLException {
        String sql = "INSERT INTO transactions (ref_id, member_id, product_id, destination, amount, status, "
                + "serial_number, h2h_invoice, payment_method, midtrans_order_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, transaction.getRefId());
            ps.setInt(2, transaction.getMemberId());
            ps.setInt(3, transaction.getProductId());
            ps.setString(4, transaction.getDestination());
            ps.setDouble(5, transaction.getAmount());
            ps.setString(6, transaction.getStatus());
            ps.setString(7, transaction.getSerialNumber());
            ps.setString(8, transaction.getH2hInvoice());
            ps.setString(9, transaction.getPaymentMethod());
            ps.setString(10, transaction.getMidtransOrderId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) transaction.setId(rs.getInt(1));
            }
        }
    }

    public void updateStatus(int id, String status, String serialNumber) throws SQLException {
        String sql = "UPDATE transactions SET status=?, serial_number=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, serialNumber);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void updateMidtransOrderId(int id, String midtransOrderId) throws SQLException {
        String sql = "UPDATE transactions SET midtrans_order_id=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, midtransOrderId);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("id"));
        t.setRefId(rs.getString("ref_id"));
        t.setMemberId(rs.getInt("member_id"));
        t.setProductId(rs.getInt("product_id"));
        t.setDestination(rs.getString("destination"));
        t.setAmount(rs.getDouble("amount"));
        t.setStatus(rs.getString("status"));
        t.setSerialNumber(rs.getString("serial_number"));
        t.setH2hInvoice(rs.getString("h2h_invoice"));
        t.setPaymentMethod(rs.getString("payment_method"));
        t.setMidtransOrderId(rs.getString("midtrans_order_id"));
        t.setCreatedAt(rs.getTimestamp("created_at"));
        t.setUpdatedAt(rs.getTimestamp("updated_at"));
        try { t.setMemberName(rs.getString("member_name")); } catch (SQLException ignored) {}
        try { t.setProductName(rs.getString("product_name")); } catch (SQLException ignored) {}
        return t;
    }
}
