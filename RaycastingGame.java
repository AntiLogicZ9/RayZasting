package com.example.raycastinggame;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.util.HashSet;
import java.util.Set;

public class RaycastingGame extends Application {
    // Window dimensions
    private static final int SCREEN_WIDTH = 1920;
    private static final int SCREEN_HEIGHT = 1080;

    // Game components
    private Player player;
    private Map map;
    private Renderer renderer;
    private InputHandler inputHandler;

    // Input tracking
    private Set<KeyCode> activeKeys = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        // Create our canvas to draw on
        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Initialize game components
        map = new Map();
        player = new Player(2.5, 2.5, 1, 0); // Starting position and direction
        renderer = new Renderer(gc, map, player, SCREEN_WIDTH, SCREEN_HEIGHT);
        inputHandler = new InputHandler(player);

        // Set up the scene and event handlers
        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);

        scene.setOnKeyPressed(e -> activeKeys.add(e.getCode()));
        scene.setOnKeyReleased(e -> activeKeys.remove(e.getCode()));

        // Main game loop
        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                // Calculate delta time in seconds
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                // Update game state
                inputHandler.handleInput(activeKeys, deltaTime);

                // Render the game
                renderer.render();
            }
        };

        // Set up the window
        primaryStage.setTitle("Raycasting Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Start the game loop
        gameLoop.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Player {
    // Position
    private double x;
    private double y;

    // Direction vector
    private double dirX;
    private double dirY;

    // Camera plane - perpendicular to direction
    private double planeX;
    private double planeY;

    // Movement properties
    private double moveSpeed = 3.0; // units per second
    private double rotSpeed = 2.0; // radians per second

    public Player(double x, double y, double dirX, double dirY) {
        this.x = x;
        this.y = y;
        this.dirX = dirX;
        this.dirY = dirY;

        // Initialize the camera plane perpendicular to direction vector
        // This determines FOV - the ratio between planeLength and dirLength
        this.planeX = 0.0;
        this.planeY = 0.90; // FOV of approximately 66 degrees
    }

    public void move(double dx, double dy, Map map) {
        // Check for collision before moving
        if (!map.isWall((int)(x + dx), (int)y)) {
            x += dx;
        }

        if (!map.isWall((int)x, (int)(y + dy))) {
            y += dy;
        }
    }

    public void rotate(double angle) {
        // Rotate direction vector and camera plane
        double oldDirX = dirX;
        dirX = dirX * Math.cos(angle) - dirY * Math.sin(angle);
        dirY = oldDirX * Math.sin(angle) + dirY * Math.cos(angle);

        double oldPlaneX = planeX;
        planeX = planeX * Math.cos(angle) - planeY * Math.sin(angle);
        planeY = oldPlaneX * Math.sin(angle) + planeY * Math.cos(angle);
    }

    // Movement methods
    public void moveForward(double deltaTime, Map map) {
        double dx = dirX * moveSpeed * deltaTime;
        double dy = dirY * moveSpeed * deltaTime;
        move(dx, dy, map);
    }

    public void moveBackward(double deltaTime, Map map) {
        double dx = -dirX * moveSpeed * deltaTime;
        double dy = -dirY * moveSpeed * deltaTime;
        move(dx, dy, map);
    }

    public void strafeLeft(double deltaTime, Map map) {
        // Move perpendicular to direction vector
        double dx = -dirY * moveSpeed * deltaTime;
        double dy = dirX * moveSpeed * deltaTime;
        move(dx, dy, map);
    }

    public void strafeRight(double deltaTime, Map map) {
        // Move perpendicular to direction vector
        double dx = dirY * moveSpeed * deltaTime;
        double dy = -dirX * moveSpeed * deltaTime;
        move(dx, dy, map);
    }

    public void turnLeft(double deltaTime) {
        rotate(-rotSpeed * deltaTime);
    }

    public void turnRight(double deltaTime) {
        rotate(rotSpeed * deltaTime);
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public double getDirX() { return dirX; }
    public double getDirY() { return dirY; }
    public double getPlaneX() { return planeX; }
    public double getPlaneY() { return planeY; }

    // Setters
    public void setMoveSpeed(double moveSpeed) { this.moveSpeed = moveSpeed; }
    public void setRotSpeed(double rotSpeed) { this.rotSpeed = rotSpeed; }
}

class Map {
    // Simple map layout - 1 represents a wall, 0 is empty space
    private final int[][] mapData = {
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 0, 0, 0, 0, 0, 0, 0, 0, 1},
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}
    };

    public int getWidth() {
        return mapData[0].length;
    }

    public int getHeight() {
        return mapData.length;
    }

    public boolean isWall(int x, int y) {
        // Check bounds
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return true; // Treat out of bounds as walls
        }

        return mapData[y][x] == 1;
    }

    public int getMapValue(int x, int y) {
        if (x < 0 || x >= getWidth() || y < 0 || y >= getHeight()) {
            return 0;
        }
        return mapData[y][x];
    }
}

