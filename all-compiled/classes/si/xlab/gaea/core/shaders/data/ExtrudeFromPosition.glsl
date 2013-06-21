#version 150 compatibility

#ifdef _VERTEX_

in vec4 in_vertex;

void main() {
    gl_Position = gl_ModelViewMatrix * in_vertex;
}

#endif
#ifdef _GEOMETRY_

uniform vec4 u_position;
uniform float u_zNear;

layout(triangles_adjacency) in;
layout(triangle_strip, max_vertices = 15) out;

void emitEdgeSkirt(mat4 proj, vec4 a, vec4 b, vec4 from, float dist){
    gl_Position = proj * a; EmitVertex();
    gl_Position = proj * b; EmitVertex();
    gl_Position = proj * (a + vec4(normalize(a.xyz-from.xyz) * dist, 0.0)); EmitVertex();
    gl_Position = proj * (b + vec4(normalize(b.xyz-from.xyz) * dist, 0.0)); EmitVertex();
    EndPrimitive();
}

void emitExtrudedTriangle(mat4 proj, vec4 a, vec4 b, vec4 c, vec4 from, float dist){
    gl_Position = proj * (a + vec4(normalize(a.xyz-from.xyz) * dist, 0.0)); EmitVertex();
    gl_Position = proj * (b + vec4(normalize(b.xyz-from.xyz) * dist, 0.0)); EmitVertex();
    gl_Position = proj * (c + vec4(normalize(c.xyz-from.xyz) * dist, 0.0)); EmitVertex();
    EndPrimitive();
}

void emitExtrudedTriangleClampToView(mat4 proj, vec4 a, vec4 b, vec4 c, vec4 from, float dist){
    gl_Position = proj * (a + vec4(normalize(a.xyz-from.xyz) * dist, 0.0)); 
    gl_Position.z = clamp(gl_Position.z/gl_Position.w, 0.01, 0.9);
    gl_Position.w = 0.0;
    EmitVertex();
    
    gl_Position = proj * (b + vec4(normalize(b.xyz-from.xyz) * dist, 0.0)); 
    gl_Position.z = clamp(gl_Position.z/gl_Position.w, 0.01, 0.9);
    gl_Position.w = 0.0;
    EmitVertex();
    
    gl_Position = proj * (c + vec4(normalize(c.xyz-from.xyz) * dist, 0.0)); 
    gl_Position.z = clamp(gl_Position.z/gl_Position.w, 0.01, 0.9);
    gl_Position.w = 0.0;
    EmitVertex();
    EndPrimitive();
}

void emitTriangleClampToView(mat4 proj, vec4 a, vec4 b, vec4 c){
    gl_Position = proj * a; 
    gl_Position.z = clamp(gl_Position.z/gl_Position.w, -1.0, 1.0);
    gl_Position.w = 0.0;
    EmitVertex();
    
    gl_Position = proj * b;
    gl_Position.z = clamp(gl_Position.z/gl_Position.w, -1.0, 1.0);
    gl_Position.w = 0.0;
    EmitVertex();
    
    gl_Position = proj * c; 
    gl_Position.z = clamp(gl_Position.z/gl_Position.w, -1.0, 1.0);
    gl_Position.w = 0.0;
    EmitVertex();
    EndPrimitive();
}

void emitTriangleProjectedToNearPlane(mat4 proj, vec4 a , vec4 b , vec4 c, 
                                                 vec3 ad, vec3 bd, vec3 cd, float nearDistance){

    float t;
    vec4 tmp;

	t = (-nearDistance - a.z)/ad.z;
	if(!(t>0)){return;}
	tmp = proj * vec4(a.x + ad.x * t, a.y + ad.y * t, -nearDistance , 1.0);
	tmp.z = -1.0 * tmp.w;
	gl_Position = tmp;
    EmitVertex();

    t = (-nearDistance - b.z)/bd.z;
	if(!(t>0)){return;}
	tmp = proj * vec4(b.x + bd.x * t, b.y + bd.y * t, -nearDistance , 1.0);
	tmp.z = -1.0 * tmp.w;
	gl_Position = tmp;
    EmitVertex();

    t = (-nearDistance - c.z)/cd.z;
	if(!(t>0)){return;}
	tmp = proj * vec4(c.x + cd.x * t, c.y + cd.y * t, -nearDistance , 1.0);
	tmp.z = -1.0 * tmp.w;
	gl_Position = tmp;
    EmitVertex();
    
    EndPrimitive();
}

bool isFrontFacing(vec4 from, vec4 a, vec4 b, vec4 c){
    vec3 dir = normalize(((a+b+c)/3.0 - from).xyz);
    vec3 facePN = cross((c-a).xyz, (b-a).xyz);
	return (sign(dot(facePN, dir.xyz)) > 0.5);
}

bool isEmpty(vec4 a, vec4 b, vec4 c){
    if(length(a-b) < 0.0001
        || length(a-c) < 0.0001
        || length(b-c) < 0.0001){
        return true;
    }
    return false;
}

void main() {

	mat4 p = gl_ProjectionMatrix;

    float skirtDist = 10000.0;
	vec4 position = gl_ModelViewMatrix * vec4(u_position.xyz - gl_LightSource[7].diffuse.xyz, 1.0);

    vec4 ver1 = gl_in[0].gl_Position;
    vec4 ver2 = gl_in[1].gl_Position;
	vec4 ver3 = gl_in[2].gl_Position;
	vec4 ver4 = gl_in[3].gl_Position;
    vec4 ver5 = gl_in[4].gl_Position;
	vec4 ver6 = gl_in[5].gl_Position;

    if(isEmpty(ver1, ver3, ver5)){ 
        return;
    }

    if(isFrontFacing(position, ver1, ver3, ver5)){
        if(isEmpty(ver1, ver2, ver3) || !isFrontFacing(position, ver1, ver2, ver3)){emitEdgeSkirt(p, ver3, ver1, position, skirtDist);}
        if(isEmpty(ver3, ver4, ver5) || !isFrontFacing(position, ver3, ver4, ver5)){emitEdgeSkirt(p, ver5, ver3, position, skirtDist);}
        if(isEmpty(ver5, ver6, ver1) || !isFrontFacing(position, ver5, ver6, ver1)){emitEdgeSkirt(p, ver1, ver5, position, skirtDist);}
        emitExtrudedTriangleClampToView(p, ver5, ver3, ver1, position, skirtDist);

        vec3 d1 = normalize(ver1.xyz-position.xyz);
        vec3 d3 = normalize(ver3.xyz-position.xyz);
        vec3 d5 = normalize(ver5.xyz-position.xyz);

        emitTriangleProjectedToNearPlane(p, ver1 , ver3 , ver5, 
                                            d1   , d3   , d5  , u_zNear);
    }
}

#endif
#ifdef _FRAGMENT_

void main() {
    if(gl_FrontFacing){
        gl_FragData[0].r = 1;
    }
    else{
        gl_FragData[0].r = -1;
    }
}

#endif