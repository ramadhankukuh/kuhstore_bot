package com.kuhstore.gui.panel;

import com.kuhstore.api.H2HClient;
import com.kuhstore.dao.ProductDAO;
import com.kuhstore.model.Product;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk mengelola produk (sync dari H2H, atur harga jual, aktif/nonaktifkan).
 */
public class ProductPanel extends JPanel {

    private final ProductDAO productDAO;
    private JTable table;
    private DefaultTableModel tableModel;

    public ProductPanel() {
        this.productDAO = new ProductDAO();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton syncButton = new JButton("🔄 Sync dari H2H");
        JButton editPriceButton = new JButton("💰 Atur Harga Jual");
        JButton toggleButton = new JButton("🔁 Aktif/Nonaktifkan");
        JButton refreshButton = new JButton("🔄 Refresh");

        syncButton.addActionListener(e -> syncFromH2H());
        editPriceButton.addActionListener(e -> editSellingPrice());
        toggleButton.addActionListener(e -> toggleStatus());
        refreshButton.addActionListener(e -> loadData());

        toolbar.add(syncButton);
        toolbar.add(editPriceButton);
        toolbar.add(toggleButton);
        toolbar.add(refreshButton);
        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Kode H2H", "Nama Produk", "Kategori", "Harga Modal", "Harga Jual", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Product> products = productDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Product p : products) {
                        tableModel.addRow(new Object[]{
                            p.getId(), p.getH2hCode(), p.getName(), p.getCategory(),
                            String.format("Rp%,.0f", p.getPrice()),
                            String.format("Rp%,.0f", p.getSellingPrice()),
                            p.getStatus()
                        });
                    }
                });
                return null;
            }
        };
        worker.execute();
    }

    private void syncFromH2H() {
        String input = JOptionPane.showInputDialog(this, "Masukkan tipe produk (contoh: voucher_game, pulsa):");
        if (input == null || input.trim().isEmpty()) return;

        String type = input.trim();
        JOptionPane.showMessageDialog(this, "Menyinkronkan data dari H2H... Mohon tunggu.");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    JSONObject response = H2HClient.getPricelist(type);
                    if (response.optBoolean("status", false)) {
                        JSONArray data = response.getJSONArray("data");
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject item = data.getJSONObject(i);
                            // Field names from H2H API use Indonesian
                            String code = item.getString("kode");
                            String name = item.getString("keterangan");
                            // Gunakan type (voucher_game/pulsa/dll) sebagai kategori, bukan dari API
                            double price = Double.parseDouble(item.getString("harga"));
                            String itemStatus = item.optString("status", "1");

                            Product existing = productDAO.findByH2hCode(code);
                            if (existing != null) {
                                existing.setName(name);
                                existing.setPrice(price);
                                existing.setCategory(type);
                                existing.setStatus("1".equals(itemStatus) ? "OPEN" : "CLOSED");
                                productDAO.update(existing);
                            } else {
                                Product product = new Product();
                                product.setH2hCode(code);
                                product.setName(name);
                                product.setCategory(type);
                                product.setPrice(price);
                                product.setSellingPrice(price);
                                product.setStatus("1".equals(itemStatus) ? "OPEN" : "CLOSED");
                                productDAO.insert(product);
                            }
                        }
                        SwingUtilities.invokeLater(() -> {
                            loadData();
                            JOptionPane.showMessageDialog(ProductPanel.this,
                                    "Sinkronisasi selesai! " + data.length() + " produk diproses.");
                        });
                    } else {
                        SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(ProductPanel.this,
                                    "Gagal sinkron: " + response.optString("message", "Unknown error"),
                                    "Error", JOptionPane.ERROR_MESSAGE));
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(ProductPanel.this,
                                "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }
        };
        worker.execute();
    }

    private void editSellingPrice() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            Product product = productDAO.findById(id);
            if (product == null) return;

            String input = JOptionPane.showInputDialog(this,
                    "Harga jual saat ini: " + String.format("Rp%,.0f", product.getSellingPrice())
                    + "\nHarga modal: " + String.format("Rp%,.0f", product.getPrice())
                    + "\nMasukkan harga jual baru:",
                    product.getSellingPrice());

            if (input != null && !input.trim().isEmpty()) {
                double newPrice = Double.parseDouble(input.replaceAll("[^0-9]", ""));
                productDAO.updateSellingPrice(id, newPrice);
                loadData();
                JOptionPane.showMessageDialog(this, "Harga jual berhasil diupdate!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih produk terlebih dahulu!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            Product product = productDAO.findById(id);
            if (product == null) return;

            String newStatus = product.getStatus().equals("OPEN") ? "CLOSED" : "OPEN";
            productDAO.updateStatus(id, newStatus);
            loadData();
            JOptionPane.showMessageDialog(this, "Status produk diubah menjadi " + newStatus);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
