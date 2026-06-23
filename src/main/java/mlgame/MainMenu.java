package mlgame;

import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.render.MatrixStack;
import cinnamon.text.Text;
import cinnamon.world.Hud;
import mlgame.brain.TrainingSelectionScreen;
import mlgame.game.Game;

import java.util.Random;

public class MainMenu extends Screen {

    private static final Random RANDOM = new Random();

    @Override
    public void init() {
        super.init();

        ContainerGrid grid = new ContainerGrid(0, 0, 4);

        Button play = new Button(0, 0, 60, 20, Text.of("Play"), b -> client.setScreen(new Game(this, RANDOM.nextLong(), false)));
        grid.addWidget(play);

        Button train = new Button(0, 0, 60, 20, Text.of("Train AI"), b -> client.setScreen(new TrainingSelectionScreen(this)));
        grid.addWidget(train);

        Button about = new Button(0, 0, 60, 20, Text.of("About"), b -> client.setScreen(new AboutScreen(this)));
        grid.addWidget(about);

        Button exit = new Button(0, 0, 60, 20, Text.of("Exit"), b -> client.window.exit());
        grid.addWidget(exit);

        //add grid to screen
        int y = (int) (height * 0.15f);
        grid.setPos((width - grid.getWidth()) / 2, y + (height - grid.getHeight() - y) / 2);
        grid.setStyle(Hud.HUD_STYLE);
        this.addWidget(grid);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(0xFF202020);
    }
}
