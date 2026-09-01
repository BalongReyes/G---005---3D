package Objects.WFC;

import Core.WFC.SocketType;
import Core.WFC.WFCModule;
import Data.Vertex;
import java.awt.Color;
import java.util.List;

/**
 * Bare ground: a surface cell that gets NO decorative geometry at all.
 *
 * WFCGenerator currently restricts every surface-layer cell to GRASS-category
 * modules, and with both GrassModule and TreeModule sharing that category,
 * every single surface cell was being forced into grass or a tree - that's
 * what was tanking the framerate. This module exists purely to give the
 * solver a cheap, empty option so most of the surface stays bare (the
 * terrain mesh from TerrainGenerator is already visible there; this module
 * doesn't need to draw anything on top of it).
 *
 * Its weight controls vegetation DENSITY directly: raise it for a sparser,
 * faster world; lower it for a denser, heavier one.
 */
public class GroundModule extends WFCModule {

    public GroundModule() {
        super(SocketType.GRASS, "Ground",
                // What this module PROVIDES to its neighbors
                new SocketType[] {
                        SocketType.WILDCARD, // Right (+X)
                        SocketType.WILDCARD, // Left (-X)
                        SocketType.AIR, // Up (+Y) - nothing stacks on top of bare ground
                        SocketType.SOLID, // Down (-Y) - sits on solid ground
                        SocketType.WILDCARD, // Forward (+Z)
                        SocketType.WILDCARD // Back (-Z)
                },
                // What this module ACCEPTS from its neighbors
                new SocketType[][] {
                        { SocketType.WILDCARD }, // Right (+X)
                        { SocketType.WILDCARD }, // Left (-X)
                        { SocketType.AIR }, // Up (+Y)
                        { SocketType.SOLID }, // Down (-Y)
                        { SocketType.WILDCARD }, // Forward (+Z)
                        { SocketType.WILDCARD } // Back (-Z)
                },
                new Color(90, 80, 60), 20.0);
    }

    @Override
    public void generateMesh(double cx, double cy, double cz, double size,
            List<Vertex> verts, List<Data.Face> faces) {
        // Intentionally empty - bare ground contributes zero geometry.
    }
}