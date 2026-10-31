#version 330

#moj_import <eventide:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 base = mix(vec3(0.34, 0.4, 0.46), vec3(0.17, 0.21, 0.27), smoothstep(-0.05, 0.75, e));
	vec2 uv = dome(d, 0.3) * 0.6 + vec2(t * 0.01, -t * 0.006);
	float clouds = fbm2(uv + fbm2(uv * 1.3 + 2.0, 3), 5);
	vec3 color = mix(base, base * 0.7, smoothstep(0.45, 0.75, clouds) * smoothstep(0.0, 0.2, e));
	vec2 h = normalize(d.xz + vec2(1e-5));
	float veil = fbm3(vec3(h * 3.2, e * 0.9 + t * 0.02), 4);
	float curtain = smoothstep(0.48, 0.78, veil) * smoothstep(-0.02, 0.25, e) * (1.0 - smoothstep(0.35, 0.95, e));
	float hue = fbm3(vec3(h * 1.4, t * 0.015), 3);
	vec3 lavender = vec3(0.78, 0.7, 0.96);
	vec3 pearl = vec3(0.9, 0.9, 0.93);
	vec3 cyan = vec3(0.58, 0.86, 0.93);
	vec3 tint = mix(mix(lavender, pearl, smoothstep(0.3, 0.55, hue)), cyan, smoothstep(0.5, 0.75, hue));
	color += tint * curtain * 0.55;
	float shaft = pow(fbm3(vec3(h * 9.0, t * 0.03), 3), 3.0) * smoothstep(0.05, 0.6, e) * (1.0 - smoothstep(0.6, 1.0, e));
	color += cyan * shaft * 0.35;
	color += pearl * horizonGlow(d, 0.06) * 0.12;
	color *= mix(0.42, 1.0, SKY_DAY);
	color = mix(color, base * 0.75 * mix(0.42, 1.0, SKY_DAY), smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
