import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

// For every client's connection we call this class
public class ClientThread extends Thread{
	private String clientName = null;
	private DataInputStream is = null;
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
                name = is.readLine().trim(); 
            } catch (Exception e) {
                name = "알수없음";
            }

            // 1. 교수/학생/일반인 구분 로직
            String targetSpot = "";      
            String messageReason = "";   

            if (name.contains("교수")) {
                targetSpot = "A-1 [연구실 전용 - 본관 서쪽]";
                messageReason = "교수님 환영합니다! 연구실과 가장 가까운 곳으로 안내해 드립니다.";
            } else if (name.contains("학생")) {
                targetSpot = "C-1 [명신관 - 강의동 근처]";
                messageReason = "학생이시군요! 수업 듣는 명신관과 가까운 곳으로 안내합니다.";
            } else {
                targetSpot = "B-1 [주차타워 - 일반 구역]";
                messageReason = "일반 방문객을 위한 넓은 주차 구역입니다.";
            }

            // 2. 환영 메시지 및 목표 전송
            os.println("==================================================");
            os.println("[시스템] " + name + "님 입차 확인되었습니다.");
            os.println("[시스템] " + messageReason);
            os.println("[시스템] 🚩 목표 주차면: " + targetSpot);
            os.println("==================================================");
            os.println("[시스템] 길안내를 시작하려면 좌표를 입력하세요. (예: /move 0 0)");
            while (true) {
                String line = is.readLine();
                if (line == null || line.startsWith("/quit")) {
                    break;
                }

                // ----------------------------------------------------
                // [수정] 주차 시스템 로직 시작
                // ----------------------------------------------------
                
                // 1. 입차 (로그인) : /login 1234 교수
                if (line.startsWith("/login")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        String carNum = parts[1];
                        String type = parts[2];
                        this.clientName = "@" + carNum; // 식별자 설정

                        // 서버의 두뇌(ParkingManager)에게 물어봄
                        String recommendation = ParkingManager.recommendSpot(this.clientName, type);
                        
                        os.println("=========================================");
                        os.println("[시스템] " + type + "님(" + carNum + ") 입차 확인.");
                        os.println("[시스템] " + recommendation);
                        os.println("[시스템] 길안내를 시작하려면 좌표를 입력하세요. (예: /move 0 0)");
                        os.println("=========================================");
                    } else {
                        os.println("[오류] 입력 형식: /login [차량번호] [유형:교수/학생]");
                    }
                } 
                // 2. 이동 (센서 좌표 수신) : /move 10 20
                else if (line.startsWith("/move")) {
                    if (this.clientName == null) {
                        os.println("[시스템] 먼저 로그인을 해주세요.");
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 3) {
                        try {
                            int curX = Integer.parseInt(parts[1]);
                            int curY = Integer.parseInt(parts[2]);
                            
                            // 서버의 두뇌에게 길안내 요청
                            String guideMsg = ParkingManager.getNavigation(this.clientName, curX, curY);
                            os.println("[내비게이션] " + guideMsg);
                        } catch (Exception e) {
                            os.println("[오류] 좌표는 숫자여야 합니다. (예: /move 10 20)");
                        }
                    }
                }
                // 3. 그 외 채팅 (디버깅용)
                else {
                    os.println("[서버] 알 수 없는 명령어입니다. (/login 또는 /move 사용)");
                }
            }

			/* Welcome the new the client. */
			os.println("Welcome " + name + " to our chat room.\nTo leave enter /quit in a new line.");
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] == this) {
						clientName = "@" + name;
						break;
					}
				}
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this) {
						threads[i].os.println("*** A new user " + name
								+ " entered the chat room !!! ***");
					}
				}
			}

			/* Start the conversation. */
			while (true) {
				String line = is.readLine();
				if (line.startsWith("/quit")) {
					break;
				}

				/* If the message is private sent it to the given client. */
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
								/* Echo this message to let the client know the private message was sent. */
										this.os.println(">" + name + "> " + words[1]);
										break;
									}
								}
							}
						}
					}
				} else {
					/* The message is public, broadcast it to all other clients. */
					synchronized (this) {
						for (int i = 0; i < maxClientsCount; i++) {
							if (threads[i] != null && threads[i].clientName != null) {
								threads[i].os.println("<" + name + "> " + line);
							}
						}
					} // end of synchronized
				}
			}
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] != null && threads[i] != this
							&& threads[i].clientName != null) {
						threads[i].os.println("*** The user " + name
							+ " is leaving the chat room !!! ***");
					}
				}
			}// end of synchronized
			os.println("*** Bye " + name + " ***");

			/* Clean up. Set the current thread variable to null so that a new client could be accepted by the server.*/
			synchronized (this) {
				for (int i = 0; i < maxClientsCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}// end of synchronized
		  
			/* Close the output stream, close the input stream, close the socket. */
			is.close();
			os.close();
			clientSocket.close();
			} catch (IOException e) {
		}
	}
}