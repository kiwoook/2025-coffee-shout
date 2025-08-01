package coffeeshout.game.ui.request;

public class AddRoundRequest {
    private int roundNumber;
    private long duration;
    private Runnable roundLogic;
    
    public AddRoundRequest() {}
    
    public AddRoundRequest(int roundNumber, long duration, Runnable roundLogic) {
        this.roundNumber = roundNumber;
        this.duration = duration;
        this.roundLogic = roundLogic;
    }
    
    public int getRoundNumber() {
        return roundNumber;
    }
    
    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public Runnable getRoundLogic() {
        return roundLogic;
    }
    
    public void setRoundLogic(Runnable roundLogic) {
        this.roundLogic = roundLogic;
    }
}