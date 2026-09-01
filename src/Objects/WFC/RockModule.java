package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;
import java.util.Random;

// Low-poly boulder/rock clutter for surface cells. Same socket shape as
// GroundModule (Up=AIR, Down=SOLID) so it drops straight into the existing
// GRASS-category surface layer alongside Grass/Tree/Ground - just register
// it in WFCGenerator.init() and the solver will start picking it.
public class RockModule extends WFCModule {

    // --- Tunables --------------------------------------------------------
    // ROCKS_PER_CELL boulders, each ROCK_SIDES-sided (ring + apex + a
    // slightly-sunk base ring so it doesn't look like it's floating on the
    // surface). Cheaper than a tree, so this can have a decent weight
    // without hurting framerate much.
    private static final int ROCKS_PER_CELL = 2;
    private static final int ROCK_SIDES = 7;
    // How much each ring vertex's radius jitters, as a fraction of the
    // rock's base radius - this is what gives the faceted "boulder" look
    // instead of a perfect cone/dome.
    private static final double RADIUS_JITTER = 0.35;
    private static final double VISUAL_SCALE = 0.5;
    // -----------------------------------------------------------------------

    public RockModule() {
        super(SocketType.GRASS, "Rock",
                new SocketType[] {
                        SocketType.WILDCARD, // Right (+X)
                        SocketType.WILDCARD, // Left (-X)
                        SocketType.AIR, // Up (+Y) - nothing stacks on a rock
                        SocketType.SOLID, // Down (-Y) - sits on solid ground
                        SocketType.WILDCARD, // Forward (+Z)
                        SocketType.WILDCARD // Back (-Z)
                },
                new SocketType[][] {
                        { SocketType.WILDCARD },
                        { SocketType.WILDCARD },
                        { SocketType.AIR },
                        { SocketType.SOLID },
                        { SocketType.WILDCARD },
                        { SocketType.WILDCARD }
                },
                new Color(120, 118, 110), 1.5);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {

        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        for (int i = 0; i < ROCKS_PER_CELL; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double offsetRadius = size * (0.15 + rng.nextDouble() * 0.2);
            double rockCx = cx + Math.cos(angle) * offsetRadius;
            double rockCz = cz + Math.sin(angle) * offsetRadius;

            double exactY = Core.World.TerrainGenerator.getExactHeightAt(rockCx, rockCz, cy, grid, isoLevel);
            double rockSize = size * (0.35 + rng.nextDouble() * 0.35) * VISUAL_SCALE;

            addRock(rockCx, exactY, rockCz, rockSize, rng, verts, faces);
        }
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {

        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        for (int i = 0; i < ROCKS_PER_CELL; i++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double offsetRadius = size * (0.15 + rng.nextDouble() * 0.2);
            double rockCx = cx + Math.cos(angle) * offsetRadius;
            double rockCz = cz + Math.sin(angle) * offsetRadius;
            double rockSize = size * (0.35 + rng.nextDouble() * 0.35) * VISUAL_SCALE;

            addRock(rockCx, cy, rockCz, rockSize, rng, verts, faces);
        }
    }

    // A rock is: a slightly-sunk base ring (so it "beds in" to the ground
    // rather than sitting on top of it), a jittered mid ring for the bulk of
    // the shape, and an apex point - basically a faceted, irregular cone.
    private void addRock(double cx, double baseY, double cz, double radius, Random rng,
            List<Vertex> verts, List<Data.Face> faces) {

        double sinkDepth = radius * 0.12;
        double midHeight = radius * (0.5 + rng.nextDouble() * 0.3);
        double apexHeight = radius * (0.9 + rng.nextDouble() * 0.5);

        double baseRotation = rng.nextDouble() * Math.PI * 2;

        Color baseColor = adjust(color, 0.65f, 0f);
        Color midColor = adjust(color, 0.9f, 0.05f);
        Color apexColor = adjust(color, 1.05f, 0.1f);

        float hueJitter = (rng.nextFloat() - 0.5f) * 0.04f;
        baseColor = shiftHue(baseColor, hueJitter);
        midColor = shiftHue(midColor, hueJitter);

        int[] baseRing = addJitteredRing(cx, baseY - sinkDepth, cz, radius * 0.85, baseRotation, rng, baseColor, verts);
        int[] midRing = addJitteredRing(cx, baseY + midHeight, cz, radius, baseRotation + 0.3, rng, midColor, verts);

        int apexIdx = verts.size();
        Vertex apex = new Vertex(cx + (rng.nextDouble() - 0.5) * radius * 0.2, baseY + apexHeight,
                cz + (rng.nextDouble() - 0.5) * radius * 0.2);
        setColor(apex, apexColor);
        verts.add(apex);

        addRingFaces(baseRing, midRing, midColor, faces);
        addFanFaces(midRing, apexIdx, apexColor, faces);
    }

    private int[] addJitteredRing(double cx, double cy, double cz, double radius, double rotation,
            Random rng, Color ringColor, List<Vertex> verts) {
        int[] ring = new int[ROCK_SIDES];
        for (int i = 0; i < ROCK_SIDES; i++) {
            double angle = rotation + Math.PI * 2 * i / ROCK_SIDES;
            double r = radius * (1.0 - RADIUS_JITTER / 2 + rng.nextDouble() * RADIUS_JITTER);
            Vertex v = new Vertex(cx + Math.cos(angle) * r, cy, cz + Math.sin(angle) * r);
            setColor(v, ringColor);
            ring[i] = verts.size();
            verts.add(v);
        }
        return ring;
    }

    private void addRingFaces(int[] ringA, int[] ringB, Color faceColor, List<Data.Face> faces) {
        int sides = ringA.length;
        for (int i = 0; i < sides; i++) {
            int a0 = ringA[i], a1 = ringA[(i + 1) % sides];
            int b0 = ringB[i], b1 = ringB[(i + 1) % sides];
            addQuad(a0, b0, b1, a1, faceColor, faces);
        }
    }

    private void addFanFaces(int[] ring, int apex, Color faceColor, List<Data.Face> faces) {
        int sides = ring.length;
        for (int i = 0; i < sides; i++) {
            int a = ring[i], b = ring[(i + 1) % sides];
            Data.Edge e1 = new Data.Edge(a, apex);
            Data.Edge e2 = new Data.Edge(apex, b);
            Data.Edge e3 = new Data.Edge(b, a);
            faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3 }, faceColor));
        }
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

    private static Color adjust(Color c, float factor, float lift) {
        int r = clamp((int) (c.getRed() * factor + (255 - c.getRed() * factor) * lift));
        int g = clamp((int) (c.getGreen() * factor + (255 - c.getGreen() * factor) * lift));
        int b = clamp((int) (c.getBlue() * factor + (255 - c.getBlue() * factor) * lift));
        return new Color(r, g, b);
    }

    private static Color shiftHue(Color c, float amount) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[0] = (hsb[0] + amount + 1f) % 1f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}