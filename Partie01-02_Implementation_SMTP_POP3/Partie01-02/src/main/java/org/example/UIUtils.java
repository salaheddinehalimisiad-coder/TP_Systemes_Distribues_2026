package org.example;

import javax.swing.*;
import java.awt.*;

public class UIUtils {
    public static final Color COLOR_BG = new Color(33, 37, 41);
    public static final Color COLOR_PANEL = new Color(45, 50, 56);
    public static final Color COLOR_PRIMARY = new Color(13, 110, 253);
    public static final Color COLOR_SUCCESS = new Color(25, 135, 84);
    public static final Color COLOR_DANGER = new Color(220, 53, 69);
    public static final Color COLOR_TEXT = new Color(248, 249, 250);
    public static final Color COLOR_TEXT_DIM = new Color(173, 181, 189);

    public static void applyPremiumTheme(JFrame frame) {
        frame.getContentPane().setBackground(COLOR_BG);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    public static JButton createStyledButton(String text, Color baseColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(baseColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect could be added with MouseListener if needed, but keeping it simple for now
        return btn;
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));
        label.setForeground(COLOR_TEXT);
        return label;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(BorderFactory.createLineBorder(new Color(60, 65, 70), 1));
        return panel;
    }

    public static void setStandardLayout(JPanel panel) {
        panel.setLayout(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(COLOR_BG);
    }
}
