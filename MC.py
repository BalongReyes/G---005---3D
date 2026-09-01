DIR_PX = 1
DIR_NX = 2
DIR_PZ = 4
DIR_NZ = 8

def getDir(fgx, fgz, tgx, tgz):
    if tgx > fgx: return DIR_PX
    if tgx < fgx: return DIR_NX
    if tgz > fgz: return DIR_PZ
    if tgz < fgz: return DIR_NZ
    return 0

path = [
    (0, 0),
    (1, 0),
    (1, 1),
    (2, 1),
    (2, 2)
]

for i in range(len(path)):
    mask = 0
    if i > 0:
        mask |= getDir(path[i][0], path[i][1], path[i-1][0], path[i-1][1])
    if i < len(path) - 1:
        mask |= getDir(path[i][0], path[i][1], path[i+1][0], path[i+1][1])
    
    arms = []
    if mask & DIR_PX: arms.append("+X")
    if mask & DIR_NX: arms.append("-X")
    if mask & DIR_PZ: arms.append("+Z")
    if mask & DIR_NZ: arms.append("-Z")
    print(f"Cell {path[i][0]},{path[i][1]} -> Mask {mask} Arms: {' '.join(arms)}")
