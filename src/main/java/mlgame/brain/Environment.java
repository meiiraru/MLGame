package mlgame.brain;

import mlgame.game.Game;
import mlgame.game.GameElement;
import mlgame.game.Platform;

public class Environment {

    public static final int INPUT_SIZE = 13;
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
        state[i++] = 1f - ((game.player.pos.y + game.player.getHeight() / 2f + game.offset) / game.height);
        state[i++] = game.player.velocity.x / 20f;
        state[i++] = game.player.velocity.y / 100f;
        state[i++] = game.player.onGround ? 1.0f : 0.0f;

        //gather the closest below and above platforms relative to the player
        Platform belowPlayer = null;
        Platform abovePlayer = null;

        for (GameElement el : game.elements) {
            if (el instanceof Platform p) {
                if (p.pos.y >= game.player.pos.y + game.player.getHeight() / 2f) {
                    if (belowPlayer == null || p.pos.y < belowPlayer.pos.y)
                        belowPlayer = p;
                } else {
                    if (abovePlayer == null || p.pos.y > abovePlayer.pos.y)
                        abovePlayer = p;
                }
            }
        }

        //push the platforms
        i = pushPlatform(belowPlayer, false, state, i);
        i = pushPlatform(abovePlayer, true, state, i);

        return state;
    }

    private int pushPlatform(Platform p, boolean top, float[] state, int i) {
        if (p != null) {
            state[i++] = p.pos.x / game.width;
            state[i++] = (game.player.pos.y + game.player.getHeight() / 2f - (p.pos.y - p.getHeight() / 2f)) / game.height; //relative distance
            state[i++] = p.width / game.width;
            state[i++] = p.velocity.x / 15f;
        } else {
            //no platform, fill with zeros
            state[i++] = 0f;
            state[i++] = top ? 1f : -1f;
            state[i++] = 0f;
            state[i++] = 0f;
        }

        return i;
    }

    //steps the game by 1 tick, applies action, returns reward
    public float step(boolean doJump) {
        if (doJump)
            game.jump();

        game.tick(); //run game logic

        //award score
        float reward = game.score - previousScore;
        previousScore = game.score;

        return reward;
    }
}