#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;

out vec3 ray;

void main() {
	gl_Position = vec4(Position.xy, 0.0, 1.0);
	if (TextureMat[3].w > 0.5) {
		ray = transpose(mat3(ModelViewMat)) * vec3(Position.x * TextureMat[3].x, Position.y * TextureMat[3].y, -1.0);
	} else {
		vec4 view = inverse(ProjMat) * vec4(Position.xy, 1.0, 1.0);
		ray = transpose(mat3(ModelViewMat)) * (view.xyz / view.w);
	}
}
