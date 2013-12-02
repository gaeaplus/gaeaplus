package si.xlab.gaea.core;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Plane;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.terrain.RectangularTessellator;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.FrameBuffer;
import gov.nasa.worldwind.util.Logging;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;
import si.xlab.gaea.core.render.Shadow;

/**
 *
 * @author vito
 */
public class ShadowMap {

	private final int textureWidth;
	private final int textureHeight;

	private Texture texture;
	private ShadowMapRenderer shadowMapRenderer;

	//minX,maxX,minY,maxY,minDistance,maxDistance
	private	double[] frustum = new double[6];

	//model matrix transform
	private Matrix modelViewShadow = Matrix.IDENTITY;

	private Vec4 shadowX;
	private Vec4 shadowY;
	private Vec4 shadowZ;
	private Vec4 shadowPos;

	private boolean shadowTerrain = true;
	
	private final Logger logger = Logging.logger(ShadowMap.class.getName());

	public ShadowMap(int textureWidth, int textureHeight){

		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
		this.texture = null;
		shadowMapRenderer = new ShadowMapRenderer();
	}

	private static MemoryCache getMemoryCache()
    {
        if (!WorldWind.getMemoryCacheSet().containsCache(ShadowMap.class.getName()))
        {
            long size = 3000000L;
            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);
            cache.setName("MinMax elevations");
            WorldWind.getMemoryCacheSet().addCache(ShadowMap.class.getName(), cache);
        }

