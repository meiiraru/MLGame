package mlgame;

import cinnamon.gui.ParentedScreen;
import cinnamon.gui.Screen;
import cinnamon.gui.widgets.types.Button;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import cinnamon.text.Style;
import cinnamon.text.Text;
import cinnamon.utils.Alignment;
import cinnamon.utils.Colors;
import cinnamon.utils.Resource;
import cinnamon.utils.TextUtils;

import java.util.List;

public class AboutScreen extends ParentedScreen {

    public AboutScreen(Screen parentScreen) {
        super(parentScreen);
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        Text text = Text.of("""
            Machine Learning Game
            
            A simple 2D platformer, similar to the game Beat Stomper
            
            The player is a simple square that needs to jump on platforms to avoid falling down
            
            The platforms are generated randomly, with different sizes and horizontal speeds
            
            The player can jump by pressing any button on the keyboard or mouse
            
            
            A neural network can be trained to play the game using a genetic algorithm
            
            The neural network takes as input the platforms alongside the player properties
            
            It outputs a value between 0 and 1, which is the probability of jumping
            
            
            The neural network consists of 3 layers, 17 input neurons, 96 hidden neurons, and 1 output neuron
            
            A genetic algorithm is used to evolve the neural network over generations, selecting the best performing networks and mutating them to create new networks
            
            A population of 1000 brains are tested simultaneously in 3 different game instances (seeds)
            
            The most performing brains are selected to create the next generation, keeping the best ones without mutations (elitism), and the worst ones completely random to allow different genetics
            """);

        List<Text> lines = TextUtils.split(text, "\n");
        float y = 20;

        for (Text line : lines) {
            List<Text> wrappedLines = TextUtils.warpToWidth(line, width - 20 - 20);
            for (Text wrapped : wrappedLines) {
                wrapped.render(VertexConsumer.MAIN, matrices, 20, y);
                y += TextUtils.getHeight(wrapped) + wrapped.getStyle().getGuiStyle().getFont().lineGap;
            }
        }

        Text.of("Made by Meiiraru (Pumpkin) Akitsuki")
                .withStyle(Style.EMPTY.color(Colors.DARK_GRAY))
                .render(VertexConsumer.MAIN, matrices, width - 4, height - 4, Alignment.BOTTOM_RIGHT);
    }
}
