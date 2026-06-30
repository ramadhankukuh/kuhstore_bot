package com.kuhstore.dao;

import com.kuhstore.model.Broadcast;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BroadcastDAO {

    public Broadcast findById(int id) throws SQLException {
        String sql = "SELECT b.*, u.full_name AS sent_by_name "
                + "FROM broadcasts b "
                + "LEFT JOIN users u ON b.sent_by = u.id "
                + "WHERE b.id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Broadcast> findAll() throws SQLException {
        List<Broadcast> list = new ArrayList<>();
        String sql = "SELECT b.*, u.full_name AS sent_by_name "
                + "FROM broadcasts b "
                + "LEFT JOIN users u ON b.sent_by = u.id "
                + "ORDER BY b.sent_at DESC";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void insert(Broadcast broadcast) throws SQLException {
        String sql = "INSERT INTO broadcasts (title, content, sent_by, recipient_count) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, broadcast.getTitle());
            ps.setString(2, broadcast.getContent());
            ps.setInt(3, broadcast.getSentBy());
            ps.setInt(4, broadcast.getRecipientCount());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) broadcast.setId(rs.getInt(1));
            }
        }
    }

    private Broadcast mapRow(ResultSet rs) throws SQLException {
        Broadcast b = new Broadcast();
        b.setId(rs.getInt("id"));
        b.setTitle(rs.getString("title"));
        b.setContent(rs.getString("content"));
        b.setSentBy(rs.getInt("sent_by"));
        b.setSentAt(rs.getTimestamp("sent_at"));
        b.setRecipientCount(rs.getInt("recipient_count"));
        try { b.setSentByName(rs.getString("sent_by_name")); } catch (SQLException ignored) {}
        return b;
    }
}
