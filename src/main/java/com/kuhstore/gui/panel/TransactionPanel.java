package com.kuhstore.gui.panel;

import com.kuhstore.api.H2HClient;
import com.kuhstore.dao.TransactionDAO;
import com.kuhstore.model.Transaction;
import org.json.JSONObject;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk melihat history transaksi dengan filter dan export.
 */
public class TransactionPanel extends JPanel {

    private final TransactionDAO transactionDAO;
    private JTable table;
    private DefaultTableModel tableModel;
    private JComboBox<String> statusFilter;

    public TransactionPanel() {
        this.transactionDAO = new TransactionDAO();
        initComponents();
        loadData(null);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        toolbar.add(new JLabel("Filter Status:"));
        statusFilter = new JComboBox<>(new String[]{"Semua", "pending", "success", "failed"});
        statusFilter.addActionListener(e -> {
            String selected = (String) statusFilter.getSelectedItem();
            loadData("Semua".equals(selected) ? null : selected);
        });
        toolbar.add(statusFilter);

        JButton cekStatusButton = new JButton("🔍 Cek Status H2H");
        cekStatusButton.addActionListener(e -> checkH2HStatus());
        toolbar.add(cekStatusButton);

        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadData(null));
        toolbar.add(refreshButton);

        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Ref ID", "Member", "Produk", "Tujuan", "Amount", "Status", "SN", "Tanggal"};
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

    private void loadData(String status) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Transaction> transactions;
                if (status != null && !status.isEmpty()) {
                    transactions = transactionDAO.findByStatus(status);
                } else {
                    transactions = transactionDAO.findAll();
                }

                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Transaction t : transactions) {
                        tableModel.addRow(new Object[]{
                            t.getId(), t.getRefId(),
                            t.getMemberName() != null ? t.getMemberName() : "N/A",
                            t.getProductName() != null ? t.getProductName() : "N/A",
                            t.getDestination(),
                            String.format("Rp%,.0f", t.getAmount()),
                            t.getStatus(),
                            t.getSerialNumber() != null ? t.getSerialNumber() : "-",
                            t.getCreatedAt()
                        });
                    }
                });
                return null;
            }
        };
        worker.execute();
    }

    private void checkH2HStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi terlebih dahulu!");
            return;
        }

        try {
            String refId = (String) tableModel.getValueAt(row, 1);
            JSONObject response = H2HClient.checkStatus(refId);
            
            StringBuilder info = new StringBuilder("🔍 Status H2H untuk " + refId + ":\n\n");
            if (response.optBoolean("status", false)) {
                JSONObject data = response.getJSONObject("data");
                info.append("Status: ").append(data.optString("status", "N/A")).append("\n");
                info.append("SN: ").append(data.optString("sn", "-")).append("\n");
                info.append("Pesan: ").append(data.optString("message", "-"));
            } else {
                info.append("Gagal: ").append(response.optString("message", "Unknown"));
            }

            JOptionPane.showMessageDialog(this, info.toString(), "Cek Status H2H",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
