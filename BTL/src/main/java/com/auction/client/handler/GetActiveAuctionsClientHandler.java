package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.util.List;
@ResponseHandler(type = "GET_ACTIVE_AUCTIONS_RESPONSE")
public class GetActiveAuctionsClientHandler implements IClientHandler {

    private Gson gson = new Gson();

    @Override
    public void handle(Response response) {
        try {
            System.out.println(">>> [HANDLER MỚI] DATA TỪ SERVER: " + response.getData());

            // 1. LẤY ĐÚNG KEY TỪ SERVER ("auctionList")
            Object rawData = response.getData().get("auctionList");

            if (rawData != null) {
                // 2. Ép kiểu an toàn tuyệt đối bằng TypeToken
                String jsonString = gson.toJson(rawData);
                java.lang.reflect.Type listType = new TypeToken<List<Auction>>(){}.getType();
                List<Auction> auctionList = gson.fromJson(jsonString, listType);

                if (auctionList != null && !auctionList.isEmpty()) {
                    System.out.println(">>> BÓC THÀNH CÔNG: " + auctionList.size() + " phiên đấu giá!");

                    // 3. Cất vào kho chung
                    ClientContext.getInstance().setAuctionList(auctionList);

                    // 4. Gọi Giao diện (TikTok) cập nhật hình ảnh (Lấy chuẩn logic của Hùng)
                    Platform.runLater(() -> {
                        TikTokAuctionController tiktokCtrl = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                        if (tiktokCtrl != null) {
                            tiktokCtrl.updateUI(ClientContext.getInstance().getCurrentAuction());
                        }
                    });
                }
            } else {
                System.err.println(">>> [LỖI] Không tìm thấy key 'auctionList' trong Response. Cần check lại Server!");
            }
        } catch (Exception e) {
            System.err.println(">>> [LỖI PARSE GSON] " + e.getMessage());
            e.printStackTrace();
        }
    }
}