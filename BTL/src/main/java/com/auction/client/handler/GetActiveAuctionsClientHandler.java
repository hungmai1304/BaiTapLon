package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.common.model.auction.Auction;
import com.auction.common.utils.LocalDateTimeAdapter;
import com.auction.protocol.Response;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ResponseHandler(type = "GET_ACTIVE_AUCTIONS_RESPONSE")
public class GetActiveAuctionsClientHandler implements IClientHandler {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();


    @Override
    public void handle(Response response) {
        try {
            System.out.println(">>> [HANDLER] Nhận dữ liệu auctionList từ Server.");

            // 1. Lấy dữ liệu với key chuẩn "auctionList"
            Object rawData = response.getData().get("auctionList");

            if (rawData != null) {
                // 2. Parse JSON sang List Auction
                String jsonString = gson.toJson(rawData);
                java.lang.reflect.Type listType = new TypeToken<List<Auction>>(){}.getType();
                List<Auction> auctionList = gson.fromJson(jsonString, listType);

                if (auctionList != null && !auctionList.isEmpty()) {
                    // 3. Cập nhật vào danh sách QUẢN LÝ MỚI (Duy nhất) trong ClientContext
                    ClientContext.getInstance().setAuctionList(auctionList);

                    // 4. Cập nhật giao diện TikTok
                    Platform.runLater(() -> {
                        TikTokAuctionController tiktokCtrl = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                        if (tiktokCtrl != null) {
                            // Dùng renderCurrentAuction để nó tự lấy con trỏ currentIndex mới nhất
                            tiktokCtrl.renderCurrentAuction();
                        }
                    });
                }
            } else {
                System.err.println(">>> [LỖI] Server trả về data null cho key 'auctionList'");
            }
        } catch (Exception e) {
            System.err.println(">>> [LỖI PARSE] " + e.getMessage());
            e.printStackTrace();
        }
    }
}