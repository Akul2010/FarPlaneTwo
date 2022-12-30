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

import lombok.NonNull;
import net.daporkchop.fp2.api.util.Direction;
import net.daporkchop.fp2.api.world.level.query.QuerySamplingMode;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionFilter;
import net.daporkchop.fp2.api.world.level.query.TypeTransitionSingleOutput;
import net.daporkchop.fp2.core.world.level.block.AbstractFBlockLevel;
import net.daporkchop.lib.common.annotation.param.NotNegative;

import java.util.List;

/**
 * @author DaPorkchop_
 */
public abstract class AbstractExactFBlockLevel<HOLDER extends AbstractExactFBlockLevelHolder<?>> extends AbstractFBlockLevel<HOLDER> {
    public AbstractExactFBlockLevel(@NonNull HOLDER holder) {
        super(holder);
    }

    @Override
    public int getNextTypeTransitions(@NonNull Direction direction, int x, int y, int z, long maxDistance,
                                      @NonNull List<@NonNull TypeTransitionFilter> filters,
                                      @NonNull TypeTransitionSingleOutput output,
                                      @NotNegative int sampleResolution, @NonNull QuerySamplingMode samplingMode) {
        //delegate to holder, using null as the prefetched world since this isn't a prefetched world
        return this.holder().getNextTypeTransitions(direction, x, y, z, maxDistance, filters, output, sampleResolution, samplingMode, null);
    }
}
