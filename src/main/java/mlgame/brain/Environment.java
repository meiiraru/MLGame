package mlgame.brain;

import mlgame.game.Game;
import mlgame.game.GameElement;
import mlgame.game.GameState;
import mlgame.game.Platform;

import java.util.ArrayList;
import java.util.List;

public class Environment {

    public static final int INPUT_SIZE = 17;
    public static final int OUTPUT_SIZE = 1;

    private final Game game;
    private int previousScore = 0;

    public Environment(Game game) {
        this.game = game;
    }

    //returns an array of floats representing the current game state
    public float[] getState() {
        int i = 0;
        float[] state = new float[INPUT_SIZE];
        state[i++] = game.player.pos.x / game.width;
        state[i++] = (game.player.pos.y + game.offset) / game.height;
        state[i++] = game.player.velocity.x / 20f;
        state[i++] = game.player.velocity.y / 100f;
        state[i++] = game.player.onGround ? 1.0f : 0.0f;

        //gather the platforms
        List<Platform> platforms = new ArrayList<>();
        for (GameElement el : game.elements) {
            if (el instanceof Platform)
                platforms.add((Platform) el);
        }

        //sort platforms by y position to ensure that the neurons get data consistently
        platforms.sort((p1, p2) -> Float.compare(p1.pos.y, p2.pos.y));

        for (int j = 0; j < 3; j++) {
            if (j < platforms.size()) {
                Platform p = platforms.get(j);
                state[i++] = p.pos.x / game.width;
                state[i++] = (p.pos.y - game.player.pos.y) / game.height; //relative distance
                state[i++] = p.width / game.width;
                state[i++] = p.speed / 15f;
            } else {
                //no platform, fill with zeros
                state[i++] = 0f;
                state[i++] = 0f;
                state[i++] = 0f;
                state[i++] = 0f;
            }
        }

        return state;
    }

    //steps the game by 1 tick, applies action, returns reward
    public float step(boolean doJump) {
        if (doJump)
            game.jump();

        game.tick(); //run game logic

        float reward = game.score - previousScore;
        previousScore = game.score;

        //massive fail penalty
        if (game.gameState == GameState.GAME_OVER)
            reward -= 1000f;

        //small penalty for passing time
        reward--;

        return reward;
    }
}