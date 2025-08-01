package coffeeshout.game.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameRound {
    private final int roundNumber;
    private final long duration;
    private final Runnable roundLogic;
    private RoundStatus status = RoundStatus.PENDING;
    private long startTime;
    private long endTime;

    public enum RoundStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }

    public GameRound(int roundNumber, long duration, Runnable roundLogic) {
        this.roundNumber = roundNumber;
        this.duration = duration;
        this.roundLogic = roundLogic;
    }
}