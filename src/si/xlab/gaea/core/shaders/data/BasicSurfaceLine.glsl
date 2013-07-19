#extension GL_ARB_explicit_attrib_location : enable

#ifdef _VERTEX_

layout(location = 0) in vec4 in_vertex;
layout(location = 1) in vec4 in_color;
layout(location = 2) in vec2 in_data;

out vec2 g_data;
out vec4 g_color;

void main() {
	g_data = in_data;
	g_color = in_color;
    vec4 view = gl_ModelViewMatrix * in_vertex;
	gl_Position = gl_ProjectionMatrix * view;
}

#endif
#ifdef _GEOMETRY_

in vec2 g_data[2];
in vec4 g_color[2];

out vec4 f_color;

layout(lines) in;
layout(triangle_strip, max_vertices = 4) out;

void main() {

    vec4 ver1 = gl_in[0].gl_Position;
    vec4 ver2 = gl_in[1].gl_Position;

	vec2 data1 = g_data[0];
	vec2 data2 = g_data[1];

	vec4 color1 = g_color[0];
	vec4 color2 = g_color[1];

	float width1 = data1.x/512.0;
	float width2 = data2.x/512.0;

    //draw quad between points
    vec2 dir = normalize((ver2-ver1).xy);
	vec4 n = vec4(dir.y, -dir.x, 0.0, 0.0);

	if(length((ver2-ver1).xy) < 1.0/512.0){
		return;
	}

	gl_Position = ver1 + n * width1;
	f_color = color1;
    EmitVertex();

	gl_Position = ver2 + n * width2;
	f_color = color2;
    EmitVertex();

	gl_Position = ver1 - n * width1;
	f_color = color1;
    EmitVertex();

	gl_Position = ver2 - n * width2;
	f_color = color2;
    EmitVertex();
	EndPrimitive();
}

#endif
#ifdef _FRAGMENT_

in vec4 f_color;
layout(location = 0) out vec4 out_color;

void main() {

	out_color = f_color;
}

#endif