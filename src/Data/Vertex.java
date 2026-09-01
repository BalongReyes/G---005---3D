package Data;

public class Vertex {
    public double x;
    public double y;
    public double z;
    public double w;
    
    // Normal Vector
    public double nx = 0, ny = 0, nz = 0;
    
    // Vertex Color
    public int r = 255, g = 255, b = 255;

    public Vertex(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = 1.0;
    }

    public Vertex(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}
