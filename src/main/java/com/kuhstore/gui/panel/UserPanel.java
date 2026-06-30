package com.kuhstore.gui.panel;

import com.kuhstore.dao.UserDAO;
import com.kuhstore.model.User;
import org.mindrot.jbcrypt.BCrypt;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel untuk CRUD user admin.
 */
public class UserPanel extends JPanel {

    private final UserDAO userDAO;
    private JTable table;
    private DefaultTableModel tableModel;

    public UserPanel() {
        this.userDAO = new UserDAO();
        initComponents();
        loadData();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("➕ Tambah User");
        JButton editButton = new JButton("✏️ Edit User");
        JButton deleteButton = new JButton("🗑️ Hapus User");
        JButton refreshButton = new JButton("🔄 Refresh");

        addButton.addActionListener(e -> addUser());
        editButton.addActionListener(e -> editUser());
        deleteButton.addActionListener(e -> deleteUser());
        refreshButton.addActionListener(e -> loadData());

        toolbar.add(addButton);
        toolbar.add(editButton);
        toolbar.add(deleteButton);
        toolbar.add(refreshButton);
        add(toolbar, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Username", "Nama Lengkap", "Role", "Dibuat"};
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
                List<User> users = userDAO.findAll();
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (User u : users) {
                        tableModel.addRow(new Object[]{
                            u.getId(), u.getUsername(), u.getFullName(),
                            u.getRole(), u.getCreatedAt()
                        });
                    }
                });
                return null;
            }
        };
        worker.execute();
    }

    private void addUser() {
        JTextField usernameField = new JTextField();
        JTextField passwordField = new JPasswordField();
        JTextField fullNameField = new JTextField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"operator", "admin"});

        JPanel panel = new JPanel(new GridLayout(4, 2, 5, 5));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Nama Lengkap:"));
        panel.add(fullNameField);
        panel.add(new JLabel("Role:"));
        panel.add(roleCombo);

        int result = JOptionPane.showConfirmDialog(this, panel, "Tambah User Baru",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String username = usernameField.getText().trim();
                String password = passwordField.getText().trim();
                String fullName = fullNameField.getText().trim();

                if (username.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Username dan password harus diisi!");
                    return;
                }

                User user = new User();
                user.setUsername(username);
                user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
                user.setFullName(fullName);
                user.setRole((String) roleCombo.getSelectedItem());
                userDAO.insert(user);
                loadData();
                JOptionPane.showMessageDialog(this, "User berhasil ditambahkan!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih user yang akan diedit!");
            return;
        }

        int id = (int) tableModel.getValueAt(row, 0);
        try {
            User user = userDAO.findById(id);
            if (user == null) return;

            JTextField usernameField = new JTextField(user.getUsername());
            JTextField fullNameField = new JTextField(user.getFullName());
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"operator", "admin"});
            roleCombo.setSelectedItem(user.getRole());

            JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
            panel.add(new JLabel("Username:"));
            panel.add(usernameField);
            panel.add(new JLabel("Nama Lengkap:"));
            panel.add(fullNameField);
            panel.add(new JLabel("Role:"));
            panel.add(roleCombo);

            int result = JOptionPane.showConfirmDialog(this, panel, "Edit User",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                user.setUsername(usernameField.getText().trim());
                user.setFullName(fullNameField.getText().trim());
                user.setRole((String) roleCombo.getSelectedItem());
                userDAO.update(user);
                loadData();
                JOptionPane.showMessageDialog(this, "User berhasil diupdate!");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Pilih user yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus user ini?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                int id = (int) tableModel.getValueAt(row, 0);
                userDAO.delete(id);
                loadData();
                JOptionPane.showMessageDialog(this, "User berhasil dihapus!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
