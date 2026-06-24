package mlgame.brain;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.ContainerGrid;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ConfirmPopup;
import cinnamon.gui.widgets.types.ContextMenu;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.settings.ArgsOptions;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.*;
import cinnamon.world.Hud;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TrainingSelectionScreen extends ParentedScreen {

    public static final Path TRAININGS_DIR = Path.of(ArgsOptions.WORKING_DIR.getAsString()).resolve("trainings");

    private final Map<Path, Trainer> trainings = new HashMap<>();

    private final WidgetList trainingsList;

    public TrainingSelectionScreen(Screen parentScreen) {
        super(parentScreen);
        trainingsList = new WidgetList(0, 0, 0, 0, 4);
        trainingsList.setBackground(true);
        trainingsList.setStyle(Hud.HUD_STYLE);
    }

    @Override
    public void init() {
        super.init();

        //main div
        ContainerGrid mainDiv = new ContainerGrid(20, 20, 12);
        addWidget(mainDiv);

        //header
        ContainerGrid header = new ContainerGrid(20, 20, 4, 2);
        mainDiv.addWidget(header);

        //new training button
        Button newTrainingButton = new Button(0, 0, width - 20 - 4 - 16 - 20, 16, Text.of("New Training"), button -> {
            Path newTrainingPath = TRAININGS_DIR.resolve("New Training");
            newTrainingPath = IOUtils.parseNonDuplicatePath(newTrainingPath, " (", ")");
            IOUtils.createOrGetDir(newTrainingPath);
            updateList();
        });
        header.addWidget(newTrainingButton);
        header.setStyle(Hud.HUD_STYLE);

        //back button
        Button closeButton = new Button(0, 0, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.of("Return to Main Menu"));
        header.addWidget(closeButton);

        //main container
        trainingsList.setDimensions(width - 20 - 20, height - 20 - 20 - 16 - 20);
        mainDiv.addWidget(trainingsList);

        //read the trainings from the disk
        updateList();
    }

    @Override
    protected void addBackButton() {
        //already added in init
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(0xFF202020);
    }

    private void updateList() {
        //get a list of all folders inside the trainings directory
        IOUtils.createOrGetDir(TRAININGS_DIR);
        File[] folders = TRAININGS_DIR.toFile().listFiles(File::isDirectory);

        trainings.keySet().removeIf(path -> !Files.exists(path));

        trainingsList.clear();
        if (folders != null) {
            Arrays.sort(folders, (a, b) -> IOUtils.FilenameComparator.compareTo(a.getName(), b.getName()));
            int w = trainingsList.getWidth() - trainingsList.getScrollbarWidth() - 2 - 1;
            for (File folder : folders) {
                if (folder.isDirectory()) {
                    Path trainingPath = folder.toPath();
                    Trainer trainer = trainings.computeIfAbsent(trainingPath, Trainer::new);

                    TrainingListEntry entry = new TrainingListEntry(w, trainer);
                    entry.setStyle(Hud.HUD_STYLE);
                    trainingsList.addWidget(entry);

                    //context menu
                    ContextMenu menu = new ContextMenu();
                    entry.setPopup(menu);

                    menu.addAction(Text.of("Start Training"), null, (b) -> trainer.startTraining());
                    menu.addAction(Text.of("Stop Training"), null, (b) -> trainer.stopTraining());
                    menu.addAction(Text.of("Open Folder"), null, (b) -> IOUtils.openInExplorer(trainingPath));
                    menu.addAction(Text.of("Duplicate"), null, (b) -> {
                        Path newTrainingPath = IOUtils.parseNonDuplicatePath(trainingPath, " (", ")");
                        //copy all files from the old training to the new one
                        try {
                            Files.walk(trainingPath).forEach(source -> {
                                try {
                                    Path destination = newTrainingPath.resolve(trainingPath.relativize(source));
                                    if (Files.isDirectory(source)) {
                                        IOUtils.createOrGetDir(destination);
                                    } else {
                                        Files.copy(source, destination);
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        updateList();
                    });
                    menu.addAction(Text.of("Delete"), null, (b) -> {
                        ConfirmPopup popup = new ConfirmPopup.YesNo(
                                Text.of("Are you sure you want to delete this training?\nThis action cannot be undone"),
                                bool -> {
                                    if (!bool)
                                        return;

                                    if (trainer.training) {
                                        ConfirmPopup.OK pop = new ConfirmPopup.OK(Text.of("Stop the training before deleting"));
                                        pop.setStyle(Hud.HUD_STYLE);
                                        UIHelper.setPopup(width / 2, height / 2, pop);
                                        pop.open();
                                        return;
                                    }

                                    IOUtils.deleteDir(trainingPath);
                                    trainings.remove(trainingPath);
                                    updateList();
                                }
                        );
                        popup.setStyle(Hud.HUD_STYLE);
                        UIHelper.setPopup(width / 2, height / 2, popup);
                        popup.open();
                    });
                    addWidget(menu);
                }
            }
        }
    }

    private static class TrainingListEntry extends Button {

        private final Trainer trainer;

        public TrainingListEntry(int width, Trainer trainer) {
            super(0, 0, width, 40, Text.of(trainer.trainingPath.getFileName().toString()), button -> Client.getInstance().setScreen(new TrainerScreen(trainer, Client.getInstance().screen)));
            this.trainer = trainer;
        }

        @Override
        protected void renderText(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            Text.of("\u205d").render(VertexConsumer.MAIN, matrices, getX() + getWidth() - 4, getY() + 4, Alignment.TOP_RIGHT);

            Text.empty().append(getFormattedMessage())
                    .append("\n")
                    .append("Generation ").append(trainer.generation)
                    .append("\n\n")
                    .append("Best ").append(trainer.bestFitness == -Float.MAX_VALUE ? "N/A" : String.format("%.0f", trainer.bestFitness))
                    .append(" @ ").append(" Gen ").append(trainer.bestGen == -1 ? "N/A" : trainer.bestGen)

            .render(VertexConsumer.MAIN, matrices, getX() + 4, getY() + 4);

            if (trainer.training)
                Text.of("\n\nTraining...")
                        .withStyle(Style.EMPTY.color(Colors.LIME).italic(true))
                        .render(VertexConsumer.MAIN, matrices, getX() + getWidth() - 4, getY() + 4, Alignment.TOP_RIGHT);

            if (trainer.saving)
                Text.of("\n\n\nSaving...")
                        .withStyle(Style.EMPTY.color(Colors.LIGHT_GRAY).italic(true))
                        .render(VertexConsumer.MAIN, matrices, getX() + getWidth() - 4, getY() + 4, Alignment.TOP_RIGHT);
        }
    }
}
