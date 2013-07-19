package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.render.DrawContext;
import java.net.URL;
import javax.media.opengl.GL;

/**
 *
 * @author vito
 */
public interface ShaderContext extends Disposable{

	public void setGLSLCompiler(BasicGLSLCompiler glslCompiler);
	public BasicGLSLCompiler getGLSLCompiler();

	public void setShaderFactory(ShaderFactory shaderFactory);
	public ShaderFactory getShaderFactory();

	public void apply(DrawContext dc);
	public DrawContext getDC();

	public void pushShader();
	public void popShader();

	public boolean isShaderVersionSupported(float version);

	/**
	* Compile shader named filename or retrieve it from cache.
	* The shader file must be located on same path as current <code>ShaderFactory</code> class is.
	* If shader don't exist or some error occurred this method returns <code>EmptyShader</code> object.
	* Don't keep reference of returned shader object in local class!
	*
	* @param filename shader filename (example: "myshader.cg" or "myshader.glsl")
	* @param runtimeCode include extra cg/glsl code or use empty string
	*
	* @return a <code>Shader</code> object
	*/
	public Shader getShader(String filename, String runtimeCode);

	/**
	* Compile shader from URL or retrieve it from cache.
	* If shader don't exist or some error occurred this method returns <code>EmptyShader</code> object.
	* Don't keep reference of returned shader object in local class!
	*
	* @param fileUrl shader <code>URL</code>
	* @param runtimeCode include extra cg/glsl code or use empty string
	*
	* @return a <code>Shader</code> object
	*/
	public Shader getShader(URL fileUrl, String runtimeCode);

	/**
	* Returns shader that is currently enabled.
	*
	* @return a <code>Shader</code> object
	*/
	public Shader getCurrentShader();

	/**
	* User don't need to call this method.
	* This method is called on Shader.enable().
	*
	* @param shader <code>shader</code> that is currently enabled
	*/
	public void setCurrentShader(Shader shader);

	/**
	* Dispose all resources held by <code>Shader</code> object and remove it from <code>ShaderContext</code> cache.
	*
	* @param shaderFileUrl <code>URL</code> of <code>Shader</code> source file
	*/
	public void disposeShader(URL shaderFileUrl);

	public ShaderSupport getShaderSupport();
}
