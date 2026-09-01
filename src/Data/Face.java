package Data;

import java.awt.Color;

public class Face {
    public Edge[] edges;
    public Color color;

    public Face(Edge[] edges, Color color) {
        this.edges = edges;
        this.color = color;
    }
}
