package mlgame.replay;

import cinnamon.Client;
import cinnamon.gui.Screen;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import mlgame.game.Game;
import mlgame.game.GameState;

public class ReplayGame extends Game {

    public static final int REPLAY_DELAY = 5 * Client.TPS;

    private final Replay replay;
    private int tickIndex = 0;
    private int replayDelay = REPLAY_DELAY;

    public ReplayGame(Screen parentScreen, Replay replay) {
        super(parentScreen, replay.getSeed(), true);
        this.replay = replay;
    }

    @Override
    public void newGame() {
        super.newGame();
        this.gameState = GameState.PLAYING;
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

        //game over restart
        if (gameState == GameState.GAME_OVER && --replayDelay <= 0)
            restartReplay();
    }

    public void restartReplay() {
        this.replayDelay = REPLAY_DELAY;
        this.tickIndex = 0;
        this.newGame();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        Text.of("Replay Mode").withStyle(Style.EMPTY.outlined(true))
                .render(VertexConsumer.MAIN, matrices, width - 4, height - 4, Alignment.BOTTOM_RIGHT);
    }
}
