import os

with open('src/Objects/WFC/RoadModule.java', 'r', encoding='utf-8') as f:
    r = f.read()
    
# Clean up duplicate addSideQuad arg block in RoadModule
r = r.replace('    }\ndouble nx_y, double pz_y, double nz_y, boolean isAnchor) {\n        \n        double y00 = getSlopeY(minX, minZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);\n        double y10 = getSlopeY(maxX, minZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);\n        double y11 = getSlopeY(maxX, maxZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);\n        double y01 = getSlopeY(minX, maxZ, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);\n\n        int idx = verts.size();\n        if (side == 0) {\n            verts.add(new Vertex(minX, y01 + bottomOffset, maxZ)); // Bot-Left\n            verts.add(new Vertex(minX, y00 + bottomOffset, minZ)); // Bot-Right\n            verts.add(new Vertex(minX, y00 + topOffset, minZ));    // Top-Right\n            verts.add(new Vertex(minX, y01 + topOffset, maxZ));    // Top-Left\n        } else if (side == 1) {\n            verts.add(new Vertex(maxX, y10 + bottomOffset, minZ)); // Bot-Left\n            verts.add(new Vertex(maxX, y11 + bottomOffset, maxZ)); // Bot-Right\n            verts.add(new Vertex(maxX, y11 + topOffset, maxZ));    // Top-Right\n            verts.add(new Vertex(maxX, y10 + topOffset, minZ));    // Top-Left\n        } else if (side == 2) {\n            verts.add(new Vertex(minX, y00 + bottomOffset, minZ)); // Bot-Left\n            verts.add(new Vertex(maxX, y10 + bottomOffset, minZ)); // Bot-Right\n            verts.add(new Vertex(maxX, y10 + topOffset, minZ));    // Top-Right\n            verts.add(new Vertex(minX, y00 + topOffset, minZ));    // Top-Left\n        } else if (side == 3) {\n            verts.add(new Vertex(maxX, y11 + bottomOffset, maxZ)); // Bot-Left\n            verts.add(new Vertex(minX, y01 + bottomOffset, maxZ)); // Bot-Right\n            verts.add(new Vertex(minX, y01 + topOffset, maxZ));    // Top-Right\n            verts.add(new Vertex(maxX, y11 + topOffset, maxZ));    // Top-Left\n        }\n        for (int i = 0; i < 4; i++) setColor(verts.get(idx + i), c);\n        \n        int[] f = { 0, 3, 2, 1 }; // CCW\n        \n        Data.Edge e1 = new Data.Edge(idx + f[0], idx + f[1]);\n        Data.Edge e2 = new Data.Edge(idx + f[1], idx + f[2]);\n        Data.Edge e3 = new Data.Edge(idx + f[2], idx + f[3]);\n        Data.Edge e4 = new Data.Edge(idx + f[3], idx + f[0]);\n        faces.add(new Data.Face(new Data.Edge[] { e1, e2, e3, e4 }, c));\n    }\n', '    }\n')
    
r = r.replace(' + yq', ', verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);')
with open('src/Objects/WFC/RoadModule.java', 'w', encoding='utf-8') as f:
    f.write(r)

with open('src/Objects/WFC/BridgeModule.java', 'r', encoding='utf-8') as f:
    r = f.read()
    pylon_start = r.find('private void addPylon')
    r1 = r[:pylon_start].replace(' + yq', ', verts, faces, cx, cz, s, center_y, px_y, nx_y, pz_y, nz_y, isAnchor);')
    r2 = r[pylon_start:].replace(' + yq', ', verts, faces, cx, cz, s, cy, cy, cy, cy, cy, true);')
    r = r1 + r2
with open('src/Objects/WFC/BridgeModule.java', 'w', encoding='utf-8') as f:
    f.write(r)
