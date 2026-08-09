package javaH;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.BiConsumer;

/**
 * Super Enhanced Modern Chat Dialog.
 * Supports Dynamic Parent Dark/Light mode inheritance, rounded bubbles, and professional UI layout.
 */
public class ChatDialog extends JDialog {

    private static final Logger log = LoggerFactory.getLogger(ChatDialog.class);

    private JPanel chatContainer;
    private JScrollPane scrollPane;
    private JTextField inputField;
    private JButton sendBtn, acceptBtn, cancelBtn, counterBtn;
    private JSpinner priceSpn, durationSpn;
    private JLabel messageCounterLabel;

    private final BiConsumer<String, String> actionCallback;
    private final String conversationId;
    private final int homeId;
    private final String peerName;
    private final int messageLimit;
    private final boolean isOwnerView;


    private Color getBgColor() { 
        if (getOwner() != null && getOwner() instanceof JFrame) {
            return ((JFrame) getOwner()).getContentPane().getBackground();
        }
        return UIManager.getColor("Panel.background"); 
    }

    private boolean isDarkMode() {
        Color bg = getBgColor();
        if (bg == null) return false;
        int brightness = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return brightness < 128;
    }

    private Color getChatBgColor() { 
        return isDarkMode() ? new Color(43, 45, 48) : new Color(245, 245, 245); 
    }
    
    private Color getTextColor() { 
        return isDarkMode() ? new Color(230, 230, 230) : new Color(30, 30, 30); 
    }
    
    private Color getSystemMsgColor() { 
        return isDarkMode() ? new Color(150, 150, 150) : new Color(100, 100, 100); 
    }
    
    private Color getPeerBubbleColor() {
        return isDarkMode() ? new Color(60, 63, 65) : new Color(230, 230, 230);
    }
    
    private Color getPeerTextColor() {
        return getTextColor();
    }
    

    private final Color selfBubbleColor = new Color(41, 128, 185);
    private final Color selfTextColor = Color.WHITE;

    public ChatDialog(Window owner, String title, boolean modal, String conversationId,
            int homeId, String peerName, BiConsumer<String, String> callback, 
            int messageLimit, int initialPrice, int initialDuration) {

        super(owner, title, modal ? ModalityType.APPLICATION_MODAL : ModalityType.MODELESS);
        
        this.actionCallback = callback;
        this.conversationId = conversationId;
        this.homeId = homeId;
        this.peerName = peerName.replace("owner_", "").replace("customer_", ""); 
        this.messageLimit = messageLimit;
        this.isOwnerView = messageLimit <= 0;
        
        setTitle("Chat with " + this.peerName + " (Home ID: " + homeId + ")");
        
        setBackground(getBgColor());

        getRootPane().putClientProperty("JRootPane.titleBarBackground", getBgColor());
        getRootPane().putClientProperty("JRootPane.titleBarForeground", getTextColor());
        
        initComponents(initialPrice, initialDuration);
        setupLayout();
        
        setSize(500, 700);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        if (!isOwnerView && messageLimit > 0) {
          updateMessageCounter(0);
        }
    }

    private void initComponents(int initialPrice, int initialDuration) {
        chatContainer = new JPanel(new MigLayout("fillx, wrap 1, insets 15", "[grow]", ""));
        chatContainer.setBackground(getChatBgColor());


        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.putClientProperty("JTextField.placeholderText", "Type a message...");
        inputField.setBackground(isDarkMode() ? new Color(69, 73, 74) : Color.WHITE);
        inputField.setForeground(getTextColor());
        inputField.setCaretColor(getTextColor());
        inputField.addActionListener(e -> sendMessageAction());

        sendBtn = new JButton("Send");
        styleButton(sendBtn, new Color(41, 128, 185));
        sendBtn.addActionListener(e -> sendMessageAction());


        if (isOwnerView) {
            acceptBtn = new JButton("Accept Offer");
            styleButton(acceptBtn, new Color(46, 204, 113));
            acceptBtn.addActionListener(e -> markAsRentedAction());

            cancelBtn = new JButton("End Negotiation");
            styleButton(cancelBtn, new Color(231, 76, 60));
            cancelBtn.addActionListener(e -> cancelNegotiationAction());
        } else {
            priceSpn = new JSpinner(new SpinnerNumberModel(Math.max(initialPrice, 100), 100, 1000000, 50));
            durationSpn = new JSpinner(new SpinnerNumberModel(Math.max(initialDuration, 1), 1, 120, 1));
            
            applySpinnerTheme(priceSpn);
            applySpinnerTheme(durationSpn);
            
            counterBtn = new JButton("Propose Counter-Offer");
            styleButton(counterBtn, new Color(243, 156, 18));
            counterBtn.addActionListener(e -> sendCounterOfferAction(initialPrice, initialDuration));

            if (messageLimit > 0) {
                messageCounterLabel = new JLabel("Messages left: " + messageLimit);
                messageCounterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
                messageCounterLabel.setHorizontalAlignment(SwingConstants.CENTER);
                messageCounterLabel.setForeground(getTextColor());
            }
        }
    }
    

