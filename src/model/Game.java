package model;

import model.geometry.AABB;
import model.geometry.Point2D;
import model.geometry.Vector2D;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nathan on 8/19/2015.
 */
public class Game {
    public Player player;
    public List<Player> players;
    public List<Object> entities;
    public Map map;

    public static final float gravity = -98;

    public float time = 0;

    public Game() {
        map = new Map();

        // Spawn player
        player = new Player();
        player.position = new Point2D(400, 549);
        player.acceleration.y = gravity;

        players = new ArrayList<>();
        players.add(player);

        entities = new ArrayList<>();
    }

    public void update(float deltaTime) {
        // Debug
        time += deltaTime;
//        System.out.println("t = " + time + ", pos = (" + player.x + ", " + player.y + "), v = (" + player.velocity.x + ", " + player.velocity.y + "), a = (" + player.acceleration.x + ", " + player.acceleration.y + ")");

        // Dynamics
        movePlayers(deltaTime);
        moveEntities(deltaTime);
    }

    private void movePlayers(float deltaTime) {
        for (Player player : players) {
            // Apply gravity
            player.velocity.add(player.acceleration.copy().scale(deltaTime));

            // Move player
            player.position.displace(player.acceleration, player.velocity, deltaTime);

            // Check collisions
            for (AABB box : map.statics) {
                Collision collision = player.collision(box);
                if (collision != null) {
                    if (Math.abs(collision.delta.x) > Math.abs(collision.delta.y)) {
                        player.position.x += collision.delta.x;
                        player.velocity.x = 0f;
                        player.acceleration.x = 0f;
                    } else {
                        player.position.y += collision.delta.y;
                        player.velocity.y = 0f;
                    }
                }
            }
        }
    }

    private void moveEntities(float deltaTime) {
        for (Object entity : entities) {
            if (entity instanceof Rocket) {
                Rocket rocket = (Rocket) entity;
                // Move player
                rocket.position.displace(rocket.acceleration, rocket.velocity, deltaTime);

                // Check collisions
                for (AABB box : map.statics) {
                    Collision collision = box.collision(rocket);
                    if (collision != null) {
//                        entities.remove(rocket);
                        for (Player player : players) {
                            float distance = player.position.distance(collision.position);
                            if (distance <= Rocket.EXPLOSION_RADIUS) {
                                Vector2D explosion = new Vector2D(player.getCenter().x - collision.position.x, player.getCenter().y - collision.position.y);
//                                explosion = explosion.scale(10);
                                player.velocity.add(explosion);
                            }
                        }
                    }
                }
            }
        }
    }

    public void shoot(Point2D xhair) {
        Rocket rocket = new Rocket(player.getCenter().x, player.getCenter().y, Rocket.RADIUS);
        rocket.owner = player;
        Point2D origin = player.getCenter();
        rocket.velocity = new Vector2D(xhair.x - origin.x, xhair.y - origin.y);
        rocket.velocity.setMagnitude(Rocket.VELOCITY);
        rocket.acceleration = new Vector2D(0, 0);
        entities.add(rocket);
    }
}
