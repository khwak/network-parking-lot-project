package server;

import java.io.*;
import java.net.Socket;
import utils.Protocol;

public class ClientHandler extends Thread {
    private String role = null;
    private String userType = "VISITOR";
    private String carNum = null;

    private BufferedReader reader = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final ClientHandler[] threads;
    private int maxClientsCount;

    public ClientHandler(Socket clientSocket, ClientHandler[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        this.maxClientsCount = threads.length;
    }

    // 신분 조회 DB 메서드
    private String determineUserType(String carNum) {
        try {
            int num = Integer.parseInt(carNum);
            if (num >= 1000 && num <= 1999) return "PROFESSOR"; // 1000번대는 교수
            if (num >= 2000 && num <= 2999) return "STUDENT";   // 2000번대는 학생
        } catch (NumberFormatException e) {
            // 숫자가 아니면 방문객 처리
        }
        return "VISITOR"; // 그 외는 방문객
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        ClientHandler[] threads = this.threads;

        try {
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            os = new PrintStream(clientSocket.getOutputStream(), true, "UTF-8");

            String loginMsg = reader.readLine();
            if (loginMsg == null) return;
            loginMsg = loginMsg.trim();

            // 1. 로그인 처리
            if (loginMsg.startsWith(Protocol.LOGIN_LPR)) {
                this.role = "LPR";
                os.println("[System] LPR Camera connected.");
            } else if (loginMsg.startsWith(Protocol.LOGIN_USER)) {
                this.role = "USER";
                // 메시지 예시: "LOGIN:USER:1111"
                if (loginMsg.split(":").length > 2) {
                    this.carNum = loginMsg.split(":")[2];

                    // 신분을 결정
                    this.userType = determineUserType(this.carNum);

                    String welcomeMsg = "";
                    if(userType.equals("PROFESSOR")) welcomeMsg = "교수님";
                    else if(userType.equals("STUDENT")) welcomeMsg = "학생";
                    else welcomeMsg = "방문객";

                    os.println("[System] " + welcomeMsg + " 차량(" + this.carNum + ") 확인. 대기 모드입니다.");
                    System.out.println("[Log] User connected: " + this.carNum + " (" + this.userType + ")");
                }
            }

            // 2. 메시지 수신
            while (true) {
                String line = reader.readLine();
                if (line == null || line.startsWith(Protocol.CMD_EXIT)) break;
                line = line.trim();

                // [신고 기능]
                if (line.startsWith("/report")) {
                    String content = line.replace("/report", "").trim();
                    os.println("[System] 신고가 접수되었습니다. (내용: " + content + ")");
                    System.out.println("[Report] 신고 접수! 차량번호: " + this.carNum + " / 내용: " + content);
                }

                // [도움 요청 기능]
                else if (line.startsWith("/help")) {
                    os.println("[System] 보안팀 호출 완료. 위치 추적 중...");
                    System.out.println("[Emergency] 긴급 도움 요청! 차량번호: " + this.carNum + " (위치: " + this.userType + " 구역 인근)");
                }

                // [LPR 로직] 차량 인식 시 -> 접속된 유저에게 알림
                else if ("LPR".equals(this.role) && line.startsWith(Protocol.DETECT_CAR)) {
                    String targetCarNum = line.split(":")[1];
                    System.out.println("[Event] Detected: " + targetCarNum);

                    synchronized (this) {
                        for (int i = 0; i < maxClientsCount; i++) {
                            ClientHandler t = threads[i];
                            if (t != null && "USER".equals(t.role) && targetCarNum.equals(t.carNum)) {
                                // 1. 결제 프로토콜 전송 (팝업용)
                                t.os.println(Protocol.MSG_PAYMENT);
                                // 2. 채팅창에 인식 알림 텍스트 전송
                                t.os.println("🔔 " + targetCarNum + "님 차량이 인식되었습니다. (출차 절차 진행)");
                                this.os.println("[System] User " + targetCarNum + " notified.");
                            }
                        }
                    }
                }

                // [길 안내 요청]
                else if ("USER".equals(this.role) && line.equals(Protocol.REQ_NAV)) {
                    System.out.println("[Nav] Navigation requested by " + this.carNum);
                    // 별도 스레드로 안내 시작
                    new Thread(this::simulateNavigation).start();
                }
            }
        } catch (IOException e) {
            System.out.println("[Error] Connection lost: " + role);
        } finally {
            // 리소스 정리 (생략 - 기존 코드와 동일)
            closeResources();
        }
    }

    // [길 안내] 상세 텍스트 내비게이션
    private void simulateNavigation() {
        try {
            String targetName = "";
            String msgStart = "";
            int destX = 0, destY = 0;

            // 이미 로그인할 때 결정된 userType을 사용
            if ("PROFESSOR".equals(this.userType)) {
                targetName = "A-1 [연구실 전용]";
                msgStart = "교수님 환영합니다! 본관 연구동 ";
                destX = 50; destY = 100;
            } else if ("STUDENT".equals(this.userType)) {
                targetName = "C-1 [명신관]";
                msgStart = "학생이시군요! 명신관 강의동 ";
                destX = -30; destY = 40;
            } else { // VISITOR
                targetName = "B-1 [주차타워]";
                msgStart = "일반 방문객 추천 구역, ";
                destX = 10; destY = 10;
            }

            // 안내 시작 메시지
            os.println("=========================================");
            os.println(msgStart + "쪽으로 안내를 시작합니다.");
            Thread.sleep(1000);
            os.println("📡 [IoT 모드] 스마트 내비게이션 활성화");
            Thread.sleep(1000);

            // 거리 및 시간 계산
            int totalDist = (int)Math.sqrt(destX * destX + destY * destY); // 원점(0,0) 기준 거리 예시
            os.println("📍 추천 주차면: " + targetName);
            os.println("📍 총 거리: " + totalDist + "m (예상 " + (totalDist / 5) + "초)");

            Thread.sleep(1000);
            os.println("🚗 주차장 입구 통과. 서행하세요.");
            Thread.sleep(1500);

            // 주행 시뮬레이션
            for (int i = 1; i <= 5; i++) {
                int curX = (destX / 5) * i;
                int curY = (destY / 5) * i;
                os.println(Protocol.NAV_COORD + curX + "," + curY);

                // 구간별 멘트
                if (i == 2) {
                    if ("PROFESSOR".equals(userType)) os.println("➡️ 20m 앞 본관 방향으로 우회전하세요.");
                    else if ("STUDENT".equals(userType)) os.println("⬅️ 15m 앞 명신관 방향으로 좌회전하세요.");
                    else os.println("⬆️ 주차타워 방향으로 직진하세요.");
                }
                else if (i == 3) {
                    if ("VISITOR".equals(userType)) os.println("➡️ 12m 앞 주차타워 진입로입니다.");
                    else os.println("🚗 목적지 방면으로 안전 운행 중...");
                }
                else if (i == 4) {
                    os.println("⚠️ 보행자 주의! 속도를 줄이세요.");
                }
                else if (i == 5) {
                    if ("PROFESSOR".equals(userType)) os.println("🔄 좌측 교수 전용 구역에 주차하세요.");
                    else if ("STUDENT".equals(userType)) os.println("🔄 우측 학생 주차 구역에 주차하세요.");
                    else os.println("🔄 전방 주차타워 입구로 진입하세요.");
                }
                Thread.sleep(1500);
            }

            Thread.sleep(1000);
            os.println("🎉 목적지 도착 완료. 안전하게 주차되었습니다.");
            os.println(Protocol.NAV_END);

        } catch (InterruptedException e) {}
    }

    private void closeResources() {
        try {
            if (reader != null) reader.close();
            if (os != null) os.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {}
    }
}