package simulation;

import javafx.scene.canvas.GraphicsContext;

public interface Obstacle {
    void draw(GraphicsContext context);

    void handleCollision(double x, double y, CollisionHandler collision);

    @FunctionalInterface
    interface CollisionHandler {
        void handle(double nx, double ny, double distance);
    }
}
