#version 330

// What each part of a knife or glove looks like: the finish patterns, wear, and the glove designs.
// Materials, finishes, knives and gloves are numbered the same way as in KnifeShading.java.

#define M_BLADE 0
#define M_EDGE 1
#define M_GUARD 2
#define M_HARDWARE 3
#define M_RUBBER 4
#define M_POLYMER 5
#define M_WOOD 6
#define M_CORD 7
#define M_CARBON 8
#define M_RING 9
#define M_ENERGY 10
#define M_HANDLE_PAINT 11
#define M_GLOVE 16
#define M_GLOVE_ACCENT 17
#define M_PALM 18
#define M_ARMOR 19
#define M_SKIN 20
#define M_SLEEVE 21
#define M_WRAP 22
#define M_CUFF 23

struct Surface {
    vec3 albedo;
    float metal;
    float rough;
    float aniso;
    float coat;
    vec3 emissive;
    float ao;
};

Surface k_surface(vec3 albedo, float metal, float rough) {
    Surface s;
    s.albedo = albedo;
    s.metal = metal;
    s.rough = rough;
    s.aniso = 0.0;
    s.coat = 0.0;
    s.emissive = vec3(0.0);
    s.ao = 1.0;
    return s;
}

// Each knife's blade, from where it leaves the guard to its tip, in pixels along the knife; and a hole
// through it (centre and radii, in pixels; zero radius for none).
const vec2 BLADE_RANGE[24] = vec2[24](
    vec2(0.8, 5.4), vec2(1.2, 12.6), vec2(1.2, 12.4), vec2(0.2, 8.6),
    vec2(1.2, 9.4), vec2(1.2, 11.2), vec2(1.0, 9.0), vec2(1.2, 12.8),
    vec2(0.2, 10.0), vec2(0.2, 8.8), vec2(0.2, 10.2), vec2(0.3, 5.5),
    vec2(0.3, 9.6), vec2(1.0, 9.6), vec2(1.0, 10.4), vec2(0.6, 9.0),
    vec2(1.0, 9.6), vec2(1.2, 10.0), vec2(0.2, 5.0), vec2(0.95, 8.6),
    vec2(1.3, 10.3), vec2(1.2, 13.8), vec2(-0.5, 3.8), vec2(1.0, 10.0)
);
const vec4 HOLE[24] = vec4[24](
    vec4(0.0), vec4(-0.38, 8.35, 0.3, 0.56), vec4(0.0), vec4(0.0),
    vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0),
    vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0),
    vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0),
    vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0),
    vec4(0.0), vec4(0.0), vec4(0.0), vec4(0.0)
);

float k_bladeT(int knife, vec2 p) {
    vec2 range = BLADE_RANGE[clamp(knife, 0, 23)];
    return clamp((p.y - range.x) / max(range.y - range.x, 0.01), 0.0, 1.0);
}

vec3 k_ramp(float t, vec3 a, vec3 b, vec3 c, vec3 d) {
    t = clamp(t, 0.0, 1.0) * 3.0;
    if (t < 1.0) return mix(a, b, smoothstep(0.0, 1.0, t));
    if (t < 2.0) return mix(b, c, smoothstep(0.0, 1.0, t - 1.0));
    return mix(c, d, smoothstep(0.0, 1.0, t - 2.0));
}

