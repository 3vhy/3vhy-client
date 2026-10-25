#version 330

#moj_import <threevhy:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 base = mix(vec3(0.42, 0.46, 0.52), vec3(0.24, 0.27, 0.32), smoothstep(-0.05, 0.6, e));
	vec2 uv = dome(d, 0.25);
	float fade = smoothstep(0.0, 0.12, e);
	float upper = fbm2(uv * 0.5 + vec2(t * 0.015, t * 0.004), 5);
	float lower = fbm2(uv * 0.9 + vec2(t * 0.03, -t * 0.01) + 7.0, 5);
	vec3 color = mix(base, vec3(0.3, 0.33, 0.38), smoothstep(0.4, 0.7, upper) * fade);
	color = mix(color, vec3(0.17, 0.19, 0.23), smoothstep(0.5, 0.75, lower) * 0.85 * fade);
	color += vec3(0.08) * smoothstep(0.55, 0.85, upper) * (1.0 - smoothstep(0.45, 0.7, lower)) * fade;
	vec2 h = normalize(d.xz + vec2(1e-5));
	float veils = smoothstep(0.45, 0.75, fbm3(vec3(h * 5.0, t * 0.02), 3));
	float streaks = 0.6 + 0.4 * noise3(vec3(h * 120.0, e * 4.0 + t * 3.0));
	float band = smoothstep(-0.02, 0.04, e) * (1.0 - smoothstep(0.06, 0.32, e));
	color = mix(color, vec3(0.28, 0.31, 0.36), veils * streaks * band * 0.7);
	color += vec3(0.1, 0.11, 0.13) * horizonGlow(d, 0.05) * 0.3;
	color *= mix(0.35, 1.0, SKY_DAY);
	color = mix(color, base * 0.8 * mix(0.35, 1.0, SKY_DAY), smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
