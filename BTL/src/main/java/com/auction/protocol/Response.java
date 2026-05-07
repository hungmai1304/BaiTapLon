package com.auction.protocol;

import java.util.HashMap;
import java.util.Map;

public class Response {
    private String type;      // Loại tin nhắn
    private String status;    // Trạng thái
    private String message;   // Lời nhắn
    private Map<String, Object> data; // Cái túi để nhét thêm dữ liệu (tên, tuổi, số dư...)

    public Response(String type, String status, String message) {
        this.type = type;
        this.status = status;
        this.message = message;
        this.data = new HashMap<>(); // Khởi tạo luôn cái túi rỗng để không bị lỗi Null
    }

    // Cái cổng (Getter) để moi cái túi data ra nhét đồ vào
    public Map<String, Object> getData() {
        return data;
    }

    // Các hàm Get/Set cơ bản khác nếu cần
    public String getType() { return type; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
}