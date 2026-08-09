package javaH;

public class NegotiationRequest {
    private String customerName;
    private int homeId;
    private String address;
    private int proposedPrice;
    private int proposedDuration;
    private String originalProposalConvId;

    public NegotiationRequest(String customerName, int homeId, String address, int proposedPrice, int proposedDuration, String originalProposalConvId) {
        this.customerName = customerName;
        this.homeId = homeId;
        this.address = address;
        this.proposedPrice = proposedPrice;
        this.proposedDuration = proposedDuration;
        this.originalProposalConvId = originalProposalConvId;
    }

    public String getAddress() {
        return address;
    }

    public String getCustomerName() {
        return customerName;
    }

    public int getHomeId() {
        return homeId;
    }

    public int getProposedPrice() {
        return proposedPrice;
    }

    public int getProposedDuration() {
        return proposedDuration;
    }

    public String getOriginalProposalConvId() {
        return originalProposalConvId;
    }
}