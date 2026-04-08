package org.example.auth;

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
        setTitle("RMI Admin Client - User Management");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        connectToRmi();

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        formPanel.add(new JLabel("Utilisateur :"));
        txtUsername = new JTextField();
        formPanel.add(txtUsername);

        formPanel.add(new JLabel("Mot de passe :"));
        txtPassword = new JPasswordField();
        formPanel.add(txtPassword);

        add(formPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnAdd = new JButton("Ajouter");
        JButton btnUpdate = new JButton("Modifier");
        JButton btnDelete = new JButton("Supprimer");

        btnAdd.addActionListener(e -> handleAdd());
        btnUpdate.addActionListener(e -> handleUpdate());
        btnDelete.addActionListener(e -> handleDelete());

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);

        add(btnPanel, BorderLayout.SOUTH);

        lblStatus = new JLabel("Status: Prêt", SwingConstants.CENTER);
        lblStatus.setForeground(Color.BLUE);
        add(lblStatus, BorderLayout.NORTH);
    }

    private void connectToRmi() {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1099);
            authService = (IAuthService) registry.lookup("AuthService");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossible de se connecter au serveur RMI. Assurez-vous qu'il est démarré.", "Erreur RMI", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void handleAdd() {
        String u = txtUsername.getText();
        String p = new String(txtPassword.getPassword());
        try {
            if (authService.registerUser(u, p)) {
                lblStatus.setText("Succès : Utilisateur ajouté.");
                lblStatus.setForeground(new Color(0, 128, 0));
            } else {
                lblStatus.setText("Erreur : Utilisateur existe déjà.");
                lblStatus.setForeground(Color.RED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            lblStatus.setText("Erreur RMI.");
        }
    }

    private void handleUpdate() {
        String u = txtUsername.getText();
        String p = new String(txtPassword.getPassword());
        try {
            if (authService.updateUser(u, p)) {
                lblStatus.setText("Succès : Mot de passe mis à jour.");
                lblStatus.setForeground(new Color(0, 128, 0));
            } else {
                lblStatus.setText("Erreur : Utilisateur introuvable.");
                lblStatus.setForeground(Color.RED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            lblStatus.setText("Erreur RMI.");
        }
    }

    private void handleDelete() {
        String u = txtUsername.getText();
        try {
            if (authService.deleteUser(u)) {
                lblStatus.setText("Succès : Utilisateur supprimé.");
                lblStatus.setForeground(new Color(0, 128, 0));
                txtUsername.setText("");
                txtPassword.setText("");
            } else {
                lblStatus.setText("Erreur : Utilisateur introuvable.");
                lblStatus.setForeground(Color.RED);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            lblStatus.setText("Erreur RMI.");
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AdminClientGui().setVisible(true));
    }
}
