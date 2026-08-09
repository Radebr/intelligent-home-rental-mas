package javaH;

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jade.gui.GuiEvent;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URI;

public class EditHomePanel extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(EditHomePanel.class);
    
    private final HomeOwnerAgent agent;
    private final ActionListener refreshListener;
    
    private HomeData currentHome;
    private File selectedImage;
    private boolean isEditMode = false;

    private JTextField addressField;
    private JTextArea descArea;
    private JSpinner minPriceSpn, maxPriceSpn, minDurSpn, maxDurSpn;
    private JComboBox<String> statusCombo;
    private JButton imgBtn, removeImgBtn, saveBtn, deleteBtn, clearBtn;
    private JLabel imgStatusLbl, imgPreviewLbl;

    public EditHomePanel(HomeOwnerAgent agent, ActionListener listener) {
        this.agent = agent;
        this.refreshListener = listener;
        initComponents();
        buildLayout();
        setMode(false);
    }

    private void initComponents() {
        addressField = new JTextField();
        descArea = new JTextArea(4, 20); 
        descArea.setLineWrap(true); 
        descArea.setWrapStyleWord(true);
        
        minPriceSpn = new JSpinner(new SpinnerNumberModel(100, 100, 500000, 50));
        maxPriceSpn = new JSpinner(new SpinnerNumberModel(1000, 100, 500000, 50));
        minDurSpn = new JSpinner(new SpinnerNumberModel(1, 1, 120, 1));
        maxDurSpn = new JSpinner(new SpinnerNumberModel(12, 1, 120, 1));
        
        statusCombo = new JComboBox<>(new String[]{"Available", "Rented", "Unavailable"});
        
        imgBtn = new JButton("Browse...");
        imgBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        imgBtn.addActionListener(e -> selectImage());
        removeImgBtn = new JButton("Remove");
        removeImgBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeImgBtn.addActionListener(e -> { 
            selectedImage = null; 
            imgStatusLbl.setText("Removed"); 
            imgPreviewLbl.setIcon(null); 
        });
        
        imgStatusLbl = new JLabel("None");
        imgPreviewLbl = new JLabel("No Image", SwingConstants.CENTER);
        imgPreviewLbl.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1));
        
        saveBtn = new JButton("Save"); 
        saveBtn.setBackground(new Color(52, 152, 219)); 
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        deleteBtn = new JButton("Delete"); 
        deleteBtn.setBackground(new Color(231, 76, 60)); 
        deleteBtn.setForeground(Color.WHITE);
        deleteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        clearBtn = new JButton("+ New");
        clearBtn.setBackground(new Color(46, 204, 113));
        clearBtn.setForeground(Color.WHITE);
        clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        saveBtn.addActionListener(e -> saveAction());
        deleteBtn.addActionListener(e -> deleteAction());
        clearBtn.addActionListener(e -> {
            clearForm();
            if (refreshListener != null) {
                refreshListener.actionPerformed(new ActionEvent(this, 0, "clearSelection"));
            }
        });
    }

    private void buildLayout() {
        setLayout(new MigLayout("fillx, insets 20", "[right]10[grow,fill]20[right]10[grow,fill]", ""));
        setBorder(BorderFactory.createTitledBorder("Listing Details"));
        
        add(new JLabel("Address:")); 
        add(addressField, "span 3, wrap 12");
        
        add(new JLabel("Desc:")); 
        add(new JScrollPane(descArea), "span 3, h 60!, wrap 12");
        
        add(new JLabel("Min Price:")); add(minPriceSpn, "");
        add(new JLabel("Max Price:")); add(maxPriceSpn, "wrap 12");
        
        add(new JLabel("Min Dur:")); add(minDurSpn, "");
        add(new JLabel("Max Dur:")); add(maxDurSpn, "wrap 15");
        
        add(new JLabel("Status:")); 
        add(statusCombo, "span 3, wrap 15");
        
        add(new JLabel("Image:")); 
        add(imgBtn, "split 2"); 
        add(removeImgBtn); 
        add(imgStatusLbl, "span 2, wrap 15");
        
        imgPreviewLbl.setHorizontalAlignment(SwingConstants.CENTER);
        imgPreviewLbl.setVerticalAlignment(SwingConstants.CENTER);
        

        JPanel editImageContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        editImageContainer.setOpaque(false);
        editImageContainer.add(imgPreviewLbl);
        
        add(editImageContainer, "span, growx, wrap 20");
        
        JPanel btnPanel = new JPanel(new MigLayout("insets 0, center", "[]10[]10[]", "[]"));
        btnPanel.add(saveBtn, "w 90!, h 32!"); 
        btnPanel.add(deleteBtn, "w 90!, h 32!"); 
        btnPanel.add(clearBtn, "w 90!, h 32!");
        add(btnPanel, "span, growx");
    }
    
    private void selectImage() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImage = fc.getSelectedFile();
            imgStatusLbl.setText(selectedImage.getName());
            try {
                Image img = new ImageIcon(selectedImage.toURI().toURL()).getImage().getScaledInstance(400, 240, Image.SCALE_SMOOTH);
                imgPreviewLbl.setIcon(new ImageIcon(img));
                imgPreviewLbl.setText(null);
            } catch (Exception e) { 
                log.error("Preview failed", e); 
            }
        }
    }

    private void saveAction() {
        String adr = addressField.getText().trim();
        if (adr.isEmpty()) { 
            JOptionPane.showMessageDialog(this, "Address required."); 
            return; 
        }
        
        String imgPath = selectedImage != null ? selectedImage.getAbsolutePath() : 
            (isEditMode && currentHome != null && !imgStatusLbl.getText().equals("Removed") ? currentHome.getImagePath() : null);
        
        GuiEvent ge = new GuiEvent(this, isEditMode ? HomeOwnerAgent.GUI_EVENT_UPDATE_HOME : HomeOwnerAgent.GUI_EVENT_ADD_HOME);
        if (isEditMode) ge.addParameter(currentHome.getId());
        
        ge.addParameter(adr); 
        ge.addParameter(descArea.getText().trim());
        ge.addParameter(minPriceSpn.getValue()); 
        ge.addParameter(maxPriceSpn.getValue());
        ge.addParameter(minDurSpn.getValue()); 
        ge.addParameter(maxDurSpn.getValue());
        
        if (isEditMode) ge.addParameter(statusCombo.getSelectedItem());
        ge.addParameter(imgPath);
        if (isEditMode) ge.addParameter(currentHome.getImagePath());
        
        agent.postGuiEvent(ge);
        if (!isEditMode) clearForm();
    }

    private void deleteAction() {
        if (!isEditMode || currentHome == null) return;
        if (JOptionPane.showConfirmDialog(this, "Delete listing?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            GuiEvent ge = new GuiEvent(this, HomeOwnerAgent.GUI_EVENT_DELETE_HOME);
            ge.addParameter(currentHome.getId()); 
            ge.addParameter(currentHome.getImagePath());
            agent.postGuiEvent(ge);
            clearForm();
            refreshListener.actionPerformed(new ActionEvent(this, 0, "refreshList"));
        }
    }

    public void displayHomeDetails(HomeData h) {
        if (h == null) { 
            clearForm(); 
            return; 
        }
        currentHome = h;
        addressField.setText(h.getAddress()); 
        descArea.setText(h.getDescription());
        minPriceSpn.setValue(h.getMinPrice()); 
        maxPriceSpn.setValue(h.getMaxPrice());
        minDurSpn.setValue(h.getMinDuration()); 
        maxDurSpn.setValue(h.getMaxDuration());
        statusCombo.setSelectedItem(h.getStatus());
        
        if (h.getImagePath() != null && !h.getImagePath().trim().isEmpty()) {
            File f = new File(h.getImagePath());
            imgStatusLbl.setText(f.getName());
            try { 
                Image img = new ImageIcon(f.toURI().toURL()).getImage().getScaledInstance(400, 240, Image.SCALE_SMOOTH);
                imgPreviewLbl.setIcon(new ImageIcon(img)); 
                imgPreviewLbl.setText(null); 
            } catch (Exception e) {
                log.error("Failed to load image preview", e);
            }
        } else {
            imgPreviewLbl.setIcon(null);
            imgPreviewLbl.setText("No Image");
        }
        setMode(true);
    }

    public void clearForm() {
        currentHome = null; 
        selectedImage = null;
        addressField.setText(""); 
        descArea.setText("");
        minPriceSpn.setValue(100); 
        maxPriceSpn.setValue(1000); 
        minDurSpn.setValue(1); 
        maxDurSpn.setValue(12);
        statusCombo.setSelectedIndex(0);
        imgStatusLbl.setText("None"); 
        imgPreviewLbl.setIcon(null); 
        imgPreviewLbl.setText("No Image");
        setMode(false);
    }

    private void setMode(boolean edit) {
        isEditMode = edit;
        deleteBtn.setEnabled(edit);
        removeImgBtn.setEnabled(edit);
        statusCombo.setEnabled(edit);
        saveBtn.setText(edit ? "Update" : "Add");
        
        saveBtn.setBackground(new Color(52, 152, 219)); 
        
        setBorder(BorderFactory.createTitledBorder(edit ? "Edit Listing" : "New Listing"));
    }
}