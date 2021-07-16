/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.mode.voxel.server;

import lombok.NonNull;
import net.daporkchop.fp2.config.FP2Config;
import net.daporkchop.fp2.mode.common.server.AbstractPlayerTracker;
import net.daporkchop.fp2.mode.heightmap.HeightmapPos;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.debug.FP2Debug.*;
import static net.daporkchop.fp2.util.Constants.*;
import static net.daporkchop.fp2.util.math.MathUtil.*;
import static net.daporkchop.lib.common.math.PMath.*;

/**
 * @author DaPorkchop_
 */
public class VoxelPlayerTracker extends AbstractPlayerTracker<VoxelPos, VoxelTile> {
    protected static boolean overlaps(int x0, int y0, int z0, int x1, int y1, int z1, int radius) {
        int dx = abs(x0 - x1);
        int dy = abs(y0 - y1);
        int dz = abs(z0 - z1);
        return dx <= radius && dy <= radius && dz <= radius;
    }

    public VoxelPlayerTracker(@NonNull VoxelWorld world) {
        super(world);
    }

    @Override
    protected void allPositions(@NonNull EntityPlayerMP player, double posX, double posY, double posZ, @NonNull Consumer<VoxelPos> callback) {
        final int dist = asrRound(FP2Config.renderDistance, T_SHIFT); //TODO: make it based on render distance
        final int playerX = floorI(posX);
        final int playerY = floorI(posY);
        final int playerZ = floorI(posZ);

        final int levels = FP2Config.maxLevels;
        final int d = asrRound(FP2Config.levelCutoffDistance, T_SHIFT) + TILE_PRELOAD_PADDING_RADIUS;

        VoxelPos[] positions = new VoxelPos[cb(d * 2 + 1)];

        for (int lvl = FP2_DEBUG && FP2Config.debug.skipLevel0 ? 1 : 0; lvl < levels; lvl++) {
            final int baseX = asrRound(playerX, T_SHIFT + lvl);
            final int baseY = asrRound(playerY, T_SHIFT + lvl);
            final int baseZ = asrRound(playerZ, T_SHIFT + lvl);

            int xMin = baseX - d;
            int xMax = baseX + d;
            int yMin = baseY - d;
            int yMax = baseY + d;
            int zMin = baseZ - d;
            int zMax = baseZ + d;

            for (int i = 0, x = xMin; x <= xMax; x++) {
                for (int y = yMin; y <= yMax; y++) {
                    for (int z = zMin; z <= zMax; z++) {
                        positions[i++] = new VoxelPos(lvl, x, y, z);
                    }
                }
            }

            Arrays.sort(positions, Comparator.comparingInt(new VoxelPos(lvl, baseX, baseY, baseZ)::manhattanDistance));
            for (VoxelPos pos : positions) {
                callback.accept(pos);
            }
        }

        Arrays.fill(positions, null);
    }

    @Override
    protected void deltaPositions(@NonNull EntityPlayerMP player, double oldX, double oldY, double oldZ, double newX, double newY, double newZ, @NonNull Consumer<VoxelPos> added, @NonNull Consumer<VoxelPos> removed) {
        final int dist = asrRound(FP2Config.renderDistance, T_SHIFT); //TODO: make it based on render distance
        final int oldPlayerX = floorI(oldX);
        final int oldPlayerY = floorI(oldY);
        final int oldPlayerZ = floorI(oldZ);
        final int newPlayerX = floorI(newX);
        final int newPlayerY = floorI(newY);
        final int newPlayerZ = floorI(newZ);

        final int levels = FP2Config.maxLevels;
        final int d = asrRound(FP2Config.levelCutoffDistance, T_SHIFT) + TILE_PRELOAD_PADDING_RADIUS;

        for (int lvl = FP2_DEBUG && FP2Config.debug.skipLevel0 ? 1 : 0; lvl < levels; lvl++) {
            final int oldBaseX = asrRound(oldPlayerX, T_SHIFT + lvl);
            final int oldBaseY = asrRound(oldPlayerY, T_SHIFT + lvl);
            final int oldBaseZ = asrRound(oldPlayerZ, T_SHIFT + lvl);
            final int newBaseX = asrRound(newPlayerX, T_SHIFT + lvl);
            final int newBaseY = asrRound(newPlayerY, T_SHIFT + lvl);
            final int newBaseZ = asrRound(newPlayerZ, T_SHIFT + lvl);

            if (oldBaseX == newBaseX && oldBaseY == newBaseY && oldBaseZ == newBaseZ) { //nothing changed, skip this level
                continue;
            }

            //removed positions
            for (int x = oldBaseX - d; x <= oldBaseX + d; x++) {
                for (int y = oldBaseY - d; y <= oldBaseY + d; y++) {
                    for (int z = oldBaseZ - d; z <= oldBaseZ + d; z++) {
                        if (!overlaps(x, y, z, newBaseX, newBaseY, newBaseZ, d)) {
                            removed.accept(new VoxelPos(lvl, x, y, z));
                        }
                    }
                }
            }

            //added positions
            for (int x = newBaseX - d; x <= newBaseX + d; x++) {
                for (int y = newBaseY - d; y <= newBaseY + d; y++) {
                    for (int z = newBaseZ - d; z <= newBaseZ + d; z++) {
                        if (!overlaps(x, y, z, oldBaseX, oldBaseY, oldBaseZ, d)) {
                            added.accept(new VoxelPos(lvl, x, y, z));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected boolean shouldTriggerUpdate(@NonNull EntityPlayerMP player, double oldX, double oldY, double oldZ, double newX, double newY, double newZ) {
        //compute distance² in 3D
        return sq(oldX - newX) + sq(oldY - newY) + sq(oldZ - newZ) >= UPDATE_TRIGGER_DISTANCE_SQUARED;
    }
}
