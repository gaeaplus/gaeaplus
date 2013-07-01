uniform mat4 eyeToWorld;
uniform float normalFactor;
uniform sampler2D colorSampler;

varying float v_alpha;
varying vec3 v_normal;
varying vec2 v_texcoord;

#ifdef _VERTEX_
void main()
{
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	v_normal = normalFactor * (eyeToWorld * (gl_ModelViewMatrix * vec4(gl_Normal, 0.0))).xyz;
	v_texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
	v_alpha = gl_Color.a;
}

#else

void main()
{

	vec4 f_color = texture2D(colorSampler, v_texcoord);
	float alpha = v_alpha * f_color.a;

	if(alpha < 0.1){discard;}

	gl_FragData[0] = vec4(f_color.xyz, v_alpha * f_color.a);
	gl_FragData[1] = vec4((normalize(v_normal) + 1.0)/2.0, normalFactor);
	gl_FragData[2] = vec4(0.0);
}
#endif