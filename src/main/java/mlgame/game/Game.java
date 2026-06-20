package mlgame.game;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Button;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game extends ParentedScreen {

    public static final float GRAVITY = 1.5f;

    public int width = 225;
    public int height = 400;
    public int score = 0, hiScore = 0;
    public float oldOffset = 0, offset = 0;

    public long seed;
    public Random random;
    public final boolean simulation;

    public Player player;
    public List<GameElement> elements = new ArrayList<>();

    public GameState gameState = GameState.START;

    public Game(Screen parentScreen, long seed, boolean simulation) {
        super(parentScreen);
        this.seed = seed;
        this.random = new Random(seed);
        this.simulation = simulation;
    }

    @Override
    public void init() {
        super.init();
        if (!simulation)
            readHiScore();
        newGame();
    }

    @Override
    public void close() {
        super.close();
        if (!simulation)
            writeHiScore();
    }

    @Override
    protected void addBackButton() {
        Button closeButton = new Button(super.width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.of("Main Menu"));
        addWidget(closeButton);
    }

    @Override
    public boolean closeOnEsc() {
        return false;
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        super.renderBackground(matrices, delta, 0xFFAD72FF, 0xFF72ADFF, size);
    }

    public void newGame() {
        score = 0;
        oldOffset = offset = 0f;
        elements.clear();
        random = new Random(seed);

        //base player
        player = new Player(this);
        player.setPos(width / 2f, 0);
        elements.add(player);

        //base platform
        Platform platform = new Platform(this, 60, 0);
        platform.setPos(width / 2f, 20);
        elements.add(platform);

        gameState = GameState.START;
    }

    public void spawnPlatform() {
        //find the highest platform
        float highestY = player.pos.y;
        for (GameElement element : elements) {
            if (element instanceof Platform)
                highestY = Math.min(highestY, element.pos.y);
        }

        //randomize the platform width between 40 and 100
        int pWidth = 40 + (int) (random.nextFloat() * 60);

        //randomize the speed between 5 and 15 or -5 and -15
        float pSpeed = 5 + (float) (random.nextFloat() * 10);
        if (random.nextFloat() > 0.5f)
            pSpeed = -pSpeed;

        //create and add the platform
        Platform platform = new Platform(this, pWidth, pSpeed);
        platform.setPos(width / 2f, highestY - 200);
        elements.add(platform);
    }

    public void jump() {
        if (gameState == GameState.GAME_OVER) {
            newGame();
            return;
        }

        if (gameState == GameState.START)
            gameState = GameState.PLAYING;

        //jump if the player is on the ground, otherwise slam downwards
        if (player.onGround) {
            player.velocity.y = -30;
            player.onGround = false;
            player.velocity.x = player.platform.velocity.x * 2;
        } else {
            player.velocity.y = 100;
            player.velocity.x = 0;
        }
    }

    @Override
    public void tick() {
        super.tick();

        score = Math.max((int) -player.pos.y, score);
        oldOffset = offset;

        //updates the offset so the player stays visible
        if (score + 280 > offset)
            offset = score + 280;

        //slowly scroll upwards even if the player is not moving up to keep the game challenging
        if (gameState != GameState.START)
            offset += 1f;

        int platformCount = 0;

        //tick elements and remove ones that fall off the bottom of the screen
        for (int i = 0; i < elements.size(); i++) {
            GameElement element = elements.get(i);
            element.tick();

            if (element instanceof Platform) {
                //if the platform goes 50 units below the camera view, delete it
                if (element.pos.y + offset > height + 50) {
                    elements.remove(i);
                    i--; //adjust index backwards to account for the removed element
                    //System.out.println("Removed! y: " + element.pos.y);
                    continue;
                }
                platformCount++;
            }
        }

        //keep platforms on the screen so the player always has something to jump on
        while (platformCount < 3) {
            spawnPlatform();
            platformCount++;
        }

        if (platformCount != 3) {
            System.out.println("Platform count: " + platformCount);
        }

        //game over
        if (player.pos.y - player.getHeight() / 2f + offset > height) {
            if (gameState != GameState.GAME_OVER) {
                gameState = GameState.GAME_OVER;
                if (score > hiScore)
                    hiScore = score;
            }
        }
    }

    public void writeHiScore() {
        Path path = IOUtils.ROOT_FOLDER.resolve("hi_score.moon");
        IOUtils.createOrGetPath(path);
        IOUtils.writeFileCompressed(path, String.valueOf(hiScore).getBytes());
    }

    public void readHiScore() {
        Path path = IOUtils.ROOT_FOLDER.resolve("hi_score.moon");
        byte[] bytes = IOUtils.readFileCompressed(path);
        if (bytes != null) {
            try {
                hiScore = Integer.parseInt(new String(bytes));
            } catch (Exception ignored) {
                hiScore = 0;
            }
        }
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //render borders
        float border = (super.width - width) / 2f;
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, 0, 0, border, height, 0x7F000000));
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, super.width - border, 0, super.width, height, 0x7F000000));

        //render widgets
        super.render(matrices, mouseX, mouseY, delta);

        //translate to the center of the screen
        matrices.pushMatrix();
        matrices.translate(border, 0, 0);

        //render the game
        matrices.pushMatrix();
        matrices.translate(0, Math.lerp(oldOffset, offset, delta), 0);
        for (GameElement element : elements)
            element.render(matrices, delta);
        matrices.popMatrix();

        //render texts
        switch (gameState) {
            case START -> Text.of("Press any button to jump...\n\nPress again to slam down!").withStyle(Style.EMPTY.outlined(true)).render(
                    VertexConsumer.MAIN, matrices,
                    width / 2f, height / 2f,
                    Alignment.CENTER
            );
            case PLAYING -> Text.of(score).append(score > hiScore && !simulation ? "\nNew Highscore!" : "").withStyle(Style.EMPTY.outlined(true)).render(
                    VertexConsumer.MAIN, matrices,
                    width / 2f, 4,
                    Alignment.TOP_CENTER
            );
            case GAME_OVER -> {
                Text.of("GAME\nOVER").withStyle(Style.EMPTY.outlined(true)).render(
                        VertexConsumer.MAIN, matrices,
                        width / 2f, height / 2f,
                        Alignment.CENTER
                );
                Text.of("Score: ").append(score).append(!simulation ? "\nHighscore: " + hiScore : "").withStyle(Style.EMPTY.outlined(true)).render(
                        VertexConsumer.MAIN, matrices,
                        width / 2f, 20,
                        Alignment.TOP_CENTER
                );
            }
        }

        matrices.popMatrix();

        Text.of(seed).withStyle(Style.EMPTY.italic(true).color(0x33FFFFFF)).render(
                VertexConsumer.MAIN, matrices,
                4, height - 4,
                Alignment.BOTTOM_LEFT
        );
    }

    @Override
    public boolean keyPress(int key, int scancode, int action, int mods) {
        boolean sup = super.keyPress(key, scancode, action, mods);
        if (sup)
            return true;

        if (!simulation && action == GLFW.GLFW_PRESS) {
            jump();
            return true;
        }

        return false;
    }

    @Override
    public boolean mousePress(int button, int action, int mods) {
        boolean sup = super.mousePress(button, action, mods);
        if (sup)
            return true;

        if (!simulation && action == GLFW.GLFW_PRESS) {
            jump();
            return true;
        }

        return false;
    }
}
