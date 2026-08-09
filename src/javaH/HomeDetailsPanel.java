package javaH;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class HomeDetailsPanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(HomeDetailsPanel.class);

    private JLabel imageLabel;
    private JLabel addressLbl, ownerLbl, priceLbl, durationLbl, statusLbl;
    private JTextArea descArea;

    public HomeDetailsPanel() {
        initComponents();
        buildLayout();
        javax.swing.border.TitledBorder detailsBorder = BorderFactory.createTitledBorder("Property Details");
        detailsBorder.setTitleColor(UIManager.getColor("Label.foreground"));
        setBorder(detailsBorder);
    }

    private void initComponents() {
        imageLabel = new JLabel("Select a home to view details", SwingConstants.CENTER);
        imageLabel.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        imageLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        imageLabel.setForeground(Color.GRAY);

        imageLabel.setPreferredSize(new Dimension(450, 250));
        
        addressLbl = new JLabel("Address: ");
        ownerLbl = new JLabel("Owner: ");
        priceLbl = new JLabel("Price Range: ");
        durationLbl = new JLabel("Duration Range: ");
        statusLbl = new JLabel("Status: ");
        
        descArea = new JTextArea();
        descArea.setEditable(false);
        descArea.setLineWrap(true);
        descArea.setWrapStyleWord(true);
        descArea.setBackground(UIManager.getColor("Panel.background"));
        descArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descArea.setBorder(new EmptyBorder(5, 5, 5, 5));
    }

    private void buildLayout() {

        setLayout(new MigLayout("fillx, insets 15", "[grow, fill]", ""));
        
        JPanel imageContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        imageContainer.setOpaque(false);
        imageContainer.add(imageLabel);
        
        add(imageContainer, "wrap 15");
        
        add(addressLbl, "wrap, gapbottom 5");
        
        JScrollPane descScroll = new JScrollPane(descArea);
        descScroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        
        add(descScroll, "h 80!, wrap, gapbottom 10");
        
        add(ownerLbl, "wrap");
        add(priceLbl, "wrap");
        add(durationLbl, "wrap");
        add(statusLbl, "wrap");
    }
    
    public void displayHomeDetails(HomeData h) {
        SwingUtilities.invokeLater(() -> {
            if (h == null) { clearDetails(); return; }
            
            loadImage(h.getImagePath());
            
            addressLbl.setText("<html><b style='font-size:11px; font-family: Segoe UI;'>Address:</b> <span style='font-size:11px; font-family: Segoe UI;'>" + h.getAddress() + "</span></html>");
            descArea.setText(h.getDescription() != null && !h.getDescription().isEmpty() ? h.getDescription() : "No description available.");
            ownerLbl.setText("<html><b style='font-size:11px; font-family: Segoe UI;'>Owner:</b> <span style='font-size:11px; font-family: Segoe UI;'>" + h.getOwnerUsername() + "</span></html>");
            priceLbl.setText(String.format("<html><b style='font-size:11px; font-family: Segoe UI;'>Price:</b> <span style='font-size:11px; font-family: Segoe UI;'>$%,d - $%,d</span></html>", h.getMinPrice(), h.getMaxPrice()));
            durationLbl.setText(String.format("<html><b style='font-size:11px; font-family: Segoe UI;'>Duration:</b> <span style='font-size:11px; font-family: Segoe UI;'>%d - %d m</span></html>", h.getMinDuration(), h.getMaxDuration()));
            
            String color = h.getStatus().equals("Available") ? "#2ecc71" : h.getStatus().equals("Rented") ? "#3498db" : "#e74c3c";
            statusLbl.setText(String.format("<html><b style='font-size:11px; font-family: Segoe UI;'>Status:</b> <font color='%s' style='font-size:11px; font-family: Segoe UI;'>%s</font></html>", color, h.getStatus()));
        });
    }

    private void loadImage(String path) {
        imageLabel.setIcon(null);
        if (path == null || path.trim().isEmpty()) { 
            imageLabel.setText("No Image Provided"); 
            return; 
        }
        
        try {
            File f = new File(path);
            if (f.exists()) {
                ImageIcon ic = new ImageIcon(f.toURI().toURL());
                Image img = ic.getImage().getScaledInstance(450, 250, Image.SCALE_SMOOTH);
                imageLabel.setIcon(new ImageIcon(img));
                imageLabel.setText(null);
            } else { 
                imageLabel.setText("Image File Not Found"); 
            }
        } catch (Exception e) {
            log.error("Failed to load image: {}", path, e);
            imageLabel.setText("Image Load Error");
        }
    }

    public void clearDetails() {
        SwingUtilities.invokeLater(() -> {
            imageLabel.setIcon(null); imageLabel.setText("Select a home to view details");
            addressLbl.setText("Address: "); descArea.setText(""); ownerLbl.setText("Owner: ");
            priceLbl.setText("Price Range: "); durationLbl.setText("Duration Range: "); statusLbl.setText("Status: ");
        });
    }
}