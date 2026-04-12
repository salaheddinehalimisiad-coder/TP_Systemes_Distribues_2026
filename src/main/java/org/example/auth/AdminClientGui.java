package org.example.auth;

import org.example.UIUtils;
import javax.swing.*;
import java.awt.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AdminClientGui extends JFrame {

    private IAuthService authService;
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblStatus;

    public AdminClientGui() {
        setTitle("RMI User Administration");
        setSize(480, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);

        // Header
        JPanel headerPanel = UIUtils.createCardPanel();
        headerPanel.setLayout(new BorderLayout(8, 8));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        headerPanel.add(UIUtils.createHeaderLabel("User Management"), BorderLayout.CENTER);

        lblStatus = new JLabel("Status: Disconnected");
        lblStatus.setFont(UIUtils.FONT_STATUS);
        lblStatus.setForeground(UIUtils.COLOR_TEXT_DIM);
        headerPanel.add(lblStatus, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = UIUtils.createCardPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(UIUtils.createLabel("Login:"), gbc);

        gbc.gridx = 1;
        txtUsername = UIUtils.createStyledTextField(15);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(UIUtils.createLabel("Password:"), gbc);

        gbc.gridx = 1;
        txtPassword = UIUtils.createStyledPasswordField(15);
        formPanel.add(txtPassword, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnAdd    = UIUtils.createStyledButton("Register", UIUtils.COLOR_SUCCESS);
        JButton btnUpdate = UIUtils.createStyledButton("Update",   UIUtils.COLOR_PRIMARY);
        JButton btnDelete = UIUtils.createStyledButton("Delete",   UIUtils.COLOR_DANGER);

        btnAdd.addActionListener(e -> handleAdd());
        btnUpdate.addActionListener(e -> handleUpdate());
        btnDelete.addActionListener(e -> handleDelete());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);

        mainPanel.add(btnPanel, BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
        connectToRmi();
    }

    private void connectToRmi() {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            authService = (IAuthService) registry.lookup("AuthService");
            lblStatus.setText("\u25CF  Connected to RMI");
            lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
        } catch (Exception e) {
            lblStatus.setText("\u25CF  RMI connection failed");
            lblStatus.setForeground(UIUtils.COLOR_DANGER);
        }
    }

    private void handleAdd() {
        String u = txtUsername.getText();
        String p = new String(txtPassword.getPassword());
        try {
            if (authService.registerUser(u, p)) {
                lblStatus.setText("\u2713  User '" + u + "' created successfully");
                lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
            } else {
                lblStatus.setText("\u2717  User already exists");
                lblStatus.setForeground(UIUtils.COLOR_DANGER);
            }
        } catch (Exception ex) {
            lblStatus.setText("\u2717  RMI exception");
            lblStatus.setForeground(UIUtils.COLOR_DANGER);
        }
    }

    private void handleUpdate() {
        String u = txtUsername.getText();
        String p = new String(txtPassword.getPassword());
        try {
            if (authService.updateUser(u, p)) {
                lblStatus.setText("\u2713  Password updated");
                lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
            } else {
                lblStatus.setText("\u2717  User not found");
                lblStatus.setForeground(UIUtils.COLOR_DANGER);
            }
        } catch (Exception ex) {
            lblStatus.setText("\u2717  RMI exception");
            lblStatus.setForeground(UIUtils.COLOR_DANGER);
        }
    }

    private void handleDelete() {
        String u = txtUsername.getText();
        try {
            if (authService.deleteUser(u)) {
                lblStatus.setText("\u2713  User deleted");
                lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
                txtUsername.setText("");
                txtPassword.setText("");
            } else {
                lblStatus.setText("\u2717  User not found");
                lblStatus.setForeground(UIUtils.COLOR_DANGER);
            }
        } catch (Exception ex) {
            lblStatus.setText("\u2717  RMI exception");
            lblStatus.setForeground(UIUtils.COLOR_DANGER);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminClientGui().setVisible(true));
    }
}
