package engine.core;

public interface iLogic {

    void init() throws Exception;

    void input();
        void update();
        void render();
        void cleanup();
}
