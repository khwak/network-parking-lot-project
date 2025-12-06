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
            // 2. 신분에 따른 추천 장소 설정
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

            // 입장 처리 (조용히)
            synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i] == this) {
                        clientName = "@" + name;
                        break;
                    }
                }
            }

            // ---------------------------------------------------------------
            // 3. 대화형 안내 로직
            // ---------------------------------------------------------------
            boolean startAutoDrive = false;

            os.println("==================================================");
            os.println("" + name + "님 차량 인식됨.");
            os.println("" + messageReason + " 안내해 드릴까요? (예 / 아니오)");
            os.println("==================================================");

            String answer = is.readLine(); 
            if (answer != null) answer = answer.trim();

            if ("예".equals(answer)) {
                os.println("네, " + targetSpot + " 구역으로 안내를 시작합니다.");
                startAutoDrive = true;

            } else {
                os.println("==================================================");
                os.println("어느 건물과 가까운 자리로 안내해드릴까요?");
                os.println("==================================================");

                String customPlace = is.readLine();
                if (customPlace != null) customPlace = customPlace.trim();

                if ("아니오".equals(customPlace)) {
                    os.println("안전한 운전 되세요!");
                    startAutoDrive = false; 
                } else {
                    targetSpot = customPlace + " 근처";
                    destX = 88; destY = 88; 
                    
                    os.println("[시스템] 네, 입력하신 '" + targetSpot + "' 로 안내를 수정합니다.");
                    startAutoDrive = true;
                }
            }

            // ---------------------------------------------------------------
            // 4. 실제 내비게이션 안내 (사용자별 맞춤 경로)
            // ---------------------------------------------------------------
            if (startAutoDrive) {
                os.println("\n===== 📡 [IoT 모드] 스마트 내비게이션 시작 =====");
                os.println("🗺️  경로 안내를 시작합니다...\n");
                
                try {
                    // 총 거리 계산
                    int totalDist = (int)Math.sqrt(destX * destX + destY * destY);
                    
                    // 1단계: 경로 정보
                    os.println("📍 총 거리: " + totalDist + "m");
                    os.println("⏱️  예상 소요 시간: " + (totalDist / 15) + "초");
                    os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    Thread.sleep(2000);
                    
                    // 2단계: 출발
                    os.println("🚗 주차장 입구에서 출발합니다.");
                    Thread.sleep(2000);
                    os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    
                    // 사용자 타입별 맞춤 경로 안내
                    if (name.contains("교수")) {
                        // 교수 - 본관 방향 (우회전 → 직진 → 좌측 진입)
                        os.println("➡️  20m 앞 본관 방향으로 우회전하세요.");
                        Thread.sleep(2500);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("🚗 연구동 방면으로 " + (totalDist / 2) + "m 직진 중...");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("⚠️  곧 교수 전용 구역입니다. 속도를 줄이세요.");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("🔄 10m 앞에서 좌측 교수 전용 주차구역으로 진입하세요.");
                        Thread.sleep(2500);
                        
                    } else if (name.contains("학생")) {
                        // 학생 - 강의동 방향 (좌회전 → 직진 → 우측 진입)
                        os.println("⬅️  15m 앞 명신관 방향으로 좌회전하세요.");
                        Thread.sleep(2500);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("🚗 강의동 방면으로 " + (totalDist / 2) + "m 직진 중...");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("⚠️  곧 일반 주차구역입니다. 속도를 줄이세요.");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("🔄 5m 앞에서 우측 일반 주차구역으로 진입하세요.");
                        Thread.sleep(2500);
                        
                    } else {
                        // 방문객 - 주차타워 방향 (직진 → 우회전 → 진입)
                        os.println("⬆️  주차타워 방향으로 직진하세요.");
                        Thread.sleep(2500);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("🚗 " + (totalDist / 3) + "m 직진 후 우회전 준비...");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("➡️  12m 앞에서 주차타워 방향으로 우회전하세요.");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("⚠️  곧 목적지입니다. 속도를 줄이세요.");
                        Thread.sleep(2000);
                        os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                        
                        os.println("🔄 8m 앞에서 주차타워 입구로 진입하세요.");
                        Thread.sleep(2500);
                    }
                    
                    os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                    
                    // 7단계: 도착
                    Thread.sleep(1000);
                    os.println("🎉 목적지 [" + targetSpot + "] 에 도착했습니다!");
                    os.println("🅿️  주차 공간을 확인하고 안전하게 주차하세요.");
                    os.println("🔒 주차 완료 후 시동을 끄고 차량을 잠가주세요.");
                    os.println("=============================================\n");

                } catch (InterruptedException e) {
                    os.println("[에러] 내비게이션 연결 끊김");
                }
            }

            // ---------------------------------------------------------------
            // 5. 채팅 모드 전환 옵션
            // ---------------------------------------------------------------
            os.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            os.println("💬 주차장 커뮤니티 채팅에 참여하시겠습니까?");
            os.println("   - 다른 운전자와 실시간 정보 공유");
            os.println("   - 관리자에게 문의/신고");
            os.println("   (참여: '예' 입력 / 종료: '/quit' 입력)");
            os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            String chatChoice = is.readLine();
            if (chatChoice != null && "예".equals(chatChoice.trim())) {
                os.println("✅ 채팅 채널에 연결되었습니다.");
                os.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                os.println("📋 사용 가능한 명령어:");
                os.println("   /report  - 관리자에게 신고/문의");
                os.println("   /help    - 도움 요청");
                os.println("   /quit    - 채팅 종료");
                os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            } else {
                os.println("👋 안전한 운전 되세요!");
                // 채팅 건너뛰고 종료
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
                return;
            }

            // ---------------------------------------------------------------
            // 6. 일반 채팅 및 수동 명령어 루프
            // ---------------------------------------------------------------

            while (true) {
                String line = is.readLine();
                if (line == null || line.startsWith("/quit")) {
                    break;
                }

                // 신고 기능
                if (line.startsWith("/report")) {
                    os.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    os.println("🚨 관리자 신고/문의");
                    os.println("신고 내용을 입력하세요:");
                    os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    
                    String reportContent = is.readLine();
                    if (reportContent != null && !reportContent.trim().isEmpty()) {
                        // 관리자에게만 전송 (실제로는 관리자 세션 확인 필요)
                        os.println("✅ 신고가 접수되었습니다. 관리자가 확인 중입니다.");
                        os.println("   신고 내용: " + reportContent);
                        
                        // 관리자 알림 (모든 사용자에게 브로드캐스트)
                        synchronized (this) {
                            for (int i = 0; i < maxClientsCount; i++) {
                                if (threads[i] != null) {
                                    threads[i].os.println("\n🔔 [관리자 알림] " + name + "님의 신고: " + reportContent + "\n");
                                }
                            }
                        }
                    }
                    continue;
                }
                
                // 도움 요청
                if (line.startsWith("/help")) {
                    os.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    os.println("🆘 긴급 도움 요청");
                    os.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    os.println("관리자가 현재 위치로 출동합니다.");
                    os.println("위치: " + targetSpot);
                    
                    // 긴급 알림
                    synchronized (this) {
                        for (int i = 0; i < maxClientsCount; i++) {
                            if (threads[i] != null) {
                                threads[i].os.println("\n🚨 [긴급] " + name + "님이 " + targetSpot + "에서 도움을 요청했습니다!\n");
                            }
                        }
                    }
                    continue;
                }

                // 수동 이동 기능
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
            }

            // 퇴장 처리
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