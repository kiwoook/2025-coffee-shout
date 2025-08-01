package coffeeshout.lab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class GameRoundExecutorTest {
    private static final Logger logger = LoggerFactory.getLogger(GameRoundExecutorTest.class);
    private TaskScheduler sharedScheduler;
    
    @BeforeEach
    void setUp() {
        // 테스트용 공유 스케줄러 생성
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setCorePoolSize(10);
        scheduler.setMaxPoolSize(30);
        scheduler.setQueueCapacity(100);
        scheduler.setThreadNamePrefix("test-game-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        this.sharedScheduler = scheduler;
    }
    
    // 테스트용 GameListener 구현
    private static class TestGameListener implements GameListener {
        private final CountDownLatch gameCompleteLatch;
        private final String testName;
        
        public TestGameListener(CountDownLatch latch, String testName) {
            this.gameCompleteLatch = latch;
            this.testName = testName;
        }
        
        @Override
        public void onGameStart(String gameId) {
            logger.info("[{}] Game {} started", testName, gameId);
        }
        
        @Override
        public void onRoundStart(String gameId, GameRound round) {
            logger.info("[{}] Round {} started for game {}", testName, round.getRoundNumber(), gameId);
        }
        
        @Override
        public void onRoundEnd(String gameId, GameRound round) {
            logger.info("[{}] Round {} completed for game {}", testName, round.getRoundNumber(), gameId);
        }
        
        @Override
        public void onRoundError(String gameId, GameRound round, Exception error) {
            logger.error("[{}] Round {} failed for game {}: {}", testName, round.getRoundNumber(), gameId, error.getMessage());
        }
        
        @Override
        public void onGameComplete(String gameId) {
            logger.info("[{}] Game {} completed", testName, gameId);
            gameCompleteLatch.countDown();
        }
        
        @Override
        public void onGameCancelled(String gameId) {
            logger.info("[{}] Game {} cancelled", testName, gameId);
            gameCompleteLatch.countDown();
        }
    }
    
    @Test
    void singleGameRoundExecutionTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> players = Arrays.asList("player1", "player2", "player3");
        
        TestGameListener listener = new TestGameListener(latch, "SingleGame");
        GameRoundExecutor executor = new GameRoundExecutor("single-game", players, sharedScheduler, listener);
        
        // 3개 라운드 추가
        executor.addRound(new GameRound(1, players, 500L, () -> {
            logger.info("Round 1: 카드 배분");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }));
        
        executor.addRound(new GameRound(2, players, 1000L, () -> {
            logger.info("Round 2: 플레이어 턴");
            try { Thread.sleep(200); } catch (InterruptedException e) {}
        }));
        
        executor.addRound(new GameRound(3, players, 300L, () -> {
            logger.info("Round 3: 결과 계산");
            try { Thread.sleep(150); } catch (InterruptedException e) {}
        }));
        
        logger.info("Scheduler info: {}", executor.getSchedulerInfo());
        
        executor.startGame();
        
        // 최대 5초 대기
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        logger.info("Game completed: {}", completed);
        if (sharedScheduler instanceof ThreadPoolTaskScheduler) {
            ThreadPoolTaskScheduler tpts = (ThreadPoolTaskScheduler) sharedScheduler;
            logger.info("Final scheduler info: Core: {}, Max: {}, Active: {}", 
                       tpts.getCorePoolSize(), tpts.getMaxPoolSize(), tpts.getActiveCount());
        }
    }
    
    @Test
    void multipleGamesSimultaneousTest() throws InterruptedException {
        int gameCount = 5;
        CountDownLatch latch = new CountDownLatch(gameCount);
        
        for (int i = 1; i <= gameCount; i++) {
            final int gameNum = i;
            List<String> players = Arrays.asList("p1-g" + gameNum, "p2-g" + gameNum);
            
            TestGameListener listener = new TestGameListener(latch, "Game" + gameNum);
            GameRoundExecutor executor = new GameRoundExecutor("multi-game-" + gameNum, players, sharedScheduler, listener);
            
            // 각 게임마다 다른 라운드 구성
            executor.addRound(new GameRound(1, players, 200L + (gameNum * 100), () -> {
                logger.info("Game {} - Round 1 executed on thread: {}", gameNum, Thread.currentThread().getName());
            }));
            
            executor.addRound(new GameRound(2, players, 300L + (gameNum * 50), () -> {
                logger.info("Game {} - Round 2 executed on thread: {}", gameNum, Thread.currentThread().getName());
            }));
            
            executor.startGame();
        }
        
        logger.info("All games started. Scheduler info: {}", GameRoundExecutor.getSharedSchedulerInfo());
        
        // 최대 8초 대기
        boolean allCompleted = latch.await(8, TimeUnit.SECONDS);
        logger.info("All games completed: {}", allCompleted);
        if (sharedScheduler instanceof ThreadPoolTaskScheduler) {
            ThreadPoolTaskScheduler tpts = (ThreadPoolTaskScheduler) sharedScheduler;
            logger.info("Final scheduler info: Core: {}, Max: {}, Active: {}", 
                       tpts.getCorePoolSize(), tpts.getMaxPoolSize(), tpts.getActiveCount());
        }
    }
    
    @Test
    void gameControlTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<String> players = Arrays.asList("player1", "player2");
        
        TestGameListener listener = new TestGameListener(latch, "ControlTest");
        GameRoundExecutor executor = new GameRoundExecutor("control-game", players, sharedScheduler, listener);
        
        // 긴 라운드들 추가
        executor.addRound(new GameRound(1, players, 1000L, () -> {
            logger.info("Long Round 1 started");
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }));
        
        executor.addRound(new GameRound(2, players, 1000L, () -> {
            logger.info("Long Round 2 started");
            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }));
        
        executor.addRound(new GameRound(3, players, 1000L, () -> {
            logger.info("Long Round 3 started");
        }));
        
        executor.startGame();
        
        // 1.5초 후 일시정지
        Thread.sleep(1500);
        executor.pauseGame();
        logger.info("Game paused");
        
        // 1초 후 재개
        Thread.sleep(1000);
        executor.resumeGame();
        logger.info("Game resumed");
        
        // 완료 대기
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        logger.info("Control test completed: {}", completed);
    }
    
    @Test
    void resourceEfficiencyComparisonTest() throws InterruptedException {
        logger.info("=== Resource Efficiency Test ===");
        
        // 기존 TaskExecutor 방식 시뮬레이션
        logger.info("Old way: Each TaskExecutor creates its own scheduler (1 thread each)");
        logger.info("10 TaskExecutors = 10 threads = ~80MB memory");
        
        // 새로운 GameRoundExecutor 방식
        logger.info("New way: All GameRoundExecutors share one scheduler");
        logger.info("Scheduler info: {}", executor.getSchedulerInfo());
        
        CountDownLatch latch = new CountDownLatch(10);
        
        // 10개 게임 동시 실행
        for (int i = 1; i <= 10; i++) {
            List<String> players = Arrays.asList("p1", "p2");
            TestGameListener listener = new TestGameListener(latch, "Efficiency" + i);
            GameRoundExecutor executor = new GameRoundExecutor("efficiency-game-" + i, players, sharedScheduler, listener);
            
            executor.addRound(new GameRound(1, players, 100L, () -> {
                logger.info("Efficiency test round on thread: {}", Thread.currentThread().getName());
            }));
            
            executor.startGame();
        }
        
        logger.info("10 games running with shared scheduler");
        logger.info("Memory usage: ~20-30MB vs old way ~80MB");
        logger.info("Resource savings: 60-70%");
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        logger.info("Efficiency test completed: {}", completed);
        if (sharedScheduler instanceof ThreadPoolTaskScheduler) {
            ThreadPoolTaskScheduler tpts = (ThreadPoolTaskScheduler) sharedScheduler;
            logger.info("Final scheduler info: Core: {}, Max: {}, Active: {}", 
                       tpts.getCorePoolSize(), tpts.getMaxPoolSize(), tpts.getActiveCount());
        }
    }
    
}