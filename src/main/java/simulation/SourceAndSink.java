package simulation;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SourceAndSink implements Obstacle{

    private final Obstacle sinkCollider;
    public final double sourceX;
    public final double sourceY;
    public final double sourceRadius;
    public final double vX;
    public final double vY;

    public SourceAndSink(Obstacle sinkCollider, double sourceX, double sourceY, double sourceRadius, double vX, double vY) {
        this.sinkCollider = sinkCollider;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceRadius = sourceRadius;
        this.vX = vX;
        this.vY = vY;
    }

    @Override
    public void draw(GraphicsContext context) {
        context.setFill(Color.RED);
        context.setStroke(Color.RED);
        sinkCollider.draw(context);
    }

    @Override
    public void handleCollision(double x, double y, Obstacle.CollisionHandler collision) {
        sinkCollider.handleCollision(x, y, collision);
    }
}
