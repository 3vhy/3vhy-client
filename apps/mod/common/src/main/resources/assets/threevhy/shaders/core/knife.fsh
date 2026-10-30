#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <threevhy:knife_lib.glsl>
#moj_import <threevhy:knife_surface.glsl>

in vec3 worldPos;
in vec3 worldNormal;
in vec3 viewPos;
in vec3 viewNormal;
in vec2 pattern;
in float edge;
flat in ivec4 info;
flat in vec3 frameInfo;
in vec4 lightMapColor;

out vec4 fragColor;

void main() {
    int material = info.x;
    int finish = info.y;
    int seedBits = info.z;
    int knife = info.w;
    float seed = float(seedBits) / 1023.0;
    float glint = frameInfo.x;
    float wear = frameInfo.y;
    float boost = frameInfo.z;
    vec2 p = pattern;

    if (material < M_GLOVE) {
        vec4 hole = HOLE[clamp(knife, 0, 23)];
        if (hole.z > 0.0) {
            vec2 d = (p - hole.xy) / hole.zw;
            if (dot(d, d) < 1.0) discard;
        }
    }

    vec3 nv3 = normalize(viewNormal);
    vec3 nw = normalize(worldNormal);
    vec3 v = normalize(-viewPos);
    vec3 vw = normalize(-worldPos);
    float t = k_bladeT(knife, p);

    Surface s;
    float paint = 0.0;
    bool finished = material == M_BLADE || material == M_EDGE || material == M_GUARD || material == M_RING || material == M_HANDLE_PAINT;
    if (material >= M_GLOVE) {
        s = k_glove(material, finish, seedBits & 7, p);
    } else if (material == M_ENERGY) {
        Surface f = k_finish(finish == 0 ? 23 : finish, p, t, edge, seed, paint);
        s = k_surface(vec3(0.0), 0.0, 1.0);
        s.emissive = mix(f.albedo * 1.4, vec3(1.0), 0.35 + 0.65 * edge);
        paint = 0.0;
    } else if (finished) {
        s = k_finish(finish, p, t, edge, seed, paint);
        if (material == M_EDGE) {
            s.rough *= 0.55;
            s.albedo = mix(s.albedo, vec3(0.92, 0.93, 0.95), 0.18);
            s.metal = max(s.metal, 0.7);
            paint *= 0.6;
        }
        if (material == M_GUARD || material == M_RING) s.aniso = 0.0;
        k_wear(s, p, edge, t, wear, paint, seed);
    } else {
        s = k_knifePart(material, p, finish);
    }

    vec3 dpdx = dFdx(viewPos);
    vec3 dpdy = dFdy(viewPos);
    vec2 dtdx = dFdx(p);
    vec2 dtdy = dFdy(p);
    float det = dtdx.y * dtdy.x - dtdy.y * dtdx.x;
    vec3 tangent = (dpdx * dtdy.x - dpdy * dtdx.x) / (abs(det) > 1e-9 ? det : 1e-9);
    tangent -= nv3 * dot(tangent, nv3);
    tangent = length(tangent) > 1e-6 ? normalize(tangent) : vec3(0.0, 1.0, 0.0);

    vec3 key = normalize(vec3(-0.5, 0.72, 0.48));
    vec3 back = normalize(vec3(0.75, 0.2, -0.62));
    float nv = clamp(dot(nv3, v), 0.0, 1.0);
    float nl = max(dot(nv3, key), 0.0);
    float nb = max(dot(nv3, back), 0.0);
    vec3 h = normalize(key + v);
    float nh = max(dot(nv3, h), 0.0);
    float d = k_ggx(nh, s.rough);
    if (s.aniso > 0.0) {
        float th = dot(tangent, h);
        float streak = pow(sqrt(max(1.0 - th * th, 0.0)), mix(18.0, 150.0, 1.0 - s.rough)) * mix(1.2, 3.0, 1.0 - s.rough);
        d = mix(d, streak, s.aniso);
    }
    vec3 f0 = mix(vec3(0.04), s.albedo, s.metal);
    vec3 fresnel = f0 + (max(vec3(1.0 - s.rough), f0) - f0) * pow(1.0 - nv, 5.0);
    vec3 r = reflect(-vw, nw);
    vec3 env = mix(k_environment(r, s.rough), vec3(0.5, 0.5, 0.52), s.rough * 0.55);

    vec3 lightIn = vec3(0.34) + vec3(1.0, 0.97, 0.92) * nl * 0.82 + vec3(0.55, 0.62, 0.78) * nb * 0.28 + vec3(0.2, 0.22, 0.26) * max(nw.y, 0.0) * 0.35;
    vec3 diffuse = s.albedo * (1.0 - s.metal) * lightIn;
    vec3 specular = fresnel * env + fresnel * d * nl * vec3(1.0, 0.97, 0.92) * 0.7;
    vec3 rim = vec3(0.75, 0.8, 0.9) * pow(1.0 - nv, 4.0) * (0.25 + nb * 0.6) * (1.0 - s.rough * 0.6);
    float coatFresnel = 0.04 + 0.96 * pow(1.0 - nv, 5.0);
    vec3 coat = s.coat * (coatFresnel * k_environment(r, 0.03) * 0.7 + k_ggx(nh, 0.05) * nl * 0.08);
    vec3 color = (diffuse + specular) * s.ao + rim + coat + s.emissive;

    if (glint > 0.0 && material <= M_RING) {
        float band = exp(-pow((t - glint) / 0.08, 2.0)) * (1.0 - s.rough * 0.8);
        color += vec3(1.7, 1.62, 1.5) * band;
    }

    vec3 light = mix(lightMapColor.rgb, vec3(1.0), boost);
    if (material == M_ENERGY) light = vec3(1.0);
    fragColor = vec4(k_softClip(color * light), 1.0);
}
