#include "Util.glsl"
#include "AtmosphereParameters.glsl"

uniform vec4 lightDirection;
uniform vec3 lightColor;
uniform vec4 cameraWorldPosition;

uniform float zNear;
uniform float zFar;

uniform sampler2D colorSampler;
uniform sampler2D normalSampler;
uniform sampler2D depthSampler;

#ifdef _SHADOW_
uniform mat4 eyeToShadowTextureTransform;
uniform sampler2DShadow shadowSampler;
#endif

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

void main()
{
	float depth = texture2D(depthSampler, coords).r;
	if(depth == 1.0){discard;}
	float dep = getDistance(zNear, zFar, depth);
	vec4 screen = vec4(coords * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
	vec4 vE = (gl_ProjectionMatrixInverse * screen);
	vE /= vE.w;
	vec3 vW = cameraWorldPosition.xyz + normalize(ray) * dep;

	vec4 color = texture2D(colorSampler, coords);
	
	/////////diffuse & specular//////////
	vec4 normal = getNormal(normalSampler, coords);
	float diffuse = 0.7;
	if(normal.a > 0.2){
		vec3 normalVec = normalize(normal.xyz);
 		diffuse = max(dot(normalVec, -normalize(lightDirection.xyz)), 0.0);
	}
	//////////////////////////////////////

	float shadow = 1.0;
#ifdef _SHADOW_
	vec4 shadowCoord = eyeToShadowTextureTransform * vE;
	shadow = shadow2DProj(shadowSampler, shadowCoord).r;
#endif
	shadow *= getShadowSphere(vW, normalize(lightDirection.xyz));
	shadow = clamp(shadow, 0.0, 1.0);

	vec3 ambientLight = vec3(0.3, 0.3, 0.3);
	vec3 colorLight = (shadow * diffuse * lightColor + ambientLight) * color.rgb;
	gl_FragColor = vec4(colorLight, 1.0);
	gl_FragDepth = depth;
	//gl_FragColor = vec4(texture2D(normalSampler, coords).xyz, 1.0);
}
#endif