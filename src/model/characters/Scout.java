package model.characters;

import model.Player;
import model.Sprite;
import model.entities.Bullet;
import model.geometry.Point2f;
import model.geometry.Vector2f;
import model.particles.Particle;
import view.Canvas;

import java.awt.*;
import java.util.Random;

/**
 * Created by Nathan on 8/31/2015.
 */
public class Scout extends Character {
    public final float maxJumpDelay = 0.25f;
    public final float wallVelocity = -50f;

    public boolean extraJump;
    public float jumpDelay;

    public transient float armSpriteTime, legSpriteTime;
    public transient Sprite arms, legs;
    public transient Direction direction;

    private static enum Direction {LEFT, RIGHT}

    public Scout() {
    }

    public Scout(Player player) {
        super(player);

        extraJump = true;
        attackInterval = 1f;
        moveSpeed = 400f;
        width = 31;
        height = 81;
    }

    @Override
    public void updateSprite(float deltaTime) {
        // Init
        if (sprite == null) {
            sprite = game.getSprite("scout_red_body");
            legs = game.getSprite("scout_legs_stand");
            arms = game.getSprite("scout_gun");
        }

        // Preserve direction
        if (movingLeft)
            direction = Direction.LEFT;
        else
            direction = Direction.RIGHT;

        if (!onGround) {
            sprite = game.getSprite("scout_red_run_body");
            legs = game.getSprite("scout_legs_run_3");
        } else if (movingLeft || movingRight) {
            // Initialize sprite
            if (!sprite.name.equals("scout_red_run_body"))
                sprite = game.getSprite("scout_red_run_body");

            // Handle legs
            if (legs == null) {
                legs = game.getSprite("scout_legs_run_1");
                legSpriteTime = 0;
            } else if (legSpriteTime >= legs.time) {
                legs = game.getSprite(legs.next);
                legSpriteTime = 0;
            }
            legSpriteTime += deltaTime;

            /*// Handle running arms
            if (currentAttackDelay > 0) {
                if (arms == null || !arms.name.startsWith("ninja_arm_attack")) {
                    arms = game.getSprite("ninja_arm_attack_1");
                    armSpriteTime = 0;
                } else if (armSpriteTime >= arms.time) {
                    arms = game.getSprite(arms.next);
                    armSpriteTime = 0;
                }
                armSpriteTime += deltaTime;
            } else if (arms == null || !arms.name.equals("ninja_arm"))
                arms = game.getSprite("ninja_arm");*/
        } else {
            sprite = game.getSprite("scout_red_body");
            legs = game.getSprite("scout_legs_stand");
        }

        spriteTime += deltaTime;
    }

