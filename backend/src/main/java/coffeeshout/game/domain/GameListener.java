package coffeeshout.game.domain;

public interface GameListener {
    
    void onGameStart(String gameId);
    
    void onRoundStart(String gameId, GameRound round);
    
    void onRoundEnd(String gameId, GameRound round);
    
    void onRoundError(String gameId, GameRound round, Exception error);
    
    void onGameComplete(String gameId);
    
    void onGameCancelled(String gameId);
}