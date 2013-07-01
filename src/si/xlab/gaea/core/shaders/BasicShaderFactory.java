package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.render.DrawContext;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GLContext;

/**
 *
 * @author vito
 */
public class BasicShaderFactory implements ShaderFactory{

	protected static final Logger logger = Logger.getLogger(BasicShaderFactory.class.getName());

	@Override
	public Shader buildGLSLShader(ShaderContext sc, String runtimeCode, URL fileUrl)
	{
		if(GLContext.getCurrent() == null){
			String msg = "ShaderContext.getShader(): GLContext not current on this thread";
			logger.severe(msg);
		}

		BasicGLSLShader shader = new BasicGLSLShader(fileUrl, runtimeCode);
		if(!sc.getGLSLCompiler().loadProgram(shader)){
			logger.log(Level.SEVERE, "Incomplete BasicShader: {0}", fileUrl.getFile());
			return new EmptyShader(fileUrl);
		}

		logger.log(Level.INFO, "Shader loaded: {0}", fileUrl.getFile());
		shader.setValid(true);
		return shader;
	}

	@Override
	public Shader buildGLSLShader(ShaderContext sc, String runtimeCode, String filename)
	{
		URL fileUrl = this.getClass().getResource(filename);
		return buildGLSLShader(sc, runtimeCode, fileUrl);
	}

	@Override
	public Shader getColorLightingShader(DrawContext dc)
	{
		return new EmptyShader(null);
	}

	@Override
	public Shader getTexLightingShader(DrawContext dc)
	{
		return new EmptyShader(null);
	}
}
