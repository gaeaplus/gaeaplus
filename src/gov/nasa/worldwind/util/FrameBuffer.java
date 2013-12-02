package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;
import java.util.HashSet;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public class FrameBuffer implements GLDisposable{

	private int id;
	
	private boolean hasRenderbufferAttached = false;

	private HashSet<Integer> usedTextureAttachements = new HashSet<Integer>();
    private HashSet<RenderBuffer> renderBuffers = new HashSet<RenderBuffer>();

	private static final Logger logger = Logging.logger("gov.nasa.worldwind.util.Framebuffer");

	/**
	 * 
	 * Get OpenGL ID of the <code>FrameBuffer<\code>.
	 * 
	 * @return OpenGL id of the <code>FrameBuffer<\code>.
	 */
	public int getID(){
		return this.id;
	}

	private int create(DrawContext dc){
		GL2 gl = dc.getGL().getGL2();

		//create fbo in graphics device
		int[] tmpId = new int[1];
		gl.glGenFramebuffers(1, tmpId, 0);
		this.hasRenderbufferAttached = false;
		return tmpId[0];
	}
	
	/**
	 * Bind frame-buffer to current GL canvas.
	 * 
	 * @param dc is <code>DrawContext</code> 
	 */
	public void bind(DrawContext dc){
		
		GL2 gl = dc.getGL().getGL2();

		if(!gl.glIsFramebuffer(id)){
			this.id = create(dc);
		}

		if(!dc.getFramebufferController().containsFrambuffer(this)){
			dc.getFramebufferController().addFramebuffer(this);
		}
		
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, id);
		dc.getFramebufferController().setCurrent(this);
	}

	/**
	 * 
	 * Indicate if <code>FrameBuffer<\code> has one or more textures attached.
	 * 
	 * @return true if it has texture attached.
	 */
	public boolean hasTextureAttached(){
		return !this.usedTextureAttachements.isEmpty();	
	}

	/**
	 * 
	 * Indicate if <code>FrameBuffer<\code> has one or more textures attached.
	 * 
	 * @return true if it has texture attached.
	 */
	public boolean hasTextureAttached(int attachement){
		return this.usedTextureAttachements.contains(attachement);	
	}
	
	/**
	 * 
	 * Indicate if <code>FrameBuffer<\code> has one or more renderbuffers attached.
	 * 
	 * @return true if it has texture attached.
	 */
    public boolean hasRenderBufferAttached(){
        return this.hasRenderbufferAttached;
    }

	/**
	 * 
	 * Attaches texture to frame-buffer.
	 * 
	 * @param texture
	 *			Specifies the texture object to attach to the framebuffer attachment point named by attachment.
	 * @param attachement
	 *			Specifies the attachment point of the framebuffer. attachment must be
	 *			GL_COLOR_ATTACHMENTi, GL_DEPTH_ATTACHMENT,
	 *			GL_STENCIL_ATTACHMENT or GL_DEPTH_STENCIL_ATTACHMENT.
	 * @param mipmapLevel
	 *			Specifies the mipmap level of texture to attach.
	 */
	public void attachTexture(DrawContext dc, int texture, int attachement, int mipmapLevel){

		if(dc.getFramebufferController().getCurrent() != this){
			logger.severe("attachTexture(): can't attach texture - Framebuffer not bound!");	
			throw new IllegalStateException("can't attach texture - Framebuffer not bound!");
		}

		dc.getGL().getGL3().glFramebufferTexture(GL2.GL_FRAMEBUFFER, attachement, texture, mipmapLevel);
		
		if(texture != 0){
			usedTextureAttachements.add(attachement);
		}
		else{
			usedTextureAttachements.remove(attachement);
		}
	}

	/**
	 * 
	 * Attaches texture to frame-buffer.
	 * 
	 * @param attachement
	 *				Specifies the attachment point of the framebuffer. Attachment must be
     *              GL_COLOR_ATTACHMENTi, GL_DEPTH_ATTACHMENT, GL_STENCIL_ATTACHMENT or GL_DEPTH_STENCIL_ATTACHMENT.
	 * @param texture
	 *				Specifies the texture object to attach to the framebuffer attachment point named by attachment.
	 * @param textureTarget
	 *				Specifies the texture target. Must be one of the following symbolic constants: GL_TEXTURE_2D,
	 *				GL_TEXTURE_CUBE_MAP_POSITIVE_X, GL_TEXTURE_CUBE_MAP_NEGATIVE_X, GL_TEXTURE_CUBE_MAP_POSITIVE_Y,
	 *				GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, GL_TEXTURE_CUBE_MAP_POSITIVE_Z, or GL_TEXTURE_CUBE_MAP_NEGATIVE_Z.
	 */
	public void attachTexture2D(DrawContext dc, int attachement, int texture, int textureTarget){
        
		if(dc.getFramebufferController().getCurrent() != this){
			logger.severe("attachTexture(): can't attach texture - Framebuffer not bound!");	
			throw new IllegalStateException("can't attach texture - Framebuffer not bound!");
		}

		dc.getGL().glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, attachement, textureTarget, texture, 0);
		
		if(texture != 0){
			usedTextureAttachements.add(attachement);
		}
		else{
			usedTextureAttachements.remove(attachement);
		}
	}

	/**
	 * 
	 * Attaches render-buffer to frame-buffer.
	 * 
	 * @param width is width in pixels
	 * @param height is height in pixels
	 * @param internalFormat
	 *			Specifies the color-renderable, depth-renderable, 
	 *			or stencil-renderable format of the render-buffer.
	 *			For example: GL_RGBA4, GL_RGB565, GL_RGB5_A1, GL_DEPTH_COMPONENT16, or GL_STENCIL_INDEX8.
	 * @param attachement
	 *			Specifies the attachment point to which render-buffer should be attached. 
	 *			Must be one of the following symbolic constants: GL_COLOR_ATTACHMENT0, GL_DEPTH_ATTACHMENT, or GL_STENCIL_ATTACHMENT.
	 * @param fsaa
	 *			Specifies full screen anti-aliasing factor.
	 */
    public void attachRenderbuffer(DrawContext dc, int width, int height, int internalFormat, int attachement, int fsaa){
        GL2 gl = dc.getGL().getGL2();

        if(dc.getFramebufferController().getCurrent() != this){
            String str = "FrameBuffer.releaseTextures() - FrameBuffer not bound!";
            logger.severe(str);
            throw new IllegalStateException(str);
        }

        RenderBuffer renderBuffer = new RenderBuffer(gl, width, height, internalFormat, fsaa);
        renderBuffer.attach(gl, attachement);
        this.renderBuffers.add(renderBuffer);
		this.hasRenderbufferAttached = true;
    }

	public void releaseTextures(DrawContext dc){

        if(dc.getFramebufferController().getCurrent() != this){
            String str = "FrameBuffer.releaseTextures() - FrameBuffer not bound!";
            logger.severe(str);
            throw new IllegalStateException(str);
        }
		
		GL2 gl = dc.getGL().getGL2();
		
		for(int attachement : usedTextureAttachements){
			gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, attachement, GL.GL_TEXTURE_2D, 0, 0);
		}
		usedTextureAttachements.clear();
	}

	public void releaseRenderbuffers(DrawContext dc){

        if(dc.getFramebufferController().getCurrent() != this){
            String str = "FrameBuffer.releaseTextures() - FrameBuffer not active!";
            logger.severe(str);
            throw new IllegalStateException(str);
        }
        
        GL2 gl = dc.getGL().getGL2();
		if(hasRenderbufferAttached){
            for(RenderBuffer rb : this.renderBuffers){
                rb.detach(gl);
                rb.dispose(gl);
            }
			this.hasRenderbufferAttached = false;
		}
	}

	public void setDrawBuffers(DrawContext dc, int[] drawBuffers){
		GL2 gl = dc.getGL().getGL2();

        if(dc.getFramebufferController().getCurrent() != this){
            String str = "FrameBuffer.releaseTextures() - FrameBuffer not bound!";
            logger.severe(str);
            throw new IllegalStateException(str);
        }
        
		gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
	}

    public void setReadBuffers(DrawContext dc, int readBuffer) {
        GL2 gl = dc.getGL().getGL2();

        if (dc.getFramebufferController().getCurrent() != this) {
            String str = "FrameBuffer.releaseTextures() - FrameBuffer not bound!";
            logger.severe(str);
            throw new IllegalStateException(str);
        }

        gl.glReadBuffer(readBuffer);
    }

    public boolean isComplete(DrawContext dc, boolean throwException){
		GL2 gl = dc.getGL().getGL2();
		int enumType = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);

		if(enumType == GL2.GL_FRAMEBUFFER_COMPLETE){
			return true;
		}
		else{
			if(throwException){
				switch(enumType){
					case GL2.GL_FRAMEBUFFER_UNSUPPORTED:
						logger.severe("isComplete(): fbo unsuppored!");
						throw new IllegalStateException("GL_FRAMEBUFFER_UNSUPPORTED_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
						logger.severe("isComplete(): incorect attachement!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
						logger.severe("isComplete(): missing attachement!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
						logger.severe("isComplete(): all attached textures must have same dimensions!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
						logger.severe("isComplete(): invalid texture format!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_FORMATS_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
						logger.severe("isComplete(): All draw buffers must specify attachment points that have images attached!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
						logger.severe("isComplete(): If the read buffer is set, then it must specify "
								+ "an attachment point that has an image attached!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT ERROR!");
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_LAYER_COUNT_EXT:
						logger.severe("isComplete(): incomplete layer count!");
						throw new IllegalStateException("GL_FRAMEBUFFER_INCOMPLETE_LAYER_COUNT_EXT ERROR!");
					default:
						logger.severe("isComplete(): unregistered frambuffer error!");
						throw new IllegalStateException("FRAMEBUFFER ERROR!");	
				}
			}
			else{
				switch(enumType){
					case GL2.GL_FRAMEBUFFER_UNSUPPORTED:
						logger.severe("isComplete(): fbo unsuppored!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
						logger.severe("isComplete(): incorect attachement!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
						logger.severe("isComplete(): missing attachement!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
						logger.severe("isComplete(): all attached textures must have same dimensions!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
						logger.severe("isComplete(): invalid texture format!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
						logger.severe("isComplete(): All draw buffers must specify attachment points that have images attached!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
						logger.severe("isComplete(): If the read buffer is set, then it must specify "
								+ "an attachment point that has an image attached!");
						break;
					case GL2.GL_FRAMEBUFFER_INCOMPLETE_LAYER_COUNT_EXT:
						logger.severe("isComplete(): incomplete layer count!");
						break;
					default:
						logger.severe("isComplete(): unregistered frambuffer error!");
						break;
				}
				return false;
			}
		}
	}

	@Override
	public void dispose(GL2 gl)
	{
		if(gl.glIsFramebuffer(id)){
			int[] tmpID = new int[1];
			tmpID[0] = this.id;
			gl.glDeleteFramebuffers(1, tmpID, 0);
		}
		if(hasRenderbufferAttached){
            for(RenderBuffer rb : this.renderBuffers){
                rb.detach(gl);
                rb.dispose(gl);
            }
			this.hasRenderbufferAttached = false;
		}
	}

	@Override
	public int hashCode()
	{
		return id;
	}

	@Override
	public boolean equals(Object obj)
	{
		if(!(obj instanceof FrameBuffer)){
			return false;
		}

		if(this.id != ((FrameBuffer)obj).id){
			return false;
		}

		return true;
	}

    private class RenderBuffer{
        
        public final int idR;
        public final int internalFormat;
        public final int width;
        public final int height;
        
        public boolean isAttached = false;
        public int attachement;

        public RenderBuffer(GL2 gl, int width, int height, int internalFormat, int fsaa){

            int[] renderBuffer = new int[1];
            gl.glGenRenderbuffers(1, renderBuffer, 0);
            idR = renderBuffer[0];
            
            gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, idR);

            this.internalFormat = internalFormat;
            this.width = width;
            this.height = height;

            if(fsaa == 0){
                gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, internalFormat,
                                        width, height);
            }
            else{
                gl.glRenderbufferStorageMultisample(GL2.GL_RENDERBUFFER, fsaa, internalFormat,
                                        width, height);
            }
            gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, 0);
        }

        public void attach(GL2 gl, int attachement){

            if(isAttached && attachement == this.attachement){
                return;
            }
            
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, id);
            gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, attachement,
                                            GL2.GL_RENDERBUFFER, idR);
            this.attachement = attachement;
            this.isAttached = true;
        }

        public void detach(GL2 gl){

            if(!isAttached){
                return;
            }
            
            gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, id);
            gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, attachement,
                                            GL2.GL_RENDERBUFFER, 0);
            this.attachement = 0;
            this.isAttached = false;
        }

        public void dispose(GL2 gl){
            
            detach(gl);
            
            if(gl.glIsRenderbuffer(idR)){
				int[] tmpID = new int[1];
				tmpID[0] = idR;
				gl.glDeleteRenderbuffers(1, tmpID, 0);
			}
            this.isAttached = false;
        }
    }
}