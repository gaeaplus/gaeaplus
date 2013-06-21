uniform sampler2D colorTex;
uniform sampler2D depthTex;
uniform float intensityAve;

#ifdef _VERTEX_

in vec4 in_vertex;
out vec2 coords;

void main() {
    coords = in_vertex.xy * 0.5 + 0.5;
    gl_Position = in_vertex;
}
#else

vec3 HDR(vec3 L, float exposure) {
    L = L * exposure;
    //L.r = L.r < 1.413 ? pow(L.r * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.r);
    //L.g = L.g < 1.413 ? pow(L.g * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.g);
    //L.b = L.b < 1.413 ? pow(L.b * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.b);
	L = 1.0 - exp(-L);
    return L;
}

in vec2 coords;
out vec4 out_Color;

void main(){

	vec4 color = texture(colorTex, coords);
	out_Color = vec4(HDR(color.rgb,	clamp((0.4 + (intensityAve * 0.1))/intensityAve, 0.2, 6.0)), 1.0);
	gl_FragDepth = texture(depthTex, coords).r;
}
#endif