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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class CustomerAgent extends GuiAgent {

    private static final Logger log = LoggerFactory.getLogger(CustomerAgent.class);

    private static class NegotiationContext {
        final String convId; final AID ownerAID; final int homeId;
        int price, duration, messagesSent; boolean active = true;
        NegotiationContext(String convId, AID ownerAID, int homeId, int price, int duration) {
            this.convId = convId; this.ownerAID = ownerAID; this.homeId = homeId;
            this.price = price; this.duration = duration; this.messagesSent = 0;
        }
    }

    private CustomerGui myGui;
    private String loggedInUsername;
    private DatabaseHelper ownerDbHelper;
    private final Map<String, NegotiationContext> activeNegotiations = new ConcurrentHashMap<>();
    private final Map<String, ChatDialog> activeChats = new ConcurrentHashMap<>();
    
    private AID selectedOwnerAID;
    private String selectedOwnerName = null; 
    private int selectedHomeId = -1;
    private static final int MSG_LIMIT = 5;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length == 1) {
            loggedInUsername = ((String) args[0]).trim();
        } else { 
            doDelete(); 
            return; 
        }

        try { 
            ownerDbHelper = new DatabaseHelper("localhost", 3306, "owner_auth_db", "root", ""); 
        } catch (Exception e) { 
            doDelete(); 
            return; 
        }

        myGui = new CustomerGui(this, loggedInUsername, ownerDbHelper);
        myGui.display();

        addBehaviour(new ReplyReceiver());
        addBehaviour(new OneShotBehaviour() { 
            @Override public void action() { refreshHomes(); } 
        });
    }

    @Override
    protected void onGuiEvent(GuiEvent ev) {
        switch (ev.getType()) {
            case 1: sendInitialOffer(ev); break;
            case 6: refreshHomes(); break;
            case 7: setTargetSelection(ev); break;
            case 12: handleCancelOffer(ev); break;
        }
    }

    private void refreshHomes() {
        if (ownerDbHelper != null && myGui != null) {
            myGui.displayAvailableHomes(ownerDbHelper.getAllAvailableHomes());
        }
    }

    private void setTargetSelection(GuiEvent ev) {
        try {
            selectedOwnerName = (String) ev.getParameter(0);
            selectedHomeId = (Integer) ev.getParameter(1);
            if (selectedOwnerName != null && selectedHomeId > 0) {
                selectedOwnerAID = new AID("owner_" + selectedOwnerName.toLowerCase(), AID.ISLOCALNAME);
            }
        } catch (Exception e) {}
    }

    private void sendInitialOffer(GuiEvent ev) {
        if (selectedOwnerAID == null || selectedHomeId <= 0 || selectedOwnerName == null) return;
        try {
            int price = Integer.parseInt((String) ev.getParameter(0));
            int duration = Integer.parseInt((String) ev.getParameter(1));
            
            if (ownerDbHelper.saveOffer(selectedHomeId, loggedInUsername, selectedOwnerName, price, duration)) {
                
                String convId = "prop-" + loggedInUsername + "-" + selectedOwnerAID.getLocalName() + "-" + selectedHomeId + "-" + System.currentTimeMillis();
                NegotiationContext ctx = new NegotiationContext(convId, selectedOwnerAID, selectedHomeId, price, duration);
                activeNegotiations.put(convId, ctx);
                
                ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
                cfp.addReceiver(selectedOwnerAID);
                cfp.setContent(selectedHomeId + ";" + price + ";" + duration);
                cfp.setConversationId(convId);
                cfp.setOntology("rental-negotiation");
                send(cfp);
                
                SwingUtilities.invokeLater(() -> myGui.updateSelection());
            } else {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(myGui, "Error: Offer might already exist.");
                    myGui.updateSelection();
                });
            }
        } catch (Exception e) {}
    }

    private void handleCancelOffer(GuiEvent ev) {
        int homeId = (Integer) ev.getParameter(0);
        String ownerName = (String) ev.getParameter(1);
        
        if (ownerDbHelper.cancelOffer(homeId, loggedInUsername)) {
            ACLMessage cancelMsg = new ACLMessage(ACLMessage.CANCEL);
            cancelMsg.addReceiver(new AID("owner_" + ownerName.toLowerCase(), AID.ISLOCALNAME));
            send(cancelMsg);
            
            SwingUtilities.invokeLater(() -> myGui.updateSelection());
        }
    }

    private void handleChatAction(String type, String content, ChatDialog dialog) {
        String convId = dialog.getConversationId();
        NegotiationContext ctx = activeNegotiations.get(convId);
        if (ctx == null || !ctx.active) return;
        
        if (ctx.messagesSent >= MSG_LIMIT) { 
            dialog.appendSystemMessage("LIMIT REACHED."); 
            dialog.disableInput(); 
            return; 
        }

        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(ctx.ownerAID);
            msg.setConversationId(convId);
            
            if ("SEND_MSG".equals(type)) { 
                msg.setOntology("chat-message"); 
                msg.setContent(content); 
            } else if ("SEND_COUNTER_OFFER".equals(type)) {
                msg.setOntology("counter-offer"); 
                msg.setContent(content);
                String[] p = content.split(";"); 
                ctx.price = Integer.parseInt(p[0]); 
                ctx.duration = Integer.parseInt(p[1]);
            }
            send(msg); 
            ctx.messagesSent++; 
            dialog.updateMessageCounter(ctx.messagesSent);
        } catch (Exception e) {}
    }

    private class ReplyReceiver extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.AGREE), 
                    MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.REFUSE), MessageTemplate.MatchPerformative(ACLMessage.INFORM))));
            
            if (msg != null) {
                String convId = msg.getConversationId();
                NegotiationContext ctx = activeNegotiations.get(convId);

                if (ctx == null && msg.getPerformative() == ACLMessage.AGREE) {
                    try {
                        String[] parts = msg.getContent().trim().split(";");
                        int homeId = Integer.parseInt(parts[0]);
                        
                        int recoveredPrice = 0;
                        int recoveredDuration = 0;
                        

                        for (NegotiationContext existing : activeNegotiations.values()) {
                            if (existing.homeId == homeId) {
                                recoveredPrice = existing.price;
                                recoveredDuration = existing.duration;
                                break;
                            }
                        }
                        

                        if (recoveredPrice == 0 && parts.length >= 3) {
                            recoveredPrice = Integer.parseInt(parts[1]);
                            recoveredDuration = Integer.parseInt(parts[2]);
                        }
                        

                        if (recoveredPrice == 0 && ownerDbHelper != null) {
                            java.util.List<HomeData> homes = ownerDbHelper.getAllAvailableHomes();
                            if (homes != null) {
                                for(HomeData h : homes) {
                                    if (h.getId() == homeId) {
                                        recoveredPrice = h.getMinPrice();
                                        recoveredDuration = h.getMinDuration();
                                        break;
                                    }
                                }
                            }
                        }
                        
                        ctx = new NegotiationContext(convId, msg.getSender(), homeId, recoveredPrice, recoveredDuration);
                        activeNegotiations.put(convId, ctx);
                    } catch (Exception e) {
                        log.error("Could not rebuild context", e);
                    }
                }

                if (ctx == null || !ctx.active) return;

                final NegotiationContext finalCtx = ctx;

                SwingUtilities.invokeLater(() -> {
                    if (msg.getPerformative() == ACLMessage.AGREE) {
                        if (!activeChats.containsKey(convId)) {
                            BiConsumer<String, String> cb = (type, content) -> handleChatAction(type, content, activeChats.get(convId));
                            ChatDialog chat = new ChatDialog(myGui, "Chat with " + msg.getSender().getLocalName(), false, convId, finalCtx.homeId, msg.getSender().getLocalName(), cb, MSG_LIMIT, finalCtx.price, finalCtx.duration);
                            activeChats.put(convId, chat);
                            chat.addWindowListener(new WindowAdapter() { 
                                @Override public void windowClosed(WindowEvent e) { activeChats.remove(convId); } 
                            });
                            chat.display(); 
                            chat.appendSystemMessage("Negotiation started!");
                        }
                    } else if (msg.getPerformative() == ACLMessage.REFUSE) {
                        JOptionPane.showMessageDialog(myGui, "Offer REJECTED by owner: " + msg.getContent(), "Rejected", JOptionPane.WARNING_MESSAGE);
                        activeNegotiations.remove(convId);
                        myGui.updateSelection(); 
                    } else if (msg.getPerformative() == ACLMessage.INFORM) {
                        ChatDialog chat = activeChats.get(convId);
                        if (chat != null) {
                            if ("chat-message".equals(msg.getOntology())) {
                                chat.appendMessage(msg.getSender().getLocalName(), msg.getContent(), false);
                            } else if ("chat-system".equals(msg.getOntology())) {
                                chat.appendSystemMessage(msg.getContent());
                                
                                if (msg.getContent().toLowerCase().contains("rented") || msg.getContent().toLowerCase().contains("end")) { 
                                    chat.disableInput(); 
                                    finalCtx.active = false;
                                    refreshHomes();
                                }
                            }
                        }
                    }
                });
            } else {
                block();
            }
        }
    }

    @Override 
    protected void takeDown() { 
        SwingUtilities.invokeLater(() -> {
            activeChats.values().forEach(ChatDialog::closeDialog); 
            if (myGui != null) {
                myGui.dispose(); 
            }
        });
    }
}