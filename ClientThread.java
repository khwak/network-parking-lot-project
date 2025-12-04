import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class ClientThread extends Thread {
    private String clientName = null;
    private BufferedReader is = null;
    private PrintStream os = null;
    private Socket clientSocket = null;
    private final ClientThread[] threads;
    private int maxClientsCount;

    public ClientThread(Socket clientSocket, ClientThread[] threads) {
        this.clientSocket = clientSocket;
        this.threads = threads;
        maxClientsCount = threads.length;
    }

    public void run() {
        int maxClientsCount = this.maxClientsCount;
        ClientThread[] threads = this.threads;

        try {
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            os = new PrintStream(clientSocket.getOutputStream(), true, "UTF-8");

            // 1. 이름 입력 및 신분 확인
            String name; 
            try {
                name = is.readLine().trim();
            } catch (Exception e) {
                name = "알수없음";
            }

            // 신분에 따른 목표 지점 설정 (가상 좌표)
            String targetSpot = "";
            String messageReason = "";
            int destX = 0, destY = 0; // 목표 좌표

            if (name.contains("교수")) {
                targetSpot = "A-1 [연구실 전용]";
                messageReason = "교수님 환영합니다! 연구실(본관)로 안내합니다.";
                destX = 50; destY = 100; // 교수님 목표 좌표
            } else if (name.contains("학생")) {
                targetSpot = "C-1 [명신관]";
                messageReason = "학생이시군요! 명신관 강의동으로 안내합니다.";
                destX = -30; destY = 40; // 학생 목표 좌표
            } else {
                targetSpot = "B-1 [주차타워]";
                messageReason = "일반 방문객 전용 구역으로 안내합니다.";
                destX = 10; destY = 10; // 일반인 목표 좌표
            }

            // 환영 메시지
            os.println("==================================================");
            os.println("[시스템] " + name + "님 차량 인식됨.");
            os.println("[시스템] " + messageReason);
            os.println("[시스템] 🚩 배정된 주차면: " + targetSpot);
            os.println("==================================================");
            os.println("[시스템] IoT 센서 연동 대기 중... ('/auto' 를 입력하여 주행 시작)");

            // 입장 알림 방송
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this) {
                        threads[i].os.println("*** " + name + " 님이 주차장에 진입했습니다. ***");
                    }
                }
            }

            /* 채팅 및 명령어 처리 루프 */
            while (true) {
                String line = is.readLine();
                if (line == null || line.startsWith("/quit")) {
                    break;
                }

                // ====================================================
                // [기능 1] 수동 이동 (기존 기능)
                // ====================================================
                if (line.startsWith("/move")) {
                    os.println("[수동] 좌표 입력됨. 이동 처리합니다.");
                    continue; 
                }

                // ====================================================
                // [기능 2] IoT 자율주행 시뮬레이션 (여기가 핵심!)
                // ====================================================
                if (line.startsWith("/auto")) {
                    os.println("\n===== 📡 [IoT 모드] 차량 센서 연동 시작 =====");
                    os.println("[시스템] 차량 GPS 신호를 자동으로 수신합니다...");
                    
                    // 5단계로 나누어 이동하는 척 연출
                    try {
                        for (int i = 1; i <= 5; i++) {
                            // 1.5초 딜레이 (이동하는 느낌)
                            Thread.sleep(1500); 
                            
                            // 현재 위치 계산 (점점 목표에 가까워짐)
                            int curX = (destX / 5) * i;
                            int curY = (destY / 5) * i;
                            
                            // IoT 센서가 보낸 것처럼 출력
                            os.println("[IoT센서] 실시간 좌표 수신: (" + curX + ", " + curY + ") ...이동 중 🚗");
                        }
                        Thread.sleep(1000);
                        os.println("---------------------------------------------");
                        os.println("🎉 [안내] 목적지 " + targetSpot + "에 도착했습니다.");
                        os.println("🅿️ [안내] 주차 완료. 시동을 끕니다.");
                        os.println("=============================================\n");

                    } catch (InterruptedException e) {
                        os.println("[에러] 센서 연결 끊김");
                    }
                    continue;
                }

                // 일반 채팅 처리
                if (line.startsWith("@")) {
                    // 귓속말 로직 (생략 없이 그대로 유지)
                    String[] words = line.split("\\s", 2);
                    if (words.length > 1 && words[1] != null) {
                        words[1] = words[1].trim();
                        if (!words[1].isEmpty()) {
                            synchronized (this) {
                                for (int i = 0; i < maxClientsCount; i++) {
                                    if (threads[i] != null && threads[i] != this
                                            && threads[i].clientName != null
                                            && threads[i].clientName.equals(words[0])) {
                                        threads[i].os.println("<" + name + "> " + words[1]);
                                        this.os.println(">" + name + "> " + words[1]);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    synchronized (this) {
                        for (int i = 0; i < maxClientsCount; i++) {
                            if (threads[i] != null && threads[i].clientName != null) {
                                threads[i].os.println("<" + name + "> " + line);
                            }
                        }
                    }
                }
            } 

            // 퇴장 처리
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] != this
                            && threads[i].clientName != null) {
                        threads[i].os.println("*** " + name + " 님이 나갔습니다. ***");
                    }
                }
            }
            os.println("*** 안녕히 가세요 " + name + " 님 ***");

            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] == this) {
                        threads[i] = null;
                    }
                }
            }
            is.close();
            os.close();
            clientSocket.close();
        } catch (IOException e) {
        }
    }
}