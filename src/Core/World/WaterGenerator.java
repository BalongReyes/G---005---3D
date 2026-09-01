package Core.World;

public class WaterGenerator {
    
    public static Data.Object3D generateWater() {
        int originalSizeX = Settings.WorldSettings.GRID_SIZE_X;
        int originalSizeZ = Settings.WorldSettings.GRID_SIZE_Z;
        double originalSpacing = Settings.WorldSettings.SPACING;
        double yLevel = Settings.WorldSettings.WATER_LEVEL;
        
        double mapWidth = (originalSizeX - 1) * originalSpacing;
        double mapDepth = (originalSizeZ - 1) * originalSpacing;
        
        // Coarse grid for water to prevent off-screen floating point precision loss
        // without burning 150,000 faces!
        int waterGridSize = 20; 
        double spacingX = mapWidth / (waterGridSize - 1);
        double spacingZ = mapDepth / (waterGridSize - 1);
        
        Data.Vertex[] verts = new Data.Vertex[waterGridSize * waterGridSize];
        
        double startX = -mapWidth / 2.0;
        double startZ = -mapDepth / 2.0;
        
        int vIndex = 0;
        for (int z = 0; z < waterGridSize; z++) {
            for (int x = 0; x < waterGridSize; x++) {
                Data.Vertex v = new Data.Vertex(startX + x * spacingX, yLevel, startZ + z * spacingZ);
                v.nx = 0.0;
                v.ny = 1.0; // Water points straight up
                v.nz = 0.0;
                verts[vIndex++] = v;
            }
        }
        
        java.awt.Color waterColor = new java.awt.Color(0, 100, 255, 180);
        int numFaces = (waterGridSize - 1) * (waterGridSize - 1) * 4;
        Data.Face[] faces = new Data.Face[numFaces];
        
        int fIndex = 0;
        for (int z = 0; z < waterGridSize - 1; z++) {
            for (int x = 0; x < waterGridSize - 1; x++) {
                int i0 = x + z * waterGridSize;
                int i1 = (x + 1) + z * waterGridSize;
                int i2 = (x + 1) + (z + 1) * waterGridSize;
                int i3 = x + (z + 1) * waterGridSize;
                
                // First Triangle (i0, i1, i2)
                Data.Edge e1 = new Data.Edge(i0, i1);
                Data.Edge e2 = new Data.Edge(i1, i2);
                Data.Edge e3 = new Data.Edge(i2, i0);
                faces[fIndex++] = new Data.Face(new Data.Edge[]{e1, e2, e3}, waterColor);
                
                // First Triangle REVERSED (i0, i2, i1) -> Points opposite direction
                Data.Edge e1_rev = new Data.Edge(i0, i2);
                Data.Edge e2_rev = new Data.Edge(i2, i1);
                Data.Edge e3_rev = new Data.Edge(i1, i0);
                faces[fIndex++] = new Data.Face(new Data.Edge[]{e1_rev, e2_rev, e3_rev}, waterColor);
                
                // Second Triangle (i0, i2, i3)
                Data.Edge e4 = new Data.Edge(i0, i2);
                Data.Edge e5 = new Data.Edge(i2, i3);
                Data.Edge e6 = new Data.Edge(i3, i0);
                faces[fIndex++] = new Data.Face(new Data.Edge[]{e4, e5, e6}, waterColor);
                
                // Second Triangle REVERSED (i0, i3, i2)
                Data.Edge e4_rev = new Data.Edge(i0, i3);
                Data.Edge e5_rev = new Data.Edge(i3, i2);
                Data.Edge e6_rev = new Data.Edge(i2, i0);
                faces[fIndex++] = new Data.Face(new Data.Edge[]{e4_rev, e5_rev, e6_rev}, waterColor);
            }
        }
        
        return new Data.Object3D(verts, faces);
    }
}
