#version 330

#moj_import <eventide:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 base = mix(vec3(0.6, 0.18, 0.04), vec3(0.12, 0.03, 0.02), smoothstep(-0.05, 0.7, e));
	vec2 uv = dome(d, 0.32) * 0.8 + vec2(t * 0.02, -t * 0.045);
	float warp = fbm2(uv * 1.4 + vec2(t * 0.03, 0.0), 4);
	float f = fbm2(uv + warp * 1.7, 5);
	float fade = smoothstep(-0.02, 0.15, e);
	vec3 flame = vec3(0.95, 0.28, 0.04) * smoothstep(0.38, 0.62, f);
	flame += vec3(1.0, 0.62, 0.18) * smoothstep(0.58, 0.78, f);
	flame += vec3(1.0, 0.9, 0.55) * smoothstep(0.74, 0.88, f) * 0.6;
	vec3 color = base + flame * fade;
	float smoke = fbm2(uv * 0.7 + 9.0 - vec2(0.0, t * 0.015), 4);
	color = mix(color, vec3(0.05, 0.025, 0.025), smoothstep(0.5, 0.72, smoke) * 0.85 * fade);
	color += vec3(1.0, 0.45, 0.1) * horizonGlow(d, 0.06) * 0.35;
	float embers = stars(d, 70.0, t * 4.0) * smoothstep(0.0, 0.5, e);
	color += vec3(1.0, 0.6, 0.2) * embers * 0.8;
	color = mix(color, vec3(0.32, 0.08, 0.03), smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
