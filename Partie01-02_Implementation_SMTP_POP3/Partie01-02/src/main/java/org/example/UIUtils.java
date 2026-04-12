package org.example;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class UIUtils {
    // Professional Light Palette
    public static final Color COLOR_BG          = new Color(245, 247, 250);
    public static final Color COLOR_PANEL        = Color.WHITE;
    public static final Color COLOR_PRIMARY      = new Color(37, 99, 235);
    public static final Color COLOR_PRIMARY_HVR  = new Color(29, 78, 216);
    public static final Color COLOR_SUCCESS      = new Color(22, 163, 74);
    public static final Color COLOR_SUCCESS_HVR  = new Color(21, 128, 61);
    public static final Color COLOR_DANGER       = new Color(220, 38, 38);
    public static final Color COLOR_DANGER_HVR   = new Color(185, 28, 28);
    public static final Color COLOR_TEXT         = new Color(30, 41, 59);
    public static final Color COLOR_TEXT_DIM     = new Color(100, 116, 139);
    public static final Color COLOR_BORDER       = new Color(226, 232, 240);
    public static final Color COLOR_LOG_BG       = new Color(248, 250, 252);
    public static final Color COLOR_NEUTRAL_BTN  = new Color(100, 116, 139);
    public static final Color COLOR_NEUTRAL_HVR  = new Color(71, 85, 105);

    public static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_BODY   = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_LOG    = new Font("Cascadia Code", Font.PLAIN, 12);
    public static final Font FONT_STATUS = new Font("Segoe UI", Font.BOLD, 13);

    public static void applyPremiumTheme(JFrame frame) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        frame.getContentPane().setBackground(COLOR_BG);
        UIManager.put("Panel.background", COLOR_BG);
        UIManager.put("TextField.background", Color.WHITE);
        UIManager.put("TextField.foreground", COLOR_TEXT);
        UIManager.put("PasswordField.background", Color.WHITE);
        UIManager.put("PasswordField.foreground", COLOR_TEXT);
        UIManager.put("Label.foreground", COLOR_TEXT);
    }

    public static JButton createStyledButton(String text, Color baseColor) {
        Color hover = deriveHover(baseColor);
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN);
        btn.setBackground(baseColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 22, 10, 22));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { if (btn.isEnabled()) btn.setBackground(hover); }
            public void mouseExited(MouseEvent e)  { btn.setBackground(baseColor); }
        });
        return btn;
    }

    public static JLabel createHeaderLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(COLOR_TEXT);
        return l;
    }

    public static JPanel createCardPanel() {
        JPanel panel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fill(new RoundRectangle2D.Float(2, 2, getWidth() - 2, getHeight() - 2, 14, 14));
                g2.setColor(COLOR_PANEL);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 3, getHeight() - 3, 14, 14));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return panel;
    }

    public static void setStandardLayout(JPanel panel) {
        panel.setLayout(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        panel.setBackground(COLOR_BG);
        panel.setOpaque(true);
    }

    public static JTextArea createLogArea() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setBackground(COLOR_LOG_BG);
        a.setForeground(COLOR_TEXT);
        a.setFont(FONT_LOG);
        a.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    public static JScrollPane createLogScrollPane(JTextArea logArea) {
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(new RoundedBorder(COLOR_BORDER, 10, 1));
        sp.getViewport().setBackground(COLOR_LOG_BG);
        return sp;
    }

    public static JTextField createStyledTextField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(FONT_BODY);
        f.setForeground(COLOR_TEXT);
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(COLOR_BORDER, 8, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return f;
    }

    public static JPasswordField createStyledPasswordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setFont(FONT_BODY);
        f.setForeground(COLOR_TEXT);
        f.setBackground(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(COLOR_BORDER, 8, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return f;
    }

    public static JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(COLOR_TEXT);
        return l;
    }

    private static Color deriveHover(Color c) {
        if (c.equals(COLOR_SUCCESS))     return COLOR_SUCCESS_HVR;
        if (c.equals(COLOR_DANGER))      return COLOR_DANGER_HVR;
        if (c.equals(COLOR_PRIMARY))     return COLOR_PRIMARY_HVR;
        if (c.equals(COLOR_NEUTRAL_BTN)) return COLOR_NEUTRAL_HVR;
        return new Color(Math.max(c.getRed()-25,0), Math.max(c.getGreen()-25,0), Math.max(c.getBlue()-25,0));
    }

    public static class RoundedBorder extends AbstractBorder {
        private final Color color; private final int radius, thickness;
        public RoundedBorder(Color c, int r, int t) { color=c; radius=r; thickness=t; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color); g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w-1, h-1, radius, radius); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(thickness+2, thickness+2, thickness+2, thickness+2); }
    }
}
