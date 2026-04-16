package com.auction.server.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.print.attribute.standard.Severity;

import com.auction.protocol.MessageType;
import com.auction.protocol.Request;
import com.auction.protocol.Response;
// Nhan vien xu ly du lieu dau vao
public class ClientHandler implements Runnable {
    private Socket clienSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    public ClientHandler(Socket clienSocket) {
        this.clienSocket=clienSocket;
    }
    @Override
    public void run() {
        try{
            out=new ObjectOutputStream(clienSocket.getOutputStream());
            in=new ObjectInputStream(clienSocket.getInputStream());
            while (true) {
                // Doc Request tu Client gui len
                Request request=(Request) in.readObject();
                switch (request.getType()) {
                    case LOGIN_REQUEST:
                        System.out.println("[SERVER] Dang xu ly yeu cau dang nhap...");
                        // TODO xu ly kiem tra tai khoan password
                        // Response một Class.Đại diện phản hồi từ server gửi về cho client 
                        Response logiResp=new Response(MessageType.LOGIN_REQUEST, true, "Dang nhap thanh cong", null);
                        out.writeObject(logiResp); // GuI TRA LAI Client
                        break;
                    case PLACE_BID_REQUEST:
                        System.out.println("SERVER Dang xu ly yeu cau dat gia");
                        // TODO ham kiem tra gia co hop le khong 
                        break;
                    default:
                        System.err.println("[SERVER] Nhan duoc lenh khong thanh cong.");
                        break;
                }
            }
        }
        catch (IOException | ClassCastException e) {
            System.out.println("Khach hang da ngat ket noi.");
        }
        finally {
            try {
                if (in!=null) in.close();
                if (out!=null) out.close();
                if (clienSocket!=null) clienSocket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}