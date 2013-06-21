#version 150 compatibility

#ifdef _VERTEX_

in vec4 in_vertex;
out vec2 position;

void main() {
    gl_Position = in_vertex;
    position = (in_vertex.xy + 1.0)/2.0;
}

#endif
#ifdef _FRAGMENT_

in vec2 position;

uniform vec4 u_color;
uniform isampler2D shadowTex;

void main() {
    ivec4 shadow = texture(shadowTex, position);

    if(shadow.r == 0){
        gl_FragData[0] = u_color;
    }
    else{
        discard;
    }
}
#endif