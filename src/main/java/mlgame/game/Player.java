package mlgame.game;

import org.joml.Vector2f;

public class Player extends GameElement {

    public final Vector2f velocity = new Vector2f();
    public boolean onGround = false;
    public Platform platform = null;

    public Player(Game game) {
        super(game);
    }

    @Override
    public void tick() {
        super.tick();

        //apply gravity and platform movement
        if (!onGround)
            velocity.y += Game.GRAVITY;
        else {
            velocity.y = 0;
            velocity.x = platform != null ? platform.speed : 0;
        }

        //check for collisions and adjust velocity accordingly
        checkCollision();

        //apply velocity
        moveTo(pos.x + velocity.x, pos.y + velocity.y);

        //check wall collisions
        float w = getWidth() / 2f;
        float minX = pos.x - w;
        float maxX = pos.x + w;

        if (minX < 0) {
            pos.x = w;
            velocity.x = -velocity.x;
            moveTo(pos.x, pos.y);
            //System.out.println("Wall! vx: " + velocity.x);
        } else if (maxX > game.width) {
            pos.x = game.width - w;
            velocity.x = -velocity.x;
            moveTo(pos.x, pos.y);
            //System.out.println("Wall! vx: " + velocity.x);
        }
    }

    public void checkCollision() {
        if (velocity.y <= 0)
            return;

        GameElement resultElement = null;

        for (GameElement element : game.elements) {
            //skip the player
            if (element == this)
                continue;

            //skip if the player is below the element
            if (element.pos.y < pos.y)
                continue;

            //get the closest element below the player
            if (resultElement == null || element.pos.y < resultElement.pos.y)
                resultElement = element;
        }

        //check if the player will collide with the platform after applying the velocity using a simple sweep test
        if (resultElement instanceof Platform targetPlatform) {

            //calculate vertical bounds
            float playerBottom = pos.y + (getHeight() / 2f);
            float nextPlayerBottom = playerBottom + velocity.y;
            float platformTop = targetPlatform.pos.y - (targetPlatform.getHeight() / 2f);

            //check if the player was above the platform and they will cross the top edge
            if (playerBottom > platformTop || nextPlayerBottom < platformTop)
                return;

            //calculate horizontal bounds (considering the player's next position)
            float nextPlayerX = pos.x + velocity.x;
            float playerLeft = nextPlayerX - (getWidth() / 2f);
            float playerRight = nextPlayerX + (getWidth() / 2f);

            float platformLeft = targetPlatform.pos.x - (targetPlatform.getWidth() / 2f);
            float platformRight = targetPlatform.pos.x + (targetPlatform.getWidth() / 2f);

            //check if the player will be within the left and right bounds of the platform
            if (playerRight < platformLeft || playerLeft > platformRight)
                return;

            //collision detected

            //System.out.println("Collided! y: " + platformTop);

            //adjust velocity and set flags
            //velocity.mul(tNear);
            velocity.y = platformTop - playerBottom;
            onGround = true;
            platform = targetPlatform;
        }
    }

    @Override
    public float getWidth() {
        return 20;
    }

    @Override
    public float getHeight() {
        return 20;
    }
}
