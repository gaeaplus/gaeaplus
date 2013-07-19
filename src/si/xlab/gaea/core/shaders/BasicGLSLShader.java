package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.Disposable;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
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

	protected static final Logger logger = Logger.getLogger(BasicGLSLShader.class.getName());

	private ParameterCache paramCache = new ParameterCache();

	public BasicGLSLShader(URL fileUrl, String runtimeCode)
	{
		this.fileUrl = fileUrl;
		this.runtimeCode = runtimeCode;
	}

	@Override
	public boolean isValid(){
		return isValid;
	}
	
	public void setValid(boolean param){
		this.isValid = param;
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
	public void enable(ShaderContext context)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.enable(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		if(!isValid){
			String msg = "Trying to enable invalid shader: " + this.fileUrl.getFile();
			logger.severe(msg);	
			return;
		}

		if(context.getCurrentShader() == this){
			return;
		}

		if(context.getCurrentShader() != null){
			context.getCurrentShader().disable(context);
			context.setCurrentShader(null);
		}

		gl.glUseProgram(programID);
		context.setCurrentShader(this);
	}

	@Override
	public void disable(ShaderContext context)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.disable(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		context.setCurrentShader(null);
		gl.glUseProgram(0);
	}

	@Override
	public void dispose(ShaderContext context)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.dispose(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		isValid = false;
		paramCache.dispose();
		if(gl.glIsProgram(programID)){
			gl.glDeleteProgram(programID);
		}
	}

	@Override
	public void setParam(String paramName, float[] floats)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.setParam(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		if(floats == null){
			return;
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		int len = floats.length;
		int p = paramCache.getParameter(paramName);

		if(p == -1)
			return;

		switch(len){
			case 1:
				gl.glUniform1f(p, floats[0]);
				break;
			case 2:
				gl.glUniform2f(p, floats[0], floats[1]);
				break;
			case 3:
				gl.glUniform3f(p, floats[0], floats[1], floats[2]);
				break;
			case 4:
				gl.glUniform4f(p, floats[0], floats[1], floats[2], floats[3]);
				break;
		}

	}

	@Override
	public void setParam(String paramName, int[] ints)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.setParam(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		if(ints == null){
			return;
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		int len = ints.length;
		int p = paramCache.getParameter(paramName);

		if(p == -1)
			return;

		switch(len){
			case 1:
				gl.glUniform1i(p, ints[0]);
				break;
			case 2:
				gl.glUniform2i(p, ints[0], ints[1]);
				break;
			case 3:
				gl.glUniform3i(p, ints[0], ints[1], ints[2]);
				break;
			case 4:
				gl.glUniform4i(p, ints[0], ints[1], ints[2], ints[3]);
				break;
		}

	}

	@Override
	public void setParam(String paramName, Vec4 vec)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.setParam(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		if(vec == null){
			return;
		}

		int p = paramCache.getParameter(paramName);
		if(p == -1)
			return;

		GL2 gl = GLContext.getCurrent().getGL().getGL2();
		gl.glUniform4f(p, (float)vec.x, (float)vec.y, (float)vec.z, (float)vec.w);
	}

	@Override
	public void setParam(String paramName, Matrix m)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.setParam(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		if(m == null){
			return;
		}
		
		int p = paramCache.getParameter(paramName);
		if(p == -1)
			return;

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		gl.glUniformMatrix4fv(p, 1, false,  matrixToArray(m), 0);
	}

	@Override
	public void setParam(String paramName, int textureUnit)
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.setParam(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		int p = paramCache.getParameter(paramName);
		if(p == -1)
			return;

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		gl.glUniform1i(p, textureUnit);
	}

	private float[] matrixToArray(Matrix m){
		double[] mvD = new double[16];
		mvD = m.toArray(mvD, 0, false);
		float[] mvF = new float[16];
		for(int i = 0; i<mvD.length; i++){
			mvF[i] = (float)mvD[i];
		}
		return mvF;
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

	private class ParameterCache implements Disposable{

		private HashMap<String, Integer> parameters = new HashMap<String, Integer>();

		public int getParameter(String paramName){

			int parameter;

			if(GLContext.getCurrent() == null){
				String message = "BasicGLSLShader.ParameterCache.getParameter(): GLContext not current!!";
				logger.severe(message);
				throw new IllegalStateException();
			}

			GL2 gl = GLContext.getCurrent().getGL().getGL2();

			//parameters cache
			if(!parameters.containsKey(paramName)){
				//set parameter

				parameter = gl.glGetUniformLocation(programID, paramName);

				if(parameter == -1){
					String message = "In shader: " + getURL().getFile() + " ShaderParameter: " + paramName + " NOT FOUND or NOT USED!";
					logger.warning(message);
				}
				this.parameters.put(paramName, parameter);
			}
			else{
				parameter = parameters.get(paramName);
			}

			return parameter;
		}

		@Override
		public void dispose(){
			parameters.clear();
		}
	}
}
