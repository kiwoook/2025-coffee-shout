package coffeeshout.lab;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

class ScheduledFutureExampleTest {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledFutureExampleTest.class);
    
    @Test
    void understandScheduledFuture() throws InterruptedException {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setCorePoolSize(2);
        scheduler.setThreadNamePrefix("example-");
        scheduler.initialize();
        
        logger.info("=== ScheduledFuture 동작 원리 이해 ===");
        
        // 1. 스케줄링 시점 - 이미 스케줄러 내부 큐에 등록됨
        logger.info("Step 1: 작업 스케줄링 중...");
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            logger.info("실제 작업 실행! 스레드: {}", Thread.currentThread().getName());
        }, Instant.now().plusMillis(2000)); // 2초 후 실행
        
        logger.info("Step 2: 스케줄링 완료 - future 객체 반환됨");
        logger.info("  - isDone(): {}", future.isDone());
        logger.info("  - isCancelled(): {}", future.isCancelled());
        logger.info("  - 스케줄러 내부에 이미 등록됨!");
        
        // 2. 대기 중 상태 확인
        Thread.sleep(1000);
        logger.info("Step 3: 1초 경과 - 아직 실행 전");
        logger.info("  - isDone(): {}", future.isDone());
        logger.info("  - 스케줄러가 내부적으로 시간 체크 중...");
        
        // 3. 실행 완료 후 확인
        Thread.sleep(2000);
        logger.info("Step 4: 실행 완료 후");
        logger.info("  - isDone(): {}", future.isDone());
        logger.info("  - 작업이 완료되어 future 상태 변경됨");
        
        scheduler.shutdown();
    }
    
    @Test
    void schedulerInternalWorkings() throws InterruptedException {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setCorePoolSize(1);
        scheduler.setThreadNamePrefix("internal-");
        scheduler.initialize();
        
        logger.info("=== 스케줄러 내부 동작 과정 ===");
        
        // 여러 작업을 다른 시간에 스케줄링
        logger.info("여러 작업 스케줄링...");
        
        ScheduledFuture<?> future1 = scheduler.schedule(() -> {
            logger.info("[작업1] 실행됨 - 스레드: {}", Thread.currentThread().getName());
        }, Instant.now().plusMillis(1000));
        
        ScheduledFuture<?> future2 = scheduler.schedule(() -> {
            logger.info("[작업2] 실행됨 - 스레드: {}", Thread.currentThread().getName());
        }, Instant.now().plusMillis(500));
        
        ScheduledFuture<?> future3 = scheduler.schedule(() -> {
            logger.info("[작업3] 실행됨 - 스레드: {}", Thread.currentThread().getName());
        }, Instant.now().plusMillis(1500));
        
        logger.info("모든 작업이 스케줄러 내부 큐에 등록됨:");
        logger.info("  - Future1 (1초 후): isDone={}", future1.isDone());
        logger.info("  - Future2 (0.5초 후): isDone={}", future2.isDone());
        logger.info("  - Future3 (1.5초 후): isDone={}", future3.isDone());
        
        // 스케줄러가 내부적으로 시간순 정렬하여 실행
        logger.info("스케줄러가 시간 순서대로 실행할 예정:");
        logger.info("  1. Future2 (0.5초 후) -> Future1 (1초 후) -> Future3 (1.5초 후)");
        
        Thread.sleep(2000);
        
        logger.info("실행 완료 후 상태:");
        logger.info("  - Future1 완료: {}", future1.isDone());
        logger.info("  - Future2 완료: {}", future2.isDone());
        logger.info("  - Future3 완료: {}", future3.isDone());
        
        scheduler.shutdown();
    }
    
    @Test
    void futureControlExample() throws InterruptedException {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setCorePoolSize(1);
        scheduler.setThreadNamePrefix("control-");
        scheduler.initialize();
        
        logger.info("=== ScheduledFuture 제어 예제 ===");
        
        // 긴 작업 스케줄링
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            logger.info("긴 작업 시작...");
            try {
                Thread.sleep(3000); // 3초 작업
                logger.info("긴 작업 완료!");
            } catch (InterruptedException e) {
                logger.info("작업이 중단됨!");
            }
        }, Instant.now().plusMillis(1000)); // 1초 후 시작
        
        logger.info("작업이 스케줄됨. 2초 후 취소 예정...");
        
        // 2초 후 작업 취소
        Thread.sleep(2000);
        boolean cancelled = future.cancel(true); // 실행 중이어도 강제 중단
        logger.info("작업 취소 결과: {}", cancelled);
        logger.info("현재 상태 - isDone: {}, isCancelled: {}", 
                   future.isDone(), future.isCancelled());
        
        Thread.sleep(2000);
        scheduler.shutdown();
    }
}