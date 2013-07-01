#include "Util.glsl"

uniform mat4 viewMatrixTranspose;
uniform vec4 cameraWorldPosition;
//light
uniform vec4 lightDirectionEye;
uniform vec4 lightDirectionWorld;
uniform vec3 lightColor;

uniform sampler2D colorSampler;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
#endif

varying vec2 texCoord0;
varying vec3 vertexNormal;
varying float vertexAlpha;
varying vec4 vertexEyePosition;
varying vec4 vertexWorldPosition;

#ifdef _VERTEX_
void main()
{
	vertexEyePosition = gl_ModelViewMatrix * gl_Vertex;
	vertexWorldPosition = cameraWorldPosition + vec4((viewMatrixTranspose * vertexEyePosition).xyz, 0.0);
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

	texCoord0 = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	vertexNormal = (gl_ModelViewMatrix * vec4(gl_Normal, 0.0)).xyz;
	//vertexNormal = gl_NormalMatrix * gl_Normal;
	vertexAlpha = gl_Color.a;
}
#else

void main()
{
	vec4 texColor = texture2D(colorSampler, texCoord0);

	vec3 colorAmbient = vec3(0.3, 0.3, 0.3);
	vec3 colorLight = lightColor;

	vec3 lE = normalize(lightDirectionEye.xyz);
	vec3 lW = normalize(lightDirectionWorld.xyz);
	vec3 N = normalize(vertexNormal);

	float diffuse = max(dot(N, -lE), 0.0);
	float shadow;

#ifdef _SHADOW_
	shadow = getShadow(vertexWorldPosition.xyz, lW, shadowSampler,
					eyeToShadowTextureTransform * vertexEyePosition);
#else
	shadow = getShadowSphere(vertexWorldPosition.xyz, lW);
#endif
	shadow = clamp(shadow, 0.0, 1.0);

	vec3 light = shadow * diffuse * colorLight + colorAmbient;

	vec3 vE = normalize(vertexEyePosition.xyz);
	vec3 R = reflect(-lE, N);
	float specular = gl_FrontMaterial.shininess > 1.0 ? pow(max(dot(R, vE), 0.0), gl_FrontMaterial.shininess) : 0.0;
	specular *= shadow;

	gl_FragColor = vec4(texColor.rgb * light + colorLight * specular, texColor.a * vertexAlpha);
}
#endif