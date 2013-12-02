package si.xlab.gaea.core;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderAttributes;
import gov.nasa.worldwind.render.DeferredRenderer;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.Shader;
import gov.nasa.worldwind.util.FrameBuffer;
import gov.nasa.worldwind.util.Logging;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public class DeferredRendererImpl implements DeferredRenderer {

	public static int FSAA = 0;
	private Rectangle viewport = null;
	private final int[] colorTexture = new int[1];
	private final int[] depthTexture = new int[1];
	private final int[] normalTexture = new int[1];
	private final int[] materialTexture = new int[1];
	private FrameBuffer fbo = new FrameBuffer();
	private FrameBuffer fboFsaa = new FrameBuffer();
	private boolean enabled = false;

	//public static class Support

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public boolean isSupported(DrawContext dc){
		if(dc.getGLRuntimeCapabilities().getNumTextureUnits() < 10 
				|| !dc.getGL().hasFullFBOSupport()
				|| !dc.getGL().isNPOTTextureAvailable()){
			return false;
		}
		return true;
	}

	public int getDrawableHeight() {
		return viewport != null ? viewport.height : 0;
	}

	public int getDrawableWidth() {
		return viewport != null ? viewport.width : 0;
	}

	public Rectangle getViewport() {
		Rectangle out = new Rectangle(viewport);
		return out;
	}

	@Override
	public int getColorTexture() {
		return colorTexture[0];
//		return materialTexture[0];
//		return normalTexture[0];
	}

	@Override
	public int getMaterialTexture() {
		return materialTexture[0];
	}

	@Override
	public int getNormalTexture() {
		return normalTexture[0];
	}

	@Override
	public int getDepthTexture() {
		return depthTexture[0];
	}

	private void createResizeFbo(DrawContext dc) {

		fbo.bind(dc);
		fbo.attachTexture2D(dc, GL2.GL_DEPTH_ATTACHMENT, depthTexture[0], GL2.GL_TEXTURE_2D);
		fbo.attachTexture2D(dc, GL2.GL_COLOR_ATTACHMENT0, colorTexture[0], GL2.GL_TEXTURE_2D);
		fbo.attachTexture2D(dc, GL2.GL_COLOR_ATTACHMENT1, normalTexture[0], GL2.GL_TEXTURE_2D);
		fbo.attachTexture2D(dc, GL2.GL_COLOR_ATTACHMENT2, materialTexture[0], GL2.GL_TEXTURE_2D);

		int[] bufs = {GL2.GL_COLOR_ATTACHMENT0};
		fbo.setDrawBuffers(dc, bufs);

		//create multisampled renderbuffers
		if (FSAA > 0) {
			fboFsaa.bind(dc);
			fboFsaa.releaseRenderbuffers(dc);
			fboFsaa.attachRenderbuffer(dc, dc.getDrawableWidth(), dc.getDrawableHeight(),
				GL2.GL_DEPTH_COMPONENT, GL2.GL_DEPTH_ATTACHMENT, FSAA);
			fboFsaa.attachRenderbuffer(dc, dc.getDrawableWidth(), dc.getDrawableHeight(),
				GL2.GL_RGBA, GL2.GL_COLOR_ATTACHMENT0, FSAA);
			fboFsaa.attachRenderbuffer(dc, dc.getDrawableWidth(), dc.getDrawableHeight(),
				GL2.GL_RGBA, GL2.GL_COLOR_ATTACHMENT1, FSAA);
			fboFsaa.attachRenderbuffer(dc, dc.getDrawableWidth(), dc.getDrawableHeight(),
				GL2.GL_RGBA, GL2.GL_COLOR_ATTACHMENT2, FSAA);
			fboFsaa.setDrawBuffers(dc, bufs);
		}

	}

	private void bindFbo(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();

		if (FSAA == 0) {
			fbo.bind(dc);
		} else {
			fboFsaa.bind(dc);
		}

		gl.glViewport(0, 0, viewport.width, viewport.height);
	}

	private void createResizeScreenDepthTexture(DrawContext dc, int[] texture) {

		GL2 gl = dc.getGL().getGL2();

		if (!gl.glIsTexture(texture[0])) {
			gl.glGenTextures(1, texture, 0);
		}

		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture[0]);

		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_LUMINANCE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_FUNC, GL2.GL_LEQUAL);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT24, viewport.width, viewport.height, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_UNSIGNED_BYTE, null);
	}

	private void createResizeScreenTexture(DrawContext dc, int[] texture) {

		GL2 gl = dc.getGL().getGL2();

		if (!gl.glIsTexture(texture[0])) {
			gl.glGenTextures(1, texture, 0);
		}

		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture[0]);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8,
			viewport.width, viewport.height, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, null);
	}

	private void createResizeScreenFloatTexture(DrawContext dc, int[] texture) {

		GL2 gl = dc.getGL().getGL2();

		if (!gl.glIsTexture(texture[0])) {
			gl.glGenTextures(1, texture, 0);
		}

		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture[0]);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA16F,
			viewport.width, viewport.height, 0, GL2.GL_RGBA, GL2.GL_FLOAT, null);
	}

	@Override
	public void begin(DrawContext dc) {

		dc.getFramebufferController().push();
		Rectangle currentViewport = dc.getView().getViewport();
		if (viewport == null || viewport.height != currentViewport.height || viewport.width != currentViewport.width) {
			viewport = new Rectangle(0, 0);
			viewport.width = currentViewport.width;
			viewport.height = currentViewport.height;
			createResizeScreenDepthTexture(dc, depthTexture);
			createResizeScreenTexture(dc, colorTexture);
			createResizeScreenFloatTexture(dc, normalTexture);
			createResizeScreenTexture(dc, materialTexture);
			createResizeFbo(dc);
		}

		GL2 gl = dc.getGL().getGL2();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, depthTexture[0]);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_DEPTH_TEXTURE_MODE, GL2.GL_LUMINANCE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_FUNC, GL2.GL_LEQUAL);

		bindFbo(dc);

		int[] bufs = {GL2.GL_COLOR_ATTACHMENT0, GL2.GL_COLOR_ATTACHMENT1, GL2.GL_COLOR_ATTACHMENT2};
		gl.glDrawBuffers(3, bufs, 0);
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
	}

	@Override
	public void end(DrawContext dc) {

		if (FSAA > 0) {
			//blit multisampled renderbuffer FBO to textures
			GL2 gl = dc.getGL().getGL2();
			gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, fboFsaa.getID());
			gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, fbo.getID());

			gl.glReadBuffer(GL2.GL_DEPTH_ATTACHMENT);
			gl.glDrawBuffer(GL2.GL_DEPTH_ATTACHMENT);
			gl.glBlitFramebuffer(0, 0, this.viewport.width, this.viewport.height,
				0, 0, this.viewport.width, this.viewport.height, GL2.GL_DEPTH_BUFFER_BIT, GL2.GL_LINEAR);

			for (int i = 0; i < 3; i++) {
				gl.glReadBuffer(GL2.GL_COLOR_ATTACHMENT0 + i);
				gl.glDrawBuffer(GL2.GL_COLOR_ATTACHMENT0 + i);
				gl.glBlitFramebuffer(0, 0, this.viewport.width, this.viewport.height,
					0, 0, this.viewport.width, this.viewport.height, GL2.GL_COLOR_BUFFER_BIT, GL2.GL_LINEAR);
			}
			gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, 0);
			gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, 0);
		}

		dc.getFramebufferController().pop();
	}

	@Override
	public void renderLayer(DrawContext dc, Layer layer) {

		GL2 gl = dc.getGL().getGL2();
		Shader shader = null;

		boolean renderToNormal = false;
		boolean usingTextures = false;
		List<Integer> drawBuffers = new ArrayList<Integer>();

		int renderType = layer.getRenderAttributes().getRenderMode();

		if (layer.getRenderAttributes().getRenderType() == RenderAttributes.RenderType.SPATIAL) {
			if ((renderType & RenderAttributes.NORMAL_MODE) > 0) {
				drawBuffers.add(GL2.GL_COLOR_ATTACHMENT1);
				renderToNormal = true;
			}
			if ((renderType & RenderAttributes.COLOR_MODE) > 0) {
				drawBuffers.add(GL2.GL_COLOR_ATTACHMENT0);
				shader = dc.getShaderContext().getShader("DefferedColor.glsl", "#version 120\n");
				usingTextures = false;
			}
			if ((renderType & RenderAttributes.TEXTURE_MODE) > 0) {
				drawBuffers.add(GL2.GL_COLOR_ATTACHMENT0);
				shader = dc.getShaderContext().getShader("DefferedTexture.glsl", "#version 120\n");
				shader.setParam("colorSampler", 0);
				usingTextures = true;
			}
			if ((renderType & RenderAttributes.MATERIAL_MODE) > 0) {
				drawBuffers.add(GL2.GL_COLOR_ATTACHMENT2);
				shader = dc.getShaderContext().getShader("DefferedObject.glsl", "#version 120\n");
				shader.setParam("colorSampler", 0);
				if(usingTextures){
					shader.setParam("useTexture", new float[]{1.0f});
				}
			}
		} 
		else if (layer.getRenderAttributes().getRenderType() == RenderAttributes.RenderType.GLOBE) {
			shader = dc.getShaderContext().getShader("SurfaceTileShader.glsl", "#version 120\n");
			shader.setParam("colorSampler", 0);
			shader.setParam("exaggeration", new float[]{(float) dc.getVerticalExaggeration()});
			drawBuffers.add(GL2.GL_COLOR_ATTACHMENT0);
		}

		int[] drawBuffersArray = new int[drawBuffers.size()];
		Iterator<Integer> iter = drawBuffers.iterator();
		for (int i = 0; i < drawBuffers.size(); i++) {
			if (iter.hasNext()) {
				drawBuffersArray[i] = iter.next().intValue();
			}
		}

		gl.glDrawBuffers(drawBuffersArray.length, drawBuffersArray, 0);

		dc.getShaderContext().pushShader();

		if (shader != null && shader.isValid()) {
			dc.getShaderContext().enable(shader);
			shader.setParam("eyeToWorld", dc.getView().getModelviewMatrix().getTranspose());
			shader.setParam("u_drawDistance", new float[]{Float.MAX_VALUE});
			if (!renderToNormal) {
				shader.setParam("normalFactor", new float[]{0.0f});
			} else {
				shader.setParam("normalFactor", new float[]{1.0f});
			}
		}

		dc.setCurrentLayer(layer);
		layer.render(dc);
		dc.setCurrentLayer(null);

		dc.getShaderContext().popShader();
	}

	public void renderOrderedRenderables(DrawContext dc, RenderAttributes.RenderType renderType){
		
		if(renderType == null){
            Logging.logger().log(Level.SEVERE, 
                                 Logging.getMessage("DeferredRendererImpl.ExceptionDuringOrderableRenderableRendering: renderType is NULL!"));
			return;
		}
		
		try{
            // Draw the deferred/ordered renderables.
            dc.setOrderedRenderingMode(true);
            // dc.applyGroupingFilters();
            dc.applyClutterFilter(renderType);
            while(dc.peekOrderedRenderables(renderType) != null){
                try{
                    dc.pollOrderedRenderables(renderType).render(dc);
                }
                catch(Exception e){
                    Logging.logger().log(Level.WARNING,
                                         Logging.getMessage("DeferredRendererImpl.ExceptionDuringRendering"), e);
                }
            }
            dc.setOrderedRenderingMode(false);
        }
        catch(Throwable e){
            Logging.logger().log(Level.SEVERE, 
                                 Logging.getMessage("DefferedRendererImpl.ExceptionDuringOrderableRenderableRendering"), e);
        }
	}

	@Override
	public void renderTerrainNormals(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();
		Layer heightMap = GlobalLighting.supportLayers.getHeightLayer(dc);
		heightMap.preRender(dc);
		int[] bufs = {GL2.GL_COLOR_ATTACHMENT1};
		gl.glDrawBuffers(1, bufs, 0);
		dc.setCurrentLayer(heightMap);

		Shader shader = dc.getShaderContext().getShader("TerrainNormal.glsl", "#version 120\n");
		dc.getShaderContext().pushShader();

		dc.getShaderContext().enable(shader);
		shader.setParam("heightTex", 0);
		shader.setParam("exaggeration", new float[]{(float) dc.getVerticalExaggeration()});

		heightMap.render(dc);

		dc.getShaderContext().popShader();
		dc.setCurrentLayer(null);
	}

	public void dispose(GL2 gl) {
		gl.glDeleteTextures(1, this.colorTexture, 0);
		gl.glDeleteTextures(1, this.depthTexture, 0);
		gl.glDeleteTextures(1, this.materialTexture, 0);
		gl.glDeleteTextures(1, this.normalTexture, 0);

		fbo.dispose(gl);
		fboFsaa.dispose(gl);
//		this.drawableWidth = 0;
//		this.drawableHeight = 0;
		viewport = null;
	}
}
