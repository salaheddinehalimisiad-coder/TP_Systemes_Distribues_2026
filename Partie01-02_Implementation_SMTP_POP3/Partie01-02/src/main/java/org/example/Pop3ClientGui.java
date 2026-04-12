package org.example;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Pop3ClientGui extends JFrame {
    private JTextField txtHost, txtPort, txtUser;
    private JPasswordField txtPass;
    private JTextArea txtLog, txtMessage;
    private JButton btnLogin, btnList, btnFetch;
    private JList<String> messageList;
    private DefaultListModel<String> listModel;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public Pop3ClientGui() {
        setTitle("POP3 Client — Retrieve Emails");
        setSize(850, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        UIUtils.applyPremiumTheme(this);

        JPanel mainPanel = new JPanel();
        UIUtils.setStandardLayout(mainPanel);

        // Sidebar: Connection & Message List
        JPanel sidebar = new JPanel(new BorderLayout(10, 10));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(300, 0));

        // Connection Card
        JPanel connPanel = UIUtils.createCardPanel();
        connPanel.setLayout(new GridBagLayout());
        connPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        gbc.gridx = 0; gbc.gridy = 0;
        connPanel.add(UIUtils.createLabel("Host:"), gbc);
        gbc.gridx = 1;
        txtHost = UIUtils.createStyledTextField(10);
        txtHost.setText("127.0.0.1");
        connPanel.add(txtHost, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        connPanel.add(UIUtils.createLabel("Port:"), gbc);
        gbc.gridx = 1;
        txtPort = UIUtils.createStyledTextField(10);
        txtPort.setText("110");
        connPanel.add(txtPort, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        connPanel.add(UIUtils.createLabel("User:"), gbc);
        gbc.gridx = 1;
        txtUser = UIUtils.createStyledTextField(10);
        txtUser.setText("salah");
        connPanel.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        connPanel.add(UIUtils.createLabel("Pass:"), gbc);
        gbc.gridx = 1;
        txtPass = UIUtils.createStyledPasswordField(10);
        txtPass.setText("123");
        connPanel.add(txtPass, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        btnLogin = UIUtils.createStyledButton("Connect & Login", UIUtils.COLOR_PRIMARY);
        connPanel.add(btnLogin, gbc);

        sidebar.add(connPanel, BorderLayout.NORTH);

        // Message List Card
        JPanel listPanel = UIUtils.createCardPanel();
        listPanel.setLayout(new BorderLayout(5, 5));
        listPanel.add(UIUtils.createLabel("Inbox Messages:"), BorderLayout.NORTH);
        
        listModel = new DefaultListModel<>();
        messageList = new JList<>(listModel);
        messageList.setFont(UIUtils.FONT_BODY);
        messageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listPanel.add(new JScrollPane(messageList), BorderLayout.CENTER);

        JPanel listActions = new JPanel(new GridLayout(1, 2, 5, 0));
        listActions.setOpaque(false);
        btnList = UIUtils.createStyledButton("List", UIUtils.COLOR_NEUTRAL_BTN);
        btnFetch = UIUtils.createStyledButton("Fetch", UIUtils.COLOR_SUCCESS);
        btnList.setEnabled(false);
        btnFetch.setEnabled(false);
        listActions.add(btnList);
        listActions.add(btnFetch);
        listPanel.add(listActions, BorderLayout.SOUTH);

        sidebar.add(listPanel, BorderLayout.CENTER);

        // Main Content Area
        JPanel contentArea = new JPanel(new BorderLayout(12, 12));
        contentArea.setOpaque(false);

        // Message Content Card
        JPanel msgPanel = UIUtils.createCardPanel();
        msgPanel.setLayout(new BorderLayout(8, 8));
        msgPanel.add(UIUtils.createLabel("Message Content:"), BorderLayout.NORTH);
        txtMessage = new JTextArea();
        txtMessage.setEditable(false);
        txtMessage.setFont(UIUtils.FONT_BODY);
        msgPanel.add(new JScrollPane(txtMessage), BorderLayout.CENTER);
        
        contentArea.add(msgPanel, BorderLayout.CENTER);

        // Log Card
        JPanel logPanel = UIUtils.createCardPanel();
        logPanel.setLayout(new BorderLayout());
        logPanel.setPreferredSize(new Dimension(0, 180));
        txtLog = UIUtils.createLogArea();
        logPanel.add(UIUtils.createLogScrollPane(txtLog), BorderLayout.CENTER);
        
        contentArea.add(logPanel, BorderLayout.SOUTH);

        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(contentArea, BorderLayout.CENTER);

        add(mainPanel);

        // Listeners
        btnLogin.addActionListener(e -> login());
        btnList.addActionListener(e -> listMessages());
        btnFetch.addActionListener(e -> fetchSelectedMessage());

        setLocationRelativeTo(null);
    }

    private void login() {
        new Thread(() -> {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                
                socket = new Socket(txtHost.getText(), Integer.parseInt(txtPort.getText()));
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                log("CONNECTED: " + in.readLine());

                send("USER " + txtUser.getText());
                String res = in.readLine();
                log(res);
                if (!res.startsWith("+OK")) return;

                send("PASS " + new String(txtPass.getPassword()));
                res = in.readLine();
                log(res);
                if (!res.startsWith("+OK")) {
                    socket.close();
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    btnLogin.setText("Logged In");
                    btnLogin.setEnabled(false);
                    btnList.setEnabled(true);
                    btnFetch.setEnabled(true);
                });

            } catch (Exception ex) {
                log("ERROR: " + ex.getMessage());
            }
        }).start();
    }

    private void listMessages() {
        new Thread(() -> {
            try {
                send("LIST");
                String line = in.readLine();
                log(line);
                if (!line.startsWith("+OK")) return;

                SwingUtilities.invokeLater(() -> listModel.clear());
                while (!(line = in.readLine()).equals(".")) {
                    log(line);
                    String finalLine = line;
                    SwingUtilities.invokeLater(() -> listModel.addElement("Message " + finalLine));
                }
            } catch (Exception ex) {
                log("ERROR: " + ex.getMessage());
            }
        }).start();
    }

    private void fetchSelectedMessage() {
        String selected = messageList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a message from the list first.");
            return;
        }

        String msgId = selected.split(" ")[1];
        new Thread(() -> {
            try {
                send("RETR " + msgId);
                String line = in.readLine();
                log(line);
                if (!line.startsWith("+OK")) return;

                StringBuilder content = new StringBuilder();
                while (!(line = in.readLine()).equals(".")) {
                    content.append(line).append("\n");
                }
                
                SwingUtilities.invokeLater(() -> txtMessage.setText(content.toString()));
            } catch (Exception ex) {
                log("ERROR: " + ex.getMessage());
            }
        }).start();
    }

    private void send(String msg) {
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
        SwingUtilities.invokeLater(() -> new Pop3ClientGui().setVisible(true));
    }
}
