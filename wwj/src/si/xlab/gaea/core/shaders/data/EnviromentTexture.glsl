const float M_PI = 3.141592657;

uniform samplerCube cubeTex;
varying vec2 coords;

#ifdef _VERTEX_

void main() {
    coords.x = gl_Vertex.x * M_PI;
    coords.y = gl_Vertex.y * M_PI/2.0;
    gl_Position = gl_Vertex;
}
#else

void main(){

	vec4 color = textureCube(cubeTex, vec3(cos(coords.y)*sin(coords.x), 
									  cos(coords.y)*cos(coords.x),
									  sin(coords.y)));
	gl_FragColor = vec4(color.rgb, 1.0);
	//gl_FragColor = vec4(1.0,0.0,0.0, 1.0);

}
#endif