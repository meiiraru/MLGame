package mlgame.gui;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.Container;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.Widget;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.Label;
import cinnamon.math.Rotation;
import cinnamon.model.GeometryHelper;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.world.Hud;
import mlgame.brain.SnapshotData;
import mlgame.brain.Trainer;
import mlgame.replay.Replay;
import mlgame.replay.ReplayGame;
import org.joml.Math;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TrainerScreen extends ParentedScreen {

    private final Trainer trainer;

    private final List<GraphElement> fitnessHistory = new ArrayList<>();
    private final Container snapshotContainer = new Container(0, 0);
    private int lastSnapshotCount = 0;
    private int maxGen = -1;
    private int graphLevel = 5;
    private float average = -Float.MAX_VALUE;

    private Label runningLabel, savingLabel, genLabel, bestLabel;
    private Button startTraining, stopTraining, replayBest, playRandom;

    public TrainerScreen(Trainer trainer, Screen parentScreen) {
        super(parentScreen);
        this.trainer = trainer;
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

        //best fitness
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

        Button viewSnapshots = new Button(0, 0, width - 8, 20, Text.of("View Snapshots"), button -> client.setScreen(new SnapshotViewerScreen(trainer, this)));
        viewSnapshots.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(viewSnapshots);

        replayBest = new Button(0, 0, width - 8, 20, Text.of("Replay Best"), button -> {
            Replay replay = Replay.load(trainer.trainingPath.resolve("best.replay"));
            if (replay != null)
                client.setScreen(new ReplayGame(this, replay));
            else
                Toast.addToast("Missing replay file!").type(Toast.ToastType.ERROR);
        });
        replayBest.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(replayBest);

        playRandom = new Button(0, 0, width - 8, 20, Text.of("Play Random Seed"), button -> {
            trainer.evaluateBrain(trainer.population[0], System.currentTimeMillis());
            Replay replay = trainer.population[0].replay;
            if (replay != null)
                client.setScreen(new ReplayGame(this, replay));
            else
                Toast.addToast("Missing replay!").type(Toast.ToastType.ERROR);
        });
        playRandom.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(playRandom);

        Button openFolder = new Button(0, 0, width - 8, 20, Text.of("Open Training Folder"), button -> IOUtils.openInExplorer(trainer.trainingPath));
        openFolder.setStyle(Hud.HUD_STYLE);
        actionGrid.addWidget(openFolder);

        //graph buttons
        ContainerGrid graphButtonGrid = new ContainerGrid(width - 4, startTraining.getY() - 12, 2, 2);
        graphButtonGrid.setAlignment(Alignment.TOP_RIGHT);
        addWidget(graphButtonGrid);

        Button minus = new Button(0, 0, 8, 8, Text.of("-"), button -> {
            graphLevel = Math.clamp(1, 10, graphLevel - 1);
            rebuildGraph();
        });
        minus.setTooltip(Text.of("Show Less"));
        minus.setStyle(Hud.HUD_STYLE);
        graphButtonGrid.addWidget(minus);

        Button plus = new Button(0, 0, 8, 8, Text.of("+"), button -> {
            graphLevel = Math.clamp(1, 10, graphLevel + 1);
            rebuildGraph();
        });
        plus.setTooltip(Text.of("Show More"));
        plus.setStyle(Hud.HUD_STYLE);
        graphButtonGrid.addWidget(plus);

        //back button
        super.init();

        //snapshots
        addWidget(snapshotContainer);
        rebuildGraph();
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
        bestLabel.setText(Text.of("Best Fitness: ")
                .append(Text.of(trainer.allTimeBest > -Float.MAX_VALUE ? String.format("%.2f", trainer.allTimeBest) : "N/A").withStyle(Style.EMPTY.color(Colors.PURPLE)))
                .append(" @ Gen ")
                .append(Text.of(trainer.bestGen != -1 ? trainer.bestGen : "N/A").withStyle(Style.EMPTY.color(Colors.PURPLE)))
                .append(" @ Avg ")
                .append(Text.of(average > -Float.MAX_VALUE ? String.format("%.2f", average) : "N/A").withStyle(Style.EMPTY.color(Colors.PURPLE)))

        );

        if (trainer.training) {
            long elapsed = (System.currentTimeMillis() - trainer.trainingStartTime) / 1000;
            runningLabel.setText(Text.of("Training... " + elapsed + "s (" + (trainer.elapsedTrainingTime / 1000 + elapsed) + "s)").withStyle(Style.EMPTY.italic(true).color(Colors.LIME)));
        } else {
            runningLabel.setText(Text.of("Training Paused - total time: " + (trainer.elapsedTrainingTime / 1000) + "s").withStyle(Style.EMPTY.italic(true).color(Colors.RED)));
        }

        if (trainer.saving)
            savingLabel.setText(Text.of("Saving...").withStyle(Style.EMPTY.italic(true).color(Colors.LIGHT_GRAY)));
        else
            savingLabel.setText(Text.empty());

        startTraining.setActive(!trainer.training);
        stopTraining.setActive(trainer.training);
        replayBest.setActive(trainer.allTimeBest > -Float.MAX_VALUE);
        playRandom.setActive(trainer.population != null && trainer.population.length > 0 && trainer.population[0] != null);

        if (trainer.snapshots.size() != lastSnapshotCount)
            rebuildGraph();
    }

    private void rebuildGraph() {
        fitnessHistory.clear();
        snapshotContainer.clear();
        lastSnapshotCount = trainer.snapshots.size();
        maxGen = -1;
        average = -Float.MAX_VALUE;

        fitnessHistory.add(new GraphElement(0, 0, 0, 0));

        if (!trainer.snapshots.isEmpty()) {
            int y0 = bestLabel.getY() + bestLabel.getHeight() + 12;
            int y1 = startTraining.getY() - 12;

            int w = width - 8;
            int h = y1 - y0;

            List<SnapshotData> snapshots = trainer.snapshots;
            int skipCount = Math.max(1, snapshots.size() / (20 * graphLevel)); //skip some snapshots if too many
            for (int i = 0; i < snapshots.size(); i++) {
                SnapshotData snapshot = snapshots.get(i);
                float fitness = snapshot.fitness();

                if (fitness != trainer.allTimeBest && i % skipCount != 0 && !snapshot.hasReplay())
                    continue;

                int gen = snapshot.generation();
                maxGen = Math.max(maxGen, gen);

                GraphElement element = new GraphElement(0, 0, gen, fitness);
                fitnessHistory.add(element);
            }

            //fix elements position
            for (GraphElement element : fitnessHistory) {
                int x = 4 + (int) ((float) element.gen / maxGen * w);
                int y = y1 - (int) (element.fitness / trainer.allTimeBest * h);
                element.setPos(x - GraphElement.r, Math.clamp(y0, y1, y) - GraphElement.r);
                snapshotContainer.addWidget(element);
            }

            //grab average fitness from the last 100
            int count = 0;
            average = 0f;
            for (int i = fitnessHistory.size() - 1; i >= 0 && count < 100; i--) {
                GraphElement element = fitnessHistory.get(i);
                average += element.fitness;
                count++;
            }
            average /= count;
        }

        fitnessHistory.sort(Comparator.comparingInt(e -> e.gen));
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        //snapshot widget

        //foreground
        int y0 = bestLabel.getY() + bestLabel.getHeight() + 12;
        int y1 = startTraining.getY() - 12;
        VertexConsumer.MAIN.consume(GeometryHelper.rectangle(matrices, 4, y0, width - 4, y1, 0x7F000000));

        //render lines
        for (int i = 1; i < fitnessHistory.size(); i++) {
            GraphElement e0 = fitnessHistory.get(i - 1);
            GraphElement e1 = fitnessHistory.get(i);
            VertexConsumer.MAIN.consume(GeometryHelper.line(matrices, e0.getCenterX(), e0.getCenterY(), e1.getCenterX(), e1.getCenterY(), 0.5f, Colors.LIME.argb));
        }

        //reset tooltip
        GraphElement.selectedElement = null;

        //render widgets
        super.render(matrices, mouseX, mouseY, delta);

        //texts
        Style style = Style.EMPTY.outlined(true);
        Text.of("0").withStyle(style).render(VertexConsumer.MAIN, matrices, 4 + 1, y1 - 1, Alignment.BOTTOM_LEFT);
        Text.of(trainer.allTimeBest > -Float.MAX_VALUE ? String.format("%.2f", trainer.allTimeBest) : "N/A").withStyle(style).render(VertexConsumer.MAIN, matrices, 4 + 1, y0 + 1, Alignment.TOP_LEFT);
        Text.of(maxGen > -1 ? maxGen : "N/A").withStyle(style).render(VertexConsumer.MAIN, matrices, width - 4 - 1, y1 - 1, Alignment.BOTTOM_RIGHT);
        Text.of("Generation").withStyle(style).render(VertexConsumer.MAIN, matrices, width / 2f, y1 - 1, Alignment.BOTTOM_CENTER);

        matrices.pushMatrix();
        matrices.translate(4 + 1, y0 + (y1 - y0) / 2f, 0);
        matrices.rotate(Rotation.Z.rotationDeg(-90f));
        Text.of("Fitness").withStyle(style).render(VertexConsumer.MAIN, matrices, 0, 0, Alignment.TOP_CENTER);
        matrices.popMatrix();
    }

    private static class GraphElement extends Widget {

        public static final int r = 1;
        public static GraphElement selectedElement = null;

        public final int gen;
        public final float fitness;
        private final Text tooltip;

        public GraphElement(int x, int y, int gen, float fitness) {
            super(x, y, r + r, r + r);
            this.gen = gen;
            this.fitness = fitness;
            this.tooltip = Text.of("Gen: " + gen + String.format("\nFitness: %.2f", fitness));
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            VertexConsumer.MAIN.consume(GeometryHelper.circle(matrices, getCenterX(), getCenterY(), r, 5, Colors.YELLOW.argb));

            if (UIHelper.isMouseOver(this, mouseX, mouseY) && (selectedElement == null || selectedElement == this)) {
                selectedElement = this;
                UIHelper.renderTooltip(matrices, mouseX, mouseY, tooltip);
            }
        }
    }
}
