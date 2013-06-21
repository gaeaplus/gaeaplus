////////////////////////////////////////////////////////////////////
// COMMON CONSTANTS
////////////////////////////////////////////////////////////////////

const float M_PI = 3.141592657;
const float M_PI2 = 1.5707963;

////////////////////////////////////////////////////////////////////
// COMMON FUNCTIONS
////////////////////////////////////////////////////////////////////

vec3 HDR(vec3 L, float exposure) {
    L = L * exposure;
    L.r = L.r < 1.413 ? pow(L.r * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.r);
    L.g = L.g < 1.413 ? pow(L.g * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.g);
    L.b = L.b < 1.413 ? pow(L.b * 0.38317, 1.0 / 2.2) : 1.0 - exp(-L.b);
	//L = 1.0 - exp(-L);
    return L;
}

vec3 HDRI(vec3 color){
	color.r = color.r < 0.75662 ? pow(color.r, 2.2) / 0.38317 : -log(1.0 - color.r);
	color.g = color.g < 0.75662 ? pow(color.g, 2.2) / 0.38317 : -log(1.0 - color.g);
	color.b = color.b < 0.75662 ? pow(color.b, 2.2) / 0.38317 : -log(1.0 - color.b);
	return color;
}

//vec4 getNormalClient(sampler2D normalSampler, vec2 textureCoords, float exaggeration){
//	vec4 normalColor = texture2D(normalSampler, textureCoords);
//	return vec4(normalize(2.0 * (normalColor.rgb - 0.5)), normalColor.a);
//}

/*
vec4 getNormalClient(sampler2D normalSampler, vec2 textureCoords, float exaggeration){
	vec4 n = texture2D(normalSampler, textureCoords);

	float x = 2.0 * (n.x - 0.5);
	float y = 2.0 * (n.y - 0.5);
	float z = sqrt(1.0 - x*x - y*y);

	return vec4(normalize(vec3(x,y,z)), 1.0);
}
*/

vec4 getNormal(sampler2D normalSampler, vec2 coords){
    vec4 normalColor = texture2D(normalSampler, coords);
    return vec4(normalize(2.0 * (normalColor.rgb - 0.5)), normalColor.a);
}

/*
vec4 getTerrainNormal(vec3 vertexWorldPosition, sampler2D normalSampler, vec2 coords, float exaggeration){
	vec3 north = vec3(0.0, 1.0, 0.0);
	vec3 normal = normalize(vertexWorldPosition);
	vec3 tangentN = exaggeration * normalize(north - (dot(north, normal) * normal));
	vec3 tangentE = exaggeration * normalize(cross(tangentN, normal));

	mat3 tangentTrans = mat3(tangentE.x, tangentN.x, normal.x,
				    		 tangentE.y, tangentN.y, normal.y,
				    		 tangentE.z, tangentN.z, normal.z);

	vec2 n = texture2D(normalSampler, coords).rg;
	float x = 2.0 * (n.r - 0.5);
	float y = 2.0 * (n.g - 0.5);
	float z = sqrt(1.0 - x*x - y*y);
	vec3 normalBump = vec3(x,y,z);

	return vec4(transpose(tangentTrans) * normalBump, 1.0);
}
*/

float getShadow(vec3 vertexWorldPosition, vec3 sunDirection, sampler2DShadow shadowSampler, vec4 shadowCoords){
	float mapShadow = clamp(shadow2DProj(shadowSampler, shadowCoords).r, 0.0, 1.0);
	float sphereShadow = clamp(dot(-sunDirection, vertexWorldPosition), 0.0, 1.0);
    return min(sphereShadow, mapShadow);
}

float getShadowSphere(vec3 vertexWorldPosition, vec3 sunDirection){
	return clamp(dot(-sunDirection, vertexWorldPosition), 0.0, 1.0);
}

float getDistance(float zNear, float zFar, float depth){
	return (zFar * zNear)/(zFar - (depth * (zFar - zNear)));
}

vec4 getEyeCoord(vec4 ncc, mat4 projectionInverse){
	vec4 eyeCoordN = projectionInverse * ncc;
	vec4 eyeCoord = eyeCoordN / eyeCoordN.w;
	eyeCoord.z = -eyeCoord.z;
	return eyeCoord;
}

float getDepth(float zNear, float zFar, float distance){
	float a = zFar / ( zFar - zNear );
	float b = zFar * zNear / ( zNear - zFar );
	return (a + b/distance);
}

bool isInTexture(vec2 texCoord){
	return !(any(bvec2(any(greaterThan(texCoord, vec2(1.0,1.0))), any(lessThan(texCoord, vec2(0.0,0.0))))));
}

vec3 hsvToRgb(float h, float s, float v)
{
	if (s <= 0 ) { return vec3 (v); }
	h = h * 6.0;
	float c = v*s;
	float x = (1.0-abs((mod(h,2.0)-1.0)))*c;
	float m = v-c;
	float r = 0.0;
	float g = 0.0;
	float b = 0.0;

	if (h < 1) { r = c; g = x;b = 0.0;}
	else if (h < 2) { r = x; g = c; b = 0.0; }
	else if (h < 3) { r = 0.0; g = c; b = x; }
	else if (h < 4) { r = 0.0; g = x; b = c; }
	else if (h < 5) { r = x; g = 0.0; b = c; }
	else  { r = c; g = 0.0; b = x; }

	return vec3(r+m,g+m,b+m);
}