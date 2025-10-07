package org.example.ui;

import org.example.controller.NetSendController;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class NetSendUI extends JFrame {
    private JTextField txtTarget, txtMessage, txtMyIP;
    private JTextArea txtDisplay;
    private JComboBox<String> comboTargetType;
    private NetSendController controller;
    private static final String GROUP_IP = "230.0.0.1";

    public NetSendUI() {
        setTitle("Net Send App - UDP Messenger");
        setSize(550, 420);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // Ngăn đóng ngay lập tức
        setLayout(new BorderLayout());

        // --- KHUNG HIỂN THỊ ---
        txtDisplay = new JTextArea();
        txtDisplay.setEditable(false);
        add(new JScrollPane(txtDisplay), BorderLayout.CENTER);

        // --- KHUNG DƯỚI: IP + MESSAGE + BUTTON ---
        JPanel panelBottom = new JPanel(new BorderLayout());
        JPanel panelTop = new JPanel(new GridLayout(2, 2, 5, 5));
        panelTop.add(new JLabel("Your IP:"));
        txtMyIP = new JTextField();
        txtMyIP.setEditable(false);
        panelTop.add(txtMyIP);
        panelTop.add(new JLabel("Send To:"));
        txtTarget = new JTextField();
        panelTop.add(txtTarget);
        panelBottom.add(panelTop, BorderLayout.NORTH);

        JPanel panelMid = new JPanel(new BorderLayout());
        comboTargetType = new JComboBox<>(new String[]{"1 máy (Unicast)", "Nhóm (Multicast)", "Tất cả (Broadcast)"});
        panelMid.add(comboTargetType, BorderLayout.WEST);
        txtMessage = new JTextField();
        panelMid.add(txtMessage, BorderLayout.CENTER);
        JButton btnSend = new JButton("Send");
        panelMid.add(btnSend, BorderLayout.EAST);
        panelBottom.add(panelMid, BorderLayout.SOUTH);
        add(panelBottom, BorderLayout.SOUTH);

        // --- CONTROLLER ---
        controller = new NetSendController(this);
        txtMyIP.setText(controller.getLocalIP());

        // --- SỰ KIỆN CHỌN LOẠI GỬI ---
        comboTargetType.addActionListener(e -> {
            String choice = (String) comboTargetType.getSelectedItem();
            switch (choice) {
                case "1 máy (Unicast)" -> txtTarget.setText("192.168.1.x");
                case "Nhóm (Multicast)" -> txtTarget.setText(GROUP_IP);
                case "Tất cả (Broadcast)" -> txtTarget.setText("*");
            }
        });

        // --- NÚT GỬI ---
        btnSend.addActionListener(e -> {
            String target = txtTarget.getText().trim();
            String message = txtMessage.getText().trim();
            if (!message.isEmpty()) {
                controller.sendMessage(target, message);
                txtMessage.setText("");
            } else {
                showMessage("❌ Message cannot be empty");
            }
        });

        // --- XỬ LÝ KHI ĐÓNG CỬA SỔ ---
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    controller.shutdown();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                dispose();
            }
        });

        setVisible(true);
    }

    public void showMessage(String msg) {
        SwingUtilities.invokeLater(() -> txtDisplay.append(msg + "\n"));
    }

    public static void main(String[] args) {
        new NetSendUI();
    }
}