// The finish on a painted or coated part. paint: 1 where the colour is a coat that wear scrapes off.
Surface k_finish(int finish, vec2 p, float t, float edge, float seed, out float paint) {
    vec2 sp = p + vec2(seed * 13.37, seed * 7.91);
    paint = 1.0;
    Surface s = k_surface(vec3(0.78, 0.8, 0.82), 1.0, 0.3);
    s.aniso = 0.0;
    if (finish == 1) {
        // Fade: yellow at the base through orange and pink to violet at the tip, across the blade at a slant.
        float f = t * 0.92 + (edge - 0.5) * 0.18 + (k_fbm3(p * 0.35) - 0.5) * 0.08;
        s.albedo = k_ramp(f, vec3(1.0, 0.86, 0.22), vec3(1.0, 0.5, 0.12), vec3(0.95, 0.24, 0.42), vec3(0.42, 0.18, 0.82));
        s.metal = 0.85;
        s.rough = 0.16;
        s.coat = 0.6;
    } else if (finish >= 2 && finish <= 5 || finish == 12 || finish == 13 || finish == 23) {
        // Doppler family: candy swirls with dark streaks, glossy and wet-looking.
        float n = k_swirl(sp * 0.22, 3.2);
        float streak = smoothstep(0.035, 0.0, abs(k_swirl(sp * 0.16 + 9.0, 2.6) - 0.52)) * 0.85;
        vec3 a;
        vec3 b;
        vec3 c;
        if (finish == 2) {
            a = vec3(0.22, 0.06, 0.38);
            b = vec3(0.62, 0.2, 0.75);
            c = vec3(1.0, 0.42, 0.8);
        } else if (finish == 3) {
            a = vec3(0.02, 0.06, 0.32);
            b = vec3(0.08, 0.32, 0.9);
            c = vec3(0.45, 0.8, 1.0);
        } else if (finish == 4) {
            a = vec3(0.25, 0.0, 0.03);
            b = vec3(0.72, 0.02, 0.1);
            c = vec3(1.0, 0.25, 0.3);
        } else if (finish == 5) {
            a = vec3(0.03, 0.02, 0.08);
            b = vec3(0.16, 0.12, 0.38);
            c = vec3(0.5, 0.42, 0.85);
        } else if (finish == 12) {
            a = vec3(0.02, 0.22, 0.12);
            b = vec3(0.12, 0.75, 0.35);
            c = vec3(0.5, 1.0, 0.75);
        } else if (finish == 13) {
            a = vec3(0.0, 0.18, 0.1);
            b = vec3(0.02, 0.55, 0.3);
            c = vec3(0.25, 0.95, 0.55);
        } else {
            a = vec3(0.18, 0.04, 0.42);
            b = vec3(0.55, 0.12, 0.95);
            c = vec3(0.92, 0.45, 1.0);
        }
        vec3 col = n < 0.5 ? mix(a, b, smoothstep(0.2, 0.5, n)) : mix(b, c, smoothstep(0.5, 0.8, n));
        if (finish == 2 || finish == 12) col = mix(col, finish == 2 ? vec3(0.25, 0.35, 0.95) : vec3(0.1, 0.55, 0.95), smoothstep(0.62, 0.8, k_fbm3(sp * 0.15 + 4.0)) * 0.5);
        if (finish == 23) col += vec3(0.6, 0.4, 0.8) * step(0.985, k_hash12(floor(p * 6.0))) * 0.8;
        s.albedo = mix(col, vec3(0.02, 0.015, 0.03), streak);
        s.metal = 0.92;
        s.rough = 0.09;
        s.coat = 1.0;
    } else if (finish == 6) {
        // Marble Fade: red, yellow and blue swirls flowing along the blade.
        float n = k_swirl(sp * 0.18, 2.4) + t * 0.65;
        float band = fract(n * 2.2);
        vec3 col = band < 0.33 ? mix(vec3(0.95, 0.12, 0.1), vec3(1.0, 0.82, 0.18), smoothstep(0.0, 0.33, band))
                : band < 0.66 ? mix(vec3(1.0, 0.82, 0.18), vec3(0.12, 0.3, 0.95), smoothstep(0.33, 0.66, band))
                : mix(vec3(0.12, 0.3, 0.95), vec3(0.95, 0.12, 0.1), smoothstep(0.66, 1.0, band));
        s.albedo = col;
        s.metal = 0.88;
        s.rough = 0.12;
        s.coat = 0.9;
    } else if (finish == 7) {
        // Crimson Web: deep red paint with a dark spider web spun across the blade.
        vec2 c = vec2(-0.6 + (seed - 0.5) * 2.0, 6.0 + seed * 3.0);
        vec2 d = p - c;
        float r = length(d);
        float a = atan(d.y, d.x);
        float spokes = 13.0;
        float wobble = k_noise(vec2(a * 3.0, r * 0.6) + seed * 5.0) * 0.25;
        float spoke = abs(fract(a / 6.28318 * spokes + wobble * 0.15) - 0.5) * 6.28318 / spokes * r;
        float sag = r * (1.0 + 0.1 * cos(a * spokes)) + wobble * 0.3;
        float ring = abs(fract(sag * 0.72) - 0.5) / 0.72;
        float line = min(spoke, ring);
        float web = 1.0 - smoothstep(0.05, 0.13, line);
        vec3 red = mix(vec3(0.42, 0.02, 0.03), vec3(0.62, 0.04, 0.05), k_fbm3(p * 0.6));
        s.albedo = mix(red, vec3(0.05, 0.01, 0.01), web * 0.92);
        s.metal = 0.15;
        s.rough = 0.42;
    } else if (finish == 8) {
        // Case Hardened: heat-tempered steel, blotched blue, violet, gold and brown, different every seed.
        float n1 = k_fbm(sp * 0.22);
        float n2 = k_fbm(sp * 0.5 + 3.1);
        float grain = k_noise(p * 9.0) * 0.12;
        vec3 steel = vec3(0.55, 0.52, 0.48);
        vec3 col = mix(steel, vec3(0.75, 0.58, 0.28), smoothstep(0.42, 0.3, n1));
        col = mix(col, vec3(0.1, 0.24, 0.72), smoothstep(0.55, 0.68, n1 + n2 * 0.25));
        col = mix(col, vec3(0.38, 0.18, 0.55), smoothstep(0.5, 0.62, n2) * smoothstep(0.62, 0.5, n1) * 0.8);
        col = mix(col, vec3(0.32, 0.22, 0.14), smoothstep(0.62, 0.75, k_fbm3(sp * 0.9 + 7.0)) * 0.6);
        s.albedo = col * (0.94 + grain);
        s.metal = 1.0;
        s.rough = 0.24;
        s.coat = 0.3;
        paint = 0.5;
    } else if (finish == 9) {
        // Tiger Tooth: gold metal with darker orange stripes across the blade.
        float stripe = sin((p.y * 1.05 + p.x * 0.35 + k_fbm3(p * 0.28 + seed * 3.0) * 3.2) * 2.4);
        float dark = smoothstep(0.35, 0.8, stripe);
        s.albedo = mix(vec3(1.0, 0.72, 0.2), vec3(0.86, 0.32, 0.04), dark);
        s.metal = 1.0;
        s.rough = 0.12;
        s.coat = 0.6;
    } else if (finish == 10) {
        // Slaughter: pink, red and orange splashes over red metal.
        vec3 cells = k_cells(sp * 0.42);
        float n = k_fbm3(sp * 0.7);
        vec3 col = mix(vec3(0.62, 0.08, 0.14), vec3(0.95, 0.42, 0.55), smoothstep(0.42, 0.2, cells.x + n * 0.25));
        col = mix(col, vec3(0.98, 0.4, 0.14), smoothstep(0.62, 0.8, cells.z) * smoothstep(0.35, 0.15, cells.x) * 0.9);
        col = mix(col, vec3(0.35, 0.02, 0.06), smoothstep(0.08, 0.0, cells.y - cells.x) * 0.7);
        s.albedo = col;
        s.metal = 0.85;
        s.rough = 0.18;
        s.coat = 0.6;
    } else if (finish == 11) {
        // Damascus Steel: dark folded steel in watery layers.
        float flow = p.x * 1.6 + k_fbm(p * vec2(0.22, 0.55) + seed * 4.0) * 7.0 + p.y * 0.12;
        float layers = sin(flow * 3.2) * 0.5 + 0.5;
        layers = pow(layers, 1.6);
        s.albedo = mix(vec3(0.12, 0.12, 0.13), vec3(0.62, 0.62, 0.64), layers);
        s.metal = 1.0;
        s.rough = 0.26;
        s.aniso = 0.3;
        paint = 0.0;
    } else if (finish == 14) {
        // Blue Steel: steel heat-blued in clouds.
        float n = k_fbm(sp * 0.3);
        s.albedo = mix(vec3(0.2, 0.3, 0.58), vec3(0.58, 0.62, 0.72), smoothstep(0.35, 0.7, n));
        s.albedo = mix(s.albedo, vec3(0.42, 0.3, 0.5), smoothstep(0.6, 0.75, k_fbm3(sp * 0.6)) * 0.5);
        s.metal = 1.0;
        s.rough = 0.2;
        paint = 0.3;
    } else if (finish == 0 || finish == 15) {
        // Stock and Stainless: plain brushed steel.
        s.albedo = finish == 15 ? vec3(0.86, 0.87, 0.89) : vec3(0.74, 0.76, 0.79);
        s.metal = 1.0;
        s.rough = finish == 15 ? 0.2 : 0.28;
        s.aniso = 0.85;
        paint = 0.0;
    } else if (finish == 16) {
        // Black Laminate: a black coated blade.
        s.albedo = vec3(0.045, 0.045, 0.05) * (0.9 + k_noise(p * 4.0) * 0.2);
        s.metal = 0.3;
        s.rough = 0.38;
    } else if (finish == 17) {
        // Night: dark grey with shadowy blotches.
        float n = k_fbm(sp * 0.35);
        s.albedo = mix(vec3(0.07, 0.07, 0.08), vec3(0.2, 0.2, 0.22), smoothstep(0.35, 0.7, n));
        s.metal = 0.55;
        s.rough = 0.36;
    } else if (finish == 18) {
        // Forest DDPAT: digital camouflage in greens and browns.
        vec2 cell = floor(p * 2.2);
        float n = k_fbm3(cell * 0.18 + seed * 6.0);
        vec3 col = n < 0.38 ? vec3(0.1, 0.11, 0.08) : n < 0.5 ? vec3(0.3, 0.22, 0.13) : n < 0.62 ? vec3(0.26, 0.33, 0.18) : vec3(0.43, 0.45, 0.3);
        s.albedo = col;
        s.metal = 0.05;
        s.rough = 0.62;
    } else if (finish == 19) {
        // Urban Masked: grey patches with hard, taped edges.
        vec2 q = floor(p * 0.6 + k_fbm3(p * 0.3) * 1.4);
        float n = k_hash12(q + seed * 9.0);
        vec3 col = n < 0.33 ? vec3(0.18, 0.19, 0.2) : n < 0.66 ? vec3(0.42, 0.43, 0.44) : vec3(0.64, 0.65, 0.66);
        s.albedo = col;
        s.metal = 0.08;
        s.rough = 0.55;
    } else if (finish == 20) {
        // Safari Mesh: sand paint under a dark net.
        float mesh = min(abs(fract((p.x + p.y) * 1.3) - 0.5), abs(fract((p.x - p.y) * 1.3) - 0.5));
        vec3 sand = mix(vec3(0.62, 0.52, 0.34), vec3(0.5, 0.42, 0.27), k_fbm3(p * 0.5));
        s.albedo = mix(vec3(0.12, 0.1, 0.07), sand, smoothstep(0.03, 0.09, mesh));
        s.metal = 0.05;
        s.rough = 0.6;
    } else if (finish == 21) {
        // Scorched: burnt patches of brown, rust and black.
        float n = k_fbm(sp * 0.45);
        vec3 col = mix(vec3(0.05, 0.04, 0.035), vec3(0.32, 0.2, 0.12), smoothstep(0.35, 0.55, n));
        col = mix(col, vec3(0.45, 0.36, 0.28), smoothstep(0.62, 0.75, n));
        s.albedo = col;
        s.metal = 0.1;
        s.rough = 0.66;
    } else if (finish == 22) {
        // Boreal Forest: soft woodland camouflage.
        float n1 = k_fbm(sp * 0.32);
        float n2 = k_fbm(sp * 0.32 + 11.0);
        vec3 col = vec3(0.33, 0.34, 0.24);
        col = mix(col, vec3(0.2, 0.24, 0.14), smoothstep(0.48, 0.55, n1));
        col = mix(col, vec3(0.3, 0.22, 0.14), smoothstep(0.55, 0.62, n2));
        col = mix(col, vec3(0.08, 0.08, 0.06), smoothstep(0.66, 0.72, n1 + n2 * 0.2));
        s.albedo = col;
        s.metal = 0.06;
        s.rough = 0.58;
    }
    return s;
}

