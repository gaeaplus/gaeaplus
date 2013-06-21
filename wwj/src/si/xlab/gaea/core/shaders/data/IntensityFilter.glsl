
uniform float minA;
uniform float minB;
uniform float maxA;
uniform float maxB;

uniform sampler2D colorTex;

varying vec2 texCoord;

#ifdef _VERTEX_
void main()
{
	texCoord = gl_Vertex.xy * 0.5 + 0.5;
    gl_Position = gl_Vertex;
}

#else

void main(void)
{

	vec4 color = texture2D(colorTex, texCoord);
	float intensity = (color.r + color.g + color.b)/3.0;

	float maskMin = smoothstep(minA, minB, intensity);
	float maskMax = smoothstep(1.0 - maxB, 1.0 - maxA, 1.0 - intensity);
	float mask = maskMin * maskMax;

	gl_FragColor = vec4(color.rgb, mask);
}

#endif