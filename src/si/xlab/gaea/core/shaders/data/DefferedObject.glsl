uniform mat4 eyeToWorld;
uniform float useTexture;

uniform sampler2D colorSampler;
uniform sampler2D bumpSampler;
uniform sampler2D envSampler;

#ifdef v150
	#ifdef _VERTEX_
		in vec4 in_vertex;

		out vec4 v_diffuseF;
		out vec4 v_diffuseB;
		out vec4 v_material;
		out vec2 v_texcoord0;		
		out vec2 v_texcoord1;		

		void main()
		{
			gl_Position = gl_ModelViewMatrix * in_vertex;

			v_texcoord0 = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
			v_texcoord1 = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
			
			v_diffuseF = gl_FrontMaterial.diffuse;
			v_diffuseB = gl_BackMaterial.diffuse;

			//materials
			vec4 s = gl_FrontMaterial.specular;
			float si = (s.x + s.y + s.z)/3.0;
			v_material.x = si;
			v_material.y = gl_FrontMaterial.shininess;
			vec4 e = gl_FrontMaterial.emission;
			v_material.z = (e.x+e.y+e.z)/3.0;
		}
	#endif

	#ifdef _GEOMETRY_

		in vec4 v_diffuseF[3];
		in vec4 v_diffuseB[3];
		in vec4 v_material[3];
		in vec2 v_texcoord0[3];		
		in vec2 v_texcoord1[3];		

		out vec4 g_diffuseF;
		out vec4 g_diffuseB;
		out vec4 g_material;
		out vec2 g_texcoord0;		
		out vec2 g_texcoord1;		
		out vec3 g_normal;

		layout(triangles) in;
		layout(triangle_strip, max_vertices = 3) out;

		void main() {

			mat4 p = gl_ProjectionMatrix;
			mat4 pInv = inverse(p);

			vec4 ver1 = gl_in[0].gl_Position;
			vec4 ver2 = gl_in[1].gl_Position;
			vec4 ver3 = gl_in[2].gl_Position;

			vec4 a = ver2 - ver1;
			vec4 b = ver3 - ver1;

			vec3 normal = normalize(cross(a.xyz, b.xyz));
			normal = faceforward(normal, vec3(0.0,0.0,-1.0), normal);
			g_normal = (eyeToWorld * vec4(normal, 0.0)).xyz;

			gl_Position = p*ver1;
			g_diffuseF = v_diffuseF[0];
			g_diffuseB = v_diffuseB[0];
			g_material = v_material[0];
			g_texcoord0 = v_texcoord0[0];
			g_texcoord1 = v_texcoord1[0];
			EmitVertex();

			gl_Position = p*ver2;
			g_diffuseF = v_diffuseF[1];
			g_diffuseB = v_diffuseB[1];
			g_material = v_material[1];
			g_texcoord0 = v_texcoord0[1];
			g_texcoord1 = v_texcoord1[1];
			EmitVertex();

			gl_Position = p*ver3;
			g_diffuseF = v_diffuseF[2];
			g_diffuseB = v_diffuseB[2];
			g_material = v_material[2];
			g_texcoord0 = v_texcoord0[2];
			g_texcoord1 = v_texcoord1[2];
			EmitVertex();

			EndPrimitive();
		}
	#endif

	#ifdef _FRAGMENT_

		in vec4 g_diffuseF;
		in vec4 g_diffuseB;
		in vec4 g_material;
		in vec2 g_texcoord0;		
		in vec2 g_texcoord1;		
		in vec3 g_normal;

		out vec4 f_color;
		out vec4 f_normal;
		out vec4 f_material;

		void main()
		{
			vec4 color = gl_FrontFacing ? g_diffuseF : g_diffuseB;
			if(useTexture > 0.5){
				vec4 f_color_tex = texture2D(colorSampler, g_texcoord0);
				color = f_color_tex.a * f_color_tex + (1.0-f_color_tex.a) * color;
			}

			vec3 normal = g_normal;

			#ifdef _BUMPMAP_
				vec3 bumpNormal = normalize(2.0 * (texture2D(bumpSampler, g_texcoord1) - 0.5)).xyz;
				vec3 b = normalize(cross(g_normal.xyz, g_texcoord1.xyz));
				vec3 n = normalize(g_normal.xyz);
				vec3 t = normalize(g_texcoord1.xyz);
				mat3 tangentTrans = mat3(t.x, b.x, n.x,
										 t.y, b.y, n.y,
										 t.z, b.z, n.z);
				normal = transpose(tangentTrans) * bumpNormal;
			#endif

			f_color = color;
			f_normal = vec4((normalize(normal) + 1.0)/2.0, 1.0);
			f_material[2] = vec4(g_material.xyz, 0.0);
		}	
	#endif

#else
	varying vec4 v_diffuseF;
	varying vec4 v_diffuseB;

	varying vec4 v_material;

	varying vec3 v_normal;
	varying vec2 v_texcoord;

	#ifdef _BUMPMAP_
		varying vec4 v_tangent;
	#endif

	#ifdef _VERTEX_

		void main()
		{
			gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
			v_normal = (eyeToWorld * (gl_ModelViewMatrix * vec4(gl_Normal, 0.0))).xyz;
			v_texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
			
			#ifdef _BUMPMAP_
				v_tangent = gl_MultiTexCoord1;
			#endif
			
			v_diffuseF = gl_FrontMaterial.diffuse;
			v_diffuseB = gl_BackMaterial.diffuse;

			vec4 s = gl_FrontMaterial.specular;
			float si = (s.x + s.y + s.z)/3.0;
			v_material.x = si;

			v_material.y = gl_FrontMaterial.shininess;
			
			vec4 e = gl_FrontMaterial.emission;
			v_material.z = (e.x+e.y+e.z)/3.0;
		}

	#else
		void main()
		{
			vec4 f_color = gl_FrontFacing ? v_diffuseF : v_diffuseB;
			if(useTexture > 0.5){
				vec4 f_color_tex = texture2D(colorSampler, v_texcoord);
				f_color = f_color_tex.a * f_color_tex + (1.0-f_color_tex.a) * f_color;
			}

			vec3 normal = v_normal;

			#ifdef _BUMPMAP_
				vec3 bumpNormal = normalize(2.0 * (texture2D(bumpSampler, v_texcoord) - 0.5)).xyz;
				vec3 b = normalize(cross(v_normal.xyz, v_tangent.xyz));
				vec3 n = normalize(v_normal.xyz);
				vec3 t = normalize(v_tangent.xyz);
				mat3 tangentTrans = mat3(t.x, b.x, n.x,
										 t.y, b.y, n.y,
										 t.z, b.z, n.z);
				normal = transpose(tangentTrans) * bumpNormal;
			#endif

			gl_FragData[0] = f_color;
			gl_FragData[1] = vec4((normalize(normal) + 1.0)/2.0, 1.0);
			gl_FragData[2] = vec4(v_material.xyz, 0.0);
		}
	#endif

#endif