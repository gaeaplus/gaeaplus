#version 150 compatibility

#ifdef _VERTEX_

in vec4 in_vertex;
in vec3 in_data;

out vec3 data;

void main() {
    vec4 view = gl_ModelViewMatrix * in_vertex;
	gl_Position = gl_ProjectionMatrix * view;
	data = in_data;
}

#endif
#ifdef _GEOMETRY_

in vec3 data[2];
out vec2 texCoord;

uniform float time;
uniform float drawLength;

layout(lines) in;
layout(triangle_strip, max_vertices = 8) out;

void main() {

    vec4 ver1 = gl_in[0].gl_Position;
    vec4 ver2 = gl_in[1].gl_Position;


	vec3 data1 = data[0];
	vec3 data2 = data[1];

	float brp1 = data1.x;
	float brp2 = data2.x;

	float time1 = data1.y;
	float time2 = data2.y;

    float pathLen1 = data1.z;
	float pathLen2 = data2.z;

    if(pathLen1 > drawLength){
        return;
    }
    if(pathLen2 > drawLength){
        float fac = (drawLength-pathLen1)/(pathLen2-pathLen1);
		ver2 = (1.0-fac) * ver1 + fac * ver2;
    }

	if(time1 > time){
		return;
	}
	if(time2 > time){
		float fac = (time-time1)/(time2-time1);
		ver2 = (1.0-fac) * ver1 + fac * ver2;
	}

    //draw quad point
	gl_Position.x = ver2.x - brp2;
	gl_Position.y = ver2.y + brp2;
	gl_Position.z = ver2.z;
	gl_Position.w = ver2.w;
	texCoord = vec2(-1.0, 1.0);
    EmitVertex();

	gl_Position.x = ver2.x + brp2;
	gl_Position.y = ver2.y + brp2;
	gl_Position.z = ver2.z;
	gl_Position.w = ver2.w;
	texCoord = vec2(1.0, 1.0);
    EmitVertex();

	gl_Position.x = ver2.x - brp2;
	gl_Position.y = ver2.y - brp2;
	gl_Position.z = ver2.z;
	gl_Position.w = ver2.w;
	texCoord = vec2(-1.0, -1.0);
    EmitVertex();

	gl_Position.x = ver2.x + brp2;
	gl_Position.y = ver2.y - brp2;
	gl_Position.z = ver2.z;
	gl_Position.w = ver2.w;
	texCoord = vec2(1.0, -1.0);
    EmitVertex();
	EndPrimitive();

    //draw quad between points
    vec2 dir = normalize((ver2-ver1).xy);
	vec4 n = vec4(dir.y, -dir.x, 0.0, 0.0);

	gl_Position = ver1 + n * brp1;
	texCoord = vec2(0.0, 1.0);
    EmitVertex();

	gl_Position = ver2 + n * brp2;
	texCoord = vec2(0.0, 1.0);
    EmitVertex();

	gl_Position = ver1 - n * brp1;
	texCoord = vec2(0.0, -1.0);
    EmitVertex();

	gl_Position = ver2 - n * brp2;
	texCoord = vec2(0.0, -1.0);
    EmitVertex();
	EndPrimitive();
}

#endif
#ifdef _FRAGMENT_

uniform vec4 color;

in vec2 texCoord;
out vec4 out_Color;

void main() {

    //if(gl_FrontFacing){
    //    discard;
    //}
    //else{
    //    out_Color = vec4(0.0,1.0,0.0,1.0);
    //}


	float l = length(texCoord);
	float d = smoothstep(0.3, 1.0, l);
	out_Color = vec4(color.rgb, 1.0 - clamp(d, 1.0-color.a, 1.0));
}

#endif