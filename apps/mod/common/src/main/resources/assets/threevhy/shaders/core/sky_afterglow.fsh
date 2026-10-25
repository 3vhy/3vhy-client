#version 330

#moj_import <threevhy:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 sun = normalize(vec3(0.85, 0.05, -0.52));
	float toward = dot(normalize(d.xz + vec2(1e-5)), normalize(sun.xz)) * 0.5 + 0.5;
	vec3 horizon = mix(vec3(0.72, 0.32, 0.72), vec3(1.0, 0.62, 0.62), toward);
	vec3 color = mix(horizon, vec3(0.55, 0.16, 0.72), smoothstep(-0.02, 0.2, e));
	color = mix(color, vec3(0.24, 0.07, 0.52), smoothstep(0.15, 0.5, e));
	color = mix(color, vec3(0.06, 0.03, 0.2), smoothstep(0.45, 1.0, e));
	float angle = acos(clamp(dot(d, sun), -1.0, 1.0));
	color += vec3(1.0, 0.7, 0.75) * exp(-angle * 5.0) * 0.55;
	color += vec3(1.0, 0.92, 0.9) * smoothstep(0.05, 0.04, angle);
	vec2 streak = dome(d, 0.18) * vec2(0.35, 2.6) + vec2(t * 0.02, 0.0);
	float s = fbm2(streak, 5);
	float lit = smoothstep(0.5, 0.75, s);
	float band = smoothstep(0.0, 0.05, e) * (1.0 - smoothstep(0.25, 0.6, e));
	vec3 cloud = mix(vec3(0.35, 0.1, 0.45), vec3(1.0, 0.55, 0.7), toward * (1.0 - smoothstep(0.0, 0.3, e)));
	color = mix(color, cloud, lit * band * 0.8);
	color += vec3(1.0, 0.75, 0.9) * smoothstep(0.48, 0.52, s) * (1.0 - smoothstep(0.52, 0.6, s)) * band * toward * 0.35;
	color += vec3(0.95, 0.9, 1.0) * starLayer(d, t) * SKY_STARS * 0.75 * smoothstep(0.3, 0.6, e) * (1.0 - SKY_DAY);
	color = mix(color, color * 1.3 + vec3(0.06, 0.03, 0.08), SKY_DAY * 0.45);
	color = mix(color, horizon * 0.45, smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
