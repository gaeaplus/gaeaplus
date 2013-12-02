package si.xlab.gaea.core.shaders;

import java.util.HashMap;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.net.URL;
import java.util.Stack;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

/**
 *
 * @author vito
 */
public class ShaderContextImpl implements ShaderContext{

	private BasicGLSLCompiler glslCompiler = new BasicGLSLCompiler();

	private Shader currentShader = null;

	private final ShaderSupport support = new ShaderSupport();
	private ShaderFactory shaderFactory = new BasicShaderFactory();

	private DrawContext dc;

	private final HashMap<ShaderKey, Shader> shaders = new HashMap<ShaderKey, Shader>();
	private final Stack<Shader> shadersStack = new Stack<Shader>();
	
	private static final Logger logger = Logging.logger(ShaderContextImpl.class.getName());

	private static class ShaderKey{
		public URL url;
		public String runtimeCode;

		public ShaderKey(URL url, String runtimeCode)
		{
			this.url = url;
			this.runtimeCode = runtimeCode;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;

			final ShaderKey key = (ShaderKey) obj;

			if (!url.equals(key.url))
				return false;
			if (!runtimeCode.equals(key.runtimeCode))
				return false;

			return true;
		}

		@Override
		public int hashCode()
		{
			int hash = 7;
			hash = 83 * hash + (this.url != null ? this.url.hashCode() : 0);
			hash = 83 * hash + (this.runtimeCode != null ? this.runtimeCode.hashCode() : 0);
			return hash;
		}
	}

	@Override
	public void apply(DrawContext dc){
		this.dc = dc;
		GL gl = dc.getGL();
		glslCompiler.defineSupportedShaders(gl);
	}

	@Override
	public void enable(Shader shader){
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.enable(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		if(!shader.isValid()){
			logger.severe("Trying to enable invalid shader!");	
			return;
		}

		if(currentShader != shader){
            
            if(currentShader != null){
                currentShader.setActive(false);
            }
            
			gl.glUseProgram(shader.getProgram());
			currentShader = shader;
			currentShader.setActive(true);
            currentShader.flush();
		}
	}

	@Override
	public void disableShaders(){
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.disable(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();

		gl.glUseProgram(0);

		if(currentShader != null){
			currentShader.setActive(false);
			currentShader = null;
		}
	}

	@Override
	public void dispose(Shader shader){
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.dispose(): GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL2 gl = GLContext.getCurrent().getGL().getGL2();
		
		ShaderKey key = new ShaderKey(shader.getURL(), shader.getRuntimeCode());

		if(currentShader == shader){
			currentShader = null;
		}

		shader.setValid(false);
		shader.setActive(false);

		if(gl.glIsProgram(shader.getProgram())){
			gl.glDeleteProgram(shader.getProgram());
		}

		shaders.remove(key);
	}

	@Override
	public Shader getCurrentShader(){
		return this.currentShader;
	}

	@Override
	public void pushShader(){
		if(this.currentShader != null){
			this.shadersStack.push(this.currentShader);
		}
	}

	@Override
	public void popShader(){
		if(GLContext.getCurrent() == null){
			String message = "BasicShaderContext.popShader: GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();	
		}
		
		if(!shadersStack.isEmpty()){
			Shader shader = shadersStack.pop();
			enable(shader);
		}
		else{
			disableShaders();
		}
	}

	@Override
	public boolean isShaderVersionSupported(float version) {
		return this.glslCompiler.isShaderVersionSupported(version);
	}

	@Override
	public DrawContext getDC(){
		return this.dc;
	}

	@Override
	public BasicGLSLCompiler getGLSLCompiler()
	{
		return glslCompiler;
	}

	@Override
	public void setGLSLCompiler(BasicGLSLCompiler compiler)
	{
		this.glslCompiler = compiler;
	}

	@Override
	public void setShaderFactory(ShaderFactory shaderFactory){
		this.shaderFactory = shaderFactory;
	}

	@Override
	public ShaderFactory getShaderFactory(){
		return this.shaderFactory;
	}

	@Override
	public Shader getShader(String fileName, String runtimeCode){
		URL fileUrl = this.shaderFactory.getClass().getResource(fileName);
        return doGetShader(fileUrl, runtimeCode);
	}

	@Override
	public Shader getShader(URL fileUrl, String runtimeCode){
		return doGetShader(fileUrl, runtimeCode);
	}

	public Shader doGetShader(URL fileUrl, String runtimeCode){
		ShaderKey key = new ShaderKey(fileUrl, runtimeCode);
		Shader shader = this.shaders.get(key);

		if(shader == null){
			if(fileUrl.getFile().endsWith("glsl")){
				shader = shaderFactory.buildGLSLShader(this, runtimeCode, fileUrl);
			}
			this.shaders.put(key, shader);
		}
		return shader;
	}

	@Override
	public void dispose()
	{
		disableShaders();
		
		for(Shader shader : this.shaders.values()){
			dispose(shader);
		}

		this.shaders.clear();

		//TODO: debug error on cgContext destroy/create
//		cgCompiler.dispose();
//		glslCompiler.dispose();
	}

	@Override
	public ShaderSupport getShaderSupport()
	{
		return support;
	}
}
