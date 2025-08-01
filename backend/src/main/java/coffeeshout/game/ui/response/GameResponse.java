package coffeeshout.game.ui.response;

public class GameResponse {
    private String gameId;
    private String message;
    
    public GameResponse() {}
    
    public GameResponse(String gameId, String message) {
        this.gameId = gameId;
        this.message = message;
    }
    
    public String getGameId() {
        return gameId;
    }
    
    public void setGameId(String gameId) {
        this.gameId = gameId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}