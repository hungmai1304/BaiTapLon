package com.auction.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class NetworkClient {
    // địa chỉ server
    // đường dẫn nối server và client
    private static final String SERVER_URL = "wss://baitaplon-qegw.onrender.com";
    private static WebSocketClient webSocketClient;

    // Quy chuẩn, bất kì lớp nào muốn nhận về thông tin của server
    // đều phải cài đặt interface này
    public interface MessageListener {
        void onMessageReceived(String message);
    }
    // nhân vật hiện tại đang đợi yêu cầu từ server
    private static MessageListener currentListener;

    // khi đang đợi yêu cầu từ server, lưu nhân vật đang yêu cầu vào ram, sau có kết quả thì
    // trả về kết quả cho nhân vật đó
    public static void setListener(MessageListener listener) {
        currentListener = listener;
    }

    // check nếu đường nối đã mở thì thoát hàm bằng return
    // không thì tạo đường nối mới
    // vừa tạo 1 đường nối mới thì lập tức yêu cầu xin thông tin sản phầm(nên sửa)
    // nếu có tin nhắn từ server thì đấy về : nếu có nhân vật đang đợi thì gọi hàm xử lí tin nhắn của hắn
    // mất kết nối hoặc lỗi: in ra terminal
    // kết nối đường ống

    public static void connectAndKeepAlive() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            return; // Nếu đang mở rồi thì thôi không mở lại
        }

        try {
            webSocketClient = new WebSocketClient(new URI(SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("🟢 Đã thiết lập kết nối Real-time!");
                    // Vừa vào là xin thông tin sản phẩm luôn
                    send("GET_CURRENT");
                }

                @Override
                public void onMessage(String message) {
                    // CÓ TIN NHẮN TỪ SERVER -> Đẩy về cho Controller
                    if (currentListener != null) {
                        currentListener.onMessageReceived(message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("🔴 Mất kết nối: " + reason);
                    // Có thể thêm logic tự động kết nối lại ở đây nếu muốn
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("❌ Lỗi mạng: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // hàm cho các nhân vật gửi tin nhắn
    // nếu đường nối ổn định, send tin nhắn cho server
    // nếu không in lỗi
    public static void sendCommand(String command) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(command);
        } else {
            System.err.println("⚠️ Chưa kết nối mạng, không thể gửi lệnh!");
        }
    }
}