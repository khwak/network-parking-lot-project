import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;

// the Server class
public class MultiThreadChatServerSync {
	// The server socket.
	private static ServerSocket serverSocket = null;
	// The client socket.
	private static Socket clientSocket = null;

	// This chat server can accept up to maxClientsCount clients' connections.
	private static final int maxClientsCount = 10;
	private static final ClientThread[] threads = new ClientThread[maxClientsCount];

	public static void main(String args[]) {

		// The default port number.
		int portNumber = 8888;
		if (args.length < 1) {
			System.out.println("Usage: java MultiThreadChatServerSync <portNumber>\n"
								+ "Now using port number=" + portNumber);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
		}

		/*
		* Open a server socket on the portNumber(default 8888). 
		* Note that we can not choose a port less than 1023 if we are not privileged users (root).
		*/
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
				System.out.println(e);
		}

		/* Create a client socket for each connection and pass it to a new client thread. */
		while (true) {
			try {
				clientSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxClientsCount; i++) {
					if (threads[i] == null) {
						(threads[i] = new ClientThread(clientSocket, threads)).start();
						break;
					}
				}
				if (i == maxClientsCount) {
					PrintStream os = new PrintStream(clientSocket.getOutputStream());
					os.println("Server too busy. Try later.");
					os.close();
					clientSocket.close();
				}
			} catch (IOException e) { 
				System.out.println(e);
			}
		}
	}  
}
// ==========================================
// 여기서부터 주차장 관리 시스템 (복사해서 붙여넣으세요)
// ==========================================

// 주차면 정보 클래스
class ParkingSpot {
    String id;
    int x, y;
    boolean isOccupied;

    public ParkingSpot(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.isOccupied = false;
    }
}

// 주차장 매니저 (서버의 두뇌)
class ParkingManager {
    static List<ParkingSpot> spots = new ArrayList<>();
    static Map<String, int[]> destinations = new HashMap<>(); 
    static Map<String, ParkingSpot> userAssignments = new HashMap<>();

    static {
        // 1. 주차면 데이터 (가상 좌표)
        spots.add(new ParkingSpot("A-1 (장애인/임산부)", 10, 10)); 
        spots.add(new ParkingSpot("B-1 (일반)", 50, 80));
        spots.add(new ParkingSpot("C-1 (일반)", 80, 20));
        spots.add(new ParkingSpot("D-1 (전기차)", 90, 90));

        // 2. 목적지 좌표 (교수님 연구실, 강의실 등)
        destinations.put("교수", new int[]{10, 15}); 
        destinations.put("학생", new int[]{80, 80}); 
    }

    public static String recommendSpot(String userName, String userType) {
        int[] dest = destinations.getOrDefault(userType, new int[]{50, 50});
        ParkingSpot bestSpot = null;
        double minDistance = Double.MAX_VALUE;

        for (ParkingSpot spot : spots) {
            if (!spot.isOccupied) {
                double dist = Math.sqrt(Math.pow(spot.x - dest[0], 2) + Math.pow(spot.y - dest[1], 2));
                if (dist < minDistance) {
                    minDistance = dist;
                    bestSpot = spot;
                }
            }
        }

        if (bestSpot != null) {
            bestSpot.isOccupied = true;
            userAssignments.put(userName, bestSpot);
            return "추천 주차면: [" + bestSpot.id + "] (좌표: " + bestSpot.x + "," + bestSpot.y + ")";
        } else {
            return "현재 이용 가능한 빈 자리가 없습니다.";
        }
    }

    public static String getNavigation(String userName, int currentX, int currentY) {
        ParkingSpot target = userAssignments.get(userName);
        if (target == null) return "배정된 주차면이 없습니다. 먼저 /login [차량번호] [유형] 을 해주세요.";

        if (Math.abs(target.x - currentX) < 5 && Math.abs(target.y - currentY) < 5) {
            return "목적지에 도착했습니다! 주차를 완료해주세요.";
        }

        StringBuilder guide = new StringBuilder();
        int dist = (int)Math.sqrt(Math.pow(target.x - currentX, 2) + Math.pow(target.y - currentY, 2));
        guide.append("목표까지 거리: ").append(dist).append("m. ");
        
        if (target.x > currentX + 5) guide.append("동쪽(East) ");
        else if (target.x < currentX - 5) guide.append("서쪽(West) ");
        
        if (target.y > currentY + 5) guide.append("남쪽(South) ");
        else if (target.y < currentY - 5) guide.append("북쪽(North) ");
        
        guide.append("으로 이동하세요.");
        return guide.toString();
    }
}