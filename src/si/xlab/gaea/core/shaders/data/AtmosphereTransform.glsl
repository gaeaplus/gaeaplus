
//elipsoidal globe parameters
const float eHeight = 6378137;
const float pHeight = 6356752;
const float eHpH = eHeight/pHeight;
const float eHpH2 = eHpH * eHpH;
const float eH2 = eHeight * eHeight;

//atmosphere uses different coordinate system compared to wwj
vec3 coordTrans(vec3 vecIn)
{
	mat3 trans = mat3(0.0, 0.0, 1.0,
					  1.0, 0.0, 0.0,
					  0.0, 1.0, 0.0);
	return (trans * vecIn);
}

vec3 globeIntersection(vec3 origin, vec3 direction, float elevation){

	float ep = (eHeight+elevation)/(pHeight+elevation);
	float ep2 = ep * ep;
	float e2 = (eHeight+elevation) * (eHeight+elevation);

	vec3 intersection = vec3(0.0, 0.0, 0.0);
	vec3 s = origin;
	vec3 v = normalize(direction);

	float a = v.x * v.x + ep2 * v.y * v.y + v.z * v.z;
	float b = 2.0 * (s.x * v.x + ep2 * s.y * v.y + s.z * v.z);
	float c = s.x * s.x + ep2 * s.y * s.y + s.z * s.z - e2;
	float discriminant = b * b - 4.0 * a * c;
	if(discriminant > 0.0){
		float discriminantRoot = sqrt(discriminant);
		intersection.x = (-b - discriminantRoot) / (2.0 * a);
		intersection.y = (-b + discriminantRoot) / (2.0 * a);
		intersection.z = 1.0;
	}
	return intersection;
}

vec3 fromWorldToAtm(vec3 vecIn)
{
	vec3 result;
	vec3 rayD = normalize(vecIn);
	float a = rayD.x * rayD.x + eHpH2 * rayD.y * rayD.y + rayD.z * rayD.z;
	float far = ((sqrt(4.0 * a * eH2)) / (2.0 * a));
	result = rayD * ((length(vecIn) - far) * 0.001 + Rg);
	return coordTrans(result);
}

void calcAtmParam(vec3 camWorldPosition, vec3 vertexWorldPosition, inout vec3 atmRayStart,
																	   inout vec3 atmRayDir,
																	   inout float atmRayLength,
																	   inout float r, inout float mu){
	atmRayStart = fromWorldToAtm(camWorldPosition);
	vec3 vwp = fromWorldToAtm(vertexWorldPosition);

	atmRayDir = normalize(vwp - atmRayStart);
	r = length(atmRayStart);
    mu = dot(atmRayStart, atmRayDir) / r;

	float distSign = r * r * (mu * mu - 1.0) + Rt * Rt;
    float dist = (-r * mu - sqrt(distSign));

	if (distSign * dist > 0.0) { // if x in space and ray intersects atmosphere
        // move x to nearest intersection of ray with top atmosphere boundary
        atmRayStart += dist * atmRayDir;
        mu = (r * mu + dist) / Rt;
        r = Rt;
		atmRayLength -= dist;
    }
}

vec2 getTransmittanceUV(float r, float mu) {
    float uR, uMu;
	uR = sqrt((r - Rg) / (Rt - Rg));
	uMu = atan((mu + 0.15) / (1.0 + 0.15) * tan(1.5)) / 1.5;
    return vec2(uMu, uR);
}

vec2 getIrradianceUV(float r, float muS) {
    float uR = (r - Rg) / (Rt - Rg);
    float uMuS = (muS + 0.2) / (1.0 + 0.2);
    return vec2(uMuS, uR);
}