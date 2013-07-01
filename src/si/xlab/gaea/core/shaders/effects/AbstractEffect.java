package si.xlab.gaea.core.shaders.effects;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.FrameBuffer;
import gov.nasa.worldwind.util.Logging;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public abstract class AbstractEffect {
	
	protected final FrameBuffer framebuffer;
	
	protected int textureIn = 0;
	protected int width;
	protected int height;
	protected int internalFormat;
	protected int format;
	protected int type;
	
	private boolean recreateInternalTextures = true;
	
	protected static final Logger logger = Logging.logger(AbstractEffect.class.getName());

	public AbstractEffect(FrameBuffer frambufer)
	{
		this.framebuffer = frambufer;
	}

	protected abstract int doRenderEffect(DrawContext dc);
	protected abstract void doGenerateInternalTextures(GL gl);

	public void setTexture(int texture, int internalFormat, int format, int type, int width, int height){

		if(internalFormat != this.internalFormat
				|| format != this.format
				|| type != this.type
				|| width != this.width 
				|| height != this.height){
			this.recreateInternalTextures = true;	
		}
		
		this.textureIn = texture;
		this.internalFormat = internalFormat;
		this.format = format;
		this.type = type;
		this.width = width;
		this.height = height;
	}

	public int renderEffect(DrawContext dc){
//		if(framebuffer == null){
//			logger.severe("renderEffect(): Framebuffer is NULL!");
//			throw new IllegalStateException("bloom(): Framebuffer is NULL!");	
//		}
		
		if(textureIn == 0){
			logger.severe("renderEffect(): Input texture is null! - call setTexture()");
			throw new IllegalStateException("bloom(): Input texture is null! - call setTexture()");
		}
		
		boolean fboBind = false;
		if(dc.getFramebufferController().getCurrent() != framebuffer){
			dc.getFramebufferController().push();
			framebuffer.bind(dc);
			fboBind = true;
		}

		GL2 gl = dc.getGL().getGL2();
		if(this.recreateInternalTextures){
			generateInternalTextures(gl);
		}

        gl.glPushAttrib(GL2.GL_ENABLE_BIT);
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glDisable(GL2.GL_BLEND);
		int out = doRenderEffect(dc);
        gl.glPopAttrib();
		
		if(fboBind){
			dc.getFramebufferController().pop();
		}

		return out;
	}

	private void generateInternalTextures(GL gl){
		doGenerateInternalTextures(gl);
		this.recreateInternalTextures = false;	
	}

	protected void drawQuadTex(GL2 gl)
	{
		gl.glBegin(GL.GL_TRIANGLE_STRIP);
		gl.glTexCoord2f(+0.0f, +0.0f);
		gl.glVertex2f(-1.0f, -1.0f);
		gl.glTexCoord2f(+1.0f, +0.0f);
		gl.glVertex2f(+1.0f, -1.0f);
		gl.glTexCoord2f(+0.0f, +1.0f);
		gl.glVertex2f(-1.0f, +1.0f);
		gl.glTexCoord2f(+1.0f, +1.0f);
		gl.glVertex2f(+1.0f, +1.0f);
		gl.glEnd();
	}
	
}
