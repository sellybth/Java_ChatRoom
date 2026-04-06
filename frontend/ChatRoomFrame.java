import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class ChatRoomFrame extends JFrame {

    private final String currentUser;
    private final long   currentUserId;

    // Active conversation
    private long   activeGroupId   = -1;
    private String activeGroupName = null;
    private String activeGroupType = null;

    // UI
    private JPanel    chatArea;
    private JLabel    chatHeaderName;
    private JLabel    chatHeaderStatus;
    private JTextField inputField;
    private JPanel    sidebarList;
    private JButton   sendBtn;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("hh:mm a").withZone(ZoneId.systemDefault());

    public ChatRoomFrame(String currentUser) {
        this.currentUser   = currentUser;
        this.currentUserId = SessionManager.getInstance().getUserId();

        setTitle("ChatRoom");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setUndecorated(true);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_DARKEST);
        root.add(buildTitleBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Theme.BG_DARKEST);
        body.add(buildNavRail(),   BorderLayout.WEST);
        body.add(buildSidebar(),   BorderLayout.CENTER);
        body.add(buildChatPanel(), BorderLayout.EAST);
        root.add(body, BorderLayout.CENTER);

        setContentPane(root);

        // Load groups into sidebar
        loadGroups();
    }

    // ── Incoming WebSocket message ────────────────────────────────────────────
    private void handleIncomingMessage(String rawJson) {
        try {
            JSONObject msg = new JSONObject(rawJson);
            long groupId  = msg.getLong("groupId");
            // Skip messages sent by me — already shown optimistically
            long senderId = msg.optLong("userId", -1);
    
            // Skip — already shown optimistically when we sent it
            if (senderId == currentUserId) return;
    
            if (groupId == activeGroupId) {
                String sender  = msg.optString("senderName", "?");
                String content = msg.optString("content", "");
                String time    = msg.optString("timestamp", "Now");
                appendBubble(sender, content, time, false);
            }
        } catch (Exception e) {
            System.err.println("[WS] Bad message: " + e.getMessage());
        }
    }

    // ── Load groups from API ──────────────────────────────────────────────────
    private void loadGroups() {
        new Thread(() -> {
            try {
                JSONArray groups = ApiService.getGroups();
                SwingUtilities.invokeLater(() -> {
                    sidebarList.removeAll();
                    addSectionHeader("CONVERSATIONS");
                    for (int i = 0; i < groups.length(); i++) {
                        JSONObject g = groups.getJSONObject(i);
                        String displayName = resolveDisplayName(
                            g.getString("name"),
                            g.getString("type")
                        );
                    
                        sidebarList.add(buildGroupCell(
                            g.getLong("groupId"),
                            displayName,   // ✅ use resolved name
                            g.getString("type")
                        ));
                    }
                    sidebarList.revalidate();
                    sidebarList.repaint();
                });
            } catch (Exception e) {
                showError("Failed to load conversations: " + e.getMessage());
            }
        }).start();
    }

    // ── Load messages for a group ─────────────────────────────────────────────
    private void loadMessages(long groupId) {
        chatArea.removeAll();
        chatArea.revalidate();
        chatArea.repaint();

        new Thread(() -> {
            try {
                JSONArray messages = ApiService.getMessages(groupId);
                SwingUtilities.invokeLater(() -> {
                    chatArea.removeAll();
                    for (int i = 0; i < messages.length(); i++) {
                        JSONObject m = messages.getJSONObject(i);
                        String sender  = m.optString("senderName", "?");
                        String content = m.optString("content", "");
                        long senderId  = m.optLong("userId", -1);
                        String ts      = m.optString("timestamp", "");
                        String time = ts.isEmpty() ? "" : ts.substring(0, 16).replace("T", " ");
                        boolean mine   = senderId == currentUserId;
                        chatArea.add(buildMessageBubble(sender, content, time, mine));
                        chatArea.add(Box.createVerticalStrut(4));
                    }
                    chatArea.add(Box.createVerticalGlue());
                    chatArea.revalidate();
                    chatArea.repaint();
                    scrollToBottom();
                });
            } catch (Exception e) {
                showError("Failed to load messages: " + e.getMessage());
            }
        }).start();
    }

    private void appendBubble(String sender, String content, String time, boolean mine) {
        // Remove glue before adding
        int count = chatArea.getComponentCount();
        if (count > 0) chatArea.remove(count - 1);
        chatArea.add(buildMessageBubble(sender, content, time, mine));
        chatArea.add(Box.createVerticalStrut(4));
        chatArea.add(Box.createVerticalGlue());
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }

    // ── Title bar ─────────────────────────────────────────────────────────────
    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
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
        winBtns.add(winButton(new Color(0xFF, 0x5F, 0x57), "✕", e -> {
            WebSocketService.getInstance().disconnect();
            System.exit(0);
        }));
        winBtns.add(winButton(new Color(0xFF, 0xBD, 0x2E), "–", e -> setState(ICONIFIED)));
        winBtns.add(winButton(new Color(0x28, 0xCA, 0x41), "⬜", e -> {
            if ((getExtendedState() & MAXIMIZED_BOTH) != 0) setExtendedState(NORMAL);
            else setExtendedState(MAXIMIZED_BOTH);
        }));

        bar.add(title, BorderLayout.WEST);
        bar.add(winBtns, BorderLayout.EAST);

        Point[] drag = {null};
        bar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { drag[0] = e.getPoint(); }
        });
        bar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (drag[0] != null) {
                    Point loc = getLocation();
                    setLocation(loc.x + e.getX() - drag[0].x,
                                loc.y + e.getY() - drag[0].y);
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
                    g2.drawString(label, (12 - fm.stringWidth(label)) / 2, 9);
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

    // ── Nav rail ──────────────────────────────────────────────────────────────
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

        JPanel avatar = buildAvatarIcon(
            currentUser.substring(0, Math.min(2, currentUser.length())).toUpperCase(),
            Theme.ACCENT, 40);
        avatar.setAlignmentX(CENTER_ALIGNMENT);

        rail.add(Box.createVerticalStrut(8));
        rail.add(avatar);
        rail.add(Box.createVerticalStrut(20));

        String[][] icons = {{"💬","Chats"}};
        for (int i = 0; i < icons.length; i++) {
            rail.add(navIcon(icons[i][0], icons[i][1], i == 0));
            rail.add(Box.createVerticalStrut(4));
        }

        rail.add(Box.createVerticalGlue());

        // Logout button
        JPanel logoutIcon = navIcon("🚪", "Logout", false);
        logoutIcon.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                    ChatRoomFrame.this,
                    "Are you sure you want to logout?",
                    "Logout", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    WebSocketService.getInstance().disconnect();
                    SessionManager.getInstance().clearSession();
                    dispose();
                    new LoginFrame().setVisible(true);
                }
            }
        });
        rail.add(logoutIcon);
        rail.add(Box.createVerticalStrut(4));
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
            public void mouseEntered(MouseEvent e) { p.setBackground(Theme.BG_HOVER); p.setOpaque(true);  p.repaint(); }
            public void mouseExited(MouseEvent e)  { p.setOpaque(false); p.repaint(); }
        });
        return p;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(Theme.BG_SIDEBAR);
        sidebar.setPreferredSize(new Dimension(300, 0));

        sidebar.add(buildSidebarTop(), BorderLayout.NORTH);

        sidebarList = new JPanel();
        sidebarList.setLayout(new BoxLayout(sidebarList, BoxLayout.Y_AXIS));
        sidebarList.setBackground(Theme.BG_SIDEBAR);

        JScrollPane scroll = new JScrollPane(sidebarList);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = Theme.BORDER_LIGHT;
                trackColor = Theme.BG_SIDEBAR;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        sidebar.add(scroll, BorderLayout.CENTER);
        return sidebar;
    }

    private JPanel buildSidebarTop() {
        JPanel top = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Theme.BG_SIDEBAR);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.BORDER);
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Search field
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
        JLabel icon = new JLabel("🔍");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        icon.setBorder(new EmptyBorder(0, 10, 0, 0));
        wrap.add(icon, BorderLayout.WEST);
        wrap.add(search, BorderLayout.CENTER);

        // New chat / group button
        JButton newBtn = new JButton("+") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        newBtn.setForeground(Color.WHITE);
        newBtn.setFont(Theme.font(Font.BOLD, 16));
        newBtn.setPreferredSize(new Dimension(32, 32));
        newBtn.setOpaque(false);
        newBtn.setContentAreaFilled(false);
        newBtn.setBorderPainted(false);
        newBtn.setFocusPainted(false);
        newBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newBtn.addActionListener(e -> showNewChatDialog());

        top.add(wrap,   BorderLayout.CENTER);
        top.add(newBtn, BorderLayout.EAST);
        return top;
    }

    private void addSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.font(Font.BOLD, 10));
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setBorder(new EmptyBorder(14, 16, 6, 16));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        sidebarList.add(lbl);
    }

    private JPanel buildGroupCell(long groupId, String name, String type) {
        boolean isDirect = "direct".equalsIgnoreCase(type);
        String  avatar   = name.substring(0, Math.min(2, name.length())).toUpperCase();
        Color   color    = avatarColor(name);

        JPanel cell = new JPanel(new BorderLayout(10, 0)) {
            boolean hovered = false;
            {
                setOpaque(false);
                setBorder(new EmptyBorder(8, 12, 8, 12));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    public void mouseClicked(MouseEvent e) {
                        activeGroupId   = groupId;
                        activeGroupName = name;
                        activeGroupType = type;
                        chatHeaderName.setText(name);
                        chatHeaderStatus.setText(isDirect ? "Direct Message" : "Group");
                        chatHeaderStatus.setForeground(isDirect ? Theme.ACCENT_LIGHT : Theme.ONLINE);
                        loadMessages(groupId);
                        inputField.setEnabled(true);
                        sendBtn.setEnabled(true);

                        //connect/resubscribe to this group's topic
                        String token = SessionManager.getInstance().getToken();
    WebSocketService.getInstance().connect(token, groupId, rawJson ->
        SwingUtilities.invokeLater(() -> handleIncomingMessage(rawJson))
    );
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (groupId == activeGroupId) {
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

        JPanel av = buildAvatarIcon(avatar, color, 40);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(Theme.font(Font.PLAIN, 13));
        nameLbl.setForeground(Theme.TEXT_PRIMARY);

        JLabel typeLbl = new JLabel(isDirect ? "Direct Message" : "Group");
        typeLbl.setFont(Theme.font(Font.PLAIN, 11));
        typeLbl.setForeground(Theme.TEXT_MUTED);

        info.add(nameLbl);
        info.add(Box.createVerticalStrut(2));
        info.add(typeLbl);

        cell.add(av,   BorderLayout.WEST);
        cell.add(info, BorderLayout.CENTER);
        return cell;
    }

    // ── New chat/group dialog ─────────────────────────────────────────────────
private void showNewChatDialog() {
    JDialog dialog = new JDialog(this, "New Conversation", true);
    dialog.setUndecorated(true);
    dialog.setSize(420, 520);
    dialog.setLocationRelativeTo(this);

    JPanel panel = new JPanel(new BorderLayout());
    panel.setBackground(Theme.BG_SIDEBAR);
    panel.setBorder(new LineBorder(Theme.BORDER, 1));

    // Header
    JPanel header = new JPanel(new BorderLayout());
    header.setOpaque(false);
    header.setBorder(new EmptyBorder(16, 20, 12, 20));
    JLabel title = new JLabel("New Conversation");
    title.setFont(Theme.font(Font.BOLD, 16));
    title.setForeground(Theme.TEXT_PRIMARY);
    JButton closeBtn = new JButton("✕");
    closeBtn.setForeground(Theme.TEXT_MUTED);
    closeBtn.setBackground(new Color(0, 0, 0, 0));
    closeBtn.setBorder(BorderFactory.createEmptyBorder());
    closeBtn.setFont(Theme.font(Font.PLAIN, 14));
    closeBtn.setContentAreaFilled(false);
    closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    closeBtn.addActionListener(e -> dialog.dispose());
    header.add(title,    BorderLayout.WEST);
    header.add(closeBtn, BorderLayout.EAST);

    // ── Type selector with clear labels ──────────────────────────────────
    JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    typeRow.setOpaque(false);
    typeRow.setBorder(new EmptyBorder(0, 20, 0, 20));

    JToggleButton dmBtn  = styledToggle("💬 Direct (1 person)", true);
    JToggleButton grpBtn = styledToggle("👥 Group (multiple)",  false);
    ButtonGroup bg = new ButtonGroup();
    bg.add(dmBtn); bg.add(grpBtn);
    typeRow.add(dmBtn);
    typeRow.add(grpBtn);

    // ── Helper hint label ─────────────────────────────────────────────────
    JLabel hintLabel = new JLabel("Select exactly 1 person");
    hintLabel.setFont(Theme.font(Font.PLAIN, 11));
    hintLabel.setForeground(Theme.TEXT_MUTED);
    hintLabel.setBorder(new EmptyBorder(4, 20, 8, 20));

    // ── Group name field (only for group type) ────────────────────────────
    JPanel groupNamePanel = new JPanel(new BorderLayout());
    groupNamePanel.setOpaque(false);
    groupNamePanel.setBorder(new EmptyBorder(4, 20, 8, 20));
    JTextField groupNameField = buildSmallField("Group name (optional)...");
    groupNamePanel.add(groupNameField);
    groupNamePanel.setVisible(false);

    dmBtn.addActionListener(e -> {
        groupNamePanel.setVisible(false);
        hintLabel.setText("Select exactly 1 person");
        dialog.revalidate();
    });
    grpBtn.addActionListener(e -> {
        groupNamePanel.setVisible(true);
        hintLabel.setText("Select 2 or more people");
        dialog.revalidate();
    });

    // ── User list ─────────────────────────────────────────────────────────
    JPanel userList = new JPanel();
    userList.setLayout(new BoxLayout(userList, BoxLayout.Y_AXIS));
    userList.setBackground(Theme.BG_SIDEBAR);
    JScrollPane scroll = new JScrollPane(userList);
    scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
    scroll.setBackground(Theme.BG_SIDEBAR);
    scroll.getViewport().setBackground(Theme.BG_SIDEBAR);

    List<Long>   selectedIds   = new ArrayList<>();
    List<String> selectedNames = new ArrayList<>();

    new Thread(() -> {
        try {
            JSONArray users = ApiService.getUsers();
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < users.length(); i++) {
                    JSONObject u  = users.getJSONObject(i);
                    long   uid    = u.getLong("userId");
                    String uname  = u.getString("name");
                    String uemail = u.getString("email");
                    if (uid == currentUserId) continue;

                    JCheckBox cb = new JCheckBox();
                    cb.setOpaque(false);

                    JPanel row = new JPanel(new BorderLayout(10, 0));
                    row.setOpaque(false);
                    row.setBorder(new EmptyBorder(8, 20, 8, 20));
                    row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    JPanel av = buildAvatarIcon(
                        uname.substring(0, Math.min(2, uname.length())).toUpperCase(),
                        avatarColor(uname), 36);

                    JPanel info = new JPanel();
                    info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                    info.setOpaque(false);
                    JLabel nl = new JLabel(uname);
                    nl.setFont(Theme.font(Font.PLAIN, 13));
                    nl.setForeground(Theme.TEXT_PRIMARY);
                    JLabel el = new JLabel(uemail);
                    el.setFont(Theme.font(Font.PLAIN, 11));
                    el.setForeground(Theme.TEXT_MUTED);
                    info.add(nl);
                    info.add(el);

                    cb.addActionListener(ev -> {
                        if (cb.isSelected()) {
                            // If DM mode, allow only 1 selection
                            if (dmBtn.isSelected() && !selectedIds.isEmpty()) {
                                cb.setSelected(false);
                                JOptionPane.showMessageDialog(dialog,
                                    "Direct message only supports 1 recipient.\nSwitch to Group for multiple people.",
                                    "One Person Only", JOptionPane.INFORMATION_MESSAGE);
                                return;
                            }
                            selectedIds.add(uid);
                            selectedNames.add(uname);
                        } else {
                            selectedIds.remove(uid);
                            selectedNames.remove(uname);
                        }
                    });

                    row.add(av,   BorderLayout.WEST);
                    row.add(info, BorderLayout.CENTER);
                    row.add(cb,   BorderLayout.EAST);
                    row.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent e) {
                            if (!(e.getSource() instanceof JCheckBox)) {
                                cb.doClick();
                            }
                        }
                    });

                    userList.add(row);
                }
                userList.revalidate();
                userList.repaint();
            });
        } catch (Exception e) {
            showError("Could not load users: " + e.getMessage());
        }
    }).start();

    // ── Create button ─────────────────────────────────────────────────────
    JButton createBtn = buildAccentButton("Create Conversation");
    JPanel btnPanel = new JPanel(new BorderLayout());
    btnPanel.setOpaque(false);
    btnPanel.setBorder(new EmptyBorder(10, 20, 16, 20));
    btnPanel.add(createBtn);

    createBtn.addActionListener(e -> {
        if (selectedIds.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Please select at least one person.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean isDM = dmBtn.isSelected();

        if (isDM && selectedIds.size() > 1) {
            JOptionPane.showMessageDialog(dialog,
                "Direct message can only have 1 recipient.\nSwitch to Group to add more people.",
                "Too Many Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isDM && selectedIds.size() < 2) {
            JOptionPane.showMessageDialog(dialog,
                "Please select at least 2 people for a group,\nor switch to Direct Message.",
                "Not Enough Members", JOptionPane.WARNING_MESSAGE);
            return;
        }

        createBtn.setEnabled(false);
        createBtn.setText("Creating...");

        new Thread(() -> {
            try {
                if (isDM) {
                    // DM: create direct conversation with the single selected user
                    ApiService.createDm(selectedIds.get(0));
                } else {
                    // Group: create group then add all selected members
                    String gName = groupNameField.getText().trim();
                    if (gName.isEmpty()) gName = String.join(", ", selectedNames);

                    JSONObject newGroup = ApiService.createGroup(gName);
                    long newGroupId = newGroup.getLong("groupId");   // ← use returned ID

                    // Add each selected member to the group
                    for (long memberId : selectedIds) {
                        ApiService.addMemberToGroup(newGroupId, memberId);  // ← ADD THIS
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    loadGroups();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    createBtn.setEnabled(true);
                    createBtn.setText("Create Conversation");
                });
            }
        }).start();
    });

    // ── Layout ────────────────────────────────────────────────────────────
    JPanel topSection = new JPanel();
    topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
    topSection.setOpaque(false);
    topSection.add(typeRow);
    topSection.add(hintLabel);
    topSection.add(groupNamePanel);

    JPanel center = new JPanel(new BorderLayout());
    center.setOpaque(false);
    center.add(topSection, BorderLayout.NORTH);
    center.add(scroll,     BorderLayout.CENTER);

    panel.add(header,   BorderLayout.NORTH);
    panel.add(center,   BorderLayout.CENTER);
    panel.add(btnPanel, BorderLayout.SOUTH);

    dialog.setContentPane(panel);
    dialog.setVisible(true);
}

    private JToggleButton styledToggle(String text, boolean selected) {
        JToggleButton btn = new JToggleButton(text, selected) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? Theme.ACCENT : Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(Theme.font(Font.PLAIN, 12));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 32));
        return btn;
    }

    private JTextField buildSmallField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
            @Override protected void paintBorder(Graphics g) {}
        };
        f.setOpaque(false);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.ACCENT_LIGHT);
        f.setFont(Theme.font(Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(8, 12, 8, 12));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return f;
    }

    private JButton buildAccentButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
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
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return btn;
    }

    // Add this helper method anywhere in ChatRoomFrame
