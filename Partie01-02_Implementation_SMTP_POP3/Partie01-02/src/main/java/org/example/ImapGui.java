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
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel(new GridLayout(2, 1));
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Status: Stopped");
        statusLabel.setForeground(Color.RED);
        clientCountLabel = new JLabel("Connected Clients: 0");
        infoPanel.add(statusLabel);
        infoPanel.add(new JLabel(" | "));
        infoPanel.add(clientCountLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        startButton = new JButton("Start Server");
        stopButton = new JButton("Stop Server");
        stopButton.setEnabled(false);
        JButton clearButton = new JButton("Clear Logs");

        buttonPanel.add(startButton);
        buttonPanel.add(stopButton);
        buttonPanel.add(clearButton);

        topPanel.add(infoPanel);
        topPanel.add(buttonPanel);
        add(topPanel, BorderLayout.NORTH);

        // Center Panel: Logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(40, 20, 20)); // Dark red/brown background
        logArea.setForeground(Color.ORANGE);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        // Listeners
        startButton.addActionListener(e -> startServer());
        stopButton.addActionListener(e -> stopServer());
        clearButton.addActionListener(e -> logArea.setText(""));

        // Timer to update client count
        Timer timer = new Timer(1000, e -> {
            if (server != null) {
                clientCountLabel.setText("Connected Clients: " + server.getConnectedCount());
            }
        });
        timer.start();

        setVisible(true);
    }

    private void startServer() {
        server = new ImapServer(port, this::addLog);
        server.start();
        statusLabel.setText("Status: Running on Port " + port);
        statusLabel.setForeground(new Color(0, 150, 0));
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        addLog("SYSTEM: IMAP Server initialization...");
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            statusLabel.setText("Status: Stopped");
            statusLabel.setForeground(Color.RED);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            addLog("SYSTEM: IMAP Server stopped by administrator.");
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
