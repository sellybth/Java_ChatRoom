import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import org.json.JSONObject;

public class LoginFrame extends JFrame {

    private JTextField     emailField;
    private JPasswordField passwordField;
    private JButton        loginBtn;
    private Timer          glowTimer;
    private float          glowPhase = 0f;

    public LoginFrame() {
        setTitle("ChatRoom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        setBackground(Theme.BG_DARKEST);

        JPanel root = new BackgroundPanel();
        root.setLayout(new GridBagLayout());
        root.setBackground(Theme.BG_DARKEST);

        JPanel card = buildCard();
        root.add(card);

        setContentPane(root);

        // Glow animation
        glowTimer = new Timer(30, e -> {
            glowPhase += 0.04f;
            root.repaint();
        });
        glowTimer.start();
    }

    // ── Animated background panel ──────────────────────────────────────────
    private class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Theme.BG_DARKEST);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Soft radial glow behind card
            float alpha = 0.18f + 0.06f * (float) Math.sin(glowPhase);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            RadialGradientPaint glow = new RadialGradientPaint(
                    new Point2D.Float(getWidth() / 2f, getHeight() / 2f),
                    400,
                    new float[]{0f, 1f},
                    new Color[]{Theme.ACCENT, new Color(0, 0, 0, 0)}
            );
            g2.setPaint(glow);
            g2.fillOval(getWidth() / 2 - 400, getHeight() / 2 - 400, 800, 800);

            // Dot grid
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
            g2.setColor(Theme.ACCENT_LIGHT);
            int spacing = 28;
            for (int x = 0; x < getWidth(); x += spacing) {
                for (int y = 0; y < getHeight(); y += spacing) {
                    g2.fillOval(x, y, 2, 2);
                }
            }

            g2.dispose();
        }
    }

    // ── Card ────────────────────────────────────────────────────────────────
    private JPanel buildCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_SIDEBAR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);

                // top accent bar
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), 4, 4, 4);

                // border
                g2.setColor(Theme.BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);

                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout());
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(440, 540));

        JPanel formPanel = buildFormPanel();

        // Wrap with close button
        JLayeredPane layer = new JLayeredPane();
        layer.setPreferredSize(new Dimension(440, 520));
        formPanel.setBounds(0, 0, 440, 520);
        layer.add(formPanel, JLayeredPane.DEFAULT_LAYER);

        // Close button
        JButton closeBtn = new JButton("X");
        closeBtn.setForeground(Theme.TEXT_MUTED);
        closeBtn.setBackground(new Color(0, 0, 0, 0));
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setFont(Theme.font(Font.PLAIN, 14));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBounds(408, 10, 24, 24);
        closeBtn.addActionListener(e -> System.exit(0));
        closeBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeBtn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e)  { closeBtn.setForeground(Theme.TEXT_MUTED); }
        });
        layer.add(closeBtn, JLayeredPane.PALETTE_LAYER);

        card.add(layer, BorderLayout.CENTER);
        return card;
    }

    // ── Form panel ──────────────────────────────────────────────────────────
    private JPanel buildFormPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(56, 44, 50, 44));

        JLabel welcome = new JLabel("Welcome back");
        welcome.setFont(Theme.font(Font.BOLD, 24));
        welcome.setForeground(Theme.TEXT_PRIMARY);
        welcome.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Sign in to continue");
        sub.setFont(Theme.font(Font.PLAIN, 13));
        sub.setForeground(Theme.TEXT_SECONDARY);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        emailField = buildTextField("Email ID");
        passwordField = buildPasswordField("Password");

        loginBtn = buildLoginButton();
        loginBtn.addActionListener(e -> {
            String email    = emailField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
        
            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please enter email and password",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        
            // Disable button while calling API
            loginBtn.setEnabled(false);
            loginBtn.setText("Signing in...");
        
            // Run in background so UI doesn't freeze
            new Thread(() -> {
                try {
                    JSONObject response = ApiService.login(email, password);
        
                    // Store token + user info
                    SessionManager.getInstance().saveSession(
                        response.getString("token"),
                        response.getLong("userId"),
                        response.getString("name")
                    );
        
                    // Open chat window on UI thread
                    SwingUtilities.invokeLater(() -> {
                        openChatRoom();  
                        dispose();

                    });
        
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null,
                            ex.getMessage(),
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Sign In");
                    });
                }
            }).start();
        });

        JLabel signUpLabel = new JLabel("<html><center><font color='#555676'>Don't have an account? </font>"
        + "<font color='#9D98FF'><u>Sign up</u></font></center></html>");
        signUpLabel.setFont(Theme.font(Font.PLAIN, 12));
        signUpLabel.setAlignmentX(LEFT_ALIGNMENT);
        signUpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        signUpLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SignUpFrame signUp = new SignUpFrame(LoginFrame.this);
                signUp.setVisible(true);
            }
        });

        p.add(welcome);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(36));
        p.add(fieldLabel("Email ID"));
        p.add(Box.createVerticalStrut(6));
        p.add(emailField);
        p.add(Box.createVerticalStrut(18));
        p.add(fieldLabel("Password"));
        p.add(Box.createVerticalStrut(6));
        p.add(passwordField);
        p.add(Box.createVerticalStrut(28));
        p.add(loginBtn);
        p.add(Box.createVerticalStrut(16));
        p.add(signUpLabel);

        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.font(Font.PLAIN, 12));
        lbl.setForeground(Theme.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField buildTextField(String placeholder) {
        JTextField f = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? Theme.ACCENT : Theme.BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.ACCENT_LIGHT);
        f.setFont(Theme.font(Font.PLAIN, 14));
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(LEFT_ALIGNMENT);
        // Repaint border on focus
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }

    private JPasswordField buildPasswordField(String placeholder) {
        JPasswordField f = new JPasswordField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? Theme.ACCENT : Theme.BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.ACCENT_LIGHT);
        f.setFont(Theme.font(Font.PLAIN, 14));
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        f.setAlignmentX(LEFT_ALIGNMENT);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { f.repaint(); }
            public void focusLost(FocusEvent e)   { f.repaint(); }
        });
        return f;
    }


    private JButton buildLoginButton() {
        JButton btn = new JButton("Sign In") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = hovered ? Theme.ACCENT_LIGHT : Theme.ACCENT;
                g2.setColor(base);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                // subtle inner highlight
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(Theme.font(Font.BOLD, 14));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        return btn;
    }

    private void openChatRoom() {
        String name = emailField.getText().isBlank() ? "You" : emailField.getText().trim();
        glowTimer.stop();
        dispose();
        SwingUtilities.invokeLater(() -> {
            ChatRoomFrame chat = new ChatRoomFrame(name);
            chat.setVisible(true);
        });
    }
}