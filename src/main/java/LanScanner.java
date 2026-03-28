import java.net.InetAddress;

public class LanScanner {
    public static void main(String[] args) {
        String subnet = "192.168.40"; // 본인 서브넷 확인
        System.out.println("[*] 네트워크 기기 스캔 중...");

        for (int i = 1; i < 255; i++) {
            String host = subnet + "." + i;
            try {
                // 100ms 안에 응답 없으면 없는 기기로 간주 (속도 중시)
                if (InetAddress.getByName(host).isReachable(100)) {
                    System.out.println("[+] 발견: " + host);
                }
            } catch (Exception e) {}
        }
        System.out.println("[*] 스캔 완료.");
    }
}