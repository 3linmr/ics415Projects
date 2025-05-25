package engine;

import engine.core.EngineManager;
import engine.core.WindowManager;
import engine.core.test.TestGame;
import engine.core.utils.Consts;

public class Main {
    private static WindowManager window;
    private static EngineManager engine;
    private static TestGame game;
    public static void main(String[] args) {
        window = new WindowManager(Consts.TITLE, 1280, 720, true);
        game = new TestGame();
        engine = new EngineManager();
        try{
            engine.start();
        } catch (Exception e){
            e.printStackTrace();
        }
        window.init();

        while (!window.windowShouldClose()) {
            window.setClearColor(0.1f, 0.1f, 0.2f, 1.0f);
            window.update();
        }

        window.cleanup();
    }

    public static WindowManager getWindow() {
        return window;
    }
    public static TestGame getGame() {
        return game;
    }
}
