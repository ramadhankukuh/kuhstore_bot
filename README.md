# 🏪 KuhStore Bot

**KuhStore** adalah bot Telegram untuk transaksi PPOB (Pulsa, Voucher Game, Token PLN, E-Wallet, dll.) secara otomatis yang dibangun dengan **Java 11** dan **Swing GUI** untuk manajemen admin.

> Proyek ini dibuat untuk tugas **UAS PBO (Pemrograman Berorientasi Objek) – Genap 2025/2026 UDINUS**  

---

## ✨ Fitur

### 🤖 Telegram Bot

- **Top-Up & Produk Digital** — Pembelian pulsa, voucher game, token PLN, e-wallet melalui H2H API
- **Pembayaran via Midtrans** — QRIS, Virtual Account, dan metode pembayaran lainnya
- **Auto-reply Keywords** — Balas otomatis berdasarkan kata kunci yang dikonfigurasi
- **Broadcast Message** — Kirim pesan ke seluruh member via GUI admin
- **Cek Saldo & Riwayat** — Member dapat cek saldo dan histori transaksi

### 🖥️ Admin GUI (Java Swing)

- **Login** — Autentikasi admin/operator dengan BCrypt
- **Dashboard** — Ringkasan total member, transaksi, dan pendapatan
- **Manajemen Member** — Lihat, cari, dan kelola data member
- **Manajemen Produk** — Kelola produk digital (pulsa, voucher, dll.)
- **Manajemen Transaksi** — Lihat dan monitor status transaksi
- **Manajemen Keyword** — Kelola auto-reply keywords
- **Broadcast** — Kirim pesan broadcast ke semua member
- **Manajemen User Admin** — Kelola akun admin/operator

---

## 🧱 Arsitektur

```
src/main/java/com/kuhstore/
├── api/           # H2H API Client, Midtrans Client, HTTP Helper
├── bot/           # Telegram Bot handlers & broadcast service
├── config/        # Konfigurasi aplikasi (database, bot, API)
├── dao/           # Data Access Object (MySQL)
├── gui/           # Admin Swing GUI
│   └── panel/     # Panel-panel dashboard
├── model/         # Model entities
└── KuhStore.java  # Entry point utama
```

---

## 📋 Prasyarat

| Kebutuhan          | Versi                                     |
| ------------------ | ----------------------------------------- |
| Java JDK           | 11 atau lebih                             |
| Apache Maven       | 3.6+                                      |
| MySQL Server       | 8.0+                                      |
| Telegram Bot Token | Dari [@BotFather](https://t.me/BotFather) |
| Akun H2H           | [h2h.id](https://h2h.id)                  |
| Akun Midtrans      | [midtrans.com](https://midtrans.com)      |

---

## 🚀 Cara Menjalankan

### 1. Clone & Build

```bash
git clone <repo-url>
cd kuhstore_bot
mvn clean compile
```

### 2. Setup Database

Jalankan script SQL untuk membuat database dan tabel:

```bash
mysql -u root -p < database/init.sql
```

### 3. Konfigurasi

Salin `config.properties.example` menjadi `config.properties`:

```bash
cp src/main/resources/config.properties.example src/main/resources/config.properties
```

Lalu isi credential yang diperlukan:

```properties
# Database
db.host=localhost
db.port=3306
db.name=kuhstore_db
db.user=root
db.pass=your_password

# Telegram Bot (dari @BotFather)
bot.token=YOUR_BOT_TOKEN
bot.username=KuhStoreBot

# H2H API
h2h.base_url=https://api.h2h.id/api/trx
h2h.member_id=YOUR_H2H_USERNAME
h2h.pin=YOUR_H2H_PIN
h2h.password=YOUR_H2H_PASSWORD

# Midtrans
midtrans.server_key=SB-Mid-server-XXXX
midtrans.client_key=SB-Mid-client-XXXX
midtrans.base_url=https://api.sandbox.midtrans.com/v2/
```

### 4. Jalankan

```bash
mvn exec:java
```

Atau:

```bash
mvn package
java -jar target/KuhStore-1.0-SNAPSHOT.jar
```

### 5. Login Admin

Buka GUI admin → Login dengan:
| Username | Password | Role |
|------------|------------|----------|
| `admin` | `admin123` | Admin |
| `operator` | `admin123` | Operator |

---

## 📦 Dependencies

| Library           | Versi    | Kegunaan                     |
| ----------------- | -------- | ---------------------------- |
| TelegramBots      | 6.8.0    | Telegram Bot API             |
| MySQL Connector/J | 8.0.33   | Koneksi database             |
| org.json          | 20231013 | Parsing JSON                 |
| OkHttp            | 4.12.0   | HTTP Client (H2H & Midtrans) |
| jBCrypt           | 0.4      | Hash password admin          |

---

## 🗄️ Database

Database MySQL dengan nama `kuhstore_db` berisi tabel:

- **users** — Akun admin/operator
- **members** — Pengguna Telegram
- **products** — Produk digital
- **transactions** — Riwayat transaksi
- **keywords** — Auto-reply keywords
- **messages** — Log pesan
- **broadcasts** — Riwayat broadcast

---

## 🛠️ Teknologi

- **Java 11** — Bahasa pemrograman
- **Java Swing** — GUI Admin
- **Maven** — Build tool
- **MySQL** — Database
- **Telegram Bot API** — Bot messaging
- **H2H API** — Penyedia layanan PPOB
- **Midtrans API** — Payment gateway

---
