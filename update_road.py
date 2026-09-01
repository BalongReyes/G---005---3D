import re

with open('src/Objects/WFC/RoadModule.java', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add roadNetwork field and constructor changes
content = content.replace(
    'private int connectionMask;',
    'private int connectionMask;\n    private Core.WFC.RoadNetwork roadNetwork;'
)

content = content.replace(
    'public RoadModule(int connectionMask) {',
    'public RoadModule(int connectionMask, Core.WFC.RoadNetwork roadNetwork) {\n        this.roadNetwork = roadNetwork;'
)
content = content.replace(
    'public RoadModule(int connectionMask) {\n        super',
    'public RoadModule(int connectionMask, Core.WFC.RoadNetwork roadNetwork) {\n        super'
)

# 2. Add slope logic
slope_methods = '''
    private double getWorldY(double gridY, double size) {
        double startY = -((Core.Settings.WorldSettings.GRID_SIZE_Y - 1) * size) / 2.0;
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
'''

content = content.replace('    public boolean modifyTerrain', slope_methods + '\n    public boolean modifyTerrain')

# 3. Update addYQuad and addSideQuad signatures and bodies
quad_logic = '''
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
'''

start_idx = content.find('    private void addYQuad')
end_idx = content.find('    private static void setColor')

content = content[:start_idx] + quad_logic + content[end_idx:]

# 4. Modify generateMesh body to pass args
generate_mesh_start = content.find('    public void generateMesh(double cx, double cy, double cz, double size,')
generate_mesh_start = content.find('        double s = size / 2.0;', generate_mesh_start)

replacement = '''
        double s = size / 2.0;
        double startX = -((Core.Settings.WorldSettings.GRID_SIZE_X - 1) * size) / 2.0;
        double startZ = -((Core.Settings.WorldSettings.GRID_SIZE_Z - 1) * size) / 2.0;
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
'''

content = content[:generate_mesh_start] + replacement + content[generate_mesh_start + 200:]

# replace method calls inside generateMesh manually with regex or simpler string operations
def repl_yquad(match):
    args = match.group(1).rsplit(',', 3)
    c = args[0].strip()
    return f"addYQuad({match.group(1).rsplit(',', 4)[0]}, {c}, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);"

def repl_sidequad(match):
    args = match.group(1).rsplit(',', 3)
    c = args[0].strip()
    return f"addSideQuad({match.group(1).rsplit(',', 4)[0]}, {c}, verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);"

# Just rewrite the generate mesh body entirely, it's easier and safer
generate_mesh_logic = replacement + '''
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
'''

content = content[:generate_mesh_start] + generate_mesh_logic + '\n' + quad_logic + content[end_idx:]

with open('src/Objects/WFC/RoadModule.java', 'w', encoding='utf-8') as f:
    f.write(content)
