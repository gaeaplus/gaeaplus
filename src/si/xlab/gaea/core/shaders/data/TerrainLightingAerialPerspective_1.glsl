#include "AtmosphereCommon.glsl"

uniform sampler2D colorSampler;
uniform sampler2D normalSampler;
uniform sampler2D depthSampler;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
	#ifdef _SHADOW_VOLUME_
	uniform sampler2D shadowVolumeSampler;
	#endif
#endif

uniform sampler2D transmittanceSampler;	//precomputed transmittance texture (T table)
uniform sampler2D irradianceSampler;	//precomputed skylight irradiance (E table)
uniform sampler3D inscatterSampler;		//precomputed inscattered light (S table)
uniform sampler2D usageSampler;			//terrain usage (watter and forest)
uniform sampler2D materialSampler;		//object materials;

//light
uniform vec4 cameraWorldPosition;
uniform vec4 lightDirection;
uniform float exposure;

uniform float zNear;
uniform float zFar;

varying vec2 coords;
varying vec3 ray;

#ifdef _VERTEX_

void main()
{
	coords = gl_Vertex.xy * 0.5 + 0.5;
	ray = (gl_ModelViewMatrixInverse * vec4((gl_ProjectionMatrixInverse * gl_Vertex).xyz, 0.0)).xyz;
    gl_Position = gl_Vertex;
}

#else

vec3 getTerrainColor(vec3 x, vec3 v, float d, vec3 s, float r, float mu,
									sampler2D colorSampler,
									sampler2D usageSampler,
								    sampler2D transmittanceSampler,
								    sampler2D irradianceSampler,
								    vec2 colorCoords, float diffuse, float specular, float skyOcclusion, float isShadow)
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
	vec3 groundSkyLight = 2.0 * irradiance(irradianceSampler, rx0, muS) * skyOcclusion;

	//terrain usage
	vec3 usageP = texture2D(usageSampler, colorCoords).rgb;
	//float forest = 1.0 - clamp((usageP.g - (usageP.r + usageP.b)/2.0), 0.0, 1.0);

	//////////ground sun color////////
	vec3 color = texture2D(colorSampler, colorCoords).rgb;
	//vec3 groundColor = pow(color, vec3(2.6)) * (diffuse * sunLight + groundSkyLight) * ISun / M_PI;
	vec3 groundColor = pow(color, vec3(2.5)) * (diffuse * sunLight + groundSkyLight) * ISun / M_PI;
	//////////////////////////////

	/////////materials///////////
	vec4 mat = texture2D(materialSampler, colorCoords);

	//////////specular////////////
	vec3 h = normalize(s - v);
	if(diffuse > 0.0){
		float highlightB = pow(clamp(1.0 - length(color - vec3(1.0, 1.0, 1.0)), 0.0, 1.0), 12.0);

		//highlight bright parts
		groundColor += 20.0 * highlightB * pow(specular, 150.0) * sunLight * ISun / M_PI;

		//material specular
		if(mat.x > 0.1 && mat.y > 0.1){
			groundColor += 20.0 * pow(specular * mat.x, mat.y * 300.0) * sunLight * ISun / M_PI;
		}
	}

	if(mat.z > 0.01){
		groundColor += pow(color, vec3(2.5)) * mat.b * ISun / M_PI;
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
	float depth = texture2D(depthSampler, coords).r;
	if(depth == 1.0){discard;}
	float dep = getDistance(zNear, zFar, depth);
	vec4 screen = vec4(coords * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);

	vec4 vE = (gl_ProjectionMatrixInverse * screen);
	vE /= vE.w;

	////////////fragment atmosphere parameters//////////////
	float r, mu, d;
	vec3 x, v;
	vec3 s = coordTrans(normalize(-lightDirection.xyz));
	/////////////////////////////////////////////////////////

	vec3 insT = globeIntersection(cameraWorldPosition.xyz, ray, 60000.0);
	vec3 insG = globeIntersection(cameraWorldPosition.xyz, ray, 1000.0);

	if(insT.x == 0.0 && insT.y == 0.0){discard;}

	float dist = max(insT.x, 0.0);
	float distG = max(insG.x, 0.0);

	vec3 vW = cameraWorldPosition.xyz + dist * normalize(ray);

	x = fromWorldToAtm(vW);
	v = normalize(coordTrans(normalize(ray)));
	r = length(x);
    mu = dot(x, v) / r;
	//d = min(length(vE.xyz), insT.y) - dist;

	d = (length(vE.xyz) - dist);
	//if(dist != 0.0){
	//d = min(d, abs(insG.y - insT.y)-0.10);
	//}
	//else if(insG.z > 0.5){
	//	d = min(d, distG);
	//}

	float inscatterFactor = 1.0;
#ifdef _SHADOW_VOLUME_
	vec4 shadow = texture2D(shadowVolumeSampler, coords);
	float inscatterDepth = d - clamp(shadow.g - shadow.r * dep, 0.0, dep - shadow.a);
	float blend = clamp(d/55000.0 + (1.0 - pow(abs(dot(v,s)), 6.0)), 0.0, 1.0);
	inscatterFactor = inscatterDepth/d;
	inscatterFactor = blend + (1.0-blend) * inscatterFactor;
#endif

	d /= 1000.0;

	vec3 inscatterColor = inscatterFactor * getInscatter(x, v, d, s, r, mu, inscatterSampler,
																	   transmittanceSampler); 

	/////////diffuse & specular & skyOcclusion//////////
	vec4 normal = getNormal(normalSampler, coords);
	float diffuse = 0.7;
	float specular = 0.0;
	float skyOcclusion = 1.0;
	if(normal.a > 0.2){
		vec3 normalVec = normalize(coordTrans(normal.xyz));
		vec3 h = normalize(s - v);
		specular = max(dot(h, normalVec), 0.0);
 		diffuse = max(dot(normalVec, s), 0.0);
		skyOcclusion = (1.0 + dot(normalVec, (x.xyz/r))) * 0.5;
	}
	////////////////////////////////////////////////////

#ifdef _SHADOW_
		vec4 shadowCoord = eyeToShadowTextureTransform * vE;
		float mapShadow = shadow2DProj(shadowSampler, shadowCoord).r;
		float isShadow = mapShadow;
	#ifdef _SHADOW_VOLUME_
			//if(!isInTexture(shadowCoord.xy)){
			isShadow *= (1.0 - clamp(abs(shadow.r), 0.0, 1.0));
			//}
	#endif
		vec3 groundColor = getTerrainColor(x, v, d, s, r, mu,
							colorSampler,
							usageSampler,
							transmittanceSampler,
							irradianceSampler,
							coords, diffuse, specular, skyOcclusion, isShadow); //R[L0]+R[L*]
#else
		vec3 groundColor = getTerrainColor(x, v, d, s, r, mu,
							colorSampler,
							usageSampler,
							transmittanceSampler,
							irradianceSampler,
							coords, diffuse, specular, skyOcclusion, 0.0); //R[L0]+R[L*]
#endif

	
	vec3 outColor = groundColor + inscatterColor;

#ifdef _POSEFFECTS_
	//float intensityW = clamp(1.0 - length(coords * 2.0 - 1.0), 0.1, 1.0);
	float intensity = (outColor.r + outColor.g + outColor.b)/3.0; 
	//gl_FragColor = vec4(outColor, intensity * intensity);
	gl_FragColor = vec4(outColor, intensity);
#else
	gl_FragColor = vec4(HDR(outColor, exposure), 1.0);
	gl_FragDepth = depth;
#endif

	//gl_FragColor = vec4((outColor) * 0.06, 1.0);
}
#endif