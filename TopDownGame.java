package com.example.topdowngame;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TopDownGame extends Application {

    // Window settings
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final String TITLE = "Flashlight Game";

    // Game objects
    private Canvas gameCanvas;
    private GraphicsContext gc;

    // Game state
    private Player player;
    private List<GameObject> gameObjects = new ArrayList<>();
    private int collectiblesGathered = 0;
    private int totalCollectibles = 0;
    private boolean levelComplete = false;

    // Vision settings
    private double visionRange = 200; // How far the player can see
    private double visionAngle = 60; // Vision cone angle in degrees

    // Input handling
    private HashMap<KeyCode, Boolean> keys = new HashMap<>();

    // Game loop
    private long lastNanoTime;

    // Debug
    private boolean debugMode = false;
    private boolean showMinimap = true;

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

            // Toggle minimap with M
            if (e.getCode() == KeyCode.M) {
                showMinimap = !showMinimap;
            }
        });
        scene.setOnKeyReleased(e -> keys.put(e.getCode(), false));

        // Configure and show the stage
        primaryStage.setTitle(TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Focus the canvas to receive key events
        gameCanvas.requestFocus();
        gameCanvas.setFocusTraversable(true);

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
        // Create player in the center
        player = new Player(WIDTH / 2, HEIGHT / 2);

        // Add walls around the edges
        gameObjects.add(new Wall(0, 0, WIDTH, 20));
        gameObjects.add(new Wall(0, 0, 20, HEIGHT));
        gameObjects.add(new Wall(0, HEIGHT - 20, WIDTH, 20));
        gameObjects.add(new Wall(WIDTH - 20, 0, 20, HEIGHT));

        // Add some interior walls
        gameObjects.add(new Wall(100, 100, 150, 20));
        gameObjects.add(new Wall(500, 200, 20, 150));
        gameObjects.add(new Wall(550, 400, 150, 20));
        gameObjects.add(new Wall(200, 450, 20, 100));
        gameObjects.add(new Wall(300, 250, 150, 20));
        gameObjects.add(new Wall(300, 150, 20, 100));

        // Add collectibles
        gameObjects.add(new Collectible(150, 200));
        gameObjects.add(new Collectible(650, 150));
        gameObjects.add(new Collectible(150, 450));
        gameObjects.add(new Collectible(650, 450));
        gameObjects.add(new Collectible(400, 100));
        gameObjects.add(new Collectible(300, 370));
        gameObjects.add(new Collectible(500, 500));

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

        // Handle player movement and direction
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
        // Move on X axis
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

        // Move on Y axis
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
        // Clear the canvas with dark color (representing darkness)
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw the vision cone (flashlight, using player's direction)
        drawVisionCone(player.getX() + player.getWidth()/2,
                player.getY() + player.getHeight()/2,
                visionRange,
                getArcAngleFromDirection(player.getDirection()),
                visionAngle);

        // Render objects only if they're in the vision cone or if debug mode is on
        for (GameObject obj : gameObjects) {
            if (isObjectVisible(obj) || debugMode) {
                if (!isObjectVisible(obj) && debugMode) {
                    // If in debug mode, draw invisible objects with transparency
                    obj.render(gc, 0.3);
                } else {
                    obj.render(gc, 1.0);
                }
            }
        }

        // Always render player at full opacity
        player.render(gc);

        // Render minimap if enabled
        if (showMinimap || debugMode) {
            drawMinimap();
        }

        // Render HUD
        renderHUD();

        // Render debug info if enabled
        if (debugMode) {
            renderDebugInfo();
        }
    }

    /**
     * Convert player direction to angle in degrees for JavaFX arcs
     * This mapping is specifically calibrated for JavaFX's arc drawing
     * which uses 0 = East and increases counterclockwise
     */
    private double getArcAngleFromDirection(Player.Direction direction) {
        switch (direction) {
            case RIGHT: return 0;    // East
            case DOWN:  return 270;  // South (FIXED - this was wrong before)
            case LEFT:  return 180;  // West
            case UP:    return 90;   // North (FIXED - this was wrong before)
            default:    return 0;
        }
    }

    /**
     * Convert player direction to vector angle for calculations
     * This uses the standard mathematical angle system
     */
    private double getMathAngleFromDirection(Player.Direction direction) {
        switch (direction) {
            case RIGHT: return 0;    // East = 0 degrees
            case DOWN:  return 90;   // South = 90 degrees
            case LEFT:  return 180;  // West = 180 degrees
            case UP:    return 270;  // North = 270 degrees
            default:    return 0;
        }
    }

    /**
     * Draws the player's vision cone (flashlight) - perfectly aligned with player direction
     */
    private void drawVisionCone(double x, double y, double radius, double arcAngleDegrees, double fovDegrees) {
        // Calculate the start angle for the arc
        double startAngle = arcAngleDegrees - fovDegrees/2;

        // Create a radial gradient for the vision cone
        Stop[] stops = new Stop[] {
                new Stop(0, Color.rgb(255, 255, 200, 0.7)),
                new Stop(0.7, Color.rgb(255, 255, 150, 0.3)),
                new Stop(1.0, Color.rgb(255, 255, 100, 0.0))
        };
        RadialGradient gradient = new RadialGradient(0, 0, x, y, radius, false, CycleMethod.NO_CYCLE, stops);

        // Draw the vision cone
        gc.save();  // Save the current state
        gc.setFill(gradient);

        // Draw an arc centered at the player position
        gc.fillArc(x - radius, y - radius, radius * 2, radius * 2, startAngle, fovDegrees, ArcType.ROUND);

        gc.restore();  // Restore to previous state

        // In debug mode, draw a line to visualize the direction
        if (debugMode) {
            // Get the Math angle (for drawing the correct angle line)
            double mathAngleDegrees = getMathAngleFromDirection(player.getDirection());
            double angleRad = Math.toRadians(mathAngleDegrees);
            double rayEndX = x + Math.cos(angleRad) * radius;
            double rayEndY = y + Math.sin(angleRad) * radius;

            gc.setStroke(Color.RED);
            gc.setLineWidth(2);
            gc.strokeLine(x, y, rayEndX, rayEndY);
        }
    }

    /**
     * Determines if an object is visible in the player's vision cone
     */
    private boolean isObjectVisible(GameObject obj) {
        // Get centers
        double playerCenterX = player.getX() + player.getWidth()/2;
        double playerCenterY = player.getY() + player.getHeight()/2;
        double objCenterX = obj.getX() + obj.getWidth()/2;
        double objCenterY = obj.getY() + obj.getHeight()/2;

        // Calculate distance
        double dx = objCenterX - playerCenterX;
        double dy = objCenterY - playerCenterY;
        double distance = Math.sqrt(dx*dx + dy*dy);

        // If object is too far, it's not visible
        if (distance > visionRange) {
            return false;
        }

        // Calculate angle to object in degrees
        double angleToObject = Math.toDegrees(Math.atan2(dy, dx));

        // Normalize to 0-360
        if (angleToObject < 0) angleToObject += 360;

        // Get player direction angle (using MATH angle, not arc angle)
        double directionAngle = getMathAngleFromDirection(player.getDirection());

        // Calculate angle difference
        double angleDiff = Math.abs(angleToObject - directionAngle);
        if (angleDiff > 180) angleDiff = 360 - angleDiff; // Handle wraparound

        // If angle difference is within half the vision angle, object is visible
        return angleDiff <= visionAngle/2;
    }

    private void renderHUD() {
        // Set up text rendering
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 20));

        // Render collection status
        gc.fillText("Collected: " + collectiblesGathered + "/" + totalCollectibles, 20, 30);

        // Show controls
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gc.fillText("WASD/Arrows: Move, M: Toggle minimap, F3: Debug Mode", 20, HEIGHT - 20);

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

    private void drawMinimap() {
        int mapSize = 150;
        int mapX = WIDTH - mapSize - 10;
        int mapY = 10;
        double scale = 0.15;

        // Draw background
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(mapX, mapY, mapSize, mapSize);

        // Draw walls
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(1);
        for (GameObject obj : gameObjects) {
            if (obj instanceof Wall) {
                gc.strokeRect(
                        mapX + obj.getX() * scale,
                        mapY + obj.getY() * scale,
                        obj.getWidth() * scale,
                        obj.getHeight() * scale
                );
            } else if (obj instanceof Collectible) {
                // Draw collectibles as dots
                double collectibleX = mapX + obj.getX() * scale + (obj.getWidth() * scale / 2);
                double collectibleY = mapY + obj.getY() * scale + (obj.getHeight() * scale / 2);
                gc.setFill(Color.GOLD);
                gc.fillOval(collectibleX - 2, collectibleY - 2, 4, 4);
            }
        }

        // Draw player
        double playerX = mapX + player.getX() * scale;
        double playerY = mapY + player.getY() * scale;
        double playerW = player.getWidth() * scale;
        double playerH = player.getHeight() * scale;

        gc.setFill(Color.BLUE);
        gc.fillRect(playerX, playerY, playerW, playerH);

        // Draw player vision cone on minimap
        double playerCenterX = playerX + playerW/2;
        double playerCenterY = playerY + playerH/2;
        double miniVisionRange = visionRange * scale;

        // Get player direction angle
        double arcAngle = getArcAngleFromDirection(player.getDirection());
        double startAngle = arcAngle - visionAngle/2;

        gc.setFill(Color.rgb(255, 255, 100, 0.3));
        gc.fillArc(playerCenterX - miniVisionRange, playerCenterY - miniVisionRange,
                miniVisionRange * 2, miniVisionRange * 2,
                startAngle, visionAngle, ArcType.ROUND);

        // In debug mode, draw a direction line
        if (debugMode) {
            // Use math angle for line drawing
            double mathAngle = getMathAngleFromDirection(player.getDirection());
            double angleRad = Math.toRadians(mathAngle);
            double dirX = playerCenterX + Math.cos(angleRad) * playerW * 2;
            double dirY = playerCenterY + Math.sin(angleRad) * playerW * 2;

            gc.setStroke(Color.RED);
            gc.setLineWidth(1);
            gc.strokeLine(playerCenterX, playerCenterY, dirX, dirY);
        }
    }

    private void renderDebugInfo() {
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", FontWeight.NORMAL, 14));

        // Show player position
        gc.fillText(String.format("Player Position: (%.1f, %.1f)", player.getX(), player.getY()), 20, 60);

        // Show player direction
        gc.fillText("Player Direction: " + player.getDirection(), 20, 80);

        // Show direction angles
        double mathAngle = getMathAngleFromDirection(player.getDirection());
        double arcAngle = getArcAngleFromDirection(player.getDirection());
        gc.fillText(String.format("Math Angle: %.1f°, Arc Angle: %.1f°", mathAngle, arcAngle), 20, 100);

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

        gc.fillText(keysPressed.toString(), 20, 120);

        // Show vision settings
        gc.fillText(String.format("Vision Range: %.0f, Angle: %.0f°", visionRange, visionAngle), 20, 140);
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

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public boolean collidesWith(GameObject other) {
        // Simple AABB collision detection with a small buffer (0.1 pixels)
        return x < other.x + other.width - 0.1 &&
                x + width > other.x + 0.1 &&
                y < other.y + other.height - 0.1 &&
                y + height > other.y + 0.1;
    }

    // Overloaded render method with opacity parameter
    public void render(GraphicsContext gc, double opacity) {
        Color adjustedColor = color.deriveColor(0, 1, 1, opacity);
        gc.setFill(adjustedColor);
        doRender(gc);
    }

    // Original render method for backward compatibility
    public void render(GraphicsContext gc) {
        render(gc, 1.0);
    }

    // Abstract method for the actual rendering
    protected abstract void doRender(GraphicsContext gc);
}

/**
 * Player class with discrete direction-based movement
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

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    protected void doRender(GraphicsContext gc) {
        // Draw player body
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
    protected void doRender(GraphicsContext gc) {
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
    protected void doRender(GraphicsContext gc) {
        // Create pulsing effect
        pulsePhase += 0.05;
        double pulseFactor = 1.0 + 0.2 * Math.sin(pulsePhase * PULSE_SPEED);

        double centerX = x + width / 2;
        double centerY = y + height / 2;
        double radius = (width / 2) * pulseFactor;

        // Draw glowing effect
        Color glowColor = Color.rgb(255, 215, 0, 0.3);
        if (gc.getFill() instanceof Color) {
            Color baseColor = (Color) gc.getFill();
            glowColor = Color.rgb(255, 215, 0, baseColor.getOpacity() * 0.3);
        }

        gc.setFill(glowColor);
        gc.fillOval(centerX - radius * 1.3, centerY - radius * 1.3, radius * 2.6, radius * 2.6);

        // Draw collectible
        if (gc.getFill() instanceof Color) {
            gc.setFill((Color) gc.getFill());
        }
        gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }
}