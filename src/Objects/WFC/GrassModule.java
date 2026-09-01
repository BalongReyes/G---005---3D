package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;
import java.util.Random;

public class GrassModule extends WFCModule {

    // --- Tunables --------------------------------------------------------
    // Total geometry per cell scales as TUFTS_PER_CELL * BLADES_PER_TUFT * 4
    // quad faces (2 bend segments x 2 sides each). The placeholder mesh in
    // WFCGenerator accumulates for the whole terrain without clearing, so if
    // chunk generation gets slow, dial these down first.
    private static final int TUFTS_PER_CELL = 2;
    private static final int BLADES_PER_TUFT = 3;
    // Extra cross-section between mid and tip. Adds ~50% more faces per
    // blade (3 segments instead of 2) in exchange for a less blocky,
    // slightly curved silhouette with a pointed tip instead of a flat quad.
    private static final boolean DETAILED_BLADES = true;
    // Splits each WFC cell footprint into a QUAD_SPLIT x QUAD_SPLIT grid of
    // sub-patches (2 -> 4 patches) and scatters TUFTS_PER_CELL tufts within
    // each patch instead of across the whole cell. Tuft footprint/height are
    // sized off the patch, not the full cell, so blades end up roughly
    // QUAD_SPLIT times smaller in each dimension - this is what keeps grass
    // from looking like oversized slabs on large mountain cells while still
    // covering the same footprint. Total faces scale with QUAD_SPLIT^2, so
    // going from 1 to 2 quadruples the geometry - drop TUFTS_PER_CELL or
    // BLADES_PER_TUFT if that's too costly.
    private static final int QUAD_SPLIT = 2;
    // Scales the grass size without shrinking its scatter footprint
    private static final double VISUAL_SCALE = 0.4;
    // -----------------------------------------------------------------------

