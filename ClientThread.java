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
            // 한글 깨짐 방지를 위한 스트림 설정
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            os = new PrintStream(clientSocket.getOutputStream(), true, "UTF-8");

            // ---------------------------------------------------------------
            // 1. 이름 입력 및 신분 확인
            // ---------------------------------------------------------------
            String name; 
            try {
                name = is.readLine().trim();
            } catch (Exception e) {
                name = "알수없음";
            }

            // ---------------------------------------------------------------
            // 2. 신분에 따른 추천 장소 설정 (일단 임시 저장)
            // ---------------------------------------------------------------
            String targetSpot = "";
            String messageReason = "";
            int destX = 0, destY = 0; // 목표 좌표

            if (name.contains("교수")) {
                targetSpot = "A-1 [연구실 전용]";
                messageReason = "교수님 환영합니다! 연구실(본관) 쪽으로";
                destX = 50; destY = 100; 
            } else if (name.contains("학생")) {
                targetSpot = "C-1 [명신관]";
                messageReason = "학생이시군요! 명신관 강의동 쪽으로";
                destX = -30; destY = 40; 
            } else {
                targetSpot = "B-1 [주차타워]";
                messageReason = "일반 방문객 추천 구역으로";
                destX = 10; destY = 10; 
            }

            // 입장 알림 방송 (채팅방에 다른 사람들에게 알림)
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

            // ---------------------------------------------------------------
            // 3. 대화형 안내 로직 (수정된 부분)
            // ---------------------------------------------------------------
            boolean startAutoDrive = false; // 주행 시작 여부

            os.println("==================================================");
            os.println("" + name + "님 차량 인식됨.");
            // 질문 던지기
            os.println("" + messageReason + " 안내해 드릴까요? (예 / 아니오)");
            os.println("==================================================");

            // 사용자의 대답 듣기
            String answer = is.readLine(); 
            if (answer != null) answer = answer.trim();

            if ("예".equals(answer)) {
                // [상황 1] 추천대로 안내
                os.println("네, " + targetSpot + " 구역으로 안내를 시작합니다.");
                startAutoDrive = true;

            } else {
                // [상황 2] 거절 -> 다른 곳 질문
                os.println("==================================================");
                os.println("어느 건물과 가까운 자리로 안내해드릴까요?");
                os.println("==================================================");

                String customPlace = is.readLine();
                if (customPlace != null) customPlace = customPlace.trim();

                if ("아니오".equals(customPlace)) {
                    // [상황 3] 두 번째 질문도 거절
                    os.println("안전한 운전 되세요!");
                    startAutoDrive = false; 
                } else {
                    // [상황 4] 특정 장소 입력 (예: 도서관)
                    targetSpot = customPlace + " 근처";
                    // 사용자 정의 좌표 (임의 설정)
                    destX = 88; destY = 88; 
                    
                    os.println("[시스템] 네, 입력하신 '" + targetSpot + "' 로 안내를 수정합니다.");
                    startAutoDrive = true;
                }
            }

            // ---------------------------------------------------------------
            // 4. IoT 자율주행 시뮬레이션 (자동 시작)
            // ---------------------------------------------------------------
            if (startAutoDrive) {
                os.println("\n===== 📡 [IoT 모드] 차량 센서 연동 시작 =====");
                os.println("차량 GPS 신호를 자동으로 수신합니다...");
                
                try {
                    for (int i = 1; i <= 5; i++) {
                        // 1.5초 딜레이 (이동하는 느낌)
                        Thread.sleep(1500); 
                        
                        // 좌표 계산 시뮬레이션
                        int curX = (destX / 5) * i;
                        int curY = (destY / 5) * i;
                        
                        // IoT 센서가 보낸 것처럼 출력
                        os.println("실시간 좌표 수신: (" + curX + ", " + curY + ") ...이동 중 🚗");
                    }
                    Thread.sleep(1000);
                    os.println("---------------------------------------------");
                    os.println("🎉 목적지 " + targetSpot + "에 도착했습니다.");
                    os.println("🅿️ 주차 완료. 시동을 끕니다.");
                    os.println("=============================================\n");

                } catch (InterruptedException e) {
                    os.println("[에러] 센서 연결 끊김");
                }
            }

            // ---------------------------------------------------------------
            // 5. 일반 채팅 및 수동 명령어 루프
            // ---------------------------------------------------------------
            os.println("채팅 채널에 연결되었습니다. ('/quit'으로 종료)");

            while (true) {
                String line = is.readLine();
                if (line == null || line.startsWith("/quit")) {
                    break;
                }

                // 수동 이동 기능 (필요하다면 유지)
                if (line.startsWith("/move")) {
                    os.println("좌표 입력됨. 이동 처리합니다!");
                    continue; 
                }

                // 귓속말 처리
                if (line.startsWith("@")) {
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
                    // 전체 채팅
                    synchronized (this) {
                        for (int i = 0; i < maxClientsCount; i++) {
                            if (threads[i] != null && threads[i].clientName != null) {
                                threads[i].os.println("<" + name + "> " + line);
                            }
                        }
                    }
                }
            } // while end

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