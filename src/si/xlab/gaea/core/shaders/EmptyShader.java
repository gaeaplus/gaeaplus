package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Vec4;
import java.net.URL;

/**
 *
 * @author vito
 */
public class EmptyShader implements Shader{

	private URL fileUrl;

	public EmptyShader(URL url)
	{
		this.fileUrl = url;
	}

	@Override
	public boolean isValid(){
		return false;
	}

	@Override
	public URL getURL()
	{
		return this.fileUrl;
	}

	@Override
	public String getRuntimeCode()
	{
		return "";
	}

	@Override
	public void setParam(String param, float[] floats)
	{
	}

	@Override
	public void setParam(String param, int[] ints)
	{
	}

	@Override
	public void setParam(String param, int sampler)
	{
	}

	@Override
	public void setParam(String param, Vec4... vec)
	{
	}

	@Override
	public void setParam(String param, Matrix... m)
	{
	}

	@Override
	public int getProgram(){
		return 0;
	}

	@Override
	public int getVS()
	{
		return 0;
	}

	@Override
	public int getFS()
	{
		return 0;
	}

	@Override
	public int getGS()
	{
		return 0;
	}

	@Override
	public void setValid(boolean isValid) {
	}

	@Override
	public void setActive(boolean isActive) {
	}

    @Override
    public void flush() {
    }
}
