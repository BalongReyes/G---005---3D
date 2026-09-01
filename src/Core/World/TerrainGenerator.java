package Core.World;

import Data.Vertex;
import Data.Object3D;
import Data.Face;
import Data.Edge;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TerrainGenerator {
    public static final int[] edgeTable = {
            0x0, 0x109, 0x203, 0x30a, 0x406, 0x50f, 0x605, 0x70c,
            0x80c, 0x905, 0xa0f, 0xb06, 0xc0a, 0xd03, 0xe09, 0xf00,
            0x190, 0x99, 0x393, 0x29a, 0x596, 0x49f, 0x795, 0x69c,
            0x99c, 0x895, 0xb9f, 0xa96, 0xd9a, 0xc93, 0xf99, 0xe90,
            0x230, 0x339, 0x33, 0x13a, 0x636, 0x73f, 0x435, 0x53c,
            0xa3c, 0xb35, 0x83f, 0x936, 0xe3a, 0xf33, 0xc39, 0xd30,
            0x3a0, 0x2a9, 0x1a3, 0xaa, 0x7a6, 0x6af, 0x5a5, 0x4ac,
            0xbac, 0xaa5, 0x9af, 0x8a6, 0xfaa, 0xea3, 0xda9, 0xca0,
            0x460, 0x569, 0x663, 0x76a, 0x66, 0x16f, 0x265, 0x36c,
            0xc6c, 0xd65, 0xe6f, 0xf66, 0x86a, 0x963, 0xa69, 0xb60,
            0x5f0, 0x4f9, 0x7f3, 0x6fa, 0x1f6, 0xff, 0x3f5, 0x2fc,
            0xdfc, 0xcf5, 0xfff, 0xef6, 0x9fa, 0x8f3, 0xbf9, 0xaf0,
            0x650, 0x759, 0x453, 0x55a, 0x256, 0x35f, 0x55, 0x15c,
            0xe5c, 0xf55, 0xc5f, 0xd56, 0xa5a, 0xb53, 0x859, 0x950,
            0x7c0, 0x6c9, 0x5c3, 0x4ca, 0x3c6, 0x2cf, 0x1c5, 0xcc,
            0xfcc, 0xec5, 0xdcf, 0xcc6, 0xbca, 0xac3, 0x9c9, 0x8c0,
            0x8c0, 0x9c9, 0xac3, 0xbca, 0xcc6, 0xdcf, 0xec5, 0xfcc,
            0xcc, 0x1c5, 0x2cf, 0x3c6, 0x4ca, 0x5c3, 0x6c9, 0x7c0,
            0x950, 0x859, 0xb53, 0xa5a, 0xd56, 0xc5f, 0xf55, 0xe5c,
            0x15c, 0x55, 0x35f, 0x256, 0x55a, 0x453, 0x759, 0x650,
            0xaf0, 0xbf9, 0x8f3, 0x9fa, 0xef6, 0xfff, 0xcf5, 0xdfc,
            0x2fc, 0x3f5, 0xff, 0x1f6, 0x6fa, 0x7f3, 0x4f9, 0x5f0,
            0xb60, 0xa69, 0x963, 0x86a, 0xf66, 0xe6f, 0xd65, 0xc6c,
            0x36c, 0x265, 0x16f, 0x66, 0x76a, 0x663, 0x569, 0x460,
            0xca0, 0xda9, 0xea3, 0xfaa, 0x8a6, 0x9af, 0xaa5, 0xbac,
            0x4ac, 0x5a5, 0x6af, 0x7a6, 0xaa, 0x1a3, 0x2a9, 0x3a0,
            0xd30, 0xc39, 0xf33, 0xe3a, 0x936, 0x83f, 0xb35, 0xa3c,
            0x53c, 0x435, 0x73f, 0x636, 0x13a, 0x33, 0x339, 0x230,
            0xe90, 0xf99, 0xc93, 0xd9a, 0xa96, 0xb9f, 0x895, 0x99c,
            0x69c, 0x795, 0x49f, 0x596, 0x29a, 0x393, 0x99, 0x190,
            0xf00, 0xe09, 0xd03, 0xc0a, 0xb06, 0xa0f, 0x905, 0x80c,
            0x70c, 0x605, 0x50f, 0x406, 0x30a, 0x203, 0x109, 0x0 };
    public static final int[][] triTable = { { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 1, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 8, 3, 9, 8, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 3, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 2, 10, 0, 2, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 8, 3, 2, 10, 8, 10, 9, 8, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 11, 2, 8, 11, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 9, 0, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 11, 2, 1, 9, 11, 9, 8, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 10, 1, 11, 10, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 10, 1, 0, 8, 10, 8, 11, 10, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 9, 0, 3, 11, 9, 11, 10, 9, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 3, 0, 7, 3, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 1, 9, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 1, 9, 4, 7, 1, 7, 3, 1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 10, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 4, 7, 3, 0, 4, 1, 2, 10, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 2, 10, 9, 0, 2, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 10, 9, 2, 9, 7, 2, 7, 3, 7, 9, 4, -1, -1, -1, -1 },
            { 8, 4, 7, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 11, 4, 7, 11, 2, 4, 2, 0, 4, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 0, 1, 8, 4, 7, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 7, 11, 9, 4, 11, 9, 11, 2, 9, 2, 1, -1, -1, -1, -1 },
            { 3, 10, 1, 3, 11, 10, 7, 8, 4, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 11, 10, 1, 4, 11, 1, 0, 4, 7, 11, 4, -1, -1, -1, -1 },
            { 4, 7, 8, 9, 0, 11, 9, 11, 10, 11, 0, 3, -1, -1, -1, -1 },
            { 4, 7, 11, 4, 11, 9, 9, 11, 10, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 5, 4, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 5, 4, 1, 5, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 5, 4, 8, 3, 5, 3, 1, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 10, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 0, 8, 1, 2, 10, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 2, 10, 5, 4, 2, 4, 0, 2, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 10, 5, 3, 2, 5, 3, 5, 4, 3, 4, 8, -1, -1, -1, -1 },
            { 9, 5, 4, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 11, 2, 0, 8, 11, 4, 9, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 5, 4, 0, 1, 5, 2, 3, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 1, 5, 2, 5, 8, 2, 8, 11, 4, 8, 5, -1, -1, -1, -1 },
            { 10, 3, 11, 10, 1, 3, 9, 5, 4, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 9, 5, 0, 8, 1, 8, 10, 1, 8, 11, 10, -1, -1, -1, -1 },
            { 5, 4, 0, 5, 0, 11, 5, 11, 10, 11, 0, 3, -1, -1, -1, -1 },
            { 5, 4, 8, 5, 8, 10, 10, 8, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 7, 8, 5, 7, 9, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 3, 0, 9, 5, 3, 5, 7, 3, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 7, 8, 0, 1, 7, 1, 5, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 7, 8, 9, 5, 7, 10, 1, 2, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 1, 2, 9, 5, 0, 5, 3, 0, 5, 7, 3, -1, -1, -1, -1 },
            { 8, 0, 2, 8, 2, 5, 8, 5, 7, 10, 5, 2, -1, -1, -1, -1 },
            { 2, 10, 5, 2, 5, 3, 3, 5, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 7, 9, 5, 7, 8, 9, 3, 11, 2, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 5, 7, 9, 7, 2, 9, 2, 0, 2, 7, 11, -1, -1, -1, -1 },
            { 2, 3, 11, 0, 1, 8, 1, 7, 8, 1, 5, 7, -1, -1, -1, -1 },
            { 11, 2, 1, 11, 1, 7, 7, 1, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 5, 8, 8, 5, 7, 10, 1, 3, 10, 3, 11, -1, -1, -1, -1 },
            { 5, 7, 0, 5, 0, 9, 7, 11, 0, 1, 0, 10, 11, 10, 0, -1 },
            { 11, 10, 0, 11, 0, 3, 10, 5, 0, 8, 0, 7, 5, 7, 0, -1 },
            { 11, 10, 5, 7, 11, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 3, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 0, 1, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 8, 3, 1, 9, 8, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 6, 5, 2, 6, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 6, 5, 1, 2, 6, 3, 0, 8, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 6, 5, 9, 0, 6, 0, 2, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 9, 8, 5, 8, 2, 5, 2, 6, 3, 2, 8, -1, -1, -1, -1 },
            { 2, 3, 11, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 11, 0, 8, 11, 2, 0, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 1, 9, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 10, 6, 1, 9, 2, 9, 11, 2, 9, 8, 11, -1, -1, -1, -1 },
            { 6, 3, 11, 6, 5, 3, 5, 1, 3, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 11, 0, 11, 5, 0, 5, 1, 5, 11, 6, -1, -1, -1, -1 },
            { 3, 11, 6, 0, 3, 6, 0, 6, 5, 0, 5, 9, -1, -1, -1, -1 },
            { 6, 5, 9, 6, 9, 11, 11, 9, 8, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 10, 6, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 3, 0, 4, 7, 3, 6, 5, 10, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 9, 0, 5, 10, 6, 8, 4, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 6, 5, 1, 9, 7, 1, 7, 3, 7, 9, 4, -1, -1, -1, -1 },
            { 6, 1, 2, 6, 5, 1, 4, 7, 8, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 5, 5, 2, 6, 3, 0, 4, 3, 4, 7, -1, -1, -1, -1 },
            { 8, 4, 7, 9, 0, 5, 0, 6, 5, 0, 2, 6, -1, -1, -1, -1 },
            { 7, 3, 9, 7, 9, 4, 3, 2, 9, 5, 9, 6, 2, 6, 9, -1 },
            { 3, 11, 2, 7, 8, 4, 10, 6, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 10, 6, 4, 7, 2, 4, 2, 0, 2, 7, 11, -1, -1, -1, -1 },
            { 0, 1, 9, 4, 7, 8, 2, 3, 11, 5, 10, 6, -1, -1, -1, -1 },
            { 9, 2, 1, 9, 11, 2, 9, 4, 11, 7, 11, 4, 5, 10, 6, -1 },
            { 8, 4, 7, 3, 11, 5, 3, 5, 1, 5, 11, 6, -1, -1, -1, -1 },
            { 5, 1, 11, 5, 11, 6, 1, 0, 11, 7, 11, 4, 0, 4, 11, -1 },
            { 0, 5, 9, 0, 6, 5, 0, 3, 6, 11, 6, 3, 8, 4, 7, -1 },
            { 6, 5, 9, 6, 9, 11, 4, 7, 9, 7, 11, 9, -1, -1, -1, -1 },
            { 10, 4, 9, 6, 4, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 10, 6, 4, 9, 10, 0, 8, 3, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 0, 1, 10, 6, 0, 6, 4, 0, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 3, 1, 8, 1, 6, 8, 6, 4, 6, 1, 10, -1, -1, -1, -1 },
            { 1, 4, 9, 1, 2, 4, 2, 6, 4, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 0, 8, 1, 2, 9, 2, 4, 9, 2, 6, 4, -1, -1, -1, -1 },
            { 0, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 3, 2, 8, 2, 4, 4, 2, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 4, 9, 10, 6, 4, 11, 2, 3, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 2, 2, 8, 11, 4, 9, 10, 4, 10, 6, -1, -1, -1, -1 },
            { 3, 11, 2, 0, 1, 6, 0, 6, 4, 6, 1, 10, -1, -1, -1, -1 },
            { 6, 4, 1, 6, 1, 10, 4, 8, 1, 2, 1, 11, 8, 11, 1, -1 },
            { 9, 6, 4, 9, 3, 6, 9, 1, 3, 11, 6, 3, -1, -1, -1, -1 },
            { 8, 11, 1, 8, 1, 0, 11, 6, 1, 9, 1, 4, 6, 4, 1, -1 },
            { 3, 11, 6, 3, 6, 0, 0, 6, 4, -1, -1, -1, -1, -1, -1, -1 },
            { 6, 4, 8, 11, 6, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 7, 10, 6, 7, 8, 10, 8, 9, 10, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 7, 3, 0, 10, 7, 0, 9, 10, 6, 7, 10, -1, -1, -1, -1 },
            { 10, 6, 7, 1, 10, 7, 1, 7, 8, 1, 8, 0, -1, -1, -1, -1 },
            { 10, 6, 7, 10, 7, 1, 1, 7, 3, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 6, 1, 6, 8, 1, 8, 9, 8, 6, 7, -1, -1, -1, -1 },
            { 2, 6, 9, 2, 9, 1, 6, 7, 9, 0, 9, 3, 7, 3, 9, -1 },
            { 7, 8, 0, 7, 0, 6, 6, 0, 2, -1, -1, -1, -1, -1, -1, -1 },
            { 7, 3, 2, 6, 7, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 3, 11, 10, 6, 8, 10, 8, 9, 8, 6, 7, -1, -1, -1, -1 },
            { 2, 0, 7, 2, 7, 11, 0, 9, 7, 6, 7, 10, 9, 10, 7, -1 },
            { 1, 8, 0, 1, 7, 8, 1, 10, 7, 6, 7, 10, 2, 3, 11, -1 },
            { 11, 2, 1, 11, 1, 7, 10, 6, 1, 6, 7, 1, -1, -1, -1, -1 },
            { 8, 9, 6, 8, 6, 7, 9, 1, 6, 11, 6, 3, 1, 3, 6, -1 },
            { 0, 9, 1, 11, 6, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 7, 8, 0, 7, 0, 6, 3, 11, 0, 11, 6, 0, -1, -1, -1, -1 },
            { 7, 11, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 0, 8, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 1, 9, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 1, 9, 8, 3, 1, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 1, 2, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 10, 3, 0, 8, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 9, 0, 2, 10, 9, 6, 11, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 6, 11, 7, 2, 10, 3, 10, 8, 3, 10, 9, 8, -1, -1, -1, -1 },
            { 7, 2, 3, 6, 2, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 7, 0, 8, 7, 6, 0, 6, 2, 0, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 7, 6, 2, 3, 7, 0, 1, 9, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 6, 2, 1, 8, 6, 1, 9, 8, 8, 7, 6, -1, -1, -1, -1 },
            { 10, 7, 6, 10, 1, 7, 1, 3, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 7, 6, 1, 7, 10, 1, 8, 7, 1, 0, 8, -1, -1, -1, -1 },
            { 0, 3, 7, 0, 7, 10, 0, 10, 9, 6, 10, 7, -1, -1, -1, -1 },
            { 7, 6, 10, 7, 10, 8, 8, 10, 9, -1, -1, -1, -1, -1, -1, -1 },
            { 6, 8, 4, 11, 8, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 6, 11, 3, 0, 6, 0, 4, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 6, 11, 8, 4, 6, 9, 0, 1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 4, 6, 9, 6, 3, 9, 3, 1, 11, 3, 6, -1, -1, -1, -1 },
            { 6, 8, 4, 6, 11, 8, 2, 10, 1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 10, 3, 0, 11, 0, 6, 11, 0, 4, 6, -1, -1, -1, -1 },
            { 4, 11, 8, 4, 6, 11, 0, 2, 9, 2, 10, 9, -1, -1, -1, -1 },
            { 10, 9, 3, 10, 3, 2, 9, 4, 3, 11, 3, 6, 4, 6, 3, -1 },
            { 8, 2, 3, 8, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 4, 2, 4, 6, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 9, 0, 2, 3, 4, 2, 4, 6, 4, 3, 8, -1, -1, -1, -1 },
            { 1, 9, 4, 1, 4, 2, 2, 4, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 1, 3, 8, 6, 1, 8, 4, 6, 6, 10, 1, -1, -1, -1, -1 },
            { 10, 1, 0, 10, 0, 6, 6, 0, 4, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 6, 3, 4, 3, 8, 6, 10, 3, 0, 3, 9, 10, 9, 3, -1 },
            { 10, 9, 4, 6, 10, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 9, 5, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 3, 4, 9, 5, 11, 7, 6, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 0, 1, 5, 4, 0, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 11, 7, 6, 8, 3, 4, 3, 5, 4, 3, 1, 5, -1, -1, -1, -1 },
            { 9, 5, 4, 10, 1, 2, 7, 6, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 6, 11, 7, 1, 2, 10, 0, 8, 3, 4, 9, 5, -1, -1, -1, -1 },
            { 7, 6, 11, 5, 4, 10, 4, 2, 10, 4, 0, 2, -1, -1, -1, -1 },
            { 3, 4, 8, 3, 5, 4, 3, 2, 5, 10, 5, 2, 11, 7, 6, -1 },
            { 7, 2, 3, 7, 6, 2, 5, 4, 9, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 5, 4, 0, 8, 6, 0, 6, 2, 6, 8, 7, -1, -1, -1, -1 },
            { 3, 6, 2, 3, 7, 6, 1, 5, 0, 5, 4, 0, -1, -1, -1, -1 },
            { 6, 2, 8, 6, 8, 7, 2, 1, 8, 4, 8, 5, 1, 5, 8, -1 },
            { 9, 5, 4, 10, 1, 6, 1, 7, 6, 1, 3, 7, -1, -1, -1, -1 },
            { 1, 6, 10, 1, 7, 6, 1, 0, 7, 8, 7, 0, 9, 5, 4, -1 },
            { 4, 0, 10, 4, 10, 5, 0, 3, 10, 6, 10, 7, 3, 7, 10, -1 },
            { 7, 6, 10, 7, 10, 8, 5, 4, 10, 4, 8, 10, -1, -1, -1, -1 },
            { 6, 9, 5, 6, 11, 9, 11, 8, 9, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 6, 11, 0, 6, 3, 0, 5, 6, 0, 9, 5, -1, -1, -1, -1 },
            { 0, 11, 8, 0, 5, 11, 0, 1, 5, 5, 6, 11, -1, -1, -1, -1 },
            { 6, 11, 3, 6, 3, 5, 5, 3, 1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 10, 9, 5, 11, 9, 11, 8, 11, 5, 6, -1, -1, -1, -1 },
            { 0, 11, 3, 0, 6, 11, 0, 9, 6, 5, 6, 9, 1, 2, 10, -1 },
            { 11, 8, 5, 11, 5, 6, 8, 0, 5, 10, 5, 2, 0, 2, 5, -1 },
            { 6, 11, 3, 6, 3, 5, 2, 10, 3, 10, 5, 3, -1, -1, -1, -1 },
            { 5, 8, 9, 5, 2, 8, 5, 6, 2, 3, 8, 2, -1, -1, -1, -1 },
            { 9, 5, 6, 9, 6, 0, 0, 6, 2, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 5, 8, 1, 8, 0, 5, 6, 8, 3, 8, 2, 6, 2, 8, -1 },
            { 1, 5, 6, 2, 1, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 3, 6, 1, 6, 10, 3, 8, 6, 5, 6, 9, 8, 9, 6, -1 },
            { 10, 1, 0, 10, 0, 6, 9, 5, 0, 5, 6, 0, -1, -1, -1, -1 },
            { 0, 3, 8, 5, 6, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 5, 6, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 11, 5, 10, 7, 5, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 11, 5, 10, 11, 7, 5, 8, 3, 0, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 11, 7, 5, 10, 11, 1, 9, 0, -1, -1, -1, -1, -1, -1, -1 },
            { 10, 7, 5, 10, 11, 7, 9, 8, 1, 8, 3, 1, -1, -1, -1, -1 },
            { 11, 1, 2, 11, 7, 1, 7, 5, 1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 3, 1, 2, 7, 1, 7, 5, 7, 2, 11, -1, -1, -1, -1 },
            { 9, 7, 5, 9, 2, 7, 9, 0, 2, 2, 11, 7, -1, -1, -1, -1 },
            { 7, 5, 2, 7, 2, 11, 5, 9, 2, 3, 2, 8, 9, 8, 2, -1 },
            { 2, 5, 10, 2, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 2, 0, 8, 5, 2, 8, 7, 5, 10, 2, 5, -1, -1, -1, -1 },
            { 9, 0, 1, 5, 10, 3, 5, 3, 7, 3, 10, 2, -1, -1, -1, -1 },
            { 9, 8, 2, 9, 2, 1, 8, 7, 2, 10, 2, 5, 7, 5, 2, -1 },
            { 1, 3, 5, 3, 7, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 7, 0, 7, 1, 1, 7, 5, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 0, 3, 9, 3, 5, 5, 3, 7, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 8, 7, 5, 9, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 8, 4, 5, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1 },
            { 5, 0, 4, 5, 11, 0, 5, 10, 11, 11, 3, 0, -1, -1, -1, -1 },
            { 0, 1, 9, 8, 4, 10, 8, 10, 11, 10, 4, 5, -1, -1, -1, -1 },
            { 10, 11, 4, 10, 4, 5, 11, 3, 4, 9, 4, 1, 3, 1, 4, -1 },
            { 2, 5, 1, 2, 8, 5, 2, 11, 8, 4, 5, 8, -1, -1, -1, -1 },
            { 0, 4, 11, 0, 11, 3, 4, 5, 11, 2, 11, 1, 5, 1, 11, -1 },
            { 0, 2, 5, 0, 5, 9, 2, 11, 5, 4, 5, 8, 11, 8, 5, -1 },
            { 9, 4, 5, 2, 11, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 5, 10, 3, 5, 2, 3, 4, 5, 3, 8, 4, -1, -1, -1, -1 },
            { 5, 10, 2, 5, 2, 4, 4, 2, 0, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 10, 2, 3, 5, 10, 3, 8, 5, 4, 5, 8, 0, 1, 9, -1 },
            { 5, 10, 2, 5, 2, 4, 1, 9, 2, 9, 4, 2, -1, -1, -1, -1 },
            { 8, 4, 5, 8, 5, 3, 3, 5, 1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 4, 5, 1, 0, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 8, 4, 5, 8, 5, 3, 9, 0, 5, 0, 3, 5, -1, -1, -1, -1 },
            { 9, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 11, 7, 4, 9, 11, 9, 10, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 8, 3, 4, 9, 7, 9, 11, 7, 9, 10, 11, -1, -1, -1, -1 },
            { 1, 10, 11, 1, 11, 4, 1, 4, 0, 7, 4, 11, -1, -1, -1, -1 },
            { 3, 1, 4, 3, 4, 8, 1, 10, 4, 7, 4, 11, 10, 11, 4, -1 },
            { 4, 11, 7, 9, 11, 4, 9, 2, 11, 9, 1, 2, -1, -1, -1, -1 },
            { 9, 7, 4, 9, 11, 7, 9, 1, 11, 2, 11, 1, 0, 8, 3, -1 },
            { 11, 7, 4, 11, 4, 2, 2, 4, 0, -1, -1, -1, -1, -1, -1, -1 },
            { 11, 7, 4, 11, 4, 2, 8, 3, 4, 3, 2, 4, -1, -1, -1, -1 },
            { 2, 9, 10, 2, 7, 9, 2, 3, 7, 7, 4, 9, -1, -1, -1, -1 },
            { 9, 10, 7, 9, 7, 4, 10, 2, 7, 8, 7, 0, 2, 0, 7, -1 },
            { 3, 7, 10, 3, 10, 2, 7, 4, 10, 1, 10, 0, 4, 0, 10, -1 },
            { 1, 10, 2, 8, 7, 4, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 9, 1, 4, 1, 7, 7, 1, 3, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 9, 1, 4, 1, 7, 0, 8, 1, 8, 7, 1, -1, -1, -1, -1 },
            { 4, 0, 3, 7, 4, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 4, 8, 7, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 10, 8, 10, 11, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 0, 9, 3, 9, 11, 11, 9, 10, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 1, 10, 0, 10, 8, 8, 10, 11, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 1, 10, 11, 3, 10, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 2, 11, 1, 11, 9, 9, 11, 8, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 0, 9, 3, 9, 11, 1, 2, 9, 2, 11, 9, -1, -1, -1, -1 },
            { 0, 2, 11, 8, 0, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 3, 2, 11, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 3, 8, 2, 8, 10, 10, 8, 9, -1, -1, -1, -1, -1, -1, -1 },
            { 9, 10, 2, 0, 9, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 2, 3, 8, 2, 8, 10, 0, 1, 8, 1, 10, 8, -1, -1, -1, -1 },
            { 1, 10, 2, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 1, 3, 8, 9, 1, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 9, 1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { 0, 3, 8, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 },
            { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 } };

    public static java.util.List<Object3D> generateMesh(ScalarField grid, double isoLevel) {
        java.util.List<Object3D> chunks = java.util.Collections.synchronizedList(new ArrayList<>());

        int sizeX = grid.sizeX;
        int sizeY = grid.sizeY;
        int sizeZ = grid.sizeZ;

        int chunkSize = Settings.WorldSettings.CHUNK_SIZE;
        double spacing = Settings.WorldSettings.SPACING;

        double startX = -((sizeX - 1) * spacing) / 2.0;
        double startY = -((sizeY - 1) * spacing) / 2.0;
        double startZ = -((sizeZ - 1) * spacing) / 2.0;

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
                .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int cz = 0; cz < sizeZ - 1; cz += chunkSize) {
            for (int cx = 0; cx < sizeX - 1; cx += chunkSize) {

                final int chunkCz = cz;
                final int chunkCx = cx;

                executor.submit(() -> {
                    // Generate chunk
                    List<Data.Vertex> meshVerts = new ArrayList<>();
                    List<Data.Face> faceList = new ArrayList<>();
                    java.util.Map<VertexKey, Integer> vertexMap = new java.util.HashMap<>();

                    int endX = Math.min(chunkCx + chunkSize, sizeX - 1);
                    int endZ = Math.min(chunkCz + chunkSize, sizeZ - 1);

                    for (int z = chunkCz; z < endZ; z++) {
                        for (int y = 0; y < sizeY - 1; y++) {
                            for (int x = chunkCx; x < endX; x++) {

                                int[] vIdx = new int[8];
                                vIdx[0] = (x) + (y) * sizeX + (z) * sizeX * sizeY;
                                vIdx[1] = (x + 1) + (y) * sizeX + (z) * sizeX * sizeY;
                                vIdx[2] = (x + 1) + (y) * sizeX + (z + 1) * sizeX * sizeY;
                                vIdx[3] = (x) + (y) * sizeX + (z + 1) * sizeX * sizeY;
                                vIdx[4] = (x) + (y + 1) * sizeX + (z) * sizeX * sizeY;
                                vIdx[5] = (x + 1) + (y + 1) * sizeX + (z) * sizeX * sizeY;
                                vIdx[6] = (x + 1) + (y + 1) * sizeX + (z + 1) * sizeX * sizeY;
                                vIdx[7] = (x) + (y + 1) * sizeX + (z + 1) * sizeX * sizeY;

                                double[] val = new double[8];
                                for (int i = 0; i < 8; i++) {
                                    val[i] = grid.temperature[vIdx[i]];
                                }

                                int cubeIndex = 0;
                                if (val[0] < isoLevel)
                                    cubeIndex |= 1;
                                if (val[1] < isoLevel)
                                    cubeIndex |= 2;
                                if (val[2] < isoLevel)
                                    cubeIndex |= 4;
                                if (val[3] < isoLevel)
                                    cubeIndex |= 8;
                                if (val[4] < isoLevel)
                                    cubeIndex |= 16;
                                if (val[5] < isoLevel)
                                    cubeIndex |= 32;
                                if (val[6] < isoLevel)
                                    cubeIndex |= 64;
                                if (val[7] < isoLevel)
                                    cubeIndex |= 128;

                                if (edgeTable[cubeIndex] == 0)
                                    continue;

                                double wx = startX + x * spacing;
                                double wy = startY + y * spacing;
                                double wz = startZ + z * spacing;
                                double wx1 = wx + spacing;
                                double wy1 = wy + spacing;
                                double wz1 = wz + spacing;

                                double[][] norms = new double[8][3];
                                int[][] vIdxXYZ = new int[][]{
                                    {x, y, z}, {x+1, y, z}, {x+1, y, z+1}, {x, y, z+1},
                                    {x, y+1, z}, {x+1, y+1, z}, {x+1, y+1, z+1}, {x, y+1, z+1}
                                };
                                for (int i = 0; i < 8; i++) {
                                    getNormalAtGridPoint(grid, vIdxXYZ[i][0], vIdxXYZ[i][1], vIdxXYZ[i][2], norms[i]);
                                }

                                Vertex[] vertlist = new Vertex[12];
                                if ((edgeTable[cubeIndex] & 1) != 0)
                                    vertlist[0] = VertexInterp(isoLevel, wx, wy, wz, norms[0], wx1, wy, wz, norms[1], val[0], val[1]);
                                if ((edgeTable[cubeIndex] & 2) != 0)
                                    vertlist[1] = VertexInterp(isoLevel, wx1, wy, wz, norms[1], wx1, wy, wz1, norms[2], val[1], val[2]);
                                if ((edgeTable[cubeIndex] & 4) != 0)
                                    vertlist[2] = VertexInterp(isoLevel, wx1, wy, wz1, norms[2], wx, wy, wz1, norms[3], val[2], val[3]);
                                if ((edgeTable[cubeIndex] & 8) != 0)
                                    vertlist[3] = VertexInterp(isoLevel, wx, wy, wz1, norms[3], wx, wy, wz, norms[0], val[3], val[0]);
                                if ((edgeTable[cubeIndex] & 16) != 0)
                                    vertlist[4] = VertexInterp(isoLevel, wx, wy1, wz, norms[4], wx1, wy1, wz, norms[5], val[4], val[5]);
                                if ((edgeTable[cubeIndex] & 32) != 0)
                                    vertlist[5] = VertexInterp(isoLevel, wx1, wy1, wz, norms[5], wx1, wy1, wz1, norms[6], val[5], val[6]);
                                if ((edgeTable[cubeIndex] & 64) != 0)
                                    vertlist[6] = VertexInterp(isoLevel, wx1, wy1, wz1, norms[6], wx, wy1, wz1, norms[7], val[6], val[7]);
                                if ((edgeTable[cubeIndex] & 128) != 0)
                                    vertlist[7] = VertexInterp(isoLevel, wx, wy1, wz1, norms[7], wx, wy1, wz, norms[4], val[7], val[4]);
                                if ((edgeTable[cubeIndex] & 256) != 0)
                                    vertlist[8] = VertexInterp(isoLevel, wx, wy, wz, norms[0], wx, wy1, wz, norms[4], val[0], val[4]);
                                if ((edgeTable[cubeIndex] & 512) != 0)
                                    vertlist[9] = VertexInterp(isoLevel, wx1, wy, wz, norms[1], wx1, wy1, wz, norms[5], val[1], val[5]);
                                if ((edgeTable[cubeIndex] & 1024) != 0)
                                    vertlist[10] = VertexInterp(isoLevel, wx1, wy, wz1, norms[2], wx1, wy1, wz1, norms[6], val[2], val[6]);
                                if ((edgeTable[cubeIndex] & 2048) != 0)
                                    vertlist[11] = VertexInterp(isoLevel, wx, wy, wz1, norms[3], wx, wy1, wz1, norms[7], val[3], val[7]);

                                for (int i = 0; triTable[cubeIndex][i] != -1; i += 3) {
                                    Vertex v1 = vertlist[triTable[cubeIndex][i]];
                                    Vertex v2 = vertlist[triTable[cubeIndex][i + 1]];
                                    Vertex v3 = vertlist[triTable[cubeIndex][i + 2]];

                                    int i1 = getOrAddVertex(v1, meshVerts, vertexMap);
                                    int i2 = getOrAddVertex(v2, meshVerts, vertexMap);
                                    int i3 = getOrAddVertex(v3, meshVerts, vertexMap);

                                    Edge e1 = new Edge(i1, i2);
                                    Edge e2 = new Edge(i2, i3);
                                    Edge e3 = new Edge(i3, i1);

                                    // Face no longer needs a color, RenderHandler will interpolate Vertex colors
                                    faceList.add(new Face(new Edge[] { e1, e2, e3 }, Color.WHITE));

                                    // Face no longer needs a color, RenderHandler will interpolate Vertex colors
                                    faceList.add(new Face(new Edge[] { e1, e2, e3 }, Color.WHITE));
                                }
                            }
                        }
                    }
                    if (!faceList.isEmpty()) {
                        Object3D chunkObj = new Object3D(meshVerts.toArray(new Vertex[0]), faceList.toArray(new Face[0]));
                        chunkObj.isTerrain = true;
                        chunks.add(chunkObj);
                    }
                });
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }

        // Pass 2: Stitch Outer Walls to World Bounds
        double endX = startX + (sizeX - 1) * spacing;
        double endY = startY + (sizeY - 1) * spacing;
        double endZ = startZ + (sizeZ - 1) * spacing;
        
        // A distance of spacing * 1.01 guarantees we catch EVERY vertex generated between 
        // the outer edge (cell 0) and the adjacent grid cell (cell 1).
        double snapDist = spacing * 1.01;
        
        for (Object3D chunk : chunks) {
            for (Vertex v : chunk.vertices) {
                if (v.x < startX + snapDist) v.x = startX;
                if (v.x > endX - snapDist) v.x = endX;
                if (v.y < startY + snapDist) v.y = startY;
                if (v.y > endY - snapDist) v.y = endY;
                if (v.z < startZ + snapDist) v.z = startZ;
                if (v.z > endZ - snapDist) v.z = endZ;
            }
        }

        return chunks;
    }

    private static class VertexKey {
        double x, y, z;

        public VertexKey(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            VertexKey k = (VertexKey) o;
            return Double.compare(x, k.x) == 0 && Double.compare(y, k.y) == 0 && Double.compare(z, k.z) == 0;
        }

        @Override
        public int hashCode() {
            return Double.hashCode(x) ^ Integer.rotateLeft(Double.hashCode(y), 11)
                    ^ Integer.rotateLeft(Double.hashCode(z), 22);
        }
    }

    private static int getOrAddVertex(Vertex v, List<Vertex> meshVerts, java.util.Map<VertexKey, Integer> vertexMap) {
        VertexKey key = new VertexKey(v.x, v.y, v.z);
        Integer existing = vertexMap.get(key);
        if (existing != null) {
            return existing;
        }
        int index = meshVerts.size();
        meshVerts.add(v);
        vertexMap.put(key, index);
        return index;
    }

    private static double getDensity(ScalarField grid, int x, int y, int z) {
        if (x < 0) x = 0; if (x >= grid.sizeX) x = grid.sizeX - 1;
        if (y < 0) y = 0; if (y >= grid.sizeY) y = grid.sizeY - 1;
        if (z < 0) z = 0; if (z >= grid.sizeZ) z = grid.sizeZ - 1;
        return grid.temperature[x + y * grid.sizeX + z * grid.sizeX * grid.sizeY];
    }

    private static void getNormalAtGridPoint(ScalarField grid, int x, int y, int z, double[] n) {
        // Gradient of the scalar field via central differences
        double dx = getDensity(grid, x + 1, y, z) - getDensity(grid, x - 1, y, z);
        double dy = getDensity(grid, x, y + 1, z) - getDensity(grid, x, y - 1, z);
        double dz = getDensity(grid, x, y, z + 1) - getDensity(grid, x, y, z - 1);
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 0) {
            n[0] = dx / len;
            n[1] = dy / len;
            n[2] = dz / len;
        } else {
            n[0] = 0; n[1] = 1; n[2] = 0;
        }
    }

    private static Vertex VertexInterp(double isolevel,
            double p1x, double p1y, double p1z, double[] n1,
            double p2x, double p2y, double p2z, double[] n2,
            double valp1, double valp2) {
            
        if (Math.abs(valp1 - valp2) < 0.00001) {
            Vertex v = new Vertex(p1x, p1y, p1z);
            v.nx = n1[0]; v.ny = n1[1]; v.nz = n1[2];
            return v;
        }

        double mu = (isolevel - valp1) / (valp2 - valp1);
        
        // Clamp mu to avoid microscopic triangles and degenerate normals (black spots)
        if (mu < 0.001) mu = 0.001;
        if (mu > 0.999) mu = 0.999;

        double x = Math.round((p1x + mu * (p2x - p1x)) * 1000.0) / 1000.0;
        double y = Math.round((p1y + mu * (p2y - p1y)) * 1000.0) / 1000.0;
        double z = Math.round((p1z + mu * (p2z - p1z)) * 1000.0) / 1000.0;
        
        double nx = n1[0] + mu * (n2[0] - n1[0]);
        double ny = n1[1] + mu * (n2[1] - n1[1]);
        double nz = n1[2] + mu * (n2[2] - n1[2]);
        
        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len; ny /= len; nz /= len;
        }

        Vertex v = new Vertex(x, y, z);
        v.nx = nx;
        v.ny = ny;
        v.nz = nz;
        return v;
    }

    private static Color getTerrainColor(double y) {
        // Sand starts exactly at WATER_LEVEL and goes up to peak (WATER_LEVEL + 50)
        double waterLevel = Settings.WorldSettings.WATER_LEVEL;
        double ratio = (y - waterLevel) / 50.0;
        if (ratio < 0)
            ratio = 0;
        if (ratio > 1)
            ratio = 1;

        int r, g, b;
        if (ratio < 0.15) {
            // Sand (237, 201, 175) to Green (34,139,34)
            double t = ratio / 0.15;
            r = (int) (237 + t * (34 - 237));
            g = (int) (201 + t * (139 - 201));
            b = (int) (175 + t * (34 - 175));
        } else if (ratio < 0.40) {
            // Green (34,139,34) to Brown (139,69,19)
            double t = (ratio - 0.15) / 0.25;
            r = (int) (34 + t * (139 - 34));
            g = (int) (139 + t * (69 - 139));
            b = (int) (34 + t * (19 - 34));
        } else if (ratio < 0.80) {
            // Brown (139,69,19) to Grey (110,110,110)
            double t = (ratio - 0.40) / 0.40;
            r = (int) (139 + t * (110 - 139));
            g = (int) (69 + t * (110 - 69));
            b = (int) (19 + t * (110 - 19));
        } else {
            // Grey (110,110,110) to White (255,255,255)
            double t = (ratio - 0.80) / 0.20;
            r = (int) (110 + t * (255 - 110));
            g = (int) (110 + t * (255 - 110));
            b = (int) (110 + t * (255 - 110));
        }
        return new Color(r, g, b);
    }
    
    public static java.util.List<int[]> getSurfaceCells(Core.World.ScalarField scalarField, double isoLevel) {
        java.util.List<int[]> surfaceCells = new java.util.ArrayList<>();
        int sizeX = scalarField.sizeX;
        int sizeY = scalarField.sizeY;
        int sizeZ = scalarField.sizeZ;
        
        // Skip the outer edges (buffer zone) to prevent objects from spawning on the boundary slopes
        for (int x = 3; x < sizeX - 4; x++) {
            for (int z = 3; z < sizeZ - 4; z++) {
                for (int y = sizeY - 2; y >= 0; y--) {
                    int idx0 = (x) + (y) * sizeX + (z) * sizeX * sizeY;
                    int idx1 = (x + 1) + (y) * sizeX + (z) * sizeX * sizeY;
                    int idx2 = (x + 1) + (y) * sizeX + (z + 1) * sizeX * sizeY;
                    int idx3 = (x) + (y) * sizeX + (z + 1) * sizeX * sizeY;
                    int idx4 = (x) + (y + 1) * sizeX + (z) * sizeX * sizeY;
                    int idx5 = (x + 1) + (y + 1) * sizeX + (z) * sizeX * sizeY;
                    int idx6 = (x + 1) + (y + 1) * sizeX + (z + 1) * sizeX * sizeY;
                    int idx7 = (x) + (y + 1) * sizeX + (z + 1) * sizeX * sizeY;
                    
                    double avgTemp = (scalarField.temperature[idx0] + 
                                      scalarField.temperature[idx1] + 
                                      scalarField.temperature[idx2] + 
                                      scalarField.temperature[idx3] + 
                                      scalarField.temperature[idx4] + 
                                      scalarField.temperature[idx5] + 
                                      scalarField.temperature[idx6] + 
                                      scalarField.temperature[idx7]) / 8.0;
                    
                    if (avgTemp < isoLevel) {
                        surfaceCells.add(new int[]{x, y, z});
                        break; // Found the highest cell with solid matter
                    }
                }
            }
        }
        return surfaceCells;
    }
    
    // Snaps an arbitrary (worldX, worldZ) coordinate to the exact Y height of the interpolated terrain mesh
    public static double getExactHeightAt(double worldX, double worldZ, double baseWorldY, Core.World.ScalarField grid, double isoLevel) {
        double spacing = Settings.WorldSettings.SPACING;
        double startX = -((grid.sizeX - 1) * spacing) / 2.0;
        double startY = -((grid.sizeY - 1) * spacing) / 2.0;
        double startZ = -((grid.sizeZ - 1) * spacing) / 2.0;

        double fgx = (worldX - startX) / spacing;
        double fgz = (worldZ - startZ) / spacing;
        
        int gx = (int) Math.floor(fgx);
        int gz = (int) Math.floor(fgz);
        double fx = fgx - gx;
        double fz = fgz - gz;

        // Clamp to valid range so vegetation exactly on the world border snaps to the edge height
        if (gx < 0) { gx = 0; fx = 0.0; }
        if (gx >= grid.sizeX - 1) { gx = grid.sizeX - 2; fx = 1.0; }
        if (gz < 0) { gz = 0; fz = 0.0; }
        if (gz >= grid.sizeZ - 1) { gz = grid.sizeZ - 2; fz = 1.0; }

        double fgy = (baseWorldY - startY) / spacing;
        int centerGy = (int) Math.floor(fgy);

        int sx = grid.sizeX;
        int sxy = grid.sizeX * grid.sizeY;

        // Scan a few cells up and down to find where the surface actually crosses the isoLevel.
        // On steep slopes, moving horizontally across a cell means the surface could be in an adjacent vertical cell!
        int startGy = Math.max(0, centerGy - 4);
        int endGy = Math.min(grid.sizeY - 2, centerGy + 4);
        
        for (int gy = startGy; gy <= endGy; gy++) {
            double v0 = grid.temperature[gx + gy * sx + gz * sxy];
            double v1 = grid.temperature[(gx+1) + gy * sx + gz * sxy];
            double v2 = grid.temperature[(gx+1) + gy * sx + (gz+1) * sxy];
            double v3 = grid.temperature[gx + gy * sx + (gz+1) * sxy];
            
            double v4 = grid.temperature[gx + (gy+1) * sx + gz * sxy];
            double v5 = grid.temperature[(gx+1) + (gy+1) * sx + gz * sxy];
            double v6 = grid.temperature[(gx+1) + (gy+1) * sx + (gz+1) * sxy];
            double v7 = grid.temperature[gx + (gy+1) * sx + (gz+1) * sxy];

            // Bilinear interpolate bottom and top faces at (fx, fz)
            double bottom01 = v0 + fx * (v1 - v0);
            double bottom32 = v3 + fx * (v2 - v3);
            double bottomVal = bottom01 + fz * (bottom32 - bottom01);
            
            double top45 = v4 + fx * (v5 - v4);
            double top76 = v7 + fx * (v6 - v7);
            double topVal = top45 + fz * (top76 - top45);

            // Check if the isoLevel falls between bottomVal and topVal
            boolean crosses = (bottomVal <= isoLevel && topVal >= isoLevel) || 
                              (bottomVal >= isoLevel && topVal <= isoLevel);
                              
            if (crosses && Math.abs(topVal - bottomVal) >= 0.0001) {
                double fy = (isoLevel - bottomVal) / (topVal - bottomVal);
                return startY + (gy + fy) * spacing;
            }
        }
        
        // Fallback if no exact crossing is found in the column
        return baseWorldY;
    }

    // Returns the normalized surface normal vector at the given world coordinates.
    // It approximates the normal by calculating the gradient of the scalar field.
    public static double[] getSurfaceNormalAt(double worldX, double worldY, double worldZ, Core.World.ScalarField grid) {
        double spacing = Settings.WorldSettings.SPACING;
        double startX = -((grid.sizeX - 1) * spacing) / 2.0;
        double startY = -((grid.sizeY - 1) * spacing) / 2.0;
        double startZ = -((grid.sizeZ - 1) * spacing) / 2.0;

        double fgx = (worldX - startX) / spacing;
        double fgy = (worldY - startY) / spacing;
        double fgz = (worldZ - startZ) / spacing;
        
        int gx = (int) Math.round(fgx);
        int gy = (int) Math.round(fgy);
        int gz = (int) Math.round(fgz);
        
        if (gx <= 0 || gx >= grid.sizeX - 2 || gz <= 0 || gz >= grid.sizeZ - 2 || gy <= 0 || gy >= grid.sizeY - 2) {
            return new double[]{0, 1, 0}; // Default straight up on chunk edges
        }

        int sx = grid.sizeX;
        int sxy = grid.sizeX * grid.sizeY;

        // Central difference gradient
        double dx = grid.temperature[(gx + 1) + gy * sx + gz * sxy] - grid.temperature[(gx - 1) + gy * sx + gz * sxy];
        double dy = grid.temperature[gx + (gy + 1) * sx + gz * sxy] - grid.temperature[gx + (gy - 1) * sx + gz * sxy];
        double dz = grid.temperature[gx + gy * sx + (gz + 1) * sxy] - grid.temperature[gx + gy * sx + (gz - 1) * sxy];

        // Normal points towards positive scalar values (Air)
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.0001) return new double[]{0, 1, 0};
        return new double[]{dx / len, dy / len, dz / len};
    }
    
    // Checks if the center of a given world coordinate falls inside the solid terrain
    public static boolean isSolidAt(double worldX, double worldY, double worldZ, Core.World.ScalarField grid, double isoLevel) {
        double spacing = Settings.WorldSettings.SPACING;
        double startX = -((grid.sizeX - 1) * spacing) / 2.0;
        double startY = -((grid.sizeY - 1) * spacing) / 2.0;
        double startZ = -((grid.sizeZ - 1) * spacing) / 2.0;

        int gx = (int) Math.floor((worldX - startX) / spacing);
        int gy = (int) Math.floor((worldY - startY) / spacing);
        int gz = (int) Math.floor((worldZ - startZ) / spacing);
        
        if (gx < 0 || gx >= grid.sizeX || gz < 0 || gz >= grid.sizeZ || gy < 0 || gy >= grid.sizeY) {
            return false;
        }
        
        return grid.temperature[gx + gy * grid.sizeX + gz * grid.sizeX * grid.sizeY] < isoLevel;
    }
    
    // action: 0 = Dig, 1 = Place, 2 = Flatten
    public static void modifyTerrain(Core.World.ScalarField scalarField, int cx, int cy, int cz, int size, int action) {
        int sizeX = scalarField.sizeX;
        int sizeY = scalarField.sizeY;
        int sizeZ = scalarField.sizeZ;
        
        // A single grid cell sits between vertices cx and cx+1
        for (int x = Math.max(0, cx); x <= Math.min(sizeX - 1, cx + size); x++) {
            for (int z = Math.max(0, cz); z <= Math.min(sizeZ - 1, cz + size); z++) {
                if (action == 1) {
                    // Place: Make the column completely solid from bottom up to cy + 1
                    for (int y = 0; y <= Math.min(sizeY - 1, cy + 1); y++) {
                        int idx = x + y * sizeX + z * sizeX * sizeY;
                        scalarField.temperature[idx] = -9999.0; // Solid
                    }
                } else if (action == 0) {
                    // Dig: Make the column completely empty from top down to cy
                    for (int y = sizeY - 1; y >= Math.max(0, cy); y--) {
                        int idx = x + y * sizeX + z * sizeX * sizeY;
                        scalarField.temperature[idx] = 9999.0; // Empty
                    }
                } else if (action == 2) {
                    // Flatten: Solid below cy, Empty above cy
                    for (int y = 0; y < sizeY; y++) {
                        int idx = x + y * sizeX + z * sizeX * sizeY;
                        if (y <= cy) {
                            scalarField.temperature[idx] = -9999.0; // Solid bottom
                        } else {
                            scalarField.temperature[idx] = 9999.0; // Empty top
                        }
                    }
                } else if (action == 3) {
                    // Tunnel: Hollow out a space for the road, leaving ground below and above intact
                    for (int y = Math.max(0, cy); y <= Math.min(sizeY - 1, cy + 2); y++) {
                        int idx = x + y * sizeX + z * sizeX * sizeY;
                        scalarField.temperature[idx] = 9999.0; // Empty for road clearance
                    }
                    if (cy > 0) {
                        int idx = x + (cy - 1) * sizeX + z * sizeX * sizeY;
                        scalarField.temperature[idx] = -9999.0; // Ensure solid ground under road
                    }
                }
            }
        }
    }
}
