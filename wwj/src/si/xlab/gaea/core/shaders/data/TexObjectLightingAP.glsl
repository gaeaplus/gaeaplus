#include "AtmosphereCommon.glsl"

//shadow volume
uniform float zNear;
uniform float zFar;

uniform float width;
uniform float height;

uniform mat4 viewMatrixTranspose;
//light
uniform vec4 cameraWorldPosition;
uniform vec4 lightDirectionEye;
uniform vec4 lightDirectionWorld;
uniform float exposure;

uniform sampler2D colorSampler;
uniform sampler2D transmittanceSampler;		//precomputed transmittance texture (T table)
uniform sampler2D irradianceSampler;		//precomputed skylight irradiance (E table)
uniform sampler3D inscatterSampler;			//precomputed inscattered light (S table)

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
uniform sampler2D shadowVolumeSampler;
#endif

varying vec2 texCoord0;

varying vec3 vertexNormal;
varying float vertexAlpha;
varying	vec4 vertexEyePosition;
varying	vec4 vertexWorldPosition;

#ifdef _VERTEX_
void main()
{
	vertexEyePosition = gl_ModelViewMatrix * gl_Vertex;
	vertexWorldPosition = cameraWorldPosition + vec4((viewMatrixTranspose * vertexEyePosition).xyz, 0.0);
	gl_Position = gl_ProjectionMatrix * vertexEyePosition;
	texCoord0 = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;

	vertexNormal = (gl_ModelViewMatrix * vec4(gl_Normal, 0.0)).xyz;
	vertexAlpha = gl_Color.a;
}
#else

void main()
{

	float texCooX = gl_FragCoord.x/width;
	float texCooY = gl_FragCoord.y/height;
	vec2 vertexTexPosition = vec2(texCooX, texCooY);

	vec4 texColor = texture2D(colorSampler, texCoord0);

	////////////fragment atmosphere parameters//////////////
	float r, mu, d;
	vec3 x, v;
	vec3 s = coordTrans(normalize(-lightDirectionWorld.xyz));
	/////////////////////////////////////////////////////////

	float inscatterFactor = 1.0;
	d = length(cameraWorldPosition.xyz - vertexWorldPosition.xyz)/1000.0;
#ifdef _SHADOW_
	float dep = getDistance(zNear, zFar, gl_FragCoord.z);
	vec4 shadow = texture2D(shadowVolumeSampler, vertexTexPosition);
	float inscatterDepth = dep - clamp(shadow.g - shadow.r * dep, 0.0, dep - shadow.a);
	inscatterFactor = inscatterDepth/(d*1000.0);
#endif

	calcAtmParam(cameraWorldPosition.xyz, vertexWorldPosition.xyz, x, v, d, r, mu);

	vec3 inscatterColor = inscatterFactor * getInscatter(x, v, d, s, r, mu, inscatterSampler,
																			transmittanceSampler);

	//float diffuse = max(dot(vertexNormal, lightDirectionEye.xyz), 0.0);
	float diffuse = 0.3;
#ifdef _SHADOW_
		vec4 shadowCoord = eyeToShadowTextureTransform * vertexEyePosition;
		float mapShadow = shadow2DProj(shadowSampler, shadowCoord).r;
		//float isShadow = mapShadow * (1.0 - clamp(abs(shadow.r), 0.0, 1.0));
		float isShadow = mapShadow;
		vec3 groundColor = getGroundColor(x, v, d, s, r, mu,
							transmittanceSampler,
							irradianceSampler,
							texColor.rgb, diffuse, isShadow); //R[L0]+R[L*]
#else
		vec3 groundColor = getGroundColor(x, v, d, s, r, mu,
							transmittanceSampler,
							irradianceSampler,
							texColor.rgb, diffuse, 0.0); //R[L0]+R[L*]
#endif

	gl_FragColor = vec4(HDR(groundColor + inscatterColor, exposure), texColor.a * vertexAlpha);
}

#endif