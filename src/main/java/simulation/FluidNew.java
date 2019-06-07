package simulation;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntIntProcedure;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FluidNew {
    /*******************************
     * draw settings               *
     *******************************/
    public boolean drawSprings = true;
    public boolean drawVelocity = true;
    public boolean drawInteractionRadius = true;
    public boolean drawParticle = true;

    /*******************************
     * simulation constants        *
     *******************************/
    private double H = 15;
    private double K = 100;
    private double K_NEAR = 10_000;
    private double K_SPRING = 1_000;
    private double P0 = 1;

    // viscosity
    private double SIGMA = 100;  // increase for high viscous fluid; 0 for water
    private double BETA = 10;

    // plasticity
    private double ALPHA = 100;    // plasticity constant
    private double GAMMA = 1;    // yield ratio

    // collision friction [0 - 1]
    private double MU = 0.5;

    private double RADIUS = H / 3;
    private double DRAW_RADIUS = RADIUS * 2;

    private double GRAVITY = -2;

    private double DELTA_T = 0.015;

    private double MAX_VELOCITY = 100;
    private boolean stabilization = true;
    private double VELOCITY_STRETCH = 10;

    /*******************************
     * particle data               *
     *******************************/
    private final int maxParticleCount;
    private final double[] X;
    private final double[] Y;
    private final double[] oldX;
    private final double[] oldY;
    private final double[] vX;
    private final double[] vY;

    private final TIntDoubleHashMap[] springs;

    private final TIntObjectHashMap<TIntArrayList> grid = new TIntObjectHashMap<>();

    private final TDoubleArrayList tmpX = new TDoubleArrayList();
    private final TDoubleArrayList tmpY = new TDoubleArrayList();
    private final TDoubleArrayList tmpInvQ = new TDoubleArrayList();

    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<SourceAndSink> sinks = new ArrayList<>();

    private final Random random = new Random(42);

    private int frames = 0;

    public FluidNew(int maxParticleCount) {
        this.maxParticleCount = maxParticleCount;
        X = new double[maxParticleCount];
        Y = new double[maxParticleCount];
        oldX = new double[maxParticleCount];
        oldY = new double[maxParticleCount];
        vX = new double[maxParticleCount];
        vY = new double[maxParticleCount];

        springs = new TIntDoubleHashMap[maxParticleCount];
        for(int i = 0; i < maxParticleCount; i++) {
            springs[i] = new TIntDoubleHashMap();
        }

        // initialize walls
        obstacles.add(new Wall(0, 10, 20, 0));
        obstacles.add(new Wall(10, 0, 0, -10));
        obstacles.add(new Wall(790, 0, 0, 10));
    }

    public static FluidNew HighViscosity() {
        FluidNew fluid = new FluidNew(4_000);

        // initialize particles
        int side = 40; //(int) Math.ceil(Math.sqrt(maxParticleCount));
        int i = 0;
        for(int x = 0; x < side; x++) {
            if (i >= fluid.maxParticleCount) break;
            for(int y = 0; y < Math.ceil(fluid.maxParticleCount / (double) side); y++) {
                if (i >= fluid.maxParticleCount) break;
                fluid.X[i] = (x * (fluid.H * 0.7)) + 10 + fluid.H / 2 + fluid.random.nextDouble() * fluid.H * 0.2;
                fluid.Y[i] = (y * (fluid.H * 0.7)) + 10 + fluid.H / 2 + fluid.random.nextDouble() * fluid.H * 0.2;

                i++;
            }
        }

        return fluid;
    }

    public static FluidNew LowViscosity() {
        FluidNew fluid = new FluidNew(4_000);

        // initialize particles
        int side = 40; //(int) Math.ceil(Math.sqrt(maxParticleCount));
        int i = 0;
        for(int x = 0; x < side; x++) {
            if (i >= fluid.maxParticleCount) break;
            for(int y = 0; y < Math.ceil(fluid.maxParticleCount / (double) side); y++) {
                if (i >= fluid.maxParticleCount) break;
                fluid.X[i] = (x * (fluid.H * 0.7)) + 10 + fluid.H / 2 + fluid.random.nextDouble() * fluid.H * 0.2;
                fluid.Y[i] = (y * (fluid.H * 0.7)) + 10 + fluid.H / 2 + fluid.random.nextDouble() * fluid.H * 0.2;

                i++;
            }
        }

        // viscosity
        fluid.SIGMA = 0;
        fluid.BETA = 5;

        return fluid;
    }

    public static FluidNew Stack(int n) {
        FluidNew fluid = new FluidNew(n);

        fluid.H = 40;
        fluid.RADIUS = fluid.H / 3;
        fluid.DRAW_RADIUS = fluid.RADIUS;
        fluid.stabilization = false;

        // initialize particles
        int side = 1; //(int) Math.ceil(Math.sqrt(maxParticleCount));
        int i = 0;

        for(int y = 0; y < Math.ceil(fluid.maxParticleCount); y++) {
            if (i >= fluid.maxParticleCount) break;
            fluid.X[i] = 60;
            fluid.Y[i] = (y * (fluid.H * 0.7)) + 10 + fluid.H / 2;

            i++;
        }

        return fluid;
    }

    public static FluidNew TwoParticles() {
        FluidNew fluid = new FluidNew(2);

        fluid.stabilization = false;
        fluid.H = 50;
        fluid.RADIUS = fluid.H / 3;
        fluid.DRAW_RADIUS = fluid.RADIUS;
        fluid.GRAVITY = 0;
        fluid.P0 = 0;
        fluid.K_NEAR = 200_000;
        fluid.VELOCITY_STRETCH = 1;

        fluid.X[0] = 100;
        fluid.Y[0] = 100;
        fluid.X[1] = 200;
        fluid.Y[1] = 101;

        fluid.vX[0] = 50;
        fluid.vY[0] = 0;
        fluid.vX[1] = -50;
        fluid.vY[1] = 0;

        return fluid;
    }

    public static FluidNew Splash() {
        int maxParticles = 2_200;
        FluidNew fluid = new FluidNew(maxParticles);

        fluid.SIGMA = 0;
        fluid.BETA = 5;
        fluid.stabilization = false;

        int i = 0;

        for(int x = 20; x < 800 - 20; x+=fluid.H * 0.6) {
            for (int y = 20; y < 200; y+=fluid.H * 0.6) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
        }

        for(int x = 250; x < 450; x+=fluid.H * 0.7) {
            for (int y = 350; y < 600; y+=fluid.H * 0.7) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
        }

        System.out.println(i);

        return fluid;
    }

    public static FluidNew End() {
        int maxParticles = 4_000;
        FluidNew fluid = new FluidNew(maxParticles);

        double r = 10;

        // E
        fluid.obstacles.add(new Capsule(129, 465, 151, 200, r));

        fluid.obstacles.add(new Capsule(129, 465, 225, 482, r));
        fluid.obstacles.add(new Capsule(140, 332.5, 249, 316, r));
        fluid.obstacles.add(new Capsule(151, 200, 257, 183, r));

        // N
        fluid.obstacles.add(new Capsule(403, 488, 495, 252, r));
        fluid.obstacles.add(new Capsule(495, 252, 270, 456, r));
        fluid.obstacles.add(new Capsule(270, 456, 315, 225, r));

        // D
        fluid.obstacles.add(new Capsule(591, 490, 549, 250, r));
        fluid.obstacles.add(new Capsule(591, 490, 683, 365, r));
        fluid.obstacles.add(new Capsule(683, 365, 549, 250, r));

        fluid.SIGMA = 0;
        fluid.BETA = 5;
        fluid.stabilization = true;
        fluid.MU = 0.1;

        int i = 0;

        for(int x = 20; x < 800 - 20; x+=fluid.H * 0.6) {
            for (int y = 20; y < 200; y+=fluid.H * 0.6) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
        }

        for(int x = 20; x < 800 - 20; x+=fluid.H * 0.6) {
            for (int y = 600; y < 900; y+=fluid.H * 0.6) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
        }

        System.out.println(i);

        return fluid;
    }

    public static FluidNew SourceSink() {

        int maxParticles = 4_000;
        FluidNew fluid = new FluidNew(maxParticles);

        fluid.SIGMA = 0;
        fluid.BETA = 5;
        fluid.MU = 0.05;
        //fluid.P0 = 20;

        fluid.H = 13;
        fluid.RADIUS = fluid.H / 3;
        fluid.DRAW_RADIUS = fluid.RADIUS * 2;

        int i = 0;

        for (int y = 550; y < 900; y+=fluid.H * 0.6) {
            for(int x = 20; x < 800 - 20; x+=fluid.H * 0.6) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
        }

        System.out.println(i);

        fluid.obstacles.add(new Sphere(400, 0, 60));

        double r = 10;
        fluid.obstacles.add(new Capsule(583, 470, 544, 392, r));
        fluid.obstacles.add(new Capsule(555, 335, 544, 392, r));
        fluid.obstacles.add(new Capsule(555, 335, 700, 330, r));
        fluid.obstacles.add(new Capsule(583, 470, 700, 330, r));

        fluid.obstacles.add(new Capsule(232, 469, 300, 370, r));
        fluid.obstacles.add(new Capsule(200, 327, 300, 370, r));
        fluid.obstacles.add(new Capsule(200, 327, 120, 425, r));
        fluid.obstacles.add(new Capsule(232, 469, 120, 425, r));

        fluid.sinks.add(new SourceAndSink(new Capsule(10, 10, 30, 10, 10), 250, 550, 25, 2, -4));
        fluid.sinks.add(new SourceAndSink(new Capsule(790, 10, 770, 10, 10), 550, 550, 25, -2, -4));

        return fluid;
    }

    public static FluidNew Fountain() {
        int maxParticles = 3_256;
        FluidNew fluid = new FluidNew(maxParticles);

        fluid.SIGMA = 0;
        fluid.BETA = 5;
        fluid.MU = 0.05;
        fluid.P0 = 3;
        fluid.stabilization = false;

        fluid.H = 10;
        fluid.RADIUS = fluid.H / 3;
        fluid.DRAW_RADIUS = fluid.RADIUS * 2;

        int i = 0;

        double r = 10;
        double fountainCenterY = 150;

        for (int y = 20; y < fountainCenterY; y+=fluid.H * 0.6) {
            for(int x = 20; x < 400 - r; x+=fluid.H * 0.5) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
            for(int x = 780; x > 400 + r; x-=fluid.H * 0.5) {
                if (i < maxParticles) {
                    fluid.X[i] = x;
                    fluid.Y[i] = y;
                    i++;
                }
            }
        }

        System.out.println(i);

        fluid.obstacles.add(new Capsule(400, fountainCenterY - 4 * r, 400, fountainCenterY - 2 * r, r));

        fluid.sinks.add(new SourceAndSink(new Capsule(10, 10, 10, 20, 5), 400, fountainCenterY, r, 0, 5));
        fluid.sinks.add(new SourceAndSink(new Capsule(790, 10, 790, 20, 5), 400, fountainCenterY, r, 0, 5));

        return fluid;
    }

    public void simulate(double delta_t) {
        frames++;

        // apply gravity forces
        for (int i = 0; i < vY.length; i++) {
            vY[i] += GRAVITY * DELTA_T;
        }

        // update grid
        updateGrid();

        System.arraycopy(vX, 0, oldX, 0, maxParticleCount);
        System.arraycopy(vY, 0, oldY, 0, maxParticleCount);

        // viscosity impulses
        forEachNeighborPair((particle, otherParticle) -> {
            double r = getDistance(particle, otherParticle);

            if (r <= 0) {
//                System.err.println("Two Particles share the same position! (" + particle + ", " + otherParticle + ") " + frames);
            } else {

                double rx = (X[otherParticle] - X[particle]) / r;
                double ry = (Y[otherParticle] - Y[particle]) / r;

                double q = r / H;

                if (q < 1) {
                    // inward radial velocity
                    double u = (oldX[particle] - oldX[otherParticle]) * rx +
                            (oldY[particle] - oldY[otherParticle]) * ry;

                    if (u > 0) {
                        // linear and quadratic impulses
                        double factor = DELTA_T * (1 - q) * (SIGMA * u + BETA * u * u);
                        double Ix = rx * factor;
                        double Iy = ry * factor;

                        double vParticleLength = Math.sqrt(vX[particle] * vX[particle] + vY[particle] * vY[particle]);
                        double vOtherLength = Math.sqrt(vX[otherParticle] * vX[otherParticle] + vY[otherParticle] * vY[otherParticle]);
                        double weight = (vParticleLength) / (vParticleLength + vOtherLength);

                        if (!stabilization) weight = 0.5;

                        vX[particle] -= Ix * weight;
                        vY[particle] -= Iy * weight;
                        vX[otherParticle] += Ix * (1 - weight);
                        vY[otherParticle] += Iy * (1 - weight);
                    }
                }
            }

            return true;
        });

        // limit max velocity
        if(stabilization) {
            MAX_VELOCITY = H / 3;
            double maxSq = MAX_VELOCITY * MAX_VELOCITY;
            for (int i = 0; i < maxParticleCount; i++) {
                if (vX[i] * vX[i] + vY[i] * vY[i] > maxSq) {
                    double magnitude = Math.sqrt(vX[i] * vX[i] + vY[i] * vY[i]);
                    vX[i] = (vX[i] / magnitude) * MAX_VELOCITY;
                    vY[i] = (vY[i] / magnitude) * MAX_VELOCITY;
                }
            }
        }

        // save old particle positions
        System.arraycopy(X, 0, oldX, 0, maxParticleCount);
        System.arraycopy(Y, 0, oldY, 0, maxParticleCount);

        // move particle according to velocity
        for (int i = 0; i < X.length; i++) {
            X[i] += vX[i];
        }
        for (int i = 0; i < Y.length; i++) {
            Y[i] += vY[i];
        }

        // update grid
        updateGrid();

        // adjust springs
        forEachNeighborPair((particle, otherParticle) -> {
            double r = getDistance(particle, otherParticle);

            double q = r / H;
            if (q < 1) {
                if (!springs[particle].containsKey(otherParticle)) {
                    springs[particle].put(otherParticle, H);
                }
                double L = springs[particle].get(otherParticle);

                // tolerable deformation = yield ratio * rest length
                double d = GAMMA * L;
                if (r > L + d) {        // stretch
                    springs[particle].put(otherParticle, L + delta_t * ALPHA * (r - L - d));
                } else if (r < L - d) { // compress
                    springs[particle].put(otherParticle, L - delta_t * ALPHA * (L - d - r));
                }
            }

            return true;
        });

        // remove unnecessary springs
        for(int i = 0; i < springs.length; i++) {
            for (var it = springs[i].iterator(); it.hasNext(); ) {
                it.advance();
                if (getDistance(i, it.key()) > H) {
                    it.remove();
                }
            }
        }

//        // apply spring displacement
//        for(int i = 0; i < springs.length; i++) {
//            final int particle = i;
//            springs[particle].forEachEntry((otherParticle, L) -> {
//                double r = getDistance(particle, otherParticle);
//
//                if (r > 0) {
//                    double rx = (X[otherParticle] - X[particle]) / r;
//                    double ry = (Y[otherParticle] - Y[particle]) / r;
//
//                    double dx = delta_t * delta_t * K_SPRING * (1 - L / H) * (L - r) * rx;
//                    double dy = delta_t * delta_t * K_SPRING * (1 - L / H) * (L - r) * ry;
//
//                    X[particle] -= dx / 2;
//                    Y[particle] -= dy / 2;
//
//                    X[otherParticle] += dx / 2;
//                    Y[otherParticle] += dy / 2;
//                }
//
//                return true;
//            });
//        }

        // update grid
        updateGrid();

        // double density relaxation
        for(int particle_index = 0; particle_index < maxParticleCount; particle_index++) {
            int cellX = posToCell(X[particle_index]);
            int cellY = posToCell(Y[particle_index]);

            tmpX.reset();
            tmpY.reset();
            tmpInvQ.reset();

            // gather vectors to neighbouring particles
            forEachNeighborParticle(particle_index, (a, b) -> a != b, (particle, otherParticle) -> {
                double r = getDistance(particle, otherParticle);
                double q = r / H;

                if (q < 1 && r > 0) {
                    tmpX.add((X[otherParticle] - X[particle]) / r);
                    tmpY.add((Y[otherParticle] - Y[particle]) / r);
                    tmpInvQ.add(1 - q);
                }

                return true;
            });

            double density = 0;
            double density_near = 0;

            // compute density and near-density
            for(int i = 0; i < tmpX.size(); i++) {
                double invQ = tmpInvQ.get(i);

                density += invQ * invQ;
                density_near += density * invQ;
            }

            // compute pressure and near-pressure
            double pressure = K * (density - P0);
            double pressure_near = K_NEAR * density_near;

            double dx = 0;
            double dy = 0;

            for(int i = 0; i < tmpX.size(); i++) {
                double factor = DELTA_T * DELTA_T * tmpInvQ.get(i) * (pressure + pressure_near * tmpInvQ.get(i)) * 0.5;
                dx -= tmpX.get(i) * factor;
                dy -= tmpY.get(i) * factor;
            }

            // apply displacement
            X[particle_index] += dx;
            Y[particle_index] += dy;
        }

        // resolve collisions
        for(int particle_index = 0; particle_index < maxParticleCount; particle_index++) {
            final int particle = particle_index;
            for(var obstacle : obstacles) {
                obstacle.handleCollision(X[particle], Y[particle], (nx, ny, dist) -> {
                    if (dist < RADIUS) {
                        double velocityX = X[particle] - oldX[particle];
                        double velocityY = Y[particle] - oldY[particle];

                        double vNormalX = (velocityX * nx + velocityY * ny) * nx;
                        double vNormalY = (velocityX * nx + velocityY * ny) * ny;

                        double vTangentX = vNormalX - velocityX;
                        double vTangentY = vNormalY - velocityY;

                        X[particle] += (-dist + RADIUS) * nx + MU * vTangentX;
                        Y[particle] += (-dist + RADIUS) * ny + MU * vTangentY;
                    }
                });
            }
        }

        // calculate new particle velocities based on old an new position
        for (int i = 0; i < vX.length; i++) {
            vX[i] = X[i] - oldX[i];
        }
        for (int i = 0; i < vY.length; i++) {
            vY[i] = Y[i] - oldY[i];
        }

        // handle source and sinks
        for(int particle_index = 0; particle_index < maxParticleCount; particle_index++) {
            final int particle = particle_index;
            for(var sink : sinks) {
                sink.handleCollision(X[particle], Y[particle], (nx, ny, dist) -> {
                    if (dist < 0) {
                        X[particle] = sink.sourceX + (random.nextDouble() - 0.5) * 2 * sink.sourceRadius;
                        Y[particle] = sink.sourceY + (random.nextDouble() - 0.5) * 2 * sink.sourceRadius;

                        vX[particle] = sink.vX;
                        vY[particle] = sink.vY;
                    }
                });
            }
        }
    }

    public void applyExternalForce(double x, double y, double force, double radius) {
        for(int i = 0; i < maxParticleCount; i++) {
            double r = Math.sqrt((X[i] - x) * (X[i] - x) + (Y[i] - y) * (Y[i] - y));
            double rx = (X[i] - x) / r;
            double ry = (Y[i] - y) / r;

            if (r < radius && r > 0) {
                vX[i] += rx * force * (1 - r / radius);
                vY[i] += ry * force * (1 - r / radius);
            }
        }
    }

    public void applyExternalForce(double x1, double y1, double x2, double y2, double force) {
        double rsq = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
        double r = Math.sqrt(rsq);
        double cx = x1 + (x2 - x1) * 0.5;
        double cy = y1 + (y2 - y1) * 0.5;
        double nx = (x2 - x1) / r;
        double ny = (y2 - y1) / r;

        for(int i = 0; i < maxParticleCount; i++) {
            double distSq = (X[i] - cx) * (X[i] - cx) + (Y[i] - cy) * (Y[i] - cy);

            if (distSq < rsq) {
                vX[i] += nx * force;
                vY[i] += ny * force;
            }
        }
    }

    private void updateGrid() {
        grid.forEachValue(cell -> {
            cell.resetQuick();
            return true;
        });

        for (int i = 0; i < maxParticleCount; i++) {
            int h = cellToInt(posToCell(X[i]), posToCell(Y[i]));
            TIntArrayList cell = grid.get(h);

            if (cell == null) {
                cell = new TIntArrayList();
                grid.put(h, cell);
            }
            cell.add(i);
        }
    }

    public void draw(GraphicsContext context) {
        context.setStroke(Color.GREY);
        context.setFill(Color.GREY);
        for(var obstacle : obstacles) obstacle.draw(context);
        for(var sink : sinks) sink.draw(context);

        // particles
        for (int i = 0; i < maxParticleCount; i++) {
            if (drawParticle) {
                double v = Math.sqrt(vX[i] * vX[i] + vY[i] * vY[i]);
                Color color = Color.MEDIUMBLUE.interpolate(Color.MEDIUMORCHID, v); //i == 0 ? Color.RED : Color.GREEN;
                color = Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.7);
                context.setFill(color);

                double r = DRAW_RADIUS;
                context.fillOval(X[i] - r, Y[i] - r, r * 2, r * 2);
            }

//            // interaction range
//            if (drawInteractionRadius) {
//                context.setStroke(Color.BLUE);
//                context.strokeOval(X[i] - H, Y[i] - H, H * 2, H * 2);
//            }

//            if (drawSprings) {
//                context.setStroke(Color.LIGHTGREEN);
//                final int particle = i;
//                springs[i].forEachKey(otherParticle -> {
//                    context.strokeLine(X[particle], Y[particle], X[otherParticle], Y[otherParticle]);
//                    return true;
//                });
//            }
        }

        if (drawVelocity) {
            context.setFill(Color.RED);
            for(int i = 0; i < maxParticleCount; i++) {
                double r = 2;
                context.fillOval(X[i] - r, Y[i] - r, r * 2, r * 2);
            }

            context.setStroke(Color.ORANGE);
            context.setLineWidth(2);
            for(int i = 0; i < maxParticleCount; i++) {
                context.strokeLine(X[i], Y[i], X[i] + vX[i] * VELOCITY_STRETCH, Y[i] + vY[i] * VELOCITY_STRETCH);
            }
            context.setLineWidth(1);
        }
    }

    private void forEachNeighborPair(TIntIntProcedure f) {
        for (int particle_index = 0; particle_index < maxParticleCount; particle_index++) {
            forEachNeighborParticle(particle_index, (particle, otherParticle) -> particle < otherParticle, f);
        }
    }

    private void forEachNeighborParticle(int particle, IntIntPredicate predicate, TIntIntProcedure f) {
        int cellX = posToCell(X[particle]);
        int cellY = posToCell(Y[particle]);
        for (int x = cellX - 1; x <= cellX + 1; x++) {
            for (int y = cellY - 1; y <= cellY + 1; y++) {
                TIntArrayList neighbours = grid.get(cellToInt(x, y));
                if (neighbours == null) continue;
                neighbours.forEach(otherParticle -> {
                    if (predicate.test(particle, otherParticle)) {
                        return f.execute(particle, otherParticle);
                    } else {
                        return true;
                    }
                });
            }
        }
    }

    private double getDistance(int i, int j) {
        return Math.sqrt(
                (X[i] - X[j]) * (X[i] - X[j])
              + (Y[i] - Y[j]) * (Y[i] - Y[j]));
    }

    private static int cellToInt(int x, int y) {
        return (x * 73_856_093) ^ (y * 19_349_663);
    }

    private int posToCell(double x) {
        return (int) Math.floor(x / H);
    }

    public void drawMouseOver(GraphicsContext context, double x, double y) {
        int cellX = posToCell(x);
        int cellY = posToCell(y);

        int h = cellToInt(cellX, cellY);
        TIntArrayList cell = grid.get(h);

        context.strokeRect(cellX * H, cellY * H, H, H);

        context.setStroke(Color.RED);
        int neighborCount = 0;
        int particle = 0;
        if (cell != null && cell.size() > 0) {
            double min = Double.POSITIVE_INFINITY;
            for (int offset = 0; offset < cell.size(); offset++) {
                int i = cell.get(offset);
                double d = (X[i] - x) * (X[i] - x) + (Y[i] - y) * (Y[i] - y);
                if (d < min) {
                    min = d;
                    particle = i;
                }
            }

            final int particle_ = particle;
            for (int cx = cellX - 1; cx <= cellX + 1; cx++) {
                for (int cy = cellY - 1; cy <= cellY + 1; cy++) {
                    TIntArrayList neighbours = grid.get(cellToInt(cx, cy));
                    if (neighbours == null) continue;
                    for(int otherParticleIndex = 0; otherParticleIndex < neighbours.size(); otherParticleIndex++) {
                        int otherParticle = neighbours.get(otherParticleIndex);

                        if (particle_ == otherParticle) continue;

                        double r = Math.sqrt((X[otherParticle] - X[particle_]) * (X[otherParticle] - X[particle_])
                                + (Y[otherParticle] - Y[particle_]) * (Y[otherParticle] - Y[particle_]));
                        if (r < H) {
                            context.strokeLine(X[particle_], Y[particle_], X[otherParticle], Y[otherParticle]);
                            neighborCount++;
                        }
                    }
                }
            }

            context.strokeOval(X[particle_] - H, Y[particle_] - H, 2 * H, 2 * H);
        }

        // display cell count
        context.save();
        context.scale(1, -1);
        context.setFill(Color.BLACK);
        context.fillText((cell == null ? "" : cell.size() + (cell.size() > 0 ? " (" + neighborCount + ", #" + particle + ")" : "")), (cellX + 0.5) * H, -(cellY + 0.5) * H - 20);
        context.fillText(x + "/" + y ,(cellX + 0.5) * H, -(cellY + 0.5) * H - 10);
        context.restore();
    }

    interface IntIntPredicate {
        boolean test(int a, int b);
    }
}
