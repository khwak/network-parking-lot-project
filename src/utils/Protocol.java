package utils;

public class Protocol {
    // 출차 프로토콜 정의
    public static final String LOGIN_LPR = "LOGIN:LPR";       // LPR 카메라 로그인 헤더
    public static final String LOGIN_USER = "LOGIN:USER:";    // 유저 앱 로그인 헤더 (뒤에 차번호 붙음)
    public static final String DETECT_CAR = "DETECT:";        // LPR이 차량 인식 시 보내는 헤더
    public static final String MSG_PAYMENT = "NOTI:PAYMENT_SUCCESS"; // 결제 완료 알림 메시지
    public static final String CMD_EXIT = "/quit";            // 종료 메시지

<<<<<<< Updated upstream
    // 종료 메시지
    public static final String CMD_EXIT = "/quit";

    // 결제 수단 관련
    public static final String NOTI_NEED_PAYMENT = "NOTI:NEED_PAYMENT"; // 결제 수단 없음 알림
    public static final String REQ_REG_PAYMENT = "REQ:REG_PAYMENT:";    // 결제 수단 등록 요청
=======
    // 길 안내 프로토콜 정의
    public static final String REQ_NAV = "REQ:NAV";        // 길 안내 요청
    public static final String NAV_COORD = "NAV:COORD:";   // 이동 중 좌표
    public static final String NAV_END = "NAV:END";        // 도착 알림
>>>>>>> Stashed changes
}
