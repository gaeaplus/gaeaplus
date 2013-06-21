uniform sampler2D colorSampler;
uniform float u_drawDistance;

varying vec4 v_texCoord;
varying vec4 v_texRecCoord;
varying float v_color;

#ifdef _VERTEX_
void main(){
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
	v_texCoord = gl_TextureMatrix[0] * gl_MultiTexCoord0;
	v_texRecCoord = gl_TextureMatrix[1] * gl_MultiTexCoord1;

	//set vertex alpha
	float verDist = length((gl_ModelViewMatrix * gl_Vertex).xyz);
	float alpha = smoothstep((1.0/2.0) * u_drawDistance, u_drawDistance, verDist);
	v_color = (1.0 - alpha) * gl_Color.a;
}

#else

void main(){

	vec4 color = texture2D(colorSampler, v_texCoord.xy);

	float min = 0.0;
	float max = 1.0;

	if(v_texCoord.x < min || v_texCoord.x > max
		|| v_texCoord.y < min || v_texCoord.y > max)
	{
		discard;
	}
	else
	{
		if(v_texRecCoord.x < min || v_texRecCoord.x > max
			|| v_texRecCoord.y < min || v_texRecCoord.y > max){discard;}
	}

	gl_FragColor = vec4(color.rgb, color.a * v_color);
}
#endif