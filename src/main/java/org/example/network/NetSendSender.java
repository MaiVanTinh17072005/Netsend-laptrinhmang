package org.example.network;

import java.net.*;

public class NetSendSender {
    private final int port = 9876;

    public void send(String target, String message) throws Exception {
        if (target.equals("*")) {
            // Broadcast
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, port);
                socket.send(packet);
            }
        } else if (isMulticastAddress(target)) {
            // Multicast
            InetAddress group = InetAddress.getByName(target);
            try (MulticastSocket multicastSocket = new MulticastSocket()) {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, port);
                multicastSocket.send(packet);
            }
        } else {
            // Unicast
            InetAddress address = InetAddress.getByName(target);
            try (DatagramSocket socket = new DatagramSocket()) {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                socket.send(packet);
            }
        }
    }

    private boolean isMulticastAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isMulticastAddress(); // Kiểm tra xem có thuộc lớp D không
        } catch (Exception e) {
            return false;
        }
    }
}
