package org.example;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImapGui extends JFrame {
    private ImapServer server;
    private JTextArea logArea;
    private JLabel statusLabel;
    private JLabel clientCountLabel;
    private JButton startButton;
    private JButton stopButton;
    private final int port = 143;

    public ImapGui() {
        setTitle("IMAP Server Supervision");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);
        
        // Header Panel
        JPanel headerPanel = UIUtils.createCardPanel();
        headerPanel.setLayout(new BorderLayout(10, 10));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = UIUtils.createHeaderLabel("IMAP ENGINE");
        headerPanel.add(title, BorderLayout.NORTH);

        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statPanel.setOpaque(false);
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(UIUtils.COLOR_DANGER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        clientCountLabel = new JLabel("Clients: 0");
        clientCountLabel.setForeground(UIUtils.COLOR_TEXT_DIM);
        clientCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        statPanel.add(statusLabel);
        statPanel.add(new JLabel("  |  ")).setForeground(UIUtils.COLOR_TEXT_DIM);
        statPanel.add(clientCountLabel);
        headerPanel.add(statPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        startButton = UIUtils.createStyledButton("START", UIUtils.COLOR_SUCCESS);
        stopButton = UIUtils.createStyledButton("STOP", UIUtils.COLOR_DANGER);
        stopButton.setEnabled(false);
        JButton clearButton = UIUtils.createStyledButton("CLEAR LOGS", Color.GRAY);

        buttonRow.add(startButton);
        buttonRow.add(stopButton);
        buttonRow.add(clearButton);
        headerPanel.add(buttonRow, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Center Panel: Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(25, 28, 31));
        logArea.setForeground(Color.ORANGE);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 70)));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearButton.addActionListener(e -> logArea.setText(""));

        // Timer to update client count
        Timer timer = new Timer(1000, e -> {
            if (server != null) {
                clientCountLabel.setText("Clients: " + server.getConnectedCount());
            }
        });
        timer.start();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startServer() {
        server = new ImapServer(port, this::addLog);
        server.start();
        statusLabel.setText("Status: RUNNING ON " + port);
        statusLabel.setForeground(UIUtils.COLOR_SUCCESS);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        addLog("SYSTEM: IMAP Engine engaged. Port 143 listening...");
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            statusLabel.setText("Status: STOPPED");
            statusLabel.setForeground(UIUtils.COLOR_DANGER);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            addLog("SYSTEM: IMAP Engine terminated by administrator.");
        }
    }

    private void addLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImapGui::new);
    }
}
