#include "AtmosphereCommon.glsl"

uniform sampler2D colorSampler;
uniform sampler2D normalSampler;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
uniform sampler2D shadowVolumeSampler;
#endif

uniform sampler2D transmittanceSampler;	//precomputed transmittance texture (T table)
uniform sampler2D irradianceSampler;	//precomputed skylight irradiance (E table)
uniform sampler3D inscatterSampler;		//precomputed inscattered light (S table)
uniform sampler2D usageSampler;			//terrain usage (watter and forest)

uniform float width;
uniform float height;

//light
uniform vec4 cameraWorldPosition;
uniform vec4 lightDirection;
uniform float exposure;

//shadow volume
uniform float zNear;
uniform float zFar;

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

vec3 getTerrainColor(vec3 x, vec3 v, float d, vec3 s, float r, float mu,
									sampler2D colorSampler,
									sampler2D usageSampler,
								    sampler2D transmittanceSampler,
								    sampler2D irradianceSampler,
								    vec2 colorCoords, vec3 normalVec, float isShadow)
{
    vec3 result;
	vec3 x0 = x + d * v;
	float rx0 = clamp(length(x0), Rg, Rt);
	vec3 n = normalize(x0);

	// direct sun light (radiance) reaching x0
	float muS = dot(n, s);

#ifdef _SHADOW_
	vec3 sunLight = isShadow * transmittance(rx0, muS, transmittanceSampler);
#else
	vec3 sunLight = transmittanceWithShadow(rx0, muS, transmittanceSampler);
#endif
	// precomputed sky light (irradiance) (=E[L*]) at x0
	vec3 groundSkyLight = irradiance(irradianceSampler, rx0, muS);

	float diffuse = max(dot(normalVec, s), 0.0);

	//terrain usage
	vec3 usageP = texture2D(usageSampler, colorCoords).rgb;
	float forest = 1.0 - clamp((usageP.g - (usageP.r + usageP.b)/2.0), 0.0, 1.0);

	//////////ground sun color////////
	vec3 color = texture2D(colorSampler, colorCoords).rgb;
	vec3 groundColor = pow(color, vec3(2.6)) * (max(diffuse, 0.0) * sunLight + groundSkyLight) * forest * ISun / M_PI;
	//////////////////////////////

	//////////specular////////////
	vec3 h = normalize(s - v);
	if(diffuse > 0.0){
		float highlightB = pow(clamp(1.0 - length(color - vec3(1.0, 1.0, 1.0)), 0.0, 1.0), 12.0);

		//highlight bright parts
		groundColor += 20.0 * highlightB * pow(max(dot(h, normalVec), 0.0), 150.0) * sunLight * ISun / M_PI;

		//global specular
		groundColor += 0.01 * pow(max(dot(h, normalVec), 0.0), 40.0 * forest) * forest * sunLight * ISun / M_PI;

	}
	//usage detection
	float river = usageP.b - (usageP.r + usageP.g)/2.0;
	// water specular color due to sunLight
	if (river > 0.4) {
		float fresnel = 0.02 + 0.98 * pow(1.0 - dot(-v, h), 5.0);
		float waterBrdf = fresnel * pow(max(dot(h, n), 0.0), 150.0);
		groundColor += river * max(waterBrdf, 0.0) * sunLight * ISun;
	}
	//////////////////////////////

	// attenuation of light to the viewer, T(x,x0)
	vec3 attenuation = transmittance(r, mu, v, x0, transmittanceSampler);
	result = attenuation * groundColor; //=R[L0]+R[L*]
    return result;
}

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

	d = length(cameraWorldPosition.xyz - vertexWorldPosition.xyz)/1000.0;
	float inscatterFactor = 1.0;
#ifdef _SHADOW_
	float dep = getDistance(zNear, zFar, gl_FragCoord.z);
	vec4 shadow = texture2D(shadowVolumeSampler, vertexTexPosition);
	float inscatterDepth = dep - clamp(shadow.g - shadow.r * dep, 0.0, dep - shadow.a);
	inscatterFactor = inscatterDepth/(d*1000.0);
#endif

	calcAtmParam(cameraWorldPosition.xyz, vertexWorldPosition.xyz, x, v, d, r, mu);

	vec3 inscatterColor = inscatterFactor * getInscatter(x, v, d, s, r, mu, inscatterSampler,
																			transmittanceSampler);

	vec4 normal = getNormal(normalSampler, vertexTexPosition);
	normal = normal.a > 0.5 ? normal : normalize(vertexWorldPosition);
	vec3 normalVec = coordTrans(normal.xyz);

#ifdef _SHADOW_
		vec4 shadowCoord = eyeToShadowTextureTransform * vertexEyePosition;
		float mapShadow = shadow2DProj(shadowSampler, shadowCoord).r;
		float isShadow = mapShadow * (1.0 - clamp(abs(shadow.r), 0.0, 1.0));
		vec3 groundColor = getTerrainColor(x, v, d, s, r, mu,
							colorSampler,
							usageSampler,
							transmittanceSampler,
							irradianceSampler,
							vertexTexPosition, normalVec, isShadow); //R[L0]+R[L*]
#else
		vec3 groundColor = getTerrainColor(x, v, d, s, r, mu,
							colorSampler,
							usageSampler,
							transmittanceSampler,
							irradianceSampler,
							vertexTexPosition, normalVec, 0.0); //R[L0]+R[L*]
#endif

	vec3 outColor = groundColor + inscatterColor;

#ifdef _POSEFFECTS_
	gl_FragColor = vec4(outColor, outColor.r + outColor.g + outColor.b);
#else
	gl_FragColor = vec4(HDR(outColor, exposure), 1.0);
#endif

	//gl_FragColor = vec4((outColor) * 0.06, 1.0);
}
#endif