// Scratches, scuffs and dirt for the wear slider (0 factory new .. 1 battle-scarred): painted coats wear
// off the edge, the tip and the spine first and show bare steel.
void k_wear(inout Surface s, vec2 p, float edge, float t, float wear, float paint, float seed) {
    if (wear <= 0.001) return;
    float n = k_fbm(p * 1.1 + seed * 3.0);
    float scratch = 0.0;
    vec2 q = mat2(0.92, 0.38, -0.38, 0.92) * p;
    float line = abs(fract(q.y * 2.3 + k_noise(q * vec2(0.2, 2.0)) * 2.0) - 0.5);
    scratch = (1.0 - smoothstep(0.0, 0.035, line)) * step(0.55, k_noise(q * vec2(0.6, 0.15) + 4.0));
    float exposure = n * 0.75 + edge * 0.55 + t * 0.25 + scratch * 0.5;
    float worn = smoothstep(1.25 - wear * 1.1, 1.32 - wear * 1.1, exposure) * paint;
    vec3 steel = vec3(0.6, 0.61, 0.62);
    s.albedo = mix(s.albedo, steel, worn);
    s.metal = mix(s.metal, 1.0, worn);
    s.rough = mix(s.rough, 0.36, worn);
    s.coat *= 1.0 - worn;
    float dirt = smoothstep(0.55, 0.85, k_fbm3(p * 0.8 + 11.0)) * wear * 0.45;
    s.albedo *= 1.0 - dirt;
    s.rough = mix(s.rough, 0.7, dirt);
    s.rough = mix(s.rough, s.rough + 0.12, wear * (1.0 - paint * 0.5));
}

