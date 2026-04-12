package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class SmtpClientGui extends JFrame {
    private JTextField txtHost, txtPort, txtFrom, txtTo, txtSubject;
    private JTextArea txtContent, txtLog;

    public SmtpClientGui() {
        setTitle("SMTP Client — Send Email");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);

        // Form Panel
        JPanel formPanel = UIUtils.createCardPanel();
        formPanel.setLayout(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(UIUtils.createLabel("Server Host:"), gbc);
        gbc.gridx = 1;
        txtHost = UIUtils.createStyledTextField(15);
        txtHost.setText("127.0.0.1");
        formPanel.add(txtHost, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(UIUtils.createLabel("Server Port:"), gbc);
        gbc.gridx = 1;
        txtPort = UIUtils.createStyledTextField(15);
        txtPort.setText("2525");
        formPanel.add(txtPort, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(UIUtils.createLabel("From:"), gbc);
        gbc.gridx = 1;
        txtFrom = UIUtils.createStyledTextField(15);
        txtFrom.setText("admin@example.com");
        formPanel.add(txtFrom, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(UIUtils.createLabel("To:"), gbc);
        gbc.gridx = 1;
        txtTo = UIUtils.createStyledTextField(15);
        txtTo.setText("salah@example.com");
        formPanel.add(txtTo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(UIUtils.createLabel("Subject:"), gbc);
        gbc.gridx = 1;
        txtSubject = UIUtils.createStyledTextField(15);
        txtSubject.setText("Test MySQL Integration");
        formPanel.add(txtSubject, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        // Content area
        JPanel contentPanel = UIUtils.createCardPanel();
        contentPanel.setLayout(new BorderLayout());
        txtContent = new JTextArea(8, 20);
        txtContent.setText("Hello,\nThis is a test message to verify MySQL database storage.");
        contentPanel.add(new JScrollPane(txtContent), BorderLayout.CENTER);
        
        JButton btnSend = UIUtils.createStyledButton("Send Email", UIUtils.COLOR_SUCCESS);
        btnSend.addActionListener(e -> sendEmail());
        contentPanel.add(btnSend, BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Logs
        txtLog = UIUtils.createLogArea();
        mainPanel.add(UIUtils.createLogScrollPane(txtLog), BorderLayout.SOUTH);

        add(mainPanel);
        setLocationRelativeTo(null);
    }

    private void sendEmail() {
        new Thread(() -> {
            try (Socket socket = new Socket(txtHost.getText(), Integer.parseInt(txtPort.getText()));
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                log("Connected to server: " + in.readLine());
                
                send(out, "HELO localhost");
                log(in.readLine());

                send(out, "MAIL FROM:<" + txtFrom.getText() + ">");
                log(in.readLine());

                send(out, "RCPT TO:<" + txtTo.getText() + ">");
                log(in.readLine());

                send(out, "DATA");
                log(in.readLine());

                out.println("Subject: " + txtSubject.getText());
                out.println();
                out.println(txtContent.getText());
                out.println(".");
                log(in.readLine());

                send(out, "QUIT");
                log(in.readLine());

            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
            }
        }).start();
    }

    private void send(PrintWriter out, String msg) {
        log("CLIENT -> " + msg);
        out.println(msg);
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SmtpClientGui().setVisible(true));
    }
}
