package com.auction.client.handler.market;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.controller.tiktok.TikTokAuctionController;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.Art;
import com.auction.common.model.product.Electronics;
import com.auction.common.model.product.Jewelry;
import com.auction.common.model.product.Vehicle;
import com.auction.common.model.product.Fashion;
import com.auction.common.model.product.Other;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

@ResponseHandler(type = "GET_ACTIVE_AUCTIONS_RESPONSE")
public class GetActiveAuctionsClientHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(GetActiveAuctionsClientHandler.class.getName());
    // 1. KHỞI TẠO NHÀ MÁY ĐA HÌNH (Dạy cho Client biết nhận diện từng mặt hàng)
    // ====================================================================================
    private static final RuntimeTypeAdapterFactory<Product> productAdapterFactory = RuntimeTypeAdapterFactory
            .of(Product.class, "category")
            .registerSubtype(Art.class, "Art")
            .registerSubtype(Electronics.class, "Electronics")
            .registerSubtype(Jewelry.class, "Jewelry")
            .registerSubtype(Vehicle.class, "Vehicles")
            .registerSubtype(Fashion.class, "Fashion")
            .registerSubtype(Other.class, "Other");
    // CẬP NHẬT: Bộ Gson an toàn đọc được cả chuỗi Local lẫn chuỗi có Múi giờ (+07:00)
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) -> {
                String dateStr = json.getAsString();
                try {
                    // Thử parse theo chuẩn chuỗi có múi giờ trước (Server mới)
                    return ZonedDateTime.parse(dateStr).toLocalDateTime();
                } catch (Exception e) {
                    // Nếu lỗi, fallback về parse chuỗi không múi giờ (Server cũ hoặc dữ liệu mẫu)
                    return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                }
            })
            //  Phải cắm cái nhà máy Đa hình vào Gson!
            .registerTypeAdapterFactory(productAdapterFactory)
            .create();

    @Override
    public void handle(Response response) {
        try {
            LOGGER.info(">>> [HANDLER] Nhận dữ liệu auctionList từ Server.");

            Object rawData = response.getData().get("auctionList");

            if (rawData != null) {
                String jsonString = gson.toJson(rawData);
                java.lang.reflect.Type listType = new TypeToken<List<Auction>>(){}.getType();
                List<Auction> auctionList = gson.fromJson(jsonString, listType);

                if (auctionList != null) {
                    ClientContext.getInstance().setAuctionList(auctionList);

                    Platform.runLater(() -> {
                        TikTokAuctionController tiktokCtrl = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                        if (tiktokCtrl != null) {
                            tiktokCtrl.renderCurrentAuction();
                        }

                        Object searchCtrl = ControllerRegistry.get("SearchController");
                        if (searchCtrl != null) {
                            try {
                                searchCtrl.getClass().getMethod("renderResults", List.class).invoke(searchCtrl, auctionList);
                            } catch (Exception e) {
                                LOGGER.severe("[Handler] Lỗi gọi renderResults ở SearchController: " + e.getMessage());
                            }
                        }
                    });
                }
            } else {
                LOGGER.severe(">>> [LỖI] Server trả về data null cho key 'auctionList'");
            }
        } catch (Exception e) {
            LOGGER.severe(">>> [LỖI PARSE] " + e.getMessage());
            e.printStackTrace();
        }
    }
}