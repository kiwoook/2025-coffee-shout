package coffeeshout.lab;

import java.util.ArrayList;
import java.util.List;

public class GameRound {
    private final int roundNumber;
    private final List<String> playerIds;
    private final long duration;
    private final Runnable roundLogic;
    private RoundStatus status = RoundStatus.PENDING;
    private long startTime;
    private long endTime;
    
    public enum RoundStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
    
    public GameRound(int roundNumber, List<String> playerIds, long duration, Runnable roundLogic) {
        this.roundNumber = roundNumber;
        this.playerIds = new ArrayList<>(playerIds);
        this.duration = duration;
        this.roundLogic = roundLogic;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public List<String> getPlayerIds() {
        return new ArrayList<>(playerIds);
    }
    
    public long getDuration() {
        return duration;
    }
    
    public Runnable getRoundLogic() {
        return roundLogic;
    }
    
    public RoundStatus getStatus() {
        return status;
    }
    
    public void setStatus(RoundStatus status) {
        this.status = status;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    @Override
    public String toString() {
        return String.format("GameRound{roundNumber=%d, status=%s, duration=%dms, players=%d}", 
                           roundNumber, status, duration, playerIds.size());
    }
}