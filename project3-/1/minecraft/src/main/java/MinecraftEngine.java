import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class MinecraftEngine {
    // Window handle
    private long window;

    // Window dimensions
    private int width = 800;
    private int height = 600;

    // Game components
    private World world;
    private Camera camera;

    // Block type to place
    private int currentBlockType = Block.GRASS;

    // Mouse capture toggle
    private boolean mouseCaptured = true;

    public void run() {
        System.out.println("Starting Minecraft Engine");
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Architecture: " + System.getProperty("os.arch"));

        init();
        loop();
        cleanup();
    }

    private void init() {
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        // Create window
        window = glfwCreateWindow(width, height, "Minecraft Engine", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        // Setup key callback
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                // Toggle mouse capture instead of closing window
                toggleMouseCapture();
            }

            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_1: currentBlockType = Block.STONE; System.out.println("Selected Stone"); break;
                    case GLFW_KEY_2: currentBlockType = Block.DIRT; System.out.println("Selected Dirt"); break;
                    case GLFW_KEY_3: currentBlockType = Block.GRASS; System.out.println("Selected Grass"); break;
                    case GLFW_KEY_4: currentBlockType = Block.WOOD; System.out.println("Selected Wood"); break;
                    case GLFW_KEY_5: currentBlockType = Block.LEAVES; System.out.println("Selected Leaves"); break;

                    // Building/destroying blocks
                    case GLFW_KEY_E: destroyBlock(); break;
                    case GLFW_KEY_Q: placeBlock(); break;

                    // Add Tab key to close the window
                    case GLFW_KEY_TAB: glfwSetWindowShouldClose(window, true); break;
                }
            }
        });

        // Setup mouse button callback
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (action == GLFW_PRESS && mouseCaptured) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    destroyBlock();
                } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    placeBlock();
                }
            }
        });

        // Window resize callback
        glfwSetFramebufferSizeCallback(window, (window, w, h) -> {
            width = w;
            height = h;
            glViewport(0, 0, width, height);
            updateProjection();
        });

        // Capture cursor for free mouse movement
        captureMouse();

        // Center window
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidmode != null) {
                glfwSetWindowPos(window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2);
            }
        }

        // OpenGL context
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // V-sync
        glfwShowWindow(window);
        GL.createCapabilities();

        // OpenGL setup
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glViewport(0, 0, width, height);

        updateProjection();

        // Game setup
        world = new World();
        camera = new Camera(0, 12, 0);

        // Create some initial blocks to interact with
        createInitialWorld();
    }

    private void createInitialWorld() {
        // Create a platform to stand on
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                world.setBlock(x, 8, z, Block.GRASS);
                world.setBlock(x, 7, z, Block.DIRT);
                world.setBlock(x, 6, z, Block.DIRT);
            }
        }

        // Create a few blocks to test breaking/building
        world.setBlock(0, 9, 0, Block.STONE);
        world.setBlock(1, 9, 0, Block.WOOD);
        world.setBlock(0, 9, 1, Block.LEAVES);
    }

    private void captureMouse() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        mouseCaptured = true;
        System.out.println("Mouse captured - ESC to release cursor, TAB to exit game");
    }

    private void releaseMouse() {
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        mouseCaptured = false;
        System.out.println("Mouse released - ESC to capture cursor again, TAB to exit game");
    }

    private void toggleMouseCapture() {
        if (mouseCaptured) {
            releaseMouse();
        } else {
            captureMouse();
        }
    }

    private void updateProjection() {
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        float fov = 70.0f;
        float aspectRatio = (float)width / (float)height;
        float near = 0.1f;
        float far = 1000.0f;

        float yScale = (float)(1.0 / Math.tan(Math.toRadians(fov / 2)));
        float xScale = yScale / aspectRatio;
        float frustumLength = far - near;

        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) matrix[i] = 0;

        matrix[0] = xScale;
        matrix[5] = yScale;
        matrix[10] = -((far + near) / frustumLength);
        matrix[11] = -1;
        matrix[14] = -((2 * near * far) / frustumLength);

        glLoadMatrixf(matrix);
        glMatrixMode(GL_MODELVIEW);
    }

    private void destroyBlock() {
        if (!mouseCaptured) return;

        System.out.println("Attempting to destroy block...");
        float[] pos = new float[]{camera.getX(), camera.getY(), camera.getZ()};
        float[] dir = getCameraDirection();

        System.out.println("Camera at: " + pos[0] + ", " + pos[1] + ", " + pos[2]);
        System.out.println("Looking direction: " + dir[0] + ", " + dir[1] + ", " + dir[2]);

        // First try the normal raycast
        int[] block = raycast(pos, dir, 5.0f);

        if (block != null) {
            System.out.println("Found block to destroy at: " + block[0] + ", " + block[1] + ", " + block[2]);
            world.setBlock(block[0], block[1], block[2], Block.AIR);
        } else {
            // If normal raycast fails, try destroying blocks directly in front of the player
            System.out.println("No block found with raycast. Testing direct destroy...");
            directDestroyBlock();
        }
    }

    private void directDestroyBlock() {
        // Try to destroy a block directly in front of the camera
        float distance = 2.0f;
        float[] dir = getCameraDirection();
        float hitX = camera.getX() + dir[0] * distance;
        float hitY = camera.getY() + dir[1] * distance;
        float hitZ = camera.getZ() + dir[2] * distance;

        int blockX = (int) Math.floor(hitX);
        int blockY = (int) Math.floor(hitY);
        int blockZ = (int) Math.floor(hitZ);

        // Check if this block exists and is not air
        Block block = world.getBlock(blockX, blockY, blockZ);
        if (block != null && block.getType() != Block.AIR) {
            System.out.println("Direct destroy - Block found at: " + blockX + ", " + blockY + ", " + blockZ);
            world.setBlock(blockX, blockY, blockZ, Block.AIR);
        } else {
            System.out.println("No block found to destroy");
        }
    }

    private void placeBlock() {
        if (!mouseCaptured) return;

        System.out.println("Attempting to place " + currentBlockType + " block...");
        float[] pos = new float[]{camera.getX(), camera.getY(), camera.getZ()};
        float[] dir = getCameraDirection();

        // First try the normal raycast placement
        int[] hit = raycast(pos, dir, 5.0f);
        if (hit != null) {
            int face = hit[3];
            int placeX = hit[0], placeY = hit[1], placeZ = hit[2];

            // Determine placement position based on hit face
            switch (face) {
                case 0: placeX--; break; // -X
                case 1: placeX++; break; // +X
                case 2: placeY--; break; // -Y
                case 3: placeY++; break; // +Y
                case 4: placeZ--; break; // -Z
                case 5: placeZ++; break; // +Z
            }

            System.out.println("Found block to place against. Placing at: " + placeX + ", " + placeY + ", " + placeZ);
            world.setBlock(placeX, placeY, placeZ, currentBlockType);
        } else {
            // If normal placement fails, try direct placement
            System.out.println("No block found with raycast. Testing direct placement...");
            directPlaceBlock();
        }
    }

    private void directPlaceBlock() {
        // Place a block right in front of the camera
        float distance = 3.0f;
        float[] dir = getCameraDirection();
        float placeX = camera.getX() + dir[0] * distance;
        float placeY = camera.getY() + dir[1] * distance;
        float placeZ = camera.getZ() + dir[2] * distance;

        int blockX = (int) Math.floor(placeX);
        int blockY = (int) Math.floor(placeY);
        int blockZ = (int) Math.floor(placeZ);

        System.out.println("Direct place - Placing at: " + blockX + ", " + blockY + ", " + blockZ);
        world.setBlock(blockX, blockY, blockZ, currentBlockType);
    }

    private float[] getCameraDirection() {
        // Get the camera angles
        float yaw = (float)Math.toRadians(camera.getYaw());
        float pitch = (float)Math.toRadians(camera.getPitch());

        // Calculate direction vector - completely inverted to fix the opposite direction issue
        float x = (float)Math.sin(yaw) * (float)Math.cos(pitch);
        float y = -(float)Math.sin(pitch);
        float z = (float)Math.cos(yaw) * (float)Math.cos(pitch);

        float length = (float)Math.sqrt(x*x + y*y + z*z);
        return new float[]{x/length, y/length, z/length};
    }

    private int[] raycast(float[] start, float[] dir, float maxDist) {
        // Debug the ray direction
        System.out.println("Raycasting with direction: " + dir[0] + ", " + dir[1] + ", " + dir[2]);

        for (float dist = 0; dist < maxDist; dist += 0.05f) {
            float x = start[0] + dir[0] * dist;
            float y = start[1] + dir[1] * dist;
            float z = start[2] + dir[2] * dist;

            int bx = (int)Math.floor(x);
            int by = (int)Math.floor(y);
            int bz = (int)Math.floor(z);

            Block block = world.getBlock(bx, by, bz);

            // Debug output every 0.5 units
            if (dist % 0.5 < 0.05) {
                System.out.println("Raycast at " + dist + ": Testing block at " + bx + "," + by + "," + bz +
                        " exists=" + (block != null) +
                        " type=" + (block != null ? block.getType() : "null"));
            }

            // Check if this is a solid block (not air)
            if (block != null && block.getType() != Block.AIR) {
                // Calculate face more reliably
                float backDist = 0.1f; // Increased back distance for better detection
                float prevX = x - dir[0] * backDist;
                float prevY = y - dir[1] * backDist;
                float prevZ = z - dir[2] * backDist;

                // Find the dominant axis to determine which face was hit
                float dx = Math.abs(x - prevX);
                float dy = Math.abs(y - prevY);
                float dz = Math.abs(z - prevZ);

                int face;
                if (dx >= dy && dx >= dz) {
                    // X axis is dominant - invert the condition
                    face = dir[0] < 0 ? 0 : 1; // -X or +X face (inverted)
                } else if (dy >= dx && dy >= dz) {
                    // Y axis is dominant - invert the condition
                    face = dir[1] < 0 ? 2 : 3; // -Y or +Y face (inverted)
                } else {
                    // Z axis is dominant - invert the condition
                    face = dir[2] < 0 ? 4 : 5; // -Z or +Z face (inverted)
                }

                System.out.println("Hit block at " + bx + "," + by + "," + bz + " on face " + face);
                return new int[]{bx, by, bz, face};
            }
        }

        return null;
    }

    private void loop() {
        glClearColor(0.5f, 0.8f, 1.0f, 0.0f);

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Only update camera if mouse is captured
            if (mouseCaptured) {
                camera.update(window);
            }

            glLoadIdentity();
            camera.apply();
            world.render();

            // Draw crosshair when mouse is captured
            if (mouseCaptured) {
                drawCrosshair();
            }

            // Draw instructions when mouse is not captured
            if (!mouseCaptured) {
                drawInstructions();
            }

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void drawCrosshair() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glColor3f(1, 1, 1);

        glBegin(GL_LINES);
        glVertex2f(width/2-10, height/2);
        glVertex2f(width/2+10, height/2);
        glEnd();

        glBegin(GL_LINES);
        glVertex2f(width/2, height/2-10);
        glVertex2f(width/2, height/2+10);
        glEnd();

        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void drawInstructions() {
        // Draw instructions when cursor is visible
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, height, 0, -1, 1);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_DEPTH_TEST);
        glColor3f(1, 1, 1);

        // Here we'd ideally render text, but without a proper text renderer
        // we'll just draw a box to indicate instructions would be here

        glBegin(GL_QUADS);
        glVertex2f(10, 10);
        glVertex2f(250, 10);
        glVertex2f(250, 100);
        glVertex2f(10, 100);
        glEnd();

        glEnable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
    }

    private void cleanup() {
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new MinecraftEngine().run();
    }
}