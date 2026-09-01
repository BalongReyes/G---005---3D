package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;
import java.util.Random;

public class TreeModule extends WFCModule {

    // --- Tunables --------------------------------------------------------
    // One tree per cell: an N-sided trunk frustum plus CANOPY_LAYERS rings of
    // leaf clusters, each cluster a 2-quad billboard cross (4 double-sided
    // faces). Total faces roughly TRUNK_SIDES*2 + CANOPY_LAYERS *
    // CLUSTERS_PER_LAYER * 8. Trees are naturally heavier than a grass tuft,
    // so this module's weight (see constructor) is kept low relative to
    // GrassModule's so trees stay sparse.
    private static final int TRUNK_SIDES = 6;
    private static final int CANOPY_LAYERS = 3;
    private static final int CLUSTERS_PER_LAYER = 4;
    // Scales the tree down independently of the grid cell size
    private static final double VISUAL_SCALE = 0.6;
    // -----------------------------------------------------------------------

    // "color" (inherited) is used as the trunk color; canopy uses its own.
    private final Color foliageColor = new Color(40, 130, 40);

    public TreeModule() {
        super(SocketType.GRASS, "Tree",
                // What this module PROVIDES to its neighbors.
                // NOTE: uses the same category (GRASS) as GrassModule, so it
                // drops into WFCGenerator's existing surface-layer logic with
                // no changes there beyond registering it in the modules list
                // (see bottom of this file for the one line to add). The
                // solver then picks between Grass and Tree at each surface
                // cell, weighted by each module's weight.
                new SocketType[] {
                        SocketType.WILDCARD, // Right (+X)
                        SocketType.WILDCARD, // Left (-X)
                        SocketType.AIR, // Up (+Y) - nothing stacks on top of a tree
                        SocketType.SOLID, // Down (-Y) - trees root into solid ground
                        SocketType.WILDCARD, // Forward (+Z)
                        SocketType.WILDCARD // Back (-Z)
                },
                // What this module ACCEPTS from its neighbors
                new SocketType[][] {
                        { SocketType.WILDCARD }, // Right (+X)
                        { SocketType.WILDCARD }, // Left (-X)
                        { SocketType.AIR }, // Up (+Y)
                        { SocketType.SOLID }, // Down (-Y)
                        { SocketType.WILDCARD }, // Forward (+Z)
                        { SocketType.WILDCARD } // Back (-Z)
                },
                new Color(90, 60, 30), 1.0);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {
        
        // Snap the trunk exactly to the interpolated terrain surface
        double exactY = Core.World.TerrainGenerator.getExactHeightAt(cx, cz, cy, grid, isoLevel);
        
        generateMesh(cx, exactY, cz, size, verts, faces);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {

        // Deterministic per-cell randomness: identical cells always regenerate
        // identically (no flicker on rebuild), but neighboring cells differ.
        long seed = (Double.doubleToLongBits(cx) * 73856093L)
                ^ (Double.doubleToLongBits(cy) * 19349663L)
                ^ (Double.doubleToLongBits(cz) * 83492791L);
        Random rng = new Random(seed);

        double treeSize = size * VISUAL_SCALE;

        double trunkHeight = treeSize * (1.6 + rng.nextDouble() * 0.8);
        double baseRadius = treeSize * (0.10 + rng.nextDouble() * 0.05);
        double topRadius = baseRadius * 0.55;

        // A slight lean at the top of the trunk, carried into the canopy
        // center, so trees don't all stand perfectly vertical.
        double leanAngle = rng.nextDouble() * Math.PI * 2;
        double leanAmount = treeSize * rng.nextDouble() * 0.15;
        double leanX = Math.cos(leanAngle) * leanAmount;
        double leanZ = Math.sin(leanAngle) * leanAmount;

        double canopyBaseY = cy + trunkHeight;
        double canopyHeight = treeSize * (1.2 + rng.nextDouble() * 0.6);
        double canopyRadius = treeSize * (0.5 + rng.nextDouble() * 0.25);

        addTrunk(cx, cy, cz, leanX, leanZ, baseRadius, topRadius, canopyBaseY, verts, faces);
        addCanopy(cx + leanX, canopyBaseY, cz + leanZ, canopyRadius, canopyHeight,
                rng, verts, faces);
    }

    // Trunk as a tapered N-sided frustum (single segment, base -> top ring),
    // rounder than a flat blade quad and cheap enough for one per cell.
    private void addTrunk(double cx, double cy, double cz, double leanX, double leanZ,
            double baseRadius, double topRadius, double topY,
            List<Vertex> verts, List<Data.Face> faces) {

        Color baseColor = adjust(color, 0.75f, 0f);
        Color topColor = adjust(color, 1.0f, 0.08f);

        int[] baseRing = addRing(cx, cy, cz, baseRadius, TRUNK_SIDES, baseColor, verts);
        int[] topRing = addRing(cx + leanX, topY, cz + leanZ, topRadius, TRUNK_SIDES, topColor, verts);

        addRingFaces(baseRing, topRing, topColor, faces);
    }

    // Canopy as CANOPY_LAYERS horizontal bands of leaf clusters scattered
    // around an ellipsoid silhouette (narrow at top/bottom, wide in the
    // middle) rather than a single foliage blob, so the tree reads as
    // rounded rather than a floating box of leaves.
    private void addCanopy(double cx, double baseY, double cz, double radius, double height,
            Random rng, List<Vertex> verts, List<Data.Face> faces) {

        for (int layer = 0; layer < CANOPY_LAYERS; layer++) {
            double t = CANOPY_LAYERS == 1 ? 0.5 : (double) layer / (CANOPY_LAYERS - 1);
            double layerY = baseY + height * (0.15 + 0.7 * t);
            // Ellipse profile: 0 at top/bottom of the canopy, 1 at the middle.
            double profile = Math.sqrt(Math.max(0.0, 1.0 - Math.pow(2 * t - 1, 2)));
            double layerRadius = radius * (0.35 + 0.65 * profile);

            for (int c = 0; c < CLUSTERS_PER_LAYER; c++) {
                double angle = rng.nextDouble() * Math.PI * 2;
                double rJitter = layerRadius * (0.5 + rng.nextDouble() * 0.5);
                double clusterX = cx + Math.cos(angle) * rJitter;
                double clusterZ = cz + Math.sin(angle) * rJitter;
                double clusterY = layerY + (rng.nextDouble() - 0.5) * height * 0.15;

                double cardSize = radius * (0.45 + rng.nextDouble() * 0.3);

                Color leafColor = adjust(foliageColor, 0.8f + rng.nextFloat() * 0.35f, 0f);
                leafColor = shiftHue(leafColor, (rng.nextFloat() - 0.5f) * 0.06f);

                addLeafCluster(clusterX, clusterY, clusterZ, cardSize, rng, leafColor, verts, faces);
            }
        }
    }

    // A small clump of foliage built from two perpendicular double-sided
    // quad "cards" crossing through a point (a classic billboard cross),
    // cheap to build while still reading as volumetric from any angle.
    private void addLeafCluster(double cx, double cy, double cz, double cardSize, Random rng,
            Color color, List<Vertex> verts, List<Data.Face> faces) {

        double half = cardSize / 2.0;
        double angle = rng.nextDouble() * Math.PI;

        addQuadCard(cx, cy, cz, angle, half, color, verts, faces);
        addQuadCard(cx, cy, cz, angle + Math.PI / 2, half, color, verts, faces);
    }

    // Builds one vertical double-sided quad of the given half-size, centered
    // at (cx,cy,cz) and rotated around the vertical axis by angle.
    private void addQuadCard(double cx, double cy, double cz, double angle, double half,
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

        int[][] sides = {
                { idx, idx + 1, idx + 2, idx + 3 },
                { idx, idx + 3, idx + 2, idx + 1 }
        };
        for (int[] f : sides) {
            Data.Edge e1 = new Data.Edge(f[0], f[1]);
            Data.Edge e2 = new Data.Edge(f[1], f[2]);
            Data.Edge e3 = new Data.Edge(f[2], f[3]);
            Data.Edge e4 = new Data.Edge(f[3], f[0]);
            faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, color));
        }
    }

