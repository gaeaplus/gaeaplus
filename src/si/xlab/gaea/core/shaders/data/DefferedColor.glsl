uniform mat4 eyeToWorld;
uniform float normalFactor;


varying vec4 v_color;
varying vec3 v_normal;

#ifdef _VERTEX_
void main()
{
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	v_normal = normalFactor * (eyeToWorld * (gl_ModelViewMatrix * vec4(gl_Normal, 0.0))).xyz;
	//v_normal = normalFactor * gl_Normal;
	//v_normal = gl_Normal;
	v_color = gl_Color;
}

#else

void main()
{
	if(v_color.a < 0.1){discard;}
	gl_FragData[0] = v_color;
	gl_FragData[1] = vec4((normalize(v_normal) + 1.0)/2.0, normalFactor);
	gl_FragData[2] = vec4(0.0);
}
#endif