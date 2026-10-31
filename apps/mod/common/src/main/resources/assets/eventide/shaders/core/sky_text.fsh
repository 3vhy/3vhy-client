#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
#ifdef INTENSITY
	vec4 glyph = texture(Sampler0, texCoord0).rrrr;
#else
	vec4 glyph = texture(Sampler0, texCoord0);
#endif
	vec4 color = glyph * vertexColor * ColorModulator;
	if (color.a < 0.004) {
		discard;
	}
	fragColor = color;
}
