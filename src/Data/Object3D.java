package Data;

public class Object3D {
    public Vertex[] vertices;
    public Face[] faces;
    public boolean isTerrain = false;
    
    public double posX = 0;
    public double posY = 0;
    public double posZ = 0;
    
    public double rotX = 0;
    public double rotY = 0;
    public double rotZ = 0;

    public double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
    public double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

    public Object3D(Vertex[] vertices, Face[] faces) {
        this.vertices = vertices;
        this.faces = faces;
        
        for (Vertex v : vertices) {
            if (v.x < minX) minX = v.x;
            if (v.x > maxX) maxX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.y > maxY) maxY = v.y;
            if (v.z < minZ) minZ = v.z;
            if (v.z > maxZ) maxZ = v.z;
        }
    }

    public void computeNormals() {
        for (Vertex v : vertices) {
            v.nx = 0; v.ny = 0; v.nz = 0;
        }
        for (Face f : faces) {
            if (f.edges.length >= 3) {
                Vertex v0 = vertices[f.edges[0].v1];
                Vertex v1 = vertices[f.edges[0].v2];
                Vertex v2 = vertices[f.edges[1].v2];
                double ux = v1.x - v0.x;
                double uy = v1.y - v0.y;
                double uz = v1.z - v0.z;
                double vx = v2.x - v0.x;
                double vy = v2.y - v0.y;
                double vz = v2.z - v0.z;
                double nx = uy * vz - uz * vy;
                double ny = uz * vx - ux * vz;
                double nz = ux * vy - uy * vx;
                
                for (Edge e : f.edges) {
                    vertices[e.v1].nx += nx;
                    vertices[e.v1].ny += ny;
                    vertices[e.v1].nz += nz;
                }
            }
        }
        for (Vertex v : vertices) {
            double len = Math.sqrt(v.nx * v.nx + v.ny * v.ny + v.nz * v.nz);
            if (len > 0) {
                v.nx /= len;
                v.ny /= len;
                v.nz /= len;
            } else {
                // Default to pointing up if degenerate
                v.ny = 1.0;
            }
        }
    }
}
