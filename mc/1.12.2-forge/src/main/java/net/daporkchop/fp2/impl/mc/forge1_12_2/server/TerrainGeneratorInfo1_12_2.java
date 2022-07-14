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
 *
 */

package net.daporkchop.fp2.impl.mc.forge1_12_2.server;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.core.server.event.GetTerrainGeneratorEvent;
import net.daporkchop.fp2.core.server.world.level.IFarLevelServer;
import net.daporkchop.fp2.core.server.world.TerrainGeneratorInfo;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.server.world.level.FLevelServer1_12;
import net.minecraft.world.WorldServer;

import static net.daporkchop.fp2.core.FP2Core.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class TerrainGeneratorInfo1_12_2 implements TerrainGeneratorInfo {
    @NonNull
    protected final FLevelServer1_12 level;
    @NonNull
    protected final WorldServer world;

    @Override
    public FLevelServer1_12 world() {
        return this.level;
    }

    @Override
    public Object implGenerator() {
        return fp2().eventBus().fireAndGetFirst(new GetTerrainGeneratorEvent(this.world())).get();
    }

    @Override
    public String generator() {
        return this.world.getWorldInfo().getTerrainType().getName();
    }

    @Override
    public String options() {
        return this.world.getWorldInfo().getGeneratorOptions();
    }

    @Override
    public long seed() {
        return this.world.getSeed();
    }
}
