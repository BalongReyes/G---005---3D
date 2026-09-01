package Core.World;

import Data.Vertex;
import Maths.PerlinNoise;

public class ScalarField {

    public double[] temperature; // -30.0 to 30.0
    public int sizeX, sizeY, sizeZ;

    public ScalarField(double[] temperature, int sizeX, int sizeY, int sizeZ) {
        this.temperature = temperature;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static ScalarField generate(int sizeX, int sizeY, int sizeZ, double spacing) {
        int total = sizeX * sizeY * sizeZ;
        double[] temps = new double[total];

        double tempScale = Settings.WorldSettings.NOISE_SCALE;
        double maxExpectedHeight = sizeY * spacing;

        // 1. Generate grid points and completely random noise
        int index = 0;
        double startX = -((sizeX - 1) * spacing) / 2.0;
        double startY = -((sizeY - 1) * spacing) / 2.0;
        double startZ = -((sizeZ - 1) * spacing) / 2.0;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int z = 0; z < sizeZ; z++) {
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    double worldX = startX + x * spacing;
                    double worldY = startY + y * spacing;
                    double worldZ = startZ + z * spacing;

                    double finalTemp = PerlinNoise.getFinalTemp(worldX, worldY, worldZ, tempScale, startY,
                            maxExpectedHeight);
                    temps[index] = finalTemp;
                    if (finalTemp < min)
                        min = finalTemp;
                    if (finalTemp > max)
                        max = finalTemp;
                    index++;
                }
            }
        }

        // 2. Normalize the values back to exactly stretch from -30.0 to 30.0
        if (max > min) {
            for (int i = 0; i < total; i++) {
                temps[i] = ((temps[i] - min) / (max - min)) * 60.0 - 30.0;
            }
        }

        // 3. Boundary Capping: Force the outer border of the grid to be empty.
        // This generates a solid vertical wall at the exact edge of the world.
        for (int z = 0; z < sizeZ; z++) {
            for (int y = 0; y < sizeY; y++) {
                for (int x = 0; x < sizeX; x++) {
                    int idx = x + y * sizeX + z * sizeX * sizeY;
                    if (y == 0) {
                        // Bottom base is ALWAYS solid, creating a flat sandbox floor
                        temps[idx] = -100.0;
                    } else if (x == 0 || x == sizeX - 1 || y == sizeY - 1 || z == 0 || z == sizeZ - 1) {
                        // Absolute outer walls and top are ALWAYS empty, sealing the mesh with a sheer wall
                        temps[idx] = 100.0;
                    }
                }
            }
        }

        return new ScalarField(temps, sizeX, sizeY, sizeZ);
    }
}