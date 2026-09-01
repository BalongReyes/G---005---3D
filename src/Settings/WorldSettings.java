package Settings;

public class WorldSettings {
    // Terrain Grid Dimensions
    public static final int GRID_SIZE_X = 200;
    public static final int GRID_SIZE_Y = 50;
    public static final int GRID_SIZE_Z = 200;

    // Spacing between each voxel point in the 3D world
    public static final double SPACING = 5.0;

    // Chunk size for Marching Cubes mesh generation
    public static final int CHUNK_SIZE = 20;

    // Perlin noise scaling factor
    public static double NOISE_SCALE = 0.005;

    // Number of octaves for fractal noise (smoothness)
    public static int NOISE_OCTAVES = 4;

    // The Y-level where the water plane is rendered
    public static double WATER_LEVEL = -20.0;

    // World Seed
    public static long SEED = 12345L;

    // Camera Speed
    public static double CAMERA_SPEED = 4.0;

    // Water Wave Intensity
    public static double WAVE_INTENSITY = 0.2;
}
