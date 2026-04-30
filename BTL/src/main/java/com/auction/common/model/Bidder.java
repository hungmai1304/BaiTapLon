package com.auction.common.model;


/**
 * Bidder kế thừa từ User.
 * Thừa hưởng: id, createdAt, name, password.
 */
public class Bidder extends User {
    public enum BidderStatus {
        NORMAL,
        BLACKLISTED,
        BANNED
    }
    private String email;
    private BidderStatus status;

    public Bidder() {
        super();
        this.status = BidderStatus.NORMAL; // Mặc định là bình thường khi mới tạo
    }

    public Bidder(String name, String password, String email) {
        super(name, password); // Gọi constructor của User
        this.email = email;
        this.status = BidderStatus.NORMAL;
    }

    // Getter & Setter
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BidderStatus getStatus() {
        return status;
    }

    public void setStatus(BidderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Bidder{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}