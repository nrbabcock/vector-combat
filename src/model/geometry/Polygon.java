package model.geometry;

import model.Collision;
import model.Game;
import view.Canvas;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nathan on 3/15/2016.
 */
public class Polygon implements Serializable {
    public transient Game game;

    public List<Point2f> vertices;
    public Vector2f velocity, acceleration;

    public Polygon() {
        velocity = new Vector2f(0, 0);
        acceleration = new Vector2f(0, 0);
    }


    public Polygon(Game game) {
        this();
        this.game = game;
    }

    public Polygon(Game game, List<Point2f> vertices) {
        this(game);
        this.vertices = vertices;
    }

    public Point2f getCenter() {
        return getBottomLeft().translate(getWidth() / 2, getHeight() / 2);
    }

    public float getWidth() {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (Point2f v : vertices) {
            min = Math.min(min, v.x);
            max = Math.max(max, v.x);
        }
        return max - min;
    }

    public float getHeight() {
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (Point2f v : vertices) {
            min = Math.min(min, v.y);
            max = Math.max(max, v.y);
        }
        return max - min;
    }

    public List<Vector2f> getSides() {
        List<Vector2f> sides = new ArrayList<>();
        for (int i = 0; i < vertices.size() - 1; i++)
            sides.add(new Vector2f(vertices.get(i), vertices.get(i + 1)));
        sides.add(new Vector2f(vertices.get(vertices.size() - 1), vertices.get(0)));
        return sides;
    }

    // TODO will list duplicate normals for parallel sides
    public List<Vector2f> getNormals() {
        List<Vector2f> normals = new ArrayList<>();
        outer:
        for (Vector2f v : getSides()) {
            Vector2f newNormal = v.normal().normalize();
            for (Vector2f n : normals)
                if (n.parallel(newNormal))
                    continue outer;
            normals.add(newNormal);
        }
        return normals;
    }

    public Point2f getPosition() {
        return vertices.get(0).copy();
    }

    public Point2f getBottomLeft() {
        return getPosition();
    }

    public Polygon setPosition(Point2f pos) {
        Vector2f delta = new Vector2f(getPosition(), pos);
        for (Point2f v : vertices)
            v.translate(delta);
        return this;
    }

    public Polygon setPosition(float x, float y) {
        return setPosition(new Point2f(x, y));
    }

    public Polygon displace(Vector2f acc, Vector2f vel, float time) {
        return setPosition(getPosition().displace(acc, vel, time));
    }

    public Polygon translate(Vector2f v) {
        return setPosition(getPosition().translate(v));
    }

    public Polygon translate(float x, float y) {
        return setPosition(getPosition().translate(x, y));
    }

    public java.awt.Polygon getAwtPoly() {
        java.awt.Polygon polygon = new java.awt.Polygon();
        for (Point2f v : vertices)
            polygon.addPoint((int) v.x, (int) v.y);
        return polygon;
    }

    public Polygon makeAABB(Point2f position, float width, float height) {
        vertices = new ArrayList<>();
        vertices.add(position);
        vertices.add(position.copy().translate(width, 0));
        vertices.add(position.copy().translate(width, height));
        vertices.add(position.copy().translate(0, height));
        return this;
    }

    public Polygon makeAABB(float x, float y, float width, float height) {
        return makeAABB(new Point2f(x, y), width, height);
    }

    public Polygon makeAABB(int x, int y, int width, int height) {
        return makeAABB((float) x, (float) y, (float) width, (float) height);
    }

    public void update(float deltaTime) {
        // Remove if necessary
//        if (getCenter().x > game.map.width || getCenter().y > game.map.height || getCenter().x < 0 || getCenter().y < 0) {
//            game.garbage.add(this);
//            return;
//        }

        // Move
        displace(acceleration, velocity, deltaTime);

        // Check collisions
        checkCollisions();
    }


    public void draw(Canvas canvas, Graphics2D g2) {
        g2.fillPolygon(getAwtPoly());
    }

    public Collision collision(Polygon other) {
        float overlap = Float.MAX_VALUE;
        Vector2f smallest = null;
        // Project onto axes
        for (Vector2f axis : getNormals()) {
            Projection p1 = project(axis);
            Projection p2 = other.project(axis);
//            System.out.println("Prjection on axis " + axis + ": this = " + p1 + ", other = " + p2);
            if (!p1.overlaps(p2))
                return null;
            else {
                // get the overlap
                float o = p1.getOverlap(p2);
                // check for minimum
                if (Math.abs(o) < Math.abs(overlap)) {
                    // then set this one as the smallest
                    overlap = o;
                    smallest = axis;
                }
            }
        }

//        System.out.println("Found collision between this (" + getPosition() + " " + getWidth() + " x " + getHeight() + ") and (" + other.getPosition() + " " + other.getWidth() + " x " + other.getHeight() + ")");
//        System.out.println("overlap = " + overlap);

        Collision collision = new Collision();
        collision.delta = smallest.setMagnitude(overlap);
        return collision;
    }

    public Projection project(Vector2f axis) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (Point2f v : vertices) {
            float p = axis.dot(v);
            min = Math.min(p, min);
            max = Math.max(p, max);
        }
        return new Projection(min, max);
    }

    class Projection {
        float min, max;

        public Projection(float min, float max) {
            this.min = min;
            this.max = max;
        }

        public boolean overlaps(Projection other) {
            return (min < other.max && min >= other.min) || (max <= other.max && max > other.min) || (other.min < max && other.min >= min) || (other.max <= max && other.max > min);
        }

        public float getOverlap(Projection other) {
            //return Math.min(other.max, max) - Math.max(other.min, min);
//            return (this.max < other.max) ? max - other.min : other.max - min;
            // if min inside other, push it
            float a = other.max - min;
            float b = other.min - max;
            if (Math.abs(a) < Math.abs(b))
                return a;
            return b;
        }

        @Override
        public String toString() {
            return "(" + min + ", " + max + ")";
        }
    }

    public void checkCollisions() {
    }

    public void handleCollision(Collision collision) {
    }


    public static void main(String[] args) {
        Polygon a = new Polygon().makeAABB(0, 10, 100, 50);
        Polygon b = new Polygon().makeAABB(5, 5, 10, 10);

        System.out.println(b.collision(a).delta);
    }

}
