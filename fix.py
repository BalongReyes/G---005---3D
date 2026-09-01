import os

with open('src/Objects/WFC/RoadModule.java', 'r', encoding='utf-8') as f:
    r = f.read()
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
