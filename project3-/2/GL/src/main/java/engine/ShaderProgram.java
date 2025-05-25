// ShaderProgram.java
package engine.core;

import org.lwjgl.opengl.GL20;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ShaderProgram {
    private final int programId;

    public ShaderProgram(String vertexPath, String fragmentPath) throws Exception {
        int vertexId = loadShader(vertexPath, GL20.GL_VERTEX_SHADER);
        int fragmentId = loadShader(fragmentPath, GL20.GL_FRAGMENT_SHADER);

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexId);
        GL20.glAttachShader(programId, fragmentId);
        GL20.glLinkProgram(programId);

        if (GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS) == 0) {
            throw new RuntimeException("Shader linking failed: " + GL20.glGetProgramInfoLog(programId));
        }

        GL20.glDeleteShader(vertexId);
        GL20.glDeleteShader(fragmentId);
    }

    private int loadShader(String filePath, int type) throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(filePath)));
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        if (GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS) == 0) {
            throw new RuntimeException("Shader compile error in " + filePath + ": " + GL20.glGetShaderInfoLog(shaderId));
        }

        return shaderId;
    }

    public void bind() {
        GL20.glUseProgram(programId);
    }

    public void unbind() {
        GL20.glUseProgram(0);
    }

    public void cleanup() {
        GL20.glDeleteProgram(programId);
    }
}

// Mesh.java

