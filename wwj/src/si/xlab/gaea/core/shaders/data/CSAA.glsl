uniform sampler2D colorTex;
uniform sampler2D csaaTex;

uniform float pixelW;
uniform float pixelH;

varying vec2 coords;

#ifdef _VERTEX_

void main() {
    coords = gl_Vertex.xy * 0.5 + 0.5;
    gl_Position = gl_Vertex;
}
#else

void main(){

	float csaa = texture2D(csaaTex, coords).r;
	vec2 offset = coords * (1.0 - csaa);

	vec4 color0 = texture2D(colorTex, offset + vec2(coords.x - pixelW, coords.y + pixelH) * csaa);
	vec4 color1 = texture2D(colorTex, offset + vec2(coords.x + pixelW, coords.y + pixelH) * csaa);
	vec4 color2 = texture2D(colorTex, offset + vec2(coords.x - pixelW, coords.y - pixelH) * csaa);
	vec4 color3 = texture2D(colorTex, offset + vec2(coords.x + pixelW, coords.y - pixelH) * csaa);


	gl_FragColor = (color0 + color1 + color2 + color3)/4.0;
	//gl_FragColor = vec4(csaa);
}
#endif