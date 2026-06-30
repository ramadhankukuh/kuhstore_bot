package com.kuhstore.gui.panel;

import com.kuhstore.dao.MemberDAO;
import com.kuhstore.dao.TransactionDAO;
import com.kuhstore.api.H2HClient;
import org.json.JSONObject;
import javax.swing.*;
import java.awt.*;

/**
 * Panel Dashboard — menampilkan statistik utama.
 */
public class DashboardPanel extends JPanel {

    private final MemberDAO memberDAO;
    private final TransactionDAO transactionDAO;

    private JLabel totalMemberLabel;
    private JLabel todayTransactionLabel;
    private JLabel todayRevenueLabel;
    private JLabel h2hBalanceLabel;
    private JLabel statusLabel;

    public DashboardPanel() {
        this.memberDAO = new MemberDAO();
        this.transactionDAO = new TransactionDAO();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Title
        JLabel title = new JLabel("📊 Dashboard KuhStore");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        // Stats cards
        JPanel cardsPanel = new JPanel(new GridLayout(2, 2, 15, 15));

        cardsPanel.add(createStatCard("Total Member", "0", new Color(0, 150, 136)));
        cardsPanel.add(createStatCard("Transaksi Hari Ini", "0", new Color(33, 150, 243)));
        cardsPanel.add(createStatCard("Pendapatan Hari Ini", "Rp0", new Color(76, 175, 80)));
        cardsPanel.add(createStatCard("Saldo H2H", "Rp0", new Color(255, 152, 0)));

        // Assign labels after creation
        totalMemberLabel = (JLabel) ((JPanel) ((JPanel) cardsPanel.getComponent(0)).getComponent(1)).getComponent(0);
        todayTransactionLabel = (JLabel) ((JPanel) ((JPanel) cardsPanel.getComponent(1)).getComponent(1)).getComponent(0);
        todayRevenueLabel = (JLabel) ((JPanel) ((JPanel) cardsPanel.getComponent(2)).getComponent(1)).getComponent(0);
        h2hBalanceLabel = (JLabel) ((JPanel) ((JPanel) cardsPanel.getComponent(3)).getComponent(1)).getComponent(0);

        add(cardsPanel, BorderLayout.CENTER);

        // Refresh button
        JButton refreshButton = new JButton("🔄 Refresh");
        refreshButton.addActionListener(e -> loadData());

        statusLabel = new JLabel("Memuat data...");

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(refreshButton);
        bottomPanel.add(Box.createHorizontalStrut(20));
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(color, 2));
        card.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(" " + title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(color);
        titleLabel.setOpaque(true);
        titleLabel.setBackground(color.brighter());
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valuePanel.add(valueLabel);
        card.add(valuePanel, BorderLayout.CENTER);

        return card;
    }

    private void loadData() {
        statusLabel.setText("Memuat data...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    int totalMembers = memberDAO.countAll();
                    int todayTransactions = transactionDAO.countToday();
                    double todayRevenue = transactionDAO.sumTodayRevenue();

                    SwingUtilities.invokeLater(() -> {
                        totalMemberLabel.setText(String.valueOf(totalMembers));
                        todayTransactionLabel.setText(String.valueOf(todayTransactions));
                        todayRevenueLabel.setText(String.format("Rp%,.0f", todayRevenue));
                    });

                    // Cek saldo H2H
                    try {
                        JSONObject balanceResponse = H2HClient.checkBalance();
                        if (balanceResponse.optBoolean("status", false)) {
                            double balance = balanceResponse.getJSONObject("data").optDouble("balance", 0);
                            SwingUtilities.invokeLater(() ->
                                h2hBalanceLabel.setText(String.format("Rp%,.0f", balance)));
                        }
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() ->
                            h2hBalanceLabel.setText("Gagal memuat"));
                    }

                } catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                        statusLabel.setText("Error: " + e.getMessage()));
                }
                return null;
            }

            @Override
            protected void done() {
                statusLabel.setText("Data diperbarui.");
            }
        };
        worker.execute();
    }
}
