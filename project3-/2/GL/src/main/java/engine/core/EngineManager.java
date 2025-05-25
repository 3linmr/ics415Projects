package engine.core;

import engine.Main;
import engine.core.utils.Consts;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.security.PublicKey;

public class EngineManager {

    public static final long NANOSECONDS_PER_SECOND = 1000000000L;

    public static final float FRAME_RATE = 1000;

    private static int fps;
    private static float frametime = 1.0f/FRAME_RATE;


    private boolean isRunning;

    private WindowManager window;
    private GLFWErrorCallback errorCallback;
    private iLogic gamelogic;

    private void init() throws Exception {
        GLFW.glfwSetErrorCallback(errorCallback = GLFWErrorCallback.createPrint(System.err));
        window = Main.getWindow();
        gamelogic = Main.getGame();
        window.init();
        gamelogic.init();
    }

    public void start() throws Exception {

        init();
        if (isRunning)
            return;
        run();
    }
public void run(){
this.isRunning = true;
int frames = 0;
long lastTime = System.nanoTime();
int frameCounter = 0;
double unprocessedTime = 0;

while (isRunning) {
    boolean render = false;
    long startTime = System.nanoTime();
    long passedTime = startTime - lastTime;
    lastTime = startTime;
    unprocessedTime += passedTime / (double) NANOSECONDS_PER_SECOND;
    frameCounter += passedTime;
    input();
    while (unprocessedTime >= frametime) {
        render = true;
        unprocessedTime -= frametime;

        if(window.windowShouldClose())
            stop();

        if (frameCounter >= NANOSECONDS_PER_SECOND) {
            setFps(frames);
            window.setTitle(Consts.TITLE + getFps());
            frames = 0;
            frameCounter = 0;


        }
    }
    if (render){
        update();
        render();
        frames++;

    }
}
cleanup();
}
private void stop(){
if(!isRunning)
    return;
isRunning = false;
}
private void input(){
        gamelogic.input();

}

private void render(){
        gamelogic.render();
window.update();
}

private void update(){
gamelogic.update();
}
private void cleanup(){
        window.cleanup();
        gamelogic.cleanup();
        errorCallback.free();
        GLFW.glfwTerminate();

}

    public static int getFps() {
        return fps;
    }
    public static void setFps(int fps) {
        EngineManager.fps = fps;
    }
}
