package javaH;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class HomeOwnerAgent extends GuiAgent {

    private static final Logger log = LoggerFactory.getLogger(HomeOwnerAgent.class);
    private static final String IMG_DIR = "home_images";

    private HomeOwnerGui myGui;
    private String loggedInUsername;
    private DatabaseHelper dbHelper;
    
    private final Map<String, NegotiationRequest> incomingReqs = new ConcurrentHashMap<>();
    private final Map<String, ChatDialog> activeChats = new ConcurrentHashMap<>();

    public static final int GUI_EVENT_ADD_HOME = 4, GUI_EVENT_REFRESH_OWNER_HOMES = 5, GUI_EVENT_REFRESH_MARKETPLACE = 8;
    public static final int GUI_EVENT_ACCEPT_REQUEST = 10, GUI_EVENT_REJECT_REQUEST = 11;
    public static final int GUI_EVENT_UPDATE_HOME = 22, GUI_EVENT_DELETE_HOME = 23;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 1) loggedInUsername = ((String) args[0]).trim();
        else { doDelete(); return; }

        try {
            dbHelper = new DatabaseHelper("localhost", 3306, "owner_auth_db", "root", "");
            Files.createDirectories(Paths.get(IMG_DIR));
        } catch (Exception e) { doDelete(); return; }

        myGui = new HomeOwnerGui(this, loggedInUsername);
        myGui.display();

        addBehaviour(new ProposalAndCancelReceiver());
        addBehaviour(new ChatReceiver());
        addBehaviour(new OneShotBehaviour() { @Override public void action() { refreshAllViews(); } });
    }

    @Override
    protected void onGuiEvent(GuiEvent ev) {
        try {
            switch (ev.getType()) {
                case GUI_EVENT_ADD_HOME: handleAddHome(ev); break;
                case GUI_EVENT_UPDATE_HOME: handleUpdateHome(ev); break;
                case GUI_EVENT_DELETE_HOME: handleDeleteHome(ev); break;
                case GUI_EVENT_ACCEPT_REQUEST: handleAcceptReq(ev); break;
                case GUI_EVENT_REJECT_REQUEST: handleRejectReq(ev); break;
                case GUI_EVENT_REFRESH_OWNER_HOMES: 
                case GUI_EVENT_REFRESH_MARKETPLACE: refreshAllViews(); break;
            }
        } catch (Exception e) {}
    }

    private void refreshAllViews() {
        if (myGui != null && dbHelper != null) {
            myGui.displayOwnerHomes(dbHelper.getHomesForOwner(loggedInUsername));
            myGui.displayMarketplaceHomes(dbHelper.getAllAvailableHomes());
            loadOffersFromDB();
        }
    }

    private void loadOffersFromDB() {
        if (myGui == null || dbHelper == null) return;
        myGui.clearNegotiationRequests();
        incomingReqs.clear();
        
        List<NegotiationRequest> dbOffers = dbHelper.getPendingOffersForOwner(loggedInUsername);
        for (NegotiationRequest req : dbOffers) {
            incomingReqs.put(req.getCustomerName() + "-" + req.getHomeId(), req);
            myGui.addNegotiationRequest(req);
        }
    }

    private void handleAddHome(GuiEvent ev) {
        String addr = (String) ev.getParameter(0); String desc = (String) ev.getParameter(1);
        int minP = (Integer) ev.getParameter(2); int maxP = (Integer) ev.getParameter(3);
        int minD = (Integer) ev.getParameter(4); int maxD = (Integer) ev.getParameter(5);
        String rawImg = (String) ev.getParameter(6);
        String dbImgPath = copyImg(rawImg);
        if (dbHelper.addHome(loggedInUsername, addr, desc, minP, maxP, minD, maxD, dbImgPath)) {
            JOptionPane.showMessageDialog(myGui, "Home Added!"); refreshAllViews();
        }
    }

    private void handleUpdateHome(GuiEvent ev) {
        int id = (Integer) ev.getParameter(0); String addr = (String) ev.getParameter(1);
        String desc = (String) ev.getParameter(2); int minP = (Integer) ev.getParameter(3);
        int maxP = (Integer) ev.getParameter(4); int minD = (Integer) ev.getParameter(5);
        int maxD = (Integer) ev.getParameter(6); String stat = (String) ev.getParameter(7);
        String newImgRaw = (String) ev.getParameter(8); String oldImg = (String) ev.getParameter(9);
        String finalImg = oldImg;
        if (newImgRaw != null && !newImgRaw.trim().isEmpty()) { finalImg = copyImg(newImgRaw); if (oldImg != null) new File(oldImg).delete(); }
        if (dbHelper.updateHome(id, addr, desc, minP, maxP, minD, maxD, stat, finalImg)) { JOptionPane.showMessageDialog(myGui, "Update Success!"); refreshAllViews(); }
    }

    private void handleDeleteHome(GuiEvent ev) {
        int id = (Integer) ev.getParameter(0); String img = (String) ev.getParameter(1);
        if (dbHelper.deleteHome(id, img)) { JOptionPane.showMessageDialog(myGui, "Deleted!"); refreshAllViews(); }
    }

    private String copyImg(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) return null;
        try {
            File src = new File(rawPath);
            Path target = Paths.get(IMG_DIR, UUID.randomUUID() + src.getName().substring(src.getName().lastIndexOf('.')));
            Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString().replace('\\', '/');
        } catch (Exception e) { return null; }
    }

    private void handleAcceptReq(GuiEvent ev) {
        String cust = (String) ev.getParameter(0); int id = (Integer) ev.getParameter(1);
        NegotiationRequest r = incomingReqs.get(cust + "-" + id);
        if (r == null) return;
        
        dbHelper.updateOfferStatus(id, cust, "Accepted");
        loadOffersFromDB();
        
        String jadeCustomerName = "customer_" + cust.toLowerCase();
        
        ACLMessage msg = new ACLMessage(ACLMessage.AGREE);
        msg.addReceiver(new AID(jadeCustomerName, AID.ISLOCALNAME));
        msg.setConversationId(r.getOriginalProposalConvId());
        msg.setOntology("rental-negotiation");
        
        msg.setContent(String.valueOf(id)); 
        send(msg);

        openChat(cust, r.getOriginalProposalConvId(), id, r.getProposedPrice(), r.getProposedDuration());
    }


    private void handleRejectReq(GuiEvent ev) {
        String cust = (String) ev.getParameter(0); 
        int id = (Integer) ev.getParameter(1);
        

        NegotiationRequest r = incomingReqs.get(cust + "-" + id);
        
        dbHelper.updateOfferStatus(id, cust, "Rejected");
        loadOffersFromDB();
        
        String jadeCustomerName = "customer_" + cust.toLowerCase();
        ACLMessage msg = new ACLMessage(ACLMessage.REFUSE);
        msg.addReceiver(new AID(jadeCustomerName, AID.ISLOCALNAME));
        msg.setOntology("rental-negotiation");
        msg.setContent("Rejected by owner.");
        

        if (r != null && r.getOriginalProposalConvId() != null) {
            msg.setConversationId(r.getOriginalProposalConvId());
        }
        
        send(msg);
    }

    private void openChat(String cust, String convId, int id, int p, int d) {
        if (!activeChats.containsKey(cust)) {
            BiConsumer<String, String> cb = (type, content) -> handleOwnerChatAction(type, content, cust, id, convId);
            ChatDialog chat = new ChatDialog(myGui, "Chat with " + cust, false, convId, id, cust, cb, -1, p, d);
            activeChats.put(cust, chat);
            chat.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { activeChats.remove(cust); } });
            chat.display();
        }
    }

    private void handleOwnerChatAction(String type, String content, String cust, int id, String convId) {
        ChatDialog chat = activeChats.get(cust);
        if (chat == null) return;
        
        String jadeCustomerName = "customer_" + cust.toLowerCase();
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(new AID(jadeCustomerName, AID.ISLOCALNAME));
        msg.setConversationId(convId);
        
        if ("SEND_MSG".equals(type)) { 
            msg.setOntology("chat-message"); 
            msg.setContent(content); 
            send(msg); 
        } 
        else if ("MARK_RENTED".equals(type)) {
            dbHelper.updateHomeStatus(id, "Rented"); 
            refreshAllViews();
            msg.setOntology("chat-system"); 
            msg.setContent("Home RENTED. Session Ended."); 
            send(msg);
            chat.disableInput();
        } 
        else if ("CANCEL_NEGOTIATION".equals(type)) {
            msg.setOntology("chat-system"); 
            msg.setContent("Owner Cancelled."); 
            send(msg);
            chat.closeDialog();
        }
    }

    private class ProposalAndCancelReceiver extends CyclicBehaviour {
        @Override public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), MessageTemplate.MatchPerformative(ACLMessage.CANCEL)));
            if (msg != null) {
                loadOffersFromDB();
            } else {
                block();
            }
        }
    }

    private class ChatReceiver extends CyclicBehaviour {
        @Override public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            if (msg != null) {
                String cleanSender = msg.getSender().getLocalName().replace("customer_", "");
                ChatDialog chat = activeChats.get(cleanSender);
                if (chat != null) {
                    if ("chat-message".equals(msg.getOntology())) chat.appendMessage(msg.getSender().getLocalName(), msg.getContent(), false);
                    else if ("counter-offer".equals(msg.getOntology())) chat.appendSystemMessage("Counter Offer: " + msg.getContent().replace(';', ' '));
                }
            } else block();
        }
    }

    @Override 
    protected void takeDown() { 
        SwingUtilities.invokeLater(() -> {
            if (activeChats != null) {
                activeChats.values().forEach(ChatDialog::closeDialog);
            }
            if (myGui != null) {
                myGui.dispose(); 
            }
        });
    }
}