private String resolveDisplayName(String rawName, String type) {
    if (!"direct".equalsIgnoreCase(type)) {
        // For groups, exclude current user's name from the display
        // Group names are stored as "alice, sarah, selly" — filter self out
        String[] parts = rawName.split(",");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.equalsIgnoreCase(currentUser)) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(trimmed);
            }
        }
        return sb.length() > 0 ? sb.toString() : rawName;
    }

    // DM: name is stored as "id:name|id:name" — pick the one that isn't me
    if (rawName.contains("|") && rawName.contains(":")) {
        String[] entries = rawName.split("\\|");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                long entryId = Long.parseLong(parts[0].trim());
                if (entryId != currentUserId) {
                    return parts[1].trim(); // ← return the other person's name
                }
            }
        }
    }

    return rawName; // fallback
}

    // ── Chat panel ────────────────────────────────────────────────────────────
    private JPanel buildChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG_MAIN);
        panel.setPreferredSize(new Dimension(836, 0));

        panel.add(buildChatHeader(), BorderLayout.NORTH);

        chatArea = new JPanel();
        chatArea.setLayout(new BoxLayout(chatArea, BoxLayout.Y_AXIS));
        chatArea.setBackground(Theme.BG_MAIN);
        chatArea.setBorder(new EmptyBorder(16, 20, 8, 20));

        // Empty state
        JLabel empty = new JLabel("Select a conversation to start chatting");
        empty.setFont(Theme.font(Font.PLAIN, 14));
        empty.setForeground(Theme.TEXT_MUTED);
        empty.setAlignmentX(CENTER_ALIGNMENT);
        chatArea.add(Box.createVerticalGlue());
        chatArea.add(empty);
        chatArea.add(Box.createVerticalGlue());

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
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }
        });

        panel.add(scroll,          BorderLayout.CENTER);
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

        JPanel nameBox = new JPanel();
        nameBox.setLayout(new BoxLayout(nameBox, BoxLayout.Y_AXIS));
        nameBox.setOpaque(false);

        chatHeaderName = new JLabel("Select a conversation");
        chatHeaderName.setFont(Theme.font(Font.BOLD, 15));
        chatHeaderName.setForeground(Theme.TEXT_PRIMARY);

        chatHeaderStatus = new JLabel("");
        chatHeaderStatus.setFont(Theme.font(Font.PLAIN, 11));
        chatHeaderStatus.setForeground(Theme.TEXT_MUTED);

        nameBox.add(chatHeaderName);
        nameBox.add(chatHeaderStatus);
        left.add(nameBox, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        for (String icon : new String[]{"📞", "📹", "🔍", "⋯"}) {
            JLabel btn = new JLabel(icon);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            actions.add(btn);
        }

        header.add(left,    BorderLayout.WEST);
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
        inputField.setEnabled(false); // disabled until a chat is selected

        sendBtn = new JButton("➤") {
            boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(!isEnabled() ? Theme.BORDER : hov ? Theme.ACCENT_LIGHT : Theme.ACCENT);
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
        sendBtn.setEnabled(false);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });

        bar.add(leftBtns,  BorderLayout.WEST);
        bar.add(inputField, BorderLayout.CENTER);
        bar.add(sendBtn,   BorderLayout.EAST);
        return bar;
    }

    // ── Send message ──────────────────────────────────────────────────────────
    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || activeGroupId == -1) return;
    
        inputField.setText("");
        sendBtn.setEnabled(false);
    
        // Show optimistically in UI immediately
        String time = TIME_FMT.format(Instant.now());
        appendBubble(currentUser, text, time, true);
    
        new Thread(() -> {
            try {
                WebSocketService.getInstance().sendMessage(activeGroupId, text);
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> showError("Failed to send: " + e.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> sendBtn.setEnabled(true));
            }
        }).start();
    }

    // ── Message bubble ────────────────────────────────────────────────────────
    private JPanel buildMessageBubble(String sender, String content, String time, boolean mine) {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        if (mine) wrapper.add(Box.createHorizontalGlue());

        if (!mine) {
            JPanel av = buildAvatarIcon(
                sender.substring(0, Math.min(2, sender.length())).toUpperCase(),
                avatarColor(sender), 28);
            av.setAlignmentY(TOP_ALIGNMENT);
            wrapper.add(av);
            wrapper.add(Box.createHorizontalStrut(8));
        }

        JPanel bubble = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(mine ? Theme.BUBBLE_SENT : Theme.BUBBLE_RECV);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                if (mine) g2.fillRect(getWidth() - 14, 0, 14, 14);
                else      g2.fillRect(0, 0, 14, 14);
                g2.dispose();
            }
        };
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setOpaque(false);
        bubble.setBorder(new EmptyBorder(9, 13, 9, 13));

        if (!mine) {
            JLabel senderLbl = new JLabel(sender);
            senderLbl.setFont(Theme.font(Font.BOLD, 11));
            senderLbl.setForeground(avatarColor(sender));
            bubble.add(senderLbl);
            bubble.add(Box.createVerticalStrut(2));
        }

        JLabel textLbl = new JLabel("<html><body style='width:300px'>" + content + "</body></html>");
        textLbl.setFont(Theme.font(Font.PLAIN, 13));
        textLbl.setForeground(mine ? Color.WHITE : Theme.TEXT_PRIMARY);

        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(Theme.font(Font.PLAIN, 10));
        timeLbl.setForeground(mine ? new Color(0xFF, 0xFF, 0xFF, 140) : Theme.TEXT_MUTED);
        timeLbl.setAlignmentX(mine ? RIGHT_ALIGNMENT : LEFT_ALIGNMENT);

        bubble.add(textLbl);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(timeLbl);
        bubble.setMaximumSize(new Dimension(500, Integer.MAX_VALUE));

        wrapper.add(bubble);
        if (!mine) wrapper.add(Box.createHorizontalGlue());

        return wrapper;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = (JScrollPane) chatArea.getParent().getParent();
            sp.getVerticalScrollBar().setValue(sp.getVerticalScrollBar().getMaximum());
        });
    }

    private void showError(String msg) {
        SwingUtilities.invokeLater(() ->
            JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE));
    }

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
            new Color(0x6C, 0x63, 0xFF), new Color(0xFF, 0x6B, 0x6B),
            new Color(0x4E, 0xCA, 0xA0), new Color(0xFF, 0x9F, 0x43),
            new Color(0x54, 0xA0, 0xFF), new Color(0xFF, 0x6B, 0xD5),
            new Color(0x5F, 0x27, 0xCD), new Color(0x00, 0xD2, 0xD3)
        };
        return colors[Math.abs(name.hashCode()) % colors.length];
    }
}