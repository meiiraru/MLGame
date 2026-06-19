package mlgame.brain;

import cinnamon.gui.Screen;
import mlgame.game.Game;
import mlgame.game.GameState;

public class ReplayGame extends Game {

    private final Replay replay;
    private int tickIndex = 0;

    public ReplayGame(Screen parentScreen, Replay replay) {
        super(parentScreen, replay.getSeed(), true);
        this.replay = replay;
    }

    @Override
    public void tick() {
        if (gameState != GameState.GAME_OVER && tickIndex < replay.getActions().size()) {
            boolean doJump = replay.getActions().get(tickIndex);
            if (doJump)
                this.jump();
            tickIndex++;
        }

        //continue normal game physics
        super.tick();
    }
}
