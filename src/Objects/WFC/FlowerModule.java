package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;
import java.util.Random;

// Sparse colorful accents for grass fields - a thin stem topped by a small
// billboard cross "bloom". Same GRASS-category sockets as GroundModule, and
// a low weight so it reads as occasional color in a field rather than
// dominating it (GrassModule's weight is 5.0, GroundModule's is 20.0 -
// sitting Flower well under Grass keeps it an accent).
public class FlowerModule extends WFCModule {

    private static final int FLOWERS_PER_CELL = 3;
    private static final double VISUAL_SCALE = 0.35;

    // Small palette of bloom colors; one is picked per flower so a field
    // reads as mixed wildflowers rather than one repeated species.
    private static final Color[] BLOOM_COLORS = {
            new Color(230, 210, 60), // yellow
            new Color(230, 90, 120), // pink
            new Color(210, 80, 200), // violet
            new Color(240, 240, 240), // white
            new Color(230, 140, 40) // orange
    };

    public FlowerModule() {
        super(SocketType.GRASS, "Flower",
                new SocketType[] {
                        SocketType.WILDCARD,
                        SocketType.WILDCARD,
                        SocketType.AIR,
                        SocketType.SOLID,
                        SocketType.WILDCARD,
                        SocketType.WILDCARD
                },
                new SocketType[][] {
                        { SocketType.WILDCARD },
                        { SocketType.WILDCARD },
                        { SocketType.AIR },
                        { SocketType.SOLID },
                        { SocketType.WILDCARD },
                        { SocketType.WILDCARD }
                },
                new Color(80, 160, 60), 2.0);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {

        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        for (int i = 0; i < FLOWERS_PER_CELL; i++) {
            double fx = cx + (rng.nextDouble() * 2 - 1) * size * 0.35;
            double fz = cz + (rng.nextDouble() * 2 - 1) * size * 0.35;
            double exactY = Core.World.TerrainGenerator.getExactHeightAt(fx, fz, cy, grid, isoLevel);
            addFlower(fx, exactY, fz, size, rng, verts, faces);
        }
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {

        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        for (int i = 0; i < FLOWERS_PER_CELL; i++) {
            double fx = cx + (rng.nextDouble() * 2 - 1) * size * 0.35;
            double fz = cz + (rng.nextDouble() * 2 - 1) * size * 0.35;
            addFlower(fx, cy, fz, size, rng, verts, faces);
        }
    }

    private void addFlower(double cx, double cy, double cz, double size, Random rng,
            List<Vertex> verts, List<Data.Face> faces) {

        double stemHeight = size * VISUAL_SCALE * (0.6 + rng.nextDouble() * 0.4);
        double stemWidth = size * 0.015;
        Color stemColor = new Color(60, 130, 50);

        // Stem: a single thin quad from ground to bloom height.
        int idx = verts.size();
        Vertex s1 = new Vertex(cx - stemWidth, cy, cz);
        Vertex s2 = new Vertex(cx + stemWidth, cy, cz);
        Vertex s3 = new Vertex(cx + stemWidth, cy + stemHeight, cz);
        Vertex s4 = new Vertex(cx - stemWidth, cy + stemHeight, cz);
        for (Vertex v : new Vertex[] { s1, s2, s3, s4 }) {
            setColor(v, stemColor);
            verts.add(v);
        }
        addQuad(idx, idx + 1, idx + 2, idx + 3, stemColor, faces);
        addQuad(idx, idx + 3, idx + 2, idx + 1, stemColor, faces);

        // Bloom: a small billboard cross at the top of the stem.
        Color bloomColor = BLOOM_COLORS[rng.nextInt(BLOOM_COLORS.length)];
        double bloomSize = size * VISUAL_SCALE * (0.35 + rng.nextDouble() * 0.2);
        double angle = rng.nextDouble() * Math.PI;
        addCard(cx, cy + stemHeight, cz, angle, bloomSize / 2, bloomColor, verts, faces);
        addCard(cx, cy + stemHeight, cz, angle + Math.PI / 2, bloomSize / 2, bloomColor, verts, faces);
    }

    private void addCard(double cx, double cy, double cz, double angle, double half,
            Color color, List<Vertex> verts, List<Data.Face> faces) {
        double dx = Math.cos(angle) * half;
        double dz = Math.sin(angle) * half;

        int idx = verts.size();
        Vertex v1 = new Vertex(cx - dx, cy - half, cz - dz);
        Vertex v2 = new Vertex(cx + dx, cy - half, cz + dz);
        Vertex v3 = new Vertex(cx + dx, cy + half, cz + dz);
        Vertex v4 = new Vertex(cx - dx, cy + half, cz - dz);
        for (Vertex v : new Vertex[] { v1, v2, v3, v4 }) {
            setColor(v, color);
            verts.add(v);
        }
        addQuad(idx, idx + 1, idx + 2, idx + 3, color, faces);
        addQuad(idx, idx + 3, idx + 2, idx + 1, color, faces);
    }

    private void addQuad(int a, int b, int c, int d, Color faceColor, List<Data.Face> faces) {
        Data.Edge e1 = new Data.Edge(a, b);
        Data.Edge e2 = new Data.Edge(b, c);
        Data.Edge e3 = new Data.Edge(c, d);
        Data.Edge e4 = new Data.Edge(d, a);
        faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, faceColor));
    }

    private static void setColor(Vertex v, Color c) {
        v.r = c.getRed();
        v.g = c.getGreen();
        v.b = c.getBlue();
    }
}