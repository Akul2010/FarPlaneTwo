/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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
 */

package net.daporkchop.fp2.core.minecraft.world;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.util.math.IntAxisAlignedBB;
import net.daporkchop.fp2.api.world.level.BlockLevelConstants;
import net.daporkchop.fp2.api.world.level.FBlockLevel;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.api.world.level.query.shape.PointsQueryShape;
import net.daporkchop.fp2.api.world.registry.FExtendedStateRegistryData;
import net.daporkchop.fp2.api.world.registry.FGameRegistry;
import net.daporkchop.fp2.core.server.world.ExactFBlockLevelHolder;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.lib.common.annotation.param.NotNegative;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.lang.Math.*;
import static net.daporkchop.fp2.core.util.GlobalAllocators.*;
import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Getter
public abstract class AbstractExactFBlockLevelHolder<PREFETCHED extends AbstractPrefetchedExactFBlockLevel> implements ExactFBlockLevelHolder {
    private final IFarLevelServer world;
    private final FGameRegistry registry;
    private final IntAxisAlignedBB dataLimits;

    public AbstractExactFBlockLevelHolder(@NonNull IFarLevelServer world) {
        this.world = world;
        this.registry = world.registry();
        this.dataLimits = world.coordLimits();
    }

    //
    // Generic coordinate utilities
    //

    /**
     * Checks whether the given Y coordinate is within the world's vertical limits.
     *
     * @param y the Y coordinate to check
     * @return whether the Y coordinate is valid
     */
    public boolean isValidY(int y) {
        return y >= this.dataLimits.minY() && y < this.dataLimits.maxY();
    }

    /**
     * Checks whether the given X,Z coordinates are within the world's horizontal limits.
     *
     * @param x the X coordinate of the position to check
     * @param z the Z coordinate of the position to check
     * @return whether the X,Z coordinates are valid
     */
    public boolean isValidXZ(int x, int z) {
        return x >= this.dataLimits.minX() && x < this.dataLimits.maxX() && z >= this.dataLimits.minZ() && z < this.dataLimits.maxZ();
    }

    /**
     * Checks whether the given point is within the world's limits.
     *
     * @param x the point's X coordinate
     * @param y the point's Y coordinate
     * @param z the point's Z coordinate
     * @return whether the given point is valid
     */
    public boolean isValidPosition(int x, int y, int z) {
        return this.dataLimits.contains(x, y, z);
    }

    //
    // Shared implementations of FBlockLevel's data availability accessors
    //

    /**
     * @see FBlockLevel#containsAnyData
     */
    public abstract boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    /**
     * @see FBlockLevel#guaranteedDataAvailableVolume
     */
    public abstract IntAxisAlignedBB guaranteedDataAvailableVolume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    //
    // Shared implementations of FBlockLevel's type transition search methods
    //

