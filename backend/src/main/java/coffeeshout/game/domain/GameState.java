package coffeeshout.game.domain;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameState {
    private final String gameId;
    private final Date createdAt;
    private Date startedAt;
    private Date completedAt;
    private GameStatus status = GameStatus.CREATED;
    private int currentRoundNumber = 0;
    private int totalRounds = 0;

    public enum GameStatus {
        CREATED, RUNNING, PAUSED, COMPLETED, CANCELLED
    }

    public GameState(String gameId) {
        this.gameId = gameId;
        this.createdAt = new Date();
    }
}