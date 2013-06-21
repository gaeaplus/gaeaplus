#include "Util.glsl"
#include "AtmosphereParameters.glsl"
#include "AtmosphereTransform.glsl"

vec4 texture4D(sampler3D table, float r, float mu, float muS, float nu)
{
    float H = sqrt(Rt * Rt - Rg * Rg);
    float rho = sqrt(r * r - Rg * Rg);
    float rmu = r * mu;
    float delta = rmu * rmu - r * r + Rg * Rg;
    vec4 cst = rmu < 0.0 && delta > 0.0 ? vec4(1.0, 0.0, 0.0, 0.5 - 0.5 / float(RES_MU)) : vec4(-1.0, H * H, H, 0.5 + 0.5 / float(RES_MU));
	float uR = 0.5 / float(RES_R) + rho / H * (1.0 - 1.0 / float(RES_R));
    float uMu = cst.w + (rmu * cst.x + sqrt(delta + cst.y)) / (rho + cst.z) * (0.5 - 1.0 / float(RES_MU));
    // paper formula
    //float uMuS = 0.5 / float(RES_MU_S) + max((1.0 - exp(-3.0 * muS - 0.6)) / (1.0 - exp(-3.6)), 0.0) * (1.0 - 1.0 / float(RES_MU_S));
    // better formula
    float uMuS = 0.5 / float(RES_MU_S) + (atan(max(muS, -0.1975) * tan(1.26 * 1.1)) / 1.1 + (1.0 - 0.26)) * 0.5 * (1.0 - 1.0 / float(RES_MU_S));
    float mLerp = (nu + 1.0) / 2.0 * (float(RES_NU) - 1.0);
    float uNu = floor(mLerp);
    mLerp = mLerp - uNu;
    return texture3D(table, vec3((uNu + uMuS) / float(RES_NU), uMu, uR)) * (1.0 - mLerp) +
           texture3D(table, vec3((uNu + uMuS + 1.0) / float(RES_NU), uMu, uR)) * mLerp;
}

// transmittance(=transparency) of atmosphere for infinite ray (r,mu)
// (mu=cos(view zenith angle)), intersections with ground ignored
vec3 transmittance(float r, float mu, sampler2D transmittanceSampler) {
	vec2 uv = getTransmittanceUV(r, mu);
    return texture2D(transmittanceSampler, uv).rgb;
}

// transmittance(=transparency) of atmosphere
vec3 transmittanceWithShadowMap(float r, float mu, sampler2D transmittanceSampler,
												  sampler2DShadow shadowSampler,
												  vec4 shadowCoords) {
	vec3 sphereShadow = mu < -sqrt(1.0 - (Rg / r) * (Rg / r)) ? vec3(0.0) : transmittance(r, mu, transmittanceSampler);
	float shadow = shadow2DProj(shadowSampler, shadowCoords).r;
	vec3 terrainShadow = clamp(shadow, 0.0, 1.0) * transmittance(r, mu, transmittanceSampler);

    return min(sphereShadow, terrainShadow);
}

// transmittance(=transparency) of atmosphere for infinite ray (r,mu)
// (mu=cos(view zenith angle)), or zero if ray intersects ground
vec3 transmittanceWithShadow(float r, float mu, sampler2D transmittanceSampler) {
    return mu < -sqrt(1.0 - (Rg / r) * (Rg / r)) ? vec3(0.0) : transmittance(r, mu, transmittanceSampler);
}

// transmittance(=transparency) of atmosphere between x and x0
// assume segment x,x0 not intersecting ground
// r=||x||, mu=cos(zenith angle of [x,x0) ray at x), v=unit direction vector of [x,x0) ray
vec3 transmittance(float r, float mu, vec3 v, vec3 x0, sampler2D transmittanceSampler) {
    vec3 result;
    float r1 = length(x0);
    float mu1 = dot(x0, v) / r1;
    if (mu > 0.0) {
        result = min(transmittance(r, mu, transmittanceSampler) / transmittance(r1, mu1, transmittanceSampler), 1.0);
    } else {
        result = min(transmittance(r1, -mu1, transmittanceSampler) / transmittance(r, -mu, transmittanceSampler), 1.0);
    }
    return result;
}

