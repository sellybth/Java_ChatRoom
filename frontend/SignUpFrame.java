
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class SignUpFrame extends JDialog {

    private JTextField     nameField;
    private JTextField     emailField;
    private JPasswordField passwordField;
    private JTextField     phoneField;
    private JButton        signUpBtn;

    public SignUpFrame(JFrame parent) {
        super(parent, true);
        setUndecorated(true);
        setSize(620, 600);
        setLocationRelativeTo(parent);
        setShape(new RoundRectangle2D.Double(0, 0, 620, 600, 20, 20));

        JPanel main = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 28));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            }
        };
        main.setLayout(new BorderLayout());
        main.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Close button
        JButton closeBtn = new JButton("X");
        closeBtn.setFont(new Font("Arial", Font.PLAIN, 20));
        closeBtn.setForeground(new Color(150, 150, 170));
        closeBtn.setBackground(new Color(18, 18, 28));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);
        topPanel.add(closeBtn);
        main.add(topPanel, BorderLayout.NORTH);

        // Center form
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        // Title
        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Join Blabber today");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(150, 150, 170));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(title);
        form.add(Box.createVerticalStrut(6));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(30));

        // Name field
        form.add(makeLabel("Full Name"));
        form.add(Box.createVerticalStrut(8));
        nameField = makeTextField();
        form.add(nameField);
        form.add(Box.createVerticalStrut(16));

        // Email field
        form.add(makeLabel("Email"));
        form.add(Box.createVerticalStrut(8));
        emailField = makeTextField();
        form.add(emailField);
        form.add(Box.createVerticalStrut(16));

        // Password field
        form.add(makeLabel("Password"));
        form.add(Box.createVerticalStrut(8));
        passwordField = new JPasswordField();
        styleTextField(passwordField);
        form.add(passwordField);
        form.add(Box.createVerticalStrut(16));

        // Phone field
        form.add(makeLabel("Phone Number"));
        form.add(Box.createVerticalStrut(8));
        phoneField = makeTextField();
        form.add(phoneField);
        form.add(Box.createVerticalStrut(24));

        // Sign Up button
        signUpBtn = new JButton("Sign Up");
        signUpBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        signUpBtn.setBackground(new Color(99, 87, 220));
        signUpBtn.setForeground(Color.WHITE);
        signUpBtn.setFocusPainted(false);
        signUpBtn.setBorderPainted(false);
        signUpBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signUpBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        signUpBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signUpBtn.addActionListener(e -> handleSignUp());
        form.add(signUpBtn);
        form.add(Box.createVerticalStrut(16));

        // Login link
        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        loginPanel.setOpaque(false);
        JLabel alreadyLabel = new JLabel("Already have an account? ");
        alreadyLabel.setForeground(new Color(150, 150, 170));
        alreadyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        JLabel loginLink = new JLabel("Login");
        loginLink.setForeground(new Color(99, 87, 220));
        loginLink.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) { dispose(); }
        });
        loginPanel.add(alreadyLabel);
        loginPanel.add(loginLink);
        loginPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(loginPanel);

        main.add(form, BorderLayout.CENTER);
        setContentPane(main);
    }

    // Handle sign up button click
    private void handleSignUp() {
        String name = nameField.getText().trim();
String email = emailField.getText().trim();
String password = new String(passwordField.getPassword()).trim();
String phone = phoneField.getText().trim();

// Regex patterns
String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
//8 digits and alphanumeric
String passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$"; 
//10 digit phone number
String phoneRegex = "^[0-9]{10}$";

// Validation
if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
    JOptionPane.showMessageDialog(this,
        "Please fill in all fields",
        "Error", JOptionPane.ERROR_MESSAGE);
    return;
}

if (!email.matches(emailRegex)) {
    JOptionPane.showMessageDialog(this,
        "Invalid email format",
        "Error", JOptionPane.ERROR_MESSAGE);
    return;
}

if (!password.matches(passwordRegex)) {
    JOptionPane.showMessageDialog(this,
        "Password must be at least 8 characters and contain letters and numbers",
        "Error", JOptionPane.ERROR_MESSAGE);
    return;
}

if (!phone.matches(phoneRegex)) {
    JOptionPane.showMessageDialog(this,
        "Phone number must be exactly 10 digits",
        "Error", JOptionPane.ERROR_MESSAGE);
    return;
}

        // Disable button while calling API
        signUpBtn.setEnabled(false);
        signUpBtn.setText("Creating account...");

        // Run in background thread so UI doesn't freeze
        new Thread(() -> {
            try {
                ApiService.register(name, email, password, phone);

                // Back to UI thread
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        "Account created! Please login.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    dispose(); // Close signup, go back to login
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                        ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    signUpBtn.setEnabled(true);
                    signUpBtn.setText("Sign Up");
                });
            }
        }).start();
    }

    // Helper — makes a styled label
    private JLabel makeLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(180, 180, 200));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    // Helper — makes a styled text field
    private JTextField makeTextField() {
        JTextField field = new JTextField();
        styleTextField(field);
        return field;
    }

    // Helper — applies dark style to any text component
    private void styleTextField(JTextField field) {
        field.setBackground(new Color(30, 30, 45));
        field.setForeground(Color.WHITE);
        field.setCaretColor(Color.WHITE);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(99, 87, 220), 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }
}