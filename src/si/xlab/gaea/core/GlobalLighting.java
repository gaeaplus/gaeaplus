package si.xlab.gaea.core;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.Shader;
import gov.nasa.worldwind.util.FrameBuffer;
import java.awt.Rectangle;
import java.nio.FloatBuffer;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import si.xlab.gaea.core.layers.atm.Atmosphere;
import si.xlab.gaea.core.layers.elev.HeightMapLayer;
import si.xlab.gaea.core.shaders.effects.FinalPostProcessing;

/**
 *
 * @author vito
 */
public class GlobalLighting
{
    private final Atmosphere atmosphere = Atmosphere.getInstance();
    private final FinalPostProcessing postProcessing;
    private final FrameBuffer frambuffer;

    private int[] shadowVolumeTexture = new int[1];
    private int[] usageTexture = new int[1];
    
    private int[] fbo = new int[1];
    private final int[] finalTextureHDR = new int[1];
    private final int[] finalTexture = new int[1];
    
    private boolean useShadowVolume = false;
    private boolean enableBloom = true;
    private boolean enableBlureDistance = true;
    
    private final FrameBuffer frambufferIntensity;
    private float intensityAveLast = 0.0f;
    private float intensityAveSumLast = 0.0f;
    private float intensityDevLast = 0;
    private float intensityDevSumLast = 0;
    
    private Rectangle viewport = null;
    //support layers
    public static final SupportLayers supportLayers = new SupportLayers();

    public GlobalLighting()
    {
        //this.useShadowVolume = true;
        //this.enablePosEffects = false;
        //this.enableBloom = false;

        this.frambuffer = new FrameBuffer();
        this.frambufferIntensity = new FrameBuffer();
        this.postProcessing = new FinalPostProcessing(frambuffer);
    }

    void drawQuadTex(GL2 gl)
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

    private void createResizeFbo(GL gl){
        if (!gl.glIsFramebuffer(fbo[0])){
            gl.glGenFramebuffers(1, fbo, 0);
        }
    }