    /**
     * @param prefetchedLevel the existing {@link PREFETCHED prefetched level} which the query is being executed in, or {@code null} if it is not being executed in a
     *                        prefetched level
     * @see FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode)
     */
    public final int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z, long maxDistance,
                                            @NonNull List<@NonNull TypeTransitionFilter> filters,
                                            @NonNull TypeTransitionSingleOutput output,
                                            @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode,
                                            PREFETCHED prefetchedLevel) {
        output.validate(); //this could potentially help JIT?

        if (notNegative(maxDistance, "maxDistance") == 0L) { //already reached the maximum search distance
            return 0;
        }

        final int outputCount = output.count();
        if (outputCount <= 0) { //we've already run out of output space lol
            return 0;
        }

        //ensure that the filters list supports constant-time random access
        if (!(filters instanceof RandomAccess)) {
            filters = new ArrayList<>(filters);
        }

        //before going any further, let's check if any filter requests that we abort the query immediately
        for (TypeTransitionFilter filter : filters) {
            if (filter.shouldAbort(0)) {
                return 0;
            }
        }

        final IntAxisAlignedBB dataLimits = this.dataLimits();
        final FExtendedStateRegistryData extendedStateRegistryData = this.registry().extendedStateRegistryData();

        final int dx = direction.x();
        final int dy = direction.y();
        final int dz = direction.z();

        if (!BlockLevelConstants.willVectorIntersectAABB(dataLimits, x, y, z, direction, maxDistance)) {
            //the data limits will never be intersected, so there's nothing left to do
            return 0;
        } else if (!dataLimits.contains(x, y, z)) {
            //the starting position is outside the data limits, but the search will eventually reach the data limits. jump directly to the position one voxel before
            maxDistance -= BlockLevelConstants.jumpToExclusiveDistance(dataLimits, x, y, z, direction, maxDistance);
            int nextX = BlockLevelConstants.jumpXCoordinateToExclusiveAABB(dataLimits, x, y, z, direction);
            int nextY = BlockLevelConstants.jumpYCoordinateToExclusiveAABB(dataLimits, x, y, z, direction);
            int nextZ = BlockLevelConstants.jumpZCoordinateToExclusiveAABB(dataLimits, x, y, z, direction);

            assert !dataLimits.contains(nextX, nextY, nextZ)
                    : "jump: position should be outside the level's bounds";
            assert dataLimits.contains(addExact(nextX, direction.x()), addExact(nextY, direction.y()), addExact(nextZ, direction.z()))
                    : "jump: position+1 should be inside the level's bounds";

            x = nextX;
            y = nextY;
            z = nextZ;
        }

        //allocate a temporary array for storing the per-filter hit counts
        ArrayAllocator<int[]> alloc = ALLOC_INT.get();
        //TODO: int[] filterHitCounts = alloc.atLeast(filters.size());
        int[] filterHitCounts = new int[filters.size()];

        //now that we've done all the complex argument validation, delegate to the real implementation
        int writtenCount = this.getNextTypeTransitions(
                x, y, z, dx, dy, dz, maxDistance,
                dataLimits, filters, filterHitCounts, output, extendedStateRegistryData, sampleResolution, samplingMode, prefetchedLevel);

        //release the temporary array again
        //TODO: alloc.release(filterHitCounts);
        return writtenCount;
    }

    /**
     * The real implementation of {@link #getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, QuerySamplingMode, AbstractPrefetchedExactFBlockLevel)}, which
     * is called after all the arguments have been validated.
     *
     * @param x                         the X coordinate to begin iteration from
     * @param y                         the Y coordinate to begin iteration from
     * @param z                         the Z coordinate to begin iteration from
     * @param dx                        the iteration direction's step along the X axis. The value may be one of {@code 0}, {@code 1} or {@code -1}. Exactly one of
     *                                  {@code dx}, {@code dy} and {@code dz} will be non-zero; the other two will be {@code 0}.
     * @param dy                        the iteration direction's step along the Y axis. The value may be one of {@code 0}, {@code 1} or {@code -1}. Exactly one of
     *                                  {@code dx}, {@code dy} and {@code dz} will be non-zero; the other two will be {@code 0}.
     * @param dz                        the iteration direction's step along the Z axis. The value may be one of {@code 0}, {@code 1} or {@code -1}. Exactly one of
     *                                  {@code dx}, {@code dy} and {@code dz} will be non-zero; the other two will be {@code 0}.
     * @param maxDistance               the maximum number of voxels to iterate
     * @param dataLimits                this level's {@link FBlockLevel#dataLimits() data limits}
     * @param filters                   the {@link TypeTransitionFilter type transition filters} to use
     * @param filterHitCounts           an {@code int[]} to use for tracking the number of times each filter has been hit. The array's length is guaranteed to be greater
     *                                  than or equal to {@code filters.size()}.
     * @param output                    the {@link TypeTransitionSingleOutput output} to store the encountered type transitions in
     * @param extendedStateRegistryData this level's {@link FExtendedStateRegistryData}
     * @param sampleResolution          the sample resolution, as described in {@link FBlockLevel}
     * @param samplingMode              the {@link QuerySamplingMode sampling mode}, as described in {@link FBlockLevel}
     * @param prefetchedLevel           the existing {@link PREFETCHED prefetched level} which the query is being executed in, or {@code null} if it is not being executed
     *                                  in a prefetched level
     * @return the number of elements written to the {@link TypeTransitionSingleOutput output}
     * @see FBlockLevel#getNextTypeTransitions(Direction, int, int, int, long, List, TypeTransitionSingleOutput, int, net.daporkchop.fp2.api.world.level.query.QuerySamplingMode)
     */
    protected abstract int getNextTypeTransitions(int x, int y, int z, int dx, int dy, int dz, long maxDistance,
                                                  @NonNull IntAxisAlignedBB dataLimits,
                                                  @NonNull List<@NonNull TypeTransitionFilter> filters, @NonNull int[] filterHitCounts,
                                                  @NonNull TypeTransitionSingleOutput output,
                                                  @NonNull FExtendedStateRegistryData extendedStateRegistryData,
                                                  @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode,
                                                  PREFETCHED prefetchedLevel);

    protected static boolean checkTransitionFiltersResult_transitionMatches(int result) {
        assert (result & 3) == result : "invalid result value???";
        return (result & 1) != 0;
    }

    protected static boolean checkTransitionFiltersResult_abort(int result) {
        assert (result & 3) == result : "invalid result value???";
        return (result & 2) != 0;
    }

    protected static int checkTransitionFilters(int lastType, int nextType, List<@NonNull TypeTransitionFilter> filters, int[] filterHitCounts) {
        assert lastType != nextType;

        //check if the transition matches any of the provided filters. we iterate over every filter even if we find a match, because we need to increment
        // the hit count for EVERY filter that reported a match, even though we'll never write more than one value into the output.
        int out = 0;
        for (int filterIndex = 0; filterIndex < filters.size(); filterIndex++) {
            TypeTransitionFilter filter = filters.get(filterIndex);
            if (!filter.shouldDisable(filterHitCounts[filterIndex]) //the filter isn't disabled
                && filter.transitionMatches(lastType, nextType)) { //the type transition matches the filter!
                out |= 1; //transitionMatches = true;
                filterHitCounts[filterIndex]++;

                if (filter.shouldAbort(filterHitCounts[filterIndex])) { //the filter wants us to abort this query after we finish writing the current value
                    out |= 2; //abort = true;
                }
            }
        }
        return out;
    }

    //
    // Generic coordinate utilities, used when computing prefetching regions
    //

    protected boolean isAnyPointValid(@NonNull PointsQueryShape.OriginSizeStride shape) {
        return this.isAnyPointValid(shape.originX(), shape.sizeX(), shape.strideX(), this.dataLimits.minX(), this.dataLimits.maxX())
               && this.isAnyPointValid(shape.originY(), shape.sizeY(), shape.strideY(), this.dataLimits.minY(), this.dataLimits.maxY())
               && this.isAnyPointValid(shape.originZ(), shape.sizeZ(), shape.strideZ(), this.dataLimits.minZ(), this.dataLimits.maxZ());
    }

    protected boolean isAnyPointValid(int origin, int size, int stride, int min, int max) {
        //this could probably be implemented way faster, but i really don't care because this will never be called with a size larger than like 20

        for (int i = 0, pos = origin; i < size; i++, pos += stride) {
            if (pos >= min && pos < max) { //the point is valid
                return true;
            }
        }

        return false; //no points were valid
    }

    protected Consumer<IntConsumer> chunkCoordSupplier(int origin, int size, int stride, int min, int max, int chunkShift, int chunkSize) {
        if (stride >= chunkSize) {
            return callback -> {
                for (int i = 0, block = origin; i < size && block < max; i++, block += stride) {
                    if (block >= min) {
                        callback.accept(block >> chunkShift);
                    }
                }
            };
        } else {
            return callback -> {
                for (int chunk = max(origin, min) >> chunkShift, limit = min(origin + (size - 1) * stride, max) >> chunkShift; chunk <= limit; chunk++) {
                    callback.accept(chunk);
                }
            };
        }
    }
}
