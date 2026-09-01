package Core;

import Core.Graphics.Window;
import Core.Graphics.ControlPanel;
import java.awt.Dimension;
import SystemManager.Handlers.ObjectHandler;
import SystemManager.Handlers.GLRenderHandler;
import SystemManager.Handlers.TickHandler;
import Data.Camera;
import org.lwjgl.glfw.GLFW;

public class Main {

    public Window window;
    public ControlPanel controlPanel;
    public Dimension canvasSize = new Dimension(800, 600);

    public TickHandler tickHandler;
    public GLRenderHandler renderHandler;
    public ObjectHandler objectHandler;
    public Camera camera;

    public static void main(String[] args) {
        Main main = new Main();
        main.init();
    }

    private void init() {
        setDefaults();
        setWindow();
        startLoop();
    }

    public Core.World.ScalarField scalarField;

    private void setDefaults() {
        Console.out("Setting Up Defaults", "\u001b[0;32m");
        tickHandler = new TickHandler();
        renderHandler = new GLRenderHandler();
        objectHandler = new ObjectHandler();
        camera = new Camera();

        scalarField = Core.World.ScalarField.generate(
                Settings.WorldSettings.GRID_SIZE_X,
                Settings.WorldSettings.GRID_SIZE_Y,
                Settings.WorldSettings.GRID_SIZE_Z,
                Settings.WorldSettings.SPACING);
        Console.out("Generated Scalar Field with " + scalarField.temperature.length + " vertices.", "\u001b[0;36m");

        updateTerrainMesh(0);
    }

    public void regenerateTerrain() {
        Maths.PerlinNoise.setSeed(Settings.WorldSettings.SEED);
        scalarField = Core.World.ScalarField.generate(
                Settings.WorldSettings.GRID_SIZE_X,
                Settings.WorldSettings.GRID_SIZE_Y,
                Settings.WorldSettings.GRID_SIZE_Z,
                Settings.WorldSettings.SPACING);
        updateTerrainMesh(isoLevel);
    }

    public double isoLevel = 0;

    public Data.Object3D waterObject;
    public Data.Object3D wfcPlaceholders;
    public Core.WFC.WFCGenerator wfcGenerator = new Core.WFC.WFCGenerator();

    public java.util.List<int[]> surfaceGridCells = new java.util.ArrayList<>();

    public java.util.List<Data.Object3D> terrainChunks = new java.util.ArrayList<>();

    public void updateTerrainMesh(double isoLevel) {
        this.isoLevel = isoLevel;
        if (scalarField == null || objectHandler == null)
            return;

        for (Data.Object3D chunk : terrainChunks) {
            objectHandler.getObjects().remove(chunk);
        }
        terrainChunks.clear();

        if (waterObject != null) {
            objectHandler.getObjects().remove(waterObject);
        }
        if (wfcPlaceholders != null) {
            objectHandler.getObjects().remove(wfcPlaceholders);
        }

        terrainChunks = Core.World.TerrainGenerator.generateMesh(scalarField, isoLevel);
        for (Data.Object3D chunk : terrainChunks) {
            objectHandler.addObject(chunk);
        }

        waterObject = Core.World.WaterGenerator.generateWater();
        objectHandler.addObject(waterObject);

        if (wfcPlaceholders != null) {
            objectHandler.addObject(wfcPlaceholders);
        }

        surfaceGridCells = Core.World.TerrainGenerator.getSurfaceCells(scalarField, isoLevel);
        Core.Console.out("Generated " + surfaceGridCells.size() + " surface grid cells.", "\u001b[0;36m");
    }

    private void setWindow() {
        Console.out("Setting Up Window", "\u001b[0;32m");
        window = new Window(this, canvasSize, 1);
        renderHandler.init(canvasSize.width, canvasSize.height);
        controlPanel = new ControlPanel(this);
    }

    public boolean running = false;

    private void startLoop() {
        Console.out("\nStarting GLFW Loop", "\u001b[0;32m");
        running = true;
        
        long windowHandle = window.getWindowHandle();
        
        double lastTime = GLFW.glfwGetTime();
        double timer = lastTime;
        int frames = 0;
        
        while (running && !GLFW.glfwWindowShouldClose(windowHandle)) {
            double currentTime = GLFW.glfwGetTime();
            lastTime = currentTime;
            
            tickHandler.tick(this);
            renderHandler.render(this);
            
            GLFW.glfwSwapBuffers(windowHandle);
            GLFW.glfwPollEvents();
            
            frames++;
            if (GLFW.glfwGetTime() - timer > 1.0) {
                timer += 1.0;
                String camInfo = camera != null ? String.format(" | Pos: (%.1f, %.1f, %.1f) | Rot: (%.2f, %.2f, %.2f)", 
                    camera.posX, camera.posY, camera.posZ, camera.rotX, camera.rotY, camera.rotZ) : "";
                GLFW.glfwSetWindowTitle(windowHandle, Settings.WindowSettings.title + " | FPS: " + frames + camInfo);
                final int currentFrames = frames;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    if (controlPanel != null && controlPanel.infoLabel != null) {
                        controlPanel.infoLabel.setText("FPS: " + currentFrames + camInfo);
                    }
                });
                frames = 0;
            }
        }
        
        stop();
    }

    public void stop() {
        Console.gap();
        Console.out("Closing App", "\u001b[0;31m");
        running = false;
        renderHandler.shutdown();
        window.cleanup();
        
        if (controlPanel != null) {
            controlPanel.dispose();
        }
    }
}
