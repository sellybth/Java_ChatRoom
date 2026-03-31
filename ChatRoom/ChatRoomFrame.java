import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

public class ChatRoomFrame extends JFrame {

    private final String                  currentUser;
    private final Map<String, List<SampleData.Message>> conversations;
    private final List<SampleData.Contact>           contacts;

    private JPanel   chatArea;
    private JLabel   chatHeaderName;
    private JLabel   chatHeaderStatus;
    private JLabel   chatHeaderAvatar;
    private JTextField inputField;
    private String   activeContact;
    private JPanel   sidebarList;

    public ChatRoomFrame(String currentUser) {
        this.currentUser   = currentUser;
        this.conversations = SampleData.getConversations();
        this.contacts      = SampleData.getContacts();

        setTitle("ChatRoom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_DARKEST);

        // Custom title bar
        root.add(buildTitleBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG_DARKEST);
        body.add(buildNavRail(),   BorderLayout.WEST);
        body.add(buildSidebar(),   BorderLayout.CENTER);
        body.add(buildChatPanel(), BorderLayout.EAST);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        // Load first contact by default
        if (!contacts.isEmpty()) {
            loadContact(contacts.get(0));
        }

        // Make draggable by title bar
        setVisible(true);
    }

    // ── Title bar ────────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(Theme.BG_DARKEST);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 36));
        bar.setBorder(new EmptyBorder(0, 16, 0, 8));

        JLabel title = new JLabel("💬  ChatRoom");
        title.setFont(Theme.font(Font.BOLD, 13));
        title.setForeground(Theme.TEXT_SECONDARY);

        JPanel winBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        winBtns.setOpaque(false);
        winBtns.add(winButton(new Color(0xFF, 0x5F, 0x57), "✕", e -> System.exit(0)));
        winBtns.add(winButton(new Color(0xFF, 0xBD, 0x2E), "–", e -> setState(ICONIFIED)));
        winBtns.add(winButton(new Color(0x28, 0xCA, 0x41), "⬜", e -> {
            if ((getExtendedState() & MAXIMIZED_BOTH) != 0) setExtendedState(NORMAL);
            else setExtendedState(MAXIMIZED_BOTH);
        }));

        bar.add(title, BorderLayout.WEST);
        bar.add(winBtns, BorderLayout.EAST);

        // Drag support
        Point[] drag = {null};
        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { drag[0] = e.getPoint(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (drag[0] != null) {
                    Point loc = getLocation();
                    setLocation(loc.x + e.getX() - drag[0].x, loc.y + e.getY() - drag[0].y);
                }
            }
        });

