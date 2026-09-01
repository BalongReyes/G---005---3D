package Core.WFC;

import java.util.ArrayList;
import java.util.List;

public class WFCGenerator {

    public WFCGrid grid;

    private Core.Main main;
    private Core.World.ScalarField scalarField;
    private List<WFCModule> modules;

    private int sizeX = 20;
    private int sizeY = 10;
    private int sizeZ = 20;

    // A column counts as "steep" (mountain flank, not a field) if its surface
    // height differs from any of its 4 neighbors by more than this many grid
    // cells. Steep columns get plain rock (SOLID) at the surface instead of
    // GRASS, which is what was letting flat, cell-sized grass tufts appear
    // stuck out of near-vertical mountainsides.
    private static final int STEEP_HEIGHT_DIFF = 2;

    private int offsetX = 0;
    private int offsetZ = 0;

    // Geometry for the chunk currently being solved only. Reset every time a
    // new chunk starts, so rebuilding it never costs more than one chunk's
    // worth of work no matter how much terrain has already been generated.
    private List<Data.Vertex> chunkVerts = new ArrayList<>();
    private List<Data.Face> chunkFaces = new ArrayList<>();
    // Tracks which cells in the current chunk have already contributed
    private boolean[][][] meshedCells;
    public WFCModule[][][] wfcGlobalGrid;
    private RoadNetwork roadNetwork;

    // The render object for the in-progress chunk. Finished chunks are left
    // in the renderer permanently and are never touched again.
    private Data.Object3D chunkPlaceholderObject;

    private boolean globalTerrainModified = false;

    private boolean isFinished = false;
    private boolean chunkInitialized = false;

    // A contradiction is a normal outcome of this solver's greedy,
    // non-backtracking random walk - one bad early guess can dead-end a
    // later cell with zero legal modules. Re-rolling the same chunk with a
    // fresh grid (and therefore a fresh random walk) resolves the vast
    // majority of these without ever needing real backtracking. Only after
    // MAX_CHUNK_RETRIES straight failures do we give up and move on, same
    // as the old behavior.
    private static final int MAX_CHUNK_RETRIES = 5;
    private int chunkRetries = 0;

    private int currentSizeX;
    private int currentSizeZ;
    private int currentSizeY;
    private int offsetY;

    // How many extra rows of guaranteed SOLID to keep below the lowest
    // surface point in the chunk, and guaranteed AIR above the highest, so
    // there's always a clean floor to root into and open sky/water above.
    private static final int BELOW_PADDING = 3;
    private static final int ABOVE_PADDING = 6;
    // Upper bound on how tall a single chunk's grid is allowed to grow, so a
    // single chunk spanning an extreme cliff can't blow up solve cost
    // unboundedly. If a chunk's real height range exceeds this, the window
    // is centered on the range and the extremes get clipped - better than
    // silently reproducing the old bug, but still worth widening if you hit it.
    private static final int MAX_SIZE_Y = 48;

    public void init(Core.Main main, Core.World.ScalarField scalarField) {
        this.main = main;
        this.scalarField = scalarField;

        System.out.println("Initializing WFC Generator State Machine...");

        modules = new ArrayList<>();
        modules.add(new Objects.WFC.AirModule());
        modules.add(new Objects.WFC.SolidModule());
        modules.add(new Objects.WFC.GroundModule());
        modules.add(new Objects.WFC.GrassModule());
        modules.add(new Objects.WFC.TreeModule());
        modules.add(new Objects.WFC.KelpModule());
        modules.add(new Objects.WFC.RockModule());
        modules.add(new Objects.WFC.FlowerModule());
        modules.add(new Objects.WFC.CoralModule());

        int[] shapes = { 0b1010, 0b0101, 0b1100, 0b1001, 0b0110, 0b0011, 0b0001, 0b0010, 0b0100, 0b1000 };
        for (int mask : shapes) {
            modules.add(new Objects.WFC.RoadModule(mask));
        }
        modules.add(new Objects.WFC.BridgeModule(0b1010)); // straight X
        modules.add(new Objects.WFC.BridgeModule(0b0101)); // straight Z

        java.util.Map<Long, Integer> globalHeightMap = buildGlobalHeightMap();
        double spacing = Settings.WorldSettings.SPACING;
        double startY = -((scalarField.sizeY - 1) * spacing) / 2.0;
        int waterLevelGridY = (int) Math.round((Settings.WorldSettings.WATER_LEVEL - startY) / spacing);

        roadNetwork = new RoadNetwork();
        // Arbitrary test points that are within bounds
        int startGx = 5;
        int startGz = 5;
        int endGx = Math.max(5, scalarField.sizeX - 6);
        int endGz = Math.max(5, scalarField.sizeZ - 6);
        roadNetwork.planPath(globalHeightMap, startGx, startGz, endGx, endGz, waterLevelGridY);

        // Pad by 1 to never build anything directly on the world edge!
        offsetX = 1;
        offsetZ = 1;
        isFinished = false;
        chunkInitialized = false;
    }

