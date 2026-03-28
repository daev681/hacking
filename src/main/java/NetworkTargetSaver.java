import java.io.*;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NetworkTargetSaver {
    public static void main(String[] args) {
        String subnet = "192.168.40"; //
        String fileName = "targets.txt";

        System.out.println("[*] 네트워크 정찰 시작 (" + subnet + ".x)");
        System.out.println("[*] 결과는 " + fileName + "에 저장됩니다.");

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            // 실행 시간 기록 (언제 켜져 있었는지 확인용)
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writer.println("\n========== 스캔 기록 [" + timeStamp + "] ==========");

            int foundCount = 0;
            for (int i = 1; i <= 254; i++) {
                String host = subnet + "." + i;
                try {
                    InetAddress addr = InetAddress.getByName(host);
                    // 150ms 정도로 약간 여유를 주면 더 정확하게 잡힙니다.
                    if (addr.isReachable(150)) {
                        String info = "[+] 발견: " + host + " (이름: " + addr.getHostName() + ")";
                        System.out.println(info);
                        writer.println(info);
                        foundCount++;
                    }
                } catch (Exception e) {
                    // 무시
                }

                // 진행률 표시
                if (i % 50 == 0) System.out.println(">> " + i + "/254 진행 중...");
            }

            writer.println("총 발견 기기: " + foundCount + "대");
            System.out.println("\n[*] 스캔 완료! 총 " + foundCount + "대를 찾았습니다.");

        } catch (IOException e) {
            System.err.println("[!] 파일 저장 중 에러 발생: " + e.getMessage());
        }
    }
}