// transmittance(=transparency) of atmosphere between x and x0
// assume segment x,x0 not intersecting ground
// d = distance between x and x0, mu=cos(zenith angle of [x,x0) ray at x)
vec3 transmittance(float r, float mu, float d, sampler2D transmittanceSampler) {
    vec3 result;
    float r1 = sqrt(r * r + d * d + 2.0 * r * mu * d);
    float mu1 = (r * mu + d) / r1;
    if (mu > 0.0) {
        result = min(transmittance(r, mu, transmittanceSampler) / transmittance(r1, mu1, transmittanceSampler), 1.0);
    } else {
        result = min(transmittance(r1, -mu1, transmittanceSampler) / transmittance(r, -mu, transmittanceSampler), 1.0);
    }
    return result;
}

vec3 irradiance(sampler2D sampler, float r, float muS) {
    vec2 uv = getIrradianceUV(r, muS);
    return texture2D(sampler, uv).rgb;
}
// Rayleigh phase function
float phaseFunctionR(float mu) {
    return (3.0 / (16.0 * M_PI)) * (1.0 + mu * mu);
}

// Mie phase function
float phaseFunctionM(float mu) {
	return 1.5 * 1.0 / (4.0 * M_PI) * (1.0 - mieG*mieG) * pow(1.0 + (mieG*mieG) - 2.0*mieG*mu, -3.0/2.0) * (1.0 + mu * mu) / (2.0 + mieG*mieG);
}

// approximated single Mie scattering (cf. approximate Cm in paragraph "Angular precision")
vec3 getMie(vec4 rayMie) { // rayMie.rgb=C*, rayMie.w=Cm,r
	return rayMie.rgb * rayMie.w / max(rayMie.r, 1e-4) * (betaR.r / betaR);
}

vec3 getInscatter(vec3 x, vec3 v, float d, vec3 s,
										float r, float mu,
										sampler3D inscatterSampler,
										sampler2D transmittanceSampler) {
	float muS = dot(x, s) / r;
	float nu = dot(v, s);

	//horizon line fix!
	float fade = (r-Rg)/8.0; 
	nu *= clamp(pow(fade, 0.2), 0.0, 1.0);

	float phaseR = phaseFunctionR(nu);
	float phaseM = phaseFunctionM(nu);

	float rG = max(sqrt(r * r + d * d + 2.0 * r * mu * d), Rg + 0.01);
    float muG = (r * mu + d) / rG;
	float muSG = (r * muS + d * nu) / rG;

	vec4 inscatter = max(texture4D(inscatterSampler, r, mu, muS, nu), 0.0);
	vec4 inscatterG = max(texture4D(inscatterSampler, rG, muG, muSG, nu), 0.0);

	vec3 ins = max(inscatter.rgb * phaseR + getMie(inscatter) * phaseM, 0.0);
	vec3 insG = max(inscatterG.rgb * phaseR + getMie(inscatterG) * phaseM, 0.0);
	
	vec3 t = transmittance(r, mu, d, transmittanceSampler);
	return max(ins - t * insG, 0.0) * ISun;
}

vec3 getGroundColor(vec3 x, vec3 v, float d, vec3 s, float r, float mu,
								    sampler2D transmittanceSampler,
								    sampler2D irradianceSampler,
								    vec3 color, float diffuse, float isShadow)
{
    vec3 result;
	vec3 x0 = x + d * v;
	float rx0 = clamp(length(x0), Rg, Rt);
	vec3 n = normalize(x0);

	float muS = dot(n, s);

#ifdef _SHADOW_
	vec3 sunLight = isShadow * transmittance(rx0, muS, transmittanceSampler);
#else
	vec3 sunLight = transmittanceWithShadow(rx0, muS, transmittanceSampler);
#endif
	// precomputed sky light (irradiance) (=E[L*]) at x0
	vec3 groundSkyLight = irradiance(irradianceSampler, rx0, muS);


	//////////ground sun color////////
	vec3 groundColor = pow(color, vec3(2.6)) * (max(diffuse, 0.0) * sunLight + groundSkyLight) * ISun / M_PI;
	//////////////////////////////

	// attenuation of light to the viewer, T(x,x0)
	vec3 attenuation = transmittance(r, mu, v, x0, transmittanceSampler);
	result = attenuation * groundColor; //=R[L0]+R[L*]
    return result;
}