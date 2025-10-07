package org.example.network;

import java.net.*;

public class NetSendSender {
    private final int port = 9876; // Khớp với unicastPort

    public void send(String target, String message) throws Exception {
        if (target == null || target.isEmpty() || message == null || message.isEmpty()) {
            throw new IllegalArgumentException("Target or message cannot be empty");
        }

        byte[] buffer = message.getBytes();
        if (target.equals("*")) {
            // Broadcast
            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setBroadcast(true);
                InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcast, port);
                socket.send(packet);
            }
        } else if (isMulticastAddress(target)) {
            // Multicast
            InetAddress group = InetAddress.getByName(target);
            try (MulticastSocket multicastSocket = new MulticastSocket()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, 9877); // Khớp với multicastPort
                multicastSocket.send(packet);
            }
        } else {
            // Unicast
            try {
                InetAddress address = InetAddress.getByName(target);
                try (DatagramSocket socket = new DatagramSocket()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
                    socket.send(packet);
                }
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Invalid IP address: " + target);
            }
        }
    }

    private boolean isMulticastAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return addr.isMulticastAddress();
        } catch (Exception e) {
            return false;
        }
    }
}