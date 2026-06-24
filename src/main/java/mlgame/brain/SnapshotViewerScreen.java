package mlgame.brain;

import cinnamon.Client;
import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.Toast;
import cinnamon.gui.widgets.WidgetList;
import cinnamon.gui.widgets.types.Button;
import cinnamon.gui.widgets.types.ComboBox;
import cinnamon.gui.widgets.types.TextField;
import cinnamon.render.MatrixStack;
import cinnamon.text.Text;
import cinnamon.utils.Resource;
import cinnamon.world.Hud;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SnapshotViewerScreen extends ParentedScreen {

    private final Trainer trainer;

    private final List<ReplayButton> buttons = new ArrayList<>();
    private final WidgetList list;
    private int lastSnapshotCount = 0;

    private SortMode sortMode = SortMode.GEN_DESC;

    private String filter = "";

    public SnapshotViewerScreen(Trainer trainer, Screen parentScreen) {
        super(parentScreen);
        this.trainer = trainer;

        this.list = new WidgetList(0, 0, 0, 0, 4);
        this.list.setBackground(true);
        this.list.setStyle(Hud.HUD_STYLE);
    }

    @Override
    public void init() {
        TextField field = new TextField(20, 20, width - 20 - 20 - 20 - 4, 20);
        field.setFilter(Character::isDigit);
        field.setHintText(Text.of("Filter by generation..."));
        field.setStyle(Hud.HUD_STYLE);
        field.setText(filter);
        field.setListener(srt -> {
            filter = srt;
            updateList();
        });
        addWidget(field);

        //sort button
        ComboBox sortButton = new ComboBox(field.getX() + field.getWidth() + 4, 20, 20, 20);
        sortButton.setStyle(Hud.HUD_STYLE);
        sortButton.addEntry(SortMode.GEN_DESC.text, null, b -> {
            sortMode = SortMode.GEN_DESC;
            sortButton.setTooltip(Text.of("Sort By:\n").append(sortMode.text));
            updateList();
        });
        sortButton.addEntry(SortMode.GEN_ASC.text, null, b -> {
            sortMode = SortMode.GEN_ASC;
            sortButton.setTooltip(Text.of("Sort By:\n").append(sortMode.text));
            updateList();
        });
        sortButton.addDivider();
        sortButton.addEntry(SortMode.FITNESS_DESC.text, null, b -> {
            sortMode = SortMode.FITNESS_DESC;
            sortButton.setTooltip(Text.of("Sort By:\n").append(sortMode.text));
            updateList();
        });
        sortButton.addEntry(SortMode.FITNESS_ASC.text, null, b -> {
            sortMode = SortMode.FITNESS_ASC;
            sortButton.setTooltip(Text.of("Sort By:\n").append(sortMode.text));
            updateList();
        });
        sortButton.setSelected(0);
        sortButton.setTooltip(Text.of("Sort By:\n").append(sortMode.text));
        addWidget(sortButton);

        //update list
        list.setPos(width / 2, 20 + 20 + 4);
        list.setDimensions(width - 20 - 20, height - 20 - 20 - 20 - 4);
        addWidget(list);

        super.init();

        //fetch snapshots
        fetchSnapshots();
    }

    @Override
    protected void addBackButton() {
        //back button
        Button closeButton = new Button(width - 4 - 16, 4, 16, 16, null, button -> close());
        closeButton.setIcon(new Resource("textures/gui/icons/close.png"));
        closeButton.setTooltip(Text.of("Return to Training"));
        addWidget(closeButton);
    }

    @Override
    protected void renderBackground(MatrixStack matrices, float delta, int color1, int color2, float size) {
        renderSolidBackground(0xFF202020);
    }

    @Override
    public void tick() {
        super.tick();

        if (trainer.snapshots.size() != lastSnapshotCount)
            fetchSnapshots();
    }

    private void fetchSnapshots() {
        lastSnapshotCount = trainer.snapshots.size();
        buttons.clear();

        int w = list.getWidth() - list.getScrollbarWidth() - 2 - 1;
        for (SnapshotData snapshot : trainer.snapshots) {
            if (!snapshot.hasReplay())
                continue;

            int generation = snapshot.generation();
            float fitness = snapshot.fitness();
            Path path = trainer.trainingPath.resolve("snapshots/" + generation + ".replay");

            ReplayButton button = new ReplayButton(w, generation, fitness, path);
            buttons.add(button);
        }

        updateList();
    }

    private void updateList() {
        //sort buttons
        switch (sortMode) {
            case GEN_DESC     -> buttons.sort((a, b) -> Integer.compare(b.generation, a.generation));
            case GEN_ASC      -> buttons.sort((a, b) -> Integer.compare(a.generation, b.generation));
            case FITNESS_DESC -> buttons.sort((a, b) -> Float.compare(b.fitness, a.fitness));
            case FITNESS_ASC  -> buttons.sort((a, b) -> Float.compare(a.fitness, b.fitness));
        }

        list.clear();
        for (ReplayButton button : buttons) {
            if (filter.isEmpty() || String.valueOf(button.generation).startsWith(filter))
                list.addWidget(button);
        }
    }

    private static class ReplayButton extends Button {
        public final int generation;
        public final float fitness;

        public ReplayButton(int width, int generation, float fitness, Path path) {
            super(0, 0, width, 12, Text.of(String.format("Gen %d - Fitness %.2f", generation, fitness)), b -> {
                Replay replay = Replay.load(path);
                if (replay != null)
                    Client.getInstance().setScreen(new ReplayGame(Client.getInstance().screen, replay));
                else
                    Toast.addToast("Missing replay file!").type(Toast.ToastType.ERROR);
            });
            this.generation = generation;
            this.fitness = fitness;
            this.setStyle(Hud.HUD_STYLE);
        }
    }

    private enum SortMode {
        GEN_DESC(Text.of("Gen ↓")),
        GEN_ASC(Text.of("Gen ↑")),
        FITNESS_DESC(Text.of("Fitness ↓")),
        FITNESS_ASC(Text.of("Fitness ↑"));

        public final Text text;

        SortMode(Text text) {
            this.text = text;
        }
    }
}
