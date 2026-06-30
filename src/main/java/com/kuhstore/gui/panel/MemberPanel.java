package com.kuhstore.gui.panel;

import com.kuhstore.dao.MemberDAO;
import com.kuhstore.model.Member;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk mengelola member Telegram.
 */
public class MemberPanel extends JPanel {

    private final MemberDAO memberDAO;
    private JTable table;
    private DefaultTableModel tableModel;

    public MemberPanel() {
        this.memberDAO = new MemberDAO();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshButton = new JButton("🔄 Refresh");
        JButton verifyButton = new JButton("✅ Verifikasi");
        JButton unverifyButton = new JButton("⛔ Blokir");

        refreshButton.addActionListener(e -> loadData());
        verifyButton.addActionListener(e -> toggleVerify(true));
        unverifyButton.addActionListener(e -> toggleVerify(false));

        toolbar.add(refreshButton);
        toolbar.add(verifyButton);
        toolbar.add(unverifyButton);
        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Telegram ID", "Username", "Nama", "No. HP", "Terverifikasi", "Saldo", "Bergabung"};
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
                List<Member> members = memberDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (Member m : members) {
                        tableModel.addRow(new Object[]{
                            m.getId(), m.getTelegramId(), m.getUsername(),
                            m.getFullName(), m.getPhone(),
                            m.isVerified() ? "✅ Ya" : "❌ Tidak",
                            String.format("Rp%,.0f", m.getBalance()),
                            m.getJoinedAt()
                        });
                    }
                });
                return null;
            }
        };
        worker.execute();
    }

    private void toggleVerify(boolean verify) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih member terlebih dahulu!");
            return;
        }

        try {
            int id = (int) tableModel.getValueAt(row, 0);
            Member member = memberDAO.findById(id);
            if (member != null) {
                member.setVerified(verify);
                memberDAO.update(member);
                loadData();
                JOptionPane.showMessageDialog(this, verify ? "Member diverifikasi!" : "Member diblokir!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