// Glove and sleeve colours: each design has eight colourways.
vec3 k_glovePalette(int design, int variant, int slot) {
    int v = variant & 7;
    if (design == 1) {
        vec3 base[8] = vec3[8](vec3(0.24, 0.245, 0.25), vec3(0.42, 0.34, 0.24), vec3(0.22, 0.25, 0.16), vec3(0.06, 0.06, 0.065),
                vec3(0.2, 0.24, 0.3), vec3(0.5, 0.44, 0.33), vec3(0.3, 0.2, 0.15), vec3(0.14, 0.17, 0.14));
        vec3 leather[8] = vec3[8](vec3(0.62, 0.6, 0.55), vec3(0.7, 0.62, 0.48), vec3(0.5, 0.48, 0.36), vec3(0.3, 0.3, 0.31),
                vec3(0.55, 0.58, 0.6), vec3(0.78, 0.72, 0.6), vec3(0.55, 0.42, 0.32), vec3(0.42, 0.44, 0.38));
        return slot == 0 ? base[v] : slot == 1 ? base[v] * 0.55 : slot == 2 ? leather[v] : base[v] * 0.7;
    }
    if (design == 2) {
        vec3 a[8] = vec3[8](vec3(0.08, 0.26, 0.85), vec3(0.75, 0.06, 0.08), vec3(0.15, 0.6, 0.2), vec3(0.95, 0.38, 0.65),
                vec3(0.06, 0.06, 0.07), vec3(0.42, 0.12, 0.85), vec3(1.0, 0.55, 0.05), vec3(0.05, 0.7, 0.8));
        vec3 b[8] = vec3[8](vec3(0.92, 0.94, 0.97), vec3(0.08, 0.08, 0.09), vec3(0.95, 0.85, 0.15), vec3(0.95, 0.95, 0.97),
                vec3(0.85, 0.65, 0.22), vec3(0.2, 0.9, 0.95), vec3(0.1, 0.1, 0.12), vec3(0.95, 0.95, 0.97));
        return slot == 0 ? a[v] : slot == 1 ? b[v] : slot == 2 ? mix(b[v], a[v], 0.25) : a[v] * 0.5;
    }
    if (design == 3) {
        vec3 leather[8] = vec3[8](vec3(0.05, 0.045, 0.045), vec3(0.32, 0.17, 0.08), vec3(0.6, 0.42, 0.24), vec3(0.52, 0.06, 0.06),
                vec3(0.85, 0.83, 0.78), vec3(0.08, 0.1, 0.2), vec3(0.2, 0.22, 0.16), vec3(0.42, 0.3, 0.18));
        return slot == 1 ? leather[v] * 1.6 + 0.05 : leather[v];
    }
    if (design == 4) {
        vec3 wrap[8] = vec3[8](vec3(0.1, 0.42, 0.18), vec3(0.62, 0.06, 0.06), vec3(0.06, 0.06, 0.07), vec3(0.88, 0.86, 0.8),
                vec3(0.1, 0.22, 0.62), vec3(0.5, 0.36, 0.22), vec3(0.7, 0.55, 0.1), vec3(0.42, 0.1, 0.55));
        return slot == 0 ? vec3(0.06, 0.055, 0.055) : wrap[v];
    }
    if (design == 5) {
        vec3 a[8] = vec3[8](vec3(0.7, 0.06, 0.06), vec3(0.1, 0.3, 0.8), vec3(0.15, 0.55, 0.15), vec3(1.0, 0.45, 0.05),
                vec3(0.9, 0.9, 0.92), vec3(0.06, 0.06, 0.07), vec3(0.95, 0.85, 0.1), vec3(0.4, 0.4, 0.42));
        return slot == 0 ? a[v] : slot == 1 ? vec3(0.06, 0.06, 0.07) : slot == 2 ? vec3(0.1, 0.1, 0.11) : vec3(0.08, 0.08, 0.09);
    }
    if (design == 6) {
        vec3 a[8] = vec3[8](vec3(0.25, 0.3, 0.18), vec3(0.55, 0.05, 0.08), vec3(0.05, 0.45, 0.32), vec3(0.95, 0.5, 0.3),
                vec3(0.85, 0.88, 0.92), vec3(0.08, 0.08, 0.09), vec3(0.15, 0.18, 0.45), vec3(0.5, 0.4, 0.25));
        return slot == 0 ? a[v] : slot == 1 ? a[v] * 0.5 : slot == 2 ? vec3(0.08, 0.08, 0.085) : vec3(0.1, 0.1, 0.11);
    }
    if (design == 7) {
        vec3 a[8] = vec3[8](vec3(0.06, 0.06, 0.065), vec3(0.3, 0.18, 0.09), vec3(0.18, 0.28, 0.12), vec3(0.42, 0.26, 0.12),
                vec3(0.25, 0.25, 0.2), vec3(0.15, 0.1, 0.08), vec3(0.45, 0.42, 0.4), vec3(0.35, 0.05, 0.05));
        vec3 metal[8] = vec3[8](vec3(0.75), vec3(0.85, 0.65, 0.3), vec3(0.6, 0.62, 0.55), vec3(0.8, 0.5, 0.25),
                vec3(0.55), vec3(0.3), vec3(0.85), vec3(0.65, 0.6, 0.55));
        return slot == 3 ? metal[v] : slot == 1 ? a[v] * 1.5 : a[v];
    }
    vec3 skin[8] = vec3[8](vec3(0.78, 0.56, 0.42), vec3(0.62, 0.42, 0.3), vec3(0.42, 0.27, 0.18), vec3(0.88, 0.68, 0.55),
            vec3(0.3, 0.2, 0.14), vec3(0.72, 0.52, 0.36), vec3(0.55, 0.36, 0.26), vec3(0.95, 0.76, 0.64));
    return skin[v];
}

