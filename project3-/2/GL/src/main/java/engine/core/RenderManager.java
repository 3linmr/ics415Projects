package engine.core;

public class RenderManager {
    private engine.core.ShaderProgram shader;
    private Mesh mesh;

    public void init() throws Exception {
        float[] vertices = new float[]{
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.0f,  0.5f, 0.0f
        };
        shader = new engine.core.ShaderProgram("shaders/vertex.glsl", "shaders/fragment.glsl");
        mesh = new Mesh(vertices);
    }

    public void render() {
        shader.bind();
        System.out.println("Rendering triangle...");
        mesh.render();
        shader.unbind();
    }

    public void cleanup() {
        shader.cleanup();
        mesh.cleanup();
    }
}
