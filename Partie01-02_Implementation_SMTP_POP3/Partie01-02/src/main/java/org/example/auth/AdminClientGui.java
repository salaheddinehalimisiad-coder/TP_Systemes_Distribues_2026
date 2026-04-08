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
        setTitle("RMI USER ADMINISTRATION");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);

        // Header
        JPanel headerPanel = UIUtils.createCardPanel();
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        headerPanel.add(UIUtils.createHeaderLabel("USER MANAGEMENT"), BorderLayout.CENTER);
        
        lblStatus = new JLabel("Status: Disconnected");
        lblStatus.setForeground(UIUtils.COLOR_TEXT_DIM);
        headerPanel.add(lblStatus, BorderLayout.SOUTH);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form
        JPanel formPanel = UIUtils.createCardPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblUser = new JLabel("Login:");
        lblUser.setForeground(UIUtils.COLOR_TEXT);
        formPanel.add(lblUser, gbc);

        gbc.gridx = 1;
        txtUsername = new JTextField(15);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lblPass = new JLabel("Password:");
        lblPass.setForeground(UIUtils.COLOR_TEXT);
        formPanel.add(lblPass, gbc);

        gbc.gridx = 1;
        txtPassword = new JPasswordField(15);
        formPanel.add(txtPassword, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setOpaque(false);
        JButton btnAdd = UIUtils.createStyledButton("REGISTER", UIUtils.COLOR_SUCCESS);
        JButton btnUpdate = UIUtils.createStyledButton("UPDATE", UIUtils.COLOR_PRIMARY);
        JButton btnDelete = UIUtils.createStyledButton("DELETE", UIUtils.COLOR_DANGER);

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
            lblStatus.setText("Status: CONNECTED TO RMI");
            lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
        } catch (Exception e) {
            lblStatus.setText("Status: RMI CONNECTION FAILED");
            lblStatus.setForeground(UIUtils.COLOR_DANGER);
        }
    }

    private void handleAdd() {
        String u = txtUsername.getText();
        String p = new String(txtPassword.getPassword());
        try {
            if (authService.registerUser(u, p)) {
                lblStatus.setText("SUCCESS: User '" + u + "' created.");
                lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
            } else {
                lblStatus.setText("ERROR: User already exists.");
                lblStatus.setForeground(UIUtils.COLOR_DANGER);
            }
        } catch (Exception ex) {
            lblStatus.setText("RMI EXCEPTION");
        }
    }

    private void handleUpdate() {
        String u = txtUsername.getText();
        String p = new String(txtPassword.getPassword());
        try {
            if (authService.updateUser(u, p)) {
                lblStatus.setText("SUCCESS: Password updated.");
                lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
            } else {
                lblStatus.setText("ERROR: User not found.");
                lblStatus.setForeground(UIUtils.COLOR_DANGER);
            }
        } catch (Exception ex) {
            lblStatus.setText("RMI EXCEPTION");
        }
    }

    private void handleDelete() {
        String u = txtUsername.getText();
        try {
            if (authService.deleteUser(u)) {
                lblStatus.setText("SUCCESS: User deleted.");
                lblStatus.setForeground(UIUtils.COLOR_SUCCESS);
                txtUsername.setText("");
                txtPassword.setText("");
            } else {
                lblStatus.setText("ERROR: User not found.");
                lblStatus.setForeground(UIUtils.COLOR_DANGER);
            }
        } catch (Exception ex) {
            lblStatus.setText("RMI EXCEPTION");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminClientGui().setVisible(true));
    }
}
