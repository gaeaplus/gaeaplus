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
	public void setValid(boolean isValid);
	
	public void setActive(boolean isActive);
    
    public void flush();
	
	public int getProgram();
	public int getVS();
	public int getFS();
	public int getGS();

	public String getRuntimeCode();

	public void setParam(String param, float[] floats);
	public void setParam(String param, int[] ints);
	public void setParam(String param, int sampler);
	public void setParam(String param, Vec4... vec);
	public void setParam(String param, Matrix... m);
}
