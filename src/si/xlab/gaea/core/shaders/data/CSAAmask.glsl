uniform sampler2D depthTex;
uniform float sampleDistX;
uniform float sampleDistY;

uniform vec2 barrier;
uniform vec2 weights;

varying vec2 coords;

#ifdef _VERTEX_

void main() {
    coords = gl_Vertex.xy * 0.5 + 0.5;
    gl_Position = gl_Vertex;
}
#else

void main(){

	float dc = texture2D(depthTex, vec2(coords.x, coords.y)).r;
	vec4 dd;

	dd.x = texture2D(depthTex, vec2(coords.x - sampleDistX, coords.y + sampleDistY)).r
			+ texture2D(depthTex, vec2(coords.x + sampleDistX, coords.y - sampleDistY)).r;
	dd.y = texture2D(depthTex, vec2(coords.x, coords.y + sampleDistY)).r
			+ texture2D(depthTex, vec2(coords.x, coords.y - sampleDistY)).r;
	dd.z = texture2D(depthTex, vec2(coords.x + sampleDistX, coords.y + sampleDistY)).r
			+ texture2D(depthTex, vec2(coords.x - sampleDistX, coords.y - sampleDistY)).r;
	dd.w = texture2D(depthTex, vec2(coords.x - sampleDistX, coords.y)).r
			+ texture2D(depthTex, vec2(coords.x + sampleDistX, coords.y)).r;

	dd = abs(2.0 * dc - dd) - barrier.y;
	dd = step(dd, vec4(0.0));
	float de = clamp(dot(dd, vec4(weights.y)), 0.0, 1.0);

	float edge = (1.0 - de) * 0.5;

	gl_FragColor = vec4(edge, edge, edge, 1.0);
}
#endif