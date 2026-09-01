package Core.WFC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Objects;

public class RoadNetwork {

    public static final int DIR_PX = 1; // +X
    public static final int DIR_NX = 2; // -X
    public static final int DIR_PZ = 4; // +Z
    public static final int DIR_NZ = 8; // -Z

    private static final double STEP_COST = 1.0;
    private static final double SLOPE_PENALTY = 25.0; // Heavily penalize going up/down to force flat roads (tunnels)
    private static final double TRENCH_PENALTY = 0.5; // Very cheap to dig into the ground (carve a tunnel)
    private static final double BRIDGE_PENALTY = 10.0; // Expensive to build bridges (prefer flat ground)

    private static final int BRIDGE_CLEARANCE = 3;

    private final Map<Long, Integer> connections = new HashMap<>();
    private final Map<Long, Double> roadHeight = new HashMap<>();
    
    private final Set<Long> bridgeColumns = new HashSet<>();
    private final Map<Long, Double> bridgeDeckHeight = new HashMap<>();

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

    public double getRoadHeight(int gx, int gz) {
        return roadHeight.getOrDefault(key(gx, gz), 0.0);
    }
    
    public double getBridgeDeckHeight(int gx, int gz) {
        return bridgeDeckHeight.getOrDefault(key(gx, gz), 0.0);
    }
    
    public static class Node {
        public final int gx;
        public final int gy;
        public final int gz;

        public Node(int gx, int gy, int gz) {
            this.gx = gx;
            this.gy = gy;
            this.gz = gz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return gx == node.gx && gz == node.gz;
        }

        @Override
        public int hashCode() {
            return Objects.hash(gx, gz);
        }
    }

    public void planPath(Map<Long, Integer> heightMap, int startGx, int startGz, int endGx, int endGz, int waterLevelGridY) {
        connections.clear();
        roadHeight.clear();
        bridgeColumns.clear();
        bridgeDeckHeight.clear();

        List<Node> path = aStar(heightMap, startGx, startGz, endGx, endGz);
        if (path == null) {
            System.out.println("RoadNetwork: no path found from (" + startGx + "," + startGz
                    + ") to (" + endGx + "," + endGz + ")");
            return;
        }
        registerPath(heightMap, path, waterLevelGridY);
    }

    private List<Node> aStar(Map<Long, Integer> heightMap, int startGx, int startGz, int endGx, int endGz) {
        long startKey = key(startGx, startGz);
        long goalKey = key(endGx, endGz);
        
        Integer startHeightBoxed = heightMap.get(startKey);
        Integer endHeightBoxed = heightMap.get(goalKey);
        if (startHeightBoxed == null || endHeightBoxed == null) return null;
        
        int startHeight = startHeightBoxed;
        int endHeight = endHeightBoxed;

        Node start = new Node(startGx, startHeight, startGz);
        Node goal = new Node(endGx, endHeight, endGz);

        Map<Node, Node> cameFrom = new HashMap<>();
        Map<Node, Double> gScore = new HashMap<>();
        gScore.put(start, 0.0);

        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingDouble(n -> gScore.getOrDefault(n, Double.MAX_VALUE)
                        + manhattan(n, goal)));
        open.add(start);
        Set<Node> visited = new HashSet<>();

