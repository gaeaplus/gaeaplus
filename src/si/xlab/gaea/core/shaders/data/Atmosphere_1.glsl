#include "AtmosphereCommon.glsl"

uniform sampler2D depthSampler;			//depth from current framebuffer;
uniform sampler2D transmittanceSampler;	//precomputed transmittance texture (T table)
uniform sampler3D inscatterSampler;		//precomputed inscattered light (S table)

//light
uniform vec4 cameraWorldPosition;
uniform vec4 lightDirection;
uniform float exposure;

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

vec3 getInscatterATM(vec3 x, vec3 v, vec3 s, float r, float mu,
												sampler3D inscatterSampler,
												sampler2D transmittanceSampler) {
    vec3 result;
    if (r <= Rt) { // if ray intersects atmosphere
        float muS = dot(x, s) / r;
		float nu = dot(v, s);
        float phaseR = phaseFunctionR(nu);
        float phaseM = phaseFunctionM(nu);
		vec4 inscatter = max(texture4D(inscatterSampler, r, mu, muS, nu), 0.0);
		result = max(inscatter.rgb * phaseR + getMie(inscatter) * phaseM, 0.0);
    } else { // x in space and ray looking in space
        result = vec3(0.0);
    }
    return result * ISun;
}

vec3 getSunColor(vec3 x, vec3 v, vec3 s, float r, float mu, sampler2D transmittanceSampler) {
    vec3 transmittance = r <= Rt ? transmittanceWithShadow(r, mu, transmittanceSampler) : vec3(1.0); // T(x,xo)

    float isun = step(cos(M_PI / 180.0), dot(v, s)) * ISun; // Lsun
	isun = clamp(isun + pow(1.0 - clamp(2.0 * acos(dot(v, s)) / M_PI, 0.0, 1.0), 131.0), 0.0, 1.0) * ISun;
    return transmittance * isun; // Eq (9)
}

void main()
{
	float depth = texture2D(depthSampler, coords).r;

	//vec3 lightN = normalize(lightDirection.xyz);
	//float distS = dot(cameraWorldPosition.xyz, lightN);
	//vec3 pointHN = normalize(cameraWorldPosition.xyz - distS * (lightN));
	//vec3 pointH = pointHN * ((pHeight + eHeight)/2.0);
	//vec3 tmp = cameraWorldPosition.xyz - pointH;
	//float dShift = max(-dot(pointHN, tmp)/dot(pointHN, normalize(ray)), 0.0);
	//float dShift = 0.0;

	if(depth < 1.0){discard;}
	////////////fragment atmosphere parameters//////////////
	float r, mu, d;
	vec3 x, v;
	vec3 s = coordTrans(normalize(-lightDirection.xyz));
	/////////////////////////////////////////////////////////

	vec3 insT = globeIntersection(cameraWorldPosition.xyz, ray, 60000.0);

	float dist = max(insT.x, 0.0);
	x = fromWorldToAtm(cameraWorldPosition.xyz + dist * normalize(ray));
	v = normalize(coordTrans(normalize(ray)));
	r = max(length(x) - 0.020, Rg);
    mu = dot(x, v) / r;

	vec3 inscatterColor = getInscatterATM(x, v, s, r, mu, inscatterSampler,
													   transmittanceSampler);

	vec3 sunColor = getSunColor(x, v, s, r, mu, transmittanceSampler); //L0

	vec3 outColor = sunColor + inscatterColor;

#ifdef _POSEFFECTS_
	//float intensityW = clamp(1.0 - length(coords * 2.0 - 1.0), 0.1, 1.0);
	float intensity = (outColor.r + outColor.g + outColor.b)/3.0; 
	gl_FragColor = vec4(outColor, intensity);
#else
	gl_FragColor = vec4(HDR(outColor, exposure), 1.0);
#endif

	//gl_FragColor = vec4((outColor) * 0.06, 1.0);
}
#endif