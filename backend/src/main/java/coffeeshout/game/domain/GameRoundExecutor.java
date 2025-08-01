package coffeeshout.game.domain;

import org.springframework.scheduling.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;

public class GameRoundExecutor {
    private static final Logger logger = LoggerFactory.getLogger(GameRoundExecutor.class);
    
    private final String gameId;
    private final TaskScheduler scheduler;
    private final GameListener gameListener;
    
    private final Queue<GameRound> rounds = new ConcurrentLinkedQueue<>();
    private final Map<Integer, ScheduledFuture<?>> roundFutures = new ConcurrentHashMap<>();

    private volatile GameRound currentRound;
    private volatile boolean gameRunning = false;
    private volatile boolean gamePaused = false;
    
    public GameRoundExecutor(String gameId, TaskScheduler scheduler, GameListener gameListener) {
        this.gameId = gameId;
        this.scheduler = scheduler;
        this.gameListener = gameListener;
    }
    
    public void addRound(GameRound round) {
        rounds.offer(round);
        logger.debug("Round {} added to game {}", round.getRoundNumber(), gameId);
    }
    
    public synchronized void startGame() {
        if (!gameRunning && !rounds.isEmpty()) {
            gameRunning = true;
            gameListener.onGameStart(gameId);
            logger.info("Game {} started with {} rounds", gameId, rounds.size());
            processNextRound();
        }
    }
    
    private void processNextRound() {
        if (!gameRunning || gamePaused) {
            return;
        }
        
        GameRound nextRound = rounds.poll();
        if (nextRound != null) {
            currentRound = nextRound;
            scheduleRound(nextRound);
        } else {
            finishGame();
        }
    }
    
    private void scheduleRound(GameRound round) {
        logger.debug("Scheduling round {} for game {} with delay {}ms", 
                    round.getRoundNumber(), gameId, round.getDuration());
        
        ScheduledFuture<?> future = scheduler.schedule(() -> executeRound(round), Instant.now().plusMillis(round.getDuration()));
        
        roundFutures.put(round.getRoundNumber(), future);
    }
    
    private void executeRound(GameRound round) {
        try {
            round.setStatus(GameRound.RoundStatus.RUNNING);
            round.setStartTime(System.currentTimeMillis());
            
            gameListener.onRoundStart(gameId, round);
            logger.info("Executing round {} for game {}", round.getRoundNumber(), gameId);
            
            round.getRoundLogic().run();
            
            round.setStatus(GameRound.RoundStatus.COMPLETED);
            round.setEndTime(System.currentTimeMillis());
            
            gameListener.onRoundEnd(gameId, round);
            logger.info("Round {} completed for game {} in {}ms", 
                       round.getRoundNumber(), gameId, 
                       round.getEndTime() - round.getStartTime());
            
        } catch (Exception e) {
            round.setStatus(GameRound.RoundStatus.FAILED);
            round.setEndTime(System.currentTimeMillis());
            
            gameListener.onRoundError(gameId, round, e);
            logger.error("Round {} failed for game {}: {}", 
                        round.getRoundNumber(), gameId, e.getMessage(), e);
        } finally {
            processNextRound();
        }
    }
    
    public synchronized void pauseGame() {
        gamePaused = true;
        logger.info("Game {} paused", gameId);
    }
    
    public synchronized void resumeGame() {
        if (gamePaused) {
            gamePaused = false;
            logger.info("Game {} resumed", gameId);
            if (gameRunning) {
                processNextRound();
            }
        }
    }
    
    public void cancelRound(int roundNumber) {
        ScheduledFuture<?> future = roundFutures.get(roundNumber);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            roundFutures.remove(roundNumber);
            logger.info("Round {} cancelled for game {}", roundNumber, gameId);
        }
        
        rounds.removeIf(round -> round.getRoundNumber() == roundNumber);
    }
    
    public synchronized void stopGame() {
        gameRunning = false;
        gamePaused = false;
        
        roundFutures.values().forEach(future -> future.cancel(false));
        roundFutures.clear();
        rounds.clear();
        
        gameListener.onGameCancelled(gameId);
        logger.info("Game {} stopped", gameId);
    }
    
    private void finishGame() {
        gameRunning = false;
        gameListener.onGameComplete(gameId);
        logger.info("Game {} completed", gameId);
    }
    
    public boolean isGameRunning() { 
        return gameRunning; 
    }
    
    public boolean isGamePaused() { 
        return gamePaused; 
    }
    
    public GameRound getCurrentRound() { 
        return currentRound; 
    }
    
    public int getRemainingRounds() { 
        return rounds.size(); 
    }
    
    public List<String> getPlayers() { 
        return new ArrayList<>(players); 
    }
    
    public String getGameId() { 
        return gameId; 
    }
}