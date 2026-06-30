package com.kuhstore.gui.panel;

import com.kuhstore.dao.BroadcastDAO;
import com.kuhstore.model.Broadcast;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk membuat dan melihat riwayat broadcast.
 */
public class BroadcastPanel extends JPanel {

    private final BroadcastDAO broadcastDAO;
    private JTable historyTable;
    private DefaultTableModel historyModel;

    // Form fields
    private JTextField titleField;
    private JTextArea contentArea;
    private JCheckBox onlyVerifiedCheck;
    private JButton sendButton;

    // Callback untuk mengirim broadcast
    private BroadcastSendListener sendListener;

    public interface BroadcastSendListener {
        void onSendBroadcast(String title, String content, boolean onlyVerified);
    }

    public void setSendListener(BroadcastSendListener listener) {
        this.sendListener = listener;
    }

    public BroadcastPanel() {
        this.broadcastDAO = new BroadcastDAO();
        initComponents();
        loadHistory();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Split pane: left = form, right = history
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Left Panel - Form Broadcast
        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("📢 Kirim Broadcast"));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        fieldsPanel.add(new JLabel("Judul:"), gbc);
        titleField = new JTextField(20);
        gbc.gridx = 1;
        gbc.weightx = 1;
        fieldsPanel.add(titleField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        fieldsPanel.add(new JLabel("Isi Pesan:"), gbc);
        contentArea = new JTextArea(8, 20);
        contentArea.setLineWrap(true);
        gbc.gridx = 1;
        gbc.weightx = 1;
        fieldsPanel.add(new JScrollPane(contentArea), gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        onlyVerifiedCheck = new JCheckBox("Hanya ke member terverifikasi", true);
        fieldsPanel.add(onlyVerifiedCheck, gbc);

        sendButton = new JButton("📤 Kirim Broadcast");
        sendButton.setBackground(new Color(0, 150, 136));
        sendButton.setForeground(Color.WHITE);
        sendButton.addActionListener(e -> doSendBroadcast());
        gbc.gridy = 3;
        fieldsPanel.add(sendButton, gbc);

        formPanel.add(fieldsPanel, BorderLayout.NORTH);

        // Right Panel - History Broadcast
        JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
        historyPanel.setBorder(BorderFactory.createTitledBorder("Riwayat Broadcast"));

        String[] columns = {"ID", "Judul", "Penerima", "Pengirim", "Tanggal"};
        historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        historyTable = new JTable(historyModel);
        historyTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        splitPane.setLeftComponent(formPanel);
        splitPane.setRightComponent(historyPanel);
        splitPane.setDividerLocation(400);

        add(splitPane, BorderLayout.CENTER);
    }

    private void doSendBroadcast() {
        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();
        boolean onlyVerified = onlyVerifiedCheck.isSelected();

        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Isi pesan tidak boleh kosong!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Yakin ingin mengirim broadcast?\n"
                + (onlyVerified ? "(Hanya member terverifikasi)" : "(Semua member)"),
                "Konfirmasi Broadcast", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (sendListener != null) {
                sendListener.onSendBroadcast(title, content, onlyVerified);
            }
            JOptionPane.showMessageDialog(this, "Broadcast sedang dikirim...");
            titleField.setText("");
            contentArea.setText("");
        }
    }

    private void loadHistory() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Broadcast> broadcasts = broadcastDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    historyModel.setRowCount(0);
                    for (Broadcast b : broadcasts) {
                        historyModel.addRow(new Object[]{
                            b.getId(),
                            b.getTitle() != null ? b.getTitle() : "-",
                            b.getRecipientCount(),
                            b.getSentByName() != null ? b.getSentByName() : "N/A",
                            b.getSentAt()
                        });
                    }
                });
                return null;
            }
        };
        worker.execute();
    }
}
