#include "AtmosphereCommon.glsl"

//light
uniform vec4 cameraWorldPosition;
uniform vec4 lightDirection;
uniform float exposure;

//shadow volume
uniform float zNear;
uniform float zFar;

uniform float width;
uniform float height;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
uniform sampler2D shadowVolumeSampler;
#endif

uniform sampler2D transmittanceSampler;	//precomputed transmittance texture (T table)
uniform sampler2D irradianceSampler;	//precomputed skylight irradiance (E table)
uniform sampler3D inscatterSampler;		//precomputed inscattered light (S table)

varying vec3 vertexNormal;
varying vec4 vertexColor;
varying vec4 vertexEyePosition;
varying vec4 vertexWorldPosition;

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
	float texCooX = gl_FragCoord.x/width;
	float texCooY = gl_FragCoord.y/height;
	vec2 vertexTexPosition = vec2(texCooX, texCooY);

	////////////fragment atmosphere parameters//////////////
	float r, mu, d;
	vec3 x, v;
	vec3 s = coordTrans(normalize(-lightDirection.xyz));
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

	float diffuse = max(dot(normalize(vertexNormal), normalize(-lightDirection.xyz)), 0.0);
#ifdef _SHADOW_
		vec4 shadowCoord = eyeToShadowTextureTransform * vertexEyePosition;
		float mapShadow = shadow2DProj(shadowSampler, shadowCoord).r;
		float isShadow = mapShadow * (1.0 - clamp(abs(shadow.r), 0.0, 1.0));
		vec3 groundColor = getGroundColor(x, v, d, s, r, mu,
							transmittanceSampler,
							irradianceSampler,
							vertexColor.rgb, diffuse, isShadow); //R[L0]+R[L*]
#else
		vec3 groundColor = getGroundColor(x, v, d, s, r, mu,
							transmittanceSampler,
							irradianceSampler,
							vertexColor.rgb, diffuse, 0.0); //R[L0]+R[L*]
#endif

	gl_FragColor = vec4(HDR(groundColor + inscatterColor, exposure), 1.0);
}

#endif