#ifdef _VERTEX_
void main()
{
	gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}

#else

void main()
{
	//gl_FragColor = vec4(1.0,1.0,1.0,1.0);
}
#endif