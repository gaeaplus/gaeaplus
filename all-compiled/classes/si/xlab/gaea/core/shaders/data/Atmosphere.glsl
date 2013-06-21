#include "AtmosphereCommon.glsl"

uniform sampler2D colorSampler;			//color from current framebuffer;
uniform sampler2D transmittanceSampler;	//precomputed transmittance texture (T table)
uniform sampler3D inscatterSampler;		//precomputed inscattered light (S table)

//light
uniform vec4 cameraWorldPosition;
uniform vec4 lightDirection;
uniform float exposure;

uniform float width;
uniform float height;

varying vec4 vertexWorldPosition;

#ifdef _VERTEX_

void main()
{
	vertexWorldPosition = gl_Vertex + gl_LightSource[7].diffuse;
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	gl_Position.z = gl_Position.w;
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

vec3 getSkyColor(float r, float mu, sampler2D transmittanceSampler, sampler2D colorSampler, vec2 colorCoords){

	vec3 color = texture2D(colorSampler, colorCoords).rgb;
	vec3 skyColor = pow(color, vec3(2.0)) * transmittance(r, mu, transmittanceSampler) * 10.0;
	return skyColor;
}

vec3 getSunColor(vec3 x, vec3 v, vec3 s, float r, float mu, sampler2D transmittanceSampler) {
    vec3 transmittance = r <= Rt ? transmittanceWithShadow(r, mu, transmittanceSampler) : vec3(1.0); // T(x,xo)

    float isun = step(cos(M_PI / 180.0), dot(v, s)) * ISun; // Lsun
	isun = clamp(isun + pow(1.0 - clamp(2.0 * acos(dot(v, s)) / M_PI, 0.0, 1.0), 131.0), 0.0, 1.0) * ISun;
    return transmittance * isun; // Eq (9)
}

void main()
{
	////////////fragment atmosphere parameters//////////////
	float r, mu, d;
	vec3 x, v;
	vec3 s = coordTrans(normalize(-lightDirection.xyz));
	/////////////////////////////////////////////////////////

	float texCooX = gl_FragCoord.x/width;
	float texCooY = gl_FragCoord.y/height;

	calcAtmParam(cameraWorldPosition.xyz, vertexWorldPosition.xyz, x, v, d, r, mu);

	vec3 inscatterColor = getInscatterATM(x, v, s, r, mu, inscatterSampler,
													   transmittanceSampler);

	vec3 sunColor = getSunColor(x, v, s, r, mu, transmittanceSampler); //L0
	vec3 skyColor = getSkyColor(r, mu, transmittanceSampler, colorSampler, vec2(texCooX, texCooY));

	vec3 outColor = sunColor + skyColor + inscatterColor;

#ifdef _POSEFFECTS_
	gl_FragColor = vec4(outColor, outColor.r + outColor.g + outColor.b);
#else
	gl_FragColor = vec4(HDR(outColor, exposure), 1.0);
#endif

	//gl_FragColor = vec4((outColor) * 0.06, 1.0);
}
#endif