    private void bindFbo(DrawContext dc, int depthTexture){
        GL2 gl = dc.getGL().getGL2();
        dc.getFramebufferController().push();
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo[0]);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
                GL.GL_TEXTURE_2D, depthTexture, 0);
        gl.glDepthMask(false);
        gl.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
    }

    private void renderLayerToFbo(DrawContext dc, Layer layer, int texture, boolean clear){
        GL gl = dc.getGL();

        gl.glBindTexture(GL.GL_TEXTURE_2D, texture);
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
                GL.GL_TEXTURE_2D, texture, 0);

        if (clear){
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        }

        //render layer
		if(layer != null){
        	dc.setCurrentLayer(layer);
        	layer.render(dc);
        	dc.setCurrentLayer(null);
		}
    }

    private void releaseFbo(DrawContext dc){
        GL gl = dc.getGL();
        gl.glDepthMask(true);
        dc.getFramebufferController().pop();
    }

    private void createResizeScreenTexture(DrawContext dc, int[] texture){
        GL gl = dc.getGL();

        if (!gl.glIsTexture(texture[0])){
            gl.glGenTextures(1, texture, 0);
        }

        gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);

        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, viewport.width,
                viewport.height, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, null);
    }

    private void createResizeScreenTextureHDR(DrawContext dc, int[] texture){
        GL gl = dc.getGL().getGL();

        if (!gl.glIsTexture(texture[0])){
            gl.glGenTextures(1, texture, 0);
        }

        gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA32F, viewport.width,
                viewport.height, 0, GL.GL_BGRA, GL.GL_FLOAT, null);
    }


    public void renderEffects(DrawContext dc, DeferredRendererImpl dr){
        GL gl = dc.getGL();

        if (!dc.isSunLightEnabled())
            return;

        if (!atmosphere.isTexturesDone())
        {
            atmosphere.precompute(dc);
        }
        if (!atmosphere.isTexturesDone())
        {
            return;
        }

        if (viewport == null || !viewport.equals(dr.getViewport()))
        {
            viewport = new Rectangle(dr.getDrawableWidth(), dr.getDrawableHeight());
            createResizeFbo(gl);
            createResizeScreenTexture(dc, usageTexture);

			//used only for pos-effects
            createResizeScreenTextureHDR(dc, finalTextureHDR);
            createResizeScreenTexture(dc, finalTexture);
        }

        if (dc.isAerialPerspectiveEnabled() && useShadowVolume)
        {
            //generate shadow volume texture
            shadowVolumeTexture[0] = ShadowMapFactory.getWorldShadowVolumeInstance().render(dc, dr, false);
        }

        /////////////////////render support layers////////////////////////////
        Layer layer = supportLayers.getMaterialLayer(dc);
		if(layer != null){
			layer.preRender(dc);
		}
        bindFbo(dc, dr.getDepthTexture());
        renderLayerToFbo(dc, supportLayers.getMaterialLayer(dc), usageTexture[0], true);
        releaseFbo(dc);
        //////////////////////////////////////////////////////////////////////

        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, dr.getColorTexture());

        gl.glActiveTexture(GL.GL_TEXTURE1);
        gl.glBindTexture(GL.GL_TEXTURE_2D, dr.getNormalTexture());

        gl.glActiveTexture(GL.GL_TEXTURE2);
        gl.glBindTexture(GL.GL_TEXTURE_2D, dr.getDepthTexture());
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_MODE, GL.GL_NONE);

        if (dc.isShadowsEnabled())
        {
            gl.glActiveTexture(GL.GL_TEXTURE3);
            ShadowMapFactory.getWorldShadowMapInstance(dc).setShadowTerrain(!this.useShadowVolume);
            ShadowMapFactory.getWorldShadowMapInstance(dc).bindTexture(gl);
        }

        if (dc.isAtmosphereEnabled())
        {
            gl.glActiveTexture(GL.GL_TEXTURE4);
            gl.glBindTexture(GL.GL_TEXTURE_2D, atmosphere.getTransmittanceTexture());

            gl.glActiveTexture(GL.GL_TEXTURE5);
            gl.glBindTexture(GL.GL_TEXTURE_2D, atmosphere.getIrradianceTexture());

            gl.glActiveTexture(GL.GL_TEXTURE6);
            gl.glBindTexture(GL2.GL_TEXTURE_3D, atmosphere.getInscatterTexture());

            if (dc.isAerialPerspectiveEnabled())
            {
                gl.glActiveTexture(GL.GL_TEXTURE7);
                gl.glBindTexture(GL.GL_TEXTURE_2D, usageTexture[0]);

                if (useShadowVolume)
                {
                    gl.glActiveTexture(GL.GL_TEXTURE8);
                    gl.glBindTexture(GL.GL_TEXTURE_2D, shadowVolumeTexture[0]);
                }
            }
        }

        ////////////object materials texture//////////
        gl.glActiveTexture(GL.GL_TEXTURE9);
        gl.glBindTexture(GL.GL_TEXTURE_2D, dr.getMaterialTexture());
        //////////////////////////////////////////////

        gl.glActiveTexture(GL.GL_TEXTURE0);

        gl.glDepthFunc(GL.GL_ALWAYS);

        ////////////////test new postProcessing//////////////

        if (dc.isPosEffectsEnabled())
        {

            //render to offscreen framebuffer
            dc.getFramebufferController().push();
            frambuffer.bind(dc);

            //reset
//			frambuffer.attachTexture(dc, 0, GL.GL_COLOR_ATTACHMENT0, 0);
//			frambuffer.attachTexture(dc, 0, GL.GL_COLOR_ATTACHMENT1_EXT, 0);

            frambuffer.releaseTextures(dc);
            frambuffer.attachTexture2D(dc, GL.GL_COLOR_ATTACHMENT0, finalTextureHDR[0], GL.GL_TEXTURE_2D);
            frambuffer.setDrawBuffers(dc, new int[]{GL.GL_COLOR_ATTACHMENT0});
			frambuffer.setReadBuffers(dc, GL.GL_COLOR_ATTACHMENT0);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            frambuffer.isComplete(dc, false);

            //render lighting effects//
            //gl.glPushAttrib(GL2.GL_ENABLE_BIT);
            gl.glDisable(GL.GL_BLEND);
            gl.glDisable(GL2.GL_ALPHA_TEST);
            
            if (dc.isAerialPerspectiveEnabled())
            {
                renderTerrainArialPerspective(dc);
            } else
            {
                renderTerrain(dc);
            }
			if (dc.isAtmosphereEnabled())
            {
                renderAtmosphere(dc);
            }
            //gl.glPopAttrib();

            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, finalTextureHDR[0]);
            float[] intensity = getIntensity(dc);
			//float[] intensity = new float[]{2.6f, 0.5f};

            //render postprocessing effects//
            postProcessing.setBloom(this.enableBloom);
            postProcessing.setBlureDistance(this.enableBlureDistance);
            postProcessing.setDepthTexture(dr.getDepthTexture());
            postProcessing.setIntensity(intensity);
            postProcessing.setTexture(finalTextureHDR[0],
                    GL.GL_RGBA32F,
                    GL.GL_BGRA,
                    GL.GL_FLOAT,
                    viewport.width, viewport.height);
            postProcessing.renderEffect(dc);

            //set last framebuffer
            dc.getFramebufferController().pop();
            postProcessing.renderTonemap(dc);
        } else
        {
            
            if (dc.isAerialPerspectiveEnabled())
            {
                renderTerrainArialPerspective(dc);
            } else
            {
                renderTerrain(dc);
            }

			if (dc.isAtmosphereEnabled())
            {
                renderAtmosphere(dc);
            }
        }
        gl.glDepthFunc(GL.GL_LEQUAL);
        //////////////////////////////////////////////////////

