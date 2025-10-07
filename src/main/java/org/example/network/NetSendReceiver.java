package org.example.network;

import java.net.*;

public class NetSendReceiver implements Runnable {
    private final int port;
    private final OnMessageReceived listener;
    private boolean running = true;

    public interface OnMessageReceived {
        void onReceived(String ip, String message);
    }

    public NetSendReceiver(int port, OnMessageReceived listener) {
        this.port = port;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            // Nhận Unicast & Broadcast
            Thread unicastThread = new Thread(this::listenUnicast);
            unicastThread.start();

            // Nhận Multicast (lớp D)
            Thread multicastThread = new Thread(this::listenMulticast);
            multicastThread.start();

            unicastThread.join();
            multicastThread.join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenUnicast() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
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
        try (MulticastSocket multicastSocket = new MulticastSocket(port)) {
            InetAddress group = InetAddress.getByName("230.0.0.1");
            multicastSocket.joinGroup(group);

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

    public void stop() {
        running = false;
    }
}
