uniform sampler2D heightTex;

uniform float texelSize;
uniform vec2 texelScale;
uniform vec4 sector;

uniform float exaggeration;

varying vec2 texCoord;
varying vec2 texRecCoord;

varying vec3 vertexWorld;
varying float earthRadi;

#ifdef _VERTEX_

void main()
{
	vertexWorld = gl_Vertex.xyz + gl_LightSource[7].diffuse.xyz;
    earthRadi = length(gl_LightSource[7].diffuse.xyz);

    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    texCoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    texRecCoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
}

#else

void main(void)
{

    float min = 0.0;
	float max = 1.0;

	if(texCoord.x < min || texCoord.x > max || texCoord.y < min || texCoord.y > max){discard;}
	if(texRecCoord.x < min || texRecCoord.x > max || texRecCoord.y < min || texRecCoord.y > max){discard;}
	
    
    float tW = texelScale.x * texelSize * cos(sector.x*(1.0-texCoord.y) + sector.y*texCoord.y);
    float tH = texelScale.y * texelSize;

    vec3 north = vec3(0.0, 1.0, 0.0);
	vec3 normalG = normalize(vertexWorld);

    //float exagg = max(exaggeration, 1.0);
	vec3 tangentN = exaggeration * normalize(north - (dot(north, normalG) * normalG));
	vec3 tangentE = exaggeration * normalize(cross(tangentN, normalG));
	mat3 tangentTrans = mat3(tangentE.x, tangentN.x, normalG.x,
				    		 tangentE.y, tangentN.y, normalG.y,
				    		 tangentE.z, tangentN.z, normalG.z);

    //parallax displacement
    //float height0 = textureOffset(heightTex, texCoord, ivec2(0,0)).r;
    //float displace = height0 - (length(vertexWorld) - earthRadi);
    //vec3 forward = normalize(tangentTrans * normalize((gl_ModelViewMatrixInverse * vec4(0.0, 0.0, 1.0, 0.0)).xyz));
    //vec2 offset = (forward.xy/forward.z) * displace;
    //offset.x /= -tW;
    //offset.y /= tH;
    //offset.x /= 512.0;
    //offset.y /= 512.0;
    //vec2 texCoordOff = texCoord + offset;

	float offset = 1.0/511.0;

    float height0 = texture2D(heightTex, clamp(texCoord + offset*vec2( 0.0,  0.0), 0.0, 1.0)).r;
    float height1 = texture2D(heightTex, clamp(texCoord + offset*vec2(-1.0,  1.0), 0.0, 1.0)).r - height0;
    float height2 = texture2D(heightTex, clamp(texCoord + offset*vec2( 0.0,  1.0), 0.0, 1.0)).r - height0;
    float height3 = texture2D(heightTex, clamp(texCoord + offset*vec2( 1.0,  1.0), 0.0, 1.0)).r - height0;

    float height8 = texture2D(heightTex, clamp(texCoord + offset*vec2(-1.0,  0.0), 0.0, 1.0)).r - height0;
    float height4 = texture2D(heightTex, clamp(texCoord + offset*vec2( 1.0,  0.0), 0.0, 1.0)).r - height0;

    float height7 = texture2D(heightTex, clamp(texCoord + offset*vec2(-1.0, -1.0), 0.0, 1.0)).r - height0;
    float height6 = texture2D(heightTex, clamp(texCoord + offset*vec2( 0.0, -1.0), 0.0, 1.0)).r - height0;
    float height5 = texture2D(heightTex, clamp(texCoord + offset*vec2( 1.0, -1.0), 0.0, 1.0)).r - height0;

    vec3 normal1 = normalize(cross(vec3(-1.0 * tW, 1.0 * tH, height1), vec3( 0.0 * tW, 1.0 * tH ,height2)));
    vec3 normal2 = normalize(cross(vec3( 0.0 * tW, 1.0 * tH, height2), vec3( 1.0 * tW, 1.0 * tH ,height3)));
    vec3 normal3 = normalize(cross(vec3( 1.0 * tW, 1.0 * tH, height3), vec3( 1.0 * tW, 0.0 * tH ,height4)));
    vec3 normal4 = normalize(cross(vec3( 1.0 * tW, 0.0 * tH, height4), vec3( 1.0 * tW,-1.0 * tH ,height5)));
    vec3 normal5 = normalize(cross(vec3( 1.0 * tW,-1.0 * tH, height5), vec3( 0.0 * tW,-1.0 * tH ,height6)));
    vec3 normal6 = normalize(cross(vec3( 0.0 * tW,-1.0 * tH, height6), vec3(-1.0 * tW,-1.0 * tH ,height7)));
    vec3 normal7 = normalize(cross(vec3(-1.0 * tW,-1.0 * tH, height7), vec3(-1.0 * tW, 0.0 * tH ,height8)));

    vec3 normal = -normalize((normal1+normal2+normal3+normal4+normal5+normal6+normal7)/7.0f);    
	normal = transpose(tangentTrans) * normal;

	gl_FragColor = vec4((normal+1.0)/2.0, 12000.0 + height0);
}

#endif