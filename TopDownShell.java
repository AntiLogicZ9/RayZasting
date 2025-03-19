package com.example.raycastinggame;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TopDownShell extends Application {

    // Window settings
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String TITLE = "Simple Top-Down Game";

    // Game objects
    private Canvas gameCanvas;
    private GraphicsContext gc;

    // Game state
    private Player player;
    private List<GameObject> gameObjects = new ArrayList<>();
    private int collectiblesGathered = 0;
    private int totalCollectibles = 0;
    private boolean levelComplete = false;

    // Input handling
    private HashMap<KeyCode, Boolean> keys = new HashMap<>();

    // Game loop
    private long lastNanoTime;

    // Debug
    private boolean debugMode = true;

    @Override
    public void start(Stage primaryStage) {
        // Setup game canvas
        gameCanvas = new Canvas(WIDTH, HEIGHT);
        gc = gameCanvas.getGraphicsContext2D();

        // Create root pane and add canvas
        Pane root = new Pane();
        root.getChildren().add(gameCanvas);

        // Create the scene
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Setup input handling
        scene.setOnKeyPressed(e -> {
            keys.put(e.getCode(), true);

            // Toggle debug mode with F3
            if (e.getCode() == KeyCode.F3) {
                debugMode = !debugMode;
            }
        });
        scene.setOnKeyReleased(e -> keys.put(e.getCode(), false));

        // Configure and show the stage
        primaryStage.setTitle(TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Focus the canvas to receive key events (fixed from scene.requestFocus())
        gameCanvas.requestFocus();

        // Initialize game
        initGame();

        // Start game loop
        lastNanoTime = System.nanoTime();

        new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                // Calculate time delta
                double elapsedTime = (currentNanoTime - lastNanoTime) / 1_000_000_000.0;
                lastNanoTime = currentNanoTime;

                // Cap delta time to avoid huge jumps
                elapsedTime = Math.min(elapsedTime, 0.05);

                // Game logic
                update(elapsedTime);

                // Render
                render();
            }
        }.start();
    }

    private void initGame() {
        // Create player in an open area (away from walls)
        player = new Player(WIDTH / 2, HEIGHT / 2);

        // Add walls around the edges
        gameObjects.add(new Wall(0, 0, WIDTH, 20));
        gameObjects.add(new Wall(0, 0, 20, HEIGHT));
        gameObjects.add(new Wall(0, HEIGHT - 20, WIDTH, 20));
        gameObjects.add(new Wall(WIDTH - 20, 0, 20, HEIGHT));

        // Add some interior walls (moved away from player start position)
        gameObjects.add(new Wall(100, 100, 150, 20));
        gameObjects.add(new Wall(500, 200, 20, 150));
        gameObjects.add(new Wall(550, 400, 150, 20));
        gameObjects.add(new Wall(200, 450, 20, 100));

        // Add collectibles (away from walls)
        gameObjects.add(new Collectible(150, 200));
        gameObjects.add(new Collectible(650, 150));
        gameObjects.add(new Collectible(150, 450));
        gameObjects.add(new Collectible(650, 450));
        gameObjects.add(new Collectible(400, 100));

        // Count total collectibles
        totalCollectibles = 0;
        for (GameObject obj : gameObjects) {
            if (obj instanceof Collectible) {
                totalCollectibles++;
            }
        }
    }

    private void update(double elapsedTime) {
        if (levelComplete) {
            // If level is complete, only check for restart
            if (isKeyPressed(KeyCode.R)) {
                resetGame();
            }
            return;
        }

        // Handle player movement
        double dx = 0;
        double dy = 0;

        if (isKeyPressed(KeyCode.UP) || isKeyPressed(KeyCode.W)) {
            dy -= player.getSpeed() * elapsedTime;
            player.setDirection(Player.Direction.UP);
        }
        if (isKeyPressed(KeyCode.DOWN) || isKeyPressed(KeyCode.S)) {
            dy += player.getSpeed() * elapsedTime;
            player.setDirection(Player.Direction.DOWN);
        }
        if (isKeyPressed(KeyCode.LEFT) || isKeyPressed(KeyCode.A)) {
            dx -= player.getSpeed() * elapsedTime;
            player.setDirection(Player.Direction.LEFT);
        }
        if (isKeyPressed(KeyCode.RIGHT) || isKeyPressed(KeyCode.D)) {
            dx += player.getSpeed() * elapsedTime;
            player.setDirection(Player.Direction.RIGHT);
        }

        // Update player position if there's movement
        if (dx != 0 || dy != 0) {
            movePlayer(dx, dy);
        }

        // Check for level completion
        if (collectiblesGathered >= totalCollectibles) {
            levelComplete = true;
        }
    }

    private void movePlayer(double dx, double dy) {
        // Move on X axis first, then Y axis (separating the movements)
        if (dx != 0) {
            player.setX(player.getX() + dx);

            // Check for collisions on X axis
            boolean collision = false;
            for (GameObject obj : gameObjects) {
                if (obj instanceof Wall && player.collidesWith(obj)) {
                    collision = true;
                    break;
                }
            }

            // If collision occurred, revert the movement
            if (collision) {
                player.setX(player.getX() - dx);
            }
        }

        // Then try Y axis movement
        if (dy != 0) {
            player.setY(player.getY() + dy);

            // Check for collisions on Y axis
            boolean collision = false;
            for (GameObject obj : gameObjects) {
                if (obj instanceof Wall && player.collidesWith(obj)) {
                    collision = true;
                    break;
                }
            }

            // If collision occurred, revert the movement
            if (collision) {
                player.setY(player.getY() - dy);
            }
        }

        // Check for collectible pickups
        List<GameObject> objectsToRemove = new ArrayList<>();

        for (GameObject obj : gameObjects) {
            if (obj instanceof Collectible && player.collidesWith(obj)) {
                objectsToRemove.add(obj);
                collectiblesGathered++;
            }
        }

        // Remove collected items
        gameObjects.removeAll(objectsToRemove);
    }

    private void render() {
        // Clear the canvas
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Render all game objects
        for (GameObject obj : gameObjects) {
            obj.render(gc);
        }

        // Render player
        player.render(gc);

        // Render HUD
        renderHUD();

        // Render debug info if enabled
        if (debugMode) {
            renderDebugInfo();
        }
    }

    private void renderHUD() {
        // Set up text rendering
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // Render collection status
        gc.fillText("Collected: " + collectiblesGathered + "/" + totalCollectibles, 20, 30);

        // Render level complete message if applicable
        if (levelComplete) {
            gc.setFill(new Color(0, 0, 0, 0.7));
            gc.fillRect(0, 0, WIDTH, HEIGHT);

            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 40));
            gc.fillText("Level Complete!", WIDTH/2 - 150, HEIGHT/2 - 20);

            gc.setFont(Font.font("Arial", FontWeight.NORMAL, 20));
            gc.fillText("Press R to restart", WIDTH/2 - 80, HEIGHT/2 + 20);
        }
    }

    private void renderDebugInfo() {
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        // Show player position
        gc.fillText(String.format("Player Position: (%.1f, %.1f)", player.getX(), player.getY()), 20, 60);

        // Show active keys
        StringBuilder keysPressed = new StringBuilder("Keys: ");
        if (isKeyPressed(KeyCode.W)) keysPressed.append("W ");
        if (isKeyPressed(KeyCode.A)) keysPressed.append("A ");
        if (isKeyPressed(KeyCode.S)) keysPressed.append("S ");
        if (isKeyPressed(KeyCode.D)) keysPressed.append("D ");
        if (isKeyPressed(KeyCode.UP)) keysPressed.append("UP ");
        if (isKeyPressed(KeyCode.DOWN)) keysPressed.append("DOWN ");
        if (isKeyPressed(KeyCode.LEFT)) keysPressed.append("LEFT ");
        if (isKeyPressed(KeyCode.RIGHT)) keysPressed.append("RIGHT ");

        gc.fillText(keysPressed.toString(), 20, 80);

        // Show controls help
        gc.fillText("WASD or Arrow Keys to move | F3 to toggle debug | R to restart after winning", 20, HEIGHT - 20);
    }

    private void resetGame() {
        // Clear game objects and reset state
        gameObjects.clear();
        collectiblesGathered = 0;
        levelComplete = false;

        // Re-initialize game
        initGame();
    }

    private boolean isKeyPressed(KeyCode key) {
        return keys.getOrDefault(key, false);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

/**
 * Base class for all game objects
 */
abstract class GameObject {
    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected Color color;

    public GameObject(double x, double y, double width, double height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public boolean collidesWith(GameObject other) {
        // Simple AABB collision detection with a small buffer (0.1 pixels)
        return x < other.x + other.width - 0.1 &&
                x + width > other.x + 0.1 &&
                y < other.y + other.height - 0.1 &&
                y + height > other.y + 0.1;
    }

    public abstract void render(GraphicsContext gc);
}

/**
 * Player class
 */
class Player extends GameObject {
    private static final double DEFAULT_SIZE = 30;
    private static final double DEFAULT_SPEED = 200;

    private double speed;
    private Direction direction;

    enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public Player(double x, double y) {
        super(x, y, DEFAULT_SIZE, DEFAULT_SIZE, Color.BLUE);
        this.speed = DEFAULT_SPEED;
        this.direction = Direction.RIGHT;
    }

    public double getSpeed() {
        return speed;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public void render(GraphicsContext gc) {
        // Draw player body
        gc.setFill(color);
        gc.fillRect(x, y, width, height);

        // Draw direction indicator (a simple triangle)
        gc.setFill(Color.WHITE);

        double centerX = x + width / 2;
        double centerY = y + height / 2;
        double indicatorSize = width / 3;

        switch (direction) {
            case RIGHT:
                gc.fillPolygon(
                        new double[] {centerX + indicatorSize, centerX, centerX},
                        new double[] {centerY, centerY - indicatorSize, centerY + indicatorSize},
                        3
                );
                break;
            case LEFT:
                gc.fillPolygon(
                        new double[] {centerX - indicatorSize, centerX, centerX},
                        new double[] {centerY, centerY - indicatorSize, centerY + indicatorSize},
                        3
                );
                break;
            case UP:
                gc.fillPolygon(
                        new double[] {centerX - indicatorSize, centerX + indicatorSize, centerX},
                        new double[] {centerY, centerY, centerY - indicatorSize},
                        3
                );
                break;
            case DOWN:
                gc.fillPolygon(
                        new double[] {centerX - indicatorSize, centerX + indicatorSize, centerX},
                        new double[] {centerY, centerY, centerY + indicatorSize},
                        3
                );
                break;
        }
    }
}

/**
 * Wall class
 */
class Wall extends GameObject {
    public Wall(double x, double y, double width, double height) {
        super(x, y, width, height, Color.DARKGRAY);
    }

    @Override
    public void render(GraphicsContext gc) {
        gc.setFill(color);
        gc.fillRect(x, y, width, height);

        // Add a simple border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, width, height);
    }
}

/**
 * Collectible class
 */
class Collectible extends GameObject {
    private static final double SIZE = 15;
    private static final double PULSE_SPEED = 2.0;
    private double pulsePhase = 0;

    public Collectible(double x, double y) {
        super(x, y, SIZE, SIZE, Color.GOLD);
    }

    @Override
    public void render(GraphicsContext gc) {
        // Create pulsing effect
        pulsePhase += 0.05;
        double pulseFactor = 1.0 + 0.2 * Math.sin(pulsePhase * PULSE_SPEED);

        double centerX = x + width / 2;
        double centerY = y + height / 2;
        double radius = (width / 2) * pulseFactor;

        // Draw glowing effect
        gc.setFill(Color.rgb(255, 215, 0, 0.3));
        gc.fillOval(centerX - radius * 1.3, centerY - radius * 1.3, radius * 2.6, radius * 2.6);

        // Draw collectible
        gc.setFill(color);
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
}