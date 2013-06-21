package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.render.DrawContext;
import java.net.URL;

/**
 *
 * @author vito
 */
public interface ShaderFactory {

	public Shader buildGLSLShader(ShaderContext sc, String runtimeCode, URL fileUrl);
	public Shader buildGLSLShader(ShaderContext sc, String runtimeCode, String filename);

	public Shader getColorLightingShader(DrawContext dc);
	public Shader getTexLightingShader(DrawContext dc);
}
