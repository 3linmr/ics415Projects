import static org.lwjgl.opengl.GL11.*;

public class Block {
    // Block types
    public static final int AIR = 0;
    public static final int GRASS = 1;
    public static final int DIRT = 2;
    public static final int STONE = 3;
    public static final int WATER = 4;
    public static final int WOOD = 5;
    public static final int LEAVES = 6;

    // Static model shared by all instances of the same block type
    private static OBJModel grassModel;

    // Block properties
    private int type;
    private boolean isActive;

    // Initialize models
    static {
        // Load grass block model (you'll need to provide the actual file paths)
        try {
            grassModel = new OBJModel("/Users/3lialnemer/Downloads/minecraft-grass-block/source/Minecraft_Grass_Block_OBJ/grass_block.obj", "/Users/3lialnemer/Downloads/minecraft-grass-block/source/Minecraft_Grass_Block_OBJ/grass_texture.png");
            System.out.println("Loaded grass block model");
        } catch (Exception e) {
            System.err.println("Failed to load grass block model: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Block(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public boolean isActive() {
        // A block is active if it's not AIR
        return type != AIR;
    }

    public void setType(int type) {
        this.type = type;
    }

    // Render the block at the specified position
    public void render(float x, float y, float z) {
        if (!isActive()) {
            return; // Don't render air blocks
        }

        // Save current matrix
        glPushMatrix();

        // Move to block position
        glTranslatef(x, y, z);

        // Render based on block type
        if (type == GRASS && grassModel != null) {
            // Use OBJ model for grass
            grassModel.render();
        } else {
            // Use colored cube for other blocks
            renderCube(getColor());
        }

        // Restore matrix
        glPopMatrix();
    }

    // Render a colored cube
    private void renderCube(float[] color) {
        glColor3f(color[0], color[1], color[2]);

        // Front face
        glBegin(GL_QUADS);
        glVertex3f(0.0f, 0.0f, 1.0f);
        glVertex3f(1.0f, 0.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(0.0f, 1.0f, 1.0f);
        glEnd();

        // Back face
        glBegin(GL_QUADS);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 1.0f, 0.0f);
        glVertex3f(1.0f, 1.0f, 0.0f);
        glVertex3f(1.0f, 0.0f, 0.0f);
        glEnd();

        // Top face
        glBegin(GL_QUADS);
        glVertex3f(0.0f, 1.0f, 0.0f);
        glVertex3f(0.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 1.0f, 0.0f);
        glEnd();

        // Bottom face
        glBegin(GL_QUADS);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(1.0f, 0.0f, 0.0f);
        glVertex3f(1.0f, 0.0f, 1.0f);
        glVertex3f(0.0f, 0.0f, 1.0f);
        glEnd();

        // Left face
        glBegin(GL_QUADS);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, 1.0f);
        glVertex3f(0.0f, 1.0f, 1.0f);
        glVertex3f(0.0f, 1.0f, 0.0f);
        glEnd();

        // Right face
        glBegin(GL_QUADS);
        glVertex3f(1.0f, 0.0f, 0.0f);
        glVertex3f(1.0f, 1.0f, 0.0f);
        glVertex3f(1.0f, 1.0f, 1.0f);
        glVertex3f(1.0f, 0.0f, 1.0f);
        glEnd();
    }

    // Get block color based on type
    public float[] getColor() {
        switch (type) {
            case GRASS:
                return new float[]{0.0f, 0.8f, 0.0f}; // Green
            case DIRT:
                return new float[]{0.6f, 0.4f, 0.2f}; // Brown
            case STONE:
                return new float[]{0.5f, 0.5f, 0.5f}; // Gray
            case WATER:
                return new float[]{0.0f, 0.0f, 0.9f}; // Blue
            case WOOD:
                return new float[]{0.5f, 0.3f, 0.1f}; // Brown
            case LEAVES:
                return new float[]{0.0f, 0.6f, 0.0f}; // Green
            default:
                return new float[]{1.0f, 1.0f, 1.0f}; // White for unknown blocks
        }
    }

    // Clean up resources
    public static void cleanup() {
        if (grassModel != null) {
            grassModel.cleanup();
        }
    }
}