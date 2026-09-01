package Core.WFC;

public class WFCModule {
    public SocketType category;
    public String name;
    
    // The socket ID for each of the 6 faces.
    // 0: Right (+X)
    // 1: Left (-X)
    // 2: Up (+Y)
    // 3: Down (-Y)
    // 4: Forward (+Z)
    // 5: Back (-Z)
    public SocketType[] providedSockets;
    public SocketType[][] acceptedSockets;
    
    // Optional: Visual color for the placeholder block
    public java.awt.Color color;
    
    // Weight for random selection (higher = more common)
    public double weight = 1.0;

    public WFCModule(SocketType category, String name, SocketType[] providedSockets, SocketType[][] acceptedSockets, java.awt.Color color, double weight) {
        this.category = category;
        this.name = name;
        this.providedSockets = providedSockets;
        this.acceptedSockets = acceptedSockets;
        this.color = color;
        this.weight = weight;
    }

    // Checks if this module can be placed next to another module in a specific direction.
    public boolean canConnect(int direction, WFCModule other) {
        int oppositeDirection = getOppositeDirection(direction);
        SocketType myProvided = this.providedSockets[direction];
        SocketType otherProvided = other.providedSockets[oppositeDirection];
        
        // 1. Check if the other module accepts what we provide
        boolean otherAcceptsUs = false;
        for (SocketType accepted : other.acceptedSockets[oppositeDirection]) {
            if (accepted == SocketType.WILDCARD || myProvided == SocketType.WILDCARD || accepted == myProvided) {
                otherAcceptsUs = true;
                break;
            }
        }
        
        // 2. Check if we accept what the other module provides
        boolean weAcceptOther = false;
        for (SocketType accepted : this.acceptedSockets[direction]) {
            if (accepted == SocketType.WILDCARD || otherProvided == SocketType.WILDCARD || accepted == otherProvided) {
                weAcceptOther = true;
                break;
            }
        }
        
        return otherAcceptsUs && weAcceptOther;
    }

    public static int getOppositeDirection(int dir) {
        switch (dir) {
            case 0: return 1; // +X -> -X
            case 1: return 0; // -X -> +X
            case 2: return 3; // +Y -> -Y
            case 3: return 2; // -Y -> +Y
            case 4: return 5; // +Z -> -Z
            case 5: return 4; // -Z -> +Z
            default: return -1;
        }
    }
    
    // Allows specific modules (like Roads or Bridges) to carve or flatten the actual terrain!
    // Returns true if the terrain was modified, so the generator knows to rebuild the mesh.
    public boolean modifyTerrain(Core.World.ScalarField field, int gx, int gy, int gz) {
        return false; // Default does nothing!
    }
    
    // Default implementation: Generates a solid colored cube if color != null
    public void generateMesh(double cx, double cy, double cz, double size, 
                             java.util.List<Data.Vertex> verts, java.util.List<Data.Face> faces) {
        if (this.color == null) return;
        
        int vOffset = verts.size();
        double s = size / 2.0;
        
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        Data.Vertex[] newVerts = new Data.Vertex[]{
            new Data.Vertex(cx - s, cy, cz - s),
            new Data.Vertex(cx + s, cy, cz - s),
            new Data.Vertex(cx + s, cy, cz + s),
            new Data.Vertex(cx - s, cy, cz + s),
            new Data.Vertex(cx - s, cy + size, cz - s),
            new Data.Vertex(cx + s, cy + size, cz - s),
            new Data.Vertex(cx + s, cy + size, cz + s),
            new Data.Vertex(cx - s, cy + size, cz + s)
        };
        
        for (Data.Vertex v : newVerts) {
            v.r = r;
            v.g = g;
            v.b = b;
            verts.add(v);
        }
        
        int[][] cubeFaces = {
            {0, 1, 2, 3}, // Bottom
            {4, 5, 6, 7}, // Top
            {0, 1, 5, 4}, // Back
            {1, 2, 6, 5}, // Right
            {2, 3, 7, 6}, // Front
            {3, 0, 4, 7}  // Left
        };
        
        for (int[] f : cubeFaces) {
            Data.Edge e1 = new Data.Edge(vOffset + f[0], vOffset + f[1]);
            Data.Edge e2 = new Data.Edge(vOffset + f[1], vOffset + f[2]);
            Data.Edge e3 = new Data.Edge(vOffset + f[2], vOffset + f[0]);
            faces.add(new Data.Face(new Data.Edge[]{e1, e2, e3}, color));
            
            Data.Edge e4 = new Data.Edge(vOffset + f[2], vOffset + f[3]);
            Data.Edge e5 = new Data.Edge(vOffset + f[3], vOffset + f[0]);
            Data.Edge e6 = new Data.Edge(vOffset + f[0], vOffset + f[2]);
            faces.add(new Data.Face(new Data.Edge[]{e4, e5, e6}, color));
        }
    }

    // Overloaded method that passes the scalar field and isoLevel for terrain snapping.
    // By default, it just calls the standard generateMesh, keeping older modules perfectly compatible.
    public void generateMesh(double cx, double cy, double cz, double size, 
                             java.util.List<Data.Vertex> verts, java.util.List<Data.Face> faces,
                             Core.World.ScalarField grid, double isoLevel) {
        generateMesh(cx, cy, cz, size, verts, faces);
    }
}
