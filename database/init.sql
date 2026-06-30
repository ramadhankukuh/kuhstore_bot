-- ============================================================
-- KuhStore Database Init Script
-- PBO UAS Genap 2025/2026 UDINUS
-- ============================================================

CREATE DATABASE IF NOT EXISTS kuhstore_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE kuhstore_db;

-- ============================================================
-- Tabel users (Admin)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,  -- BCrypt hash
    full_name VARCHAR(100),
    role ENUM('admin', 'operator') DEFAULT 'operator',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tabel members (Pengguna Telegram)
-- ============================================================
CREATE TABLE IF NOT EXISTS members (
    id INT AUTO_INCREMENT PRIMARY KEY,
    telegram_id BIGINT UNIQUE NOT NULL,
    username VARCHAR(100),
    full_name VARCHAR(100),
    phone VARCHAR(20),
    is_verified BOOLEAN DEFAULT FALSE,
    balance DECIMAL(15,2) DEFAULT 0.00,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tabel products (Produk dari H2H)
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    h2h_code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50),   -- pulsa, voucher_game, pln, e_wallet, dll
    price DECIMAL(15,2) NOT NULL,
    selling_price DECIMAL(15,2) NOT NULL,  -- harga jual ke member
    status ENUM('OPEN', 'CLOSED') DEFAULT 'OPEN',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tabel transactions
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ref_id VARCHAR(100) UNIQUE NOT NULL,
    member_id INT,
    product_id INT,
    destination VARCHAR(100) NOT NULL,   -- nomor HP / ID game / meter PLN
    amount DECIMAL(15,2) NOT NULL,
    status ENUM('pending','success','failed') DEFAULT 'pending',
    serial_number VARCHAR(255),          -- SN/token dari H2H
    h2h_invoice VARCHAR(100),
    payment_method VARCHAR(50),
    midtrans_order_id VARCHAR(100),
    game_username VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tabel keywords
-- ============================================================
CREATE TABLE IF NOT EXISTS keywords (
    id INT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) UNIQUE NOT NULL,
    response TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tabel messages (History pesan masuk/keluar)
-- ============================================================
CREATE TABLE IF NOT EXISTS messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT,
    direction ENUM('in','out') NOT NULL,
    content TEXT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Tabel broadcasts
-- ============================================================
CREATE TABLE IF NOT EXISTS broadcasts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(150),
    content TEXT NOT NULL,
    sent_by INT,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    recipient_count INT DEFAULT 0,
    FOREIGN KEY (sent_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Seed Data: Admin default
-- Password: admin123 (BCrypt hash)
-- ============================================================
INSERT INTO users (username, password, full_name, role) VALUES
('admin', '$2a$12$ZMYJgH0aAKNpg.mKtAkA0eeHcl46PclUo7LylNWT3AJwsZcP7Ldiy', 'Administrator', 'admin'),
('operator', '$2a$12$ZMYJgH0aAKNpg.mKtAkA0eeHcl46PclUo7LylNWT3AJwsZcP7Ldiy', 'Operator', 'operator');

-- ============================================================
-- Seed Data: Contoh keyword auto-reply
-- ============================================================
INSERT INTO keywords (keyword, response) VALUES
('halo', '👋 Halo! Selamat datang di KuhStore. Ada yang bisa kami bantu? Ketik /menu untuk melihat layanan kami.'),
('admin', '📞 Hubungi admin: @kuhstore_admin'),
('cara order', '📖 Cara Order:\n1. Pilih produk via menu\n2. Masukkan ID tujuan\n3. Lakukan pembayaran\n4. Produk akan diproses otomatis'),
('jam operasional', '🕐 Jam Operasional: 24 Jam Non-Stop 🚀');
