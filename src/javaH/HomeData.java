package javaH;

public class HomeData {
    
    private int id;
    private String ownerUsername;
    private String address;
    private String description;
    private int minPrice;
    private int maxPrice;
    private int minDuration;
    private int maxDuration;
    private String status;
    private String imagePath;


    public HomeData() {
    }

    public HomeData(int id, String ownerUsername, String address, String description, 
                    int minPrice, int maxPrice, int minDuration, int maxDuration, 
                    String status, String imagePath) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.address = address;
        this.description = description;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minDuration = minDuration;
        this.maxDuration = maxDuration;
        this.status = status;
        this.imagePath = imagePath;
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    public int getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(int minDuration) {
        this.minDuration = minDuration;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}