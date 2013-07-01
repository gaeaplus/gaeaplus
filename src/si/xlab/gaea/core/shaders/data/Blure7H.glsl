
uniform float sampleDist;
uniform float maskWeight;
uniform float maskPower;
uniform sampler2D RT;
uniform sampler2D maskSampler;
varying vec2 texCoord;

#ifdef _VERTEX_
void main()
{
	texCoord = gl_Vertex.xy * 0.5 + 0.5;
    gl_Position = gl_Vertex;
}

#else

void main(void)
{
	vec4 sum = vec4(0.0);

	sum += texture2D(RT, vec2(texCoord.x - 1.0*sampleDist, texCoord.y)) * 0.25/4.0;
	sum += texture2D(RT, vec2(texCoord.x - 0.666*sampleDist, texCoord.y)) * 0.5/4.0;
	sum += texture2D(RT, vec2(texCoord.x - 0.333*sampleDist, texCoord.y)) * 0.75/4.0;
	sum += texture2D(RT, vec2(texCoord.x, texCoord.y)) * 1.0/4.0;
	sum += texture2D(RT, vec2(texCoord.x + 0.333*sampleDist, texCoord.y)) * 0.75/4.0;
	sum += texture2D(RT, vec2(texCoord.x + 0.666*sampleDist, texCoord.y)) * 0.5/4.0;
	sum += texture2D(RT, vec2(texCoord.x + 1.0*sampleDist, texCoord.y)) * 0.25/4.0;

	vec4 color = texture2D(RT, vec2(texCoord.x, texCoord.y));
	float mask = texture2D(maskSampler, vec2(texCoord.x, texCoord.y)).r * maskWeight;
	mask = (maskWeight < 0.001) ? 1.0 : pow(mask, maskPower);
	sum = sum * mask + color * (1.0 - mask);

	gl_FragColor = sum;
}

#endif