    private void applySpinnerTheme(JSpinner spinner) {
        boolean isDark = isDarkMode();
        Color bg = isDark ? new Color(69, 73, 74) : Color.WHITE;
        Color fg = isDark ? getTextColor() : Color.BLACK;
        Color arrowBtnBg = isDark ? new Color(80, 84, 85) : new Color(230, 230, 230); 
        Color borderColor = isDark ? new Color(80, 80, 80) : new Color(200, 200, 200);


        spinner.setUI(new BasicSpinnerUI());
        spinner.setBorder(BorderFactory.createLineBorder(borderColor));
        spinner.setBackground(bg);
        spinner.setForeground(fg);

        for (Component c : spinner.getComponents()) {
            if (c instanceof JButton) {
                c.setBackground(arrowBtnBg);
                c.setForeground(fg);

                ((JButton) c).setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            } else {
                c.setBackground(bg);
                c.setForeground(fg);
            }
        }

        if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            tf.setBackground(bg);
            tf.setForeground(fg);
            tf.setCaretColor(fg);
            tf.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        }
    }

    private void styleButton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 0", "[grow]", "[grow][shrink][shrink]"));
        mainPanel.setBackground(getBgColor());


        scrollPane = new JScrollPane(chatContainer);
        scrollPane.getViewport().setBackground(getChatBgColor()); 
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, isDarkMode() ? new Color(80, 80, 80) : new Color(200, 200, 200)));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(scrollPane, "grow, wrap");


        JPanel inputPanel = new JPanel(new MigLayout("fillx, insets 10", "[grow][shrink]", "[]"));
        inputPanel.setBackground(getBgColor());
        inputPanel.add(inputField, "growx, h 40!");
        inputPanel.add(sendBtn, "h 40!, w 80!");
        mainPanel.add(inputPanel, "growx, wrap");


        JPanel actionPanel = new JPanel(new MigLayout("fillx, insets 5 10 10 10", "[grow]", "[]"));
        actionPanel.setBackground(getBgColor());
        
        if (isOwnerView) {
            actionPanel.add(acceptBtn, "split 2, right, h 35!, w 120!");
            actionPanel.add(cancelBtn, "h 35!, w 140!");
        } else {
            JPanel counterPanel = new JPanel(new MigLayout("insets 5, center", "[][][][][]", "[]"));
            counterPanel.setOpaque(false);
            
            JLabel priceLbl = new JLabel("Price ($):"); priceLbl.setForeground(getTextColor());
            JLabel durLbl = new JLabel("Dur (m):"); durLbl.setForeground(getTextColor());
            
            counterPanel.add(priceLbl);
            counterPanel.add(priceSpn, "w 80!");
            counterPanel.add(durLbl, "gapleft 10");
            counterPanel.add(durationSpn, "w 60!");
            counterPanel.add(counterBtn, "h 35!, gapleft 10");
            
            actionPanel.add(counterPanel, "growx, wrap");
            if (messageCounterLabel != null) {
                actionPanel.add(messageCounterLabel, "growx");
            }
        }
        mainPanel.add(actionPanel, "growx");

        setContentPane(mainPanel);
    }

    private void sendMessageAction() {
        String msg = inputField.getText().trim();
        if (msg.isEmpty()) return;
        
        if (!isOwnerView && getRemainingMessages() <= 0) {
            appendSystemMessage("MESSAGE LIMIT REACHED. Cannot send more messages.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        
        try {
            actionCallback.accept("SEND_MSG", msg);
            appendMessage("You", msg, true);
            inputField.setText("");
        } catch (Exception ex) {
            log.error("Error sending message", ex);
            appendSystemMessage("[System Error] Send message failed.");
        }
    }

    private void sendCounterOfferAction(int initialPrice, int initialDuration) {
        if (!counterBtn.isEnabled() || (messageLimit > 0 && getRemainingMessages() <= 0)) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        int p = (Integer) priceSpn.getValue();
        int d = (Integer) durationSpn.getValue();

        if (p < initialPrice || d < initialDuration) {
            appendSystemMessage("[Offer Error] Cannot propose below initial ($" + initialPrice + " / " + initialDuration + "m).");
            return;
        }

        String offer = p + ";" + d;
        try {
            actionCallback.accept("SEND_COUNTER_OFFER", offer);
            appendMessage("You", String.format("Proposed New Offer: $%d / %d months", p, d), true);
        } catch (Exception ex) {
            log.error("Error sending counter offer", ex);
        }
    }

    private void markAsRentedAction() {
        if (JOptionPane.showConfirmDialog(this, "Accept offer and mark Home " + homeId + " as RENTED?", "Confirm Deal", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            acceptBtn.setEnabled(false);
            actionCallback.accept("MARK_RENTED", null);
        }
    }

    private void cancelNegotiationAction() {
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to END this negotiation?", "Confirm Cancel", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cancelBtn.setEnabled(false);
            actionCallback.accept("CANCEL_NEGOTIATION", null);
        }
    }

    public void updateMessageCounter(int messagesSentCount) {
        if (messageCounterLabel == null || isOwnerView) return;

        SwingUtilities.invokeLater(() -> {
            int remaining = Math.max(0, messageLimit - messagesSentCount);
            messageCounterLabel.setText("Messages left: " + remaining);
            messageCounterLabel.setForeground(remaining <= 2 ? new Color(231, 76, 60) : getTextColor());
            if (remaining <= 0) disableInput();
        });
    }

    public void disableInput() {
        SwingUtilities.invokeLater(() -> {
            if (inputField != null) inputField.setEnabled(false);
            if (sendBtn != null) sendBtn.setEnabled(false);
            if (counterBtn != null) counterBtn.setEnabled(false);
            if (acceptBtn != null) acceptBtn.setEnabled(false);
            if (cancelBtn != null) cancelBtn.setEnabled(false);
        });
    }

    public void appendMessage(String sender, String message) {
        appendMessage(sender, message, "You".equalsIgnoreCase(sender));
    }

    public void appendMessage(String sender, String message, boolean isSelf) {
        SwingUtilities.invokeLater(() -> {
            JPanel bubble = new JPanel(new BorderLayout());
            bubble.setOpaque(false);
            bubble.setBorder(new BubbleBorder(isSelf ? selfBubbleColor : getPeerBubbleColor(), isSelf));

            JLabel msgLabel = new JLabel("<html><div style='font-family: Segoe UI; font-size: 13px;'>" + message.replace("\n", "<br>") + "</div></html>");
            msgLabel.setForeground(isSelf ? selfTextColor : getPeerTextColor());
            msgLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
            
            if (!isSelf) {
                JLabel nameLabel = new JLabel(peerName);
                nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
                nameLabel.setForeground(new Color(150, 150, 150));
                nameLabel.setBorder(new EmptyBorder(4, 12, 0, 12));
                bubble.add(nameLabel, BorderLayout.NORTH);
            }

            bubble.add(msgLabel, BorderLayout.CENTER);
            
            String constraints = isSelf ? "align right, wmax 75%" : "align left, wmax 75%";
            chatContainer.add(bubble, constraints);
            
            chatContainer.revalidate();
            chatContainer.repaint();
            
            scrollToBottom();
        });
    }

    public void appendSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JLabel sysLbl = new JLabel(message, SwingConstants.CENTER);
            sysLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            sysLbl.setForeground(getSystemMsgColor());
            sysLbl.setBorder(new EmptyBorder(10, 5, 10, 5));

            chatContainer.add(sysLbl, "align center");
            chatContainer.revalidate();
            chatContainer.repaint();
            
            scrollToBottom();
        });
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            Timer timer = new Timer(50, e -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    private int getRemainingMessages() {
        if (isOwnerView || messageCounterLabel == null) return 999;
        try {
            String txt = messageCounterLabel.getText();
            return Integer.parseInt(txt.substring(txt.lastIndexOf(':') + 1).trim());
        } catch (Exception e) { return 0; }
    }

    public String getConversationId() { return conversationId; }
    public int getHomeId() { return homeId; }
    public String getPeerAgentName() { return peerName; }
    
    public void display() {
        if (!isVisible()) SwingUtilities.invokeLater(() -> setVisible(true));
        else SwingUtilities.invokeLater(() -> { toFront(); requestFocus(); inputField.requestFocusInWindow(); });
    }
    
    public void closeDialog() {
        if (isVisible()) SwingUtilities.invokeLater(this::dispose);
    }

    private static class BubbleBorder extends AbstractBorder {
        private final Color color;
        private final boolean isSelf;
        private final int radius = 16;

        public BubbleBorder(Color color, boolean isSelf) {
            this.color = color;
            this.isSelf = isSelf;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            
            g2.fill(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));
            
            if (isSelf) {
                g2.fillRect(x + width - radius - 1, y + height - radius - 1, radius, radius);
            } else {
                g2.fillRect(x, y, radius, radius);
            }
            
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }
    }
}