package gov.nasa.worldwind.util;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.render.DrawContext;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Logger;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;

/**
 *
 * @author vito
 */
public class FrameBufferController implements Disposable{

	private FrameBuffer currentFramebuffer = null;
	
	private final HashSet<FrameBuffer> framebuffers = new HashSet<FrameBuffer>();
	private final Stack<FrameBuffer> framebufferStack = new Stack<FrameBuffer>();

	private DrawContext dc;
	
	private static final Logger logger = Logging.logger("gov.nasa.worldwind.util.FramebufferState");
	
	public FrameBufferController(){
		
	}

	public void init(DrawContext dc){
		this.dc = dc;

		GL2 gl = dc.getGL().getGL2();
		for(FrameBuffer fb : framebuffers){
			if(!gl.glIsFramebuffer(fb.getID())){
				framebuffers.remove(fb);
			}
		}
	}

	public void addFramebuffer(FrameBuffer frambuffer){
		this.framebuffers.add(frambuffer);
	}

	public void removeFramebuffer(FrameBuffer frambuffer){
		this.framebuffers.remove(frambuffer);
	}

	public boolean containsFrambuffer(FrameBuffer frambuffer){
		return this.framebuffers.contains(frambuffer);
	}

	public void push(){

		if(GLContext.getCurrent() == null){
			logger.severe("push(): GLContext not current on current thread");
			throw new IllegalStateException("GLContext not current on current thread");
		}
		
		if(currentFramebuffer == null){
			return;
		}
		this.framebufferStack.push(currentFramebuffer);
	}

	public void pop(){

		if(GLContext.getCurrent() == null){
			logger.severe("push(): GLContext not current on current thread");
			throw new IllegalStateException("GLContext not current on current thread");
		}
		
		if(this.framebufferStack.empty()){
			GL2 gl = dc.getGL().getGL2();
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			this.currentFramebuffer = null;
		}
		else{
			FrameBuffer fb = this.framebufferStack.pop();
			fb.bind(dc);
		}
	}

	public void setCurrent(FrameBuffer framebuffer){
		this.currentFramebuffer = framebuffer;	
	}

	public FrameBuffer getCurrent(){
		return this.currentFramebuffer;
	}

	public void dispose()
	{
		if(GLContext.getCurrent() == null){
			String message = "BasicGLSLShader.disable: GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}
		
		for(FrameBuffer fb : framebuffers){
			fb.dispose(GLContext.getCurrent().getGL().getGL2());
		}
		framebuffers.clear();
		framebufferStack.clear();
	}
}