    // Builds one horizontal ring of TRUNK_SIDES vertices and returns their
    // indices in order.
    private int[] addRing(double cx, double cy, double cz, double radius, int sides,
            Color ringColor, List<Vertex> verts) {
        int[] ring = new int[sides];
        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2 * i / sides;
            Vertex v = new Vertex(cx + Math.cos(angle) * radius, cy, cz + Math.sin(angle) * radius);
            setColor(v, ringColor);
            ring[i] = verts.size();
            verts.add(v);
        }
        return ring;
    }

    // Connects two same-length rings into a band of double-sided quads
    // (trunk sides), so the trunk reads correctly even without backface culling.
    private void addRingFaces(int[] ringA, int[] ringB, Color faceColor, List<Data.Face> faces) {
        int sides = ringA.length;
        for (int i = 0; i < sides; i++) {
            int a0 = ringA[i], a1 = ringA[(i + 1) % sides];
            int b0 = ringB[i], b1 = ringB[(i + 1) % sides];

            int[][] quads = {
                    { a0, a1, b1, b0 },
                    { a0, b0, b1, a1 }
            };
            for (int[] f : quads) {
                Data.Edge e1 = new Data.Edge(f[0], f[1]);
                Data.Edge e2 = new Data.Edge(f[1], f[2]);
                Data.Edge e3 = new Data.Edge(f[2], f[3]);
                Data.Edge e4 = new Data.Edge(f[3], f[0]);
                faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, faceColor));
            }
        }
    }

    private static void setColor(Vertex v, Color c) {
        v.r = c.getRed();
        v.g = c.getGreen();
        v.b = c.getBlue();
    }

    // factor scales brightness (0..1+), lift pushes toward white (0..1) for lighter
    // rows.
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