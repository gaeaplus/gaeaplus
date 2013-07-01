package si.xlab.gaea.core.render;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

/**
 *
 * @author vito
 */
public abstract class AbstractRenderToTexture{

	private static Integer FBOdefault;
	public static final int TextureWDefault = 512;
	public static final int TextureHDefault = 1024;

	private int FBO = 0;
	private int RBO = 0;

	private int textureWidth;
	private int textureHeight;

	private int[] textureID;
	private Texture texture;
	private Texture dumyTexture;

	private int attachment = GL.GL_COLOR_ATTACHMENT0;
	private boolean depthTest = false;

	protected TextureData td;

	Sector sector;
	boolean renderLatLon = false;

	private boolean readyToRender = false;

	public AbstractRenderToTexture(){
		this.FBO = 0;
		this.textureWidth = TextureWDefault;
		this.textureHeight = TextureHDefault;
		this.textureID = new int[1];

		td = new TextureData(GLProfile.getDefault(), GL.GL_RGBA, textureWidth, textureHeight, 0,
				GL.GL_BGRA, GL.GL_UNSIGNED_BYTE,
				false, false, false, null, null);
	}

	public AbstractRenderToTexture(int fbo, int textureWidth, int textureHeight) {

		this.FBO = fbo;

		if(fbo != 0){
			this.textureWidth = textureWidth;
			this.textureHeight = textureHeight;
		}
		else{
			this.textureWidth = TextureWDefault;
			this.textureHeight = TextureHDefault;
		}
		this.textureID = new int[1];

		td = new TextureData(GLProfile.getDefault(), GL.GL_RGBA, textureWidth, textureHeight, 0,
				GL.GL_BGRA, GL.GL_UNSIGNED_BYTE,
				true, false, false, null, null);
	}

	public void renderLatLon(Sector sector, boolean enable){
		this.sector = sector;
		renderLatLon = enable;
	}

	public void setAttachment(int i){
		this.attachment = i;
	}

	public void enableDepthTest(boolean enable){
		this.depthTest = enable;
	}

	public Texture renderToTexture(DrawContext dc){

		if(!this.readyToRender)
			return null;

		GL gl = dc.getGL();

		this.texture = TextureIO.newTexture(td);
		this.texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		this.texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		this.texture.setTexParameterf(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		this.texture.setTexParameterf(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

		textureID[0] = texture.getTextureObject(gl);

		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, this.attachment,
					  texture.getTarget(), texture.getTextureObject(gl), 0);

		boolean frameBufferComplete = false;
		int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
		if(status == GL.GL_FRAMEBUFFER_COMPLETE){
			frameBufferComplete = true;
		}

		if(frameBufferComplete)
			render(dc);


		return texture;
	}

	public Texture renderToTexture(DrawContext dc, Texture texture){

		if(!this.readyToRender)
			return null;

		GL gl = dc.getGL();

		textureID[0] = texture.getTextureObject(gl);

		if(this.attachment == GL.GL_DEPTH_ATTACHMENT){
			if(dumyTexture == null){
				TextureData temp = new TextureData(GLProfile.getDefault(), GL.GL_RGBA, texture.getWidth(), texture.getHeight(), 0,
				GL.GL_BGRA, GL.GL_UNSIGNED_BYTE,
				false, false, false, null, null);

				this.dumyTexture = TextureIO.newTexture(temp);
				this.dumyTexture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
				this.dumyTexture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
				this.dumyTexture.setTexParameterf(gl, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
				this.dumyTexture.setTexParameterf(gl, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);

				gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
					  dumyTexture.getTarget(), dumyTexture.getTextureObject(gl), 0);
			}
		}

		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, this.attachment,
					  texture.getTarget(), texture.getTextureObject(gl), 0);

		boolean frameBufferComplete = false;
		int status = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
		if(status == GL.GL_FRAMEBUFFER_COMPLETE){
			frameBufferComplete = true;
		}

		if(frameBufferComplete)
			render(dc);

		return texture;
	}

	public void beginRendering(DrawContext dc){

		this.readyToRender = false;

		GL gl = dc.getGL();
		GL2 gl2 = dc.getGL().getGL2();

		if(FBO == 0){
			if(FBOdefault == null){
				FBOdefault = makeFBO(gl);
			}
			FBO = FBOdefault;
		}

		if(depthTest == true && RBO == 0){
			this.RBO = makeDepthRBO(gl);
		}

		gl2.glPushAttrib(GL2.GL_VIEWPORT_BIT | GL2.GL_SCISSOR_BIT);

		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPushMatrix();

		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPushMatrix();

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, FBO);

		gl2.glMatrixMode(GL2.GL_PROJECTION);

		gl2.glLoadIdentity();
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		gl2.glLoadIdentity();

		this.readyToRender = setProjection(dc);

		gl2.glMatrixMode(GL2.GL_MODELVIEW);
	}

	protected boolean setProjection(DrawContext dc){

		GL gl = dc.getGL();
		GL2 gl2 = dc.getGL().getGL2();

		gl.glEnable(GL.GL_SCISSOR_TEST);
        gl.glScissor(0, 0, textureWidth, textureHeight);
		gl.glViewport(0,0,textureWidth, textureHeight);

		gl2.glOrtho(-textureWidth/2, textureWidth/2,
				   -textureHeight/2, textureHeight/2,
				   -20000.0f, 20000.0f);

		return true;
	}

	public void endRendering(DrawContext dc){

		GL gl = dc.getGL();
		GL2 gl2 = dc.getGL().getGL2();

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);

		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPopMatrix();

		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPopMatrix();
		gl2.glPopAttrib();
	}

	public static int makeFBO(GL gl){

		int[] frameBuffer = new int[1];
//		int[] renderBuffer = new int[1];
		gl.glGenFramebuffers(1, frameBuffer, 0);

//		gl.glGenRenderbuffersEXT(1, renderBuffer, 0);
//		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, renderBuffer[0]);
//		gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, GL.GL_DEPTH_COMPONENT,
//					 textureWidth, textureHeight);
//		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, 0);

//		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, frameBuffer[0]);
//		gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT,
//						 GL.GL_RENDERBUFFER_EXT, renderBuffer[0]);
//		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
		return frameBuffer[0];
	}

	public int makeDepthRBO(GL gl){
		int[] renderBuffer = new int[1];

		gl.glGenRenderbuffers(1, renderBuffer, 0);
		gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, renderBuffer[0]);
		gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT,
					 textureWidth, textureHeight);
		gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, this.FBO);
		gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
						 GL.GL_RENDERBUFFER, renderBuffer[0]);
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		return renderBuffer[0];
	}

	private void render(DrawContext dc){

		GL gl = dc.getGL();
		GL2 gl2 = dc.getGL().getGL2();

		if(depthTest == true && RBO != 0){
			gl.glEnable(GL.GL_DEPTH_TEST);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		}
		else{
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		}
		gl2.glLoadIdentity();

		if(renderLatLon && (this.sector != null)){

			LatLon center = sector.getCentroid();
			gl2.glScaled((double)textureWidth/(sector.getDeltaLonDegrees()),
						(double)textureHeight/(sector.getDeltaLatDegrees()),
						1.0d);

			gl2.glTranslated(-(center.getLongitude().degrees),
							-(center.getLatitude().degrees),
							0.0d);
		}

		doRender(dc);
	}

	public abstract void doRender(DrawContext dc);
}