#version 120

uniform sampler2D normalTex;
uniform sampler2D elevationColorTex;

varying vec2 texCoord;

#ifdef _VERTEX_

void main()
{
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

	vec2 tmp = gl_Position.xy / gl_Position.w;
	texCoord = (tmp * 0.5 + 0.5);
}

#else

void main(void)
{
	float elevation = texture2D(normalTex, texCoord).a - 12000.0;
	float elevationN = (elevation/12000.0 + 1.0) / 2.0;

	vec3 color = texture2D(elevationColorTex, vec2(elevationN, 0.5)).rgb;

	gl_FragColor = vec4(color.rgb, 1.0);
}

#endif