package javaH;

import net.miginfocom.swing.MigLayout;
import jade.gui.GuiEvent;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class HomeOwnerGui extends JFrame implements ActionListener {

    private final HomeOwnerAgent myAgent;
    private final String username;

    private JTabbedPane tabbedPane;
    
    private JPanel requestsContainer;
    private int requestCount = 0; 

    private JTable myHomesTable;
    private DefaultTableModel myHomesModel;
    private EditHomePanel editHomePanel;
    private JButton refreshMyHomesBtn;
    private List<HomeData> myHomes = new ArrayList<>();

    private JTable marketplaceTable;
    private DefaultTableModel marketplaceModel;
    private HomeDetailsPanel marketplaceDetails;
    private JButton refreshMarketplaceBtn;
    private List<HomeData> allHomes = new ArrayList<>();

    public HomeOwnerGui(HomeOwnerAgent agent, String username) {
        super("Owner Dashboard: " + username);
        this.myAgent = agent;
        this.username = username;
        
        initComponents();
        setSize(1004, 816);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { if (myAgent != null) myAgent.doDelete(); }
        });
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Negotiation Requests", buildRequestsPanel());
        tabbedPane.addTab("Manage My Homes", buildManageHomesPanel());
        tabbedPane.addTab("Marketplace", buildMarketplacePanel());
        setContentPane(tabbedPane);
    }

    private JPanel buildRequestsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        requestsContainer = new JPanel(new MigLayout("fillx, wrap 1, insets 10", "[grow]", "[]"));
        JScrollPane scroll = new JScrollPane(requestsContainer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        mainPanel.add(scroll, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel buildManageHomesPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 10", "[grow]", "[grow][]"));
        String[] cols = {"ID", "Status", "Address"};
        myHomesModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        myHomesTable = new JTable(myHomesModel);
        
        myHomesTable.setAutoCreateRowSorter(true);
        myHomesTable.getRowSorter().toggleSortOrder(0);

        myHomesTable.getColumnModel().getColumn(0).setMaxWidth(50);
        myHomesTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        myHomesTable.getColumnModel().getColumn(1).setMaxWidth(90);
        myHomesTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        
        myHomesTable.setRowHeight(25);
        myHomesTable.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        myHomesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && myHomesTable.getSelectedRow() >= 0) editHomePanel.displayHomeDetails(myHomes.get(myHomesTable.convertRowIndexToModel(myHomesTable.getSelectedRow())));
        });
        
        editHomePanel = new EditHomePanel(myAgent, this);
        refreshMyHomesBtn = new JButton("Refresh My Homes");
        refreshMyHomesBtn.addActionListener(this); refreshMyHomesBtn.setActionCommand("REFRESH_OWNER_HOMES");
        refreshMyHomesBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JScrollPane tableScroll = new JScrollPane(myHomesTable);
        tableScroll.setMinimumSize(new Dimension(300, 0)); 
        
        JScrollPane editScroll = new JScrollPane(editHomePanel); 
        editScroll.setBorder(null); 
        editScroll.getVerticalScrollBar().setUnitIncrement(16);
        editScroll.setMinimumSize(new Dimension(450, 0)); 

        editScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        JSplitPane myHomesSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScroll, editScroll);
        myHomesSplit.setContinuousLayout(true);
        myHomesSplit.setDividerSize(6);
        myHomesSplit.setDividerLocation(400);
        myHomesSplit.setResizeWeight(0.5);
        myHomesSplit.setBorder(null);
        
        panel.add(myHomesSplit, "grow, wrap");
        panel.add(refreshMyHomesBtn, "left");
        return panel;
    }

    private JPanel buildMarketplacePanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 10", "[grow]", "[grow][]"));
        String[] cols = {"ID", "Status", "Owner", "Address"};
        marketplaceModel = new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        marketplaceTable = new JTable(marketplaceModel);
        
        marketplaceTable.setAutoCreateRowSorter(true);
        marketplaceTable.getRowSorter().toggleSortOrder(0);

        marketplaceTable.getColumnModel().getColumn(0).setMaxWidth(50);
        marketplaceTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        marketplaceTable.getColumnModel().getColumn(1).setMaxWidth(90);
        marketplaceTable.getColumnModel().getColumn(1).setPreferredWidth(85);
        marketplaceTable.getColumnModel().getColumn(2).setMaxWidth(90);
        marketplaceTable.getColumnModel().getColumn(2).setPreferredWidth(85);
        
        marketplaceTable.setRowHeight(25);
        marketplaceTable.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        marketplaceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && marketplaceTable.getSelectedRow() >= 0) marketplaceDetails.displayHomeDetails(allHomes.get(marketplaceTable.convertRowIndexToModel(marketplaceTable.getSelectedRow())));
        });
        
        marketplaceDetails = new HomeDetailsPanel();
        refreshMarketplaceBtn = new JButton("Refresh Marketplace");
        refreshMarketplaceBtn.addActionListener(this); refreshMarketplaceBtn.setActionCommand("REFRESH_MARKETPLACE");
        refreshMarketplaceBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        JScrollPane mTableScroll = new JScrollPane(marketplaceTable);
        mTableScroll.setMinimumSize(new Dimension(300, 0)); 

        JScrollPane detailsScroll = new JScrollPane(marketplaceDetails); 
        detailsScroll.setBorder(null); 
        detailsScroll.getVerticalScrollBar().setUnitIncrement(16);
        detailsScroll.setMinimumSize(new Dimension(450, 0)); 

        detailsScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        JSplitPane marketSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mTableScroll, detailsScroll);
        marketSplit.setContinuousLayout(true);
        marketSplit.setDividerSize(6);
        marketSplit.setDividerLocation(450);
        marketSplit.setResizeWeight(0.5);
        marketSplit.setBorder(null);
        
        panel.add(marketSplit, "grow, wrap");
        panel.add(refreshMarketplaceBtn, "left");
        return panel;
    }

    public void displayOwnerHomes(List<HomeData> homes) { SwingUtilities.invokeLater(() -> { myHomes = homes != null ? homes : new ArrayList<>(); myHomesModel.setRowCount(0); for (HomeData h : myHomes) myHomesModel.addRow(new Object[]{h.getId(), h.getStatus(), h.getAddress()}); }); }
    public void displayMarketplaceHomes(List<HomeData> homes) { SwingUtilities.invokeLater(() -> { allHomes = homes != null ? homes : new ArrayList<>(); marketplaceModel.setRowCount(0); for (HomeData h : allHomes) marketplaceModel.addRow(new Object[]{h.getId(), h.getStatus(), h.getOwnerUsername(), h.getAddress()}); }); }
    
    public void clearNegotiationRequests() { 
        SwingUtilities.invokeLater(() -> {
            requestsContainer.removeAll();
            requestCount = 0;
            updateTabBadge();
            requestsContainer.repaint();
        }); 
    }
    
    public void addNegotiationRequest(NegotiationRequest req) { 
        SwingUtilities.invokeLater(() -> {
            RequestCard card = new RequestCard(req, 
                e -> postGuiEvent(10, req.getCustomerName(), req.getHomeId()), 
                e -> postGuiEvent(11, req.getCustomerName(), req.getHomeId())  
            );
            requestsContainer.add(card, "growx, gapbottom 10");
            requestCount++;
            updateTabBadge();
            requestsContainer.revalidate();
            requestsContainer.repaint();
        }); 
    }

    private void updateTabBadge() {
        if (requestCount > 0) {
            tabbedPane.setTitleAt(0, "<html>Negotiation Requests <font color='#E74C3C'><b>(" + requestCount + ")</b></font></html>");
        } else {
            tabbedPane.setTitleAt(0, "Negotiation Requests");
        }
    }

    public void clearEditPanel() { SwingUtilities.invokeLater(editHomePanel::clearForm); }

    @Override public void actionPerformed(ActionEvent e) {
        if ("REFRESH_OWNER_HOMES".equals(e.getActionCommand())) postGuiEvent(5);
        else if ("REFRESH_MARKETPLACE".equals(e.getActionCommand())) postGuiEvent(8);
        else if ("refreshList".equals(e.getActionCommand())) clearEditPanel();
        else if ("clearSelection".equals(e.getActionCommand())) myHomesTable.clearSelection();
    }

    private void postGuiEvent(int type, Object... params) {
        GuiEvent ge = new GuiEvent(this, type);
        for (Object p : params) ge.addParameter(p);
        if (myAgent != null) myAgent.postGuiEvent(ge);
    }
    public void display() { SwingUtilities.invokeLater(() -> setVisible(true)); }

    class RequestCard extends JPanel {
        public RequestCard(NegotiationRequest r, ActionListener onAccept, ActionListener onReject) {
            setLayout(new MigLayout("fillx, insets 15", "[][grow][][]", "[]5[]"));
            
            setOpaque(true);
            

            Color bgColor = UIManager.getColor("Panel.background");
            

            int brightness = (bgColor.getRed() * 299 + bgColor.getGreen() * 587 + bgColor.getBlue() * 114) / 1000;
            
            if (brightness < 128) {

                setBackground(new Color(Math.min(bgColor.getRed() + 15, 255), 
                                        Math.min(bgColor.getGreen() + 15, 255), 
                                        Math.min(bgColor.getBlue() + 15, 255)));
            } else {

                setBackground(Color.WHITE);
            }
            

            Color borderColor = UIManager.getColor("Component.borderColor");
            if (borderColor == null) borderColor = Color.LIGHT_GRAY;
            
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1, true),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            JLabel iconLabel = new JLabel();
            java.net.URL imgURL = getClass().getResource("icons/customer_icon.png");

            if (imgURL != null) {
                ImageIcon originalIcon = new ImageIcon(imgURL);
                Image scaledImg = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
                iconLabel.setIcon(new ImageIcon(scaledImg));
            }
            
            add(iconLabel, "cell 0 0 1 2, aligny center, gapright 15");
            
            JLabel title = new JLabel("Offer from: " + r.getCustomerName());
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            add(title, "cell 1 0, wrap");
            
            JLabel details = new JLabel("<html>Home Address: " + r.getAddress() + "<br>Proposed Price: <font color='#3498DB'><b>$" + r.getProposedPrice() + "</b></font> for <b>" + r.getProposedDuration() + "</b> months</html>");
            details.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            add(details, "cell 1 1");

            JButton accept = new JButton("Accept & Chat");
            accept.setBackground(new Color(46, 204, 113));
            accept.setForeground(Color.WHITE);
            accept.setFocusPainted(false);
            accept.setCursor(new Cursor(Cursor.HAND_CURSOR));
            accept.addActionListener(onAccept);
            
            JButton reject = new JButton("Reject");
            reject.setBackground(new Color(231, 76, 60));
            reject.setForeground(Color.WHITE);
            reject.setFocusPainted(false);
            reject.setCursor(new Cursor(Cursor.HAND_CURSOR));
            reject.addActionListener(onReject);

            add(accept, "cell 2 0 1 2, h 35!");
            add(reject, "cell 3 0 1 2, h 35!");
        }
    }
    
    class StatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            String status = value != null ? value.toString() : "";
            if ("Available".equalsIgnoreCase(status)) c.setForeground(new Color(46, 204, 113));
            else if ("Rented".equalsIgnoreCase(status)) c.setForeground(new Color(52, 152, 219));
            else c.setForeground(new Color(231, 76, 60));
            if (!isSelected) c.setBackground(table.getBackground());
            return c;
        }
    }
}