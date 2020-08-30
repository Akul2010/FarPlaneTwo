/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-$today.year DaPorkchop_
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

//
//
// VERTEX ATTRIBUTES
//
//

layout(location = 0) in int in_state;
layout(location = 1) in vec2 in_light;
layout(location = 2) in vec3 in_color;
layout(location = 3) in vec2 in_light_water;
layout(location = 4) in vec3 in_color_water;

layout(location = 5) in dvec3 in_pos_low;
layout(location = 6) in dvec3 in_pos_high;
layout(location = 7) in float in_level_scale;

//
//
// OUTPUTS
//
//

out VS_OUT {
    vec3 pos;
    vec2 light;

    flat vec3 color;
    flat int state;
} vs_out;
