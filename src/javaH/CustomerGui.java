package javaH;

import net.miginfocom.swing.MigLayout;
import jade.gui.GuiEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomerGui extends JFrame {
    
    private final CustomerAgent myAgent;
    private final String username;
    private final DatabaseHelper dbHelper;

    private JTable availableHomesTable;
    private DefaultTableModel tableModel;
    private HomeDetailsPanel homeDetailsPanel;
    
    private JPanel offerPanel;
    private JSpinner priceSpn, durationSpn;
    private JButton submitOfferBtn, cancelOfferBtn, refreshBtn;
    

    private JPanel actionBtnPanel;
    private CardLayout cardLayout;

    private List<HomeData> currentHomes = new ArrayList<>();
    private String selectedOwner = null;
    private int selectedHomeId = -1;

    public CustomerGui(CustomerAgent agent, String username, DatabaseHelper dbHelper) {
        super("Customer Dashboard: " + username);
        this.myAgent = agent;
        this.username = username;
        this.dbHelper = dbHelper;
        
        initComponents();
        setupLayout();
        
        setSize(1004, 816);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override 
            public void windowClosed(java.awt.event.WindowEvent e) { 
                if (myAgent != null) myAgent.doDelete(); 
            }
        });
    }

    private void initComponents() {
        String[] cols = {"ID", "Status", "Owner", "Address"};
        tableModel = new DefaultTableModel(cols, 0) { 
            @Override 
            public boolean isCellEditable(int r, int c) { return false; } 
        };
        availableHomesTable = new JTable(tableModel);
        
        availableHomesTable.setAutoCreateRowSorter(true);
        availableHomesTable.getRowSorter().toggleSortOrder(0);
        
        availableHomesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        availableHomesTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        availableHomesTable.getColumnModel().getColumn(1).setMaxWidth(90);
        availableHomesTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        availableHomesTable.getColumnModel().getColumn(2).setMaxWidth(90);
        availableHomesTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        
        availableHomesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availableHomesTable.setRowHeight(25);
        availableHomesTable.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        availableHomesTable.setRowSorter(sorter);
        
        availableHomesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) updateSelection();
        });

        homeDetailsPanel = new HomeDetailsPanel();
        
        priceSpn = new JSpinner(new SpinnerNumberModel(500, 100, 500000, 50));
        durationSpn = new JSpinner(new SpinnerNumberModel(6, 1, 120, 1));
        
        submitOfferBtn = new JButton("Submit Initial Offer");
        submitOfferBtn.setBackground(new Color(41, 128, 185));
        submitOfferBtn.setForeground(Color.WHITE);
        submitOfferBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitOfferBtn.addActionListener(e -> submitOffer());

        cancelOfferBtn = new JButton("Cancel My Pending Offer");
        cancelOfferBtn.setBackground(new Color(231, 76, 60)); 
        cancelOfferBtn.setForeground(Color.WHITE);
        cancelOfferBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelOfferBtn.addActionListener(e -> cancelOfferAction());


        cardLayout = new CardLayout();
        actionBtnPanel = new JPanel(cardLayout);
        actionBtnPanel.setOpaque(false);
        actionBtnPanel.add(submitOfferBtn, "SUBMIT");
        actionBtnPanel.add(cancelOfferBtn, "CANCEL");

        refreshBtn = new JButton("Refresh List");
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> postGuiEvent(6));
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 10", "[grow]", "[grow][]"));
        
        JScrollPane tableScroll = new JScrollPane(availableHomesTable);
        javax.swing.border.TitledBorder tableBorder = BorderFactory.createTitledBorder("Available Properties");
        tableBorder.setTitleColor(UIManager.getColor("Label.foreground"));
        tableScroll.setBorder(tableBorder);
        tableScroll.setMinimumSize(new Dimension(300, 0));

        JPanel rightPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[][grow]"));
        rightPanel.add(homeDetailsPanel, "growx, wrap");

        offerPanel = new JPanel(new MigLayout("fillx, insets 10", "[][grow]", "[][][]"));
        javax.swing.border.TitledBorder offerBorder = BorderFactory.createTitledBorder("Make Your Initial Offer");
        offerBorder.setTitleColor(UIManager.getColor("Label.foreground"));
        offerPanel.setBorder(offerBorder);
        offerPanel.add(new JLabel("Offer Price ($):")); offerPanel.add(priceSpn, "growx, wrap");
        offerPanel.add(new JLabel("Duration (m):")); offerPanel.add(durationSpn, "growx, wrap");
        

        offerPanel.add(actionBtnPanel, "span 2, growx, h 40!");
        
        offerPanel.setVisible(false);
        rightPanel.add(offerPanel, "growx, top");
        
        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(null);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        rightScroll.setMinimumSize(new Dimension(450, 0));
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, rightScroll);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(6);
        splitPane.setDividerLocation(450);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        mainPanel.add(splitPane, "grow, wrap"); 
        mainPanel.add(refreshBtn, "right");
        setContentPane(mainPanel);
    }

    public void displayAvailableHomes(List<HomeData> homes) {
        SwingUtilities.invokeLater(() -> {

            int savedHomeId = this.selectedHomeId;
            
            this.currentHomes = homes != null ? homes : new ArrayList<>();
            tableModel.setRowCount(0);
            for (HomeData h : currentHomes) {
                tableModel.addRow(new Object[]{h.getId(), h.getStatus(), h.getOwnerUsername(), h.getAddress()});
            }
            

            boolean reselected = false;
            if (savedHomeId != -1) {
                for (int i = 0; i < currentHomes.size(); i++) {
                    if (currentHomes.get(i).getId() == savedHomeId) {
                        int viewRow = availableHomesTable.convertRowIndexToView(i);
                        availableHomesTable.setRowSelectionInterval(viewRow, viewRow);
                        reselected = true;
                        break;
                    }
                }
            }
            

            if (!reselected) {
                clearSelection();
            }
        });
    }

    public void updateSelection() {
        int row = availableHomesTable.getSelectedRow();
        if (row < 0) {
            offerPanel.setVisible(false);
            return;
        }
        
        int modelRow = availableHomesTable.convertRowIndexToModel(row);
        HomeData h = currentHomes.get(modelRow);
        
        this.selectedHomeId = h.getId();
        this.selectedOwner = h.getOwnerUsername();
        
        homeDetailsPanel.displayHomeDetails(h);
        
        boolean isAvailable = "Available".equalsIgnoreCase(h.getStatus());
        boolean hasOffer = dbHelper.hasPendingOffer(h.getId(), username);

        offerPanel.setVisible(isAvailable);
        
        if (isAvailable) {
            if (hasOffer) {

                cardLayout.show(actionBtnPanel, "CANCEL");
                cancelOfferBtn.setEnabled(true);
                priceSpn.setEnabled(false);
                durationSpn.setEnabled(false);
            } else {

                cardLayout.show(actionBtnPanel, "SUBMIT");
                submitOfferBtn.setEnabled(true);
                priceSpn.setEnabled(true);
                durationSpn.setEnabled(true);
                priceSpn.setValue(Math.max(100, h.getMinPrice()));
                durationSpn.setValue(Math.max(1, h.getMinDuration()));
            }
            postGuiEvent(7, selectedOwner, selectedHomeId);
        }
    }

    public void clearSelection() {
        availableHomesTable.clearSelection();
        homeDetailsPanel.clearDetails();
        offerPanel.setVisible(false);
        selectedHomeId = -1;
        selectedOwner = null;
    }

    private void submitOffer() {
        if (selectedHomeId <= 0 || selectedOwner == null) return;
        int p = (Integer) priceSpn.getValue();
        int d = (Integer) durationSpn.getValue();
        postGuiEvent(1, String.valueOf(p), String.valueOf(d));
        submitOfferBtn.setEnabled(false); 
    }

    private void cancelOfferAction() {
        if (selectedHomeId <= 0 || selectedOwner == null) return;
        postGuiEvent(12, selectedHomeId, selectedOwner);
        cancelOfferBtn.setEnabled(false); 
    }

    private void postGuiEvent(int type, Object... params) {
        GuiEvent ge = new GuiEvent(this, type);
        for (Object p : params) ge.addParameter(p);
        if (myAgent != null) myAgent.postGuiEvent(ge);
    }

    public void display() { 
        SwingUtilities.invokeLater(() -> setVisible(true)); 
    }
    
    class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value != null ? value.toString() : "";
            if ("Available".equalsIgnoreCase(status)) {
                c.setForeground(new Color(46, 204, 113));
            } else if ("Rented".equalsIgnoreCase(status)) {
                c.setForeground(new Color(52, 152, 219));
            } else {
                c.setForeground(new Color(231, 76, 60));
            }
            if (!isSelected) {
                c.setBackground(table.getBackground());
            }
            return c;
        }
    }
}