import static org.lwjgl.opengl.GL11.*;

public class Chunk {
    public static final int CHUNK_SIZE = 16; // 16x16x16 blocks per chunk

    private Block[][][] blocks;
    private int x, y, z; // Chunk position in world

    // Get block at local coordinates
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            return null;
        }
        return blocks[x][y][z];
    }

    // Set block at local coordinates
    public void setBlock(int x, int y, int z, int blockType) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            return;
        }
        blocks[x][y][z].setType(blockType);
    }

    public Chunk(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
        blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        // Initialize with some blocks for testing
        generateTestTerrain();
    }

    private void generateTestTerrain() {
        // Fill chunk with blocks
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    // Basic terrain: Stone base, dirt layer, grass on top
                    int blockType;
                    if (y < 4) {
                        blockType = Block.STONE;
                    } else if (y < 8) {
                        blockType = Block.DIRT;
                    } else if (y == 8) {
                        blockType = Block.GRASS;
                    } else {
                        blockType = Block.AIR;
                    }

                    // Randomly place some trees
                    if (y == 9 && blockType == Block.AIR && Math.random() < 0.02) {
                        blockType = Block.WOOD;
                    }

                    // Add leaves above trees
                    if (y > 9 && y < 13 && blocks[x][y-1][z] != null &&
                            blocks[x][y-1][z].getType() == Block.WOOD && Math.random() < 0.7) {
                        blockType = Block.LEAVES;
                    }

                    blocks[x][y][z] = new Block(blockType);
                }
            }
        }
    }

    public void render() {
        // Render all blocks in the chunk
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Block block = blocks[x][y][z];
                    if (block.isActive()) {
                        // Only render visible blocks (face culling)
                        if (isBlockVisible(x, y, z)) {
                            renderBlock(x, y, z, block);
                        }
                    }
                }
            }
        }
    }

    private boolean isBlockVisible(int x, int y, int z) {
        // Check if at least one face of the block is visible
        // A block is visible if it's on the edge of the chunk or adjacent to an air block

        // Check if block is on chunk boundary
        if (x == 0 || x == CHUNK_SIZE - 1 ||
                y == 0 || y == CHUNK_SIZE - 1 ||
                z == 0 || z == CHUNK_SIZE - 1) {
            return true;
        }

        // Check if any adjacent block is air
        return !blocks[x+1][y][z].isActive() ||
                !blocks[x-1][y][z].isActive() ||
                !blocks[x][y+1][z].isActive() ||
                !blocks[x][y-1][z].isActive() ||
                !blocks[x][y][z+1].isActive() ||
                !blocks[x][y][z-1].isActive();
    }

    // In the Chunk class, update the renderBlock method:
    private void renderBlock(int localX, int localY, int localZ, Block block) {
        // Calculate world position
        float worldX = this.x * CHUNK_SIZE + localX;
        float worldY = this.y * CHUNK_SIZE + localY;
        float worldZ = this.z * CHUNK_SIZE + localZ;

        // Render the block at its position
        block.render(worldX, worldY, worldZ);
    }
}