        int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.gx == endGx && current.gz == endGz && current.gy == endHeight) {
                return reconstruct(cameFrom, current);
            }
            if (!visited.add(current)) continue;

            for (int[] d : dirs) {
                int ngx = current.gx + d[0], ngz = current.gz + d[1];
                long nextKey = key(ngx, ngz);
                Integer terrainHeightBoxed = heightMap.get(nextKey);
                if (terrainHeightBoxed == null) continue; // outside generated terrain
                int terrainHeight = terrainHeightBoxed;

                for (int dy = -1; dy <= 1; dy++) {
                    int ngy = current.gy + dy;
                    Node next = new Node(ngx, ngy, ngz);
                    
                    double cost = STEP_COST;
                    if (dy != 0) cost += SLOPE_PENALTY;
                    
                    double diff = ngy - terrainHeight;
                    if (diff < 0) {
                        cost += Math.abs(diff) * TRENCH_PENALTY;
                    } else if (diff > 0) {
                        cost += Math.abs(diff) * BRIDGE_PENALTY;
                    }

                    double tentativeG = gScore.getOrDefault(current, Double.MAX_VALUE) + cost;

                    if (tentativeG < gScore.getOrDefault(next, Double.MAX_VALUE)) {
                        cameFrom.put(next, current);
                        gScore.put(next, tentativeG);
                        open.add(next);
                    }
                }
            }
        }
        return null;
    }

    private double manhattan(Node a, Node b) {
        return Math.abs(a.gx - b.gx) + Math.abs(a.gz - b.gz) + Math.abs(a.gy - b.gy) * 5.0;
    }

    private List<Node> reconstruct(Map<Node, Node> cameFrom, Node current) {
        List<Node> path = new ArrayList<>();
        while (true) {
            path.add(current);
            Node prev = cameFrom.get(current);
            if (prev == null) {
                break;
            }
            current = prev;
        }
        Collections.reverse(path);
        return path;
    }

    private void registerPath(Map<Long, Integer> heightMap, List<Node> path, int waterLevelGridY) {
        int n = path.size();
        int[] roadY = new int[n];
        int[] masks = new int[n];

        for (int i = 0; i < n; i++) {
            Node here = path.get(i);
            roadY[i] = here.gy;

            int mask = 0;
            if (i > 0) mask |= directionTo(here, path.get(i - 1));
            if (i < n - 1) mask |= directionTo(here, path.get(i + 1));
            
            // Fix WFC fatal contradiction: an isolated road piece (or single node path) must not have mask 0.
            // If mask is 0, WFC Grid initChunk() removes ALL road modules, creating an empty cell and an unresolvable cascade.
            if (mask == 0) mask = DIR_PX | DIR_NX;
            
            connections.put(key(here.gx, here.gz), mask);
            masks[i] = mask;
        }
        
        // Find flat anchor points (turns, intersections, ends)
        boolean[] isAnchor = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (i == 0 || i == n - 1) {
                isAnchor[i] = true;
            } else {
                // mask 3 is straight X, mask 12 is straight Z
                if (masks[i] != 3 && masks[i] != 12) {
                    isAnchor[i] = true;
                }
            }
        }
        
        // Smooth heights between anchors
        double[] smoothHeight = new double[n];
        int lastAnchor = 0;
        for (int i = 0; i < n; i++) {
            if (isAnchor[i]) {
                if (i == lastAnchor) {
                    smoothHeight[i] = roadY[i];
                } else {
                    double h1 = roadY[lastAnchor];
                    double h2 = roadY[i];
                    int dist = i - lastAnchor;
                    
                    // Cap steepness to max 1 block per cell
                    double diff = h2 - h1;
                    if (Math.abs(diff) > dist) {
                        h2 = h1 + Math.signum(diff) * dist;
                    }
                    
                    for (int j = lastAnchor; j <= i; j++) {
                        double t = (double) (j - lastAnchor) / dist;
                        smoothHeight[j] = h1 + (h2 - h1) * t;
                    }
                    lastAnchor = i;
                }
            }
        }
        
        if (lastAnchor < n - 1) {
            double h1 = roadY[lastAnchor];
            for (int j = lastAnchor; j < n; j++) smoothHeight[j] = h1;
        }

        int i = 0;
        while (i < n) {
            Integer natHeight = heightMap.get(key(path.get(i).gx, path.get(i).gz));
            boolean isWater = natHeight != null && natHeight < waterLevelGridY;
            
            if (isWater) {
                int gapStart = i;
                int gapEnd = i;
                while (gapEnd + 1 < n) {
                    Integer nextNatHeight = heightMap.get(key(path.get(gapEnd + 1).gx, path.get(gapEnd + 1).gz));
                    boolean nextIsWater = nextNatHeight != null && nextNatHeight < waterLevelGridY;
                    if (!nextIsWater) break;
                    gapEnd++;
                }
                registerBridgeSpan(path, smoothHeight, gapStart, gapEnd, waterLevelGridY);
                i = gapEnd + 1;
            } else {
                roadHeight.put(key(path.get(i).gx, path.get(i).gz), smoothHeight[i]);
                i++;
            }
        }
    }

    private void registerBridgeSpan(List<Node> path, double[] smoothHeight,
            int gapStart, int gapEnd, int waterLevelGridY) {

        double minDeck = waterLevelGridY + BRIDGE_CLEARANCE;
        double beforeHeight = gapStart > 0 ? smoothHeight[gapStart - 1] : minDeck;
        double afterHeight = gapEnd < smoothHeight.length - 1 ? smoothHeight[gapEnd + 1] : minDeck;

        int span = gapEnd - gapStart + 1;
        for (int i = gapStart; i <= gapEnd; i++) {
            double t = span == 1 ? 0.5 : (double) (i - gapStart) / (span - 1);
            double interpolated = beforeHeight + (afterHeight - beforeHeight) * t;
            double deckHeight = Math.max(interpolated, minDeck);
            
            long k = key(path.get(i).gx, path.get(i).gz);
            bridgeColumns.add(k);
            bridgeDeckHeight.put(k, deckHeight);
        }
    }

    private int directionTo(Node from, Node to) {
        if (to.gx > from.gx) return DIR_PX;
        if (to.gx < from.gx) return DIR_NX;
        if (to.gz > from.gz) return DIR_PZ;
        if (to.gz < from.gz) return DIR_NZ;
        return 0;
    }
}
