package javaH;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;

public class MainGui extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(MainGui.class);
    
    private AgentContainer mainContainer;
    private boolean isDarkMode = true;
    private JButton themeToggleButton;
    private ImageIcon sunIcon;
    private ImageIcon moonIcon;

    private DatabaseHelper ownerDbHelper;
    private DatabaseHelper customerDbHelper;

    private static final int THEME_ICON_WIDTH = 107;
    private static final int THEME_ICON_HEIGHT = 49;

    private static final int LOGO_WIDTH = 220;
    private static final int LOGO_HEIGHT = 220;

    public MainGui() {
        super("Intelligent Home Rental System");

        initializeJade();
        initializeDatabaseHelpers();
        loadThemeIcons();

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 30 20 40 20", "[center]", "[top][center][bottom]"));

        themeToggleButton = new JButton();
        themeToggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themeToggleButton.setContentAreaFilled(false);
        themeToggleButton.setFocusPainted(false);
        themeToggleButton.setBorderPainted(false);
        updateThemeToggleButtonIcon(); 
        themeToggleButton.addActionListener(e -> toggleTheme());
        
        mainPanel.add(themeToggleButton, "align center, wrap, gapbottom 10");

        JPanel brandPanel = new JPanel(new MigLayout("insets 0, center", "[center]", "[]15[]5[]"));
        
        JLabel logoLabel = new JLabel();
        try {
            URL logoUrl = getClass().getResource("icons/logo.png");
            if (logoUrl != null) {
                Image img = new ImageIcon(logoUrl).getImage().getScaledInstance(LOGO_WIDTH, LOGO_HEIGHT, Image.SCALE_SMOOTH);
                logoLabel.setIcon(new ImageIcon(img));
            }
        } catch (Exception e) {
            log.warn("Error loading logo", e);
        }
        
        JLabel titleLabel = new JLabel("Welcome to HomeRent");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        
        JLabel subtitleLabel = new JLabel("Smart Multi-Agent Negotiation Platform");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitleLabel.setForeground(Color.GRAY);

        brandPanel.add(logoLabel, "wrap");
        brandPanel.add(titleLabel, "wrap");
        brandPanel.add(subtitleLabel, "wrap");
        
        mainPanel.add(brandPanel, "align center, wrap, gapbottom 40");

        JButton customerButton = createButton("Customer Portal", "icons/customer_icon.png");
        JButton ownerButton = createButton("Owner Portal", "icons/owner_icon.png");

        customerButton.addActionListener(e -> handleCustomerButtonClick());
        ownerButton.addActionListener(e -> handleOwnerButtonClick());

        JPanel buttonPanel = new JPanel(new MigLayout("insets 0", "[220!]25[220!]", "[55!]"));
        buttonPanel.setOpaque(false);
        buttonPanel.add(customerButton, "grow");
        buttonPanel.add(ownerButton, "grow");
        
        mainPanel.add(buttonPanel, "align center");

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(mainPanel, BorderLayout.CENTER);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(650, 600));
        pack();
        setLocationRelativeTo(null);
    }

     private void handleCustomerButtonClick() {
         if (!isPlatformReady() || customerDbHelper == null) {
              log.error("JADE platform or Customer DB not configured.");
              JOptionPane.showMessageDialog(this, "System initialization error.", "Error", JOptionPane.ERROR_MESSAGE);
              return;
         }
         CustomerLoginDialog loginDialog = new CustomerLoginDialog(this, customerDbHelper);
         loginDialog.setVisible(true);

         if (loginDialog.isLoginSuccessful()) {
             String username = loginDialog.getLoggedInUsername();
             if (username != null && !username.trim().isEmpty()) {
                 String agentName = "customer_" + username.trim().toLowerCase();
                 launchAgent(agentName, "javaH.CustomerAgent", new Object[] { username });
             }
         }
         loginDialog.dispose();
     }

     private void handleOwnerButtonClick() {
        if (!isPlatformReady() || ownerDbHelper == null) {
             log.error("JADE platform or Owner DB not configured.");
             JOptionPane.showMessageDialog(this, "System initialization error.", "Error", JOptionPane.ERROR_MESSAGE);
             return;
        }
        HomeOwnerLoginDialog loginDialog = new HomeOwnerLoginDialog(this, ownerDbHelper);
        loginDialog.setVisible(true);

        if (loginDialog.isLoginSuccessful()) {
            String username = loginDialog.getLoggedInUsername();
            if (username != null && !username.trim().isEmpty()) {
                String agentName = "owner_" + username.trim().toLowerCase();
                launchAgent(agentName, "javaH.HomeOwnerAgent", new Object[] { username });
            }
        }
        loginDialog.dispose();
     }

    private boolean isPlatformReady() { return mainContainer != null; }

    private void initializeDatabaseHelpers() {
        String dbHost = "localhost"; int dbPort = 3306; String dbUser = "root"; String dbPass = ""; 
        try {
            ownerDbHelper = new DatabaseHelper(dbHost, dbPort, "owner_auth_db", dbUser, dbPass);
            customerDbHelper = new DatabaseHelper(dbHost, dbPort, "customer_auth_db", dbUser, dbPass);
            log.info("Database pools initialized successfully.");
        } catch (Exception e) {
            log.error("FATAL: Failed to initialize database pools", e);
            JOptionPane.showMessageDialog(this, "Database connection error.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadThemeIcons() {
        try {
            URL sunUrl = getClass().getResource("icons/sun_icon.png");
            URL moonUrl = getClass().getResource("icons/moon_icon.png");
            if (sunUrl != null) sunIcon = new ImageIcon(new ImageIcon(sunUrl).getImage().getScaledInstance(THEME_ICON_WIDTH, THEME_ICON_HEIGHT, Image.SCALE_SMOOTH));
            if (moonUrl != null) moonIcon = new ImageIcon(new ImageIcon(moonUrl).getImage().getScaledInstance(THEME_ICON_WIDTH, THEME_ICON_HEIGHT, Image.SCALE_SMOOTH));
        } catch (Exception e) { log.warn("Error loading theme toggle icons", e); }
    }

    private void toggleTheme() {
       try {
           if (isDarkMode) { FlatLightLaf.setup(); isDarkMode = false; }
           else { FlatDarkLaf.setup(); isDarkMode = true; }
           SwingUtilities.updateComponentTreeUI(this);
           updateThemeToggleButtonIcon();
       } catch (Exception e) { log.error("Failed to toggle Look and Feel.", e); }
    }

    private void updateThemeToggleButtonIcon() {
        if (themeToggleButton != null) {
             themeToggleButton.setIcon(isDarkMode ? sunIcon : moonIcon);
             themeToggleButton.setToolTipText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");
             themeToggleButton.setText(null);
        }
    }

    private JButton createButton(String text, String iconFileName) {
      JButton button = new JButton(text);
      button.setFont(new Font("Segoe UI", Font.BOLD, 15));
      button.setCursor(new Cursor(Cursor.HAND_CURSOR));
      try {
          URL iconUrl = getClass().getResource(iconFileName);
          if (iconUrl != null) button.setIcon(new ImageIcon(new ImageIcon(iconUrl).getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
      } catch (Exception e) { log.warn("Icon not found: {}", iconFileName); }
      return button;
    }

    private void initializeJade() {
      Runtime rt = Runtime.instance();
      rt.setCloseVM(true);
      Profile profile = new ProfileImpl();
      profile.setParameter(Profile.GUI, "true");
      mainContainer = rt.createMainContainer(profile);
      if (mainContainer == null) {
          log.error("FATAL: Error creating JADE main container!");
          System.exit(1);
      }
      log.info("JADE platform started successfully.");
    }

    private void launchAgent(String agentName, String agentClassName, Object[] args) {
      try {
          AgentController agent = mainContainer.createNewAgent(agentName, agentClassName, args);
          agent.start();
          log.info("Launched agent: {}", agentName);
      } catch (Exception e) {
          log.error("Error launching agent: {}", agentName, e);
          JOptionPane.showMessageDialog(this, "Error launching agent: " + e.getMessage(), "Agent Error", JOptionPane.ERROR_MESSAGE);
      }
    }

    public static void main(String[] args) {
        try { FlatDarkLaf.setup(); } 
        catch (Exception e) { log.error("FlatLaf failed to initialize.", e); }
        SwingUtilities.invokeLater(() -> {
            MainGui mainGui = new MainGui();
            mainGui.setVisible(true);
        });
    }
}