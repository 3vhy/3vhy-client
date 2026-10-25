#version 330

#moj_import <threevhy:sky_lib.glsl>

in vec3 ray;

out vec4 fragColor;

void main() {
	vec3 d = normalize(ray);
	float t = SKY_TIME;
	float e = d.y;
	vec3 color = mix(vec3(0.78, 0.36, 0.14), vec3(0.32, 0.1, 0.3), smoothstep(-0.02, 0.22, e));
	color = mix(color, vec3(0.05, 0.02, 0.1), smoothstep(0.18, 0.8, e));
	color += vec3(0.95, 0.9, 1.0) * starLayer(d, t) * SKY_STARS * 0.8 * smoothstep(0.15, 0.45, e) * (1.0 - SKY_DAY);
	vec3 moon = normalize(vec3(-0.62, 0.2, -0.76));
	float angle = acos(clamp(dot(d, moon), -1.0, 1.0));
	float radius = 0.085;
	vec3 mu = normalize(cross(vec3(0.0, 1.0, 0.0), moon));
	vec3 mv = cross(moon, mu);
	vec2 mq = vec2(dot(d, mu), dot(d, mv)) / radius;
	float aa = fwidth(angle) * 1.5 / radius;
	float disc = smoothstep(1.0 + aa, 1.0 - aa, angle / radius) * step(0.0, dot(d, moon));
	color += vec3(1.0, 0.6, 0.25) * exp(-angle * 9.0) * 0.45;
	float craters = fbm2(mq * 2.2 + 4.0, 4);
	vec3 surface = mix(vec3(1.0, 0.78, 0.46), vec3(0.86, 0.55, 0.3), smoothstep(0.45, 0.7, craters));
	surface *= 0.85 + 0.15 * sqrt(max(0.0, 1.0 - dot(mq, mq)));
	color = mix(color, surface, disc);
	vec2 wisp = dome(d, 0.2) * vec2(0.5, 2.2) + vec2(t * 0.03, 0.0);
	float w = fbm2(wisp, 5);
	float band = smoothstep(0.02, 0.1, e) * (1.0 - smoothstep(0.2, 0.45, e));
	color = mix(color, vec3(0.08, 0.03, 0.09), smoothstep(0.52, 0.72, w) * band * 0.9);
	color += vec3(0.9, 0.45, 0.2) * smoothstep(0.5, 0.56, w) * (1.0 - smoothstep(0.56, 0.66, w)) * band * 0.25;
	color = mix(color, color * 1.35 + vec3(0.06, 0.03, 0.05), SKY_DAY * 0.45);
	color = mix(color, vec3(0.3, 0.12, 0.1), smoothstep(0.0, -0.3, e));
	fragColor = vec4(finish(color, d), SKY_ALPHA);
}
