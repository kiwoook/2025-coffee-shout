package coffeeshout.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebSocketThreadTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("여러 웹소켓 연결이 같은 쓰레드에서 처리되는지 확인")
    void 여러_웹소켓_연결_쓰레드_확인() throws Exception {
        int connectionCount = 5;
        CountDownLatch connectionLatch = new CountDownLatch(connectionCount);
        Set<String> threadNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
        
        for (int i = 0; i < connectionCount; i++) {
            Thread connectionThread = new Thread(() -> {
                try {
                    WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(
                            new WebSocketTransport(new StandardWebSocketClient())
                    )));
                    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
                    
                    String url = "ws://localhost:" + port + "/ws";
                    
                    StompSession session = stompClient.connectAsync(url, 
                            new ThreadTrackingStompSessionHandler(connectionLatch, threadNames))
                            .get(10, TimeUnit.SECONDS);
                    
                    Thread.sleep(1000);
                    session.disconnect();
                } catch (Exception e) {
                    System.err.println("연결 에러: " + e.getMessage());
                    connectionLatch.countDown();
                }
            });
            connectionThread.start();
        }
        
        assertThat(connectionLatch.await(15, TimeUnit.SECONDS)).isTrue();
        
        System.out.println("=== 웹소켓 연결 쓰레드 분석 결과 ===");
        System.out.println("총 연결 수: " + connectionCount);
        System.out.println("사용된 쓰레드 수: " + threadNames.size());
        System.out.println("쓰레드 목록:");
        threadNames.forEach(threadName -> System.out.println("  - " + threadName));
        
        if (threadNames.size() == 1) {
            System.out.println("✅ 모든 웹소켓 연결이 같은 쓰레드에서 처리됨");
        } else {
            System.out.println("❌ 웹소켓 연결이 여러 쓰레드에서 처리됨");
        }
    }

    @Test
    @DisplayName("동시 웹소켓 연결 시 쓰레드 처리 방식 확인")
    void 동시_웹소켓_연결_쓰레드_처리방식_확인() throws Exception {
        int connectionCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch connectionLatch = new CountDownLatch(connectionCount);
        Set<String> connectionThreadNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Set<String> messageProcessingThreadNames = Collections.newSetFromMap(new ConcurrentHashMap<>());
        
        for (int i = 0; i < connectionCount; i++) {
            int connectionId = i;
            Thread connectionThread = new Thread(() -> {
                try {
                    startLatch.await();
                    
                    WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(List.of(
                            new WebSocketTransport(new StandardWebSocketClient())
                    )));
                    stompClient.setMessageConverter(new MappingJackson2MessageConverter());
                    
                    String url = "ws://localhost:" + port + "/ws";
                    System.out.println(String.format("클라이언트 스레드: %s", Thread.currentThread().getName()));
                    
                    StompSession session = stompClient.connectAsync(url, 
                            new DetailedThreadTrackingHandler(connectionLatch, connectionThreadNames, messageProcessingThreadNames, connectionId))
                            .get(10, TimeUnit.SECONDS);
                    
                    session.subscribe("/topic/room/" + connectionId, new TestFrameHandler(messageProcessingThreadNames));
                    
                    Thread.sleep(2000);
                    session.disconnect();
                } catch (Exception e) {
                    System.err.println("연결 " + connectionId + " 에러: " + e.getMessage());
                    connectionLatch.countDown();
                }
            });
            connectionThread.start();
        }
        
        startLatch.countDown();
        
        assertThat(connectionLatch.await(20, TimeUnit.SECONDS)).isTrue();
        
        System.out.println("=== 동시 웹소켓 연결 쓰레드 분석 결과 ===");
        System.out.println("총 연결 수: " + connectionCount);
        System.out.println("연결 처리 쓰레드 수: " + connectionThreadNames.size());
        System.out.println("메시지 처리 쓰레드 수: " + messageProcessingThreadNames.size());
        
        System.out.println("\n연결 처리 쓰레드 목록:");
        connectionThreadNames.forEach(threadName -> System.out.println("  - " + threadName));
        
        System.out.println("\n메시지 처리 쓰레드 목록:");
        messageProcessingThreadNames.forEach(threadName -> System.out.println("  - " + threadName));
        
        if (connectionThreadNames.size() == 1) {
            System.out.println("\n✅ 모든 웹소켓 연결이 동일한 쓰레드에서 처리됨");
        } else {
            System.out.println("\n❌ 웹소켓 연결이 여러 쓰레드에서 처리됨 (쓰레드 풀 사용)");
        }
    }

    private static class ThreadTrackingStompSessionHandler implements StompSessionHandler {
        private final CountDownLatch latch;
        private final Set<String> threadNames;

        public ThreadTrackingStompSessionHandler(CountDownLatch latch, Set<String> threadNames) {
            this.latch = latch;
            this.threadNames = threadNames;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            String threadName = Thread.currentThread().getName();
            threadNames.add(threadName);
            System.out.println("연결 성공 - 쓰레드: " + threadName);
            latch.countDown();
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                  StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("STOMP 에러: " + exception.getMessage());
            latch.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("전송 에러: " + exception.getMessage());
            latch.countDown();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            String threadName = Thread.currentThread().getName();
            threadNames.add(threadName);
            System.out.println("프레임 처리 - 쓰레드: " + threadName);
        }
    }

    private static class DetailedThreadTrackingHandler implements StompSessionHandler {
        private final CountDownLatch latch;
        private final Set<String> connectionThreadNames;
        private final Set<String> messageProcessingThreadNames;
        private final int connectionId;

        public DetailedThreadTrackingHandler(CountDownLatch latch, Set<String> connectionThreadNames, 
                                           Set<String> messageProcessingThreadNames, int connectionId) {
            this.latch = latch;
            this.connectionThreadNames = connectionThreadNames;
            this.messageProcessingThreadNames = messageProcessingThreadNames;
            this.connectionId = connectionId;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            String threadName = Thread.currentThread().getName();
            connectionThreadNames.add(threadName);
            System.out.println("연결 " + connectionId + " 성공 - 연결 쓰레드: " + threadName);
            latch.countDown();
        }

        @Override
        public void handleException(StompSession session, StompCommand command,
                                  StompHeaders headers, byte[] payload, Throwable exception) {
            System.err.println("연결 " + connectionId + " STOMP 에러: " + exception.getMessage());
            latch.countDown();
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            System.err.println("연결 " + connectionId + " 전송 에러: " + exception.getMessage());
            latch.countDown();
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            String threadName = Thread.currentThread().getName();
            messageProcessingThreadNames.add(threadName);
            System.out.println("연결 " + connectionId + " 프레임 처리 - 메시지 처리 쓰레드: " + threadName);
        }
    }

    private static class TestFrameHandler implements org.springframework.messaging.simp.stomp.StompFrameHandler {
        private final Set<String> messageProcessingThreadNames;

        public TestFrameHandler(Set<String> messageProcessingThreadNames) {
            this.messageProcessingThreadNames = messageProcessingThreadNames;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            String threadName = Thread.currentThread().getName();
            messageProcessingThreadNames.add(threadName);
            System.out.println("토픽 메시지 수신 - 쓰레드: " + threadName + ", 메시지: " + payload);
        }
    }
}
