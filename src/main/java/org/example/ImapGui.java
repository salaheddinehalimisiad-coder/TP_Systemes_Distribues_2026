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
        setTitle("IMAP Server — Supervision");
        setSize(750, 520);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);

        JPanel headerPanel = UIUtils.createCardPanel();
        headerPanel.setLayout(new BorderLayout(10, 10));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = UIUtils.createHeaderLabel("IMAP Server");
        headerPanel.add(title, BorderLayout.NORTH);

        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statPanel.setOpaque(false);
        statusLabel = new JLabel("\u25CF  Stopped");
        statusLabel.setForeground(UIUtils.COLOR_DANGER);
        statusLabel.setFont(UIUtils.FONT_STATUS);

        clientCountLabel = new JLabel("Clients: 0");
        clientCountLabel.setForeground(UIUtils.COLOR_TEXT_DIM);
        clientCountLabel.setFont(UIUtils.FONT_BODY);

        statPanel.add(statusLabel);
        JLabel sep = new JLabel("  \u2502  ");
        sep.setForeground(UIUtils.COLOR_BORDER);
        statPanel.add(sep);
        statPanel.add(clientCountLabel);
        headerPanel.add(statPanel, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        startButton = UIUtils.createStyledButton("Start", UIUtils.COLOR_SUCCESS);
        stopButton = UIUtils.createStyledButton("Stop", UIUtils.COLOR_DANGER);
        stopButton.setEnabled(false);
        JButton clearButton = UIUtils.createStyledButton("Clear Logs", UIUtils.COLOR_NEUTRAL_BTN);

        buttonRow.add(startButton);
        buttonRow.add(stopButton);
        buttonRow.add(clearButton);
        headerPanel.add(buttonRow, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        logArea = UIUtils.createLogArea();
        JScrollPane scrollPane = UIUtils.createLogScrollPane(logArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearButton.addActionListener(e -> logArea.setText(""));

        Timer timer = new Timer(1000, e -> {
            if (server != null) clientCountLabel.setText("Clients: " + server.getConnectedCount());
        });
        timer.start();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void startServer() {
        server = new ImapServer(port, this::addLog);
        server.start();
        statusLabel.setText("\u25CF  Running on port " + port);
        statusLabel.setForeground(UIUtils.COLOR_SUCCESS);
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        addLog("IMAP server started — listening on port " + port);
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            statusLabel.setText("\u25CF  Stopped");
            statusLabel.setForeground(UIUtils.COLOR_DANGER);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            addLog("IMAP server stopped by administrator.");
        }
    }

    private void addLog(String message) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + ts + "]  " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImapGui::new);
    }
}