    private java.util.Map<Long, Integer> buildGlobalHeightMap() {
        java.util.Map<Long, Integer> map = new java.util.HashMap<>();
        for (int[] cell : main.surfaceGridCells) {
            map.put(columnKey(cell[0], cell[2]), cell[1]);
        }
        return map;
    }

    public boolean tick() {
        if (scalarField == null)
            return false;
        if (isFinished)
            return true;

        if (!chunkInitialized) {
            // Check if we've processed all chunks
            if (offsetX >= scalarField.sizeX - 1) {
                finishGeneration();
                return true;
            }

            initChunk();
            chunkInitialized = true;
        }

        // Do multiple steps per tick to speed up the animation
        int stepsPerTick = 50;
        int status = 0;
        for (int i = 0; i < stepsPerTick; i++) {
            status = grid.step();
            if (status != 0)
                break;
        }

        // Mesh whichever cells were just resolved, whether or not the whole
        // chunk is done yet. This is what actually makes the collapse visible
        // step by step instead of only popping in once a chunk finishes.
        updateChunkPreview();

        if (status != 0) {
            if (status == 1) {
                // Success: leave the finished preview mesh in the renderer
                // permanently and stop tracking it, so the next chunk starts fresh.
                boolean modified = applyModifiersToTerrain();
                if (modified)
                    globalTerrainModified = true;
                chunkPlaceholderObject = null;

                chunkRetries = 0;
                advanceToNextChunk();
            } else {
                // Failed: discard the partial preview for this chunk so no
                // half-resolved geometry is left behind.
                if (chunkPlaceholderObject != null) {
                    main.objectHandler.getObjects().remove(chunkPlaceholderObject);
                    chunkPlaceholderObject = null;
                }

                chunkRetries++;
                if (chunkRetries < MAX_CHUNK_RETRIES) {
                    // Re-roll the same chunk footprint with a fresh grid.
                    // chunkInitialized stays false so the top of tick() calls
                    // initChunk() again next iteration, but offsetX/offsetZ
                    // are untouched so it's the exact same region.
                    System.out.println("WFC contradiction, retrying chunk (" + chunkRetries
                            + "/" + MAX_CHUNK_RETRIES + ")...");
                } else {
                    System.out.println("WFC gave up on chunk after " + MAX_CHUNK_RETRIES
                            + " attempts, skipping.");
                    chunkRetries = 0;
                    advanceToNextChunk();
                }
            }

            chunkInitialized = false;
        }

        return isFinished;
    }

    // Advances offsetX/offsetZ to the next chunk footprint. Split out so both
    // the success path and the give-up-after-retries path share it.
    private void advanceToNextChunk() {
        offsetZ += sizeZ;
        if (offsetZ >= scalarField.sizeZ - 1) {
            offsetZ = 1;
            offsetX += sizeX;
        }
    }