        return bar;
    }

    private JButton winButton(Color col, String label, ActionListener action) {
        JButton b = new JButton() {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(col);
                g2.fillOval(0, 0, 12, 12);
                if (hov) {
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.setFont(Theme.font(Font.BOLD, 7));
                    FontMetrics fm = g2.getFontMetrics();
                    String t = label;
                    g2.drawString(t, (12 - fm.stringWidth(t)) / 2, 9);
                }
                g2.dispose();
            }
        };
        b.setPreferredSize(new Dimension(12, 12));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(action);
        return b;
    }

    // ── Left nav rail ────────────────────────────────────────────────────────
    private JPanel buildNavRail() {
        JPanel rail = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG_DARKEST);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        rail.setLayout(new BoxLayout(rail, BoxLayout.Y_AXIS));
        rail.setPreferredSize(new Dimension(64, 0));
        rail.setBorder(new EmptyBorder(12, 0, 12, 0));

        // Avatar (current user)
        JPanel avatar = buildAvatarIcon(currentUser.substring(0, Math.min(2, currentUser.length())).toUpperCase(),
                Theme.ACCENT, 40);
        avatar.setAlignmentX(CENTER_ALIGNMENT);

        rail.add(Box.createVerticalStrut(8));
        rail.add(avatar);
        rail.add(Box.createVerticalStrut(20));

        // Nav icons
        String[][] icons = {{"💬","Chats"}, {"👥","Groups"}, {"📌","Pinned"}, {"🔔","Alerts"}};
        for (int i = 0; i < icons.length; i++) {
            rail.add(navIcon(icons[i][0], icons[i][1], i == 0));
            rail.add(Box.createVerticalStrut(4));
        }

        rail.add(Box.createVerticalGlue());

        // Settings at bottom
        rail.add(navIcon("⚙️", "Settings", false));
        rail.add(Box.createVerticalStrut(8));

        return rail;
    }

    private JPanel navIcon(String emoji, String tooltip, boolean active) {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(Theme.ACCENT_DIM);
                    g2.fillRoundRect(8, 0, getWidth() - 16, getHeight(), 12, 12);
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(0, getHeight() / 2 - 12, 3, 24, 3, 3);
                }
                super.paintComponent(g);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setLayout(new GridBagLayout());
        p.setMaximumSize(new Dimension(64, 44));
        p.setPreferredSize(new Dimension(64, 44));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.setToolTipText(tooltip);

        JLabel lbl = new JLabel(emoji);
        lbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        p.add(lbl);

        p.addMouseListener(new MouseAdapter() {
            boolean hov = false;
            public void mouseEntered(MouseEvent e) { hov = true;  p.setBackground(Theme.BG_HOVER); p.setOpaque(hov); p.repaint(); }
            public void mouseExited(MouseEvent e)  { hov = false; p.setOpaque(false); p.repaint(); }
        });
        return p;
    }

    // ── Sidebar contact list ─────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(300, 0));

        // Top search bar
        sidebar.add(buildSearchBar(), BorderLayout.NORTH);

        // Contact list
        sidebarList = new JPanel();
        sidebarList.setLayout(new BoxLayout(sidebarList, BoxLayout.Y_AXIS));
        sidebarList.setBackground(Theme.BG_SIDEBAR);

        // Section header
        JLabel section = new JLabel("MESSAGES");
        section.setFont(Theme.font(Font.BOLD, 10));
        section.setForeground(Theme.TEXT_MUTED);
        section.setBorder(new EmptyBorder(14, 16, 6, 16));
        section.setAlignmentX(LEFT_ALIGNMENT);
        sidebarList.add(section);

        for (SampleData.Contact c : contacts) {
            sidebarList.add(buildContactCell(c));
        }

        JScrollPane scroll = new JScrollPane(sidebarList);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = Theme.BORDER_LIGHT;
                trackColor = Theme.BG_SIDEBAR;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });

        sidebar.add(scroll, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildSearchBar() {
        JPanel top = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG_SIDEBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 12, 10, 12));

        JTextField search = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        search.setOpaque(false);
        search.setForeground(Theme.TEXT_PRIMARY);
        search.setCaretColor(Theme.ACCENT_LIGHT);
        search.setFont(Theme.font(Font.PLAIN, 13));
        search.setBorder(new EmptyBorder(7, 32, 7, 12));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        searchIcon.setBorder(new EmptyBorder(0, 10, 0, 0));
        wrap.add(search, BorderLayout.CENTER);
        wrap.add(searchIcon, BorderLayout.WEST);

        top.add(wrap, BorderLayout.CENTER);
        return top;
    }

    private JPanel buildContactCell(SampleData.Contact c) {
        JPanel cell = new JPanel(new BorderLayout(10, 0)) {
            boolean hovered = false;
            boolean selected = false;

            {
                setOpaque(false);
                setBorder(new EmptyBorder(8, 12, 8, 12));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    public void mouseClicked(MouseEvent e) {
                        loadContact(c);
                        // Deselect all, select this
                        for (Component comp : sidebarList.getComponents()) {
                            if (comp instanceof JPanel) comp.repaint();
                        }
                        selected = true;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (c.name.equals(activeContact)) {
                    g2.setColor(Theme.BG_SELECTED);
                    g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 10, 10);
                } else if (hovered) {
                    g2.setColor(Theme.BG_HOVER);
                    g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 10, 10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cell.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        // Avatar
        Color avatarColor = avatarColor(c.name);
        JPanel avatar = buildAvatarIcon(c.avatar, avatarColor, 40);

        // Status dot overlay
        JLayeredPane avatarLayer = new JLayeredPane();
        avatarLayer.setPreferredSize(new Dimension(44, 44));
        avatarLayer.setMaximumSize(new Dimension(44, 44));
        avatar.setBounds(2, 2, 40, 40);
        avatarLayer.add(avatar, JLayeredPane.DEFAULT_LAYER);
        if (c.online) {
            JPanel dot = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.BG_SIDEBAR);
                    g2.fillOval(0, 0, 12, 12);
                    g2.setColor(Theme.ONLINE);
                    g2.fillOval(2, 2, 8, 8);
                    g2.dispose();
                }
            };
            dot.setOpaque(false);
            dot.setBounds(30, 30, 14, 14);
            avatarLayer.add(dot, JLayeredPane.PALETTE_LAYER);
        }

        // Text info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JPanel nameRow = new JPanel(new BorderLayout());
        nameRow.setOpaque(false);
        JLabel name = new JLabel(c.name);
        name.setFont(Theme.font(c.unread > 0 ? Font.BOLD : Font.PLAIN, 13));
        name.setForeground(c.unread > 0 ? Theme.TEXT_PRIMARY : Theme.TEXT_PRIMARY);
        JLabel time = new JLabel(c.time);
        time.setFont(Theme.font(Font.PLAIN, 10));
        time.setForeground(c.unread > 0 ? Theme.TEXT_ACCENT : Theme.TEXT_MUTED);
        nameRow.add(name, BorderLayout.WEST);
        nameRow.add(time, BorderLayout.EAST);

        JPanel msgRow = new JPanel(new BorderLayout());
        msgRow.setOpaque(false);
        JLabel msg = new JLabel(truncate(c.lastMsg, 32));
        msg.setFont(Theme.font(Font.PLAIN, 12));
        msg.setForeground(c.unread > 0 ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);

        msgRow.add(msg, BorderLayout.WEST);
        if (c.unread > 0) {
            JLabel badge = new JLabel(String.valueOf(c.unread)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(Theme.font(Font.BOLD, 10));
            badge.setForeground(Color.WHITE);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setPreferredSize(new Dimension(18, 18));
            msgRow.add(badge, BorderLayout.EAST);
        }

        info.add(nameRow);
        info.add(Box.createVerticalStrut(2));
        info.add(msgRow);

        cell.add(avatarLayer, BorderLayout.WEST);
        cell.add(info, BorderLayout.CENTER);
        return cell;
    }

    // ── Chat panel ───────────────────────────────────────────────────────────
    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_MAIN);
        panel.setPreferredSize(new Dimension(836, 0));

        // Header
        JPanel header = buildChatHeader();
        panel.add(header, BorderLayout.NORTH);

        // Messages area
        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
        chatArea.setBackground(Theme.BG_MAIN);
        chatArea.setBorder(new EmptyBorder(16, 20, 8, 20));

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBorder(null);
        scroll.setBackground(Theme.BG_MAIN);
        scroll.getViewport().setBackground(Theme.BG_MAIN);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = Theme.BORDER_LIGHT;
                trackColor = Theme.BG_MAIN;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroButton(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroButton(); }
            private JButton zeroButton() {
                JButton b = new JButton(); b.setPreferredSize(new Dimension(0,0)); return b;
            }
        });

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buildInputBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildChatHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG_SIDEBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);

        chatHeaderAvatar = new JLabel();
        chatHeaderAvatar.setPreferredSize(new Dimension(38, 38));

        JPanel nameBox = new JPanel();
        nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.Y_AXIS));
        nameBox.setOpaque(false);
        chatHeaderName   = new JLabel("Select a chat");
        chatHeaderName.setFont(Theme.font(Font.BOLD, 15));
        chatHeaderName.setForeground(Theme.TEXT_PRIMARY);
        chatHeaderStatus = new JLabel("...");
        chatHeaderStatus.setFont(Theme.font(Font.PLAIN, 11));
        chatHeaderStatus.setForeground(Theme.ONLINE);
        nameBox.add(chatHeaderName);
        nameBox.add(chatHeaderStatus);

        left.add(chatHeaderAvatar, BorderLayout.WEST);
        left.add(nameBox, BorderLayout.CENTER);

        // Right actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        for (String icon : new String[]{"📞", "📹", "🔍", "⋯"}) {
            JLabel btn = new JLabel(icon);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            actions.add(btn);
        }

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
        return header;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG_SIDEBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Attach + emoji
        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBtns.setOpaque(false);
        for (String icon : new String[]{"📎", "😊"}) {
            JLabel btn = new JLabel(icon);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            leftBtns.add(btn);
        }

        inputField = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        inputField.setOpaque(false);
        inputField.setForeground(Theme.TEXT_PRIMARY);
        inputField.setCaretColor(Theme.ACCENT_LIGHT);
        inputField.setFont(Theme.font(Font.PLAIN, 14));
        inputField.setBorder(new EmptyBorder(10, 16, 10, 16));

        JButton sendBtn = new JButton("➤") {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? Theme.ACCENT_LIGHT : Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        sendBtn.setFont(Theme.font(Font.BOLD, 14));
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setPreferredSize(new Dimension(44, 44));
        sendBtn.setOpaque(false);
        sendBtn.setContentAreaFilled(false);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });

        bar.add(leftBtns, BorderLayout.WEST);
        bar.add(inputField, BorderLayout.CENTER);
        bar.add(sendBtn, BorderLayout.EAST);
        return bar;
    }

    // ── Load contact ─────────────────────────────────────────────────────────
    private void loadContact(SampleData.Contact c) {
        activeContact = c.name;
        chatHeaderName.setText(c.name);
        chatHeaderStatus.setText(c.online ? "● Online" : "● Offline");
        chatHeaderStatus.setForeground(c.online ? Theme.ONLINE : Theme.TEXT_MUTED);

        // Redraw avatar in header
        JPanel av = buildAvatarIcon(c.avatar, avatarColor(c.name), 36);
        chatHeaderAvatar.removeAll();
        chatHeaderAvatar.setLayout(new BorderLayout());
        chatHeaderAvatar.add(av);
        chatHeaderAvatar.revalidate();

        // Populate messages
        chatArea.removeAll();
        List<SampleData.Message> msgs = conversations.getOrDefault(c.name, List.of());

        String lastDate = "";
        for (SampleData.Message m : msgs) {
            String dateLabel = "Today";
            if (!dateLabel.equals(lastDate)) {
                chatArea.add(buildDateSeparator(dateLabel));
                lastDate = dateLabel;
            }
            chatArea.add(buildMessageBubble(m));
            chatArea.add(Box.createVerticalStrut(4));
        }
        chatArea.add(Box.createVerticalGlue());

        chatArea.revalidate();
        chatArea.repaint();
        sidebarList.repaint();

        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) chatArea.getParent().getParent();
            JScrollBar vsb = sp.getVerticalScrollBar();
            vsb.setValue(vsb.getMaximum());
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || activeContact == null) return;

        SampleData.Message m = new SampleData.Message(currentUser, text, "Now", true);
        conversations.computeIfAbsent(activeContact, k -> new java.util.ArrayList<>()).add(m);

        chatArea.remove(chatArea.getComponentCount() - 1); // remove glue
        chatArea.add(buildMessageBubble(m));
        chatArea.add(Box.createVerticalStrut(4));
        chatArea.add(Box.createVerticalGlue());
        chatArea.revalidate();
        chatArea.repaint();

        inputField.setText("");

        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) chatArea.getParent().getParent();
            sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());
        });
    }

    // ── Message bubble ────────────────────────────────────────────────────────
    private JPanel buildMessageBubble(SampleData.Message m) {
        boolean mine = m.isMine;

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        if (mine) wrapper.add(Box.createHorizontalGlue());

        // Avatar for received messages
        if (!mine) {
            JPanel av = buildAvatarIcon(m.sender.substring(0, Math.min(2, m.sender.length())).toUpperCase(),
                    avatarColor(m.sender), 28);
            av.setAlignmentY(TOP_ALIGNMENT);
            wrapper.add(av);
            wrapper.add(Box.createHorizontalStrut(8));
        }

        // Bubble
        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(mine ? Theme.BUBBLE_SENT : Theme.BUBBLE_RECV);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // sharp corner on the message side
                if (mine) {
                    g2.fillRect(getWidth() - 14, 0, 14, 14);
                } else {
                    g2.fillRect(0, 0, 14, 14);
                }
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(9, 13, 9, 13));

        // Sender name (for groups / received)
        if (!mine) {
            JLabel senderLbl = new JLabel(m.sender);
            senderLbl.setFont(Theme.font(Font.BOLD, 11));
            senderLbl.setForeground(avatarColor(m.sender));
            bubble.add(senderLbl);
            bubble.add(Box.createVerticalStrut(2));
        }

        JLabel text = new JLabel("<html><body style='width:300px'>" + m.content + "</body></html>");
        text.setFont(Theme.font(Font.PLAIN, 13));
        text.setForeground(mine ? Color.WHITE : Theme.TEXT_PRIMARY);

        JLabel time = new JLabel(m.time);
        time.setFont(Theme.font(Font.PLAIN, 10));
        time.setForeground(mine ? new Color(0xFF, 0xFF, 0xFF, 140) : Theme.TEXT_MUTED);
        time.setAlignmentX(mine ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);

        bubble.add(text);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(time);
        bubble.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        wrapper.add(bubble);

        if (!mine) wrapper.add(Box.createHorizontalGlue());

        return wrapper;
    }

    private JPanel buildDateSeparator(String label) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));

        JSeparator left  = styledSep();
        JSeparator right = styledSep();

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.font(Font.PLAIN, 11));
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        row.add(left,  BorderLayout.WEST);
        row.add(lbl,   BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return row;
    }

    private JSeparator styledSep() {
        JSeparator sep = new JSeparator() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BORDER);
                g.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        };
        sep.setPreferredSize(new Dimension(100, 10));
        return sep;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JPanel buildAvatarIcon(String initials, Color color, int size) {
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 0, size, size);
                g2.setColor(Color.WHITE);
                g2.setFont(Theme.font(Font.BOLD, size / 3));
                FontMetrics fm = g2.getFontMetrics();
                String text = initials.length() > 2 ? initials.substring(0, 2) : initials;
                g2.drawString(text,
                        (size - fm.stringWidth(text)) / 2,
                        (size - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        av.setOpaque(false);
        av.setPreferredSize(new Dimension(size, size));
        av.setMaximumSize(new Dimension(size, size));
        av.setMinimumSize(new Dimension(size, size));
        return av;
    }

    private Color avatarColor(String name) {
        Color[] colors = {
            new Color(0x6C, 0x63, 0xFF),
            new Color(0xFF, 0x6B, 0x6B),
            new Color(0x4E, 0xCA, 0xA0),
            new Color(0xFF, 0x9F, 0x43),
            new Color(0x54, 0xA0, 0xFF),
            new Color(0xFF, 0x6B, 0xD5),
            new Color(0x5F, 0x27, 0xCD),
            new Color(0x00, 0xD2, 0xD3)
        };
        return colors[Math.abs(name.hashCode()) % colors.length];
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
