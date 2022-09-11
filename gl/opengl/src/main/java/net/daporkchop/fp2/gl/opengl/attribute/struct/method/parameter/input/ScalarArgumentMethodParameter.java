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

package net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.input;

import lombok.Data;
import lombok.NonNull;
import net.daporkchop.fp2.gl.opengl.attribute.struct.method.parameter.MethodParameter;
import net.daporkchop.fp2.gl.opengl.attribute.struct.property.ComponentType;
import org.objectweb.asm.MethodVisitor;

import static net.daporkchop.lib.common.util.PValidation.*;

/**
 * @author DaPorkchop_
 */
@Data
public class ScalarArgumentMethodParameter implements MethodParameter {
    @NonNull
    protected final ComponentType componentType;
    protected final int argumentLvtIndex;

    @Override
    public int components() {
        return 1;
    }

    @Override
    public void load(@NonNull MethodVisitor mv, int structLvtIndexIn, int lvtIndexAllocatorIn, @NonNull LoadCallback callback) {
        callback.accept(structLvtIndexIn, lvtIndexAllocatorIn, (structLvtIndex, lvtIndexAllocator, componentIndex) -> {
            checkIndex(1, componentIndex);
            this.componentType.load(mv, this.argumentLvtIndex);
        });
    }
}
