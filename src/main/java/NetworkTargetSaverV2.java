import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NetworkTargetSaverV2 {
    public static void main(String[] args) {
        String subnet = "192.168.40"; // 원룸 메인 대역
        String fileName = "targetsV2.txt";

        System.out.println("[*] 1단계: 네트워크 기기 자극 중 (ARP 테이블 생성)...");
        pingAll(subnet);

        System.out.println("\n[*] 2단계: ARP 테이블 읽기 및 MAC 주소 저장 시작...");
        saveWithMac(subnet, fileName);
    }

    // 1. 먼저 핑을 날려 윈도우 ARP 테이블에 정보를 채웁니다.
    private static void pingAll(String subnet) {
        for (int i = 1; i <= 254; i++) {
            try {
                // 응답 여부와 상관없이 '시도'만 해도 ARP 테이블에 기록될 확률이 높습니다.
                InetAddress.getByName(subnet + "." + i).isReachable(100);
                if (i % 50 == 0) System.out.print(i + "..");
            } catch (Exception e) {}
        }
        System.out.println("\n[*] 자극 완료.");
    }

    // 2. 윈도우 arp 명령어를 실행해 결과를 파일에 씁니다.
    private static void saveWithMac(String subnet, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("\n========== 스캔 기록 (MAC 포함) [" + timeStamp + "] ==========");

            Process process = Runtime.getRuntime().exec("cmd.exe /c arp -a");
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "MS949"));

            String line;
            int foundCount = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // 내가 찾는 대역(40.x)이고 실제 기기(동적)인 경우만 저장
                if (line.contains(subnet) && !line.contains(".255") && line.contains("동적")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        String ip = parts[0];
                        String mac = parts[1].toUpperCase().replace("-", ":");

                        String info = String.format("[+] IP: %-15s | MAC: %s", ip, mac);
                        System.out.println(info);
                        writer.println(info);
                        foundCount++;
                    }
                }
            }
            writer.println("총 발견 기기: " + foundCount + "대");
            System.out.println("\n[*] 스캔 완료! " + fileName + "을 확인하세요.");

        } catch (Exception e) {
            System.err.println("[!] 에러: " + e.getMessage());
        }
    }
}