package Core.WFC;

import java.util.*;

// Plans a road route across the whole terrain (not per-chunk) using A* over
// grid columns (gx, gz), weighted heavily against elevation change so the
// route naturally curves around mountains instead of climbing them. Must run
// BEFORE any WFC chunk solving starts, since a road is a global constraint
// that WFCGenerator's local per-chunk random walk has no way to discover on
// its own. WFCGenerator.initChunk() consults this to force road cells at the
// planned height/connectivity instead of leaving them to random choice.
public class RoadNetwork {

    public static final int DIR_PX = 1; // +X
    public static final int DIR_NX = 2; // -X
    public static final int DIR_PZ = 4; // +Z
    public static final int DIR_NZ = 8; // -Z

    // How strongly elevation change is penalized per unit of slope between
    // adjacent columns. High relative to the flat per-step cost, so the
    // path detours a long way around a cliff rather than crossing it.
    private static final double SLOPE_PENALTY = 25.0;
    private static final double STEP_COST = 1.0;

    // How far above the water level (grid-Y cells) a bridge deck must
    // clear, so the deck never dips down to touch the water it's crossing.
    private static final int BRIDGE_CLEARANCE = 3;

    // Packed (gx,gz) -> connection bitmask (which of the 4 lateral
    // neighbors are also road).
    private final Map<Long, Integer> connections = new HashMap<>();
    // Packed (gx,gz) -> the grid-Y the road sits at (its natural surface
    // height at that column, since the terrain gets flattened per-column
    // rather than smoothed across the whole route).
    private final Map<Long, Integer> roadHeight = new HashMap<>();
    
    private final Set<Long> bridgeColumns = new HashSet<>();
    private final Map<Long, Integer> bridgeDeckHeight = new HashMap<>();

    private static long key(int gx, int gz) {
        return (((long) gx) << 32) ^ (gz & 0xffffffffL);
    }

    public boolean isRoad(int gx, int gz) {
        return connections.containsKey(key(gx, gz));
    }
    
    public boolean isBridge(int gx, int gz) { 
        return bridgeColumns.contains(key(gx, gz)); 
    }

    public int getConnections(int gx, int gz) {
        return connections.getOrDefault(key(gx, gz), 0);
    }

    public int getRoadHeight(int gx, int gz) {
        return roadHeight.getOrDefault(key(gx, gz), 0);
    }

    public int getBridgeDeckHeight(int gx, int gz) { 
        return bridgeDeckHeight.getOrDefault(key(gx, gz), 0); 
    }

    // heightMap: (gx,gz) -> surface grid-Y, built once globally from
    // main.surfaceGridCells (see WFCGenerator integration below).
    // waterLevelGridY: WATER_LEVEL converted to grid-Y
    public void planPath(Map<Long, Integer> heightMap, int startGx, int startGz, int endGx, int endGz, int waterLevelGridY) {
        List<long[]> path = aStar(heightMap, startGx, startGz, endGx, endGz);
        if (path == null) {
            System.out.println("RoadNetwork: no path found from (" + startGx + "," + startGz
                    + ") to (" + endGx + "," + endGz + ")");
            return;
        }
        registerPath(heightMap, path, waterLevelGridY);
    }

    private List<long[]> aStar(Map<Long, Integer> heightMap, int startGx, int startGz, int endGx, int endGz) {
        long start = key(startGx, startGz);
        long goal = key(endGx, endGz);

        Map<Long, Long> cameFrom = new HashMap<>();
        Map<Long, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);

