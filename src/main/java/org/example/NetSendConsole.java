package org.example;


import java.net.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class NetSendConsole {
    // Cổng để giao tiếp giữa các máy - TẤT CẢ máy phải dùng cùng port này
    private static final int PORT = 9999;

    // Địa chỉ nhóm multicast - các máy muốn nhận tin multicast phải join vào group này
    private static final String MULTICAST_GROUP = "230.0.0.1";

    // Socket duy nhất để NHẬN tất cả các loại tin nhắn (unicast, multicast, broadcast)
    private static MulticastSocket receiveSocket;

    // Biến kiểm soát vòng lặp - khi false thì chương trình dừng
    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        System.out.println("=== CHƯƠNG TRÌNH NET SEND (CONSOLE) ===");
        System.out.println("Khởi động chương trình...\n");

        // Tạo và khởi động thread riêng để NHẬN tin nhắn
        // Thread này chạy song song với thread chính (main)
        Thread receiverThread = new Thread(() -> startReceiver());
        receiverThread.setDaemon(true); // Daemon thread sẽ tự động dừng khi main thread kết thúc
        receiverThread.start();

        // Chờ receiver khởi động hoàn tất
        try {
            Thread.sleep(500); // Đợi 0.5 giây
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Bắt đầu giao diện console để GỬI tin nhắn
        startSendingInterface();
    }

    /**
     * Khởi động receiver để lắng nghe và nhận tin nhắn
     * Hàm này chạy trong thread riêng, không block main thread
     */
    private static void startReceiver() {
        try {
            System.out.println("[LOG] Đang khởi tạo receiver...");

            // TẠO MULTICAST SOCKET - socket đặc biệt có thể nhận cả 3 loại tin
            receiveSocket = new MulticastSocket(PORT);
            System.out.println("[LOG] Đã tạo MulticastSocket trên port " + PORT);

            // BẬT CHẾ ĐỘ BROADCAST cho socket - cho phép nhận tin broadcast
            receiveSocket.setBroadcast(true);
            System.out.println("[LOG] Đã bật chế độ broadcast");

            // SET TIMEOUT - tránh bị block vô hạn khi gọi receive()
            // Sau 100ms không nhận được gì thì ném SocketTimeoutException
            receiveSocket.setSoTimeout(100);

            // JOIN VÀO MULTICAST GROUP - đăng ký nhận tin từ group này
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            receiveSocket.joinGroup(group);
            System.out.println("[LOG] Đã tham gia multicast group: " + MULTICAST_GROUP);

            System.out.println("[LOG] Receiver đã sẵn sàng nhận tin nhắn!\n");

            // VÒNG LẶP CHÍNH - liên tục lắng nghe và nhận tin nhắn
            byte[] buffer = new byte[1024]; // Buffer để chứa dữ liệu nhận được (tối đa 1024 bytes)

            while (isRunning) {
                try {
                    // TẠO PACKET để nhận dữ liệu
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    // NHẬN TIN NHẮN - hàm này sẽ block cho đến khi:
                    // 1. Nhận được tin nhắn, HOẶC
                    // 2. Timeout (100ms) -> ném SocketTimeoutException
                    receiveSocket.receive(packet);

                    // TRÍCH XUẤT THÔNG TIN từ packet nhận được
                    // Lấy nội dung tin nhắn từ byte array
                    String message = new String(packet.getData(), 0, packet.getLength());

                    // Lấy địa chỉ IP của máy gửi
                    String senderIP = packet.getAddress().getHostAddress();

                    // XÁC ĐỊNH LOẠI TIN NHẮN
                    // Kiểm tra địa chỉ đích có phải multicast không
                    String messageType = "UNICAST/BROADCAST";
                    if (packet.getAddress().isMulticastAddress()) {
                        messageType = "MULTICAST";
                    }

                    // HIỂN THỊ tin nhắn đã nhận
                    displayReceivedMessage(senderIP, message, messageType);

                } catch (SocketTimeoutException e) {
                    // Timeout là bình thường - chỉ để tránh block vô hạn
                    // Không cần xử lý gì, tiếp tục vòng lặp
                } catch (IOException e) {
                    // Lỗi khác (nếu chương trình vẫn chạy thì báo lỗi)
                    if (isRunning) {
                        System.err.println("[LỖI] Lỗi khi nhận tin nhắn: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            // Lỗi nghiêm trọng khi khởi tạo receiver
            System.err.println("[LỖI NGHIÊM TRỌNG] Không thể khởi tạo receiver: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Giao diện console để người dùng nhập lệnh gửi tin nhắn
     */
    private static void startSendingInterface() {
        // Tạo Scanner để đọc input từ bàn phím
        Scanner scanner = new Scanner(System.in);

        System.out.println("═══════════════════════════════════════════════");
        System.out.println("HƯỚNG DẪN SỬ DỤNG:");
        System.out.println("1. Gửi đến một IP: net send <IP> <tin nhắn>");
        System.out.println("2. Gửi đến group:  net send group <tin nhắn>");
        System.out.println("3. Gửi đến tất cả: net send * <tin nhắn>");
        System.out.println("4. Thoát chương trình: exit");
        System.out.println("═══════════════════════════════════════════════\n");

        // VÒNG LẶP CHÍNH - liên tục đọc lệnh từ người dùng
        while (true) {
            System.out.print("Nhập lệnh: ");
            String input = scanner.nextLine().trim();

            // LỆNH THOÁT - dừng chương trình
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("[LOG] Đang thoát chương trình...");
                isRunning = false; // Dừng receiver thread
                cleanup(); // Đóng socket
                System.out.println("[LOG] Chương trình đã dừng. Tạm biệt!");
                System.exit(0);
            }

            // Bỏ qua dòng rỗng
            if (input.isEmpty()) {
                continue;
            }

            // KIỂM TRA CÚ PHÁP - lệnh phải bắt đầu bằng "net send"
            if (!input.toLowerCase().startsWith("net send ")) {
                System.out.println("[LỖI] Lệnh phải bắt đầu bằng 'net send'");
                continue;
            }

            // CẮT BỎ "net send " để lấy phần còn lại
            String command = input.substring(9).trim(); // 9 = độ dài của "net send "

            if (command.isEmpty()) {
                System.out.println("[LỖI] Thiếu đích đến và tin nhắn");
                continue;
            }

            // TÁCH DESTINATION VÀ MESSAGE
            // Tìm khoảng trắng đầu tiên để tách đích đến và tin nhắn
            int spaceIndex = command.indexOf(' ');
            if (spaceIndex == -1) {
                System.out.println("[LỖI] Thiếu tin nhắn");
                continue;
            }

            String destination = command.substring(0, spaceIndex).trim(); // Đích đến (IP/group/*)
            String message = command.substring(spaceIndex + 1).trim();    // Tin nhắn

            if (message.isEmpty()) {
                System.out.println("[LỖI] Tin nhắn không được để trống");
                continue;
            }

            // GỬI TIN NHẮN
            sendMessage(destination, message);
        }
    }

    /**
     * Gửi tin nhắn đến đích chỉ định
     * @param destination Đích đến: IP cụ thể / "group" / "*"
     * @param message Nội dung tin nhắn
     */
    private static void sendMessage(String destination, String message) {
        System.out.println("\n[LOG] Chuẩn bị gửi tin nhắn...");
        System.out.println("[LOG] Đích đến: " + destination);
        System.out.println("[LOG] Nội dung: " + message);

        try {
            // CHUYỂN ĐỔI tin nhắn từ String sang byte array
            byte[] buffer = message.getBytes();

            // Biến để lưu packet sẽ gửi
            DatagramPacket packet;

            // PHÂN LOẠI VÀ XỬ LÝ THEO LOẠI GỬI

            if (destination.equals("*")) {
                // ============ BROADCAST ============
                System.out.println("[LOG] Chế độ: BROADCAST");

                // Tạo địa chỉ broadcast đặc biệt - gửi đến TẤT CẢ máy trong mạng LAN
                InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

                // Tạo packet với địa chỉ broadcast
                packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, PORT);

                // Tạo socket GỬI mới (không dùng receiveSocket)
                DatagramSocket sendSocket = new DatagramSocket();

                // BẮT BUỘC phải bật broadcast mode để gửi được
                sendSocket.setBroadcast(true);

                System.out.println("[LOG] Đang gửi broadcast đến 255.255.255.255:" + PORT);

                // GỬI PACKET
                sendSocket.send(packet);

                // ĐÓNG SOCKET ngay sau khi gửi xong
                sendSocket.close();

                System.out.println("[THÀNH CÔNG] Đã gửi broadcast thành công!\n");

            } else if (destination.equalsIgnoreCase("group")) {
                // ============ MULTICAST ============
                System.out.println("[LOG] Chế độ: MULTICAST");

                // Tạo địa chỉ multicast group
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);

                // Tạo packet với địa chỉ multicast
                packet = new DatagramPacket(buffer, buffer.length, group, PORT);

                // Tạo MulticastSocket để gửi
                MulticastSocket sendSocket = new MulticastSocket();

                System.out.println("[LOG] Đang gửi multicast đến group " + MULTICAST_GROUP + ":" + PORT);

                // GỬI PACKET - tất cả máy đã joinGroup sẽ nhận được
                sendSocket.send(packet);

                // ĐÓNG SOCKET
                sendSocket.close();

                System.out.println("[THÀNH CÔNG] Đã gửi multicast thành công!\n");

            } else {
                // ============ UNICAST ============
                System.out.println("[LOG] Chế độ: UNICAST");

                try {
                    // PHÂN GIẢI địa chỉ IP/hostname
                    // Ví dụ: "192.168.1.100" hoặc "localhost" -> InetAddress
                    InetAddress targetAddress = InetAddress.getByName(destination);
                    System.out.println("[LOG] Địa chỉ IP đích: " + targetAddress.getHostAddress());

                    // Tạo packet với địa chỉ IP cụ thể
                    packet = new DatagramPacket(buffer, buffer.length, targetAddress, PORT);

                    // Tạo socket GỬI
                    DatagramSocket sendSocket = new DatagramSocket();

                    System.out.println("[LOG] Đang gửi unicast đến " + targetAddress.getHostAddress() + ":" + PORT);

                    // GỬI PACKET - chỉ máy có IP này nhận được
                    sendSocket.send(packet);

                    // ĐÓNG SOCKET
                    sendSocket.close();

                    System.out.println("[THÀNH CÔNG] Đã gửi unicast thành công!\n");

                } catch (UnknownHostException e) {
                    // Lỗi: Không tìm thấy địa chỉ IP
                    System.err.println("[LỖI] Không tìm thấy địa chỉ IP: " + destination);
                    System.err.println("[LỖI] Chi tiết: " + e.getMessage() + "\n");
                    return;
                }
            }

        } catch (SocketException e) {
            // Lỗi khi tạo/sử dụng socket
            System.err.println("[LỖI] Lỗi socket khi gửi tin nhắn: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            // Lỗi I/O khác
            System.err.println("[LỖI] Lỗi I/O khi gửi tin nhắn: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Hiển thị tin nhắn đã nhận lên console với format đẹp
     * @param senderIP Địa chỉ IP của máy gửi
     * @param message Nội dung tin nhắn
     * @param type Loại tin nhắn (UNICAST/BROADCAST/MULTICAST)
     */
    private static void displayReceivedMessage(String senderIP, String message, String type) {
        // Lấy thời gian hiện tại
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String time = sdf.format(new Date());

        // Tạo khung đẹp cho tin nhắn
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║ TIN NHẮN MỚI (" + type + ") - " + time);
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║ Từ IP: " + senderIP);
        System.out.println("║ Nội dung: " + message);
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        // In lại dòng nhắc lệnh
        System.out.print("Nhập lệnh: ");
    }

    /**
     * Dọn dẹp và đóng socket khi thoát chương trình
     */
    private static void cleanup() {
        System.out.println("[LOG] Đang đóng các socket...");
        try {
            if (receiveSocket != null && !receiveSocket.isClosed()) {
                // RỜI KHỎI multicast group trước khi đóng
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                receiveSocket.leaveGroup(group);

                // ĐÓNG SOCKET
                receiveSocket.close();

                System.out.println("[LOG] Đã rời multicast group và đóng socket");
            }
        } catch (Exception e) {
            System.err.println("[LỖI] Lỗi khi đóng socket: " + e.getMessage());
        }
    }
}