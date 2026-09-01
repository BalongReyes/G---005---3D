package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;
import java.util.Random;

// A single vertical "segment" of a kelp strand, one per grid cell. Roots
// into SOLID seabed, stacks on top of itself (KELP -> KELP) to grow tall
// columns, and terminates into AIR once the solver picks Air for a cell -
// see the WFCGenerator.initChunk() note at the bottom of this file for the
// column-restriction change that lets Kelp/Air compete above a submerged
// seabed cell in the first place.
public class KelpModule extends WFCModule {

    // --- Tunables --------------------------------------------------------
    // FRONDS_PER_CELL fronds, each SEGMENTS_PER_FROND tall cross-sections
    // (base -> tip), so cost per cell is roughly
    // FRONDS_PER_CELL * (SEGMENTS_PER_FROND - 1) * 2 quad faces. Kept modest
    // since a tall kelp bed can stack many cells vertically.
    private static final int FRONDS_PER_CELL = 2;
    private static final int SEGMENTS_PER_FROND = 4;
    // How far a frond sways sideways over the height of one cell, as a
    // fraction of cell size. Alternating phase per segment is what gives the
    // S-curve "underwater current" look instead of a straight blade.
    private static final double SWAY_AMOUNT = 0.35;
    // -----------------------------------------------------------------------

    public KelpModule() {
        super(SocketType.KELP, "Kelp",
                // What this module PROVIDES to its neighbors
                new SocketType[] {
                        SocketType.WILDCARD, // Right (+X)
                        SocketType.WILDCARD, // Left (-X)
                        SocketType.KELP, // Up (+Y) - lets another kelp segment stack on top
                        SocketType.KELP, // Down (-Y) - lets a kelp segment below know it continues
                        SocketType.WILDCARD, // Forward (+Z)
                        SocketType.WILDCARD // Back (-Z)
                },
                // What this module ACCEPTS from its neighbors
                new SocketType[][] {
                        { SocketType.WILDCARD }, // Right (+X)
                        { SocketType.WILDCARD }, // Left (-X)
                        { SocketType.AIR, SocketType.KELP }, // Up (+Y) - open water or more kelp
                        { SocketType.SOLID, SocketType.KELP }, // Down (-Y) - seabed root or more kelp
                        { SocketType.WILDCARD }, // Forward (+Z)
                        { SocketType.WILDCARD } // Back (-Z)
                },
                new Color(40, 110, 90), 2.0);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {
            
        // Check if the cell directly below this one is SOLID terrain
        boolean isBase = Core.World.TerrainGenerator.isSolidAt(cx, cy - size, cz, grid, isoLevel);
        
        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        for (int f = 0; f < FRONDS_PER_CELL; f++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double offsetRadius = size * (0.10 + rng.nextDouble() * 0.15);
            double frondCx = cx + Math.cos(angle) * offsetRadius;
            double frondCz = cz + Math.sin(angle) * offsetRadius;

            double swayAngle = rng.nextDouble() * Math.PI * 2;
            double swayPhase = rng.nextDouble() * Math.PI * 2;
            double width = size * (0.12 + rng.nextDouble() * 0.06);

            // If we're the bottom-most segment, stretch down to perfectly anchor to the mesh.
            // Note that Kelp's original center 'cy' represents the bottom of the frond.
            double startY = cy;
            if (isBase) {
                startY = Core.World.TerrainGenerator.getExactHeightAt(frondCx, frondCz, cy, grid, isoLevel);
            }
            
            // End exactly at the standard WFC height boundary so the next kelp block connects seamlessly!
            double endY = cy + size;

            addFrond(frondCx, startY, endY, frondCz, size, width, swayAngle, swayPhase, rng, verts, faces);
        }
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {
            
        // Fallback for non-snapped generation
        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        for (int f = 0; f < FRONDS_PER_CELL; f++) {
            double angle = rng.nextDouble() * Math.PI * 2;
            double offsetRadius = size * (0.10 + rng.nextDouble() * 0.15);
            double frondCx = cx + Math.cos(angle) * offsetRadius;
            double frondCz = cz + Math.sin(angle) * offsetRadius;

            double swayAngle = rng.nextDouble() * Math.PI * 2;
            double swayPhase = rng.nextDouble() * Math.PI * 2;
            double width = size * (0.12 + rng.nextDouble() * 0.06);

            addFrond(frondCx, cy, cy + size, frondCz, size, width, swayAngle, swayPhase, rng, verts, faces);
        }
    }

    // Builds one swaying frond spanning the full cell height as a chain of
    // tapering cross-sections that drift sideways along a sine curve, giving
    // an underwater-current look rather than a rigid blade.
    private void addFrond(double cx, double startY, double endY, double cz, double height, double width,
            double swayAngle, double swayPhase, Random rng,
            List<Vertex> verts, List<Data.Face> faces) {

        double dirX = Math.cos(swayAngle);
        double dirZ = Math.sin(swayAngle);

        Color darkColor = adjust(color, 0.6f, 0f);
        Color midColor = adjust(color, 0.95f, 0.05f);
        Color tipColor = adjust(color, 1.0f, 0.25f);

        float hueJitter = (rng.nextFloat() - 0.5f) * 0.06f;
        darkColor = shiftHue(darkColor, hueJitter);
        midColor = shiftHue(midColor, hueJitter);
        tipColor = shiftHue(tipColor, hueJitter);

        int[] rows = new int[SEGMENTS_PER_FROND];
        Color[] colors = new Color[SEGMENTS_PER_FROND];

        for (int i = 0; i < SEGMENTS_PER_FROND; i++) {
            double t = (double) i / (SEGMENTS_PER_FROND - 1);

            // Sideways drift builds up gradually from a fixed root, using a
            // sine wave so the strand curves back and forth rather than
            // leaning in one direction.
            double sway = Math.sin(t * Math.PI * 1.5 + swayPhase) * height * SWAY_AMOUNT * t;
            double rowCx = cx + dirX * sway;
            double rowCz = cz + dirZ * sway;
            
            // Stretch vertically from our snapped base (startY) to our grid-aligned top (endY)
            double rowCy = startY + (endY - startY) * t;

            // Taper the blade width toward the tip, same idea as
            // GrassModule's bent blades.
            double rowWidth = width * (1.0 - 0.6 * t);

            Color rowColor = t < 0.5
                    ? blend(darkColor, midColor, (float) (t * 2))
                    : blend(midColor, tipColor, (float) ((t - 0.5) * 2));
            colors[i] = rowColor;

            rows[i] = addRow(rowCx, rowCy, rowCz, dirZ * rowWidth, -dirX * rowWidth, rowColor, verts);
        }

        for (int i = 0; i < rows.length - 1; i++) {
            addSegmentFaces(rows[i], rows[i + 1], colors[i + 1], faces);
        }
    }

    private static Color blend(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clamp(r), clamp(g), clamp(bl));
    }

    // Adds a left/right vertex pair for one cross-section and returns the
    // index of the left vertex (right is always left + 1).
    private int addRow(double cx, double cy, double cz, double dx, double dz,
            Color rowColor, List<Vertex> verts) {
        int idx = verts.size();
        Vertex left = new Vertex(cx - dx, cy, cz - dz);
        Vertex right = new Vertex(cx + dx, cy, cz + dz);
        setColor(left, rowColor);
        setColor(right, rowColor);
        verts.add(left);
        verts.add(right);
        return idx;
    }

    // Builds the double-sided quad connecting two rows (front + reversed winding).
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