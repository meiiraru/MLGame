package mlgame.game;

import cinnamon.math.Maths;
import cinnamon.model.GeometryHelper;
import cinnamon.model.Vertex;
import cinnamon.render.MatrixStack;
import cinnamon.render.batch.VertexConsumer;
import org.joml.Vector2f;

public abstract class GameElement {

    protected final Game game;

    public Vector2f pos = new Vector2f();
    public Vector2f oPos = new Vector2f();
    public final Vector2f velocity = new Vector2f();

    public GameElement(Game game) {
        this.game = game;
    }

    public void tick() {
        oPos.set(pos);
    }

    public void render(MatrixStack matrices, float delta) {
        VertexConsumer.MAIN.consume(getRenderShape(0xFF000000, matrices, delta));
    }

    public Vertex[] getRenderShape(int color, MatrixStack matrices, float delta) {
        Vector2f p = Maths.lerp(oPos, pos, delta);
        float w = getWidth() / 2f;
        float h = getHeight() / 2f;
        return GeometryHelper.rectangle(matrices, p.x - w, p.y - h, p.x + w, p.y + h, color);
    }

    public void setPos(float x, float y) {
        oPos.set(pos.set(x, y));
        wallBounce();
    }

    public void moveTo(float x, float y) {
        pos.set(x, y);
        wallBounce();
    }

    public void wallBounce() {
        //check wall collisions
        float w = getWidth() / 2f;

        if (pos.x - w < 0) {
            pos.x = w;
            velocity.x = -velocity.x;
            //System.out.println("Wall! vx: " + velocity.x);
        } else if (pos.x + w > game.width) {
            pos.x = game.width - w;
            velocity.x = -velocity.x;
            //System.out.println("Wall! vx: " + velocity.x);
        }
    }

    public abstract float getWidth();

    public abstract float getHeight();
}