        return WorldWind.getMemoryCacheSet().getCache(ShadowMap.class.getName());
    }

	public void bindTexture(GL gl){
		if(texture != null){
			texture.bind(gl);
		}
	}

	public void dispose(GL gl){
		this.texture.destroy(gl);
	}

	public void setShadowTerrain(boolean enable){
		this.shadowTerrain = enable;
	}

	public Matrix computeWorldToTextureTransform(){

		if(modelViewShadow == null){
			return null;
		}

		double width = Math.abs(frustum[1] - frustum[0]);
		double height = Math.abs(frustum[3] - frustum[2]);
		double depth = Math.abs(frustum[5] - frustum[4]);

		Vec4 translate = new Vec4(frustum[0], frustum[2], frustum[5]);
		translate = translate.getNegative3();

		Matrix projection = new Matrix(1.0d/width, 0.0d       , 0.0d       , translate.x/width,
									   0.0d      , 1.0d/height, 0.0d       , translate.y/height,
									   0.0d      , 0.0d       , -1.0d/depth, -translate.z/depth,
									   0.0d      , 0.0d       , 0.0d       , 1.0d,
									   false);

		return projection.multiply(modelViewShadow);
	}

	public Matrix computeEyeToTextureTransform(View view){

		if(modelViewShadow == null){
			return null;
		}

		double width = Math.abs(frustum[1] - frustum[0]);
		double height = Math.abs(frustum[3] - frustum[2]);
		double depth = Math.abs(frustum[5] - frustum[4]);

		Vec4 translate = new Vec4(frustum[0], frustum[2], frustum[5]);
		translate = translate.getNegative3();

		Matrix projection = new Matrix(1.0d/width, 0.0d       , 0.0d       , translate.x/width,
									   0.0d      , 1.0d/height, 0.0d       , translate.y/height,
									   0.0d      , 0.0d       , -1.0d/depth, -translate.z/depth,
									   0.0d      , 0.0d       , 0.0d       , 1.0d,
									   false);

		Vec4 eyeZ = view.getForwardVector().getNegative3().normalize3();
		Vec4 eyeY = view.getUpVector().normalize3();
		Vec4 eyeX = eyeY.cross3(eyeZ).normalize3();
		Vec4 eyePos = view.getEyePoint();

		Matrix worldToEyeRotation = new Matrix(eyeX.x, eyeY.x, eyeZ.x, 0.0d,
											    eyeX.y, eyeY.y, eyeZ.y, 0.0d,
											    eyeX.z, eyeY.z, eyeZ.z, 0.0d,
											    0.0d  , 0.0d  , 0.0d  , 1.0d, true);

		worldToEyeRotation = worldToEyeRotation.getInverse();

		Vec4 sInEyeX = shadowX.transformBy4(worldToEyeRotation);
		Vec4 sInEyeY = shadowY.transformBy4(worldToEyeRotation);
		Vec4 sInEyeZ = shadowZ.transformBy4(worldToEyeRotation);
		Vec4 sInEyePos = shadowPos.subtract3(eyePos).transformBy4(worldToEyeRotation);

		Matrix eyeToShadowTransform =  new Matrix(sInEyeX.x, sInEyeY.x, sInEyeZ.x, sInEyePos.x,
											      sInEyeX.y, sInEyeY.y, sInEyeZ.y, sInEyePos.y,
											      sInEyeX.z, sInEyeY.z, sInEyeZ.z, sInEyePos.z,
											      0.0d  , 0.0d  , 0.0d  , 1.0d, true);

		eyeToShadowTransform = eyeToShadowTransform.getInverse();

		return projection.multiply(eyeToShadowTransform);
	}

	public Matrix computeWindowToTextureTransform(View view){

		Matrix m = computeEyeToTextureTransform(view);

		if(m == null || view == null)
			return null;

		Matrix mp = view.getProjectionMatrix().getInverse();

		return m.multiply(mp);
	}

	protected LatLon getNearestLatLonInSector(DrawContext dc, Sector sector)
    {
        Position eyePos = dc.getView().getEyePosition();
        if (sector.contains(eyePos))
        {
            return eyePos;
        }

        LatLon nearestCorner = null;
        Angle nearestDistance = Angle.fromDegrees(Double.MAX_VALUE);
        for (LatLon ll : sector)
        {
            Angle d = LatLon.greatCircleDistance(ll, eyePos);
            if (d.compareTo(nearestDistance) < 0)
            {
                nearestCorner = ll;
                nearestDistance = d;
            }
        }
		return nearestCorner;
    }

	private class MinMaxSectorElevation implements Cacheable{

		double[] minMax;
		Extent extent;

		public MinMaxSectorElevation(DrawContext dc, Sector sector, double[] minMaxElevations){
			minMax = minMaxElevations;

			if(minMax[1] - minMax[0] < 40)
				minMax[1] = minMax[0] + 40.0d;

			extent = Sector.computeBoundingBox(dc.getGlobe(),
											   dc.getVerticalExaggeration(),
											   sector, minMax[0], minMax[1]+10.0d);
		}
		@Override
		public long getSizeInBytes()
		{
			return 80;
		}
		
	}

	private MinMaxSectorElevation getMinMaxElevations(DrawContext dc, Sector sector, int resolution){

		Sector secG = sector;

		MinMaxSectorElevation minMaxElev = (MinMaxSectorElevation)getMemoryCache().getObject(sector);

		if(minMaxElev != null){
			return minMaxElev;
		}

		double[] minMax = new double[2];
		minMax[0] = Double.POSITIVE_INFINITY;
		minMax[1] = Double.NEGATIVE_INFINITY;

		Angle minLat = secG.getMinLatitude();
		Angle minLon = secG.getMinLongitude();

		Angle dLat = secG.getDeltaLat();
		Angle dLon = secG.getDeltaLon();

		double density = 10;
		double stepLat = dLat.degrees / density;
		double stepLon = dLon.degrees / density;

		for(int i = 0; i<density; i++){
			double lat = minLat.degrees + (double)i * stepLat;
			for(int j = 0; j<density; j++){
				double lon = minLon.degrees + (double)j * stepLon;
				double elev = dc.getGlobe().getElevation(Angle.fromDegrees(lat), Angle.fromDegrees(lon));
				if(elev < minMax[0])
					minMax[0] = elev;
				if(elev > minMax[1])
					minMax[1] = elev;
			}
		}

		minMaxElev = new MinMaxSectorElevation(dc, sector, minMax);
		getMemoryCache().add(sector, minMaxElev);
		return minMaxElev;
	}

	private void createTexture(DrawContext dc){

		if(texture != null){
			return;
		}
		
		GL2 gl = dc.getGL().getGL2();

		TextureData td = new TextureData(GLProfile.getDefault(), GL.GL_DEPTH_COMPONENT24, textureWidth, textureHeight, 0,
				GL2.GL_DEPTH_COMPONENT, GL.GL_FLOAT,
				true, false, false, null, null);

		this.texture = TextureIO.newTexture(td);
		this.texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		this.texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		this.texture.setTexParameterfv(gl, GL2.GL_TEXTURE_BORDER_COLOR, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, 0);
		this.texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
		this.texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);

		//shadow parameters
		this.texture.setTexParameteri(gl, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_COMPARE_R_TO_TEXTURE);
		this.texture.setTexParameteri(gl, GL2.GL_TEXTURE_COMPARE_FUNC, GL.GL_LEQUAL);
	}

	public boolean renderShadowMap(DrawContext dc, Vec4 lightDirection){

		if(lightDirection == null){
			logger.warning("sun direction is NULL!");
			return false;
		}

		createTexture(dc);
		shadowMapRenderer.setLightDirection(lightDirection);
		shadowMapRenderer.render(dc);
		//shadowMapCreated = (shadowMapCreated && hasLayerToRender);
		//return shadowMapCreated;
		return true;
	}

	private class ShadowMapRenderer {

		private Vec4 lightDirection;
		private Plane texturePlain;

		ShadowTessellator tesselatorDistance;
		ShadowTessellator tesselatorView;

		FrameBuffer framebuffer = new FrameBuffer();

		boolean isValid = false;

		public ShadowMapRenderer(){
			tesselatorDistance = null;
			tesselatorView = null;
		}

		public void setLightDirection(Vec4 light){
			this.lightDirection = light.getNegative3().normalize3();
		}

		protected boolean setProjection(DrawContext dc)
		{
			if(dc == null){
				isValid = false;
				return false;
			}

			if(dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() == 0){
				isValid = false;
				return false;
			}

			if(dc.getGlobe() == null || dc.getView() == null){
				isValid = false;
				return false;
			}

			GL gl = dc.getGL();

			frustum[0] = Double.POSITIVE_INFINITY;
			frustum[1] = Double.NEGATIVE_INFINITY;
			frustum[2] = Double.POSITIVE_INFINITY;
			frustum[3] = Double.NEGATIVE_INFINITY;
			frustum[4] = Double.POSITIVE_INFINITY;
			frustum[5] = Double.NEGATIVE_INFINITY;

			texturePlain = new Plane(lightDirection.x,
									 lightDirection.y,
									 lightDirection.z, 0.0d);

			ArrayList<Vec4> extremeVertices = new ArrayList<Vec4>();
			Sector shadowSector = null;

			for(SectorGeometry sg : dc.getSurfaceGeometry()){

				MinMaxSectorElevation minMaxExtent = getMinMaxElevations(dc, sg.getSector(), 20);
				Extent extent = minMaxExtent.extent;

				if(!dc.getView().getFrustumInModelCoordinates().intersects(extent)){
					continue;
				}

//				performance test
				LatLon ll = getNearestLatLonInSector(dc, sg.getSector());
//				LatLon ll = sg.getSector().getCentroid();

				Vec4 sv = dc.getGlobe().computePointFromPosition(ll, 0.0d);
				Vec4 ev = dc.getGlobe().computePointFromPosition(dc.getView().getEyePosition(), 0.0d);

				double distance = dc.getView().getEyePosition().elevation
						- dc.getGlobe().getElevation(dc.getView().getEyePosition().latitude, 
									     dc.getView().getEyePosition().longitude);

				distance = distance * 4.0d;
				if(distance < 200.0d)
					distance = 200.0d;

				if(sv.distanceTo3(ev) < distance){

					if(shadowSector == null)
						shadowSector = sg.getSector();

					shadowSector = shadowSector.union(sg.getSector());
					Sector secG = sg.getSector();

					for(LatLon sll : secG.asList()){
						Vec4 minV = dc.getGlobe().computePointFromPosition(sll, dc.getVerticalExaggeration() * minMaxExtent.minMax[0]);
						Vec4 maxV = dc.getGlobe().computePointFromPosition(sll, dc.getVerticalExaggeration() * minMaxExtent.minMax[1]);
						extremeVertices.add(minV);
						extremeVertices.add(maxV);
					}
				}
			}

			if(shadowSector == null){
				logger.warning("Shadow sector is NULL!");
				return false;
			}

			double centerElev = dc.getGlobe().getElevation(shadowSector.getCentroid().latitude,
								       shadowSector.getCentroid().longitude);
			Vec4 center = dc.getGlobe().computePointFromPosition(shadowSector.getCentroid(), centerElev);

			Line centerLine = new Line(center, lightDirection);
			Vec4 centerProjected = texturePlain.intersect(centerLine);

			Vec4 newZ = lightDirection;
			Vec4 newY = centerProjected.normalize3();
			Vec4 newX = newY.cross3(newZ).normalize3();

			shadowX = newX;
			shadowY = newY;
			shadowZ = newZ;
			shadowPos = center;

			modelViewShadow = new Matrix(newX.x, newY.x, newZ.x, center.x,
									 	 newX.y, newY.y, newZ.y, center.y,
										 newX.z, newY.z, newZ.z, center.z,
										 0.0d  , 0.0d  , 0.0d  , 1.0d, true);

			modelViewShadow = modelViewShadow.getInverse();

			for(Vec4 extremeVertex : extremeVertices){

				Vec4 extremeVertexProjected = extremeVertex.transformBy4(modelViewShadow);

				if(extremeVertexProjected.x < frustum[0])
					frustum[0] = extremeVertexProjected.x;
				if(extremeVertexProjected.x > frustum[1])
					frustum[1] = extremeVertexProjected.x;
				if(extremeVertexProjected.y < frustum[2])
					frustum[2] = extremeVertexProjected.y;
				if(extremeVertexProjected.y > frustum[3])
					frustum[3] = extremeVertexProjected.y;
				if(extremeVertexProjected.z < frustum[4])
					frustum[4] = extremeVertexProjected.z;
				if(extremeVertexProjected.z > frustum[5])
					frustum[5] = extremeVertexProjected.z;
			}

			frustum[5] = frustum[5] + 500.0d;

			gl.glViewport(1,1,textureWidth-2, textureHeight-2);

			GL2 gl2 = gl.getGL2();
			gl2.glOrtho(frustum[0], frustum[1],
					   frustum[2], frustum[3],
					   -frustum[5] -30000.0d, -frustum[5]);

			gl.glDepthRange(0.0f, 0.0001);

			return true;
		}

		public void render(DrawContext dc)
		{
			try
			{
				GL2 gl = dc.getGL().getGL2();
				gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
				
				dc.getFramebufferController().push();
				framebuffer.bind(dc);

				if(!framebuffer.hasTextureAttached()){
                    framebuffer.attachTexture2D(dc, GL2.GL_DEPTH_ATTACHMENT, texture.getTextureObject(gl), GL.GL_TEXTURE_2D);
                    framebuffer.attachRenderbuffer(dc, textureWidth, textureHeight, GL.GL_RGBA, GL.GL_COLOR_ATTACHMENT0, 0);
				}

				if(!framebuffer.isComplete(dc, true)){
					return;
				}
						
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPushMatrix();
				gl.glLoadIdentity();
				gl.glMatrixMode(GL2.GL_PROJECTION);
				gl.glPushMatrix();
				gl.glLoadIdentity();

				gl.glEnable(GL.GL_DEPTH_TEST);
				gl.glClear(GL.GL_DEPTH_BUFFER_BIT);

				if(!setProjection(dc)){
					return;
				}
				
				//set modelView matrix to shadowMap coordinate space
				dc.getView().setModelviewMatrixAlt(modelViewShadow, false);
				dc.getView().useModelviewMatrixAlt(true);

				gl.glPolygonOffset(5.0f, 5.0f);
				gl.glEnable(GL.GL_POLYGON_OFFSET_FILL);

				//render terrain
				if(shadowTerrain)
				{
					if(tesselatorDistance == null)
						tesselatorDistance = new ShadowTessellator(false);
					if(tesselatorView == null)
						tesselatorView = new ShadowTessellator(true);

					SectorGeometryList sglD = tesselatorDistance.tessellate(dc);

					sglD.beginRendering(dc);
					for(SectorGeometry sg : sglD)
					{
						sg.beginRendering(dc, 1);
						sg.renderMultiTexture(dc, 1);
						sg.endRendering(dc);
					}
					sglD.endRendering(dc);
				}

				gl.getGL2().glMatrixMode(GL2.GL_PROJECTION);
				gl.getGL2().glLoadIdentity();

				gl.getGL2().glOrtho(frustum[0], frustum[1],
						   frustum[2], frustum[3],
						  -frustum[5], -frustum[4]);

				gl.glDepthRange(0.0001,1.0);
//					gl.glDepthRange(0.0,1.0);
				gl.getGL2().glMatrixMode(GL2.GL_MODELVIEW);

				if(shadowTerrain)
				{
					SectorGeometryList sglV = tesselatorView.tessellate(dc);
					sglV.beginRendering(dc);
					for(SectorGeometry sg : sglV)
					{
						sg.beginRendering(dc, 1);
						sg.renderMultiTexture(dc, 1);
						sg.endRendering(dc);
					}
					sglV.endRendering(dc);
				}

				//render layers that cast shadows
				if (dc.getLayers() != null)
				{
					for (Layer layer : dc.getLayers())
					{
						try
						{
							if (layer != null && layer instanceof Shadow)
							{
								dc.setCurrentLayer(layer);
								Shadow shadowLayer = (Shadow)layer;
								shadowLayer.renderShadow(dc);
							}
						}
						catch (Exception e)
						{
							String message = Logging.getMessage("ShadowMap.ExceptionWhileRenderingShadowLayer",
							(layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
							Logging.logger().log(Level.SEVERE, message, e);
							// Don't abort; continue on to the next layer.
						}
					}

					dc.setCurrentLayer(null);
				}

				// Draw the deferred/ordered renderables.
				dc.setOrderedRenderingMode(true);
				for(OrderedRenderable or : dc.getOrderedRenderables(RenderAttributes.RenderType.SPATIAL)){
					try {
						or.render(dc);
					} catch (Exception e) {
						Logging.logger().log(Level.WARNING,
								Logging.getMessage("BasicSceneController.ExceptionDuringRendering"), e);
					}
				}
				dc.setOrderedRenderingMode(false);
				//

				gl.glDisable(GL.GL_POLYGON_OFFSET_FILL);
				gl.glDisable(GL.GL_DEPTH_TEST);
				gl.glDepthRange(0.0, 1.0);

				//pop modelView matrix
				dc.getView().useModelviewMatrixAlt(false);

				gl.glMatrixMode(GL2.GL_PROJECTION);
				gl.glPopMatrix();
				gl.glMatrixMode(GL2.GL_MODELVIEW);
				gl.glPopMatrix();
				
				dc.getFramebufferController().pop();
				gl.glPopAttrib();
			}
			catch (Throwable e)
			{
				Logging.logger().log(Level.SEVERE, "ShadowMap.ExceptionDuringRendering", e);
			}
		}

	}

	private class ShadowTessellator extends RectangularTessellator{

		private double frustumWidth;
		private Frustum frustumFarShadowInModelCoord;
		private Frustum frustumViewShadowInModelCoord;

		private final boolean tessellateVisibleTiles;

		public ShadowTessellator(boolean tessellateVisibleTiles){
			super();
			this.tessellateVisibleTiles = tessellateVisibleTiles;
		}

		@Override
		public SectorGeometryList tessellate(DrawContext dc)
		{
			if(tessellateVisibleTiles){
				computeViewFrustum();
			}
			else{
				computeFarFrustum();
			}

			return super.tessellate(dc);
		}


		public void computeFarFrustum(){

			frustumWidth = frustum[1] - frustum[0];

			Plane leftPlane = new Plane(1.0, 0.0, 0.0, -frustum[0]);
        	Plane rightPlane = new Plane(-1.0, 0.0, 0.0, frustum[1]);
        	Plane bottomPlane = new Plane(0.0, 1.0, 0.0, -frustum[2]);
        	Plane topPlane = new Plane(0.0, -1.0, 0.0, frustum[3]);
        	Plane nearPlane = new Plane(0.0, 0.0, -1.0, frustum[5] + 30000.0d);
        	Plane farPlane = new Plane(0.0, 0.0, 1.0, -frustum[5]);
//        	Plane farPlane = new Plane(0.0, 0.0, 1.0, 0.0d);

			Frustum f = new Frustum(leftPlane, rightPlane, bottomPlane, topPlane, nearPlane, farPlane);

			frustumFarShadowInModelCoord = f.transformBy(modelViewShadow.getTranspose());
		}

		public void computeViewFrustum(){

			frustumWidth = frustum[1] - frustum[0];

			Plane leftPlane = new Plane(1.0, 0.0, 0.0, -frustum[0]);
        	Plane rightPlane = new Plane(-1.0, 0.0, 0.0, frustum[1]);
        	Plane bottomPlane = new Plane(0.0, 1.0, 0.0, -frustum[2]);
        	Plane topPlane = new Plane(0.0, -1.0, 0.0, frustum[3]);
        	Plane nearPlane = new Plane(0.0, 0.0, -1.0, frustum[5]);
        	Plane farPlane = new Plane(0.0, 0.0, 1.0, -frustum[4]);

			Frustum f = new Frustum(leftPlane, rightPlane, bottomPlane, topPlane, nearPlane, farPlane);

			frustumViewShadowInModelCoord = f.transformBy(modelViewShadow.getTranspose());
		}

		@Override
		protected void selectVisibleTiles(DrawContext dc, RectTile tile)
		{
			Extent extent = getMinMaxElevations(dc, tile.getSector(), 20).extent;
//			Extent extent = tile.getExtent();

			frustumWidth = frustum[1] - frustum[0];

			if (!tessellateVisibleTiles
					&& extent != null
					&& !extent.intersects(frustumFarShadowInModelCoord)){
				return;
			}

			if (tessellateVisibleTiles
					&& extent != null
					&& (!extent.intersects(frustumViewShadowInModelCoord))){
				return;
			}

			if (this.currentLevel < this.maxLevel - 1 && this.needToSplit(dc, tile))
			{
				++this.currentLevel;
				RectTile[] subtiles = this.split(dc, tile);
				for (RectTile child : subtiles)
				{
					this.selectVisibleTiles(dc, child);
				}
				--this.currentLevel;
				return;
			}
			this.currentCoverage = tile.getSector().union(this.currentCoverage);
			this.currentTiles.add(tile);
		}

		@Override
		protected boolean needToSplit(DrawContext dc, RectTile tile)
		{
			Extent extent = getMinMaxElevations(dc, tile.getSector(), 20).extent;
//			Extent extent = tile.getExtent();

			if(tessellateVisibleTiles){
				if(extent.intersects(dc.getView().getFrustumInModelCoordinates())){
					return super.needToSplit(dc, tile);
				}
				else{
					Vec4[] corners = tile.getSector().computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());

					double length = corners[0].subtract3(corners[2]).getLength3();

					if(length > frustumWidth && this.currentLevel < 12){
						return true;
					}
					else{
						return false;
					}
				}
			}
			else{
				Vec4[] corners = tile.getSector().computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());

				double length = corners[0].subtract3(corners[2]).getLength3();

				if(length > frustumWidth && this.currentLevel < 12){
					return true;
				}
				else{
					return false;
				}
			}
		}
	}
}
