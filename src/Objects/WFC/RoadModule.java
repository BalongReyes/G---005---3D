package Objects.WFC;

import java.awt.Color;
import java.util.List;

import Core.WFC.RoadNetwork;
import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;

// A single flat road-deck tile. One instance per connectivity shape
// (straight, corner, dead-end, junction, ...) - WFCGenerator.initChunk()
// forces the exact matching instance onto each planned road column instead
// of letting the solver choose freely, since the route is a global decision
// made by RoadNetwork, not something local socket-matching can discover.
public class RoadModule extends WFCModule {

    private final int connectionMask;
    private final Core.WFC.RoadNetwork roadNetwork;
    private static final double DECK_THICKNESS = 0.12; // slab thickness so it reads as a proper road deck

    public RoadModule(int connectionMask, Core.WFC.RoadNetwork roadNetwork) {
        super(SocketType.ROAD, "Road_" + connectionMask,
                buildProvided(connectionMask),
                buildAccepted(connectionMask),
                new Color(60, 60, 65), 1.0);
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
                SocketType.WILDCARD, // Up - Provide wildcard so SolidModule can sit on top as a tunnel
                SocketType.SOLID, // Down - sits on flattened solid ground
                has(mask, RoadNetwork.DIR_PZ) ? SocketType.ROAD : SocketType.WILDCARD,
                has(mask, RoadNetwork.DIR_NZ) ? SocketType.ROAD : SocketType.WILDCARD
        };
    }

    private static SocketType[][] buildAccepted(int mask) {
        return new SocketType[][] {
                { has(mask, RoadNetwork.DIR_PX) ? SocketType.ROAD : SocketType.WILDCARD },
                { has(mask, RoadNetwork.DIR_NX) ? SocketType.ROAD : SocketType.WILDCARD },
                { SocketType.AIR, SocketType.SOLID, SocketType.KELP }, // Up - Allow solid/kelp above for tunnels/underwater roads
                { SocketType.SOLID },
                { has(mask, RoadNetwork.DIR_PZ) ? SocketType.ROAD : SocketType.WILDCARD },
                { has(mask, RoadNetwork.DIR_NZ) ? SocketType.ROAD : SocketType.WILDCARD }
        };
    }

    private static boolean has(int mask, int dir) {
        return (mask & dir) != 0;
    }

    // The actual terrain-carving side effect - this is what makes the
    // ground under the road flat instead of following the marching-cubes
    // surface. action=2 is Flatten in TerrainGenerator.modifyTerrain: solid
    // below gy, empty above.
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

    public boolean modifyTerrain(Core.World.ScalarField field, int gx, int gy, int gz) {
        return false;
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces, Core.World.ScalarField grid, double isoLevel) {
        generateMesh(cx, cy, cz, size, verts, faces);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
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
            center_y = getWorldY(roadNetwork.getRoadHeight(gx, gz), size);
            px_y = getWorldY(roadNetwork.getRoadHeight(gx + 1, gz), size);
            nx_y = getWorldY(roadNetwork.getRoadHeight(gx - 1, gz), size);
            pz_y = getWorldY(roadNetwork.getRoadHeight(gx, gz + 1), size);
            nz_y = getWorldY(roadNetwork.getRoadHeight(gx, gz - 1), size);
            isAnchor = (connectionMask != 3 && connectionMask != 12);
        }

        double w = size * 0.35; // road half-width
        double bottomOffset = -DECK_THICKNESS / 2.0;
        double topOffset = DECK_THICKNESS / 2.0;
        
        String yq_dummy = ""; // just for replacement string hack to look clean

        // --- ASPHALT DECK ---
        addYQuad(cx - w, cx + w, cz - w, cz + w, topOffset, true, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        addYQuad(cx - w, cx + w, cz - w, cz + w, bottomOffset, false, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

        if (!has(connectionMask, Core.WFC.RoadNetwork.DIR_NX)) addSideQuad(cx - w, cx + w, cz - w, cz + w, bottomOffset, topOffset, 0, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        if (!has(connectionMask, Core.WFC.RoadNetwork.DIR_PX)) addSideQuad(cx - w, cx + w, cz - w, cz + w, bottomOffset, topOffset, 1, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        if (!has(connectionMask, Core.WFC.RoadNetwork.DIR_NZ)) addSideQuad(cx - w, cx + w, cz - w, cz + w, bottomOffset, topOffset, 2, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        if (!has(connectionMask, Core.WFC.RoadNetwork.DIR_PZ)) addSideQuad(cx - w, cx + w, cz - w, cz + w, bottomOffset, topOffset, 3, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);

        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PX)) {
            addYQuad(cx + w, cx + s, cz - w, cz + w, topOffset, true, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx + w, cx + s, cz - w, cz + w, bottomOffset, false, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz + w, bottomOffset, topOffset, 2, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx + w, cx + s, cz - w, cz + w, bottomOffset, topOffset, 3, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_NX)) {
            addYQuad(cx - s, cx - w, cz - w, cz + w, topOffset, true, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx - s, cx - w, cz - w, cz + w, bottomOffset, false, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz + w, bottomOffset, topOffset, 2, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - s, cx - w, cz - w, cz + w, bottomOffset, topOffset, 3, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_PZ)) {
            addYQuad(cx - w, cx + w, cz + w, cz + s, topOffset, true, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx - w, cx + w, cz + w, cz + s, bottomOffset, false, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz + w, cz + s, bottomOffset, topOffset, 0, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz + w, cz + s, bottomOffset, topOffset, 1, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }
        if (has(connectionMask, Core.WFC.RoadNetwork.DIR_NZ)) {
            addYQuad(cx - w, cx + w, cz - s, cz - w, topOffset, true, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addYQuad(cx - w, cx + w, cz - s, cz - w, bottomOffset, false, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz - s, cz - w, bottomOffset, topOffset, 0, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
            addSideQuad(cx - w, cx + w, cz - s, cz - w, bottomOffset, topOffset, 1, color, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);
        }

        // --- STRIPES ---
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
