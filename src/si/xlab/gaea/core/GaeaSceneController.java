package si.xlab.gaea.core;

import gov.nasa.worldwind.AbstractSceneController;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Quaternion;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderAttributes;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.ShaderFactory;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.MeasureRenderTime;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import si.xlab.gaea.avlist.AvKeyExt;
import si.xlab.gaea.core.shaders.data.GaeaShaderFactoryGLSL;

/**
 *
 * @author vito
 */
public class GaeaSceneController extends AbstractSceneController {

	private boolean shadowsEnabled = false;
	private boolean atmosphereEnabled = false;
	private boolean aerialPerspectiveEnabled = false;
	private boolean posEffectsEnabled = false;
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
		this.posEffectsEnabled = false;
		this.isRecordingMode = false;

		this.addPropertyChangeListener(new PropertyListener());
	}

	private class PropertyListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_SUNLIGHT)) {
				GaeaSceneController.this.isSunLightEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_SHADOWS)) {
				GaeaSceneController.this.shadowsEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_SHADOWS_ON_CAMERA_STOP)) {
				GaeaSceneController.this.drawShadowsWhenCameraStop = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_ATMOSPHERE)) {
				GaeaSceneController.this.atmosphereEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_ATMOSPHERE_WITH_AERIAL_PERSPECTIVE)) {
				GaeaSceneController.this.aerialPerspectiveEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_POS_EFFECTS)) {
				GaeaSceneController.this.posEffectsEnabled = (Boolean) propertyChangeEvent.getNewValue();
			}
			if (propertyChangeEvent.getPropertyName().equals(AvKeyExt.ENABLE_RECORDING_MODE)) {
				GaeaSceneController.this.isRecordingMode = (Boolean) propertyChangeEvent.getNewValue();
			}
		}
	}

	protected void applySun(DrawContext dc) {
		Vec4 zenithPosition = dc.getGlobe().getZenithPosition();
		Vec4 sunlightDirection = null;
		
		if(zenithPosition != null){
			sunlightDirection = dc.getGlobe().getZenithPosition().normalize3().getNegative3();
		}
		else{
			Vec4 eyePoint = dc.getView().getCurrentEyePoint();
			sunlightDirection = eyePoint.perpendicularTo3(Vec4.UNIT_Y)
							.transformBy3(Quaternion.fromAxisAngle(Angle.fromDegrees(40.0), Vec4.UNIT_Y))
							.normalize3()
							.getNegative3();
		}

		if(sunlightDirection != null){
			//animate
			Vec4 delta = sunlightDirection.subtract3(dc.getSunlightDirection());
			dc.setSunlightDirection(dc.getSunlightDirection().add3(delta.multiply3(0.06)));
			if(delta.getLength3() > 0.1){
				this.firePropertyChange(AVKey.REPAINT, null, null);
			}
		}
		
		if(isSunLightEnabled && !dc.getDeferredRenderer().isSupported(dc)){
			logger.severe("Disabling effects. DeferredRenderer not supported!");
			atmosphereEnabled = false;
			aerialPerspectiveEnabled = false;
			posEffectsEnabled = false;
			isSunLightEnabled = false;
		}

		dc.setAtmosphereEnabled(atmosphereEnabled);
		dc.setAerialPerspectiveEnabled(aerialPerspectiveEnabled);
		dc.setPosEffectsEnabled(posEffectsEnabled);
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
				|| dc.getSunlightDirection() == null) {

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

		this.drawShadows = true;
		dc.setShadowsEnabled(drawShadows);

		if (!drawShadows) {
			return;
		}

		//draw shadow map//////////////////////////////////////////
		dc.enableShadowMode();
		if(!ShadowMapFactory.getWorldShadowMapInstance(dc).renderShadowMap(dc, dc.getSunlightDirection().normalize3())){
			this.drawShadows = false;
			dc.setShadowsEnabled(false);
		}
		dc.disableShadowMode();
		///////////////////////////////////////////////////////////
	}

	//occlusion filter test
	/*
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
	*/

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
				MeasureRenderTime.startMeasure(dc, layer.getName());
				deferredRenderer.renderLayer(dc, layer);
				MeasureRenderTime.stopMeasure(dc);
				return;
			}

			if (layer != null && layer.isEnabled()) {
				MeasureRenderTime.startMeasure(dc, layer.getName());
				dc.setCurrentLayer(layer);
				layer.render(dc);
				MeasureRenderTime.stopMeasure(dc);
			}
		} catch (Exception e) {
			String message = Logging.getMessage("SceneController.ExceptionWhileRenderingLayer",
					(layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
			Logging.logger().log(Level.SEVERE, message, e);
			// Don't abort; continue on to the next layer.
		}
	}

	@Override
	public void doRepaint(DrawContext dc) {
		this.initializeFrame(dc);
		try {
			MeasureRenderTime.startMeasure(dc, "applyView()");
			this.applyView(dc);
			MeasureRenderTime.stopMeasure(dc);

			MeasureRenderTime.startMeasure(dc, "applySun()");
			this.applySun(dc);
			MeasureRenderTime.stopMeasure(dc);

			dc.addPickPointFrustum();

			MeasureRenderTime.startMeasure(dc, "createTerrain()");
			this.createTerrain(dc);
			MeasureRenderTime.stopMeasure(dc);

			//if(deferredRenderer.isEnabled()){
			//	this.occlusionFilterStart(dc);
			//}

			MeasureRenderTime.startMeasure(dc, "preRender()");
			this.preRender(dc);
			MeasureRenderTime.stopMeasure(dc);

			MeasureRenderTime.startMeasure(dc, "ShadowMap()");
			this.drawShadowMap(dc);
			MeasureRenderTime.stopMeasure(dc);

			//if(deferredRenderer.isEnabled()){
			//	this.occlusionFilerEnd(dc);
			//}

			MeasureRenderTime.startMeasure(dc, "picking()");
			this.clearFrame(dc);
			this.pick(dc);
			MeasureRenderTime.stopMeasure(dc);

			MeasureRenderTime.startMeasure(dc, "draw()");
			this.clearFrame(dc);

			if (deferredRenderer.isEnabled()) {
				MeasureRenderTime.startMeasure(dc, "deferredRenderer");
				deferredRenderer.begin(dc);

				MeasureRenderTime.startMeasure(dc, "normalMap");
				deferredRenderer.renderTerrainNormals(dc);
				MeasureRenderTime.stopMeasure(dc);

				//draw all layers
				drawLayers(dc, RenderAttributes.RenderType.SPATIAL);
				drawLayers(dc, RenderAttributes.RenderType.GLOBE);

				drawOrderedSurfaceRenderables(dc);
				drawOrderedRenderables(dc, RenderAttributes.RenderType.SPATIAL);
				drawOrderedRenderables(dc, RenderAttributes.RenderType.GLOBE);

				deferredRenderer.end(dc);
				MeasureRenderTime.stopMeasure(dc);

				MeasureRenderTime.startMeasure(dc, "globalLighting");
				globalLighting.renderEffects(dc, deferredRenderer);
				MeasureRenderTime.stopMeasure(dc);

				drawLayers(dc, RenderAttributes.RenderType.SCREEN);

				drawScreenCreditController(dc);
				drawOrderedRenderables(dc, RenderAttributes.RenderType.SCREEN);
				drawDiagnosticDisplay(dc);
			} else {
				draw(dc);
			}
			MeasureRenderTime.stopMeasure(dc);
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
