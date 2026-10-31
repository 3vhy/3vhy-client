#version 330

#moj_import <eventide:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	vec3 base = mix(vec3(0.19, 0.2, 0.22), vec3(0.085, 0.09, 0.1), smoothstep(-0.05, 0.7, d.y));
	vec3 p = d * 1.25;
	vec3 flow = vec3(t * 0.016, t * 0.005, -t * 0.011);
	float w1 = fbm3(p * 1.1 + flow, 4);
	float w2 = fbm3(p * 1.1 + vec3(5.2, 1.3, 2.8) - flow, 4);
	vec3 warped = p + vec3(w1, w2, w1 * w2) * 2.1;
	float n = fbm3(warped * 1.15 + flow * 2.0, 5);
	float m = fbm3(warped * 0.6 + 4.0, 3);
	float vein = pow(ridge(n), 34.0) * smoothstep(0.35, 0.6, m);
	float fine = pow(ridge(fbm3(warped * 2.2 - flow, 4)), 48.0) * smoothstep(0.45, 0.7, m);
	float halo = pow(ridge(n), 9.0) * smoothstep(0.35, 0.6, m);
	vec3 color = base + vec3(0.07, 0.075, 0.085) * smoothstep(0.45, 0.8, m);
	color += vec3(0.85, 0.88, 0.95) * (vein * 0.9 + fine * 0.35);
	color += vec3(0.55, 0.58, 0.66) * halo * 0.16;
	color += vec3(0.25) * horizonGlow(d, 0.08) * 0.25;
	color += vec3(0.9, 0.92, 1.0) * starLayer(d, t) * SKY_STARS * 0.45 * smoothstep(0.0, 0.3, d.y) * (1.0 - SKY_DAY);
	color = mix(color, color * 1.35 + vec3(0.05), SKY_DAY * 0.5);
	color = mix(color, base * 0.8, smoothstep(0.0, -0.3, d.y));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
