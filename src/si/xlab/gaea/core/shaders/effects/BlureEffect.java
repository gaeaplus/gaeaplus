package si.xlab.gaea.core.shaders.effects;

import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.Shader;
import gov.nasa.worldwind.util.FrameBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public class BlureEffect extends AbstractEffect{

	private int textureBlure1;
	private int textureBlure0;
	
	private int numOfPasses = 1;
	private int textureMask = 0;
	private float textureMaskPower = 1;

	private float blureRadius = 3.5f;
	
	public BlureEffect(FrameBuffer framebuffer)
	{
		super(framebuffer);
	}

	public void setNumOfPasses(int count){
		this.numOfPasses = count;
	}

	public void setBlureRadius(float rPixels){
		this.blureRadius = rPixels;	
	}

	public void setTextureMask(int textureMask){
		this.textureMask = textureMask;
	}

	public void setTextureMaskPower(float textureMaskPower){
		this.textureMaskPower = textureMaskPower;
	}

	public int doRenderEffect(DrawContext dc){
		
		GL2 gl = dc.getGL().getGL2();
		float pixelHDist = ((float)(1.0d/this.height)) * 3;
		float pixelWDist = ((float)(1.0d/this.width)) * 3;

		framebuffer.attachTexture2D(dc, GL2.GL_COLOR_ATTACHMENT0, textureBlure0, GL.GL_TEXTURE_2D);
		framebuffer.attachTexture2D(dc, GL2.GL_COLOR_ATTACHMENT1, textureBlure1, GL.GL_TEXTURE_2D);
		framebuffer.isComplete(dc, false);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureBlure0);
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureBlure1);
		gl.glActiveTexture(GL.GL_TEXTURE2);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureIn);
		gl.glActiveTexture(GL.GL_TEXTURE3);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureMask);
		gl.glActiveTexture(GL.GL_TEXTURE0);

		float sampleDist = this.blureRadius;
		float maskWeight = textureMask == 0 ? 0 : 1;
		pixelHDist = ((float)(1.0d/this.height)) * sampleDist;
		pixelWDist = ((float)(1.0d/this.width)) * sampleDist;

		Shader shader;
		
		//blure vertical
		shader = dc.getShaderContext().getShader("Blure7V.glsl", "#version 120\n");
		shader.enable(dc.getShaderContext());

		shader.setParam("maskSampler", 3);
		shader.setParam("sampleDist", new float[]{pixelHDist});
		shader.setParam("maskWeight", new float[]{maskWeight});
		shader.setParam("maskPower", new float[]{textureMaskPower});
		
		int textureCurrent = 2;
		for(int i = 0; i<numOfPasses; i++){
			if(textureCurrent == 0){
				shader.setParam("RT", textureCurrent);
				framebuffer.setDrawBuffers(dc, new int[]{GL2.GL_COLOR_ATTACHMENT1});
				textureCurrent = 1;
			}
			else{
				shader.setParam("RT", textureCurrent);	
				framebuffer.setDrawBuffers(dc, new int[]{GL2.GL_COLOR_ATTACHMENT0});
				textureCurrent = 0;
			}
			drawQuadTex(gl);
		}
		shader.disable(dc.getShaderContext());

		///blure horizontal
		shader = dc.getShaderContext().getShader("Blure7H.glsl", "#version 120\n");
		shader.enable(dc.getShaderContext());

		shader.setParam("maskSampler", 3);
		shader.setParam("sampleDist", new float[]{pixelWDist});
		shader.setParam("maskWeight", new float[]{maskWeight});
		shader.setParam("maskPower", new float[]{textureMaskPower});
		
		for(int i = 0; i<numOfPasses; i++){
			if(textureCurrent == 0){
				shader.setParam("RT", textureCurrent);
				framebuffer.setDrawBuffers(dc, new int[]{GL2.GL_COLOR_ATTACHMENT1});
				textureCurrent = 1;
			}
			else{
				shader.setParam("RT", textureCurrent);	
				framebuffer.setDrawBuffers(dc, new int[]{GL2.GL_COLOR_ATTACHMENT0});
				textureCurrent = 0;
			}
			drawQuadTex(gl);
		}
		shader.disable(dc.getShaderContext());

		int outTexture;
		if(textureCurrent == 0){
			outTexture = textureBlure0;
		}
		else{
			outTexture = textureBlure1;
		}
		return outTexture;
	}

	@Override
	protected void doGenerateInternalTextures(GL gl){

		if(!gl.glIsTexture(textureBlure0)){
			int[] tmp = new int[1];
			gl.glGenTextures(1, tmp, 0);
			this.textureBlure0 = tmp[0];
		}

		if(!gl.glIsTexture(textureBlure1)){
			int[] tmp = new int[1];
			gl.glGenTextures(1, tmp, 0);
			this.textureBlure1 = tmp[0];
		}

		gl.glBindTexture(GL.GL_TEXTURE_2D, textureBlure0);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, this.internalFormat, 
				this.width, this.height, 0, this.format, this.type, null);	

		gl.glBindTexture(GL.GL_TEXTURE_2D, textureBlure1);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, this.internalFormat, 
				this.width, this.height, 0, this.format, this.type, null);
	}
	
}
