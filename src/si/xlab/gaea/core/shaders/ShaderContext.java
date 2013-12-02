package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.render.DrawContext;
import java.net.URL;

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

	public void enable(Shader shader);
	public void disableShaders();

	public void pushShader();
	public void popShader();

	public boolean isShaderVersionSupported(float version);

	/**
	* Compile shader named filename or retrieve it from cache.
	* The shader file must be located on same path as current <code>ShaderFactory</code> class is.
	* If shader don't exist or some error occurred this method returns <code>EmptyShader</code> object.
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
	*
	* @param fileUrl shader <code>URL</code>
	* @param runtimeCode include extra cg/glsl code or use empty string
	*
	* @return a <code>Shader</code> object
	*/
	public Shader getShader(URL fileUrl, String runtimeCode);

	/**
	* Returns shader that is currently active.
	*
	* @return a <code>Shader</code> object
	*/
	public Shader getCurrentShader();

	/**
	* Dispose all resources held by <code>Shader</code> object and remove it from <code>ShaderContext</code> cache.
	*
	* @param shaderFileUrl <code>URL</code> of <code>Shader</code> source file
	*/
	public void dispose(Shader shader);

	public ShaderSupport getShaderSupport();
}
