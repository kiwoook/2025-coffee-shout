package coffeeshout.game.ui.response;

import java.util.Map;

public class GameStatsResponse {
    private Map<String, Object> stats;
    
    public GameStatsResponse() {}
    
    public GameStatsResponse(Map<String, Object> stats) {
        this.stats = stats;
    }
    
    public Map<String, Object> getStats() {
        return stats;
    }
    
    public void setStats(Map<String, Object> stats) {
        this.stats = stats;
    }
}