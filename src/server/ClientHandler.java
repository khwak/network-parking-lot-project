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
<<<<<<< Updated upstream

                // [Case 2] 유저가 결제 수단을 등록함
                else if ("USER".equals(this.role) && line.startsWith(Protocol.REQ_REG_PAYMENT)) {
                    String method = line.split(":")[2];
                    this.hasPaymentMethod = true; // 서버 메모리에 상태 저장!

                    this.os.println("[Server] 결제 수단(" + method + ")이 등록되었습니다.");

                    // 등록 즉시 결제 완료 처리 (사용자 편의)
                    this.os.println(Protocol.MSG_PAYMENT);
                    System.out.println("[Log] Payment method registered for " + this.carNum);
=======
                // [길 안내] 유저가 길 안내를 요청했을 때 ("REQ:NAV")
                else if ("USER".equals(this.role) && line.equals(Protocol.REQ_NAV)) {
                    System.out.println("[Log] User " + this.carNum + " requested navigation.");
                    // 서버가 바쁘지 않게 별도 스레드로 시뮬레이션 시작
                    new Thread(this::simulateNavigation).start();
>>>>>>> Stashed changes
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
    // [길 안내] 자율주행 시뮬레이션 로직
    private void simulateNavigation() {
        try {
            // 1. 목적지 설정 (팀원 코드의 "교수/학생" 로직을 단순화하여 적용)
            // 차량 번호가 짝수면 '교수(본관)', 홀수면 '학생(명신관)'으로 가정해봅시다.
            String targetName;
            int destX, destY;

            // 간단히 차번호 끝자리를 이용해 분류
            char lastChar = carNum.charAt(carNum.length() - 1);
            if ((lastChar - '0') % 2 == 0) {
                targetName = "본관(교수 연구동)";
                destX = 50; destY = 100;
            } else {
                targetName = "명신관(강의동)";
                destX = -30; destY = 40;
            }

            os.println("[Server] " + targetName + "으로 안내를 시작합니다. (IoT 센서 연동 중...)");
            Thread.sleep(1000); // 준비 시간

            // 2. 주행 시뮬레이션 (팀원 코드의 for 루프 활용)
            for (int i = 1; i <= 5; i++) {
                // 1.5초 딜레이 (이동하는 느낌)
                Thread.sleep(1500);

                // 현재 위치 계산 (선형 보간)
                int curX = (destX / 5) * i;
                int curY = (destY / 5) * i;

                // 클라이언트에게 좌표 전송 (프로토콜: "NAV:COORD:X,Y")
                os.println(Protocol.NAV_COORD + curX + "," + curY);
            }

            // 3. 도착 알림
            Thread.sleep(1000);
            os.println("[Server] 목적지 도착: " + targetName);
            os.println(Protocol.NAV_END); // 종료 신호

        } catch (InterruptedException e) {
            System.out.println("[Error] Navigation interrupted.");
        }
    }
}