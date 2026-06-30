package com.kuhstore.dao;

import com.kuhstore.model.Keyword;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KeywordDAO {

    public Keyword findById(int id) throws SQLException {
        String sql = "SELECT * FROM keywords WHERE id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public Keyword findByKeyword(String keyword) throws SQLException {
        String sql = "SELECT * FROM keywords WHERE keyword = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, keyword.toLowerCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Keyword> findAll() throws SQLException {
        List<Keyword> list = new ArrayList<>();
        String sql = "SELECT * FROM keywords ORDER BY keyword";
        try (Statement stmt = DBConnection.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public void insert(Keyword keyword) throws SQLException {
        String sql = "INSERT INTO keywords (keyword, response) VALUES (?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, keyword.getKeyword().toLowerCase().trim());
            ps.setString(2, keyword.getResponse());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) keyword.setId(rs.getInt(1));
            }
        }
    }

    public void update(Keyword keyword) throws SQLException {
        String sql = "UPDATE keywords SET keyword=?, response=? WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, keyword.getKeyword().toLowerCase().trim());
            ps.setString(2, keyword.getResponse());
            ps.setInt(3, keyword.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM keywords WHERE id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Keyword mapRow(ResultSet rs) throws SQLException {
        Keyword k = new Keyword();
        k.setId(rs.getInt("id"));
        k.setKeyword(rs.getString("keyword"));
        k.setResponse(rs.getString("response"));
        k.setCreatedAt(rs.getTimestamp("created_at"));
        return k;
    }
}
