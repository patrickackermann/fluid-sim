package simulation;

import javafx.scene.canvas.GraphicsContext;

public class Sphere implements Obstacle {

    private final double X;
    private final double Y;
    private final double R;

    public Sphere(double x, double y, double r) {
        this.X = x;
        this.Y = y;
        this.R = r;
    }

    @Override
    public void draw(GraphicsContext context) {
        context.fillOval(X - R, Y - R, 2 * R, 2 * R);
    }

    @Override
    public void handleCollision(double x, double y, CollisionHandler collision) {
        double tx = x - X;
        double ty = y - Y;
        double d = Math.sqrt(tx * tx + ty * ty);

        double nx = (x - X) / d;
        double ny = (y - Y) / d;

        collision.handle(nx, ny, d - R);
    }
}