    private void initChunk() {
        // Ensure WFC grid doesn't hang off the edge of the Marching Cubes mesh
        currentSizeX = Math.min(sizeX, scalarField.sizeX - offsetX - 1);
        currentSizeZ = Math.min(sizeZ, scalarField.sizeZ - offsetZ - 1);

        int centerGX = offsetX + currentSizeX / 2;
        int centerGZ = offsetZ + currentSizeZ / 2;
        int centerGY = 15;

        for (int[] cell : main.surfaceGridCells) {
            if (cell[0] == centerGX && cell[2] == centerGZ) {
                centerGY = cell[1];
                break;
            }
        }

        // Build a (gx,gz) -> surface height lookup once for this chunk,
        // padded by 1 cell so we can check the immediate neighbors of every
        // column in the chunk for slope, including columns right at the
        // chunk's edge. While we're scanning it, also track the actual
        // min/max surface height found strictly INSIDE the chunk footprint
        // (not the slope-check padding) - this is what the chunk's vertical
        // window gets sized around, instead of just the center column's
        // height. A single fixed-size window anchored to one sample point
        // was the reason shoreline chunks only ever showed whichever terrain
        // type happened to land near that one sample: any column whose real
        // surface fell outside that window couldn't reach its own surface
        // layer at all.
        java.util.Map<Long, Integer> heightMap = new java.util.HashMap<>();
        int minSurfaceGY = Integer.MAX_VALUE;
        int maxSurfaceGY = Integer.MIN_VALUE;
        for (int[] cell : main.surfaceGridCells) {
            int gx = cell[0], gz = cell[2];
            if (gx < offsetX - 1 || gx > offsetX + currentSizeX
                    || gz < offsetZ - 1 || gz > offsetZ + currentSizeZ)
                continue;
            heightMap.put(columnKey(gx, gz), cell[1]);

            if (gx >= offsetX && gx < offsetX + currentSizeX
                    && gz >= offsetZ && gz < offsetZ + currentSizeZ) {
                minSurfaceGY = Math.min(minSurfaceGY, cell[1]);
                maxSurfaceGY = Math.max(maxSurfaceGY, cell[1]);
            }
        }
        // Fall back to the center-column sample if no surface cells were
        // found inside the footprint at all (e.g. an empty/ungenerated area).
        if (minSurfaceGY > maxSurfaceGY) {
            minSurfaceGY = centerGY;
            maxSurfaceGY = centerGY;
        }

        int desiredSizeY = (maxSurfaceGY + ABOVE_PADDING) - (minSurfaceGY - BELOW_PADDING) + 1;
        currentSizeY = Math.max(sizeY, Math.min(MAX_SIZE_Y, desiredSizeY));
        offsetY = (minSurfaceGY - BELOW_PADDING);
        // If clamping made the window shorter than the full desired range,
        // keep it centered on the actual terrain range rather than always
        // anchored at the bottom, so an oversized chunk clips evenly off
        // both ends instead of always losing the sky side.
        int overflow = desiredSizeY - currentSizeY;
        if (overflow > 0) {
            offsetY += overflow / 2;
        }

        grid = new WFCGrid(currentSizeX, currentSizeY, currentSizeZ, modules);

        // Same grid-Y -> world-Y conversion used in updateChunkPreview, needed
        // here to compare a surface cell's height against the water level.
        double spacing = Settings.WorldSettings.SPACING;
        double startY = -((scalarField.sizeY - 1) * spacing) / 2;

        for (int x = 0; x < currentSizeX; x++) {
            for (int z = 0; z < currentSizeZ; z++) {
                int gx = offsetX + x;
                int gz = offsetZ + z;

                // Sync with TerrainGenerator.getSurfaceCells buffer zone (3 to sizeX - 5)
                if (gx < 3 || gz < 3 || gx > Settings.WorldSettings.GRID_SIZE_X - 5
                        || gz > Settings.WorldSettings.GRID_SIZE_Z - 5) {
                    // Out of bounds or at the sheer wall boundary. Force to AIR.
                    for (int y = 0; y < currentSizeY; y++) {
                        grid.grid[x][y][z].possibleModules.removeIf(m -> m.category != SocketType.AIR);
                    }
                    continue;
                }

                Integer localGYBoxed = heightMap.get(columnKey(gx, gz));
                int localGY = localGYBoxed != null ? localGYBoxed : centerGY;

                boolean steep = isSteep(heightMap, gx, gz, localGY);

                // A surface cell counts as submerged if it sits at or below
                // the water plane's Y level - lake/ocean floor, not dry land.
                double surfaceWorldY = startY + localGY * spacing + spacing / 2.0;
                boolean submerged = surfaceWorldY <= Settings.WorldSettings.WATER_LEVEL;

                // Road/bridge columns are a hard global constraint planned by
                // RoadNetwork before any chunk solving starts - the ONE cell
                // at the road/bridge's exact height must be stamped with the
                // matching RoadModule/BridgeModule below. Both of those
                // modules have category == ROAD, which doesn't match
                // SOLID/GRASS/AIR/KELP, so if the normal terrain/vegetation
                // category filter ran on that exact cell it would strip every
                // RoadModule/BridgeModule out of possibleModules before the
                // stamping pass ever runs - leaving 0 possibilities there and
                // cascading into an immediate contradiction.
                //
                // Figure out which single y-index (if any) will be stamped,
                // BEFORE running the filter loop, so we can skip the filter
                // for just that one cell. Every other cell in the column
                // still goes through the normal filter - which, as a side
                // effect, already excludes ROAD-category modules there too
                // (it only ever keeps SOLID, GRASS, or AIR/KELP) - so a road
                // piece can never spawn stacked above/below its intended
                // height in the same column.
                boolean isBridgeColumn = roadNetwork.isBridge(gx, gz);
                boolean isRoadColumnOnly = !isBridgeColumn && roadNetwork.isRoad(gx, gz);
                int stampY = -1;
                if (isBridgeColumn) {
                    stampY = roadNetwork.getBridgeDeckHeight(gx, gz) - offsetY;
                } else if (isRoadColumnOnly) {
                    stampY = roadNetwork.getRoadHeight(gx, gz) - offsetY;
                }

                for (int y = 0; y < currentSizeY; y++) {
                    if (y == stampY)
                        continue; // reserved for the stamping pass below - leave possibleModules untouched here

                    int gy = offsetY + y;
                    if (gy < localGY) {
                        grid.grid[x][y][z].possibleModules.removeIf(m -> m.category != SocketType.SOLID);
                    } else if (gy == localGY) {
                        if (steep || submerged) {
                            grid.grid[x][y][z].possibleModules.removeIf(m -> m.category != SocketType.SOLID);
                        } else {
                            grid.grid[x][y][z].possibleModules.removeIf(m -> m.category != SocketType.GRASS);
                        }
                    } else {
                        // Above the seabed: if this cell is still underwater
                        // (column is submerged and we haven't reached the
                        // water line yet), let Kelp and Air compete for the
                        // cell instead of forcing Air outright. Once we're
                        // above the water line - or the column was never
                        // submerged to begin with - it's dry open air only.
                        double cellWorldY = startY + gy * spacing + spacing / 2.0;
                        if (submerged && cellWorldY <= Settings.WorldSettings.WATER_LEVEL) {
                            grid.grid[x][y][z].possibleModules
                                    .removeIf(m -> m.category != SocketType.AIR && m.category != SocketType.KELP);
                        } else {
                            grid.grid[x][y][z].possibleModules.removeIf(m -> m.category != SocketType.AIR);
                        }
                    }
                }

                // Stamping pass for roads and bridges - forces the single
                // reserved cell (stampY, skipped above) down to the exact
                // matching module and nothing else.
                if (isBridgeColumn) {
                    int mask = roadNetwork.getConnections(gx, gz);
                    if (stampY >= 0 && stampY < currentSizeY) {
                        grid.grid[x][stampY][z].possibleModules.removeIf(m -> !(m instanceof Objects.WFC.BridgeModule
                                && ((Objects.WFC.BridgeModule) m).getConnectionMask() == mask));
                    }
                } else if (isRoadColumnOnly) {
                    int mask = roadNetwork.getConnections(gx, gz);
                    if (stampY >= 0 && stampY < currentSizeY) {
                        grid.grid[x][stampY][z].possibleModules.removeIf(m -> !(m instanceof Objects.WFC.RoadModule
                                && ((Objects.WFC.RoadModule) m).getConnectionMask() == mask));
                    }
                }
            }
        }

        // Run an initial propagation pass to prune impossible configurations
        // (e.g. KELP category modules like Coral that can't spawn mid-water)
        grid.initialPropagate();

        // Reset per-chunk preview state for the new chunk.
        meshedCells = new boolean[currentSizeX][currentSizeY][currentSizeZ];
        chunkVerts.clear();
        chunkFaces.clear();
        if (chunkPlaceholderObject != null) {
            main.objectHandler.getObjects().remove(chunkPlaceholderObject);
            chunkPlaceholderObject = null;
        }
    }

