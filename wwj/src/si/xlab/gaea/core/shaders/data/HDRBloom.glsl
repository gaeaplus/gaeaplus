uniform sampler2D bloomTex;
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

in vec2 coords;
out vec4 out_Color;

vec3 HDR(vec3 L, float exposure) {
    L = L * exposure;
    //L.r = L.r < 1.413 ? pow(L.r * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.r);
    //L.g = L.g < 1.413 ? pow(L.g * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.g);
    //L.b = L.b < 1.413 ? pow(L.b * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.b);

	L.r = L.r < 1.0 ? pow(L.r * 0.47, 0.6073) : 1.0 - exp(-L.r);
    L.g = L.g < 1.0 ? pow(L.g * 0.47, 0.6073) : 1.0 - exp(-L.g);
    L.b = L.b < 1.0 ? pow(L.b * 0.47, 0.6073) : 1.0 - exp(-L.b);	

	//L = 1.0 - exp(-L);
    return L;
}

void main(){

	vec4 color = texture(colorTex, coords);
	vec4 bloom = texture(bloomTex, coords);
	float depth = texture(depthTex, coords).r;

	out_Color = vec4(HDR(color.rgb + bloom.rgb * bloom.a * 0.3, clamp((0.4 + (intensityAve * 0.1))/intensityAve, 0.2, 6.0)), 1.0);
	//out_Color = vec4(HDR(bloom.rgb * bloom.a, clamp(0.60/intensityAve, 0.30, 0.6)), 1.0);
	gl_FragDepth = depth;
}
#endif