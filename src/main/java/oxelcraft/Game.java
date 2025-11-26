package oxelcraft;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Game {

    private long window;
    private int width = 800;
    private int height = 600;
    private boolean running = true;

    // Input
    private boolean up, down, left, right;

    // Player
    private float playerX = 400 - 16;
    private float playerY = 300 - 16;
    private final float playerSize = 32;
    private final float playerSpeed = 220f;

    // Enemies
    private static class Enemy {
        float x, y, size, speedX;
        Enemy(float x, float y, float size, float speedX) {
            this.x = x; this.y = y; this.size = size; this.speedX = speedX;
        }
    }
    private final List<Enemy> enemies = new ArrayList<>();
    private final Random rnd = new Random();
    private double spawnTimer = 0;

    // Game state
    private int score = 0;
    private boolean gameOver = false;

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("Cannot initialize GLFW");

        // CREATE WINDOW with exact title "Oxelcraft"
        window = GLFW.glfwCreateWindow(width, height, "Oxelcraft", 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();

        // Keyboard callback
        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            boolean pressed = action != GLFW.GLFW_RELEASE;
            if (key == GLFW.GLFW_KEY_W || key == GLFW.GLFW_KEY_UP) up = pressed;
            if (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_DOWN) down = pressed;
            if (key == GLFW.GLFW_KEY_A || key == GLFW.GLFW_KEY_LEFT) left = pressed;
            if (key == GLFW.GLFW_KEY_D || key == GLFW.GLFW_KEY_RIGHT) right = pressed;

            if (key == GLFW.GLFW_KEY_R && pressed && gameOver) restart();
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS)
                GLFW.glfwSetWindowShouldClose(window, true);
        });

        // 2D orthographic projection
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);

        enemies.clear();
        spawnEnemy();
    }

    private void loop() {
        double lastTime = GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window) && running) {
            double now = GLFW.glfwGetTime();
            float delta = (float)(now - lastTime);
            lastTime = now;

            update(delta);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void update(float dt) {
        if (gameOver) return;

        // Player movement
        float dx = 0, dy = 0;
        if (up) dy -= 1;
        if (down) dy += 1;
        if (left) dx -= 1;
        if (right) dx += 1;
        if (dx != 0 && dy != 0) { dx *= 0.7071f; dy *= 0.7071f; }

        playerX += dx * playerSpeed * dt;
        playerY += dy * playerSpeed * dt;

        playerX = clamp(playerX, 0, width - playerSize);
        playerY = clamp(playerY, 0, height - playerSize);

        spawnTimer += dt;
        if (spawnTimer > Math.max(0.7, 2.0 - score * 0.05)) {
            spawnTimer = 0;
            spawnEnemy();
        }

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.x += e.speedX * dt;

            if (e.x + e.size < 0) { it.remove(); score++; continue; }

            if (rectsOverlap(playerX, playerY, playerSize, playerSize, e.x, e.y, e.size, e.size)) {
                gameOver = true;
            }
        }
    }

    private void render() {
        glClear(GL_COLOR_BUFFER_BIT);
        glClearColor(0.08f, 0.08f, 0.12f, 1.0f);

        // Player
        drawRect(playerX, playerY, playerSize, playerSize, 0.2f, 0.9f, 0.3f);

        // Enemies
        for (Enemy e : enemies) {
            drawRect(e.x, e.y, e.size, e.size, 0.9f, 0.25f, 0.25f);
        }

        // Score bar
        drawRect(12, 16, Math.min(100, score), 12, 0.2f, 0.6f, 0.9f);

        if (gameOver) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            drawRect(0,0,width,height,0.7f,0f,0f,0.45f);
            glDisable(GL_BLEND);
        }
    }

    private void drawRect(float x, float y, float w, float h, float r, float g, float b) {
        drawRect(x,y,w,h,r,g,b,1f);
    }

    private void drawRect(float x, float y, float w, float h, float r, float g, float b, float a) {
        glColor4f(r,g,b,a);
        glBegin(GL_QUADS);
        glVertex2f(x, y);
        glVertex2f(x + w, y);
        glVertex2f(x + w, y + h);
        glVertex2f(x, y + h);
        glEnd();
        glColor4f(1f,1f,1f,1f);
    }

    private void spawnEnemy() {
        float size = 18 + rnd.nextFloat()*36;
        float y = 20 + rnd.nextFloat()*(height - 40 - size);
        float x = width + 10;
        float speed = -80 - rnd.nextFloat()*(120 + score*2);
        enemies.add(new Enemy(x,y,size,speed));
    }

    private void restart() {
        playerX = width/2 - playerSize/2;
        playerY = height/2 - playerSize/2;
        enemies.clear();
        score = 0;
        gameOver = false;
        spawnTimer = 0;
        spawnEnemy();
    }

    private boolean rectsOverlap(float x1,float y1,float w1,float h1,float x2,float y2,float w2,float h2) {
        return x1 < x2+w2 && x1+w1 > x2 && y1 < y2+h2 && y1+h1 > y2;
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private void cleanup() {
        GLFW.glfwTerminate();
    }
}
