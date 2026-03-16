package org.example;

import javax.swing.*;
import java.awt.*;

/**
 * Main Launcher for the Mail System Administration.
 * Starts all three supervision interfaces (SMTP, POP3, IMAP).
 */
public class MailServerLauncher extends JFrame {

    public MailServerLauncher() {
        setTitle("Mail System Administration - TP Distributed Systems");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(3, 1, 10, 10));

        JButton btnSmtp = new JButton("Launch SMTP Supervision");
        JButton btnPop3 = new JButton("Launch POP3 Supervision");
        JButton btnImap = new JButton("Launch IMAP Supervision");

        btnSmtp.addActionListener(e -> new SmtpGui());
        btnPop3.addActionListener(e -> new Pop3Gui());
        btnImap.addActionListener(e -> new ImapGui());

        add(btnSmtp);
        add(btnPop3);
        add(btnImap);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        // Set Look and Feel to System default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(MailServerLauncher::new);
    }
}
