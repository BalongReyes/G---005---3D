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
    private static final double RAIL_THICKNESS_FRAC = 0.05;
    private static final double PYLON_WIDTH_FRAC = 0.12;

    private final int connectionMask;
    private final Core.WFC.RoadNetwork roadNetwork;

    public BridgeModule(int connectionMask, Core.WFC.RoadNetwork roadNetwork) {
        super(SocketType.ROAD, "Bridge_" + connectionMask,
                buildProvided(connectionMask),
                buildAccepted(connectionMask),
                new Color(100, 100, 105), 1.0);
        this.roadNetwork = roadNetwork;
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

    private double getWorldY(double gridY, double size) {
        double startY = -((Settings.WorldSettings.GRID_SIZE_Y - 1) * size) / 2.0;
        return startY + gridY * size + size / 2.0;
    }

    private double getSlopeY(double x, double z, double cx, double cz, double s, double center_y, double px_y, double nx_y, double pz_y, double nz_y, boolean isAnchor) {
        if (isAnchor) return center_y;
        
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PX) && has(connectionMask, Core.WFC.RoadNetwork.DIR_NX) && 
            !has(connectionMask, Core.WFC.RoadNetwork.DIR_PZ) && !has(connectionMask, Core.WFC.RoadNetwork.DIR_NZ)) {
            double dx = (x - cx) / s;
            if (dx > 0) return center_y + (px_y - center_y) / 2.0 * dx;
            else return center_y + (center_y - nx_y) / 2.0 * dx;
        }
        if (!has(connectionMask, Core.WFC.RoadNetwork.DIR_PX) && !has(connectionMask, Core.WFC.RoadNetwork.DIR_NX) && 
            has(connectionMask, Core.WFC.RoadNetwork.DIR_PZ) && has(connectionMask, Core.WFC.RoadNetwork.DIR_NZ)) {
            double dz = (z - cz) / s;
            if (dz > 0) return center_y + (pz_y - center_y) / 2.0 * dz;
            else return center_y + (center_y - nz_y) / 2.0 * dz;
        }
        return center_y;
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

    private void addDeckAndRails(double cx, double cy, double cz, double size, boolean spansX_ignored,
            List<Vertex> verts, List<Data.Face> faces) {


        double s = size / 2.0;
        double startX = -((Settings.WorldSettings.GRID_SIZE_X - 1) * size) / 2.0;
        double startZ = -((Settings.WorldSettings.GRID_SIZE_Z - 1) * size) / 2.0;
        int gx = (int) Math.round((cx - startX - s) / size);
        int gz = (int) Math.round((cz - startZ - s) / size);
        
        double center_y = cy;
        double px_y = cy;
        double nx_y = cy;
        double pz_y = cy;
        double nz_y = cy;
        boolean isAnchor = true;
        
        if (roadNetwork != null) {
            center_y = getWorldY(roadNetwork.getBridgeDeckHeight(gx, gz), size);
            px_y = getWorldY(roadNetwork.getBridgeDeckHeight(gx + 1, gz), size);
            nx_y = getWorldY(roadNetwork.getBridgeDeckHeight(gx - 1, gz), size);
            pz_y = getWorldY(roadNetwork.getBridgeDeckHeight(gx, gz + 1), size);
            nz_y = getWorldY(roadNetwork.getBridgeDeckHeight(gx, gz - 1), size);
            isAnchor = (connectionMask != 3 && connectionMask != 12);
        }

        double w = size * 0.35; // match RoadModule half-width
        double topOffset = 0;
        double bottomOffset = -size * DECK_THICKNESS_FRAC;
        double railTopOffset = size * RAIL_HEIGHT_FRAC;
        double railThickness = size * 0.05; // Was RAIL_THICKNESS_FRAC which is not defined here
        
        String yq_dummy = "";

        addYQuad(cx - w, cx + w, cz - w, cz + w, topOffset, true, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        addYQuad(cx - w, cx + w, cz - w, cz + w, bottomOffset, false, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PX)) {
            addYQuad(cx + w, cx + s, cz - w, cz + w, topOffset, true, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx + w, cx + s, cz - w, cz + w, bottomOffset, false, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz + w, bottomOffset, topOffset, 2, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz + w, bottomOffset, topOffset, 3, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            
            addYQuad(cx + w, cx + s, cz - w, cz - w + railThickness, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz - w + railThickness, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz - w + railThickness, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz - w + railThickness, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz - w + railThickness, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

            addYQuad(cx + w, cx + s, cz + w - railThickness, cz + w, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz + w - railThickness, cz + w, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz + w - railThickness, cz + w, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz + w - railThickness, cz + w, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz + w - railThickness, cz + w, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_NX)) {
            addYQuad(cx - s, cx - w, cz - w, cz + w, topOffset, true, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx - s, cx - w, cz - w, cz + w, bottomOffset, false, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz + w, bottomOffset, topOffset, 2, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz + w, bottomOffset, topOffset, 3, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

            addYQuad(cx - s, cx - w, cz - w, cz - w + railThickness, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz - w + railThickness, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz - w + railThickness, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz - w + railThickness, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz - w + railThickness, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

            addYQuad(cx - s, cx - w, cz + w - railThickness, cz + w, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz + w - railThickness, cz + w, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz + w - railThickness, cz + w, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz + w - railThickness, cz + w, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz + w - railThickness, cz + w, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PZ)) {
            addYQuad(cx - w, cx + w, cz + w, cz + s, topOffset, true, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx - w, cx + w, cz + w, cz + s, bottomOffset, false, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz + w, cz + s, bottomOffset, topOffset, 0, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz + w, cz + s, bottomOffset, topOffset, 1, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            
            addYQuad(cx - w, cx - w + railThickness, cz + w, cz + s, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz + w, cz + s, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz + w, cz + s, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz + w, cz + s, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz + w, cz + s, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

            addYQuad(cx + w - railThickness, cx + w, cz + w, cz + s, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz + w, cz + s, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz + w, cz + s, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz + w, cz + s, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz + w, cz + s, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_NZ)) {
            addYQuad(cx - w, cx + w, cz - s, cz - w, topOffset, true, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx - w, cx + w, cz - s, cz - w, bottomOffset, false, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz - s, cz - w, bottomOffset, topOffset, 0, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz - s, cz - w, bottomOffset, topOffset, 1, DECK_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

            addYQuad(cx - w, cx - w + railThickness, cz - s, cz - w, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz - s, cz - w, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz - s, cz - w, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz - s, cz - w, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx - w + railThickness, cz - s, cz - w, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

            addYQuad(cx + w - railThickness, cx + w, cz - s, cz - w, railTopOffset, true, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz - s, cz - w, topOffset, railTopOffset, 0, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz - s, cz - w, topOffset, railTopOffset, 1, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz - s, cz - w, topOffset, railTopOffset, 2, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w - railThickness, cx + w, cz - s, cz - w, topOffset, railTopOffset, 3, RAIL_COLOR, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        
        Color stripeColor = new Color(220, 200, 40);
        double stripeW = size * 0.05;
        double stTop = topOffset + 0.05;

        addYQuad(cx - stripeW, cx + stripeW, cz - stripeW, cz + stripeW, stTop, true, stripeColor, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PX)) {
            addYQuad(cx + stripeW, cx + s, cz - stripeW, cz + stripeW, stTop, true, stripeColor, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_NX)) {
            addYQuad(cx - s, cx - stripeW, cz - stripeW, cz + stripeW, stTop, true, stripeColor, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PZ)) {
            addYQuad(cx - stripeW, cx + stripeW, cz + stripeW, cz + s, stTop, true, stripeColor, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_NZ)) {
            addYQuad(cx - stripeW, cx + stripeW, cz - s, cz - stripeW, stTop, true, stripeColor, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
    }

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


        double pylonW = size * PYLON_WIDTH_FRAC / 2.0;
        double topOffset = -size * DECK_THICKNESS_FRAC;
        double bottomOffset = floorY - cy;
        
        double s = size/2.0;
        String yq = ", verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);";

        addYQuad(cx - pylonW, cx + pylonW, cz - pylonW, cz + pylonW, topOffset, true, PYLON_COLOR, verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);
        addYQuad(cx - pylonW, cx + pylonW, cz - pylonW, cz + pylonW, bottomOffset, false, PYLON_COLOR, verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);
        addSideQuad(cx - pylonW, cx + pylonW, cz - pylonW, cz + pylonW, bottomOffset, topOffset, 0, PYLON_COLOR, verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);
        addSideQuad(cx - pylonW, cx + pylonW, cz - pylonW, cz + pylonW, bottomOffset, topOffset, 1, PYLON_COLOR, verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);
        addSideQuad(cx - pylonW, cx + pylonW, cz - pylonW, cz + pylonW, bottomOffset, topOffset, 2, PYLON_COLOR, verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);
        addSideQuad(cx - pylonW, cx + pylonW, cz - pylonW, cz + pylonW, bottomOffset, topOffset, 3, PYLON_COLOR, verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);
    }

    private void addYQuad(double minX, double maxX, double minZ, double maxZ, double yOffset, boolean isTop, Color c, List<Vertex> verts, List<Data.Face> faces,
                          double cx, double cz, double s, double center_y, double px_y, double nx_y, double pz_y, double nz_y, boolean isAnchor) {
        
        double y00 = getSlopeY(minX, minZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor) + yOffset;
        double y10 = getSlopeY(maxX, minZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor) + yOffset;
        double y11 = getSlopeY(maxX, maxZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor) + yOffset;
        double y01 = getSlopeY(minX, maxZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor) + yOffset;
        
        int idx = verts.size();
        verts.add(new Vertex(minX, y00, minZ)); // 0
        verts.add(new Vertex(maxX, y10, minZ)); // 1
        verts.add(new Vertex(maxX, y11, maxZ)); // 2
        verts.add(new Vertex(minX, y01, maxZ)); // 3
        for (int i = 0; i < 4; i++) setColor(verts.get(idx + i), c);
        
        int[] f = isTop ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};
        
        Data.Edge e1 = new Data.Edge(idx + f[0], idx + f[1]);
        Data.Edge e2 = new Data.Edge(idx + f[1], idx + f[2]);
        Data.Edge e3 = new Data.Edge(idx + f[2], idx + f[3]);
        Data.Edge e4 = new Data.Edge(idx + f[3], idx + f[0]);
        faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, c));
    }

    private void addSideQuad(double minX, double maxX, double minZ, double maxZ, double bottomOffset, double topOffset, int side, Color c, List<Vertex> verts, List<Data.Face> faces,
                             double cx, double cz, double s, double center_y, double px_y, double nx_y, double pz_y, double nz_y, boolean isAnchor) {
        
        double y00 = getSlopeY(minX, minZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        double y10 = getSlopeY(maxX, minZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        double y11 = getSlopeY(maxX, maxZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        double y01 = getSlopeY(minX, maxZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

        int idx = verts.size();
        if (side == 0) {
            verts.add(new Vertex(minX, y01 + bottomOffset, maxZ)); // Bot-Left
            verts.add(new Vertex(minX, y00 + bottomOffset, minZ)); // Bot-Right
            verts.add(new Vertex(minX, y00 + topOffset, minZ));    // Top-Right
            verts.add(new Vertex(minX, y01 + topOffset, maxZ));    // Top-Left
        } else if (side == 1) {
            verts.add(new Vertex(maxX, y10 + bottomOffset, minZ)); // Bot-Left
            verts.add(new Vertex(maxX, y11 + bottomOffset, maxZ)); // Bot-Right
            verts.add(new Vertex(maxX, y11 + topOffset, maxZ));    // Top-Right
            verts.add(new Vertex(maxX, y10 + topOffset, minZ));    // Top-Left
        } else if (side == 2) {
            verts.add(new Vertex(minX, y00 + bottomOffset, minZ)); // Bot-Left
            verts.add(new Vertex(maxX, y10 + bottomOffset, minZ)); // Bot-Right
            verts.add(new Vertex(maxX, y10 + topOffset, minZ));    // Top-Right
            verts.add(new Vertex(minX, y00 + topOffset, minZ));    // Top-Left
        } else if (side == 3) {
            verts.add(new Vertex(maxX, y11 + bottomOffset, maxZ)); // Bot-Left
            verts.add(new Vertex(minX, y01 + bottomOffset, maxZ)); // Bot-Right
            verts.add(new Vertex(minX, y01 + topOffset, maxZ));    // Top-Right
            verts.add(new Vertex(maxX, y11 + topOffset, maxZ));    // Top-Left
        }
        for (int i = 0; i < 4; i++) setColor(verts.get(idx + i), c);
        
        int[] f = { 0, 3, 2, 1 }; // CCW
        
        Data.Edge e1 = new Data.Edge(idx + f[0], idx + f[1]);
        Data.Edge e2 = new Data.Edge(idx + f[1], idx + f[2]);
        Data.Edge e3 = new Data.Edge(idx + f[2], idx + f[3]);
        Data.Edge e4 = new Data.Edge(idx + f[3], idx + f[0]);
        faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, c));
    }
    private static void setColor(Vertex v, Color c) {
        v.r = c.getRed();
        v.g = c.getGreen();
        v.b = c.getBlue();
    }
}
