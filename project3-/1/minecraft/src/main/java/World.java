import java.util.HashMap;
import java.util.Map;

public class World {
    // Use a map to store chunks by position
    private Map<String, Chunk> chunks;

    // World dimensions in chunks
    private int worldSizeX = 2;
    private int worldSizeY = 1;
    private int worldSizeZ = 2;

    public World() {
        chunks = new HashMap<>();
        generateWorld();
    }

    private void generateWorld() {
        // Generate chunks for the world
        for (int x = 0; x < worldSizeX; x++) {
            for (int y = 0; y < worldSizeY; y++) {
                for (int z = 0; z < worldSizeZ; z++) {
                    Chunk chunk = new Chunk(x, y, z);
                    chunks.put(getChunkKey(x, y, z), chunk);
                }
            }
        }
    }

    private String getChunkKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    public Chunk getChunk(int x, int y, int z) {
        return chunks.get(getChunkKey(x, y, z));
    }

    // Check if a chunk exists at the given coordinates
    public boolean hasChunk(int x, int y, int z) {
        return chunks.containsKey(getChunkKey(x, y, z));
    }

    // Add a new chunk to the world
    public void addChunk(int x, int y, int z) {
        if (!hasChunk(x, y, z)) {
            Chunk chunk = new Chunk(x, y, z);
            chunks.put(getChunkKey(x, y, z), chunk);
        }
    }

    // Remove a chunk from the world
    public void removeChunk(int x, int y, int z) {
        chunks.remove(getChunkKey(x, y, z));
    }

    // Render the world
    public void render() {
        // Render all chunks
        for (Chunk chunk : chunks.values()) {
            chunk.render();
        }
    }

    // Get a block at world coordinates
    public Block getBlock(int x, int y, int z) {
        // Convert world coordinates to chunk coordinates
        int chunkX = Math.floorDiv(x, Chunk.CHUNK_SIZE);
        int chunkY = Math.floorDiv(y, Chunk.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(z, Chunk.CHUNK_SIZE);

        // Get local coordinates within the chunk
        int localX = Math.floorMod(x, Chunk.CHUNK_SIZE);
        int localY = Math.floorMod(y, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(z, Chunk.CHUNK_SIZE);

        // Get the chunk
        Chunk chunk = getChunk(chunkX, chunkY, chunkZ);
        if (chunk == null) {
            return null;
        }

        // Get the block
        return chunk.getBlock(localX, localY, localZ);
    }

    // Set a block at world coordinates
    public void setBlock(int x, int y, int z, int blockType) {
        // Convert world coordinates to chunk coordinates
        int chunkX = Math.floorDiv(x, Chunk.CHUNK_SIZE);
        int chunkY = Math.floorDiv(y, Chunk.CHUNK_SIZE);
        int chunkZ = Math.floorDiv(z, Chunk.CHUNK_SIZE);

        // Get local coordinates within the chunk
        int localX = Math.floorMod(x, Chunk.CHUNK_SIZE);
        int localY = Math.floorMod(y, Chunk.CHUNK_SIZE);
        int localZ = Math.floorMod(z, Chunk.CHUNK_SIZE);

        // Get the chunk
        Chunk chunk = getChunk(chunkX, chunkY, chunkZ);
        if (chunk == null) {
            // Create a new chunk if it doesn't exist
            addChunk(chunkX, chunkY, chunkZ);
            chunk = getChunk(chunkX, chunkY, chunkZ);
        }

        // Set the block
        chunk.setBlock(localX, localY, localZ, blockType);
    }
}