#version 330

#moj_import <eventide:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

vec3 planet(vec3 d, vec3 color, float t) {
	float a = 1.25 + sin(t * 0.004) * 0.35;
	vec3 p = normalize(vec3(cos(a) * 0.8, 0.46, sin(a) * 0.8));
	vec3 u = normalize(cross(vec3(0.0, 1.0, 0.0), p));
	vec3 v = cross(p, u);
	float radius = 0.11;
	if (dot(d, p) < 0.5) {
		return color;
	}
	vec2 q = vec2(dot(d, u), dot(d, v)) / radius;
	float r = length(q);
	float aa = fwidth(r) * 1.5;
	mat2 tilt = mat2(cos(-0.38), sin(-0.38), -sin(-0.38), cos(-0.38));
	vec2 rq = tilt * q;
	float rr = length(vec2(rq.x, rq.y / 0.26));
	float ringAa = fwidth(rr) * 1.5;
	float ring = smoothstep(1.32 - ringAa, 1.32 + ringAa, rr) * smoothstep(2.35 + ringAa, 2.35 - ringAa, rr);
	ring *= 0.55 + 0.45 * sin(rr * 26.0) * sin(rr * 7.0 + 1.0);
	ring *= 1.0 - 0.75 * smoothstep(1.86, 1.9, rr) * smoothstep(1.98, 1.94, rr);
	vec3 ringColor = vec3(0.86, 0.78, 0.98) * (0.55 + 0.45 * smoothstep(-1.0, 1.0, rq.x));
	float disc = smoothstep(1.0 + aa, 1.0 - aa, r);
	if (rq.y > 0.0) {
		color = mix(color, ringColor, ring * 0.85 * (1.0 - disc));
	}
	color += vec3(0.6, 0.35, 1.0) * smoothstep(1.35, 1.0, r) * (1.0 - disc) * 0.35;
	if (disc > 0.0) {
		vec3 n = normalize(q.x * u + q.y * v + sqrt(max(0.0, 1.0 - r * r)) * p);
		vec3 light = normalize(vec3(-0.55, 0.65, 0.4));
		float shade = clamp(dot(n, light), 0.0, 1.0) * 0.95 + 0.07;
		float band = noise2(vec2(q.y * 6.0 + 1.2 * fbm2(q * 3.0 + t * 0.01, 3), 3.0));
		float swirl = fbm2(q * 5.0 + vec2(band * 2.0, t * 0.02), 3);
		vec3 surface = mix(vec3(0.42, 0.36, 0.55), vec3(0.88, 0.85, 0.94), band * 0.7 + swirl * 0.4);
		color = mix(color, surface * shade, disc);
	}
	if (rq.y <= 0.0) {
		color = mix(color, ringColor * (disc > 0.5 ? 0.8 : 1.0), ring * 0.9);
	}
	vec3 moonDir = normalize(p + u * -0.26 + v * 0.1);
	float m = acos(clamp(dot(d, moonDir), -1.0, 1.0)) / 0.012;
	color = mix(color, vec3(0.75, 0.72, 0.82) * (0.6 + 0.4 * smoothstep(1.0, -1.0, dot(normalize(d - moonDir), u))), smoothstep(1.0, 0.8, m));
	return color;
}

float meteor(vec3 d, float t) {
	float slot = floor(t / 6.0);
	float phase = fract(t / 6.0);
	if (phase > 0.16) {
		return 0.0;
	}
	float s = hash12(vec2(slot, 4.1));
	float a = s * 6.2831;
	vec3 start = normalize(vec3(cos(a), 0.55 + 0.35 * hash12(vec2(slot, 9.3)), sin(a)));
	vec3 dir = normalize(cross(start, vec3(0.0, 1.0, 0.0)) + vec3(0.0, -0.35, 0.0));
	float k = phase / 0.16;
	vec3 head = normalize(start + dir * k * 0.55);
	vec3 tail = normalize(start + dir * max(0.0, k * 0.55 - 0.18));
	vec2 dp = dome(d, 0.3);
	vec2 hp = dome(head, 0.3);
	vec2 tp = dome(tail, 0.3);
	vec2 seg = hp - tp;
	float h = clamp(dot(dp - tp, seg) / max(dot(seg, seg), 1e-5), 0.0, 1.0);
	float dist = length(dp - tp - seg * h);
	return smoothstep(0.012, 0.0, dist) * h * h * sin(k * 3.14159) * step(0.0, d.y);
}

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 low = vec3(0.42, 0.13, 0.68);
	vec3 mid = vec3(0.2, 0.05, 0.4);
	vec3 top = vec3(0.045, 0.02, 0.13);
	vec3 color = mix(low, mid, smoothstep(-0.02, 0.28, e));
	color = mix(color, top, smoothstep(0.25, 0.95, e));
	vec3 q = d * 2.1 + vec3(0.0, 0.0, t * 0.008);
	float warp = fbm3(q * 1.3 + 3.0, 3);
	float neb = fbm3(q + warp * 1.5, 5);
	vec3 nebColor = mix(vec3(0.5, 0.1, 0.78), vec3(0.95, 0.32, 0.88), fbm3(q * 2.0 + 7.0, 2));
	color += nebColor * smoothstep(0.42, 0.85, neb) * 0.55 * smoothstep(-0.1, 0.35, e);
	color *= 1.0 - 0.45 * smoothstep(0.55, 0.8, fbm3(q * 2.6 + 11.0, 3)) * smoothstep(0.05, 0.5, e);
	color += vec3(0.95, 0.9, 1.0) * starLayer(d, t) * SKY_STARS * smoothstep(-0.02, 0.18, e) * (1.0 - SKY_DAY * 0.85);
	vec2 h = normalize(d.xz + vec2(1e-5));
	float edge = 0.05 + 0.11 * fbm3(vec3(h * 1.7, t * 0.025), 3);
	float height = smoothstep(edge - 0.01, edge + 0.035, e) * (1.0 - smoothstep(edge + 0.05, edge + 0.45, e));
	float rays = fbm3(vec3(h * 22.0 + fbm3(vec3(h * 2.4, t * 0.04), 2) * 3.5, t * 0.12), 3);
	rays = pow(rays, 2.2) * 2.4;
	float sweep = 0.55 + 0.45 * sin(dot(h, vec2(2.0, 1.3)) * 2.5 + t * 0.15);
	vec3 aurora = mix(vec3(0.58, 0.14, 1.0), vec3(1.0, 0.5, 0.95), smoothstep(edge, edge + 0.4, e));
	color += aurora * height * rays * sweep * 1.25;
	color += vec3(0.7, 0.28, 1.0) * height * 0.12;
	color += vec3(0.95, 0.65, 1.0) * horizonGlow(d, 0.05) * 0.18;
	color += vec3(1.0, 0.92, 1.0) * meteor(d, t) * SKY_STARS;
	color = planet(d, color, t);
	color = mix(color, color * 1.5 + vec3(0.1, 0.04, 0.18), SKY_DAY * 0.55);
	color = mix(color, low * 0.4, smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
