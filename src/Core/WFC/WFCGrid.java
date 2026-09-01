package Core.WFC;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WFCGrid {
    public int sizeX, sizeY, sizeZ;
    public WFCCell[][][] grid;
    public List<WFCModule> allModules;
    
    private Random random = new Random();
    
    // Defines the offset for the 6 directions
    private static final int[][] DIRS = {
        {1, 0, 0},  // 0: +X
        {-1, 0, 0}, // 1: -X
        {0, 1, 0},  // 2: +Y
        {0, -1, 0}, // 3: -Y
        {0, 0, 1},  // 4: +Z
        {0, 0, -1}  // 5: -Z
    };

    public WFCGrid(int sizeX, int sizeY, int sizeZ, List<WFCModule> modules) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.allModules = modules;
        
        grid = new WFCCell[sizeX][sizeY][sizeZ];
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    grid[x][y][z] = new WFCCell(x, y, z, allModules);
                }
            }
        }
    }
    
    public int attempts = 0;
    
    public int step() {
        int maxAttempts = sizeX * sizeY * sizeZ * 2;
        
        if (isFullyCollapsed()) {
            return 1;
        }
        
        if (attempts++ > maxAttempts) {
            System.out.println("WFC failed to solve in time.");
            return -1;
        }
        
        WFCCell cell = getMinEntropyCell();
        if (cell == null) {
            return 1;
        }
        if (cell.possibleModules.isEmpty()) {
            System.out.println("Contradiction reached at " + cell.x + "," + cell.y + "," + cell.z);
            System.out.println("Checking neighbors:");
            for (int d = 0; d < 6; d++) {
                int nx = cell.x + DIRS[d][0];
                int ny = cell.y + DIRS[d][1];
                int nz = cell.z + DIRS[d][2];
                if (nx >= 0 && nx < sizeX && ny >= 0 && ny < sizeY && nz >= 0 && nz < sizeZ) {
                    WFCCell neighbor = grid[nx][ny][nz];
                    System.out.print("  Dir " + d + " (" + nx + "," + ny + "," + nz + "): ");
                    if (neighbor.collapsed) {
                        System.out.println("Collapsed to " + neighbor.possibleModules.get(0).name);
                    } else {
                        System.out.print(neighbor.possibleModules.size() + " possibilities [");
                        for (WFCModule m : neighbor.possibleModules) System.out.print(m.name + " ");
                        System.out.println("]");
                    }
                } else {
                    System.out.println("  Dir " + d + " is OUT OF BOUNDS");
                }
            }
            return -1; // Contradiction
        }
        
        // Randomly select a module based on weights
        WFCModule chosen = selectRandomModule(cell.possibleModules);
        cell.collapse(chosen);
        
        // Propagate the consequences of this choice
        propagate(cell);
        
        return 0;
    }
    
    public boolean solve() {
        attempts = 0;
        while (true) {
            int result = step();
            if (result == 1) {
                System.out.println("WFC solved successfully!");
                return true;
            } else if (result == -1) {
                return false;
            }
        }
    }
    
    private boolean isFullyCollapsed() {
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    if (!grid[x][y][z].collapsed) return false;
                }
            }
        }
        return true;
    }
    
    private WFCCell getMinEntropyCell() {
        double minEntropy = Double.MAX_VALUE;
        WFCCell minCell = null;
        
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    WFCCell cell = grid[x][y][z];
                    if (!cell.collapsed) {
                        double entropy = cell.getEntropy();
                        // Add a tiny bit of random noise to break ties
                        entropy -= random.nextDouble() * 0.0001; 
                        
                        if (entropy < minEntropy) {
                            minEntropy = entropy;
                            minCell = cell;
                        }
                    }
                }
            }
        }
        return minCell;
    }
    
    private WFCModule selectRandomModule(List<WFCModule> modules) {
        double totalWeight = 0;
        for (WFCModule m : modules) {
            totalWeight += m.weight;
        }
        double value = random.nextDouble() * totalWeight;
        for (WFCModule m : modules) {
            value -= m.weight;
            if (value <= 0) return m;
        }
        return modules.get(modules.size() - 1);
    }
    
    private void propagate(WFCCell startCell) {
        List<WFCCell> stack = new ArrayList<>();
        stack.add(startCell);
        
        while (!stack.isEmpty()) {
            WFCCell current = stack.remove(stack.size() - 1);
            
            for (int d = 0; d < 6; d++) {
                int nx = current.x + DIRS[d][0];
                int ny = current.y + DIRS[d][1];
                int nz = current.z + DIRS[d][2];
                
                if (nx >= 0 && nx < sizeX && ny >= 0 && ny < sizeY && nz >= 0 && nz < sizeZ) {
                    WFCCell neighbor = grid[nx][ny][nz];
                    if (neighbor.collapsed) continue;
                    
                    boolean changed = constrainNeighbor(current, neighbor, d);
                    if (changed) {
                        stack.add(neighbor);
                    }
                }
            }
        }
    }
    
    public void initialPropagate() {
        List<WFCCell> stack = new ArrayList<>();
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    stack.add(grid[x][y][z]);
                }
            }
        }
        
        while (!stack.isEmpty()) {
            WFCCell current = stack.remove(stack.size() - 1);
            
            for (int d = 0; d < 6; d++) {
                int nx = current.x + DIRS[d][0];
                int ny = current.y + DIRS[d][1];
                int nz = current.z + DIRS[d][2];
                
                if (nx >= 0 && nx < sizeX && ny >= 0 && ny < sizeY && nz >= 0 && nz < sizeZ) {
                    WFCCell neighbor = grid[nx][ny][nz];
                    if (neighbor.collapsed) continue;
                    
                    boolean changed = constrainNeighbor(current, neighbor, d);
                    if (changed) {
                        if (!stack.contains(neighbor)) {
                            stack.add(neighbor);
                        }
                    }
                }
            }
        }
    }
    
    // Constrains neighbor's possible modules based on the current cell's possible modules.
    // direction is the direction FROM current TO neighbor.
    private boolean constrainNeighbor(WFCCell current, WFCCell neighbor, int direction) {
        boolean changed = false;
        
        // Remove modules from neighbor if they cannot mathematically connect 
        // to AT LEAST ONE of the possible modules in the current cell.
        for (int i = neighbor.possibleModules.size() - 1; i >= 0; i--) {
            WFCModule neighMod = neighbor.possibleModules.get(i);
            
            boolean canConnectToAnything = false;
            for (WFCModule curMod : current.possibleModules) {
                if (curMod.canConnect(direction, neighMod)) {
                    canConnectToAnything = true;
                    break;
                }
            }
            
            if (!canConnectToAnything) {
                if (neighbor.possibleModules.size() == 1) {
                    System.out.println("FATAL CONTRADICTION: Cell " + neighbor.x + "," + neighbor.y + "," + neighbor.z + 
                        " lost its LAST possibility: " + neighMod.name + " because it couldn't connect to current cell " + current.x + "," + current.y + "," + current.z + " in direction " + direction);
                    System.out.print("Current cell possible modules: ");
                    for (WFCModule m : current.possibleModules) System.out.print(m.name + " ");
                    System.out.println("\n");
                }
                neighbor.possibleModules.remove(i);
                changed = true;
            }
        }
        
        return changed;
    }
}
