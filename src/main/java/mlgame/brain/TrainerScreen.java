package mlgame.brain;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.render.MatrixStack;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.IOUtils;
import cinnamon.utils.Resource;
import cinnamon.world.Hud;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;

public class TrainerScreen extends ParentedScreen {

    private final Trainer trainer;
    private final WatchService watchService;

    private Label runningLabel, savingLabel, genLabel, bestLabel;
    private Button startTraining, stopTraining, replayBest;

    public TrainerScreen(Trainer trainer, Screen parentScreen) {
        super(parentScreen);
        this.trainer = trainer;

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            Path snapshots = trainer.trainingPath.resolve("snapshots");
            IOUtils.createOrGetDir(snapshots);
            snapshots.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize WatchService", e);
        }
    }

    @Override
    public void init() {
        //title
        String name = trainer.trainingPath.getFileName().toString();
        Label title = new Label(width / 2, 4 + 8, Text.of(name), Alignment.CENTER);
        addWidget(title);

        //running
        runningLabel = new Label(4, title.getY() + title.getHeight() + 4, Text.empty(), Alignment.TOP_LEFT);
        addWidget(runningLabel);

        //saving
        savingLabel = new Label(width - 4, title.getY() + title.getHeight() + 4, Text.empty(), Alignment.TOP_RIGHT);
        addWidget(savingLabel);

        //current gen
        genLabel = new Label(4, runningLabel.getY() + title.getHeight() + 4, Text.empty(), Alignment.TOP_LEFT);
        addWidget(genLabel);

        //best score
        bestLabel = new Label(4, genLabel.getY() + title.getHeight() + 4, Text.empty(), Alignment.TOP_LEFT);
        addWidget(bestLabel);

        //actions
        ContainerGrid actionGrid = new ContainerGrid(4, height - 4, 4);
        actionGrid.setAlignment(Alignment.BOTTOM_LEFT);
        addWidget(actionGrid);

        startTraining = new Button(0, 0, width - 8, 20, Text.of("Start Training"), button -> trainer.startTraining());
        startTraining.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(startTraining);

        stopTraining = new Button(0, 0, width - 8, 20, Text.of("Stop Training"), button -> trainer.stopTraining());
        stopTraining.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(stopTraining);

        Button takeSnapshot = new Button(0, 0, width - 8, 20, Text.of("Take Snapshot"), button -> trainer.snapshot());
        takeSnapshot.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(takeSnapshot);

        Button viewSnapshots = new Button(0, 0, width - 8, 20, Text.of("View Snapshots"), button -> {});
        viewSnapshots.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(viewSnapshots);

        replayBest = new Button(0, 0, width - 8, 20, Text.of("Replay Best"), button -> {
            Replay replay = Replay.load(trainer.trainingPath.resolve("best_ai_replay.replay"));
            if (replay != null)
                client.setScreen(new ReplayGame(this, replay));
        });
        replayBest.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(replayBest);

        Button openFolder = new Button(0, 0, width - 8, 20, Text.of("Open Training Folder"), button -> IOUtils.openInExplorer(trainer.trainingPath));
        openFolder.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(openFolder);

        //back button
        super.init();
    }

    @Override
    protected void addBackButton() {
        //back button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.of("Return to Training Selection"));
        addWidget(closeButton);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(0xFF202020);
    }

    @Override
    public void tick() {
        super.tick();

        genLabel.setText(Text.of("Generation: ").append(Text.of(trainer.generation).withStyle(Style.EMPTY.color(Colors.PURPLE))));
        bestLabel.setText(Text.of("Best Score: ")
                .append(Text.of(trainer.bestScore > Integer.MIN_VALUE ? trainer.bestScore : "N/A").withStyle(Style.EMPTY.color(Colors.PURPLE)))
                .append(" @ Gen ")
                .append(Text.of(trainer.bestGen != -1 ? trainer.bestGen : "N/A").withStyle(Style.EMPTY.color(Colors.PURPLE)))
        );

        if (trainer.training) {
            long elapsed = (System.currentTimeMillis() - trainer.trainingStartTime) / 1000;
            runningLabel.setText(Text.of("Training... " + elapsed + "s").withStyle(Style.EMPTY.italic(true).color(Colors.LIME)));
            startTraining.setActive(false);
            stopTraining.setActive(true);
        } else {
            runningLabel.setText(Text.of("Not Training").withStyle(Style.EMPTY.italic(true).color(Colors.RED)));
            startTraining.setActive(true);
            stopTraining.setActive(false);
        }

        if (trainer.saving)
            savingLabel.setText(Text.of("Saving...").withStyle(Style.EMPTY.italic(true).color(Colors.LIGHT_GRAY)));
        else
            savingLabel.setText(Text.empty());

        replayBest.setActive(trainer.bestScore > Integer.MIN_VALUE);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
    }
}
