package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Core.WFC.RoadNetwork;
import Data.Vertex;
import java.awt.Color;
import java.util.List;

// A single flat road-deck tile. One instance per connectivity shape
// (straight, corner, dead-end, junction, ...) - WFCGenerator.initChunk()
// forces the exact matching instance onto each planned road column instead
// of letting the solver choose freely, since the route is a global decision
// made by RoadNetwork, not something local socket-matching can discover.
public class RoadModule extends WFCModule {

    private final int connectionMask;
    private static final double DECK_HEIGHT = 0.05; // slight raise above flattened ground

    public RoadModule(int connectionMask) {
        super(SocketType.ROAD, "Road_" + connectionMask,
                buildProvided(connectionMask),
                buildAccepted(connectionMask),
                new Color(60, 60, 65), 1.0);
        this.connectionMask = connectionMask;
    }

    public int getConnectionMask() {
        return connectionMask;
    }

    private static SocketType[] buildProvided(int mask) {
        return new SocketType[] {
                has(mask, RoadNetwork.DIR_PX) ? SocketType.ROAD : SocketType.WILDCARD,
                has(mask, RoadNetwork.DIR_NX) ? SocketType.ROAD : SocketType.WILDCARD,
                SocketType.AIR,   // Up - nothing stacks on a road
                SocketType.SOLID, // Down - sits on flattened solid ground
                has(mask, RoadNetwork.DIR_PZ) ? SocketType.ROAD : SocketType.WILDCARD,
                has(mask, RoadNetwork.DIR_NZ) ? SocketType.ROAD : SocketType.WILDCARD
        };
    }

    private static SocketType[][] buildAccepted(int mask) {
        return new SocketType[][] {
                { has(mask, RoadNetwork.DIR_PX) ? SocketType.ROAD : SocketType.WILDCARD },
                { has(mask, RoadNetwork.DIR_NX) ? SocketType.ROAD : SocketType.WILDCARD },
                { SocketType.AIR },
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
    @Override
    public boolean modifyTerrain(Core.World.ScalarField field, int gx, int gy, int gz) {
        Core.World.TerrainGenerator.modifyTerrain(field, gx, gy, gz, 0, 2);
        return true;
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
        double deckY = cy + DECK_HEIGHT;

        int idx = verts.size();
        Vertex v0 = new Vertex(cx - s, deckY, cz - s);
        Vertex v1 = new Vertex(cx + s, deckY, cz - s);
        Vertex v2 = new Vertex(cx + s, deckY, cz + s);
        Vertex v3 = new Vertex(cx - s, deckY, cz + s);
        for (Vertex v : new Vertex[] { v0, v1, v2, v3 }) {
            setColor(v, color);
            verts.add(v);
        }

        Data.Edge e1 = new Data.Edge(idx, idx + 3);
        Data.Edge e2 = new Data.Edge(idx + 3, idx + 2);
        Data.Edge e3 = new Data.Edge(idx + 2, idx + 1);
        Data.Edge e4 = new Data.Edge(idx + 1, idx);
        faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, color));
    }

    private static void setColor(Vertex v, Color c) {
        v.r = c.getRed();
        v.g = c.getGreen();
        v.b = c.getBlue();
    }
}