        PriorityQueue<Long> open = new PriorityQueue<>(
                Comparator.comparingDouble(n -> gScore.getOrDefault(n, Double.MAX_VALUE)
                        + manhattan(n, goal)));
        open.add(start);
        Set<Long> visited = new HashSet<>();

        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!open.isEmpty()) {
            long current = open.poll();
            if (current == goal) return reconstruct(cameFrom, current);
            if (!visited.add(current)) continue;

            int cgx = (int) (current >> 32);
            int cgz = (int) current;
            Integer curHeightBoxed = heightMap.get(current);
            if (curHeightBoxed == null) continue;
            int curHeight = curHeightBoxed;

            for (int[] d : dirs) {
                int ngx = cgx + d[0], ngz = cgz + d[1];
                long next = key(ngx, ngz);
                Integer nextHeightBoxed = heightMap.get(next);
                if (nextHeightBoxed == null) continue; // outside generated terrain

                double slope = Math.abs(nextHeightBoxed - curHeight);
                double cost = STEP_COST + slope * SLOPE_PENALTY;
                double tentativeG = gScore.getOrDefault(current, Double.MAX_VALUE) + cost;

                if (tentativeG < gScore.getOrDefault(next, Double.MAX_VALUE)) {
                    cameFrom.put(next, current);
                    gScore.put(next, tentativeG);
                    open.add(next);
                }
            }
        }
        return null;
    }

    private double manhattan(long a, long b) {
        int agx = (int) (a >> 32), agz = (int) a;
        int bgx = (int) (b >> 32), bgz = (int) b;
        return Math.abs(agx - bgx) + Math.abs(agz - bgz);
    }

    private List<long[]> reconstruct(Map<Long, Long> cameFrom, long current) {
        List<long[]> path = new ArrayList<>();
        while (current != 0 || cameFrom.containsKey(current)) {
            path.add(new long[] { current });
            Long prev = cameFrom.get(current);
            if (prev == null) break;
            current = prev;
        }
        Collections.reverse(path);
        return path;
    }

    private void registerPath(Map<Long, Integer> heightMap, List<long[]> path, int waterLevelGridY) {
        int n = path.size();
        int[] naturalHeight = new int[n];

        for (int i = 0; i < n; i++) {
            long here = path.get(i)[0];
            naturalHeight[i] = heightMap.getOrDefault(here, 0);

            int mask = 0;
            if (i > 0) mask |= directionTo(path.get(i - 1)[0], here);
            if (i < n - 1) mask |= directionTo(here, path.get(i + 1)[0]);
            connections.put(here, mask);
        }

        int i = 0;
        while (i < n) {
            if (naturalHeight[i] < waterLevelGridY) {
                int gapStart = i;
                int gapEnd = i;
                while (gapEnd + 1 < n && naturalHeight[gapEnd + 1] < waterLevelGridY) gapEnd++;
                registerBridgeSpan(path, naturalHeight, gapStart, gapEnd, waterLevelGridY);
                i = gapEnd + 1;
            } else {
                roadHeight.put(path.get(i)[0], naturalHeight[i]);
                i++;
            }
        }
    }

    private void registerBridgeSpan(List<long[]> path, int[] naturalHeight,
            int gapStart, int gapEnd, int waterLevelGridY) {

        int minDeck = waterLevelGridY + BRIDGE_CLEARANCE;
        int beforeHeight = gapStart > 0 ? naturalHeight[gapStart - 1] : minDeck;
        int afterHeight = gapEnd < naturalHeight.length - 1 ? naturalHeight[gapEnd + 1] : minDeck;

        int span = gapEnd - gapStart + 1;
        for (int i = gapStart; i <= gapEnd; i++) {
            double t = span == 1 ? 0.5 : (double) (i - gapStart) / (span - 1);
            int interpolated = (int) Math.round(beforeHeight + (afterHeight - beforeHeight) * t);
            int deckHeight = Math.max(interpolated, minDeck);

            long here = path.get(i)[0];
            bridgeColumns.add(here);
            bridgeDeckHeight.put(here, deckHeight);
        }
    }

    private int directionTo(long from, long to) {
        int fgx = (int) (from >> 32), fgz = (int) from;
        int tgx = (int) (to >> 32), tgz = (int) to;
        if (tgx > fgx) return DIR_PX;
        if (tgx < fgx) return DIR_NX;
        if (tgz > fgz) return DIR_PZ;
        if (tgz < fgz) return DIR_NZ;
        return 0;
    }
}
