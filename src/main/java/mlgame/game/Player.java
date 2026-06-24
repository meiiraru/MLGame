package mlgame.game;

import cinnamon.math.collision.AABB;
import cinnamon.math.collision.Hit;
import org.joml.Vector3f;

public class Player extends GameElement {

    public boolean onGround = false;
    public Platform platform = null;

    public Player(Game game) {
        super(game);
    }

    @Override
    public void tick() {
        super.tick();

        //apply gravity
        if (!onGround)
            velocity.y += Game.GRAVITY;
        else {
            velocity.x = 0;
            velocity.y = 0;
        }

        //check for collisions and adjust velocity accordingly
        checkCollision();

        //apply velocity
        moveTo(pos.x + velocity.x, pos.y + velocity.y);
    }

    public void checkCollision() {
        //ignore all collisions if the player is moving upwards or stationary
        if (velocity.y <= 0)
            return;

        AABB thisBB = getAABBForElement(this);
        Vector3f tempVel = new Vector3f(velocity, 0);
        Hit resultCollision = null;
        Platform resultElement = null;

        for (GameElement element : game.elements) {
            //skip the player
            if (element == this)
                continue;

            //sweep test
            AABB elementBB = getAABBForElement(element);
            Hit hit = thisBB.sweepAABB(elementBB, tempVel);
            if (hit != null && hit.tNear() >= 0f && (resultCollision == null || hit.tNear() < resultCollision.tNear())) {
                resultCollision = hit;
                resultElement = (Platform) element;
            }
        }

        //no hit - just skip
        if (resultCollision == null)
            return;

        //no vertical hit - skip
        if (resultCollision.normal().y > -0.99f)
            return;

        //adjust velocity to land on the platform
        velocity.x *= resultCollision.tNear();

        //snap the player to the top of the platform
        float platformTopY = resultElement.pos.y - resultElement.getHeight() / 2f;
        float targetY = platformTopY - getHeight() / 2f;
        velocity.y = targetY - pos.y;

        //score
        if (platform != null && platform != resultElement)
            game.score();

        //apply flags
        onGround = true;
        platform = resultElement;
        platform.hasPlayer = true;
    }

    @Override
    public float getWidth() {
        return 20;
    }

    @Override
    public float getHeight() {
        return 20;
    }

    public static AABB getAABBForElement(GameElement element) {
        return new AABB()
                .inflate(element.getWidth() / 2f, element.getHeight() / 2f, 1)
                .translate(element.pos.x, element.pos.y, 0);
    }
}
