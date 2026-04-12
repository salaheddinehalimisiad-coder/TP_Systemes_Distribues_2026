package org.example;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SubjectTerm;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Properties;

/**
 * Partie 7 : Client de messagerie standard utilant JavaMail (Jakarta Mail)
 * Supporte SMTP, POP3 et IMAP avec une interface graphique unifiée.
 */
public class StandardMailClientGui extends JFrame {
    private JTextField hostField, portField, userField;
    private JPasswordField passField;
    private JComboBox<String> protocolCombo;
    
    // SMTP Components
    private JTextField toField, subjectField;
    private JTextArea contentArea;
    
    // POP3/IMAP Components
    private JTable mailTable;
    private DefaultTableModel tableModel;
    private JTextArea messageViewer;
    private JTextField searchField;

    public StandardMailClientGui() {
        setTitle("EMP Standard Mail Client (Partie 7 - JavaMail)");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // --- Config Panel (North) ---
        JPanel configPanel = new JPanel(new GridLayout(2, 5, 10, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("Paramètres de Connexion"));
        
        protocolCombo = new JComboBox<>(new String[]{"SMTP", "POP3", "IMAP"});
        hostField = new JTextField("localhost");
        portField = new JTextField("2525");
        userField = new JTextField("salah");
        passField = new JPasswordField("salah123");

        configPanel.add(new JLabel("Protocole:"));
        configPanel.add(new JLabel("Hôte:"));
        configPanel.add(new JLabel("Port:"));
        configPanel.add(new JLabel("Utilisateur:"));
        configPanel.add(new JLabel("Mot de passe:"));
        
        configPanel.add(protocolCombo);
        configPanel.add(hostField);
        configPanel.add(portField);
        configPanel.add(userField);
        configPanel.add(passField);

        add(configPanel, BorderLayout.NORTH);

        // --- Tabs (Center) ---
        JTabbedPane tabs = new JTabbedPane();
        
        // Tab 1: SMTP Send
        tabs.addTab("📤 Envoyer (SMTP)", createSmtpPanel());
        
        // Tab 2: Inbox (POP3/IMAP)
        tabs.addTab("📥 Boîte de Réception (POP3/IMAP)", createInboxPanel());

        add(tabs, BorderLayout.CENTER);

        // Update port automatically when protocol changes
        protocolCombo.addActionListener(e -> {
            String proto = (String) protocolCombo.getSelectedItem();
            if ("SMTP".equals(proto)) portField.setText("2525");
            else if ("POP3".equals(proto)) portField.setText("110");
            else if ("IMAP".equals(proto)) portField.setText("143");
        });
    }

    private JPanel createSmtpPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel fields = new JPanel(new GridLayout(2, 2, 10, 10));
        toField = new JTextField();
        subjectField = new JTextField();
        fields.add(new JLabel("À (Destinataire):"));
        fields.add(toField);
        fields.add(new JLabel("Sujet:"));
        fields.add(subjectField);

        contentArea = new JTextArea();
        JButton sendBtn = new JButton("🚀 Envoyer le message");
        sendBtn.setBackground(new Color(26, 115, 232));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        sendBtn.addActionListener(e -> sendSmtp());

        p.add(fields, BorderLayout.NORTH);
        p.add(new JScrollPane(contentArea), BorderLayout.CENTER);
        p.add(sendBtn, BorderLayout.SOUTH);
        
        return p;
    }

    private JPanel createInboxPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton syncBtn = new JButton("🔄 Synchroniser");
        searchField = new JTextField(15);
        JButton searchBtn = new JButton("🔍 Rechercher (IMAP)");
        JButton deleteBtn = new JButton("🗑️ Supprimer (POP3)");
        
        toolbar.add(syncBtn);
        toolbar.add(new JLabel("   "));
        toolbar.add(searchField);
        toolbar.add(searchBtn);
        toolbar.add(deleteBtn);

        // Table
        String[] cols = {"ID / Index", "Expéditeur", "Sujet", "Date"};
        tableModel = new DefaultTableModel(cols, 0);
        mailTable = new JTable(tableModel);
        
