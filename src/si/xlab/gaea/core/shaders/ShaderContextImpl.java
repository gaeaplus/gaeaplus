package si.xlab.gaea.core.shaders;

import java.util.HashMap;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.net.URL;
import java.util.Stack;
import java.util.logging.Logger;
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

	private HashMap<ShaderKey, Shader> shaders = new HashMap<ShaderKey, Shader>();
	private Stack shadersStack = new Stack();
	
	private final Logger logger = Logging.logger(ShaderContextImpl.class.getName());

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

	public void apply(DrawContext dc){
		this.dc = dc;
	}

	public void setCurrentShader(Shader shader){
		this.currentShader = shader;
	}

	public Shader getCurrentShader(){
		return this.currentShader;
	}

	public void pushShader(){
		if(this.currentShader != null){
			this.shadersStack.push(this.currentShader);
		}
	}

	public void popShader(){
		if(GLContext.getCurrent() == null){
			String message = "BasicShaderContext.popShader: GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();	
		}
		
		if(!shadersStack.isEmpty()){
			Shader shader = (Shader)shadersStack.pop();
			shader.enable(this);
		}
		else{
			GL2 gl = GLContext.getCurrent().getGL().getGL2();
            setCurrentShader(null);
			gl.glUseProgram(0);
		}
	}

	public DrawContext getDC(){
		return this.dc;
	}

	public BasicGLSLCompiler getGLSLCompiler()
	{
		return glslCompiler;
	}

	public void setGLSLCompiler(BasicGLSLCompiler compiler)
	{
		this.glslCompiler = compiler;
	}

	public void setShaderFactory(ShaderFactory shaderFactory){
		this.shaderFactory = shaderFactory;
	}

	public ShaderFactory getShaderFactory(){
		return this.shaderFactory;
	}

	public Shader getShader(String fileName, String runtimeCode){
		return getShader(fileName, runtimeCode, false);
	}

	public Shader getShader(String fileName, String runtimeCode, boolean removeExisting){

		URL fileUrl = this.shaderFactory.getClass().getResource(fileName);

		ShaderKey key = new ShaderKey(fileUrl, runtimeCode);
		Shader shader = this.shaders.get(key);

		if(shader == null){

			if(removeExisting){
				Stack stack = new Stack();
				for(ShaderKey k : this.shaders.keySet()){
					if(k.url.equals(fileUrl)){
						shader.disable(this);
						stack.push(k);
						shader.dispose(this);
						shader = null;	
					}
				}
				for(Object o : stack){
					shaders.remove((ShaderKey)o);
				}
				stack.clear();
			}
			
			if(fileName.endsWith("glsl")){
				shader = shaderFactory.buildGLSLShader(this, runtimeCode, fileName);
			}
			this.shaders.put(key, shader);
		}
		return shader;
	}

	@Override
	public Shader getShader(URL fileUrl, String runtimeCode){
		return getShader(fileUrl, runtimeCode, false);
	}

	public Shader getShader(URL fileUrl, String runtimeCode, boolean removeExisting){
		ShaderKey key = new ShaderKey(fileUrl, runtimeCode);
		Shader shader = this.shaders.get(key);

		if(shader == null){

			if(removeExisting){
				Stack stack = new Stack();
				for(ShaderKey k : this.shaders.keySet()){
					if(k.url.equals(fileUrl)){
						shader.disable(this);
						stack.push(k);
						shader.dispose(this);
						shader = null;	
					}
				}
				for(Object o : stack){
					shaders.remove((ShaderKey)o);
				}
				stack.clear();
			}
			
			if(fileUrl.getQuery().endsWith("glsl")){
				shader = shaderFactory.buildGLSLShader(this, runtimeCode, fileUrl);
			}
			this.shaders.put(key, shader);
		}
		return shader;
	}

	@Override
	public void disposeShader(URL shaderFileUrl)
	{
		Shader shader;
		if(this.shaders.containsKey(shaderFileUrl)){
			shader = this.shaders.get(shaderFileUrl);
			this.shaders.remove(shaderFileUrl);
			shader.dispose(this);
		}
	}

	@Override
	public void dispose()
	{
		for(Shader shader : this.shaders.values()){
			shader.disable(this);
			shader.dispose(this);
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
