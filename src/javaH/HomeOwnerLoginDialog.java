package javaH;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class HomeOwnerLoginDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(HomeOwnerLoginDialog.class);

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton, registerButton, cancelButton;
    private JLabel statusLabel;
    
    private final DatabaseHelper dbHelper;
    private boolean loginSuccessful = false;
    private String loggedInUsername = null;

    public HomeOwnerLoginDialog(Frame owner, DatabaseHelper dbHelper) {
        super(owner, "Owner Secure Login", true);
        this.dbHelper = dbHelper;
        
        if (this.dbHelper == null) {
             log.error("FATAL: DatabaseHelper is NULL in HomeOwnerLoginDialog!");
             JOptionPane.showMessageDialog(this, "Internal error: Database helper not available.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        initComponents();
        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new MigLayout("fill, insets 30 40 30 40", "[center]", "[][][][]"));

        JPanel headerPanel = new JPanel(new MigLayout("insets 0, center", "[center]", "[]5[]"));
        JLabel logoLabel = new JLabel();
        try {
            URL logoUrl = getClass().getResource("icons/login.png");
            if (logoUrl != null) {
                logoLabel.setIcon(new ImageIcon(new ImageIcon(logoUrl).getImage().getScaledInstance(70, 70, Image.SCALE_SMOOTH)));
            }
        } catch (Exception e) { log.warn("Login logo not found."); }
        
        JLabel titleLabel = new JLabel("Property Owner Portal");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        headerPanel.add(logoLabel, "wrap");
        headerPanel.add(titleLabel, "wrap");
        add(headerPanel, "wrap, gapbottom 20");

        JPanel inputPanel = new JPanel(new MigLayout("insets 0", "[right]15[grow, fill]", "[]15[]"));
        
        JLabel userLbl = new JLabel("Username:"); userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JLabel passLbl = new JLabel("Password:"); passLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        inputPanel.add(userLbl); inputPanel.add(usernameField, "h 35!, wrap");
        inputPanel.add(passLbl); inputPanel.add(passwordField, "h 35!, wrap");
        
        add(inputPanel, "growx, wrap, gapbottom 10");

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        add(statusLabel, "wrap, gapbottom 15");

        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[grow, fill][grow, fill]", "[]10[]"));
        
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(230, 126, 34));
        loginButton.setForeground(Color.WHITE);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        registerButton = new JButton("Register");
        cancelButton = new JButton("Cancel");
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        loginButton.addActionListener(e -> performLogin());
        registerButton.addActionListener(e -> performRegistration());
        cancelButton.addActionListener(e -> { loginSuccessful = false; dispose(); });

        getRootPane().setDefaultButton(loginButton);
        
        buttonPanel.add(loginButton, "span 2, h 40!, wrap");
        buttonPanel.add(registerButton, "h 32!");
        buttonPanel.add(cancelButton, "h 32!");
        
        add(buttonPanel, "growx");
    }

    private void performLogin() {
        if (dbHelper == null) { setStatus("Internal Database Error.", Color.RED); return; }
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
             setStatus("Please enter both username and password.", Color.RED); return;
        }

        if (dbHelper.checkLogin(username, password)) {
            loginSuccessful = true; loggedInUsername = username;
            setStatus("Login successful! Redirecting...", new Color(46, 204, 113));
            Timer timer = new Timer(600, ae -> dispose()); timer.setRepeats(false); timer.start();
        } else {
            loginSuccessful = false; loggedInUsername = null;
            setStatus("Invalid username or password.", Color.RED);
            passwordField.setText("");
        }
    }

    private void performRegistration() {
        if (dbHelper == null) { setStatus("Internal Database Error.", Color.RED); return; }
        loginSuccessful = false; loggedInUsername = null;
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
             setStatus("Please enter username and password to register.", Color.RED); return;
        }

        if (dbHelper.registerUser(username, password)) {
            setStatus("Registration successful! Please login.", new Color(52, 152, 219));
            usernameField.setText(""); passwordField.setText(""); usernameField.requestFocusInWindow();
        } else {
            setStatus("Registration failed (Username might exist).", Color.RED);
        }
    }
    
    private void setStatus(String msg, Color color) { statusLabel.setText(msg); statusLabel.setForeground(color); }
    public boolean isLoginSuccessful() { return loginSuccessful; }
    public String getLoggedInUsername() { return loginSuccessful ? loggedInUsername : null; }
}