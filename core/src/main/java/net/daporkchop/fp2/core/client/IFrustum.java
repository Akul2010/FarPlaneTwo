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

package net.daporkchop.fp2.core.client;

import lombok.NonNull;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayTransform;
import net.daporkchop.fp2.gl.attribute.annotation.ArrayType;
import net.daporkchop.fp2.gl.attribute.annotation.Attribute;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarConvert;
import net.daporkchop.fp2.gl.attribute.annotation.ScalarType;

/**
 * A view frustum which can check for intersection with objects.
 *
 * @author DaPorkchop_
 */
public interface IFrustum {
    /**
     * Checks whether or not the given point is contained in this frustum.
     *
     * @param x the X coordinate of the point
     * @param y the Y coordinate of the point
     * @param z the Z coordinate of the point
     * @return whether or not the given point is contained in this frustum
     */
    boolean containsPoint(double x, double y, double z);

    /**
     * Checks whether or not the given axis-aligned bounding box intersects with this frustum.
     *
     * @param minX the minimum X coordinate of the bounding box
     * @param minY the minimum Y coordinate of the bounding box
     * @param minZ the minimum Z coordinate of the bounding box
     * @param maxX the maximum X coordinate of the bounding box
     * @param maxY the maximum Y coordinate of the bounding box
     * @param maxZ the maximum Z coordinate of the bounding box
     * @return whether or not the given axis-aligned bounding box intersects with this frustum
     */
    boolean intersectsBB(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

    /**
     * @return the clipping planes which define this view frustum
     */
    ClippingPlanes clippingPlanes();

    /**
     * @author DaPorkchop_
     */
    final class ClippingPlanes {
        public static final int PLANES_MAX = 10;

        @Attribute
        @ScalarType(convert = @ScalarConvert(ScalarConvert.Type.TO_UNSIGNED))
        public int clippingPlaneCount = 0;

        @Attribute
        public final float @ArrayType(length = PLANES_MAX * 4, transform = @ArrayTransform(value = ArrayTransform.Type.TO_VECTOR_ARRAY, vectorComponents = 4)) [] clippingPlanes = new float[PLANES_MAX * 4];

        public ClippingPlanes put(float x, float y, float z, float w) {
            assert this.clippingPlaneCount < PLANES_MAX : this.clippingPlaneCount;

            this.clippingPlanes[this.clippingPlaneCount * 4 + 0] = x;
            this.clippingPlanes[this.clippingPlaneCount * 4 + 1] = y;
            this.clippingPlanes[this.clippingPlaneCount * 4 + 2] = z;
            this.clippingPlanes[this.clippingPlaneCount * 4 + 3] = w;
            this.clippingPlaneCount++;

            return this;
        }

        public ClippingPlanes put(@NonNull float[] plane) {
            assert this.clippingPlaneCount < PLANES_MAX : this.clippingPlaneCount;
            assert plane.length == 4 : plane.length;

            System.arraycopy(plane, 0, this.clippingPlanes, this.clippingPlaneCount * 4, 4);
            this.clippingPlaneCount++;

            return this;
        }
    }
}
