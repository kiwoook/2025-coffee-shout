package coffeeshout.game.ui.request;

import coffeeshout.game.domain.GameRound;

import java.util.List;

public class CreateGameRequest {
    private List<String> players;
    private List<GameRound> rounds;
    
    public CreateGameRequest() {}
    
    public CreateGameRequest(List<String> players, List<GameRound> rounds) {
        this.players = players;
        this.rounds = rounds;
    }
    
    public List<String> getPlayers() {
        return players;
    }
    
    public void setPlayers(List<String> players) {
        this.players = players;
    }
    
    public List<GameRound> getRounds() {
        return rounds;
    }
    
    public void setRounds(List<GameRound> rounds) {
        this.rounds = rounds;
    }
}