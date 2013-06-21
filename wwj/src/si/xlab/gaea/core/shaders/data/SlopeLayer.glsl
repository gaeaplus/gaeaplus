
#include "Util.glsl"

uniform float maxSlope;
uniform vec4 color;
uniform sampler2D normalTex;

varying vec3 normalGlobe;
varying vec2 texCoord;

#ifdef _VERTEX_
void main()
{
	normalGlobe = normalize(gl_Vertex + gl_LightSource[7].diffuse).xyz;
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

	vec2 tmp = gl_Position.xy / gl_Position.w;
	texCoord = (tmp * 0.5 + 0.5);
}

#else

void main(void)
{
	vec3 normal = getNormal(normalTex, texCoord.xy).xyz;

	float a_dot_b = dot(normalize(normal), normalize(normalGlobe));
	float angle = acos(a_dot_b);
	float filter = step(maxSlope, angle);

	gl_FragColor = vec4(color.rgb, color.a * filter);
	//gl_FragColor = vec4(hsvToRgb(0.5 - (angle/M_PI2), 1.0, 1.0), color.a * (1.0-filter));
	//gl_FragColor = vec4(hsvToRgb(0.5 - (clamp(angle,0.0,maxSlope)/maxSlope), 1.0, 1.0), color.a * (1.0-filter));
	//gl_FragColor = vec4(1.0, 0.0, 0.0, 0.5);
	//gl_FragColor = vec4(normalize(normal), 1.0);
	//gl_FragColor = vec4(normalize(normalGlobe), 1.0);
}

#endif