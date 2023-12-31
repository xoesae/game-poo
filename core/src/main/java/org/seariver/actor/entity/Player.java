package org.seariver.actor.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import org.seariver.BaseActor;
import org.seariver.actor.Solid;

import static com.badlogic.gdx.Gdx.input;
import static com.badlogic.gdx.Input.Keys;

public class Player extends BaseActor {

    private BaseActor belowSensor;
    private Directions direction = Directions.RIGHT;
    private int lifes = 1;
    private int delayHit = 0;

    private Animation stand;
    private Animation walk;
    private Animation jump;
    private Animation hurt;

    private final float walkAcceleration;
    private final float maxHorizontalSpeed;
    private final float gravity;
    private final float walkDeceleration;
    private float jumpSpeed;

    public Player(float x, float y, Stage stage) {

        super(x, y, stage);

        this.setAnimations();

        this.maxHorizontalSpeed = 250;
        this.walkAcceleration = 500;
        this.walkDeceleration = 1000;
        this.gravity = 700;
        this.lifes = 3;

        this.setJump();

        setBoundaryPolygon(8);
        this.setBelowSensor(stage);
    }

    protected void setBelowSensor(Stage stage) {
        this.belowSensor = new BaseActor(0, 0, stage);
        this.belowSensor.loadTexture("white.png");
        this.belowSensor.setSize(this.getWidth() - 8, 8);
        this.belowSensor.setBoundaryRectangle();
        this.belowSensor.setVisible(false);
    }

    protected void setAnimations() {
        String[] idleFileNames = {
                "player/idle/player-idle-1.png",
                "player/idle/player-idle-2.png",
                "player/idle/player-idle-3.png",
                "player/idle/player-idle-4.png"
        };
        this.stand = loadAnimationFromFiles(idleFileNames, 0.2f, true);

        String[] walkFileNames = {
                "player/run/player-run-1.png",
                "player/run/player-run-2.png",
                "player/run/player-run-3.png",
                "player/run/player-run-4.png",
                "player/run/player-run-6.png",
                "player/run/player-run-6.png"
        };
        this.walk = loadAnimationFromFiles(walkFileNames, 0.2f, true);

        String[] hurtFileNames = {
            "player/hurt/player-hurt-1.png",
            "player/hurt/player-hurt-2.png"
        };

        this.hurt = loadAnimationFromFiles(hurtFileNames, 0.5f, false);
    }

    protected void setJump() {
        String[] jumpFileNames = {
                "player/jump/player-jump-1.png",
                "player/jump/player-jump-2.png"
        };
        this.jump = loadAnimationFromFiles(jumpFileNames, 0.2f, true);
        this.jumpSpeed = 450;
    }

    protected void setFace() {
        if (this.velocityVec.x > 0){
            setScaleX(Directions.RIGHT.getDirection());
        }

        if (this.velocityVec.x < 0) {
            setScaleX(Directions.LEFT.getDirection());
        }
    }

    protected void accelerate(Directions direction) {

        float acc = 0;

        if (direction == Directions.LEFT) {
            acc = -walkAcceleration;
        } else {
            acc = walkAcceleration;
        }

        accelerationVec.add(acc, 0);
        this.direction = direction;
    }

    protected void deceleration(float deltaTime) {

        float decelerationAmount = this.walkDeceleration * deltaTime;

        Directions walkDirection;

        if (this.velocityVec.x > 0) {
            walkDirection = Directions.RIGHT;
        } else {
            walkDirection = Directions.LEFT;
        }

        float walkSpeed = Math.abs(this.velocityVec.x);

        walkSpeed -= decelerationAmount;

        if (walkSpeed < 0) {
            walkSpeed = 0;
        }

        this.velocityVec.x = walkSpeed * walkDirection.getDirection();
    }

    protected void move(float deltaTime) {

        this.velocityVec.add(
            this.accelerationVec.x * deltaTime,
            this.accelerationVec.y * deltaTime
        );

        this.velocityVec.x = MathUtils.clamp(velocityVec.x, -this.maxHorizontalSpeed, this.maxHorizontalSpeed);

        this.moveBy(velocityVec.x * deltaTime, velocityVec.y * deltaTime);

        // reset acceleration
        accelerationVec.set(0, 0);
    }

    protected void manageAnimations() {

        if (this.isOnSolid()) {
            this.belowSensor.setColor(Color.GREEN);

            if (this.velocityVec.x == 0) {
                this.setAnimation(stand);
            } else {
                this.setAnimation(walk);
            }
        } else {
            this.belowSensor.setColor(Color.RED);
            this.setAnimation(jump);
        }

        if (this.delayHit > 0) {
            this.setAnimation(hurt);
        }

    }

    protected boolean toLeft() {
        return input.isKeyPressed(Keys.A) || input.isKeyPressed(Keys.LEFT);
    }

    protected boolean toRight() {
        return input.isKeyPressed(Keys.D) || input.isKeyPressed(Keys.RIGHT);
    }

    public void act(float deltaTime) {

        super.act(deltaTime);

        if (toLeft()) {
            accelerate(Directions.LEFT);
        }

        if (toRight()) {
            accelerate(Directions.RIGHT);
        }

        if (! toRight() && ! toLeft()) {
            this.deceleration(deltaTime);
        }

        // apply gravity
        accelerationVec.add(0, -gravity);

        this.move(deltaTime);

        // move the below sensor below the
        belowSensor.setPosition(getX() + 4, getY() - 8);

        // manage animations
        this.manageAnimations();

        this.setFace();

        alignCamera();
        boundToWorld();

        delayHit--;
    }

    public boolean belowOverlaps(BaseActor actor) {
        return this.belowSensor.overlaps(actor);
    }

    public boolean isOnSolid() {

        for (BaseActor actor : BaseActor.getList(getStage(), "org.seariver.actor.Solid")) {
            Solid solid = (Solid) actor;
            if (this.belowOverlaps(solid) && solid.isEnabled())
                return true;
        }

        return false;
    }

    public void jump() {
        this.velocityVec.y = jumpSpeed;
    }

    public boolean isFalling() {
        return (this.velocityVec.y < 0);
    }

    public void spring() {
        this.velocityVec.y = 1.5f * this.jumpSpeed;
    }

    public void takeDamage()
    {
        this.lifes--;
    }
    public boolean isDead()
    {
        return this.lifes <= 0;
    }

    public void takeHit()
    {
        this.delayHit = 10;
    }

    public boolean canTakeHit()
    {
        return this.delayHit <= 0;
    }

    public boolean isWalkingTo(Directions direction) {
        return this.direction == direction;
    }

    public int getLifes() {
        return this.lifes;
    }
}
