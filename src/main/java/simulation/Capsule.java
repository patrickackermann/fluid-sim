package simulation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.shape.StrokeLineCap;

public class Capsule implements Obstacle {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final double R;

    private final double dx;
    private final double dy;

    private final double dLengthSq;

    public Capsule(double x1, double y1, double x2, double y2, double r) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.R = r;

        dx = x2 - x1;
        dy = y2 - y1;

        dLengthSq = dx * dx + dy * dy;
    }

    @Override
    public void draw(GraphicsContext context) {
        context.fillOval(x1 - R, y1 - R, 2 * R, 2 * R);
        context.fillOval(x2 - R, y2 - R, 2 * R, 2 * R);

        double lineWidth = context.getLineWidth();
        context.setLineCap(StrokeLineCap.BUTT);
        context.setLineWidth(2 * R);
        context.strokeLine(x1, y1, x2, y2);
        context.setLineWidth(lineWidth);
    }

    @Override
    public void handleCollision(double x, double y, CollisionHandler collision) {
        double t = (dy * (y - y1) - dx * (x1 - x)) / dLengthSq;
        t = Math.max(Math.min(t, 1), 0);
        double sx = x1 + t * dx;
        double sy = y1 + t * dy;
        double d = Math.sqrt((sx - x) * (sx - x) + (sy - y) * (sy - y));

        double nx = (x - sx) / d;
        double ny = (y - sy) / d;

        collision.handle(nx, ny, d - R);
    }
}
