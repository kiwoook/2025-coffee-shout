package coffeeshout.game.application;

import coffeeshout.game.domain.GameBuilder;
import coffeeshout.game.domain.GameListener;
import coffeeshout.game.domain.GameRound;
import coffeeshout.game.domain.GameRoundExecutor;
import coffeeshout.game.domain.GameState;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class GameManager implements GameListener {
    private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

    @Autowired
    @Qualifier("gameTaskScheduler")
    private TaskScheduler gameTaskScheduler;

    private final Map<String, GameRoundExecutor> activeGames = new ConcurrentHashMap<>();
    private final Map<String, GameState> gameStates = new ConcurrentHashMap<>();

    public String createGame(List<String> players, List<GameRound> rounds) {
        String gameId = "game-" + UUID.randomUUID().toString().substring(0, 8);

        GameRoundExecutor executor = new GameBuilder()
                .gameId(gameId)
                .gameListener(this)
                .build(gameTaskScheduler);

        rounds.forEach(executor::addRound);

        GameState gameState = new GameState(gameId);
        gameState.setTotalRounds(rounds.size());

        activeGames.put(gameId, executor);
        gameStates.put(gameId, gameState);

        logger.info("Game {} created with {} players and {} rounds",
                gameId, players.size(), rounds.size());

        return gameId;
    }

    public String createGameWithBuilder(List<String> players) {
        String gameId = "game-" + UUID.randomUUID().toString().substring(0, 8);

        GameRoundExecutor executor = new GameBuilder()
                .gameId(gameId)
                .gameListener(this)
                .build(gameTaskScheduler);

        GameState gameState = new GameState(gameId);

        activeGames.put(gameId, executor);
        gameStates.put(gameId, gameState);

        logger.info("Game {} created with {} players (builder pattern)", gameId, players.size());

        return gameId;
    }

    public boolean addRoundToGame(String gameId, int roundNumber, long duration, Runnable roundLogic) {
        GameRoundExecutor executor = activeGames.get(gameId);
        GameState gameState = gameStates.get(gameId);

        if (executor != null && gameState != null) {
            GameRound round = new GameRound(roundNumber, duration, roundLogic);
            executor.addRound(round);
            gameState.setTotalRounds(gameState.getTotalRounds() + 1);
            logger.debug("Round {} added to game {}", roundNumber, gameId);
            return true;
        }
        return false;
    }

    public boolean startGame(String gameId) {
        GameRoundExecutor executor = activeGames.get(gameId);
        GameState gameState = gameStates.get(gameId);

        if (executor != null && gameState != null) {
            executor.startGame();
            gameState.setStatus(GameState.GameStatus.RUNNING);
            gameState.setStartedAt(new Date());
            return true;
        }
        return false;
    }

    public boolean pauseGame(String gameId) {
        GameRoundExecutor executor = activeGames.get(gameId);
        GameState gameState = gameStates.get(gameId);

        if (executor != null && gameState != null) {
            executor.pauseGame();
            gameState.setStatus(GameState.GameStatus.PAUSED);
            return true;
        }
        return false;
    }

    public boolean resumeGame(String gameId) {
        GameRoundExecutor executor = activeGames.get(gameId);
        GameState gameState = gameStates.get(gameId);

        if (executor != null && gameState != null) {
            executor.resumeGame();
            gameState.setStatus(GameState.GameStatus.RUNNING);
            return true;
        }
        return false;
    }

    public boolean stopGame(String gameId) {
        GameRoundExecutor executor = activeGames.get(gameId);
        GameState gameState = gameStates.get(gameId);

        if (executor != null && gameState != null) {
            executor.stopGame();
            gameState.setStatus(GameState.GameStatus.CANCELLED);
            activeGames.remove(gameId);
            return true;
        }
        return false;
    }

    public boolean cancelRound(String gameId, int roundNumber) {
        GameRoundExecutor executor = activeGames.get(gameId);
        if (executor != null) {
            executor.cancelRound(roundNumber);
            return true;
        }
        return false;
    }

    public GameState getGameState(String gameId) {
        return gameStates.get(gameId);
    }

    public List<String> getActiveGameIds() {
        return new ArrayList<>(activeGames.keySet());
    }

    public Map<String, Object> getGameStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeGames", activeGames.size());
        stats.put("totalGames", gameStates.size());

        long runningGames = gameStates.values().stream()
                .mapToLong(state -> state.getStatus() == GameState.GameStatus.RUNNING ? 1 : 0)
                .sum();
        stats.put("runningGames", runningGames);

        long pausedGames = gameStates.values().stream()
                .mapToLong(state -> state.getStatus() == GameState.GameStatus.PAUSED ? 1 : 0)
                .sum();
        stats.put("pausedGames", pausedGames);

        long completedGames = gameStates.values().stream()
                .mapToLong(state -> state.getStatus() == GameState.GameStatus.COMPLETED ? 1 : 0)
                .sum();
        stats.put("completedGames", completedGames);

        return stats;
    }

    public GameRoundExecutor getGameExecutor(String gameId) {
        return activeGames.get(gameId);
    }

    @Override
    public void onGameStart(String gameId) {
        logger.info("Game {} started", gameId);
    }

    @Override
    public void onRoundStart(String gameId, GameRound round) {
        GameState gameState = gameStates.get(gameId);
        if (gameState != null) {
            gameState.setCurrentRoundNumber(round.getRoundNumber());
        }
        logger.info("Round {} started for game {}", round.getRoundNumber(), gameId);
    }

    @Override
    public void onRoundEnd(String gameId, GameRound round) {
        logger.info("Round {} completed for game {}", round.getRoundNumber(), gameId);
    }

    @Override
    public void onRoundError(String gameId, GameRound round, Exception error) {
        logger.error("Round {} failed for game {}: {}",
                round.getRoundNumber(), gameId, error.getMessage());
    }

    @Override
    public void onGameComplete(String gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState != null) {
            gameState.setStatus(GameState.GameStatus.COMPLETED);
            gameState.setCompletedAt(new Date());
        }
        activeGames.remove(gameId);
        logger.info("Game {} completed", gameId);
    }

    @Override
    public void onGameCancelled(String gameId) {
        GameState gameState = gameStates.get(gameId);
        if (gameState != null) {
            gameState.setStatus(GameState.GameStatus.CANCELLED);
        }
        logger.info("Game {} cancelled", gameId);
    }
}