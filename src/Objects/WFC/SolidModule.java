package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;

public class SolidModule extends WFCModule {
    public SolidModule() {
        super(SocketType.SOLID, "Solid", 
            // What this module PROVIDES to its neighbors
            new SocketType[]{
                SocketType.WILDCARD, // Right (+X)
                SocketType.WILDCARD, // Left (-X)
                SocketType.SOLID,    // Up (+Y)
                SocketType.SOLID,    // Down (-Y)
                SocketType.WILDCARD, // Forward (+Z)
                SocketType.WILDCARD  // Back (-Z)
            },
            // What this module ACCEPTS from its neighbors
            new SocketType[][]{
                { SocketType.WILDCARD }, // Right (+X)
                { SocketType.WILDCARD }, // Left (-X)
                { SocketType.SOLID, SocketType.AIR, SocketType.KELP },    // Up (+Y)
                { SocketType.SOLID },    // Down (-Y)
                { SocketType.WILDCARD }, // Forward (+Z)
                { SocketType.WILDCARD }  // Back (-Z)
            }, 
            null, 50.0);
    }
}