class Renderer {
    private final GraphicsContext gc;
    private final Map map;
    private final Player player;
    private final int screenWidth;
    private final int screenHeight;

    // Colors
    private final Color ceilingColor = Color.DARKBLUE;
    private final Color floorColor = Color.DARKGRAY;
    private final Color[] wallColors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    public Renderer(GraphicsContext gc, Map map, Player player, int screenWidth, int screenHeight) {
        this.gc = gc;
        this.map = map;
        this.player = player;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void render() {
        // Clear the screen
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, screenWidth, screenHeight);

        // Draw ceiling
        gc.setFill(ceilingColor);
        gc.fillRect(0, 0, screenWidth, screenHeight / 2);

        // Draw floor
        gc.setFill(floorColor);
        gc.fillRect(0, screenHeight / 2, screenWidth, screenHeight / 2);

        // Cast rays for each column of the screen
        for (int x = 0; x < screenWidth; x++) {
            // Calculate ray position and direction
            double cameraX = 2 * x / (double)screenWidth - 1; // x-coordinate in camera space
            double rayDirX = player.getDirX() + player.getPlaneX() * cameraX;
            double rayDirY = player.getDirY() + player.getPlaneY() * cameraX;

            // Which box of the map we're in
            int mapX = (int)player.getX();
            int mapY = (int)player.getY();

            // Length of ray from current position to next x or y-side
            double sideDistX;
            double sideDistY;

            // Length of ray from one x or y-side to next x or y-side
            double deltaDistX = Math.abs(1 / rayDirX);
            double deltaDistY = Math.abs(1 / rayDirY);

            // What direction to step in x or y direction (either +1 or -1)
            int stepX;
            int stepY;

            // Variables for hit detection
            boolean hit = false;
            int side = 0; // was a NS or a EW wall hit?

            // Calculate step and initial sideDist
            if (rayDirX < 0) {
                stepX = -1;
                sideDistX = (player.getX() - mapX) * deltaDistX;
            } else {
                stepX = 1;
                sideDistX = (mapX + 1.0 - player.getX()) * deltaDistX;
            }

            if (rayDirY < 0) {
                stepY = -1;
                sideDistY = (player.getY() - mapY) * deltaDistY;
            } else {
                stepY = 1;
                sideDistY = (mapY + 1.0 - player.getY()) * deltaDistY;
            }

            // Perform DDA (Digital Differential Analysis)
            while (!hit) {
                // Jump to next map square, either in x-direction, or in y-direction
                if (sideDistX < sideDistY) {
                    sideDistX += deltaDistX;
                    mapX += stepX;
                    side = 0;
                } else {
                    sideDistY += deltaDistY;
                    mapY += stepY;
                    side = 1;
                }

                // Check if ray has hit a wall
                if (map.isWall(mapX, mapY)) {
                    hit = true;
                }
            }

            // Calculate distance projected on camera direction
            double perpWallDist;
            if (side == 0) {
                perpWallDist = (mapX - player.getX() + (1 - stepX) / 2) / rayDirX;
            } else {
                perpWallDist = (mapY - player.getY() + (1 - stepY) / 2) / rayDirY;
            }

            // Calculate height of line to draw on screen
            int lineHeight = (int)(screenHeight / perpWallDist);

            // Calculate lowest and highest pixel to fill in current stripe
            int drawStart = -lineHeight / 2 + screenHeight / 2;
            if (drawStart < 0) drawStart = 0;
            int drawEnd = lineHeight / 2 + screenHeight / 2;
            if (drawEnd >= screenHeight) drawEnd = screenHeight - 1;

            // Choose wall color based on map value
            Color color = wallColors[map.getMapValue(mapX, mapY) % wallColors.length];

            // Make color darker for y-sides
            if (side == 1) {
                color = color.darker();
            }

            // Draw the vertical line
            gc.setStroke(color);
            gc.setLineWidth(1);
            gc.strokeLine(x, drawStart, x, drawEnd);
        }
    }
}

class InputHandler {
    private final Player player;
    private final Map map;

    public InputHandler(Player player) {
        this.player = player;
        this.map = new Map(); // Assuming we want to use the same map instance
    }

    public void handleInput(Set<KeyCode> activeKeys, double deltaTime) {
        // Forward/backward movement
        if (activeKeys.contains(KeyCode.W)) {
            player.moveForward(deltaTime, map);
        }
        if (activeKeys.contains(KeyCode.S)) {
            player.moveBackward(deltaTime, map);
        }

        // Strafing left/right
        if (activeKeys.contains(KeyCode.A)) {
            player.strafeLeft(deltaTime, map);
        }
        if (activeKeys.contains(KeyCode.D)) {
            player.strafeRight(deltaTime, map);
        }

        // Turning left/right
        if (activeKeys.contains(KeyCode.LEFT)) {
            player.turnLeft(deltaTime);
        }
        if (activeKeys.contains(KeyCode.RIGHT)) {
            player.turnRight(deltaTime);
        }
    }
}