    /*@Override
    public void updateSprite(float deltaTime) {
        if (sprite == null)
            sprite = game.getSprite("scout_standing");

        if (wallLeft || wallRight) {
            sprite = game.getSprite("scout_walljump");
        } else if (movingLeft || movingRight) {
            if (!sprite.name.startsWith("scout_walking")) {
                sprite = game.getSprite("scout_walking_1");
                spriteTime = 0;
            } else if (spriteTime >= sprite.time) {
                sprite = game.getSprite(sprite.next);
                spriteTime = 0;
            }
        } else {
            sprite = game.getSprite("scout_standing");
        }
        spriteTime += deltaTime;
    }*/

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);

        // Reset double jump
        if (onGround || wallLeft || wallRight)
            extraJump = true;

        // Stick to walls and slide down them
        if ((wallLeft || wallRight) && velocity.y < 0) {
            velocity.y = wallVelocity;
            generateParticleTrail(deltaTime);
        }
    }

    @Override
    public void attack(float deltaTime) {
        float NUM_PELLETS = 8;
        float MAX_SPREAD = 120;
        float PELLET_SIZE = 6;
        float PELLET_VELOCITY = 900;

        if (currentAttackDelay > 0)
            currentAttackDelay -= deltaTime;

        if (!attacking || currentAttackDelay > 0)
            return;

        Random r = new Random();
        for (int i = 0; i < NUM_PELLETS; i++) {
            Bullet bullet = new Bullet(game, getCenter().x, getCenter().y, PELLET_SIZE);
            Point2f origin = getCenter();
            bullet.owner = player.clientID;
            bullet.velocity = new Vector2f(xhair.x - origin.x, xhair.y - origin.y);
            bullet.velocity.setMagnitude(PELLET_VELOCITY);
            bullet.velocity.x += r.nextInt((int) ((MAX_SPREAD * 2 + 1) - MAX_SPREAD));
            bullet.velocity.y += r.nextInt((int) ((MAX_SPREAD * 2 + 1) - MAX_SPREAD));
            game.entities.add(bullet);
        }
        currentAttackDelay = attackInterval;
    }

    @Override
    public void jump(float deltaTime) {
        // TODO refactor to avoid duplicate code
        if (jumpDelay > 0)
            jumpDelay -= deltaTime;

        if (!movingUp) return;

        // Normal jump
        if (onGround) {
            velocity.y = jumpVelocity;
            jumpDelay = maxJumpDelay;
        }

        // Double jump
        else if (!onGround && !wallLeft && !wallRight && extraJump && jumpDelay <= 0) {
            Vector2f jump;
            if (movingLeft)
                jump = new Vector2f(-1, 1);
            else if (movingRight)
                jump = new Vector2f(1, 1);
            else
                jump = new Vector2f(0, 1);
            jump.setMagnitude(jumpVelocity);
//            velocity.y = jumpVelocity;
            velocity = jump;
            extraJump = false;
        }
    }

    @Override
    public void move(float deltaTime) {
        // Wall jump
        if (movingRight && wallLeft) {
            Vector2f wallJump = new Vector2f(1, 1);
            wallJump.setMagnitude(jumpVelocity);
            velocity = wallJump;
            jumpDelay = maxJumpDelay;
        } else if (movingLeft && wallRight) {
            Vector2f wallJump = new Vector2f(-1, 1);
            wallJump.setMagnitude(jumpVelocity);
            velocity = wallJump;
            jumpDelay = maxJumpDelay;
        } else {
            super.move(deltaTime);
        }
    }

    private void generateParticleTrail(float deltaTime) {
        // TODO reduce particles

        // Particle effects
        final int AVG_PARTICLES = 1;
        final int AVG_SIZE = 5;
        final int MAX_DEVIATION = 3;
        final int AVG_VELOCITY = 100;

        Random r = new Random();
        for (int i = 0; i < AVG_PARTICLES; i++) {
            Particle particle = new Particle(game);
            particle.position = getBottomLeft().copy();
            if (wallRight)
                particle.position.x += width;
            int sign;
            if (r.nextBoolean())
                sign = -1;
            else
                sign = 1;
            particle.size = AVG_SIZE + (r.nextInt(MAX_DEVIATION + 1) * sign);
            particle.color = new Color(0, 0, 0);
            particle.angle = (float) Math.toRadians(r.nextInt(360));
            particle.growth = 0;// -15; // - (r.nextInt(5) + 10);
            particle.rotation = (float) Math.toRadians(r.nextInt(361));
            particle.velocity = new Vector2f(r.nextInt(AVG_VELOCITY * 2) - AVG_VELOCITY, r.nextInt(AVG_VELOCITY * 2) - AVG_VELOCITY);
            particle.acceleration = new Vector2f(0, game.GRAVITY);
            game.particles.add(particle);
        }
    }

    @Override
    public void merge(Character other) {
        super.merge(other);
        if (!(other instanceof Scout))
            return;
        final Scout otherScout = (Scout) other;
        legs = otherScout.legs;
        legSpriteTime = otherScout.legSpriteTime;
        arms = otherScout.arms;
        armSpriteTime = otherScout.armSpriteTime;
        direction = otherScout.direction;
    }

    public void draw(Graphics2D g2) {
        // Draw hitbox
//        g2.setColor(Color.RED);
//        g2.drawRect(0, (int) -height, (int) width, (int) height);


        // Setup coordinate spaces
        Sprite head = game.getSprite("scout_head");
        Point2f ARM_ORIGIN = new Point2f(14, 4); // The arms rotation center, in canvas coordinates, relative to the arm sprite
        Graphics2D armCanvas = (Graphics2D) g2.create();
        armCanvas.translate(arms.offsetX + 1, -(arms.offsetY + arms.height));
        armCanvas.rotate(-new Vector2f(position, xhair).getDirection(), ARM_ORIGIN.x, ARM_ORIGIN.y);
        Graphics2D headCanvas = (Graphics2D) g2.create();
        headCanvas.translate(head.offsetX + 1, -(head.offsetY + head.height));

        // Looking left
        if (xhair.x < position.x) {
            armCanvas.scale(1, -1);
            armCanvas.translate(-16, -9);

            headCanvas.scale(-1, 1);
        }

        // Moving left
        if (direction == Direction.LEFT) {
            g2.scale(-1, 1);
            g2.translate(-width, 0);
        }

        // Draw legs
        if (legs != null)
            g2.drawImage(legs.image, legs.offsetX + 2, -(legs.offsetY + legs.height), legs.width, legs.height, null);

        // Draw main sprite
        g2.drawImage(sprite.image, sprite.offsetX + 2, -(sprite.offsetY + sprite.height), sprite.width, sprite.height, null);

        // Draw head
        headCanvas.drawImage(head.image, 0, 0, head.width, head.height, null);

        // Draw arms
        if (arms != null)
            armCanvas.drawImage(arms.image, 0, 0, arms.width, arms.height, null);

    }

    @Override
    public void draw(Canvas canvas, Graphics2D g2) {
        // DEBUG HITBOXES
//        Graphics2D g3 = (Graphics2D) g2.create();
//        g3.translate(canvas.cameraOffsetX, canvas.getHeight() - canvas.cameraOffsetY);
//        if (currentAttackDelay > 0) { // Attack hitbox
//            AABB attack = getAttackHitbox();
//            g3.drawRect(((int) attack.getBottomLeft().x), -(int) (attack.getBottomLeft().y + attack.height), (int) attack.width, (int) attack.height);
//        }
//        if (currentParryDelay >= parryInterval - parryWindow) { // Parry hitbox
//            AABB parry = getParryHitbox();
//            g3.drawRect(((int) parry.getBottomLeft().x), -(int) (parry.getBottomLeft().y + parry.height), (int) parry.width, (int) parry.height);
//        }

        g2 = (Graphics2D) g2.create();
        g2.translate(getBottomLeft().x + canvas.cameraOffsetX, canvas.getHeight() - canvas.cameraOffsetY - getBottomLeft().y);
        draw(g2);
    }
}
