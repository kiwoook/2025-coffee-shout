package coffeeshout.lab;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class CompletableFutureTest {

    @Test
    void 특정_시간_이후에_작업을_순차적으로_실행한다() throws InterruptedException {
        // given
        CompletableFuture<Void> future1 = CompletableFuture
                .runAsync(() -> System.out.println("게임 시작"))
                .thenRunAsync(() -> System.out.println("🕒 10초 지남"),
                        CompletableFuture.delayedExecutor(10, TimeUnit.SECONDS));

        CompletableFuture<Void> future2 = future1.thenCompose(ignored ->
                CompletableFuture.runAsync(() -> System.out.println("로딩 시작"))
                        .thenRunAsync(() -> System.out.println("🕒 3초 지남"),
                                CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS))
        );
        future2.join();
    }

    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler() {{
        this.setPoolSize(1);
        this.setThreadNamePrefix("my-task-");
        this.initialize();
    }};

    @Test
    @DisplayName("스케줄러 사용")
    void 특정_시간_이후에_작업을_순차적으로_실행한다2() throws InterruptedException {
        // given
        List<ScheduledFuture<Void>> scheduledFutures = new ArrayList<>();

        ScheduledFuture<?> future1 = scheduler.schedule(
                () -> System.out.println("🟢 실행됨"),
                triggerContext -> new Date(System.currentTimeMillis() + 5000).toInstant()
        );
        ScheduledFuture<?> future2 = scheduler.schedule(
                () -> System.out.println("🟢 실행됨2"),
                triggerContext -> new Date(System.currentTimeMillis() + 5000).toInstant()
        );
        Thread.sleep(7000);
    }
}
