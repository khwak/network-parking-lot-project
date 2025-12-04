package server;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import utils.Protocol;

public class ClientHandler extends Thread {
    private String role = null;
    private String carNum = null;

    // 결제 수단 등록 여부 (기본값: false = 없음)
    private boolean hasPaymentMethod = false;

    private DataInputStream is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final ClientHandler[] threads;
    private int maxClientsCount;

    public ClientHandler(Socket clientSocket, ClientHandler[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.maxClientsCount = threads.length;
    }

    public void run() {
        try {
            is = new DataInputStream(clientSocket.getInputStream());
            os = new PrintStream(clientSocket.getOutputStream());

            // --- 1. 로그인 처리 ---
            String loginMsg = is.readLine();
            if (loginMsg == null) return;
            loginMsg = loginMsg.trim();

            if (loginMsg.startsWith(Protocol.LOGIN_LPR)) {
                this.role = "LPR";
                os.println("[Server] LPR Camera registered.");
                System.out.println("[Log] LPR connected.");
            } else if (loginMsg.startsWith(Protocol.LOGIN_USER)) {
                this.role = "USER";
                if (loginMsg.split(":").length > 2) {
                    this.carNum = loginMsg.split(":")[2];
                    os.println("[Server] User registered. Car: " + this.carNum);
                    System.out.println("[Log] User connected: " + this.carNum);
                } else {
                    return;
                }
            } else {
                os.println("[Server] Unknown client type.");
                return;
            }

            // --- 2. 메시지 수신 루프 ---
            while (true) {
                String line = is.readLine();
                if (line == null || line.startsWith(Protocol.CMD_EXIT)) break;
                line = line.trim();

                // [Case 1] LPR이 차량 인식 (결제 시도)
                if ("LPR".equals(this.role) && line.startsWith(Protocol.DETECT_CAR)) {
                    String targetCarNum = line.split(":")[1];
                    System.out.println("[Event] LPR detected: " + targetCarNum);

                    synchronized (this) {
                        boolean found = false;
                        for (ClientHandler t : threads) {
                            if (t != null && "USER".equals(t.role) && targetCarNum.equals(t.carNum)) {
                                found = true;

                                // ★ 핵심: 결제 수단 여부 체크
                                if (t.hasPaymentMethod) {
                                    t.os.println(Protocol.MSG_PAYMENT); // 결제 성공
                                    this.os.println("[Server] Payment Success sent to " + targetCarNum);
                                } else {
                                    t.os.println(Protocol.NOTI_NEED_PAYMENT); // 등록 요청
                                    this.os.println("[Server] Payment Required sent to " + targetCarNum);
                                }
                                break;
                            }
                        }
                        if (!found) this.os.println("[Server] User not found.");
                    }
                }

                // [Case 2] 유저가 결제 수단을 등록함
                else if ("USER".equals(this.role) && line.startsWith(Protocol.REQ_REG_PAYMENT)) {
                    String method = line.split(":")[2];
                    this.hasPaymentMethod = true; // 서버 메모리에 상태 저장!

                    this.os.println("[Server] 결제 수단(" + method + ")이 등록되었습니다.");

                    // 등록 즉시 결제 완료 처리 (사용자 편의)
                    this.os.println(Protocol.MSG_PAYMENT);
                    System.out.println("[Log] Payment method registered for " + this.carNum);
                }
            }
            os.println("*** Bye ***");

        } catch (IOException e) {
            System.out.println("[Error] Connection lost: " + role);
        } finally {
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) threads[i] = null;
                }
            }
            try { if (is != null) is.close(); if (os != null) os.close(); if (clientSocket != null) clientSocket.close(); } catch (IOException e) {}
        }
    }
}