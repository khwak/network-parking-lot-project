package client;

<<<<<<< Updated upstream
=======
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
>>>>>>> Stashed changes
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;
import utils.Protocol;

public class UserApp {

<<<<<<< Updated upstream
    // 수신 전용 스레드 클래스 (내부 클래스)
    static class ReceiveThread extends Thread {
=======
    // UI 컴포넌트
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton btnRegCard; // 결제 수단 등록 버튼

    // 네트워크 변수
    private Socket socket;
    private PrintStream os;
    private String myCarNum;

    public UserApp() {
        // 1. 윈도우 설정
        setTitle("Smart Parking System - Client");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 2. 로그인 (차량 번호 입력)
        myCarNum = JOptionPane.showInputDialog(this, "차량 번호를 입력하세요:", "로그인", JOptionPane.QUESTION_MESSAGE);
        if (myCarNum == null || myCarNum.trim().isEmpty()) {
            System.exit(0);
        }

        // 3. 상단 패널: 결제 수단 등록 버튼
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRegCard = new JButton("💳 결제 수단 등록");
        btnRegCard.setBackground(new Color(255, 240, 200)); // 연한 주황색 강조
        btnRegCard.addActionListener(e -> registerPaymentMethod());
        topPanel.add(btnRegCard);
        add(topPanel, BorderLayout.NORTH);

        // 4. 중앙 패널: 로그/알림창
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        chatArea.append("[System] " + myCarNum + "님 환영합니다.\n");
        chatArea.append("[System] 출차 대기 모드입니다.\n");
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // 5. 하단 패널: 메시지 전송 (테스트용)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 이벤트 리스너
        ActionListener sendAction = e -> sendMessage();
        inputField.addActionListener(sendAction);
        sendButton.addActionListener(sendAction);

        setVisible(true);

        // 6. 서버 연결
        connectToServer();
    }

    // --- 결제 수단 등록 팝업 ---
    private void registerPaymentMethod() {
        String[] options = {"신용카드", "삼성페이", "카카오페이"};
        String selected = (String) JOptionPane.showInputDialog(
                this,
                "결제 수단을 선택해주세요:",
                "결제 수단 등록",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (selected != null && os != null) {
            // 서버에 등록 요청 전송
            os.println(Protocol.REQ_REG_PAYMENT + selected);
            chatArea.append("[Me] 결제 수단(" + selected + ") 등록 요청...\n");
        }
    }

    // --- 네트워크 연결 ---
    private void connectToServer() {
        String host = "10.101.17.50"; // ★ 서버 IP 확인 필요
        int port = 8888;

        try {
            socket = new Socket(host, port);
            os = new PrintStream(socket.getOutputStream());

            // 로그인 패킷 전송
            os.println(Protocol.LOGIN_USER + myCarNum);

            // 수신 스레드 시작
            new ReceiveThread(socket).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패: " + e.getMessage());
            System.exit(0);
        }
    }

    private void sendMessage() {
        String input = inputField.getText();
        if (input.isEmpty()) return;

        if (input.equalsIgnoreCase("/quit")) {
            os.println(Protocol.CMD_EXIT);
            try { socket.close(); } catch (IOException e) {}
            System.exit(0);
        } else {
            chatArea.append("[Me] " + input + "\n");
            // os.println(input); // 채팅 기능 필요하면 주석 해제
        }
        inputField.setText("");
    }

    // --- 수신 스레드 (서버 알림 처리) ---
    class ReceiveThread extends Thread {
>>>>>>> Stashed changes
        private BufferedReader reader;
        private Socket socket;

        public ReceiveThread(Socket socket) {
<<<<<<< Updated upstream
            this.socket = socket;
            try {
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
=======
            try { this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); }
            catch (IOException e) {}
>>>>>>> Stashed changes
        }

        @Override
        public void run() {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
<<<<<<< Updated upstream
                    // 서버로부터 온 메시지 출력
                    System.out.println("\n[App Alert] " + line);

                    // 결제 완료 메시지 수신 시 처리
                    if (line.equals(Protocol.MSG_PAYMENT)) {
                        System.out.println(">>> -------------------------------- <<<");
                        System.out.println(">>>  [알림] 자동 결제가 완료되었습니다.  <<<");
                        System.out.println(">>>      안녕히 가십시오 (출차 가능)     <<<");
                        System.out.println(">>> -------------------------------- <<<");
                        System.out.print("Input Command (/quit to exit): "); // 프롬프트 다시 출력
                    }
                }
            } catch (IOException e) {
                System.out.println("[System] Server disconnected.");
                System.exit(0);
=======
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> {

                        // 1. [알림] 결제 성공 -> 출차 가능
                        if (msg.equals(Protocol.MSG_PAYMENT)) {
                            chatArea.append(">>> [알림] 결제 완료! 출차 가능합니다. <<<\n");
                            JOptionPane.showMessageDialog(UserApp.this,
                                    "자동 결제가 완료되었습니다.\n안녕히 가십시오!",
                                    "출차 알림",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }

                        // 2. [경고] 결제 수단 없음 -> 등록 유도
                        else if (msg.equals(Protocol.NOTI_NEED_PAYMENT)) {
                            chatArea.append(">>> [경고] 결제 수단이 없습니다! <<<\n");
                            int ans = JOptionPane.showConfirmDialog(UserApp.this,
                                    "등록된 결제 수단이 없습니다.\n지금 등록하시겠습니까?",
                                    "결제 실패",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE);

                            if (ans == JOptionPane.YES_OPTION) {
                                registerPaymentMethod(); // 등록창 띄우기
                            }
                        }

                        // 3. 일반 메시지 로그
                        else {
                            chatArea.append("[Server] " + msg + "\n");
                            chatArea.setCaretPosition(chatArea.getDocument().getLength());
                        }
                    });
                }
            } catch (IOException e) {
                chatArea.append("[System] 연결이 끊어졌습니다.\n");
>>>>>>> Stashed changes
            }
        }
    }

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8888;
        Scanner sc = new Scanner(System.in);

        System.out.println("=== Smart Parking User App ===");
        System.out.print("Enter your Car Number to login: ");
        String myCarNum = sc.nextLine();

        try {
            Socket socket = new Socket(host, port);
            PrintStream os = new PrintStream(socket.getOutputStream());

            // 1. 로그인 패킷 전송
            os.println(Protocol.LOGIN_USER + myCarNum);

            // 2. 수신 스레드 시작 (서버 알림 대기)
            new ReceiveThread(socket).start();

            // 3. 메인 스레드는 사용자 입력 대기 (종료 명령용)
            while (true) {
                String input = sc.nextLine();
                if (input.equalsIgnoreCase("/quit")) {
                    os.println(Protocol.CMD_EXIT);
                    socket.close();
                    break;
                }
            }
            sc.close();
        } catch (IOException e) {
            System.out.println("Cannot connect to server: " + e.getMessage());
        }
    }
}