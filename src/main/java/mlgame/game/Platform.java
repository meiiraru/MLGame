package mlgame.game;

public class Platform extends GameElement {

    public float speed;
    public final float width;

    public Platform(Game game, int width, float speed) {
        super(game);
        this.width = width;
        this.speed = speed;
    }

    @Override
    public void tick() {
        super.tick();

        if (speed != 0) {
            //move platform
            moveTo(pos.x + speed, pos.y);

            //check wall collisions
            float w = getWidth() / 2f;
            float minX = pos.x - w;
            float maxX = pos.x + w;

            if (minX < 0) {
                pos.x = w;
                speed = -speed;
                moveTo(pos.x, pos.y);
            } else if (maxX > game.width) {
                pos.x = game.width - w;
                speed = -speed;
                moveTo(pos.x, pos.y);
            }
        }
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return 20;
    }
}
