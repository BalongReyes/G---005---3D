package Core.WFC;

import java.util.ArrayList;
import java.util.List;

public class WFCCell {
    public int x, y, z;
    
    // The list of modules that can still be placed in this cell.
    public List<WFCModule> possibleModules;
    
    // Once collapsed, this holds the final chosen module.
    public WFCModule finalModule = null;
    
    // True if this cell has been finalized.
    public boolean collapsed = false;

    public WFCCell(int x, int y, int z, List<WFCModule> allModules) {
        this.x = x;
        this.y = y;
        this.z = z;
        // Initially, all modules are possible.
        this.possibleModules = new ArrayList<>(allModules);
    }
    
    public double getEntropy() {
        if (collapsed) return Double.MAX_VALUE; // Ignore already collapsed cells
        
        // A simple entropy calculation is the number of possible modules.
        // A more advanced one would use Shannon entropy based on module weights.
        if (possibleModules.size() == 0) return 0;
        
        double sumOfWeights = 0;
        double sumOfWeightLogWeight = 0;
        for (WFCModule m : possibleModules) {
            sumOfWeights += m.weight;
            sumOfWeightLogWeight += m.weight * Math.log(m.weight);
        }
        return Math.log(sumOfWeights) - (sumOfWeightLogWeight / sumOfWeights);
    }

    public void collapse(WFCModule module) {
        this.finalModule = module;
        this.possibleModules.clear();
        this.possibleModules.add(module);
        this.collapsed = true;
    }
}
