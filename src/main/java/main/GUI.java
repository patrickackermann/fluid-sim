package main;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Affine;
import javafx.stage.Stage;
import simulation.FluidNew;

import java.util.*;
import java.util.function.Supplier;

public class GUI extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;

    private boolean running;
    private static final double DELTA_T = 0.0333333; //0.0333333;
    private static final int MAX_PARTICLES = 1000;

    private double mouseX;
    private double mouseY;
    private double mouseXold;
    private double mouseYold;
    private boolean mouseMoved = false;

    private Supplier<FluidNew> FluidFactory = () -> new FluidNew(0);
    private Map<String, Supplier<FluidNew>> sceneOptions = Map.of(
            "High Viscosity", FluidNew::HighViscosity,
            "Low Viscosity", FluidNew::LowViscosity,
            "Stack Huge", () -> FluidNew.Stack(1000),
            "Stack 100", () -> FluidNew.Stack(100),
            "Stack 20", () -> FluidNew.Stack(20),
            "Two Particles", FluidNew::TwoParticles,
            "Splash", FluidNew::Splash,
            "End", FluidNew::End,
            "SourceSink", FluidNew::SourceSink,
            "Fountain", FluidNew::Fountain
    );

    FluidNew fluid;
    GraphicsContext context;
    Canvas canvas;

    private void drawFluid() {
        Affine transform = context.getTransform();
        context.setTransform(new Affine());
        context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        context.setTransform(transform);
        fluid.draw(context);
    }

    private void toggleDisplayStats() {
        fluid.drawVelocity = !fluid.drawVelocity;
        fluid.drawSprings = !fluid.drawSprings;
        fluid.drawInteractionRadius = !fluid.drawInteractionRadius;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        fluid = FluidFactory.get();
        toggleDisplayStats();
        canvas = new Canvas(WIDTH, HEIGHT);
        context = canvas.getGraphicsContext2D();
        Affine transform = new Affine();
        transform.appendScale(1, -1);
        transform.appendTranslation(0, -HEIGHT);
        context.setTransform(transform);

        Label particleCount = new Label();
//        particleCount.textProperty().bind(Bindings.size(fluid.particles).asString());

        Button stepButton = new Button("Step");
        stepButton.setOnAction(e -> {
            if (!running) {
                fluid.simulate(DELTA_T);
                drawFluid();
            }
        });
        Button playButton = new Button("Play");
        playButton.setOnAction(e -> {
            if (running) {
                playButton.setText("Play");
                running = false;
            } else {
                playButton.setText("Stop");
                running = true;
            }
        });
        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            boolean displayStats = fluid.drawVelocity;

            fluid = FluidFactory.get();
//            particleCount.textProperty().bind(Bindings.size(fluid.particles).asString());

            if (!displayStats) {
                toggleDisplayStats();
            }

            drawFluid();
        });
        Button onlyParticleButton = new Button("Display");
        onlyParticleButton.setOnAction(e -> {
            toggleDisplayStats();

            drawFluid();
        });
        ComboBox<String> sceneSelection = new ComboBox<>();
        List<String> options = new ArrayList<>(sceneOptions.keySet());
        Collections.sort(options);
        sceneSelection.getItems().addAll(options);
        sceneSelection.setOnAction(e -> {
            if (running) {
                playButton.setText("Play");
                running = false;
            }

            FluidFactory = sceneOptions.get(sceneSelection.getSelectionModel().getSelectedItem());
            fluid = FluidFactory.get();

            toggleDisplayStats();
        });

        Node space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        HBox controls = new HBox(particleCount, space, playButton, stepButton, resetButton, onlyParticleButton, sceneSelection);
        controls.setAlignment(Pos.TOP_RIGHT);

        StackPane root = new StackPane();
        root.getChildren().addAll(canvas, controls);
        StackPane.setAlignment(controls, Pos.TOP_RIGHT);

//        root.setOnMouseMoved(e -> {
//            if (e.isControlDown()) {
//                fluid.particles.add(new Particle(new Point2D(e.getX(), root.getHeight() - e.getY()), Point2D.ZERO));
//            }
//        });
//
//        root.setOnKeyPressed(e -> {
//            if (e.getCode() == KeyCode.A) {
//                fluid.produce = true;
//            } else if (e.getCode() == KeyCode.S) {
//                fluid.produce2 = true;
//            }
//        });
//
//        root.setOnKeyReleased(e -> {
//            if (e.getCode() == KeyCode.A) {
//                fluid.produce = false;
//            } else if (e.getCode() == KeyCode.S) {
//                fluid.produce2 = false;
//            }
//        });

        root.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = root.getHeight() - e.getY();
            if (!running) {
                drawFluid();
                fluid.drawMouseOver(context, mouseX, mouseY);
            }

            if (e.isControlDown()) {
                fluid.applyExternalForce(mouseX, mouseY, 1, 50);
            } else if (e.isShiftDown()) {
                fluid.applyExternalForce(mouseX, mouseY, -1, 100);
            } else if (e.isAltDown()) {
                fluid.applyExternalForce(mouseXold, mouseYold, mouseX, mouseY, 3);
            }
            mouseXold = mouseX;
            mouseYold = mouseY;
        });

        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.show();

        fluid.draw(context);

        new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (running) {
                    fluid.simulate(DELTA_T);
                    fluid.simulate(DELTA_T);
                    drawFluid();
                    fluid.drawMouseOver(context, mouseX, mouseY);
                }
            }
        }.start();
    }
}
