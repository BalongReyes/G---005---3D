package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;
import java.util.Random;

// Branching coral clumps for the seabed. Uses the KELP category so it
// competes with KelpModule/AirModule for submerged cells above SOLID -
// see the note at the bottom of KelpModule.java about that column
// restriction. Unlike Kelp, coral doesn't stack on itself (Up -> AIR only),
// it's a single rooted cell-sized clump.
public class CoralModule extends WFCModule {

    private static final int BRANCHES_PER_CLUMP = 5;
    private static final int SEGMENTS_PER_BRANCH = 3;
    private static final double VISUAL_SCALE = 0.5;

    private static final Color[] CORAL_COLORS = {
            new Color(230, 110, 90), // coral-orange
            new Color(220, 90, 150), // pink
            new Color(160, 100, 210), // purple
            new Color(240, 190, 80) // yellow-gold
    };

    public CoralModule() {
        super(SocketType.KELP, "Coral",
                new SocketType[] {
                        SocketType.WILDCARD,
                        SocketType.WILDCARD,
                        SocketType.AIR, // Up (+Y) - nothing stacks on coral
                        SocketType.SOLID, // Down (-Y) - roots into seabed
                        SocketType.WILDCARD,
                        SocketType.WILDCARD
                },
                new SocketType[][] {
                        { SocketType.WILDCARD },
                        { SocketType.WILDCARD },
                        { SocketType.AIR, SocketType.KELP }, // open water or kelp above
                        { SocketType.SOLID },
                        { SocketType.WILDCARD },
                        { SocketType.WILDCARD }
                },
                new Color(220, 120, 100), 1.5);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {

        double exactY = Core.World.TerrainGenerator.getExactHeightAt(cx, cz, cy, grid, isoLevel);
        generateMesh(cx, exactY, cz, size, verts, faces);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {

        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        Color clumpColor = CORAL_COLORS[rng.nextInt(CORAL_COLORS.length)];
        double clumpScale = size * VISUAL_SCALE;

        for (int b = 0; b < BRANCHES_PER_CLUMP; b++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double lean = 0.15 + rng.nextDouble() * 0.35; // how far branch leans from vertical
            double offsetRadius = clumpScale * (0.05 + rng.nextDouble() * 0.15);
            double branchCx = cx + Math.cos(angle) * offsetRadius;
            double branchCz = cz + Math.sin(angle) * offsetRadius;
            double branchHeight = clumpScale * (0.5 + rng.nextDouble() * 0.6);
            double width = clumpScale * (0.05 + rng.nextDouble() * 0.04);

            Color branchColor = shiftHue(clumpColor, (rng.nextFloat() - 0.5f) * 0.05f);
            addBranch(branchCx, cy, branchCz, angle, lean, branchHeight, width, rng,
                    branchColor, verts, faces);
        }
    }

    // A branch is a rigid chain of tapering cross-sections, angled outward
    // and slightly randomized per segment - no sway (coral is stiff, unlike
    // kelp), just a consistent outward lean from the base.
    private void addBranch(double cx, double cy, double cz, double leanAngle, double leanStrength,
            double height, double width, Random rng, Color branchColor,
            List<Vertex> verts, List<Data.Face> faces) {

        double dirX = Math.cos(leanAngle);
        double dirZ = Math.sin(leanAngle);

        Color darkColor = adjust(branchColor, 0.6f, 0f);
        Color tipColor = adjust(branchColor, 1.05f, 0.15f);

        int[] rows = new int[SEGMENTS_PER_BRANCH];
        for (int i = 0; i < SEGMENTS_PER_BRANCH; i++) {
            double t = (double) i / (SEGMENTS_PER_BRANCH - 1);
            double drift = height * leanStrength * t * t; // accelerating outward lean
            double rowCx = cx + dirX * drift;
            double rowCz = cz + dirZ * drift;
            double rowCy = cy + height * t;
            double rowWidth = width * (1.0 - 0.7 * t);

            int idx = verts.size();
            Vertex left = new Vertex(rowCx - dirZ * rowWidth, rowCy, rowCz + dirX * rowWidth);
            Vertex right = new Vertex(rowCx + dirZ * rowWidth, rowCy, rowCz - dirX * rowWidth);
            Color rowColor = blend(darkColor, tipColor, (float) t);
            setColor(left, rowColor);
            setColor(right, rowColor);
            verts.add(left);
            verts.add(right);
            rows[i] = idx;
        }

        for (int i = 0; i < rows.length - 1; i++) {
            addSegmentFaces(rows[i], rows[i + 1], tipColor, faces);
        }
    }

    private static Color blend(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clamp(r), clamp(g), clamp(bl));
    }

    private void addSegmentFaces(int rowA, int rowB, Color faceColor, List<Data.Face> faces) {
        int[][] sides = {
                { rowA, rowA + 1, rowB + 1, rowB },
                { rowA, rowB, rowB + 1, rowA + 1 }
        };
        for (int[] f : sides) {
            Data.Edge e1 = new Data.Edge(f[0], f[1]);
            Data.Edge e2 = new Data.Edge(f[1], f[2]);
            Data.Edge e3 = new Data.Edge(f[2], f[3]);
            Data.Edge e4 = new Data.Edge(f[3], f[0]);
            faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, faceColor));
        }
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