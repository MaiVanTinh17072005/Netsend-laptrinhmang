package org.example.controller;

import org.example.network.NetSendReceiver;
import org.example.network.NetSendSender;
import org.example.ui.NetSendUI;

public class NetSendController {
    private final NetSendSender sender;
    private final NetSendReceiver receiver;
    private final NetSendUI ui;

    public NetSendController(NetSendUI ui) {
        this.ui = ui;
        this.sender = new NetSendSender();

        this.receiver = new NetSendReceiver(9876, (ip, msg) -> {
            ui.showMessage("[From " + ip + "]: " + msg);
        });

        new Thread(receiver).start();
    }

    public void sendMessage(String target, String message) {
        try {
            sender.send(target, message);
            ui.showMessage("[You → " + target + "]: " + message);
        } catch (Exception e) {
            ui.showMessage("❌ Error: " + e.getMessage());
        }
    }
    public String getLocalIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "Unknown";
        }
    }

}
