package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;
import java.util.HashSet;
import java.util.logging.Logger;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;

/**
 *
 * @author vito
 */
public class FrameBuffer implements GLDisposable{

	private int id;
	
	private boolean hasTexturesAttached = false;
	private boolean hasRenderbufferAttached = false;

    private HashSet<RenderBuffer> renderBuffers = new HashSet<RenderBuffer>();

	private static final Logger logger = Logging.logger("gov.nasa.worldwind.util.Framebuffer");

	public FrameBuffer()
	{
	}

	public int getID(){
		return this.id;
	}

	private int create(DrawContext dc){
		GL2 gl = dc.getGL().getGL2();

		//create fbo in graphics device
		int[] tmpId = new int[1];
		gl.glGenFramebuffers(1, tmpId, 0);
		this.hasTexturesAttached = false;
		this.hasRenderbufferAttached = false;
		return tmpId[0];
	}
	
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

	public boolean hasTexturesAttached(){
		return this.hasTexturesAttached;	
	}

    public boolean hasRenderBuffersAttached(){
        return this.hasRenderbufferAttached;
    }

	public void attachTexture(DrawContext dc, int texture, int target, int mipmapLevel){

		if(dc.getFramebufferController().getCurrent() != this){
			logger.severe("attachTexture(): can't attach texture - Framebuffer not bound!");	
			throw new IllegalStateException("can't attach texture - Framebuffer not bound!");
		}

		dc.getGL().getGL3().glFramebufferTexture(GL2.GL_FRAMEBUFFER, target, texture, mipmapLevel);
		
		if(texture != 0){
			this.hasTexturesAttached = true;
		}
	}

	public void attachTexture2D(DrawContext dc, int attachement, int texture, int textureTarget){
        
		if(dc.getFramebufferController().getCurrent() != this){
			logger.severe("attachTexture(): can't attach texture - Framebuffer not bound!");	
			throw new IllegalStateException("can't attach texture - Framebuffer not bound!");
		}

		dc.getGL().glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, attachement, textureTarget, texture, 0);
		
		if(texture != 0){
			this.hasTexturesAttached = true;
		}
	}

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

		GL3 gl = dc.getGL().getGL3(); 
		gl.glFramebufferTexture(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, 0, 0);
		gl.glFramebufferTexture(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT1, 0, 0);
		gl.glFramebufferTexture(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT2, 0, 0);
		gl.glFramebufferTexture(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT3, 0, 0);
		gl.glFramebufferTexture(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, 0, 0);
		this.hasTexturesAttached = false;
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
	
	public void setReadBuffers(DrawContext dc, int readBuffer){
		GL2 gl = dc.getGL().getGL2();

        if(dc.getFramebufferController().getCurrent() != this){
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