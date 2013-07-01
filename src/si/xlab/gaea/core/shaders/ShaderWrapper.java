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
	public int getVS(){return shader.getVS();}
	@Override
	public int getFS(){return shader.getFS();}
	@Override
	public int getGS(){return shader.getGS();}
	@Override
	public String getRuntimeCode(){ return shader.getRuntimeCode();}

	@Override
	public void enable(ShaderContext context)
	{
		shader.enable(context);
		pushGLState(context.getDC());
	}

	@Override
	public void disable(ShaderContext context)
	{
		popGLState(context.getDC());
		shader.disable(context);
	}

	@Override
	public void dispose(ShaderContext context){shader.dispose(context);}
	@Override
	public void setParam(String param, float[] floats){shader.setParam(param, floats);}
	@Override
	public void setParam(String param, Vec4 vec){shader.setParam(param, vec);}
	@Override
	public void setParam(String param, Matrix m){shader.setParam(param, m);}
	@Override
	public void setParam(String param, int textureUnit){shader.setParam(param, textureUnit);}

	public abstract void pushGLState(DrawContext dc);
	public abstract void popGLState(DrawContext dc);
}
