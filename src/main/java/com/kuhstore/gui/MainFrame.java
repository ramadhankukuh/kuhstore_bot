package com.kuhstore.gui;

import com.kuhstore.KuhStore;
import com.kuhstore.gui.panel.*;
import com.kuhstore.model.User;
import javax.swing.*;
import java.awt.*;

/**
 * Jendela utama admin GUI KuhStore.
 */
public class MainFrame extends JFrame {

    private final User currentUser;
    private JTabbedPane tabbedPane;

    private DashboardPanel dashboardPanel;
    private UserPanel userPanel;
    private MemberPanel memberPanel;
    private ProductPanel productPanel;
    private TransactionPanel transactionPanel;
    private KeywordPanel keywordPanel;
    private BroadcastPanel broadcastPanel;

    public MainFrame(User currentUser) {
        this.currentUser = currentUser;
        initComponents();
    }

    private void initComponents() {
        setTitle("KuhStore Admin - " + currentUser.getFullName() + " (" + currentUser.getRole() + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem logoutItem = new JMenuItem("Logout");
        logoutItem.addActionListener(e -> doLogout());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(logoutItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "KuhStore Admin Panel v1.0\nPBO UAS Genap 2025/2026 UDINUS", "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        // Tabbed pane
        tabbedPane = new JTabbedPane();

        dashboardPanel = new DashboardPanel();
        userPanel = new UserPanel();
        memberPanel = new MemberPanel();
        productPanel = new ProductPanel();
        transactionPanel = new TransactionPanel();
        keywordPanel = new KeywordPanel();
        broadcastPanel = new BroadcastPanel();
        broadcastPanel.setSendListener((title, content, onlyVerified) -> {
            if (KuhStore.getBroadcastService() != null) {
                KuhStore.getBroadcastService().sendBroadcast(
                    title, content, currentUser.getId(), onlyVerified
                );
            } else {
                JOptionPane.showMessageDialog(this,
                    "Bot belum siap. Tunggu beberapa saat dan coba lagi.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        tabbedPane.addTab("📊 Dashboard", dashboardPanel);
        tabbedPane.addTab("👤 Users", userPanel);
        tabbedPane.addTab("👥 Members", memberPanel);
        tabbedPane.addTab("📦 Products", productPanel);
        tabbedPane.addTab("💳 Transactions", transactionPanel);
        tabbedPane.addTab("🔑 Keywords", keywordPanel);
        tabbedPane.addTab("📢 Broadcast", broadcastPanel);

        // Nonaktifkan tab yang hanya untuk admin
        if (!currentUser.getRole().equals("admin")) {
            tabbedPane.setEnabledAt(1, false); // Users tab
        }

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        JLabel statusLabel = new JLabel("Logged in as: " + currentUser.getUsername() + " | Role: " + currentUser.getRole());
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private void doLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dispose();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
        }
    }
}
