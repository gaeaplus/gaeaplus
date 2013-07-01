#include "Util.glsl"

uniform vec4 lightDirection;
uniform vec3 lightColor;

varying vec3 vertexNormal;
varying vec4 vertexColor;
varying vec4 vertexEyePosition;
varying vec4 vertexWorldPosition;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
#endif

#ifdef _VERTEX_
void main()
{
	vertexWorldPosition = gl_Vertex + gl_LightSource[7].diffuse;
	vertexEyePosition = gl_ModelViewMatrix * gl_Vertex;
	gl_Position = gl_ProjectionMatrix * vertexEyePosition;
	vertexNormal = gl_Normal;
	vertexColor = gl_Color;
}

#else

void main()
{
	vec3 colorAmbient = vec3(0.3, 0.3, 0.3);
	vec3 colorLight = lightColor;
	float diffuse = max(dot(normalize(vertexNormal.xyz), normalize(lightDirection.xyz)),0.0);

#ifdef _SHADOW_
	float shadow = getShadow(vertexWorldPosition.xyz, -normalize(lightDirection.xyz), shadowSampler,
					eyeToShadowTextureTransform * vertexEyePosition);
	diffuse = diffuse * shadow;
#else
	diffuse = diffuse * getShadowSphere(vertexWorldPosition.xyz, -normalize(lightDirection.xyz));
#endif

	vec3 light = colorAmbient + diffuse * colorLight;
	gl_FragColor = vec4(vertexColor.rgb * light, vertexColor.a);
}
#endif