//		if(this.enableCSAA){
//			//copy color (frame buffer -> texture)
//			gl.glBindTexture(GL.GL_TEXTURE_2D, finalTexture[0]);
//			gl.glCopyTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8, 0, 0, viewport.width, viewport.height, 0);
//			////////////////////////////////////
//			gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, finalFbo[0]);
////			blure(dc, finalTexture[0], effectTexture1[0], false, 0.0f, 4.0f);
//
//			gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0,
//				  GL.GL_TEXTURE_2D, effectTexture1[0], 0);
//			renderCSAAmask(dc);
//			gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_COLOR_ATTACHMENT0,
//				  GL.GL_TEXTURE_2D, finalTextureHDR[0], 0);
//			gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
//
//			gl.glActiveTexture(GL.GL_TEXTURE0);
//			gl.glBindTexture(GL.GL_TEXTURE_2D, finalTexture[0]);
//
//			gl.glActiveTexture(GL.GL_TEXTURE1);
//			gl.glBindTexture(GL.GL_TEXTURE_2D, effectTexture1[0]);
//
//			float pixelHDist = ((float)(1.0d/viewport.height));
//			float pixelWDist = ((float)(1.0d/viewport.width));
//
//			Shader shader;
//			shader = dc.getShaderContext().getShader("CSAA.glsl", "#version 120\n");
//			shader.enable(dc.getShaderContext());
//			shader.setParam("colorTex", 0);
//			shader.setParam("csaaTex", 1);
//			shader.setParam("pixelW", new float[]{pixelWDist});
//			shader.setParam("pixelH", new float[]{pixelHDist});
//
//			drawQuadTex(gl);
//
//			shader.disable(dc.getShaderContext());
//			gl.glActiveTexture(GL.GL_TEXTURE0);
//		}
    }

    /*
    private void renderCSAAmask(DrawContext dc){
    
    GL gl = dc.getGL();
    
    float pixelHDist = ((float)(1.0d/viewport.height));
    float pixelWDist = ((float)(1.0d/viewport.width));
    
    Shader shader;
    shader = dc.getShaderContext().getShader("CSAAmask.glsl", "#version 120\n");
    shader.enable(dc.getShaderContext());
    //		shader.setParam("normalTex", 1);
    shader.setParam("depthTex", 2);
    shader.setParam("sampleDistX", new float[]{pixelWDist});
    shader.setParam("sampleDistY", new float[]{pixelHDist});
    
    float b = (float)NbPreferences.root().getDouble("TIME_C_V_I", 1.0d)/1000.0f;
    float w = (float)NbPreferences.root().getDouble("TIME_C_V_B", 1.0d);
    
    shader.setParam("barrier", new float[]{b,b});
    shader.setParam("weights", new float[]{w,w});
    drawQuadTex(gl);
    shader.disable(dc.getShaderContext());
    }
     * 
     */
    private float[] getIntensity(DrawContext dc){
		
        GL gl = dc.getGL();
        GL2 gl2 = dc.getGL().getGL2();
        dc.getFramebufferController().push();
        
        if(!this.frambufferIntensity.hasRenderBufferAttached()){
            this.frambufferIntensity.bind(dc);
            this.frambufferIntensity.attachRenderbuffer(dc, 32, 32, GL.GL_RGBA32F, GL.GL_COLOR_ATTACHMENT0, 0);   
        }
        
        if(!this.frambufferIntensity.isComplete(dc, false)){
            return new float[]{2.6f, 0.5f}; 
        }

        gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, this.frambuffer.getID());
        gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, this.frambufferIntensity.getID());

        gl2.glReadBuffer(GL.GL_COLOR_ATTACHMENT0);
        gl2.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);
        gl2.glBlitFramebuffer(0, 0, this.viewport.width, this.viewport.height,
                                0, 0, 32, 32, GL.GL_COLOR_BUFFER_BIT, GL.GL_LINEAR);

        gl2.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, this.frambufferIntensity.getID());
		int samples = 32;

		float intensity = 0;
		float[] intensities = new float[samples];
		float intensityAve = 0;
		float intensityDev = 0;

		FloatBuffer buffer = Buffers.newDirectFloatBuffer(4);
		
		int counter = 0;
		for(int i=0; i<samples; i++){
			int x = (int)(Math.random()*(double)32);
			int y = (int)(Math.random()*(double)32);

			gl.glReadPixels(x, y, 1, 1, GL.GL_BGRA, GL.GL_FLOAT, buffer);
			intensity = buffer.get(3);
			buffer.rewind();
			if(intensity < 0.0001 || Float.isNaN(intensity)){
				continue;
			}
			intensities[counter] = Math.max(intensity, 0.0001f);
			intensityAve += intensities[counter];
			counter += 1;
		}
		buffer.clear();

		intensityAve = (intensityAve+0.0001f)/((float)counter + 0.01f);

		for(int i=0; i<counter; i++){
			intensityDev += ((intensities[i] - intensityAve) * (intensities[i] - intensityAve));
		}

		intensityDev = (float)Math.sqrt(intensityDev/((float)counter+0.01f));
	
		float k = 0.1f;
		intensityAve = intensityAveLast + ((intensityAve - intensityAveLast) * k);
		intensityAveLast = intensityAve;
		intensityAve = intensityAveSumLast + ((intensityAve - intensityAveSumLast) * k);
		intensityAveSumLast = intensityAve;

		intensityDev = intensityDevLast + ((intensityDev - intensityDevLast) * k);
		intensityDevLast = intensityDev;
		intensityDev = intensityDevSumLast + ((intensityDev - intensityDevSumLast) * k);
		intensityDevSumLast = intensityDev;
        
        dc.getFramebufferController().pop();
		
		//debug graph
