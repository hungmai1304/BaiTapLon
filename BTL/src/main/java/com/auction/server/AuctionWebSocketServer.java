package com.auction.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import com.google.gson.Gson;
import com.auction.common.model.Product;

public class AuctionWebSocketServer extends WebSocketServer {

    private Gson gson = new Gson();

    // 🔥 ĐÂY LÀ "DATABASE TẠM THỜI" CỦA CHÚNG TA
    private Product currentProduct;

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));

        // Khởi tạo một sản phẩm mẫu khi Server vừa bật lên
        currentProduct = new Product();
        // Giả sử class Product của bạn có các hàm set này, hãy sửa lại cho khớp với class của bạn nhé
        currentProduct.setId("P001");
        currentProduct.setName("Laptop Gaming Siêu Cấp");
        currentProduct.setCurrentPrice(100000); // Giá khởi điểm: 100k
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("🟢 Có người vào phòng: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📩 Nhận lệnh: " + message);

        if (message.equals("GET_CURRENT")) {
            // Khi có người mới vào, gửi giá trị của "Database tạm" cho họ
            String jsonResponse = gson.toJson(currentProduct);
            conn.send(jsonResponse);
        }
        else if (message.startsWith("BID:")) {
            try {
                // Tách lấy số tiền từ chuỗi "BID:500000"
                double bidAmount = Double.parseDouble(message.split(":")[1]);

                // 🔥 LOGIC KIỂM TRA TRẢ GIÁ: Giá mới phải lớn hơn giá hiện tại
                if (bidAmount > currentProduct.getCurrentPrice()) {

                    // 1. Cập nhật giá mới vào "Database tạm"
                    currentProduct.setCurrentPrice(bidAmount);

                    // 2. Chuyển thành JSON
                    String jsonToPush = gson.toJson(currentProduct);

                    // 3. 📢 PHÁT LOA CHO TẤT CẢ MỌI NGƯỜI ĐANG XEM
                    broadcast(jsonToPush);
                    System.out.println("🚀 Đã có người trả giá mới: " + bidAmount);

                } else {
                    // Nếu trả giá thấp hơn hoặc bằng, báo lỗi cho riêng người đó
                    conn.send("ERROR:Giá trả phải lớn hơn " + currentProduct.getCurrentPrice());
                }
            } catch (Exception e) {
                conn.send("ERROR:Lệnh trả giá không hợp lệ!");
            }
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("🔴 Client đã thoát: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Lỗi WebSocket: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket Server Real-time đã sẵn sàng!");
    }
}