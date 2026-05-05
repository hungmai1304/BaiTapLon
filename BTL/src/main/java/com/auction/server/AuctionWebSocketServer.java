package com.auction.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import com.google.gson.Gson;
import com.auction.common.model.Product;


// class cha có khả năng đẩy tin nhắn về cho nhiều nguoi khác
// có khả năng nhận tin nhắn
// có khả năng gửi dữ liệu realtime
// nhận client kết nối
public class AuctionWebSocketServer extends WebSocketServer {

    private Gson gson = new Gson();


    private Product currentProduct;
//-----------------------------------------------------------------------------------
    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));


        currentProduct = new Product();

        currentProduct.setId("P001");
        currentProduct.setName("Laptop Gaming Siêu Cấp");
        currentProduct.setCurrentPrice(100000); // Giá khởi điểm: 100k
    }
    //-----------------------------------------------------------------------------------
    // chạy khi có ai đó kết nối
    // in ra có người vào phòng
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("🟢 Có người vào phòng: " + conn.getRemoteSocketAddress());
    }

//-----------------------------------------------------------------------------------
    // nhận tin nhắn
    // nếu client gửi get_current: chuyển product thành json rồi gửi
    // nếu gửi lệnh bid thì kiểm tra và in ra có người đấu giá mới, gửi đi cho tất cả các người đang online
    // if bidd is invalid : send error to player

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📩 Nhận lệnh: " + message);

        if (message.equals("GET_CURRENT")) {

            String jsonResponse = gson.toJson(currentProduct);
            conn.send(jsonResponse);
        }
        else if (message.startsWith("BID:")) {
            try {

                double bidAmount = Double.parseDouble(message.split(":")[1]);


                if (bidAmount > currentProduct.getCurrentPrice()) {


                    currentProduct.setCurrentPrice(bidAmount);


                    String jsonToPush = gson.toJson(currentProduct);


                    broadcast(jsonToPush);
                    System.out.println("🚀 Đã có người trả giá mới: " + bidAmount);

                } else {

                    conn.send("ERROR:Giá trả phải lớn hơn " + currentProduct.getCurrentPrice());
                }
            } catch (Exception e) {
                conn.send("ERROR:Lệnh trả giá không hợp lệ!");
            }
        }
    }




//-----------------------------------------------------------------------------------
    // client out: print client out

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("🔴 Client đã thoát: " + conn.getRemoteSocketAddress());
    }



//-----------------------------------------------------------------------------------
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Lỗi WebSocket: " + ex.getMessage());
    }
//-----------------------------------------------------------------------------------
    // when server done deploy
    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket Server Real-time đã sẵn sàng!");
    }
}