import java.util.*;

public class MC {
    public static final int DIR_PX = 1;
    public static final int DIR_NX = 2;
    public static final int DIR_PZ = 4;
    public static final int DIR_NZ = 8;
    
    private static int getDir(int fgx, int fgz, int tgx, int tgz) {
        if (tgx > fgx) return DIR_PX; // 1
        if (tgx < fgx) return DIR_NX; // 2
        if (tgz > fgz) return DIR_PZ; // 4
        if (tgz < fgz) return DIR_NZ; // 8
        return 0;
    }
    
    public static void main(String[] args) {
        int[][] path = {
            {0, 0},
            {1, 0},
            {1, 1},
            {2, 1},
            {2, 2}
        };
        
        for (int i = 0; i < path.length; i++) {
            int mask = 0;
            if (i > 0) {
                mask |= getDir(path[i][0], path[i][1], path[i-1][0], path[i-1][1]);
            }
            if (i < path.length - 1) {
                mask |= getDir(path[i][0], path[i][1], path[i+1][0], path[i+1][1]);
            }
            
            System.out.println("Cell " + path[i][0] + "," + path[i][1] + " -> Mask " + mask);
            
            String arms = "";
            if ((mask & DIR_PX) != 0) arms += "+X ";
            if ((mask & DIR_NX) != 0) arms += "-X ";
            if ((mask & DIR_PZ) != 0) arms += "+Z ";
            if ((mask & DIR_NZ) != 0) arms += "-Z ";
            System.out.println("  Arms: " + arms);
        }
    }
}
