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

public class RayCaster {
    private Camera camera;
    private World world;

    // Maximum distance a player can interact with blocks
    private static final float MAX_REACH = 5.0f;
    // Step size for ray casting
    private static final float RAY_STEP = 0.05f;

    // Last hit block coordinates and face
    private int lastHitX = -1;
    private int lastHitY = -1;
    private int lastHitZ = -1;
    private int lastHitFace = -1; // 0: -X, 1: +X, 2: -Y, 3: +Y, 4: -Z, 5: +Z

    public RayCaster(Camera camera, World world) {
        this.camera = camera;
        this.world = world;
    }

    /**
     * Cast a ray from the camera position in the camera's looking direction
     * to find the first block hit
     *
     * @return true if a block was hit, false otherwise
     */
    public boolean castRay() {
        // Start position (camera position)
        float x = camera.getX();
        float y = camera.getY();
        float z = camera.getZ();

        // Get looking direction from camera
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());

        // Fix: inverted direction calculation to match the expected behavior
        float dx = (float) Math.sin(yaw) * (float) Math.cos(pitch);
        float dy = -(float) Math.sin(pitch);
        float dz = (float) Math.cos(yaw) * (float) Math.cos(pitch);

        // Normalize direction vector
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        dx /= length;
        dy /= length;
        dz /= length;

        // Check each step along the ray
        for (float step = 0; step <= MAX_REACH; step += RAY_STEP) {
            // Get current position along the ray
            float rayX = x + dx * step;
            float rayY = y + dy * step;
            float rayZ = z + dz * step;

            // Convert to block coordinates (floor)
            int blockX = (int) Math.floor(rayX);
            int blockY = (int) Math.floor(rayY);
            int blockZ = (int) Math.floor(rayZ);

            // Get the block at this position
            Block block = world.getBlock(blockX, blockY, blockZ);

            // If we hit a solid block
            if (block != null && block.isActive() && block.getType() != Block.AIR) {
                // Save hit position and face
                lastHitX = blockX;
                lastHitY = blockY;
                lastHitZ = blockZ;

                // Determine which face was hit
                // We work backwards from the hit point by a tiny amount
                float backStep = 0.01f;
                float prevX = rayX - dx * backStep;
                float prevY = rayY - dy * backStep;
                float prevZ = rayZ - dz * backStep;

                // Find the most significant component of the direction
                float absDx = Math.abs(rayX - prevX);
                float absDy = Math.abs(rayY - prevY);
                float absDz = Math.abs(rayZ - prevZ);

                if (absDx >= absDy && absDx >= absDz) {
                    lastHitFace = rayX - prevX > 0 ? 0 : 1; // -X or +X
                } else if (absDy >= absDx && absDy >= absDz) {
                    lastHitFace = rayY - prevY > 0 ? 2 : 3; // -Y or +Y
                } else {
                    lastHitFace = rayZ - prevZ > 0 ? 4 : 5; // -Z or +Z
                }

                return true;
            }
        }

        // If we didn't hit anything, reset last hit
        lastHitX = -1;
        lastHitY = -1;
        lastHitZ = -1;
        lastHitFace = -1;

        return false;
    }

    /**
     * Get the block position that was hit by the ray cast
     *
     * @return int array with block [x, y, z] coordinates, or null if no hit
     */
    public int[] getHitPosition() {
        if (lastHitX == -1) {
            return null;
        }
        return new int[] { lastHitX, lastHitY, lastHitZ };
    }

    /**
     * Get the position where a new block would be placed
     * based on the hit position and face
     *
     * @return int array with new block [x, y, z] coordinates, or null if no hit
     */
    public int[] getPlacePosition() {
        if (lastHitX == -1) {
            return null;
        }

        int placeX = lastHitX;
        int placeY = lastHitY;
        int placeZ = lastHitZ;

        // Adjust position based on hit face
        switch (lastHitFace) {
            case 0: placeX--; break; // -X
            case 1: placeX++; break; // +X
            case 2: placeY--; break; // -Y
            case 3: placeY++; break; // +Y
            case 4: placeZ--; break; // -Z
            case 5: placeZ++; break; // +Z
        }

        return new int[] { placeX, placeY, placeZ };
    }

    /**
     * Render a wireframe box around the currently selected block
     */
    public void renderHighlight() {
        // If no block is selected, return
        if (lastHitX == -1) {
            return;
        }

        // Save current attributes
        glPushAttrib(GL_CURRENT_BIT | GL_ENABLE_BIT | GL_LINE_BIT);

        // Setup for wireframe rendering
        glDisable(GL_TEXTURE_2D);
        glDisable(GL_LIGHTING);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Set line properties
        glLineWidth(2.0f);
        glColor4f(1.0f, 1.0f, 1.0f, 0.8f);

        // Draw wireframe cube
        float offset = 0.001f; // Slight offset to avoid z-fighting
        glBegin(GL_LINES);

        // Bottom face
        glVertex3f(lastHitX - offset, lastHitY - offset, lastHitZ - offset);
        glVertex3f(lastHitX + 1 + offset, lastHitY - offset, lastHitZ - offset);

        glVertex3f(lastHitX + 1 + offset, lastHitY - offset, lastHitZ - offset);
        glVertex3f(lastHitX + 1 + offset, lastHitY - offset, lastHitZ + 1 + offset);

        glVertex3f(lastHitX + 1 + offset, lastHitY - offset, lastHitZ + 1 + offset);
        glVertex3f(lastHitX - offset, lastHitY - offset, lastHitZ + 1 + offset);

        glVertex3f(lastHitX - offset, lastHitY - offset, lastHitZ + 1 + offset);
        glVertex3f(lastHitX - offset, lastHitY - offset, lastHitZ - offset);

        // Top face
        glVertex3f(lastHitX - offset, lastHitY + 1 + offset, lastHitZ - offset);
        glVertex3f(lastHitX + 1 + offset, lastHitY + 1 + offset, lastHitZ - offset);

        glVertex3f(lastHitX + 1 + offset, lastHitY + 1 + offset, lastHitZ - offset);
        glVertex3f(lastHitX + 1 + offset, lastHitY + 1 + offset, lastHitZ + 1 + offset);

        glVertex3f(lastHitX + 1 + offset, lastHitY + 1 + offset, lastHitZ + 1 + offset);
        glVertex3f(lastHitX - offset, lastHitY + 1 + offset, lastHitZ + 1 + offset);

        glVertex3f(lastHitX - offset, lastHitY + 1 + offset, lastHitZ + 1 + offset);
        glVertex3f(lastHitX - offset, lastHitY + 1 + offset, lastHitZ - offset);

        // Connecting edges
        glVertex3f(lastHitX - offset, lastHitY - offset, lastHitZ - offset);
        glVertex3f(lastHitX - offset, lastHitY + 1 + offset, lastHitZ - offset);

        glVertex3f(lastHitX + 1 + offset, lastHitY - offset, lastHitZ - offset);
        glVertex3f(lastHitX + 1 + offset, lastHitY + 1 + offset, lastHitZ - offset);

        glVertex3f(lastHitX + 1 + offset, lastHitY - offset, lastHitZ + 1 + offset);
        glVertex3f(lastHitX + 1 + offset, lastHitY + 1 + offset, lastHitZ + 1 + offset);

        glVertex3f(lastHitX - offset, lastHitY - offset, lastHitZ + 1 + offset);
        glVertex3f(lastHitX - offset, lastHitY + 1 + offset, lastHitZ + 1 + offset);

        glEnd();

        // Restore previous attributes
        glPopAttrib();
    }
}