    public GrassModule() {
        super(SocketType.GRASS, "Grass",
                // What this module PROVIDES to its neighbors
                new SocketType[] {
                        SocketType.WILDCARD, // Right (+X)
                        SocketType.WILDCARD, // Left (-X)
                        SocketType.GRASS, // Up (+Y)
                        SocketType.SOLID, // Down (-Y) (It acts as a top-layer of Solid)
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
                new Color(50, 200, 50), 5.0);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {
        
        double patchSize = size / QUAD_SPLIT;
        double patchStart = -size / 2.0 + patchSize / 2.0;

        for (int qx = 0; qx < QUAD_SPLIT; qx++) {
            for (int qz = 0; qz < QUAD_SPLIT; qz++) {
                double patchCx = cx + patchStart + qx * patchSize;
                double patchCz = cz + patchStart + qz * patchSize;

                // Deterministic per-patch randomness using the base cy
                long seed = (Double.doubleToLongBits(patchCx) * 73856093L)
                        ^ (Double.doubleToLongBits(cy) * 19349663L)
                        ^ (Double.doubleToLongBits(patchCz) * 83492791L);
                Random rng = new Random(seed);

                for (int t = 0; t < TUFTS_PER_CELL; t++) {
                    double jitterRange = patchSize * 0.30;
                    double tuftX = patchCx + (rng.nextDouble() * 2 - 1) * jitterRange;
                    double tuftZ = patchCz + (rng.nextDouble() * 2 - 1) * jitterRange;

                    double tuftScale = 0.55 + rng.nextDouble() * 0.35; // vary tuft footprint
                    
                    // Snap exactly to terrain!
                    double exactY = Core.World.TerrainGenerator.getExactHeightAt(tuftX, tuftZ, cy, grid, isoLevel);
                    
                    // Get terrain normal and build an orthonormal basis (T, N, B)
                    double[] N = Core.World.TerrainGenerator.getSurfaceNormalAt(tuftX, exactY, tuftZ, grid);
                    
                    // Blend normal with strict UP to prevent extreme horizontal grass on cliffs
                    // (Plants still grow upwards!)
                    double blendUp = 0.4;
                    N[0] = N[0] * (1.0 - blendUp);
                    N[1] = N[1] * (1.0 - blendUp) + blendUp; // UP is (0,1,0)
                    N[2] = N[2] * (1.0 - blendUp);
                    
                    double nLen = Math.sqrt(N[0]*N[0] + N[1]*N[1] + N[2]*N[2]);
                    N[0] /= nLen; N[1] /= nLen; N[2] /= nLen;
                    
                    // Calculate tangent T (project global X onto N plane)
                    double dotX = N[0]; // (1,0,0) dot N
                    double[] T = { 1.0 - dotX * N[0], -dotX * N[1], -dotX * N[2] };
                    double lenT = Math.sqrt(T[0]*T[0] + T[1]*T[1] + T[2]*T[2]);
                    if (lenT > 0.0001) {
                        T[0] /= lenT; T[1] /= lenT; T[2] /= lenT;
                    } else {
                        T = new double[]{0, 0, 1}; // fallback if N is perfectly horizontal along X
                    }
                    
                    // B = N cross T
                    double[] B = {
                        N[1]*T[2] - N[2]*T[1],
                        N[2]*T[0] - N[0]*T[2],
                        N[0]*T[1] - N[1]*T[0]
                    };
                    
                    double[][] basis = { T, N, B };
                    
                    addTuft(tuftX, exactY, tuftZ, patchSize * tuftScale, rng, verts, faces, basis);
                }
            }
        }
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {

        double patchSize = size / QUAD_SPLIT;
        double patchStart = -size / 2.0 + patchSize / 2.0;

        for (int qx = 0; qx < QUAD_SPLIT; qx++) {
            for (int qz = 0; qz < QUAD_SPLIT; qz++) {
                double patchCx = cx + patchStart + qx * patchSize;
                double patchCz = cz + patchStart + qz * patchSize;

                // Deterministic per-patch randomness: identical patches always
                // regenerate identically (no flicker on rebuild), but
                // neighboring patches - including the 4 within one cell -
                // differ from each other.
                long seed = (Double.doubleToLongBits(patchCx) * 73856093L)
                        ^ (Double.doubleToLongBits(cy) * 19349663L)
                        ^ (Double.doubleToLongBits(patchCz) * 83492791L);
                Random rng = new Random(seed);

                for (int t = 0; t < TUFTS_PER_CELL; t++) {
                    // Scatter tufts around the patch footprint instead of a single
                    // clump dead-center, so the ground reads as a field rather than
                    // a grid of dots.
                    double jitterRange = patchSize * 0.30;
                    double tuftX = patchCx + (rng.nextDouble() * 2 - 1) * jitterRange;
                    double tuftZ = patchCz + (rng.nextDouble() * 2 - 1) * jitterRange;

                    double tuftScale = 0.55 + rng.nextDouble() * 0.35; // vary tuft footprint
                    double[][] defaultBasis = { {1,0,0}, {0,1,0}, {0,0,1} };
                    addTuft(tuftX, cy, tuftZ, patchSize * tuftScale, rng, verts, faces, defaultBasis);
                }
            }
        }
    }

    private void addTuft(double cx, double cy, double cz, double size, Random rng,
            List<Vertex> verts, List<Data.Face> faces, double[][] basis) {

        size = size * VISUAL_SCALE;
        double s = size / 2.0;
        double baseRotation = rng.nextDouble() * Math.PI;
        double heightJitter = 0.8 + rng.nextDouble() * 0.4;
        double tuftHeight = size * heightJitter;

        // Base -> mid -> tip color ramp (dark "soil" green up to a light,
        // slightly yellowed tip), plus a small per-tuft hue jitter for variety.
        Color baseColor = adjust(color, 0.55f, 0f);
        Color midColor = adjust(color, 0.85f, 0.10f);
        Color tipColor = adjust(color, 1.0f, 0.35f);

        float hueJitter = (rng.nextFloat() - 0.5f) * 0.08f;
        baseColor = shiftHue(baseColor, hueJitter);
        midColor = shiftHue(midColor, hueJitter);
        tipColor = shiftHue(tipColor, hueJitter);

        for (int i = 0; i < BLADES_PER_TUFT; i++) {
            double angle = baseRotation + (Math.PI / BLADES_PER_TUFT) * i;
            double widthJitter = 0.75 + rng.nextDouble() * 0.5;
            double dx = Math.cos(angle) * s * widthJitter;
            double dz = Math.sin(angle) * s * widthJitter;

            // Independent lean direction/strength per blade, applied gradually
            // up the height, so each blade curves rather than standing rigid.
            double leanAngle = rng.nextDouble() * Math.PI * 2;
            double leanAmount = size * (0.15 + rng.nextDouble() * 0.25);
            double leanX = Math.cos(leanAngle) * leanAmount;
            double leanZ = Math.sin(leanAngle) * leanAmount;

            double h = tuftHeight * (0.85 + rng.nextDouble() * 0.3);

            addBentBlade(cx, cy, cz, dx, dz, leanX, leanZ, h,
                    baseColor, midColor, tipColor, verts, faces, basis);
        }
    }

    // A blade built from stacked cross-sections (base -> ... -> tip) that
    // leans sideways as it rises, giving a curved silhouette instead of a
    // rigid flat rectangle, and tapers inward toward a near-zero-width tip
    // so it reads as a pointed blade rather than a flat quad.
    private void addBentBlade(double cx, double cy, double cz, double dx, double dz,
            double leanX, double leanZ, double height, Color baseColor, Color midColor,
            Color tipColor, List<Vertex> verts, List<Data.Face> faces, double[][] basis) {

        if (!DETAILED_BLADES) {
            double midTaper = 0.75;
            double tipTaper = 0.35;
            double midY = cy + height * 0.5;
            double tipY = cy + height;
            double midLeanX = leanX * 0.35, midLeanZ = leanZ * 0.35;

            int vBase = addRow(cx, cy, cz, dx, dz, 0, 0, 0.0, baseColor, verts, basis);
            int vMid = addRow(cx, cy, cz, dx * midTaper, dz * midTaper,
                    midLeanX, midLeanZ, height * 0.5, midColor, verts, basis);
            int vTip = addRow(cx, cy, cz, dx * tipTaper, dz * tipTaper,
                    leanX, leanZ, height, tipColor, verts, basis);

            addSegmentFaces(vBase, vMid, midColor, faces);
            addSegmentFaces(vMid, vTip, tipColor, faces);
            return;
        }

        // Cross-sections at t = 0 (base), 0.45 (low), 0.8 (upper), 1.0 (tip).
        // Width taper accelerates toward the tip and the tip width collapses
        // to a sliver so the blade comes to a soft point instead of ending
        // in a flat-topped quad. Lean is distributed with an ease-in curve
        // (t^1.5) so blades stay straighter near the base and curve more
        // toward the top, which reads as a natural bend rather than a hinge.
        double[] tParams = { 0.0, 0.45, 0.8, 1.0 };
        double[] widthTaper = { 1.0, 0.85, 0.55, 0.08 };
        Color[] colors = { baseColor, midColor, blend(midColor, tipColor, 0.5f), tipColor };

        int[] rows = new int[tParams.length];
        for (int i = 0; i < tParams.length; i++) {
            double t = tParams[i];
            double leanT = Math.pow(t, 1.5);
            double rowDy = height * t;
            double rowDx = dx * widthTaper[i];
            double rowDz = dz * widthTaper[i];
            double rowLeanX = leanX * leanT;
            double rowLeanZ = leanZ * leanT;
            rows[i] = addRow(cx, cy, cz, rowDx, rowDz, rowLeanX, rowLeanZ, rowDy, colors[i], verts, basis);
        }

        for (int i = 0; i < rows.length - 1; i++) {
            addSegmentFaces(rows[i], rows[i + 1], colors[i + 1], faces);
        }
    }

    // Linear RGB blend between two colors, used for an in-between shade on
    // the extra cross-section added by DETAILED_BLADES.
    private static Color blend(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(clamp(r), clamp(g), clamp(bl));
    }

    // Adds a left/right vertex pair for one cross-section of a blade and
    // returns the index of the left vertex (right is always left + 1).
    private int addRow(double cx, double cy, double cz, double dx, double dz,
            double leanX, double leanZ, double dy, Color rowColor, List<Vertex> verts, double[][] basis) {
        int idx = verts.size();
        
        // Left vertex local coords relative to base (cx, cy, cz)
        double lx = -dx + leanX;
        double lz = -dz + leanZ;
        
        // Right vertex local coords
        double rx = dx + leanX;
        double rz = dz + leanZ;

        // Apply basis transformation (T = basis[0], N = basis[1], B = basis[2])
        Vertex left = new Vertex(
            cx + lx * basis[0][0] + dy * basis[1][0] + lz * basis[2][0],
            cy + lx * basis[0][1] + dy * basis[1][1] + lz * basis[2][1],
            cz + lx * basis[0][2] + dy * basis[1][2] + lz * basis[2][2]
        );
        
        Vertex right = new Vertex(
            cx + rx * basis[0][0] + dy * basis[1][0] + rz * basis[2][0],
            cy + rx * basis[0][1] + dy * basis[1][1] + rz * basis[2][1],
            cz + rx * basis[0][2] + dy * basis[1][2] + rz * basis[2][2]
        );
        
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