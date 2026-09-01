
package Maths;

public class PerlinNoise{
    
    public static final int[] P = new int[512];
    
    public static final int[] PERMUTATION = { 151,160,137,91,90,15,
        131,13,201,95,96,53,194,233, 7,225,140,36,103,30,69,142,8,99,37,240,21,10,23,
        190, 6,148,247,120,234,75,0,26,197,62,94,252,219,203,117,35,11,32,57,177,33,
        88,237,149,56,87,174,20,125,136,171,168, 68,175,74,165,71,134,139,48,27,166,
        77,146,158,231,83,111,229,122,60,211,133,230,220,105,92,41,55,46,245,40,244,
        102,143,54, 65,25,63,161, 1,216,80,73,209,76,132,187,208, 89,18,169,200,196,
        135,130,116,188,159,86,164,100,109,198,173,186, 3,64,52,217,226,250,124,123,
        5,202,38,147,118,126,255,82,85,212,207,206,59,227,47,16,58,17,182,189,28,42,
        223,183,170,213,119,248,152, 2,44,154,163, 70,221,153,101,155,167, 43,172,9,
        129,22,39,253, 19,98,108,110,79,113,224,232,178,185, 112,104,218,246,97,228,
        251,34,242,193,238,210,144,12,191,179,162,241, 81,51,145,235,249,14,239,107,
        49,192,214, 31,181,199,106,157,184, 84,204,176,115,121,50,45,127, 4,150,254,
        138,236,205,93,222,114,67,29,24,72,243,141,128,195,78,66,215,61,156,180
    };

    public static double perlin2D(double x, double z) {
        int X = (int)Math.floor(x) & 255;
        int Z = (int)Math.floor(z) & 255;
        
        x -= Math.floor(x);
        z -= Math.floor(z);
        
        double u = fade(x);
        double v = fade(z);
        
        int A = P[X]+Z, B = P[X+1]+Z;
        
        return lerp(v, lerp(u, grad(P[A], x, z),   grad(P[B], x-1, z)),
                       lerp(u, grad(P[A+1], x, z-1), grad(P[B+1], x-1, z-1)));
    }

    public static double fade(double t) { 
        return t * t * t * (t * (t * 6 - 15) + 10); 
    }
    
    public static double lerp(double t, double a, double b) { 
        return a + t * (b - a); 
    }
    
    public static double grad(int hash, double x, double z) {
        int h = hash & 7;
        double u = h < 4 ? x : z;
        double v = h < 4 ? z : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? 2.0 * v : -2.0 * v);
    }
    
    public static double getFinalTemp(double worldX, double worldY, double worldZ, double tempScale, double startY, double maxExpectedHeight){
        int octaves = Settings.WorldSettings.NOISE_OCTAVES;
        double persistence = 0.5;
        double lacunarity = 2.0;
        
        double totalNoise = 0;
        double amplitude = 1.0;
        double frequency = 1.0;
        double maxAmplitude = 0;
        
        for (int i = 0; i < octaves; i++) {
            double sampleX = worldX * tempScale * frequency;
            double sampleZ = worldZ * tempScale * frequency;
            
            // Generate basic perlin noise and map it from [-1, 1] to [-1, 1] approximately
            double noiseVal = perlin2D(sampleX, sampleZ);
            
            totalNoise += noiseVal * amplitude;
            maxAmplitude += amplitude;
            
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        
        // Normalize the result back to approximately [-1, 1]
        double normalizedNoise = totalNoise / maxAmplitude;
        
        double baseTemp = normalizedNoise * 20.0;
        double heightRatio = (worldY - startY) / maxExpectedHeight;
        
        // Shape the terrain: Higher points should be strictly hotter (positive value, empty space).
        // By adding a strong penalty as heightRatio reaches 1.0, we force the noise to taper off
        // into empty air, preventing it from flattening against the skybox ceiling.
        double finalTemp = baseTemp - 20.0 + (heightRatio * 60.0);
        
        return finalTemp;
    }
    
    public static void setSeed(long seed) {
        java.util.Random rand = new java.util.Random(seed);
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            int j = rand.nextInt(256);
            int swap = p[i];
            p[i] = p[j];
            p[j] = swap;
        }
        for (int i = 0; i < 256; i++) {
            P[256 + i] = P[i] = p[i];
        }
    }

    static {
        setSeed(Settings.WorldSettings.SEED);
    }

}
