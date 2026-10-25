#version 330

#moj_import <threevhy:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 base = mix(vec3(0.84, 0.87, 0.91), vec3(0.62, 0.69, 0.78), smoothstep(-0.05, 0.75, e));
	vec2 uv = dome(d, 0.3);
	float fade = smoothstep(0.0, 0.15, e);
	float a = fbm2(uv * 0.45 + vec2(t * 0.008, 0.0), 5);
	float b = fbm2(uv * 0.8 + vec2(-t * 0.012, t * 0.004) + 3.0, 4);
	vec3 color = mix(base, vec3(0.9, 0.92, 0.95), smoothstep(0.45, 0.75, a) * 0.7 * fade);
	color = mix(color, vec3(0.7, 0.74, 0.8), smoothstep(0.55, 0.8, b) * 0.45 * fade);
	vec3 sun = normalize(SKY_SUN);
	color += vec3(1.0, 0.95, 0.88) * pow(max(dot(d, sun), 0.0), 12.0) * 0.18;
	color += vec3(0.95, 0.96, 1.0) * horizonGlow(d, 0.07) * 0.08;
	color *= mix(0.32, 1.0, SKY_DAY);
	color = mix(color, base * 0.85 * mix(0.32, 1.0, SKY_DAY), smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