// A glove part: design 0 is bare hands; p runs along the part (x) and around it (y), in pixels.
Surface k_glove(int material, int design, int variant, vec2 p) {
    Surface s = k_surface(vec3(0.5), 0.0, 0.7);
    if (material == M_SKIN || design == 0 && material != M_SLEEVE && material != M_CUFF) {
        s.albedo = k_glovePalette(0, variant, 0) * (0.94 + k_noise(p * 3.0) * 0.08);
        s.rough = 0.62;
        return s;
    }
    if (material == M_SLEEVE) {
        vec3 jacket = design == 2 || design == 5 ? vec3(0.1, 0.11, 0.13) : design == 1 || design == 6 ? vec3(0.17, 0.19, 0.14) : vec3(0.07, 0.07, 0.08);
        float weave = k_noise(p * vec2(6.0, 6.0)) * 0.06 + k_fbm3(p * 0.5) * 0.08;
        s.albedo = jacket * (0.9 + weave);
        s.rough = 0.85;
        return s;
    }
    vec3 shell = k_glovePalette(design, variant, 0);
    vec3 accent = k_glovePalette(design, variant, 1);
    vec3 palm = k_glovePalette(design, variant, 2);
    vec3 armor = k_glovePalette(design, variant, 3);
    float fabric = k_noise(p * vec2(9.0, 9.0)) * 0.05 + k_noise(p * 2.0) * 0.04;
    if (material == M_GLOVE) {
        s.albedo = shell;
        if (design == 2) {
            float stripe = step(0.5, fract(p.y * 0.22 + 0.25)) * step(abs(fract(p.x * 0.11) - 0.5), 0.36);
            s.albedo = mix(shell, accent, stripe);
            s.rough = 0.55;
        } else if (design == 3) {
            float stitch = step(0.5, fract(p.x * 1.6)) * (1.0 - smoothstep(0.03, 0.07, abs(fract(p.y * 0.25) - 0.5)));
            s.albedo = mix(shell, accent, stitch * 0.8) * (0.92 + k_noise(p * 7.0) * 0.12);
            s.rough = 0.38;
            s.coat = 0.25;
        } else if (design == 4) {
            float band = smoothstep(0.42, 0.5, abs(fract(p.x * 0.5 + p.y * 0.18) - 0.5) * 2.0);
            s.albedo = mix(shell, accent, 1.0 - band * 0.25);
            s.rough = 0.8;
        } else if (design == 6) {
            float camo = k_fbm3(p * 0.35 + float(variant) * 4.0);
            s.albedo = mix(shell, shell * 0.55 + accent * 0.2, smoothstep(0.45, 0.55, camo));
            s.rough = 0.72;
        } else if (design == 7) {
            vec3 scales = k_cells(p * 1.4);
            s.albedo = shell * (0.75 + smoothstep(0.0, 0.25, scales.y - scales.x) * 0.4);
            s.rough = 0.45;
            s.coat = 0.2;
        } else {
            s.rough = design == 5 ? 0.5 : 0.78;
        }
        s.albedo *= 0.95 + fabric;
    } else if (material == M_GLOVE_ACCENT) {
        s.albedo = accent * (0.95 + fabric);
        s.rough = design == 3 || design == 7 ? 0.4 : 0.6;
    } else if (material == M_PALM) {
        s.albedo = palm * (0.9 + k_noise(p * 6.0) * 0.12);
        s.rough = design == 3 ? 0.4 : 0.75;
    } else if (material == M_ARMOR) {
        s.albedo = armor;
        s.rough = design == 5 ? 0.22 : design == 7 ? 0.2 : 0.5;
        s.metal = design == 7 ? 1.0 : 0.0;
        s.coat = design == 5 ? 0.8 : 0.0;
    } else if (material == M_WRAP) {
        float band = abs(fract(p.x * 0.42 + p.y * 0.12) - 0.5);
        s.albedo = accent * (0.82 + smoothstep(0.38, 0.5, band) * -0.3 + k_noise(p * vec2(10.0, 2.0)) * 0.12);
        s.rough = 0.85;
    } else if (material == M_CUFF) {
        s.albedo = design == 2 ? accent : design == 4 ? accent : shell * 0.7;
        s.albedo *= 0.92 + k_noise(p * 8.0) * 0.1;
        s.rough = 0.7;
    }
    return s;
}

