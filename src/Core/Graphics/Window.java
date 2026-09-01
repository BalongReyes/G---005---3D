package Core.Graphics;

import Core.Main;
import Settings.WindowSettings;
import SystemManager.Listeners.KeyListener;
import SystemManager.Listeners.MouseListener;

import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.Dimension;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Window {
    private final Main main;
    public Dimension size;
    private long windowHandle;
    
    private KeyListener keyListener;
    private MouseListener mouseListener;

    public Window(Main main, Dimension size, int onScreen) {
        this.main = main;
        this.size = size;
        this.keyListener = new KeyListener(main);
        this.mouseListener = new MouseListener(main);
        
        init();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);

        windowHandle = glfwCreateWindow(size.width, size.height, WindowSettings.title, NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            keyListener.invoke(key, action, mods);
        });

        glfwSetCursorPosCallback(windowHandle, (window, xpos, ypos) -> {
            mouseListener.invokeCursorPos(xpos, ypos);
        });
        
        glfwSetMouseButtonCallback(windowHandle, (window, button, action, mods) -> {
            mouseListener.invokeMouseButton(button, action, mods);
        });
        
        glfwSetScrollCallback(windowHandle, (window, xoffset, yoffset) -> {
            mouseListener.invokeScroll(yoffset);
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            glfwSetWindowPos(
                windowHandle,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }

        glfwMakeContextCurrent(windowHandle);
        glfwSwapInterval(1); // Enable v-sync
        glfwShowWindow(windowHandle);
        
        // This line is critical for LWJGL's interoperation with GLFW's OpenGL context
        GL.createCapabilities();
        
        glfwSetFramebufferSizeCallback(windowHandle, (window, width, height) -> {
            main.canvasSize.width = width;
            main.canvasSize.height = height;
            if (main.renderHandler != null) {
                main.renderHandler.resize(width, height);
            }
        });
        
        // Initial resize
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
            main.canvasSize.width = pWidth.get(0);
            main.canvasSize.height = pHeight.get(0);
        }
    }

    public long getWindowHandle() {
        return windowHandle;
    }
    
    public void cleanup() {
        Callbacks.glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    private static boolean keyLock = false;
    
    public static void setKeyLock(boolean keyLock){
        Window.keyLock = keyLock;
    }
    
    public static boolean getKeyLock(){
        return keyLock;
    }
}
