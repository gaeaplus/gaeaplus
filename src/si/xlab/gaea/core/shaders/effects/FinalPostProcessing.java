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
public class FinalPostProcessing extends AbstractEffect{

	
	private float intensityAve;
	private float intensityDev;

	private int textureDepth;
	private int textureBloom;
	private int textureInDistanceBlure;
	
	private BloomEffect bloomEffect;
	private BlureEffect blureDistanceEffect;

	private boolean renderBloom = false;
	private boolean renderBlureDistance = false;
	
	public FinalPostProcessing(FrameBuffer framebuffer)
	{
		super(framebuffer);
		this.bloomEffect = new BloomEffect(this.framebuffer);
		this.blureDistanceEffect = new BlureEffect(this.framebuffer);
	}

	public void setIntensity(float[] intensity){
		this.intensityAve = intensity[0];
		this.intensityDev = intensity[1];
	}

	public void setBloom(boolean enable){
		this.renderBloom = enable;	
	}

	public void setBlureDistance(boolean enable){
		this.renderBlureDistance = enable;	
	}

	public void setDepthTexture(int textureDepth){
		this.textureDepth = textureDepth;
	}

	public int doRenderEffect(DrawContext dc)
	{
		if(renderBloom){
			bloomEffect.setTexture(textureIn, internalFormat, format, type, width, height);
			bloomEffect.setTextureAverageIntensity(intensityAve);
			bloomEffect.setTextureAverageIntensityDiv(intensityDev);
			textureBloom = bloomEffect.renderEffect(dc);
		}

		if(renderBlureDistance){
			blureDistanceEffect.setTexture(textureIn, internalFormat, format, type, width, height);	
			blureDistanceEffect.setTextureMask(this.textureDepth);
			blureDistanceEffect.setTextureMaskPower(1800.0f);
			blureDistanceEffect.setNumOfPasses(1);
			blureDistanceEffect.setBlureRadius(2.0f);
			textureInDistanceBlure = blureDistanceEffect.renderEffect(dc);
		}

		return 0;
	}

	public void renderTonemap(DrawContext dc){

		GL2 gl = dc.getGL().getGL2();

        gl.glPushAttrib(GL2.GL_ENABLE_BIT);
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glDisable(GL.GL_BLEND);

		int textureColor = textureIn;

		if(renderBlureDistance){
			textureColor = textureInDistanceBlure;
		}

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureColor);

		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glBindTexture(GL.GL_TEXTURE_2D, textureDepth);
		
		if(renderBloom){
			
			gl.glActiveTexture(GL.GL_TEXTURE2);
			gl.glBindTexture(GL.GL_TEXTURE_2D, textureBloom);
			
			Shader shader = dc.getShaderContext().getShader("HDRBloom.glsl", "#version 120\n");
			shader.enable(dc.getShaderContext());
			shader.setParam("intensityAve", new float[]{intensityAve});
			shader.setParam("colorTex", 0);
			shader.setParam("depthTex", 1);
			shader.setParam("bloomTex", 2);

			if(!shader.isValid()){
				logger.severe("render(): HDRBloom.glsl shader is invalid!");
				throw new IllegalStateException("FinalPostProcessing.render(): HDRBloom.glsl shader is invalid!");
			}

			drawQuadTex(gl);
			shader.disable(dc.getShaderContext());

		}
		else{

			Shader shader = dc.getShaderContext().getShader("HDR.glsl", "#version 120\n");
			shader.enable(dc.getShaderContext());
			shader.setParam("intensityAve", new float[]{intensityAve});
			shader.setParam("colorTex", 0);
			shader.setParam("depthTex", 1);

			if(!shader.isValid()){
				logger.severe("render(): HDR.glsl shader is invalid!");
				throw new IllegalStateException("FinalPostProcessing.render(): HDR.glsl shader is invalid!");
			}

			drawQuadTex(gl);
			shader.disable(dc.getShaderContext());
			
		}
		gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glPopAttrib();
	}

	protected void doGenerateInternalTextures(GL gl)
	{
	}
}
