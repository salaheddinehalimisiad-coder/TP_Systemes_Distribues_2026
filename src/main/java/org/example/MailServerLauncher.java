package org.example;

import javax.swing.*;
import java.awt.*;

public class MailServerLauncher extends JFrame {

    public MailServerLauncher() {
        setTitle("Mail System — Control Center");
        setSize(480, 520);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        JLabel title = UIUtils.createHeaderLabel("Mail System Control Center");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);

        JLabel subtitle = new JLabel("Launch and supervise your mail systems");
        subtitle.setFont(UIUtils.FONT_BODY);
        subtitle.setForeground(UIUtils.COLOR_TEXT_DIM);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(subtitle);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 24)));

        // Server Section
        mainPanel.add(UIUtils.createLabel("--- Servers ---"));
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnSmtp = UIUtils.createStyledButton("Launch SMTP Server", UIUtils.COLOR_PRIMARY);
        JButton btnPop3 = UIUtils.createStyledButton("Launch POP3 Server", UIUtils.COLOR_PRIMARY);
        JButton btnImap = UIUtils.createStyledButton("Launch IMAP Server", UIUtils.COLOR_PRIMARY);
        
        btnSmtp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnPop3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnImap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        btnSmtp.addActionListener(e -> new SmtpGui());
        btnPop3.addActionListener(e -> new Pop3Gui());
        btnImap.addActionListener(e -> new ImapGui());

        mainPanel.add(btnSmtp);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(btnPop3);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(btnImap);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Client Section
        mainPanel.add(UIUtils.createLabel("--- Clients ---"));
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton btnSmtpCli = UIUtils.createStyledButton("Launch SMTP Client", UIUtils.COLOR_SUCCESS);
        JButton btnPop3Cli = UIUtils.createStyledButton("Launch POP3 Client", UIUtils.COLOR_SUCCESS);

        btnSmtpCli.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnPop3Cli.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        btnSmtpCli.addActionListener(e -> new SmtpClientGui().setVisible(true));
        btnPop3Cli.addActionListener(e -> new Pop3ClientGui().setVisible(true));

        mainPanel.add(btnSmtpCli);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        mainPanel.add(btnPop3Cli);

        add(mainPanel);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MailServerLauncher::new);
        try {
            // Maintenir le processus en vie pour mvn exec:java
            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
