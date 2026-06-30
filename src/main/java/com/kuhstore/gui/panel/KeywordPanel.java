package com.kuhstore.gui.panel;

import com.kuhstore.dao.KeywordDAO;
import com.kuhstore.model.Keyword;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk CRUD keyword & jawaban otomatis.
 */
public class KeywordPanel extends JPanel {

    private final KeywordDAO keywordDAO;
    private JTable table;
    private DefaultTableModel tableModel;

    public KeywordPanel() {
        this.keywordDAO = new KeywordDAO();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("➕ Tambah Keyword");
        JButton editButton = new JButton("✏️ Edit");
        JButton deleteButton = new JButton("🗑️ Hapus");
        JButton previewButton = new JButton("👁️ Preview");
        JButton refreshButton = new JButton("🔄 Refresh");

        addButton.addActionListener(e -> addKeyword());
        editButton.addActionListener(e -> editKeyword());
        deleteButton.addActionListener(e -> deleteKeyword());
        previewButton.addActionListener(e -> previewKeyword());
        refreshButton.addActionListener(e -> loadData());

        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(previewButton);
        toolbar.add(refreshButton);
        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Keyword", "Response"};
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
                List<Keyword> keywords = keywordDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Keyword k : keywords) {
                        String responsePreview = k.getResponse().length() > 50
                                ? k.getResponse().substring(0, 50) + "..."
                                : k.getResponse();
                        tableModel.addRow(new Object[]{k.getId(), k.getKeyword(), responsePreview});
                    }
                });
                return null;
            }
        };
        worker.execute();
    }

    private void addKeyword() {
        JTextField keywordField = new JTextField();
        JTextArea responseArea = new JTextArea(5, 30);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JPanel formPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        formPanel.add(new JLabel("Keyword:"));
        formPanel.add(keywordField);
        formPanel.add(new JLabel("Response:"));
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(responseArea), BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "Tambah Keyword Baru",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String keyword = keywordField.getText().trim().toLowerCase();
                String response = responseArea.getText().trim();

                if (keyword.isEmpty() || response.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Keyword dan response harus diisi!");
                    return;
                }

                Keyword k = new Keyword();
                k.setKeyword(keyword);
                k.setResponse(response);
                keywordDAO.insert(k);
                loadData();
                JOptionPane.showMessageDialog(this, "Keyword berhasil ditambahkan!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editKeyword() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih keyword yang akan diedit!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            Keyword keyword = keywordDAO.findById(id);
            if (keyword == null) return;

            JTextField keywordField = new JTextField(keyword.getKeyword());
            JTextArea responseArea = new JTextArea(keyword.getResponse(), 5, 30);

            JPanel panel = new JPanel(new BorderLayout(5, 5));
            JPanel formPanel = new JPanel(new GridLayout(2, 1, 5, 5));
            formPanel.add(new JLabel("Keyword:"));
            formPanel.add(keywordField);
            formPanel.add(new JLabel("Response:"));
            panel.add(formPanel, BorderLayout.NORTH);
            panel.add(new JScrollPane(responseArea), BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(this, panel, "Edit Keyword",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                keyword.setKeyword(keywordField.getText().trim().toLowerCase());
                keyword.setResponse(responseArea.getText().trim());
                keywordDAO.update(keyword);
                loadData();
                JOptionPane.showMessageDialog(this, "Keyword berhasil diupdate!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteKeyword() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih keyword yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus keyword ini?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int id = (int) tableModel.getValueAt(row, 0);
                keywordDAO.delete(id);
                loadData();
                JOptionPane.showMessageDialog(this, "Keyword berhasil dihapus!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void previewKeyword() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih keyword untuk preview!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            Keyword keyword = keywordDAO.findById(id);
            if (keyword != null) {
                JTextArea textArea = new JTextArea(keyword.getResponse());
                textArea.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(textArea),
                        "Response untuk keyword: \"" + keyword.getKeyword() + "\"",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