    private static long columnKey(int gx, int gz) {
        return (((long) gx) << 32) ^ (gz & 0xffffffffL);
    }

    // A column is steep if its surface height drops off sharply from any of
    // its 4-neighbors, i.e. it's on a mountain flank rather than a field.
    // Unknown neighbors (outside the cached surface set) are treated as
    // matching so chunk edges don't get falsely flagged as steep.
    private boolean isSteep(java.util.Map<Long, Integer> heightMap, int gx, int gz, int localGY) {
        int[][] neighbors = { { gx + 1, gz }, { gx - 1, gz }, { gx, gz + 1 }, { gx, gz - 1 } };
        for (int[] n : neighbors) {
            Integer h = heightMap.get(columnKey(n[0], n[1]));
            if (h == null)
                continue;
            if (Math.abs(h - localGY) > STEEP_HEIGHT_DIFF) {
                return true;
            }
        }
        return false;
    }

    // Meshes any cells in the current chunk that were resolved since the last
    // call and refreshes the render object for just this chunk. Cost is
    // bounded by this chunk's own size, not by how much terrain already exists.
    private void updateChunkPreview() {
        double spacing = Settings.WorldSettings.SPACING;
        double startX = -((scalarField.sizeX - 1) * spacing) / 2;
        double startY = -((scalarField.sizeY - 1) * spacing) / 2;
        double startZ = -((scalarField.sizeZ - 1) * spacing) / 2;

        boolean addedAny = false;

        for (int x = 0; x < currentSizeX; x++) {
            for (int y = 0; y < currentSizeY; y++) {
                for (int z = 0; z < currentSizeZ; z++) {
                    if (meshedCells[x][y][z])
                        continue;

                    WFCModule mod = grid.grid[x][y][z].finalModule;
                    if (mod == null)
                        continue;

                    int gx = offsetX + x;
                    int gy = offsetY + y;
                    int gz = offsetZ + z;

                    double cx = startX + gx * spacing + spacing / 2.0;
                    double cz = startZ + gz * spacing + spacing / 2.0;
                    double cy = startY + gy * spacing + spacing / 2.0;

                    mod.generateMesh(cx, cy, cz, spacing, chunkVerts, chunkFaces, scalarField, main.isoLevel);
                    meshedCells[x][y][z] = true;
                    addedAny = true;
                }
            }
        }

        if (!addedAny)
            return;

        Data.Object3D updated = new Data.Object3D(
                chunkVerts.toArray(new Data.Vertex[0]),
                chunkFaces.toArray(new Data.Face[0]));
        updated.computeNormals();

        if (chunkPlaceholderObject != null) {
            main.objectHandler.getObjects().remove(chunkPlaceholderObject);
        }
        chunkPlaceholderObject = updated;
        main.objectHandler.addObject(updated);
    }

    // Applies terrain-carving side effects (e.g. Roads/Bridges flattening the
    // scalar field). Only runs once a chunk has fully, successfully resolved,
    // same as before — mesh generation now happens separately in
    // updateChunkPreview().
    private boolean applyModifiersToTerrain() {
        boolean terrainModified = false;

        for (int x = 0; x < currentSizeX; x++) {
            for (int y = 0; y < currentSizeY; y++) {
                for (int z = 0; z < currentSizeZ; z++) {
                    WFCModule mod = grid.grid[x][y][z].finalModule;
                    if (mod == null)
                        continue;

                    int gx = offsetX + x;
                    int gy = offsetY + y;
                    int gz = offsetZ + z;

                    if (mod.modifyTerrain(scalarField, gx, gy, gz)) {
                        terrainModified = true;
                    }
                }
            }
        }

        return terrainModified;
    }

    private void finishGeneration() {
        isFinished = true;

        if (globalTerrainModified) {
            System.out.println("Applied WFC to Terrain. Rebuilding Mesh...");
            main.updateTerrainMesh(main.isoLevel);
        } else {
            System.out.println("Applied WFC Placeholders (Terrain not modified).");
        }
    }
}