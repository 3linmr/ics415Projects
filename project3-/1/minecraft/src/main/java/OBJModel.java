import java.io.*;
import java.util.*;
import java.nio.*;
import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class OBJModel {
    private List<Vector3f> vertices = new ArrayList<>();
    private List<Vector2f> textures = new ArrayList<>();
    private List<Vector3f> normals = new ArrayList<>();
    private List<Face> faces = new ArrayList<>();

    private int textureID;
    private int vaoID;
    private int vertexCount;

    // Load and store model
    public OBJModel(String objFile, String textureFile) {
        loadOBJ(objFile);
        loadTexture(textureFile);
        setupVAO();
    }

    private void loadOBJ(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;

                String prefix = parts[0];

                if (prefix.equals("v")) {
                    // Vertex position
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    vertices.add(new Vector3f(x, y, z));
                } else if (prefix.equals("vt")) {
                    // Texture coordinate
                    float u = Float.parseFloat(parts[1]);
                    float v = Float.parseFloat(parts[2]);
                    textures.add(new Vector2f(u, v));
                } else if (prefix.equals("vn")) {
                    // Normal
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    float z = Float.parseFloat(parts[3]);
                    normals.add(new Vector3f(x, y, z));
                } else if (prefix.equals("f")) {
                    // Face
                    // Format: f v1/vt1/vn1 v2/vt2/vn2 v3/vt3/vn3 ...
                    TexturedVertex[] faceVertices = new TexturedVertex[parts.length - 1];

                    for (int i = 1; i < parts.length; i++) {
                        String[] indices = parts[i].split("/");

                        int vertexIndex = Integer.parseInt(indices[0]) - 1;
                        int textureIndex = Integer.parseInt(indices[1]) - 1;
                        int normalIndex = Integer.parseInt(indices[2]) - 1;

                        faceVertices[i - 1] = new TexturedVertex(vertexIndex, textureIndex, normalIndex);
                    }

                    // Triangulate faces
                    for (int i = 0; i < faceVertices.length - 2; i++) {
                        faces.add(new Face(faceVertices[0], faceVertices[i + 1], faceVertices[i + 2]));
                    }
                }
            }

            System.out.println("Loaded OBJ model: " + filePath);
            System.out.println("Vertices: " + vertices.size());
            System.out.println("Texture coords: " + textures.size());
            System.out.println("Normals: " + normals.size());
            System.out.println("Faces: " + faces.size());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTexture(String filePath) {
        // This is a simplified version without actual texture loading
        // In a real implementation, you'd use a texture loading library
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Sample data (green grass-like texture)
        int width = 64;
        int height = 64;
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 34 + (int)(Math.random() * 50);
                int g = 139 + (int)(Math.random() * 50);
                int b = 34 + (int)(Math.random() * 50);
                int a = 255;

                buffer.put((byte) r)
                        .put((byte) g)
                        .put((byte) b)
                        .put((byte) a);
            }
        }

        buffer.flip();

        // Upload texture
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        // Generate mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);

        System.out.println("Generated sample grass texture");
    }

    private void setupVAO() {
        // Convert model data to arrays for rendering
        float[] verticesArray = new float[faces.size() * 9]; // 3 vertices per face, 3 coords per vertex
        float[] texCoordsArray = new float[faces.size() * 6]; // 3 vertices per face, 2 coords per texture
        float[] normalsArray = new float[faces.size() * 9]; // 3 vertices per face, 3 coords per normal

        int vertexPointer = 0;
        int texturePointer = 0;
        int normalPointer = 0;

        for (Face face : faces) {
            TexturedVertex v1 = face.v1;
            TexturedVertex v2 = face.v2;
            TexturedVertex v3 = face.v3;

            // Process vertex 1
            Vector3f position1 = vertices.get(v1.vertexIndex);
            verticesArray[vertexPointer++] = position1.x;
            verticesArray[vertexPointer++] = position1.y;
            verticesArray[vertexPointer++] = position1.z;

            Vector2f texture1 = textures.get(v1.textureIndex);
            texCoordsArray[texturePointer++] = texture1.x;
            texCoordsArray[texturePointer++] = texture1.y;

            Vector3f normal1 = normals.get(v1.normalIndex);
            normalsArray[normalPointer++] = normal1.x;
            normalsArray[normalPointer++] = normal1.y;
            normalsArray[normalPointer++] = normal1.z;

            // Process vertex 2
            Vector3f position2 = vertices.get(v2.vertexIndex);
            verticesArray[vertexPointer++] = position2.x;
            verticesArray[vertexPointer++] = position2.y;
            verticesArray[vertexPointer++] = position2.z;

            Vector2f texture2 = textures.get(v2.textureIndex);
            texCoordsArray[texturePointer++] = texture2.x;
            texCoordsArray[texturePointer++] = texture2.y;

            Vector3f normal2 = normals.get(v2.normalIndex);
            normalsArray[normalPointer++] = normal2.x;
            normalsArray[normalPointer++] = normal2.y;
            normalsArray[normalPointer++] = normal2.z;

            // Process vertex 3
            Vector3f position3 = vertices.get(v3.vertexIndex);
            verticesArray[vertexPointer++] = position3.x;
            verticesArray[vertexPointer++] = position3.y;
            verticesArray[vertexPointer++] = position3.z;

            Vector2f texture3 = textures.get(v3.textureIndex);
            texCoordsArray[texturePointer++] = texture3.x;
            texCoordsArray[texturePointer++] = texture3.y;

            Vector3f normal3 = normals.get(v3.normalIndex);
            normalsArray[normalPointer++] = normal3.x;
            normalsArray[normalPointer++] = normal3.y;
            normalsArray[normalPointer++] = normal3.z;
        }

        vertexCount = faces.size() * 3;

        // Create VAO and VBOs
        // Note: This is a simplified version using OpenGL 1.1 compatible code
        // In a real implementation, you'd use modern OpenGL with shaders
        vaoID = glGenLists(1);
        glNewList(vaoID, GL_COMPILE);

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, textureID);

        glBegin(GL_TRIANGLES);

        for (int i = 0; i < vertexCount; i++) {
            // Set texture coordinate
            int texIndex = i * 2;
            glTexCoord2f(texCoordsArray[texIndex], texCoordsArray[texIndex + 1]);

            // Set normal
            int normIndex = i * 3;
            glNormal3f(normalsArray[normIndex], normalsArray[normIndex + 1], normalsArray[normIndex + 2]);

            // Set vertex
            int vertIndex = i * 3;
            glVertex3f(verticesArray[vertIndex], verticesArray[vertIndex + 1], verticesArray[vertIndex + 2]);
        }

        glEnd();

        glDisable(GL_TEXTURE_2D);

        glEndList();

        System.out.println("Created display list: " + vaoID);
    }

    public void render() {
        glCallList(vaoID);
    }

    public void cleanup() {
        glDeleteLists(vaoID, 1);
        glDeleteTextures(textureID);
    }

    // Helper classes
    private static class Vector3f {
        public float x, y, z;

        public Vector3f(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    private static class Vector2f {
        public float x, y;

        public Vector2f(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private static class TexturedVertex {
        public int vertexIndex;
        public int textureIndex;
        public int normalIndex;

        public TexturedVertex(int vertexIndex, int textureIndex, int normalIndex) {
            this.vertexIndex = vertexIndex;
            this.textureIndex = textureIndex;
            this.normalIndex = normalIndex;
        }
    }

    private static class Face {
        public TexturedVertex v1, v2, v3;

        public Face(TexturedVertex v1, TexturedVertex v2, TexturedVertex v3) {
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }
    }
}