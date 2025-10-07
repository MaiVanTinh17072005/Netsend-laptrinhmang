package org.example.network;

import java.io.IOException;
import java.net.*;

public class NetSendReceiver implements Runnable {
    private final int unicastPort;
    private final int multicastPort;
    private final NetSendReceiver.OnMessageReceived listener;
    private boolean running = true;
    private MulticastSocket multicastSocket; // Thêm để giữ reference

    // Định nghĩa interface bên trong lớp
    public interface OnMessageReceived {
        void onReceived(String ip, String message);
    }

    public NetSendReceiver(int unicastPort, int multicastPort, OnMessageReceived listener) {
        this.unicastPort = unicastPort;
        this.multicastPort = multicastPort;
        this.listener = listener;
        // Tự động join group khi khởi tạo
        try {
            this.multicastSocket = new MulticastSocket(multicastPort);
            multicastSocket.setReuseAddress(true); // Cho phép tái sử dụng cổng
            InetAddress group = InetAddress.getByName("230.0.0.1");
            multicastSocket.joinGroup(group);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Thread unicastThread = new Thread(this::listenUnicast);
        Thread multicastThread = new Thread(this::listenMulticast);
        unicastThread.start();
        multicastThread.start();
        // Không cần join, để thread tự kết thúc khi running = false
    }

    private void listenUnicast() {
        try (DatagramSocket socket = new DatagramSocket(unicastPort)) {
            socket.setReuseAddress(true); // Cho phép tái sử dụng cổng
            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String senderIP = packet.getAddress().getHostAddress();
                listener.onReceived(senderIP, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenMulticast() {
        if (multicastSocket == null) return; // Đảm bảo socket đã được khởi tạo
        try {
            byte[] buffer = new byte[1024];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                multicastSocket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String senderIP = packet.getAddress().getHostAddress();
                listener.onReceived(senderIP, msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() throws IOException {
        running = false;
        if (multicastSocket != null && !multicastSocket.isClosed()) {
            multicastSocket.leaveGroup(InetAddress.getByName("230.0.0.1"));
            multicastSocket.close();
        }
    }
}