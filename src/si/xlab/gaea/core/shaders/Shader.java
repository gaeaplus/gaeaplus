package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import java.net.URL;

/**
 *
 * @author vito
 */
public interface Shader{

	public URL getURL();
	public boolean isValid();

	public int getProgram();
	public int getVS();
	public int getFS();
	public int getGS();

	public String getRuntimeCode();

	public void enable(ShaderContext context);
	public void disable(ShaderContext context);
	public void dispose(ShaderContext context);

	public void setParam(String param, float[] floats);
	public void setParam(String param, int[] floats);
	public void setParam(String param, Vec4 vec);
	public void setParam(String param, Matrix m);
	public void setParam(String param, int textureUnit);
}
