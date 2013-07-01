uniform mat4 eyeToWorld;
uniform float normalFactor;

uniform sampler2D colorSampler;
uniform sampler2D bumpSampler;
uniform sampler2D envSampler;

varying vec4 v_diffuseF;
varying vec4 v_diffuseB;

varying vec4 v_material;
varying float useTexture;

varying vec3 v_normal;
varying vec2 v_texcoord;

#ifdef _BUMPMAP_
varying vec4 v_tangent;
#endif

#ifdef _VERTEX_
void main()
{
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	v_normal = normalFactor * (eyeToWorld * (gl_ModelViewMatrix * vec4(gl_Normal, 0.0))).xyz;
	v_texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
#ifdef _BUMPMAP_
	v_tangent = gl_MultiTexCoord1;
#endif
	
	v_diffuseF = gl_FrontMaterial.diffuse;
	v_diffuseB = gl_BackMaterial.diffuse;

	vec4 s = gl_FrontMaterial.specular;
	float si = (s.x + s.y + s.z)/3.0;
	v_material.x = si;

	v_material.y = gl_FrontMaterial.shininess;
	
	vec4 e = gl_FrontMaterial.emission;
	v_material.z = (e.x+e.y+e.z)/3.0;
	
	useTexture = gl_LightSource[7].specular.a;
}

#else

void main()
{

	vec4 f_color = texture2D(colorSampler, v_texcoord);
	if(useTexture < 0.5){
		f_color = gl_FrontFacing ? v_diffuseF : v_diffuseB;
	}

	vec3 normal = v_normal;

#ifdef _BUMPMAP_
	
	vec3 bumpNormal = normalize(2.0 * (texture2D(bumpSampler, v_texcoord) - 0.5)).xyz;
	
	vec3 b = normalize(cross(v_normal.xyz, v_tangent.xyz));
	vec3 n = normalize(v_normal.xyz);
	vec3 t = normalize(v_tangent.xyz);
	
	mat3 tangentTrans = mat3(t.x, b.x, n.x,
							 t.y, b.y, n.y,
					  		 t.z, b.z, n.z);

	normal = transpose(tangentTrans) * bumpNormal;

#endif

	gl_FragData[0] = f_color;
	gl_FragData[1] = vec4((normalize(normal) + 1.0)/2.0, normalFactor);
	gl_FragData[2] = vec4(v_material.xyz, 0.0);
}
#endif