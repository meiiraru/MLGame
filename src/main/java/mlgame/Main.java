package mlgame;

import cinnamon.Cinnamon;
import cinnamon.Client;
import cinnamon.events.EventType;
import org.lwjgl.glfw.GLFW;

public class Main {

    private final static int w = 225;
    private final static int h = 400;
    private static float s = 2f;

    public static void main(String... args) {
        Client.mainScreen = MainMenu::new;
        Cinnamon.NAMESPACE = "mlgame";
        Cinnamon.TITLE = "Machine Learning Game";
        Cinnamon.WIDTH = (int) (w * s);
        Cinnamon.HEIGHT = (int) (h * s);
        Client client = Client.getInstance();
        client.events.registerEvent(EventType.WINDOW_RESIZE, event -> {
            client.window.setResizable(false);
            client.window.updateSize((int) (w * s), (int) (h * s), s, true);
        });
        client.events.registerEvent(EventType.KEY_PRESS, event -> {
            //key, scancode, action, mods
            if ((int) event[2] == GLFW.GLFW_PRESS) {
                switch ((int) event[0]) {
                    case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> {
                        s = Math.max(1f, s - 1f);
                        client.window.setSize((int) (w * s), (int) (h * s));
                    }
                    case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> {
                        s++;
                        client.window.setSize((int) (w * s), (int) (h * s));
                    }
                }
            }
        });
        new Cinnamon(args).run();
    }
}
