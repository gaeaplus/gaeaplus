package si.xlab.gaea.core;

import gov.nasa.worldwind.AbstractSceneController;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerRenderAttributes;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.ShaderFactory;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.MeasureRenderTime;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.glu.GLU;
import si.xlab.gaea.core.shaders.data.GaeaShaderFactoryGLSL;

/**
 *
 * @author vito
 */
public class GaeaSceneController extends AbstractSceneController {

	private boolean shadowsEnabled = false;
	private boolean atmosphereEnabled = false;
	private boolean aerialPerspectiveEnabled = false;
	private boolean drawShadows = false;
	private boolean drawShadowsWhenCameraStop = false;
	private ShaderFactory shaderFactory = new GaeaShaderFactoryGLSL();
	protected DeferredRendererImpl deferredRenderer = new DeferredRendererImpl();
	protected GlobalLighting globalLighting = new GlobalLighting();
	private boolean isSunLightEnabled = false;
	private boolean isRecordingMode = false;
	private int[] occlusionQueries = null;

	public GaeaSceneController() {
		this.isSunLightEnabled = false;
		this.shadowsEnabled = false;
		this.drawShadowsWhenCameraStop = false;
		this.atmosphereEnabled = false;
		this.aerialPerspectiveEnabled = false;
		this.isRecordingMode = false;

		this.addPropertyChangeListener(new PropertyListener());
	}

	private class PropertyListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

