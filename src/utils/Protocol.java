package utils;

public class Protocol {
    // 명령어 프로토콜 정의
    public static final String LOGIN_LPR = "LOGIN:LPR";       // LPR 카메라 로그인 헤더
    public static final String LOGIN_USER = "LOGIN:USER:";    // 유저 앱 로그인 헤더 (뒤에 차번호 붙음)
    public static final String DETECT_CAR = "DETECT:";        // LPR이 차량 인식 시 보내는 헤더
    public static final String MSG_PAYMENT = "NOTI:PAYMENT_SUCCESS"; // 결제 완료 알림 메시지

    // 종료 메시지
    public static final String CMD_EXIT = "/quit";

    // 결제 수단 관련
    public static final String NOTI_NEED_PAYMENT = "NOTI:NEED_PAYMENT"; // 결제 수단 없음 알림
    public static final String REQ_REG_PAYMENT = "REQ:REG_PAYMENT:";    // 결제 수단 등록 요청
}
