package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

/**
 *
 * @author vito
 */
public class BasicGLSLShader implements Shader{

	private final URL fileUrl;
	private final String runtimeCode;
	
	private int programID;
	private int vertexID;
	private int geomID;
	private int fragmentID;

	private boolean isValid;
	private boolean isActive;

	protected static final Logger logger = Logger.getLogger(BasicGLSLShader.class.getName());

	private HashMap<String, UniformParameter> parameters = new HashMap<String, UniformParameter>();
	
	public BasicGLSLShader(URL fileUrl, String runtimeCode)
	{
		this.fileUrl = fileUrl;
		this.runtimeCode = runtimeCode;
		this.isValid = false;
	}

	@Override
	public boolean isValid(){
		return isValid;
	}
	
	@Override
	public void setValid(boolean isValid){
		this.isValid = isValid;
	}

	@Override
	public void setActive(boolean isActive){
		this.isActive = isActive;
	}

	@Override
	public URL getURL()
	{
		return this.fileUrl;
	}

	@Override
	public String getRuntimeCode()
	{
		return runtimeCode;
	}

    @Override
	public void flush(){
		//update parameters
		for(UniformParameter parameter : this.parameters.values()){
			parameter.execute();
		}
	}

	@Override
	public void setParam(String paramName, float[] floats)
	{
		UniformParameter up = parameters.get(paramName);
		
		if(up == null){
			up = new UniformParameter(paramName, programID);
			parameters.put(paramName, up);
		}
		
		up.setValue(floats, 1);
		
		if(isActive && GLContext.getCurrent() != null){ up.execute(); } 
	}

	@Override
	public void setParam(String paramName, int[] ints)
	{
		UniformParameter up = parameters.get(paramName);
		
		if(up == null){
			up = new UniformParameter(paramName, programID);
			parameters.put(paramName, up);
		}
		
		up.setValue(ints, 1);
		
		if(isActive && GLContext.getCurrent() != null){ up.execute(); } 
	}

	@Override
	public void setParam(String paramName, int sampler)
	{
		UniformParameter up = parameters.get(paramName);
		
		if(up == null){
			up = new UniformParameter(paramName, programID);
			parameters.put(paramName, up);
		}
		
		up.setValue(new int[]{sampler}, 1);
		
		if(isActive && GLContext.getCurrent() != null){ up.execute(); } 
	}

	@Override
	public void setParam(String paramName, Vec4... vec)
	{
		UniformParameter up = parameters.get(paramName);
		
		if(up == null){
			up = new UniformParameter(paramName, programID);
			parameters.put(paramName, up);
		}
		
		up.setValue(vec);
		
		if(isActive && GLContext.getCurrent() != null){ up.execute(); } 
	}

	@Override
	public void setParam(String paramName, Matrix... m)
	{
		UniformParameter up = parameters.get(paramName);
		
		if(up == null){
			up = new UniformParameter(paramName, programID);
			parameters.put(paramName, up);
		}
		
		up.setValue(m);
		
		if(isActive && GLContext.getCurrent() != null){ up.execute(); } 
	}


	@Override
	public int getProgram(){
		return programID;
	}

	@Override
	public int getVS()
	{
		return vertexID;
	}

	@Override
	public int getFS()
	{
		return fragmentID;
	}

	@Override
	public int getGS()
	{
		return geomID;
	}

	public void setProgram(int id){
		this.programID = id;
	}

	public void setVS(int id){
		this.vertexID = id;
	}

	public void setGS(int id){
		this.geomID = id;
	}

	public void setFS(int id){
		this.fragmentID = id;
	}

	private static class UniformParameter{

		private final String name;
		private final int program;

		private Integer id = null;

		private int size;

		private int[] ints;
		private float[] floats;
		private Matrix[] matrices;
		private Vec4[] vectors;

		private boolean update = false;
		
		public UniformParameter(String name, int programID) {
			this.name = name;
			this.program = programID;
		}

		public void setValue(int[] ints, int size){
			if(Arrays.equals(this.ints, ints)){
				return;
			}	
			this.ints = ints;	
			this.size = size;
			this.update = true;
		}

		public void setValue(float[] floats, int size){
			if(Arrays.equals(this.floats, floats)){
				return;
			}	
			this.floats = floats;	
			this.size = size;
			this.update = true;
		}

		public void setValue(Vec4[] v){
			if(Arrays.equals(this.vectors, v)){
				return;
			}	
			this.vectors = v;	
			this.size = v.length;
			this.update = true;
		}

		public void setValue(Matrix[] m){
			if(Arrays.equals(this.matrices, m)){
				return;
			}	
			this.matrices = m;	
			this.size = m.length;
			this.update = true;
		}

		public void execute() {
			GL2 gl = GLContext.getCurrentGL().getGL2();
			
			if(id == null) { id = gl.glGetUniformLocation(program, name); }
			if(id == -1) { return; }
			
			if(update) { update = false; }
			else { return; }
			
			if(ints != null){
				switch(ints.length / size){
					case 1:
						gl.glUniform1iv(id, size, ints, 0);
						break;
					case 2:
						gl.glUniform2iv(id, size, ints, 0);
						break;
					case 3:
						gl.glUniform3iv(id, size, ints, 0);
						break;
					case 4:
						gl.glUniform4iv(id, size, ints, 0);
						break;
				}
			}
			else if(floats != null){
				switch(floats.length / size){
					case 1:
						gl.glUniform1fv(id, size, floats, 0);
						break;
					case 2:
						gl.glUniform2fv(id, size, floats, 0);
						break;
					case 3:
						gl.glUniform3fv(id, size, floats, 0);
						break;
					case 4:
						gl.glUniform4fv(id, size, floats, 0);
						break;
				}	
			}
			else if(vectors != null){
				gl.glUniform4fv(id, size, vectorToArray(vectors), 0);	
			}
			else if(matrices != null){
				gl.glUniformMatrix4fv(id, size, false,  matrixToArray(matrices), 0);
			}
		}

		private float[] vectorToArray(Vec4... vectors){
			double[] temp = new double[4];
			
			int counter = 0;
			float[] vf = new float[vectors.length * 4];
			for(Vec4 v : vectors){
				temp = v.toArray4(temp, 0);
				
				for(int i = 0; i<temp.length; i++){
					vf[counter] = (float)temp[i];
					counter++;
				}
			}
			return vf;
		}

		private float[] matrixToArray(Matrix... matrices){
			
			double[] temp = new double[16];
			
			int counter = 0;
			float[] mvF = new float[matrices.length * 16];
			for(Matrix m : matrices){
				temp = m.toArray(temp, 0, false);
				
				for(int i = 0; i<temp.length; i++){
					mvF[counter] = (float)temp[i];
					counter++;
				}
			}
			return mvF;
		}

		@Override
		public int hashCode() {
			int hash =1;
			hash = hash * 17 + program;
			hash = hash * 353 + name.hashCode();
			return hash;
		}

		@Override
		public boolean equals(Object obj) {

			if(obj instanceof UniformParameter){
				return (name.equals(((UniformParameter)obj).name) && program == ((UniformParameter)obj).program);
			}
			else{
				return false;
			}
		}
	}
}
