#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

// Eventide's knives and gloves. Each vertex carries what it's made of instead of a colour:
// Color = (material, finish + high seed bits, low seed bits, how near the cutting edge),
// UV0 = where it is on the knife in pixels (for the finish pattern),
// UV1 = (knife + 64 x glint step, wear + 128 x light boost), UV2 = the world's light.

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

out vec3 worldPos;
out vec3 worldNormal;
out vec3 viewPos;
out vec3 viewNormal;
out vec2 pattern;
out float edge;
flat out ivec4 info;
flat out vec3 frameInfo;
out vec4 lightMapColor;

void main() {
    vec4 view = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * view;
    worldPos = Position;
    worldNormal = Normal;
    viewPos = view.xyz;
    viewNormal = mat3(ModelViewMat) * Normal;
    pattern = UV0;
    ivec4 c = ivec4(round(Color * 255.0));
    edge = Color.a;
    info = ivec4(c.r, c.g & 63, ((c.g >> 6) << 8) | c.b, UV1.x & 63);
    frameInfo = vec3(float(UV1.x >> 6) / 63.0, float(UV1.y & 127) / 100.0, float(UV1.y >> 7) / 100.0);
    lightMapColor = texture(Sampler2, clamp(vec2(UV2) / 256.0 + 0.5 / 16.0, vec2(0.5 / 16.0), vec2(15.5 / 16.0)));
}
