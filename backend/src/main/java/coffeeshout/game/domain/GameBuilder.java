package coffeeshout.game.domain;

import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.TaskScheduler;

public class GameBuilder {
    private String gameId;
    private List<String> players = new ArrayList<>();
    private List<GameRound> rounds = new ArrayList<>();
    private GameListener gameListener;

    public GameBuilder gameId(String gameId) {
        this.gameId = gameId;
        return this;
    }

    public GameBuilder addRound(int roundNumber, long duration, Runnable roundLogic) {
        this.rounds.add(new GameRound(roundNumber, duration, roundLogic));
        return this;
    }

    public GameBuilder addRound(GameRound round) {
        this.rounds.add(round);
        return this;
    }

    public GameBuilder gameListener(GameListener listener) {
        this.gameListener = listener;
        return this;
    }

    public GameRoundExecutor build(TaskScheduler scheduler) {
        if (gameId == null || gameId.trim().isEmpty()) {
            throw new IllegalArgumentException("Game ID cannot be null or empty");
        }
        if (players.isEmpty()) {
            throw new IllegalArgumentException("At least one player is required");
        }
        if (gameListener == null) {
            throw new IllegalArgumentException("Game listener cannot be null");
        }

        GameRoundExecutor executor = new GameRoundExecutor(gameId,scheduler, gameListener);
        rounds.forEach(executor::addRound);
        return executor;
    }
}