        messageViewer = new JTextArea();
        messageViewer.setEditable(false);
        messageViewer.setBackground(new Color(248, 249, 250));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(mailTable), new JScrollPane(messageViewer));
        split.setDividerLocation(200);

        p.add(toolbar, BorderLayout.NORTH);
        p.add(split, BorderLayout.CENTER);

        syncBtn.addActionListener(e -> fetchMails(null));
        searchBtn.addActionListener(e -> fetchMails(searchField.getText()));
        deleteBtn.addActionListener(e -> deleteSelected());

        return p;
    }

    // --- Core Logic using JavaMail ---

    private void sendSmtp() {
        Properties props = new Properties();
        props.put("mail.smtp.host", hostField.getText());
        props.put("mail.smtp.port", portField.getText());
        props.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(userField.getText(), new String(passField.getPassword()));
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(userField.getText() + "@emp.dz"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toField.getText()));
            message.setSubject(subjectField.getText());
            message.setText(contentArea.getText());

            Transport.send(message);
            JOptionPane.showMessageDialog(this, "✅ Message envoyé avec succès via SMTP !");
        } catch (Exception ex) {
            showError("Erreur d'envoi SMTP", ex);
        }
    }

    private void fetchMails(String searchSubject) {
        String protocol = ((String) protocolCombo.getSelectedItem()).toLowerCase();
        if ("smtp".equals(protocol)) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner POP3 ou IMAP pour lire les emails.");
            return;
        }

        Properties props = new Properties();
        props.put("mail." + protocol + ".host", hostField.getText());
        props.put("mail." + protocol + ".port", portField.getText());

        try {
            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore(protocol);
            store.connect(userField.getText(), new String(passField.getPassword()));

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages;
            if (searchSubject != null && !searchSubject.isEmpty() && "imap".equals(protocol)) {
                messages = inbox.search(new SubjectTerm(searchSubject));
            } else {
                messages = inbox.getMessages();
            }

            tableModel.setRowCount(0);
            for (Message msg : messages) {
                tableModel.addRow(new Object[]{
                        msg.getMessageNumber(),
                        InternetAddress.toString(msg.getFrom()),
                        msg.getSubject(),
                        msg.getSentDate()
                });
            }

            // Selection listener
            mailTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int row = mailTable.getSelectedRow();
                    if (row != -1) {
                        try {
                            messageViewer.setText(messages[row].getContent().toString());
                            // Imap: Mark as read
                            if ("imap".equals(protocol)) {
                                messages[row].setFlag(Flags.Flag.SEEN, true);
                            }
                        } catch (Exception ex) { messageViewer.setText("Erreur de lecture du contenu."); }
                    }
                }
            });

        } catch (Exception ex) {
            showError("Erreur de récupération " + protocol.toUpperCase(), ex);
        }
    }

    private void deleteSelected() {
        int row = mailTable.getSelectedRow();
        if (row == -1) return;
        
        try {
            String protocol = ((String) protocolCombo.getSelectedItem()).toLowerCase();
            Properties props = new Properties();
            props.put("mail." + protocol + ".host", hostField.getText());
            props.put("mail." + protocol + ".port", portField.getText());

            Session session = Session.getDefaultInstance(props);
            Store store = session.getStore(protocol);
            store.connect(userField.getText(), new String(passField.getPassword()));

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            
            Message msg = inbox.getMessage(row + 1);
            msg.setFlag(Flags.Flag.DELETED, true);
            inbox.close(true); // expunge
            store.close();
            
            JOptionPane.showMessageDialog(this, "🗑️ Message marqué pour suppression !");
            fetchMails(null);
        } catch (Exception ex) {
            showError("Erreur de suppression", ex);
        }
    }

    private void showError(String title, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, 
                "❌ " + title + ":\n" + ex.getMessage(), 
                "Erreur Système", 
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        
        SwingUtilities.invokeLater(() -> new StandardMailClientGui().setVisible(true));
    }
}
