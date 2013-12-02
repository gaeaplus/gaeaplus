package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import java.net.URL;

/**
 *
 * @author vito
 */
public abstract class ShaderWrapper implements Shader{

	private final Shader shader;

	public ShaderWrapper(Shader shader)
	{
		this.shader = shader;
	}

	@Override
	public URL getURL(){return shader.getURL();}
	@Override
	public boolean isValid(){return shader.isValid();}
    @Override
    public void setValid(boolean isValid){shader.setValid(isValid);}
    @Override
	public void setActive(boolean isActive){shader.setActive(isActive);}
    @Override
    public void flush(){shader.flush();}
	@Override
	public int getVS(){return shader.getVS();}
	@Override
	public int getFS(){return shader.getFS();}
	@Override
	public int getGS(){return shader.getGS();}
	@Override
	public String getRuntimeCode(){ return shader.getRuntimeCode();}

	@Override
	public void setParam(String param, float[] floats){shader.setParam(param, floats);}
	@Override
	public void setParam(String param, int[] ints){shader.setParam(param, ints);}
	@Override
	public void setParam(String param, int sampler){shader.setParam(param, sampler);}
	@Override
	public void setParam(String param, Vec4... vec){shader.setParam(param, vec);}
	@Override
	public void setParam(String param, Matrix... m){shader.setParam(param, m);}

	public abstract void pushGLState(DrawContext dc);
	public abstract void popGLState(DrawContext dc);
}
