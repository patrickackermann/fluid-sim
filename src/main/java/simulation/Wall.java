package simulation;

import javafx.scene.canvas.GraphicsContext;

public class Wall implements Obstacle {
    // tangential vector
    private final double tx;
    private final double ty;

    // normal vector
    private final double nx;
    private final double ny;

    // start vector
    private final double sx;
    private final double sy;

    /**
     * construct wall with point on wall and direction vector
     */
    public Wall(double sx, double sy, double tx, double ty) {
        this.sx = sx;
        this.sy = sy;

        double d = Math.sqrt(tx * tx + ty * ty);
        this.tx = tx / d;
        this.ty = ty / d;

        this.nx = -this.ty;
        this.ny = this.tx;
    }

    @Override
    public void draw(GraphicsContext context) {
        double length = 1000;
        double width = 50;

        context.beginPath();
        context.moveTo(sx, sy);
        context.lineTo(sx + length * tx, sy + length * ty);
        context.lineTo(sx + length * tx + width * ty, sy + length * ty - width * tx);
        context.lineTo(sx - length * tx + width * ty, sy - length * ty - width * tx);
        context.lineTo(sx - length * tx, sy - length * ty);
        context.closePath();
        context.fill();
    }

    @Override
    public void handleCollision(double x, double y, CollisionHandler collision) {
        double d = (x - sx) * nx + (y - sy) * ny;

        collision.handle(nx, ny, d);
    }
}