//		fGraph.render(dc, (0.77f + (intensityAve * 0.005f))/intensityAve, Graph.FILL);
		//
		
		return new float[]{intensityAve, intensityDev};
	}

//	Graph fGraph = new Graph(0.0f, 20.0f, new float[]{1.0f,0.0f,0.0f,0.5f});
//	Graph rGraph = new Graph(0.0f, 20.0f, new float[]{0.0f,0.0f,1.0f,0.5f});
//	float f;
//	float r;
    public void renderAtmosphere(DrawContext dc)
    {
        Shader shader;

        String glslOptions = "#version 120\n" + atmosphere.getParamCode();
        glslOptions += dc.isPosEffectsEnabled() ? "#define _POSEFFECTS_ \n" : "";

        shader = dc.getShaderContext().getShader("Atmosphere_1.glsl", glslOptions);
        shader.enable(dc.getShaderContext());

        dc.getView().pushReferenceCenter(dc, Vec4.ZERO);

        shader.setParam("depthSampler", 2);
        shader.setParam("transmittanceSampler", 4);
        shader.setParam("inscatterSampler", 6);

        shader.setParam("lightDirection", dc.getSunlightDirection());
        shader.setParam("cameraWorldPosition", dc.getView().getEyePoint());
        shader.setParam("exposure", new float[]
                {
                    calcExposure(dc)
                });

        drawQuadTex(dc.getGL().getGL2());
        dc.getView().popReferenceCenter(dc);

        shader.disable(dc.getShaderContext());
    }

    public void renderTerrain(DrawContext dc)
    {

		String glslOptions = "#version 120\n";
        glslOptions += dc.isPosEffectsEnabled() ? "#define _POSEFFECTS_ \n" : "";
        glslOptions += dc.isShadowsEnabled() ? "#define _SHADOW_ \n" : "";
        glslOptions += (useShadowVolume && dc.isShadowsEnabled()) ? "#define _SHADOW_VOLUME_ \n" : "";
		
        Shader shader;
        shader = dc.getShaderContext().getShader("TerrainLighting_1.glsl", glslOptions);
			
        shader.enable(dc.getShaderContext());

        if (dc.isShadowsEnabled())
        {
            shader.setParam("eyeToShadowTextureTransform",
                    ShadowMapFactory.getWorldShadowMapInstance(dc).computeEyeToTextureTransform(dc.getView()));
            shader.setParam("shadowSampler", 3);
        }

        shader.setParam("lightDirection", dc.getSunlightDirection());
        shader.setParam("lightColor", simpleSunColor(dc));
        shader.setParam("colorSampler", 0);
        shader.setParam("normalSampler", 1);
        shader.setParam("depthSampler", 2);
        shader.setParam("cameraWorldPosition", dc.getView().getEyePoint());
        shader.setParam("zNear", new float[]
                {
                    (float) dc.getView().getNearClipDistance()
                });
        shader.setParam("zFar", new float[]
                {
                    (float) dc.getView().getFarClipDistance()
                });
		shader.setParam("exposure", new float[]
                {
                    calcExposure(dc)
                });

        dc.getView().pushReferenceCenter(dc, Vec4.ZERO);
        drawQuadTex(dc.getGL().getGL2());
        dc.getView().popReferenceCenter(dc);
        shader.disable(dc.getShaderContext());
    }

    public void renderTerrainArialPerspective(DrawContext dc)
    {
        Shader shader;

        String glslOptions = "#version 120\n" + atmosphere.getParamCode();
        glslOptions += dc.isPosEffectsEnabled() ? "#define _POSEFFECTS_ \n" : "";
        glslOptions += dc.isShadowsEnabled() ? "#define _SHADOW_ \n" : "";
        glslOptions += (useShadowVolume && dc.isShadowsEnabled()) ? "#define _SHADOW_VOLUME_ \n" : "";

        shader = dc.getShaderContext().getShader("TerrainLightingAerialPerspective_1.glsl", glslOptions);

        shader.enable(dc.getShaderContext());

        if (dc.isShadowsEnabled())
        {
            shader.setParam("eyeToShadowTextureTransform",
                    ShadowMapFactory.getWorldShadowMapInstance(dc).computeEyeToTextureTransform(dc.getView()));
            shader.setParam("shadowSampler", 3);
            if (useShadowVolume)
            {
                shader.setParam("shadowVolumeSampler", 8);
            }
        }
        shader.setParam("colorSampler", 0);
        shader.setParam("normalSampler", 1);
        shader.setParam("depthSampler", 2);
        shader.setParam("transmittanceSampler", 4);
        shader.setParam("irradianceSampler", 5);
        shader.setParam("inscatterSampler", 6);
        shader.setParam("usageSampler", 7);
        shader.setParam("materialSampler", 9);

        shader.setParam("lightDirection", dc.getSunlightDirection());
        shader.setParam("exposure", new float[]
                {
                    calcExposure(dc)
                });
        shader.setParam("cameraWorldPosition", dc.getView().getEyePoint());
        shader.setParam("zNear", new float[]
                {
                    (float) dc.getView().getNearClipDistance()
                });
        shader.setParam("zFar", new float[]
                {
                    (float) dc.getView().getFarClipDistance()
                });

        dc.getView().pushReferenceCenter(dc, Vec4.ZERO);
        drawQuadTex(dc.getGL().getGL2());
        dc.getView().popReferenceCenter(dc);

        shader.disable(dc.getShaderContext());
    }

    public static float calcExposure(DrawContext dc)
    {

        Vec4 eye = dc.getView().getEyePoint();

        double eR = dc.getGlobe().getRadiusAt(dc.getView().getEyePosition());
        double cR = eye.getLength3();

        double maxAngle = Math.PI - Math.asin(eR / cR);
        double angle = eye.angleBetween3(dc.getSunlightDirection().getNegative3()).radians;

        double pow = 4.5;
        double fac = 0.65;

//		double pow = (float)NbPreferences.root().getDouble("TIME_C_V_I", 1.0d);
//		double fac = (float)NbPreferences.root().getDouble("TIME_C_V_G", 1.0d);

        return (float) (2.2f * (0.06d + Math.pow(Math.min(angle / maxAngle, 3.8d) * fac, pow)));
    }

    public static float[] simpleSunColor(DrawContext dc)
    {

        float[] color = new float[3];

        float tmp = 1.0f;
        if (dc.getSunlightDirection() != null && dc.getView() != null && dc.getView().getCenterPoint() != null)
        {
            tmp = (float) (dc.getSunlightDirection().getNegative3().normalize3().dot3(dc.getView().getCenterPoint().normalize3()));
        }

        color[0] = 1.0f;
        color[1] = 0.85f + 0.15f * (float) Math.sqrt(Math.max(tmp, 0.0));
        color[2] = 0.5f + 0.5f * (float) Math.sqrt(Math.max(tmp, 0.0));

        return color;
    }

    public void dispose(GL gl)
    {
        GlobalLighting.supportLayers.dispose();
        gl.glDeleteTextures(1, this.usageTexture, 0);
        gl.glDeleteTextures(1, this.shadowVolumeTexture, 0);
        gl.glDeleteTextures(1, this.finalTextureHDR, 0);
        gl.glDeleteTextures(1, this.finalTexture, 0);
        gl.glDeleteFramebuffers(1, this.fbo, 0);
        viewport = null;
    }

    public static SupportLayers getSupportLayers()
    {
        return supportLayers;
    }

    public static class SupportLayers
    {
		private HeightMapLayer heightMap = null;
		private Layer materialLayer = null;

        public void dispose()
        {
            if(heightMap != null)
                heightMap.dispose();
        }

		public Layer getMaterialLayer(DrawContext dc){
			return materialLayer;
		}

        public Layer getHeightLayer(DrawContext dc){
			if(heightMap == null){
				heightMap = new HeightMapLayer();
				heightMap.setRetainLevelZeroTiles(false);
				heightMap.setForceLevelZeroLoads(true);
				heightMap.setDetailHint(0.25);
				heightMap.setUseMipMaps(false);
				heightMap.setUseTransparentTextures(false);
				heightMap.setEnabled(true);
				heightMap.addPropertyChangeListener(dc.getModel().getLayers());
			}
			return heightMap;
		}
    }
}