// A non-finish knife part: rubber, polymer, wood, cord, carbon or hardware.
Surface k_knifePart(int material, vec2 p, int finish) {
    if (material == M_RUBBER) {
        float grip = k_noise(p * 8.0) * 0.08;
        return k_surface(vec3(0.07, 0.07, 0.075) * (1.0 + grip), 0.0, 0.62);
    }
    if (material == M_POLYMER) {
        Surface s = k_surface(vec3(0.09, 0.09, 0.1), 0.0, 0.3);
        s.coat = 0.5;
        return s;
    }
    if (material == M_WOOD) {
        float grain = sin((p.y * 1.8 + k_fbm3(p * vec2(0.6, 0.15)) * 6.0) * 3.0) * 0.5 + 0.5;
        vec3 wood = finish == 16 ? mix(vec3(0.35, 0.22, 0.12), vec3(0.08, 0.06, 0.05), step(0.5, fract(p.x * 1.6)))
                : mix(vec3(0.42, 0.24, 0.12), vec3(0.62, 0.4, 0.22), grain);
        Surface s = k_surface(wood, 0.0, 0.45);
        s.coat = 0.4;
        return s;
    }
    if (material == M_CORD) {
        float twist = abs(fract(p.x * 1.6 + p.y * 1.2) - 0.5);
        vec3 cord = finish == 18 || finish == 22 ? vec3(0.28, 0.3, 0.18) : finish == 7 || finish == 10 ? vec3(0.45, 0.04, 0.05) : vec3(0.1, 0.1, 0.11);
        return k_surface(cord * (0.65 + twist * 0.9), 0.0, 0.85);
    }
    if (material == M_CARBON) {
        vec2 w = floor(p * 3.0);
        float weave = mod(w.x + w.y, 2.0);
        Surface s = k_surface(mix(vec3(0.05), vec3(0.14), weave), 0.2, 0.25);
        s.coat = 0.8;
        return s;
    }
    Surface s = k_surface(vec3(0.3, 0.31, 0.33), 1.0, 0.32);
    return s;
}
