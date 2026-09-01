package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Core.WFC.RoadNetwork;
import Data.Vertex;
import java.awt.Color;
import java.util.List;

// A road-deck tile that spans a gap (water, ravine) instead of sitting on
// flattened ground. WFCGenerator places these wherever RoadNetwork marks a
// column as a bridge span - see the post-pass in initChunk() below. Unlike
// RoadModule this does NOT flatten the terrain underneath (leaving the gap
// intact is the whole point) and grows a support pylon down to whatever
// solid ground or seabed actually exists below the deck.
public class BridgeModule extends WFCModule {

    private static final Color DECK_COLOR = new Color(90, 75, 55);
    private static final Color RAIL_COLOR = new Color(70, 58, 42);
    private static final Color PYLON_COLOR = new Color(80, 80, 85);

    private static final double DECK_THICKNESS_FRAC = 0.08;
    private static final double RAIL_HEIGHT_FRAC = 0.25;
    private static final double PYLON_WIDTH_FRAC = 0.12;

    private final int connectionMask;

    public BridgeModule(int connectionMask) {
        super(SocketType.ROAD, "Bridge_" + connectionMask,
                buildProvided(connectionMask), buildAccepted(connectionMask), DECK_COLOR, 1.0);
        this.connectionMask = connectionMask;
    }

    public int getConnectionMask() {
        return connectionMask;
    }

    private static SocketType[] buildProvided(int mask) {
        return new SocketType[] {
                has(mask, RoadNetwork.DIR_PX) ? SocketType.ROAD : SocketType.WILDCARD,
                has(mask, RoadNetwork.DIR_NX) ? SocketType.ROAD : SocketType.WILDCARD,
                SocketType.AIR, // Up - nothing stacks on the deck
                SocketType.AIR, // Down - the deck is suspended, not rooted
                has(mask, RoadNetwork.DIR_PZ) ? SocketType.ROAD : SocketType.WILDCARD,
                has(mask, RoadNetwork.DIR_NZ) ? SocketType.ROAD : SocketType.WILDCARD
        };
    }

    private static SocketType[][] buildAccepted(int mask) {
        return new SocketType[][] {
                { has(mask, RoadNetwork.DIR_PX) ? SocketType.ROAD : SocketType.WILDCARD },
                { has(mask, RoadNetwork.DIR_NX) ? SocketType.ROAD : SocketType.WILDCARD },
                { SocketType.AIR },
                // Below can legitimately be open air (ravine), water/kelp,
                // or shallow solid near the shore ends - all fine, since the
                // deck doesn't rest on whatever's underneath it.
                { SocketType.AIR, SocketType.KELP, SocketType.SOLID },
                { has(mask, RoadNetwork.DIR_PZ) ? SocketType.ROAD : SocketType.WILDCARD },
                { has(mask, RoadNetwork.DIR_NZ) ? SocketType.ROAD : SocketType.WILDCARD }
        };
    }

    private static boolean has(int mask, int dir) {
        return (mask & dir) != 0;
    }

    // Deliberately does NOT flatten - the gap below is exactly what the
    // bridge exists to preserve.
    @Override
    public boolean modifyTerrain(Core.World.ScalarField field, int gx, int gy, int gz) {
        return false;
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {
        boolean spansX = has(connectionMask, RoadNetwork.DIR_PX) || has(connectionMask, RoadNetwork.DIR_NX);
        addDeckAndRails(cx, cy, cz, size, spansX, verts, faces);
        addPylon(cx, cy, cz, size, grid, isoLevel, verts, faces);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {
        // Fallback for non-snapped generation: no scalar field available to
        // find the floor, so draw the deck without a pylon.
        boolean spansX = has(connectionMask, RoadNetwork.DIR_PX) || has(connectionMask, RoadNetwork.DIR_NX);
        addDeckAndRails(cx, cy, cz, size, spansX, verts, faces);
    }

    private void addDeckAndRails(double cx, double cy, double cz, double size, boolean spansX,
            List<Vertex> verts, List<Data.Face> faces) {

        double s = size / 2.0;
        double deckThickness = size * DECK_THICKNESS_FRAC;
        double topY = cy;
        double bottomY = cy - deckThickness;

        addBox(cx, bottomY, cz, topY, s, s, DECK_COLOR, verts, faces);

        // Railings along the two long edges only, running the span
        // direction - a straight-X bridge gets rails on +Z/-Z and vice versa.
        double railHeight = size * RAIL_HEIGHT_FRAC;
        double railThickness = size * 0.03;
        if (spansX) {
            addBox(cx, topY, cz - s + railThickness, topY + railHeight, s, railThickness / 2, RAIL_COLOR, verts, faces);
            addBox(cx, topY, cz + s - railThickness, topY + railHeight, s, railThickness / 2, RAIL_COLOR, verts, faces);
        } else {
            addBox(cx - s + railThickness, topY, cz, topY + railHeight, railThickness / 2, s, RAIL_COLOR, verts, faces);
            addBox(cx + s - railThickness, topY, cz, topY + railHeight, railThickness / 2, s, RAIL_COLOR, verts, faces);
        }
    }

    // Grows a single support pylon straight down from the deck to the
    // actual solid seabed/ground, so a span over deep water still gets a
    // visible pier rather than a deck that appears to float. Search depth is
    // capped so a very deep trench doesn't grow an unbounded pylon.
    private void addPylon(double cx, double cy, double cz, double size,
            Core.World.ScalarField grid, double isoLevel, List<Vertex> verts, List<Data.Face> faces) {

        double spacing = Settings.WorldSettings.SPACING;
        double maxSearchDepth = size * 20;
        double floorY = cy - maxSearchDepth;

        for (double probeY = cy; probeY > cy - maxSearchDepth; probeY -= spacing) {
            if (Core.World.TerrainGenerator.isSolidAt(cx, probeY, cz, grid, isoLevel)) {
                floorY = Core.World.TerrainGenerator.getExactHeightAt(cx, cz, probeY, grid, isoLevel);
                break;
            }
        }

        double pylonWidth = size * PYLON_WIDTH_FRAC;
        addBox(cx, floorY, cz, cy - size * DECK_THICKNESS_FRAC, pylonWidth / 2, pylonWidth / 2,
                PYLON_COLOR, verts, faces);
    }

    // Axis-aligned box from bottomY to topY, half-extents hx/hz.
    private void addBox(double cx, double bottomY, double cz, double topY, double hx, double hz,
            Color boxColor, List<Vertex> verts, List<Data.Face> faces) {

        int idx = verts.size();
        double[][] corners = {
                { cx - hx, bottomY, cz - hz }, { cx + hx, bottomY, cz - hz },
                { cx + hx, bottomY, cz + hz }, { cx - hx, bottomY, cz + hz },
                { cx - hx, topY, cz - hz }, { cx + hx, topY, cz - hz },
                { cx + hx, topY, cz + hz }, { cx - hx, topY, cz + hz }
        };
        for (double[] c : corners) {
            Vertex v = new Vertex(c[0], c[1], c[2]);
            setColor(v, boxColor);
            verts.add(v);
        }

        int[][] faceIdx = {
                { 0, 3, 2, 1 }, { 4, 7, 6, 5 },
                { 0, 1, 5, 4 }, { 1, 2, 6, 5 },
                { 2, 3, 7, 6 }, { 3, 0, 4, 7 }
        };
        for (int[] f : faceIdx) {
            addQuad(idx + f[0], idx + f[1], idx + f[2], idx + f[3], boxColor, faces);
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
}
