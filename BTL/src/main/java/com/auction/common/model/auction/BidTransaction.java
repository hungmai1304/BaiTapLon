package com.auction.common.model.auction;

import com.auction.common.model.user.User;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L; 

    private String auctionId;        // ID của phiên đấu giá 
    private User bidder;             // Người vung tiền
    private double bidAmount;        // Số tiền đặt giá
    private LocalDateTime timestamp; // Thời gian bấm nút

    // Bắt buộc phải có constructor rỗng cho hệ thống
    public BidTransaction() {}

    public BidTransaction(String auctionId, User bidder, double bidAmount, LocalDateTime timestamp) {
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public User getBidder() {
        return bidder;
    }

    public void setBidder(User bidder) {
        this.bidder = bidder;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}