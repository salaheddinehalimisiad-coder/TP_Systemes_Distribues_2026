package org.example;

import javax.swing.*;
import java.awt.*;

public class MailServerLauncher extends JFrame {

    public MailServerLauncher() {
        setTitle("Distributed Mail System - Control Center");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        UIUtils.applyPremiumTheme(this);
        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JLabel title = UIUtils.createHeaderLabel("MAIL SYSTEM CONTROL CENTER");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        JButton btnSmtp = UIUtils.createStyledButton("LAUNCH SMTP SUPERVISION", UIUtils.COLOR_PRIMARY);
        JButton btnPop3 = UIUtils.createStyledButton("LAUNCH POP3 SUPERVISION", UIUtils.COLOR_PRIMARY);
        JButton btnImap = UIUtils.createStyledButton("LAUNCH IMAP SUPERVISION", UIUtils.COLOR_PRIMARY);

        btnSmtp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnPop3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnImap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        btnSmtp.addActionListener(e -> new SmtpGui());
        btnPop3.addActionListener(e -> new Pop3Gui());
        btnImap.addActionListener(e -> new ImapGui());

        mainPanel.add(btnSmtp);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(btnPop3);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        mainPanel.add(btnImap);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MailServerLauncher::new);
    }
}
