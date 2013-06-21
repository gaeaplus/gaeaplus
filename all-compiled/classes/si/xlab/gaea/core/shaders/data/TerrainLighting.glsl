#include "Util.glsl"
#include "AtmosphereParameters.glsl"

uniform float width;
uniform float height;
uniform vec4 lightDirection;
uniform vec3 lightColor;
uniform float exposure;

uniform sampler2D colorSampler;
uniform sampler2D normalSampler;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
#endif

varying vec4 vertexEyePosition;
varying vec4 vertexWorldPosition;

#ifdef _VERTEX_

void main()
{
	vertexWorldPosition = gl_Vertex + gl_LightSource[7].diffuse;
	vertexEyePosition = gl_ModelViewMatrix * gl_Vertex;
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}

#else

void main()
{

	float texCooX = gl_FragCoord.x/width;
	float texCooY = gl_FragCoord.y/height;
	vec2 vertexTexPosition = vec2(texCooX, texCooY);

	vec4 color = texture2D(colorSampler, vertexTexPosition);
	vec4 normal = getNormal(normalSampler, vertexTexPosition);
	normal = normal.a > 0.5 ? normal : normalize(vertexWorldPosition);

#ifdef _SHADOW_
	float shadow = getShadow(vertexWorldPosition.xyz, lightDirection.xyz, shadowSampler,
					eyeToShadowTextureTransform * vertexEyePosition);
#else
	float shadow = getShadowSphere(vertexWorldPosition.xyz, normalize(lightDirection.xyz));
#endif
	shadow = clamp(shadow, 0.0, 1.0);

	vec3 ambientLight = vec3(0.3, 0.3, 0.3);

	float diffuse = shadow * max(dot(normal.xyz, -normalize(lightDirection.xyz)), 0.0);
	vec3 colorLight = (diffuse * lightColor + ambientLight) * color.rgb;

	gl_FragColor = vec4(colorLight, 1.0);
}
#endif