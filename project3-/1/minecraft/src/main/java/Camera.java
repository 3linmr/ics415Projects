import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Camera {
    // Camera position
    private float x;
    private float y;
    private float z;

    // Camera rotation
    private float pitch;
    private float yaw;
    private float roll;

    // Camera settings
    private float moveSpeed = 0.1f;
    private float sensitivity = 0.1f;

    // Mouse tracking
    private double lastX;
    private double lastY;
    private boolean firstMouse = true;

    public Camera(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = 0;
        this.yaw = 0;
        this.roll = 0;
    }

    public void update(long window) {
        // Handle mouse input for rotation
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        glfwGetCursorPos(window, mouseX, mouseY);

        if (firstMouse) {
            lastX = mouseX[0];
            lastY = mouseY[0];
            firstMouse = false;
        }

        double xOffset = mouseX[0] - lastX;
        double yOffset = lastY - mouseY[0]; // Reversed y-coordinate

        lastX = mouseX[0];
        lastY = mouseY[0];

        // Apply sensitivity and restore original direction
        xOffset *= -sensitivity; // Inverted to restore original behavior
        yOffset *= -sensitivity; // Inverted to restore original behavior

        // Update rotation angles
        yaw += xOffset;
        pitch += yOffset;

        // Constrain pitch to avoid flipping
        if (pitch > 89.0f) pitch = 89.0f;
        if (pitch < -89.0f) pitch = -89.0f;

        // Handle keyboard input for movement
        // W and S are still swapped as requested in the original
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            moveForward();
        }
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            moveBackward();
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            moveLeft();
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            moveRight();
        }
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            moveUp();
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            moveDown();
        }
    }

    public void apply() {
        // Apply the camera transformations to OpenGL
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        glRotatef(roll, 0.0f, 0.0f, 1.0f);
        glTranslatef(-x, -y, -z);
    }

    // Move forward (now mapped to S key) - Restoring original direction calculation
    private void moveForward() {
        x -= Math.sin(Math.toRadians(yaw)) * moveSpeed; // Restored original sign
        z += Math.cos(Math.toRadians(yaw)) * moveSpeed; // Restored original sign
    }

    // Move backward (now mapped to W key) - Restoring original direction calculation
    private void moveBackward() {
        x += Math.sin(Math.toRadians(yaw)) * moveSpeed; // Restored original sign
        z -= Math.cos(Math.toRadians(yaw)) * moveSpeed; // Restored original sign
    }

    // Restored original direction calculations
    private void moveLeft() {
        x -= Math.sin(Math.toRadians(yaw - 90)) * moveSpeed; // Restored original sign
        z += Math.cos(Math.toRadians(yaw - 90)) * moveSpeed; // Restored original sign
    }

    private void moveRight() {
        x -= Math.sin(Math.toRadians(yaw + 90)) * moveSpeed; // Restored original sign
        z += Math.cos(Math.toRadians(yaw + 90)) * moveSpeed; // Restored original sign
    }

    private void moveUp() {
        y += moveSpeed;
    }

    private void moveDown() {
        y -= moveSpeed;
    }

    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getPitch() { return pitch; }
    public float getYaw() { return yaw; }
    public float getRoll() { return roll; }
}