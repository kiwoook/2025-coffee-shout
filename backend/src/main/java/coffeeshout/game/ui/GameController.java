package coffeeshout.game.ui;

import coffeeshout.game.application.GameManager;
import coffeeshout.game.domain.GameRound;
import coffeeshout.game.domain.GameState;
import coffeeshout.game.ui.request.CreateGameRequest;
import coffeeshout.game.ui.request.AddRoundRequest;
import coffeeshout.game.ui.response.GameResponse;
import coffeeshout.game.ui.response.GameStatsResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class GameController {
    
    @Autowired
    private GameManager gameManager;
    
    @PostMapping("/create")
    public ResponseEntity<GameResponse> createGame(@RequestBody CreateGameRequest request) {
        try {
            String gameId = gameManager.createGame(request.getPlayers(), request.getRounds());
            return ResponseEntity.ok(new GameResponse(gameId, "Game created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GameResponse(null, "Failed to create game: " + e.getMessage()));
        }
    }
    
    @PostMapping("/create-empty")
    public ResponseEntity<GameResponse> createEmptyGame(@RequestBody List<String> players) {
        try {
            String gameId = gameManager.createGameWithBuilder(players);
            return ResponseEntity.ok(new GameResponse(gameId, "Empty game created successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GameResponse(null, "Failed to create game: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{gameId}/rounds")
    public ResponseEntity<GameResponse> addRound(@PathVariable String gameId, @RequestBody AddRoundRequest request) {
        boolean success = gameManager.addRoundToGame(
            gameId, 
            request.getRoundNumber(), 
            request.getDuration(), 
            request.getRoundLogic()
        );
        
        if (success) {
            return ResponseEntity.ok(new GameResponse(gameId, "Round added successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{gameId}/start")
    public ResponseEntity<GameResponse> startGame(@PathVariable String gameId) {
        boolean success = gameManager.startGame(gameId);
        if (success) {
            return ResponseEntity.ok(new GameResponse(gameId, "Game started successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{gameId}/pause")
    public ResponseEntity<GameResponse> pauseGame(@PathVariable String gameId) {
        boolean success = gameManager.pauseGame(gameId);
        if (success) {
            return ResponseEntity.ok(new GameResponse(gameId, "Game paused successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{gameId}/resume")
    public ResponseEntity<GameResponse> resumeGame(@PathVariable String gameId) {
        boolean success = gameManager.resumeGame(gameId);
        if (success) {
            return ResponseEntity.ok(new GameResponse(gameId, "Game resumed successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{gameId}/stop")
    public ResponseEntity<GameResponse> stopGame(@PathVariable String gameId) {
        boolean success = gameManager.stopGame(gameId);
        if (success) {
            return ResponseEntity.ok(new GameResponse(gameId, "Game stopped successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{gameId}/rounds/{roundNumber}")
    public ResponseEntity<GameResponse> cancelRound(@PathVariable String gameId, @PathVariable int roundNumber) {
        boolean success = gameManager.cancelRound(gameId, roundNumber);
        if (success) {
            return ResponseEntity.ok(new GameResponse(gameId, "Round " + roundNumber + " cancelled successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{gameId}/state")
    public ResponseEntity<GameState> getGameState(@PathVariable String gameId) {
        GameState state = gameManager.getGameState(gameId);
        return state != null ? ResponseEntity.ok(state) : ResponseEntity.notFound().build();
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<String>> getActiveGames() {
        return ResponseEntity.ok(gameManager.getActiveGameIds());
    }
    
    @GetMapping("/stats")
    public ResponseEntity<GameStatsResponse> getStats() {
        Map<String, Object> stats = gameManager.getGameStats();
        return ResponseEntity.ok(new GameStatsResponse(stats));
    }
    
    @GetMapping("/{gameId}/details")
    public ResponseEntity<Map<String, Object>> getGameDetails(@PathVariable String gameId) {
        GameState gameState = gameManager.getGameState(gameId);
        if (gameState == null) {
            return ResponseEntity.notFound().build();
        }
        
        var executor = gameManager.getGameExecutor(gameId);
        Map<String, Object> details = Map.of(
            "gameState", gameState,
            "isRunning", executor != null ? executor.isGameRunning() : false,
            "isPaused", executor != null ? executor.isGamePaused() : false,
            "remainingRounds", executor != null ? executor.getRemainingRounds() : 0,
            "currentRound", executor != null ? executor.getCurrentRound() : null
        );
        
        return ResponseEntity.ok(details);
    }
}