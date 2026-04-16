package com.auction.server.service;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    // Cổng PORT mặc địch để Server mở cửa đón khách
    private static final int PORT=8000;
    public static void main(String[] args) {
        System.out.println("=== He thong may chu BIDDING dang khoi dong ===");            
        try (ServerSocket serverSocket=new ServerSocket(PORT)) {
            // ServerSocket là lớp được dùng để tạo server lắng nghe các kết nối từ phía Client 
            System.out.println("Server dang lang nghe ket noi tai cong " + PORT);
            // Vong lap vo tan de servrer luon bat va chao don nguoi dung
            while (true) {
                // Lệnh accept() sẽ chặn luồng lại ở đây cho đến khi có một Client kết nối tới
                Socket clienSocket=serverSocket.accept();
                System.out.println("Co mot nguoi choi vua ket noi tu IP: " + clienSocket.getInetAddress());
                // Tao nhan vien xu ly va cho chay luong 
                ClientHandler handler=new ClientHandler(clienSocket);
                new Thread(handler).start();
                 
            }
        }
        catch (IOException e) {
            System.err.println("Errol: Loi he thong");
        }

    }
}