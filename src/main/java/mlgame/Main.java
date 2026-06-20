package mlgame;

import cinnamon.Cinnamon;
import cinnamon.Client;
import cinnamon.events.EventType;

public class Main {

    public static void main(String... args) {
        Client.mainScreen = MainMenu::new;
        Cinnamon.TITLE = "Machine Learning Game";
        Cinnamon.WIDTH = 450;
        Cinnamon.HEIGHT = 800;
        Client.getInstance().events.registerEvent(EventType.WINDOW_RESIZE, event -> {
            Client.getInstance().window.setResizable(false);
            Client.getInstance().window.updateSize(Cinnamon.WIDTH, Cinnamon.HEIGHT, 2f, true);
        });
        new Cinnamon(args).run();
    }
}