			if (propertyChangeEvent.getPropertyName().equals(AVKey.ENABLE_SUNLIGHT)) {
				GaeaSceneController.this.isSunLightEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AVKey.ENABLE_SHADOWS)) {
				GaeaSceneController.this.shadowsEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AVKey.ENABLE_SHADOWS_ON_CAMERA_STOP)) {
				GaeaSceneController.this.drawShadowsWhenCameraStop = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AVKey.ENABLE_ATMOSPHERE)) {
				GaeaSceneController.this.atmosphereEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AVKey.ENABLE_ATMOSPHERE_WITH_AERIAL_PERSPECTIVE)) {
				GaeaSceneController.this.aerialPerspectiveEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AVKey.ENABLE_RECORDING_MODE)) {
				GaeaSceneController.this.isRecordingMode = (Boolean) propertyChangeEvent.getNewValue();
			}
		}
	}

	protected void applySun(DrawContext dc) {
		dc.setSunPosition(dc.getGlobe().getSunDirection());
		dc.setAtmosphereEnabled(atmosphereEnabled);
		dc.setAerialPerspectiveEnabled(aerialPerspectiveEnabled);
		dc.setSunLightEnabled(isSunLightEnabled);
		this.deferredRenderer.setEnabled(isSunLightEnabled);
	}

	protected void drawShadowMap(DrawContext dc) {
		if (dc == null) {
			return;
		}

		if (dc.getView() == null
				|| !this.shadowsEnabled
				|| !this.isSunLightEnabled
				|| dc.getSunPosition() == null) {

			this.drawShadows = false;
			dc.setShadowsEnabled(drawShadows);
			return;
		}

		if (this.drawShadowsWhenCameraStop) {
			if (dc.getView().isAnimating()) {
				this.drawShadows = false;
				dc.setShadowsEnabled(false);
				return;
			}
		}

		//get terrain quality
//		double target;
//		if (Configuration.getStringValue(AVKey.TESSELLATOR_CLASS_NAME).equalsIgnoreCase(
//				RectangularTessellator.class.getName()))
//		{
//			target = RectangularTessellator.getResolutionTarget();
//		}
//		else if (Configuration.getStringValue(AVKey.TESSELLATOR_CLASS_NAME).equalsIgnoreCase(
//				RectangularNormalTessellator.class.getName()))
//		{
//			target = RectangularNormalTessellator.getResolutionTarget();
//		}
//		else
//		{
//			this.shadowsEnabled = false;
//			this.drawShadows = false;
//			dc.setShadowsEnabled(drawShadows);
//			return;
//		}

		//max altitude draw shadows table
//		double[][] maxShadowElevationMap = new double[6][2];
//
//		maxShadowElevationMap[0][0] = 1.3;
//		maxShadowElevationMap[0][1] = 3500.0;
//		maxShadowElevationMap[1][0] = 1.7;
//		maxShadowElevationMap[1][1] = 9500.0;
//		maxShadowElevationMap[2][0] = 1.96;
//		maxShadowElevationMap[2][1] = 30000.0;
//		maxShadowElevationMap[3][0] = 2.03;
//		maxShadowElevationMap[3][1] = 60000.0;
//		maxShadowElevationMap[4][0] = 2.05;
//		maxShadowElevationMap[4][1] = 130000.0;
//		maxShadowElevationMap[5][0] = 2.1;
//		maxShadowElevationMap[5][1] = 410000.0;

		//compute absolute eye altitude
//		Position eyePosition = dc.getView().getEyePosition();
//		double terrainElev = dc.getGlobe().getElevation(eyePosition);
//		double distance = eyePosition.elevation - terrainElev;

//		double maxDrawShadowElevation = 3500.0;

		//compute max altitude
//		for(int i = maxShadowElevationMap.length - 1; i >= 0; i--){
//			if(target >= maxShadowElevationMap[i][0]){
//				maxDrawShadowElevation = maxShadowElevationMap[i][1];
//				break;
//			}
//		}

//		this.drawShadows = (distance < maxDrawShadowElevation ? true : false);
		this.drawShadows = true;
		dc.setShadowsEnabled(drawShadows);

		if (!drawShadows) {
			return;
		}

		//draw shadow map//////////////////////////////////////////
		dc.enableShadowMode();
		if(!ShadowMapFactory.getWorldShadowMapInstance(dc).renderShadowMap(dc, dc.getSunPosition().normalize3())){
			this.drawShadows = false;
			dc.setShadowsEnabled(false);
		}
		dc.disableShadowMode();
		///////////////////////////////////////////////////////////
	}

	protected void occlusionFilterStart(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();
		clearFrame(dc);
		SectorGeometryList sgl = dc.getSurfaceGeometry();
		if (sgl.isEmpty()) {
			return;
		}
		SortedMap<Double, SectorGeometry> sglSorted = new TreeMap<Double, SectorGeometry>();

		Vec4 eyePoint = dc.getView().getEyePoint();
		for (SectorGeometry sg : sgl) {
			double dist = (sg.getExtent().getCenter().subtract3(eyePoint)).getLengthSquared3();
			sglSorted.put(dist, sg);
		}
		sgl.clear();
		sgl.addAll(sglSorted.values());
		dc.setSurfaceGeometry(sgl);
		sglSorted.clear();

		if (occlusionQueries == null) {
			occlusionQueries = new int[200];
			gl.glGenQueries(occlusionQueries.length, occlusionQueries, 0);
		}
		gl.glColorMask(false, false, false, false);
		for (SectorGeometry sg : sgl) {
			sg.render(dc);
		}
		gl.glDepthMask(false);

		gl.glDepthFunc(GL2.GL_LEQUAL);
		for (int i = 0; i < sgl.size(); i++) {
			if (i >= 200) {
				break;
			}
			gl.glBeginQuery(GL2.GL_SAMPLES_PASSED, occlusionQueries[i]);
//			renderExtent(dc, sgl.get(i).getExtent());
			sgl.get(i).render(dc);
			gl.glEndQuery(GL2.GL_SAMPLES_PASSED);
		}
		gl.glColorMask(true, true, true, true);
		gl.glDepthMask(true);

		gl.glFlush();
	}

	protected void occlusionFilerEnd(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();
		SectorGeometryList sgl = dc.getSurfaceGeometry();
		if (sgl.isEmpty()) {
			return;
		}
		SectorGeometryList sglC = null;
		if (occlusionQueries != null) {
			sglC = new SectorGeometryList();
			int[] done = new int[1];
			do {
				int testq = sgl.size() < 200 ? sgl.size() - 1 : 199;
				gl.glGetQueryObjectiv(occlusionQueries[testq],
						GL2.GL_QUERY_RESULT_AVAILABLE,
						done, 0);
			} while (done[0] == GL2.GL_FALSE);

			int[] samples = new int[1];
			for (int i = 0; i < sgl.size(); i++) {
				if (i >= 200) {
					break;
				}
				gl.glGetQueryObjectuiv(occlusionQueries[i], GL2.GL_QUERY_RESULT, samples, 0);
				if (samples[0] > 100) {

					sglC.add(sgl.get(i));
				}
			}
		}


		Sector visibleSector = Sector.EMPTY_SECTOR;
		for (SectorGeometry sg : sglC) {
			visibleSector = visibleSector.union(sg.getSector());
		}
		sglC.setSector(visibleSector);
		dc.setSurfaceGeometry(sglC);
		dc.setVisibleSector(visibleSector);
	}

	private void renderExtent(DrawContext dc, Extent e) {

		GL2 gl = dc.getGL().getGL2();
		if (e instanceof Box) {
			Box b = (Box) e;
			Vec4 bc = b.getBottomCenter();
			dc.getView().pushReferenceCenter(dc, bc);

			Vec4 b1 = (b.getSAxis().multiply3(0.5)).add3(b.getTAxis().multiply3(0.5));
			Vec4 b2 = (b.getSAxis().multiply3(-0.5)).add3(b.getTAxis().multiply3(0.5));
			Vec4 b3 = (b.getSAxis().multiply3(-0.5)).add3(b.getTAxis().multiply3(-0.5));
			Vec4 b4 = (b.getSAxis().multiply3(0.5)).add3(b.getTAxis().multiply3(-0.5));

			Vec4 t1 = b.getRAxis().add3(b.getSAxis().multiply3(0.5)).add3(b.getTAxis().multiply3(0.5));
			Vec4 t2 = b.getRAxis().add3(b.getSAxis().multiply3(-0.5)).add3(b.getTAxis().multiply3(0.5));
			Vec4 t3 = b.getRAxis().add3(b.getSAxis().multiply3(-0.5)).add3(b.getTAxis().multiply3(-0.5));
			Vec4 t4 = b.getRAxis().add3(b.getSAxis().multiply3(0.5)).add3(b.getTAxis().multiply3(-0.5));

			gl.glBegin(GL2.GL_QUADS);
			gl.glVertex3d(b1.x, b1.y, b1.z);
			gl.glVertex3d(b2.x, b2.y, b2.z);
			gl.glVertex3d(b3.x, b3.y, b3.z);
			gl.glVertex3d(b4.x, b4.y, b4.z);

			gl.glVertex3d(t1.x, t1.y, t1.z);
			gl.glVertex3d(t2.x, t2.y, t2.z);
			gl.glVertex3d(t3.x, t3.y, t3.z);
			gl.glVertex3d(t4.x, t4.y, t4.z);
			gl.glEnd();

			gl.glBegin(GL2.GL_QUAD_STRIP);
			gl.glVertex3d(b1.x, b1.y, b1.z);
			gl.glVertex3d(t1.x, t1.y, t1.z);

			gl.glVertex3d(b2.x, b2.y, b2.z);
			gl.glVertex3d(t2.x, t2.y, t2.z);

			gl.glVertex3d(b3.x, b3.y, b3.z);
			gl.glVertex3d(t3.x, t3.y, t3.z);

			gl.glVertex3d(b4.x, b4.y, b4.z);
			gl.glVertex3d(t4.x, t4.y, t4.z);

			gl.glVertex3d(b1.x, b1.y, b1.z);
			gl.glVertex3d(t1.x, t1.y, t1.z);
			gl.glEnd();

			dc.getView().popReferenceCenter(dc);
		} else {
			Vec4 c = e.getCenter();
			dc.getView().pushReferenceCenter(dc, c);
			GLU glu = dc.getGLU();
			glu.gluSphere(glu.gluNewQuadric(), e.getRadius(), 6, 6);
			dc.getView().popReferenceCenter(dc);
		}
	}

	@Override
	protected void initializeFrame(DrawContext dc) {
		super.initializeFrame(dc);
		dc.setDefferedRenderer(deferredRenderer);
		dc.getShaderContext().setShaderFactory(this.shaderFactory);
		dc.setRecordingMode(isRecordingMode);
	}

	@Override
	protected void drawLayer(DrawContext dc, Layer layer) {
		try {
			if (deferredRenderer.isEnabled()) {
				MeasureRenderTime.startMesure(dc, layer.getName());
				deferredRenderer.renderLayer(dc, layer);
				MeasureRenderTime.stopMesure(dc);
				return;
			}

			if (layer != null && layer.isEnabled()) {
				MeasureRenderTime.startMesure(dc, layer.getName());
				dc.setCurrentLayer(layer);
				layer.render(dc);
				MeasureRenderTime.stopMesure(dc);
			}
		} catch (Exception e) {
			String message = Logging.getMessage("SceneController.ExceptionWhileRenderingLayer",
					(layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
			Logging.logger().log(Level.SEVERE, message, e);
			// Don't abort; continue on to the next layer.
		}
	}

//	protected void doDrawLayer(DrawContext dc, Layer layer, Shader shader){
//		if(shader == null){
//			dc.setCurrentLayer(layer);
//			layer.render(dc);
//		}
//		else{
//			shader.enable(dc.getShaderContext());
//			dc.setCurrentLayer(layer);
//			layer.render(dc);
//			shader.disable(dc.getShaderContext());
//		}
//	}

	/*
	 private Shader setShaderForLayer(DrawContext dc, Layer layer){

	 Shader shader = null;

	 if(dc.isPickingMode() || !dc.isSunLightEnabled()){
	 return null;
	 }

	 if(dc.isSunLightEnabled() && layer instanceof Shadow){
	 if(dc.isShadowMode()){
	 shader = dc.getShaderContext().getShader("DepthShader.glsl", "#version 120\n");
	 }
	 else if(layer.getRenderAttributes().getRenderMode() == LayerRenderAttributes.COLOR_MODE)
	 {
	 shader = dc.getShaderContext().getShaderFactory().getColorLightingShader(dc);
	 }
	 else if(layer.getRenderAttributes().getRenderMode() == LayerRenderAttributes.TEXTURE_MODE)
	 {
	 shader = dc.getShaderContext().getShaderFactory().getTexLightingShader(dc);
	 }
	 }
	
	 return shader;
	 }
	 */
	@Override
	public void doRepaint(DrawContext dc) {
		this.initializeFrame(dc);
		try {
			MeasureRenderTime.startMesure(dc, "applyView()");
			this.applyView(dc);
			MeasureRenderTime.stopMesure(dc);

			MeasureRenderTime.startMesure(dc, "applySun()");
			this.applySun(dc);
			MeasureRenderTime.stopMesure(dc);

			dc.addPickPointFrustum();

			MeasureRenderTime.startMesure(dc, "createTerrain()");
			this.createTerrain(dc);
			MeasureRenderTime.stopMesure(dc);

//			if(deferredRenderer.isEnabled()){
//				this.occlusionFilterStart(dc);
//			}

			MeasureRenderTime.startMesure(dc, "ShadowMap()");
			this.drawShadowMap(dc);
			MeasureRenderTime.stopMesure(dc);

			MeasureRenderTime.startMesure(dc, "preRender()");
			this.preRender(dc);
			MeasureRenderTime.stopMesure(dc);

//			if(deferredRenderer.isEnabled()){
//				this.occlusionFilerEnd(dc);
//			}

			MeasureRenderTime.startMesure(dc, "picking()");
			this.clearFrame(dc);
			this.pick(dc);
			MeasureRenderTime.stopMesure(dc);

			MeasureRenderTime.startMesure(dc, "draw()");
			this.clearFrame(dc);

			if (deferredRenderer.isEnabled()) {
				MeasureRenderTime.startMesure(dc, "deferredRenderer");
				deferredRenderer.begin(dc);

				//render depth
				GL gl = dc.getGL();
				gl.glColorMask(false, false, false, false);
				for (SectorGeometry sg : dc.getSurfaceGeometry()) {
					sg.render(dc);
				}
				gl.glColorMask(true, true, true, true);

				MeasureRenderTime.startMesure(dc, "normalMap");
				deferredRenderer.renderTerrainNormals(dc);
				MeasureRenderTime.stopMesure(dc);

				//draw all layers
				drawLayers(dc, LayerRenderAttributes.RenderType.GLOBE);

				deferredRenderer.end(dc);
				MeasureRenderTime.stopMesure(dc);

				MeasureRenderTime.startMesure(dc, "globalLighting");
				globalLighting.renderEffects(dc, deferredRenderer);
				MeasureRenderTime.stopMesure(dc);

				drawLayers(dc, LayerRenderAttributes.RenderType.SCREEN);

				drawOrderedRenderables(dc);
				drawDiagnosticDisplay(dc);
			} else {
				draw(dc);
			}
			MeasureRenderTime.stopMesure(dc);
		} finally {
			this.finalizeFrame(dc);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		GL2 gl = GLContext.getCurrent().getGL().getGL2();
		this.deferredRenderer.dispose(gl);
		this.globalLighting.dispose(gl);
		gl.glDeleteQueries(occlusionQueries.length, occlusionQueries, 0);
	}

	@Override
	public void reinitialize() {
		super.reinitialize();
		this.deferredRenderer.dispose(GLContext.getCurrent().getGL().getGL2());
		this.globalLighting.dispose(GLContext.getCurrent().getGL().getGL2());
	}
}
