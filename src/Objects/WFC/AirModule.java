package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;

public class AirModule extends WFCModule {
    public AirModule() {
        super(SocketType.AIR, "Air", 
            // What this module PROVIDES to its neighbors
            new SocketType[]{
                SocketType.WILDCARD, // Right (+X)
                SocketType.WILDCARD, // Left (-X)
                SocketType.AIR,      // Up (+Y)
                SocketType.AIR,      // Down (-Y)
                SocketType.WILDCARD, // Forward (+Z)
                SocketType.WILDCARD  // Back (-Z)
            },
            // What this module ACCEPTS from its neighbors
            new SocketType[][]{
                { SocketType.WILDCARD }, // Right (+X)
                { SocketType.WILDCARD }, // Left (-X)
                { SocketType.AIR },      // Up (+Y)
                { SocketType.AIR, SocketType.GRASS, SocketType.KELP, SocketType.SOLID }, // Down (-Y)
                { SocketType.WILDCARD }, // Forward (+Z)
                { SocketType.WILDCARD }  // Back (-Z)
            }, 
            null, 50.0);
    }
}