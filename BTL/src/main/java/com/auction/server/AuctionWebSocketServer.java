package com.auction.server;

import com.auction.server.model.ServerContext;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import com.google.gson.Gson;
import com.auction.common.model.product.Product;


// class cha có khả năng đẩy tin nhắn về cho nhiều nguoi khác
// có khả năng nhận tin nhắn
// có khả năng gửi dữ liệu realtime
// nhận client kết nối
public class AuctionWebSocketServer extends WebSocketServer {

    private Gson gson = new Gson();
    private final MessageDispatcher dispatcher;
    private Product currentProduct;
//-----------------------------------------------------------------------------------
    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        // Tạo túi đồ Context

        // Sửa đổi: Chuyển ServerContext sang Singleton để đảm bảo duy nhất 1 bộ nhớ dùng chung,
        // tránh việc khởi tạo nhiều instance gây sai lệch dữ liệu online/đấu giá.
        ServerContext context = ServerContext.getInstance();
        context.initData(this, currentProduct);

        // Khởi tạo Dispatcher (nó sẽ tự động đi quét toàn bộ dự án)
        this.dispatcher = new MessageDispatcher(gson, context);

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
    @Override
    public void onMessage(WebSocket conn, String message) {
        dispatcher.dispatch(conn, message);
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