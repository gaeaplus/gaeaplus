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
public class BloomEffect extends AbstractEffect{

	private int textureBloom;
	
	private float averageIntensity = 0.5f;
	private float averageIntensityDiv = 0.2f;

	private BlureEffect blureEffect;
	
	public BloomEffect(FrameBuffer frambuffer)
	{
		super(frambuffer);
		this.blureEffect = new BlureEffect(framebuffer);
	}

	public void setTextureAverageIntensity(float averageIntensity){
		this.averageIntensity = averageIntensity;	
	}

	public void setTextureAverageIntensityDiv(float intensityDiviation){
		this.averageIntensityDiv = intensityDiviation;	
	}

	public int doRenderEffect(DrawContext dc){
		
		GL2 gl = dc.getGL().getGL2();

		//generate bloom mask
//		framebuffer.releaseTextures(dc);
		framebuffer.attachTexture2D(dc, GL2.GL_COLOR_ATTACHMENT0, textureBloom, GL.GL_TEXTURE_2D);
		framebuffer.setDrawBuffers(dc, new int[]{GL2.GL_COLOR_ATTACHMENT0});
		framebuffer.isComplete(dc, false);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureIn);

		Shader shader;
		shader = dc.getShaderContext().getShader("IntensityFilter.glsl", "#version 120\n");
		shader.enable(dc.getShaderContext());
		shader.setParam("colorTex", 0);
		shader.setParam("minA", new float[]{averageIntensity + 1.0f * averageIntensityDiv});
//		shader.setParam("minB", new float[]{averageIntensity + 1.5f * averageIntensityDiv + averageIntensityDiv * 0.05f});
		shader.setParam("minB", new float[]{averageIntensity + 2.0f * averageIntensityDiv});
		shader.setParam("maxA", new float[]{200.0f});
		shader.setParam("maxB", new float[]{300.0f});
		drawQuadTex(gl);
		shader.disable(dc.getShaderContext());

		//blure bloom mask
		blureEffect.setNumOfPasses(1);
		blureEffect.setBlureRadius(3.0f);
		blureEffect.setTexture(textureBloom, internalFormat, format, type, width, height);
		int out = blureEffect.renderEffect(dc);
		
		return out;
	}

	protected void doGenerateInternalTextures(GL gl){

		if(!gl.glIsTexture(textureBloom)){
			int[] tmp = new int[1];
			gl.glGenTextures(1, tmp, 0);
			this.textureBloom = tmp[0];
		}

		gl.glBindTexture(GL.GL_TEXTURE_2D, textureBloom);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, this.internalFormat, 
				this.width, this.height, 0, this.format, this.type, null);	
	}
}
