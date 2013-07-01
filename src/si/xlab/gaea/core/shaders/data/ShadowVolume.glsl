#version 150 compatibility

#ifdef _VERTEX_

in vec4 in_vertex;

void main() {
    gl_Position = gl_ModelViewMatrix * in_vertex;
}

#endif
#ifdef _GEOMETRY_

uniform vec4 u_light;
uniform float u_zNear;

layout(triangles_adjacency) in;
layout(triangle_strip, max_vertices = 15) out;

void main() {

	mat4 p = gl_ProjectionMatrix;
	mat4 pInv = inverse(p);

	vec4 light =  gl_ModelViewMatrix * vec4(normalize(u_light).xyz,0.0);

    vec4 ver1 = gl_in[0].gl_Position;
    vec4 ver2 = gl_in[1].gl_Position;
	vec4 ver3 = gl_in[2].gl_Position;
	vec4 ver4 = gl_in[3].gl_Position;
    vec4 ver5 = gl_in[4].gl_Position;
	vec4 ver6 = gl_in[5].gl_Position;

	vec3 facePN = cross((ver5 - ver1).xyz, (ver3 - ver1).xyz);

	float faceP = sign(dot(facePN, light.xyz));

	if(faceP < 0.5 || length(facePN) < 50.0){
		return;
	}

	vec3 face1N1 = cross((ver3 - ver1).xyz, (ver2 - ver1).xyz);
	vec3 face1N = (length(face1N1) < 50.0) ? -facePN : face1N1;

	vec3 face2N1 = cross((ver6 - ver1).xyz, (ver5 - ver1).xyz);
	vec3 face2N = (length(face2N1) < 50.0) ? -facePN : face2N1;

	vec3 face3N1 = cross((ver4 - ver5).xyz, (ver3 - ver5).xyz);
	vec3 face3N = (length(face3N1) < 50.0) ? -facePN : face3N1;

	float face1 = sign(dot(face1N, light.xyz));
	float face2 = sign(dot(face2N, light.xyz));
	float face3 = sign(dot(face3N, light.xyz));

	vec4 move = 10000000.0 * light;

	vec4 ver1F = ver1 + move;
	vec4 ver3F = ver3 + move;
	vec4 ver5F = ver5 + move;

	if(faceP != face1){
		gl_Position = p*ver3;
    	EmitVertex();
		gl_Position = p*ver1;
    	EmitVertex();
		gl_Position = p*ver3F;
    	EmitVertex();
		gl_Position = p*ver1F;
    	EmitVertex();
		EndPrimitive();
	}

	if(faceP != face2){
		gl_Position = p*ver1;
    	EmitVertex();
		gl_Position = p*ver5;
    	EmitVertex();
		gl_Position = p*ver1F;
    	EmitVertex();
		gl_Position = p*ver5F;
    	EmitVertex();
		EndPrimitive();
	}

	if(faceP != face3){
		gl_Position = p*ver5;
    	EmitVertex();
		gl_Position = p*ver3;
    	EmitVertex();
		gl_Position = p*ver5F;
    	EmitVertex();
		gl_Position = p*ver3F;
    	EmitVertex();
		EndPrimitive();
	}

	vec4 verP;
	vec4 verW;

	vec4 cap1;
	vec4 cap2;
	vec4 cap3;

	float t;

	//cap: vertex 1
	verW = ver5;
	t = (-u_zNear - verW.z)/light.z;
	if(!(t>0)){return;}
	verP = p * vec4(verW.x + light.x * t, verW.y + light.y * t, -u_zNear , 1.0);
	verP.z = -1.0 * verP.w;
	cap1 = verP;

	//cap: vertex 2
	verW = ver3;
	t = (-u_zNear - verW.z)/light.z;
	if(!(t>0)){return;}
	verP = p * vec4(verW.x + light.x * t, verW.y + light.y * t, -u_zNear , 1.0);
	verP.z = -1.0 * verP.w;
	cap2 = verP;

	//cap: vertex 3
	verW = ver1;
	t = (-u_zNear - verW.z)/light.z;
	if(!(t>0)){return;}
	verP = p * vec4(verW.x + light.x * t, verW.y + light.y * t, -u_zNear , 1.0);
	verP.z = -1.0 * verP.w;
	cap3 = verP;

	/*
	//if(sign(dot(facePN, vec3(0.0, 0.0, 1.0))) < 0.5){
	if(facePN.z < 0.0){
		vec4 tmp = cap3;
		cap3 = cap1;
		cap1 = tmp;
	}
	*/

	gl_Position = cap1;
	EmitVertex();
	gl_Position = cap2;
	EmitVertex();
	gl_Position = cap3;
	EmitVertex();
	EndPrimitive();
}

#endif
#ifdef _FRAGMENT_

uniform float u_zNear;
uniform float u_zFar;

out vec4 out_Color;

void main() {

	float d = (u_zFar * u_zNear)/(u_zFar - (gl_FragCoord.z * (u_zFar - u_zNear)));

	float fac = gl_FrontFacing ? -1.0 : 1.0;
	out_Color.r = fac;
	out_Color.g = fac * d;
	out_Color.a = d;
}

#endif