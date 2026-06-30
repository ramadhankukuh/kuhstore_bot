package com.kuhstore.dao;

import com.kuhstore.model.Member;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public Member findById(int id) throws SQLException {
        String sql = "SELECT * FROM members WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Member findByTelegramId(long telegramId) throws SQLException {
        String sql = "SELECT * FROM members WHERE telegram_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setLong(1, telegramId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Member> findAll() throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY id";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Member> findVerified() throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT * FROM members WHERE is_verified = TRUE ORDER BY id";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM members";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void insert(Member member) throws SQLException {
        String sql = "INSERT INTO members (telegram_id, username, full_name, phone, is_verified, balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, member.getTelegramId());
            ps.setString(2, member.getUsername());
            ps.setString(3, member.getFullName());
            ps.setString(4, member.getPhone());
            ps.setBoolean(5, member.isVerified());
            ps.setDouble(6, member.getBalance());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) member.setId(rs.getInt(1));
            }
        }
    }

    public void update(Member member) throws SQLException {
        String sql = "UPDATE members SET username=?, full_name=?, phone=?, is_verified=?, balance=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, member.getUsername());
            ps.setString(2, member.getFullName());
            ps.setString(3, member.getPhone());
            ps.setBoolean(4, member.isVerified());
            ps.setDouble(5, member.getBalance());
            ps.setInt(6, member.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM members WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setId(rs.getInt("id"));
        m.setTelegramId(rs.getLong("telegram_id"));
        m.setUsername(rs.getString("username"));
        m.setFullName(rs.getString("full_name"));
        m.setPhone(rs.getString("phone"));
        m.setVerified(rs.getBoolean("is_verified"));
        m.setBalance(rs.getDouble("balance"));
        m.setJoinedAt(rs.getTimestamp("joined_at"));
        return m;
    }
}
