package SystemManager.Listeners;

import Core.Main;
import org.lwjgl.glfw.GLFW;

public class KeyListener {

    private final Main main;

    public KeyListener(Main main) {
        this.main = main;
    }

    public static boolean shift = false;
    public static boolean w = false;
    public static boolean s = false;
    public static boolean a = false;
    public static boolean d = false;
    public static boolean r = false;
    public static boolean f = false;
    public static boolean q = false;
    public static boolean e = false;

    public void invoke(int key, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            switch (key) {
                case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> shift = true;
                case GLFW.GLFW_KEY_W -> w = true;
                case GLFW.GLFW_KEY_S -> s = true;
                case GLFW.GLFW_KEY_A -> a = true;
                case GLFW.GLFW_KEY_D -> d = true;
                case GLFW.GLFW_KEY_R -> r = true;
                case GLFW.GLFW_KEY_F -> f = true;
                case GLFW.GLFW_KEY_Q -> q = true;
                case GLFW.GLFW_KEY_E -> e = true;
                case GLFW.GLFW_KEY_F1 -> SystemManager.Handlers.GLRenderHandler.showChunkBounds = !SystemManager.Handlers.GLRenderHandler.showChunkBounds;
                case GLFW.GLFW_KEY_F2 -> SystemManager.Handlers.GLRenderHandler.showScalarGridBounds = !SystemManager.Handlers.GLRenderHandler.showScalarGridBounds;
                case GLFW.GLFW_KEY_F6 -> main.wfcGenerator.init(main, main.scalarField);
                case GLFW.GLFW_KEY_F12 -> {
                    String str = String.format(java.util.Locale.US, "Pos: %.1f, %.1f, %.1f | Rot: %.2f, %.2f, %.2f",
                            main.camera.posX, main.camera.posY, main.camera.posZ,
                            main.camera.rotX, main.camera.rotY, main.camera.rotZ);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                            .setContents(new java.awt.datatransfer.StringSelection(str), null);
                    System.out.println("Copied to clipboard: " + str);
                }
            }
        } else if (action == GLFW.GLFW_RELEASE) {
            switch (key) {
                case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> shift = false;
                case GLFW.GLFW_KEY_W -> w = false;
                case GLFW.GLFW_KEY_S -> s = false;
                case GLFW.GLFW_KEY_A -> a = false;
                case GLFW.GLFW_KEY_D -> d = false;
                case GLFW.GLFW_KEY_R -> r = false;
                case GLFW.GLFW_KEY_F -> f = false;
                case GLFW.GLFW_KEY_Q -> q = false;
                case GLFW.GLFW_KEY_E -> e = false;
            }
        }
    }
}
