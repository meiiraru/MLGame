package mlgame.game;

public class Platform extends GameElement {

    public final float width;
    public boolean hasPlayer;

    public Platform(Game game, int width, float speed) {
        super(game);
        this.width = width;
        this.velocity.x = speed;
    }

    @Override
    public void tick() {
        super.tick();

        //move platform
        if (velocity.x != 0)
            moveTo(pos.x + velocity.x, pos.y);

        //move player with platform
        if (hasPlayer) {
            if (!game.player.onGround) {
                hasPlayer = false;
            } else {
                float dx = pos.x - oPos.x;
                game.player.moveTo(game.player.pos.x + dx, game.player.pos.y);
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
