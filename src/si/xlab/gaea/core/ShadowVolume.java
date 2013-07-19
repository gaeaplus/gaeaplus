package si.xlab.gaea.core;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Frustum.Corners;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Plane;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.Shader;
import gov.nasa.worldwind.terrain.RectangularTessellator;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.terrain.Tessellator;
import gov.nasa.worldwind.util.Logging;
import java.util.HashSet;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL3;
import javax.media.opengl.GLContext;

/**
 *
 * @author vito
 */
public class ShadowVolume implements Disposable{

	private final Logger logger = Logging.logger(ShadowVolume.class.getName());
	private Tessellator tesselator = new ShadowVolumeTessellator();

	private int[] texture = new int[1];
	private int depthTexture = 0;
	private int width = 0;
	private int height = 0;
	private boolean textureDone = false;

	private int[] fbo = new int[1];

	public void bindTexture(){

		if(GLContext.getCurrent() == null){
			String message = "ShadowVolume.bindTexture: GLContext not current!!";
			logger.severe(message);
			throw new IllegalStateException();
		}

		GL gl = GLContext.getCurrent().getGL();
		gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
	}

	private void init(DrawContext dc, DeferredRendererImpl dr){

		GL gl = dc.getGL();

		if(textureDone
				&& this.depthTexture == dr.getDepthTexture()
				&& width == dr.getDrawableWidth()
				&& height == dr.getDrawableHeight()
				&& gl.glIsFramebuffer(fbo[0])){
			return;
		}

		this.depthTexture = dr.getDepthTexture();

		width = dr.getDrawableWidth();
		height = dr.getDrawableHeight();

		if(gl.glIsTexture(texture[0])){
			gl.glDeleteTextures(1, texture, 0);
		}

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glGenTextures(1, texture, 0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA32F, width, height, 0, GL.GL_RGBA, GL.GL_FLOAT, null);
		textureDone = true;

		if(!gl.glIsFramebuffer(fbo[0])){
			gl.glGenFramebuffers(1, fbo, 0);
		}

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo[0]);
		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT,
					  GL.GL_TEXTURE_2D, depthTexture, 0);
		gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, GL.GL_COLOR_ATTACHMENT0,
					  GL.GL_TEXTURE_2D, texture[0], 0);

		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
	}

	public int render(DrawContext dc, DeferredRendererImpl dr, boolean debugIn)
	{
		boolean debug = debugIn;
//		boolean debug = false;

		init(dc, dr);
		GL gl = dc.getGL();
		GL2 gl2 = dc.getGL().getGL2();
		GL3 gl3 = dc.getGL().getGL3();

		SectorGeometryList sgl;

		if(debug){
			gl.glDisable(GL.GL_DEPTH_TEST);
			sgl = tesselator.tessellate(dc);
			for(SectorGeometry sg : sgl){
				sg.render(dc);
			}
			((ShadowVolumeTessellator)tesselator).renderFrustrum(dc);
			gl.glEnable(GL.GL_DEPTH_TEST);
			return 0;
		}
		else{
			if(dc.isRecordingMode()){
				sgl = tesselator.tessellate(dc);
			}
			else{
				sgl = dc.getSurfaceGeometry();
			}
		}

		if(!debug){
			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbo[0]);
			gl2.glDrawBuffer(GL.GL_COLOR_ATTACHMENT0);

			gl.glClearColor(0, 0, 100000, 100000000);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT);
		}

		Shader shader;
		if(!debug){
			shader = dc.getShaderContext().getShader("ShadowVolume.glsl", "");
		}
		else{
			shader = dc.getShaderContext().getShader("ShadowVolume_1.glsl", "");
		}

		if(!shader.isValid())
			return 0;

		gl3.glBindAttribLocation(shader.getProgram(), 0, "in_vertex");

		shader.enable(dc.getShaderContext());


		shader.setParam("u_light", dc.getSunlightDirection());
		shader.setParam("u_zNear", new float[]{(float)(dc.getView().getNearClipDistance())});
		shader.setParam("u_zFar", new float[]{(float)dc.getView().getFarClipDistance()});

		if(!debug){
//			gl.glDisable(GL.GL_DEPTH_TEST);
			gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
			gl.glDepthMask(false);
			gl.glEnable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
			gl.glBlendEquationSeparate(GL.GL_FUNC_ADD, GL2.GL_MIN);
		}
		else{
//			gl.glEnable(GL.GL_CULL_FACE);
//			gl.glCullFace(GL.GL_BACK);
			gl.glDisable(GL.GL_BLEND);
			shader.setParam("u_cut", new float[]{1.0f});
			shader.setParam("u_color", new Vec4(0.0, 0.0, 0.0));
			gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
		}

		sgl.beginRendering(dc);
		for(SectorGeometry sg : sgl){
			if(!isSectorInShadow(dc, sg.getSector())){
				sg.renderAdjacency(dc,1, false);
			}
		}
		sgl.endRendering(dc);
//		renderTest(dc);

		if(debug){
//			gl.glCullFace(GL.GL_FRONT);
//			shader.setParam("u_color", new Vec4(0.6, 0.0, 0.0));
//			for(SectorGeometry sg : dc.getSurfaceGeometry()){
//				sg.renderAdjacency(dc,1);
//			}
//			gl.glDisable(GL.GL_CULL_FACE);
	
			gl.glEnable(GL2.GL_POLYGON_OFFSET_LINE);
			gl.glPolygonOffset(-1.0f, -1.0f);
			gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_LINE);
			shader.setParam("u_color", new Vec4(1.0, 0.0, 0.0));

			for(SectorGeometry sg : sgl){
				sg.renderAdjacency(dc,1, false);
			}
	//		renderTest(dc);

			gl.glDisable(GL2.GL_POLYGON_OFFSET_LINE);
			gl2.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2.GL_FILL);
		}
		else{
			gl.glDepthMask(true);
			gl.glDisable(GL.GL_BLEND);
			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glBlendEquationSeparate(GL.GL_FUNC_ADD, GL.GL_FUNC_ADD);
//			gl.glEnable(GL.GL_DEPTH_TEST);
		}

		shader.disable(dc.getShaderContext());

		if(!debug){
			gl.glClearColor(0, 0, 0, 0);
			gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
		}

		return texture[0];
	}

	public void dispose()
	{
		if(GLContext.getCurrent() == null){
			String msg = "GLContext not current!";
			logger.severe(msg);
			return;
		}

		GL gl = GLContext.getCurrent().getGL();

		if(gl.glIsTexture(texture[0])){
			gl.glDeleteTextures(1, texture, 0);
		}
		textureDone = false;
	}

	float[] vArray = null;
	java.nio.FloatBuffer bufferVer = null;
	java.nio.IntBuffer buffer = null;

	private boolean isSectorInShadow(DrawContext dc, Sector sector){
		float is = 1;
		Vec4[] corners = sector.computeCornerPoints(dc.getGlobe(), dc.getVerticalExaggeration());	
		for(int i = 0; i<corners.length; i++){
			is *= Math.max(dc.getSunlightDirection().normalize3().dot3(corners[i].normalize3()), 0.0d);
		}

		return (is == 0 ? false : true);
	}

	private void renderTest(DrawContext dc){
		GL gl = dc.getGL();
		GL2 gl2 = dc.getGL().getGL2();
		GL3 gl3 = dc.getGL().getGL3();

		dc.getView().pushReferenceCenter(dc, Vec4.ZERO);

		if(vArray == null){
			vArray = new float[16 * 3];
			float distance = 1000000;

			int counter = 0;
			for(int i=0; i<4; i++){
				for(int j=0;j<4; j++){
					vArray[counter] = i * distance;
					vArray[counter + 1] = j * distance;
					vArray[counter + 2] = 0;
					counter += 3;
				}
			}

			bufferVer = Buffers.newDirectFloatBuffer(16 *3);
			for(float n : vArray){
				bufferVer.put(n);
			}
			bufferVer.rewind();
			processIndicesAdjacency(dc, 1);
		}

		gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl2.glVertexPointer(3, GL.GL_FLOAT, 0, bufferVer.rewind());

		gl3.glDrawElements(GL3.GL_TRIANGLES_ADJACENCY, buffer.limit(), GL.GL_UNSIGNED_INT, buffer.rewind());

		gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);

		dc.getView().popReferenceCenter(dc);
	}

	protected void processIndicesAdjacency(DrawContext dc, int density)
    {
        if (density < 1)
            density = 1;

        int sideSize = density + 2;

        int indexCount = sideSize * sideSize * 2 * 6;
        buffer = Buffers.newDirectIntBuffer(indexCount);
        int k = 0;
        for (int i = 0; i < sideSize; i++)
        {
			for(int j = 0; j < sideSize; j++)
        	{
				int x = (i * (sideSize + 1)) + j;
				int xU = x + sideSize + 1;
				int xR = x + 1;

				int xL = ((j - 1) > -1) ? (xU - 1) : xU;

				int xRR = ((j + 2) < sideSize + 1) ? (x + 2) : xR;
				int xD = ((i - 1) > -1) ? (x - sideSize) : xR;
				int xUU = ((i + 2) < sideSize + 1) ? (xU + sideSize + 1) : xU;

				int xUR = xU + 1;

				buffer.put(x);
//				System.out.println(x);
				buffer.put(xD);
//				System.out.println(xD);
				buffer.put(xR);
//				System.out.println(xR);
				buffer.put(xUR);
//				System.out.println(xUR);
				buffer.put(xU);
//				System.out.println(xU);
				buffer.put(xL);
//				System.out.println(xL);

				buffer.put(xR);
//				System.out.println(xR);
				buffer.put(xRR);
//				System.out.println(xRR);
				buffer.put(xUR);
//				System.out.println(xUR);
				buffer.put(xUU);
//				System.out.println(xUU);
				buffer.put(xU);
//				System.out.println(xU);
				buffer.put(x);
//				System.out.println(x);
        	}
        }

        buffer.rewind();
    }

	private class ShadowVolumeTessellator extends RectangularTessellator{

		Frustum sunFrustrum;

		public ShadowVolumeTessellator()
		{
			super();
			this.setMakeTileSkirts(false);
		}

		@Override
		public SectorGeometryList tessellate(DrawContext dc)
		{
			computeSunFrustum(dc);
			return super.tessellate(dc);
		}


		public void computeSunFrustum(DrawContext dc){

//			Sector sector = new Sector(Angle.fromDegrees(45.4), Angle.fromDegrees(45.6), Angle.fromDegrees(14.4), Angle.fromDegrees(14.6));

			Vec4 dirSun = dc.getSunlightDirection().normalize3();
			Line[] tempLine = new Line[3];
			double[] tempDist = new double[3];
			Plane sunPlane = new Plane(dirSun.x, dirSun.y, dirSun.z, 0.0);

			Vec4 center = dc.getGlobe().computePointFromPosition(dc.getVisibleSector().getCentroid(), 0.0d);
			Line centerLine = new Line(center, dirSun);
			Vec4 centerProjected = sunPlane.intersect(centerLine);
			Vec4 newZ = dc.getSunlightDirection().normalize3();
			Vec4 newY = centerProjected.normalize3();
			Vec4 newX = newY.cross3(newZ).normalize3();

			HashSet<Vec4> corners = new HashSet<Vec4>();
			for(SectorGeometry sg : dc.getSurfaceGeometry()){
				Vec4 eC = sg.getExtent().getCenter();
				double r = sg.getExtent().getRadius();
				corners.add(eC.add3(newZ.multiply3(r)));
				corners.add(eC.add3(newZ.multiply3(-r)));
				corners.add(eC.add3(newX.multiply3(r)));
				corners.add(eC.add3(newX.multiply3(-r)));
				corners.add(eC.add3(newY.multiply3(r)));
				corners.add(eC.add3(newY.multiply3(-r)));
			}

			Plane[] tempPlane = new Plane[]{sunPlane,
											new Plane(newY.x, newY.y, newY.z, 0.0d),
											new Plane(newX.x, newX.y, newX.z, 0.0d)};

			double farD = Double.NEGATIVE_INFINITY;
			double nearD = Double.POSITIVE_INFINITY;

			double topD = Double.NEGATIVE_INFINITY;
			double bottomD = Double.POSITIVE_INFINITY;

			double leftD = Double.NEGATIVE_INFINITY;
			double rightD = Double.POSITIVE_INFINITY;

			for(Vec4 corner : corners){
				tempLine[0] = new Line(corner, newZ);
				tempLine[1] = new Line(corner, newY);
				tempLine[2] = new Line(corner, newX);

				tempDist[0] = tempPlane[0].intersectDistance(tempLine[0]);
				tempDist[1] = tempPlane[1].intersectDistance(tempLine[1]);
				tempDist[2] = tempPlane[2].intersectDistance(tempLine[2]);

				farD = tempDist[0] > farD ? tempDist[0] : farD;
				nearD = tempDist[0] < nearD ? tempDist[0] : nearD;
				
				topD = tempDist[1] > topD ? tempDist[1] : topD;
				bottomD = tempDist[1] < bottomD ? tempDist[1] : bottomD;

				leftD = tempDist[2] > leftD ? tempDist[2] : leftD;
				rightD = tempDist[2] < rightD ? tempDist[2] : rightD;
			}

			Plane leftPlane = new Plane(newX.x, newX.y, newX.z, -rightD);
        	Plane rightPlane = new Plane(-newX.x, -newX.y, -newX.z, leftD);

			Plane bottomPlane = new Plane(-newY.x, -newY.y, -newY.z, -bottomD);
        	Plane topPlane = new Plane(newY.x, newY.y, newY.z, topD);

			Plane nearPlane = new Plane(-newZ.x, -newZ.y, -newZ.z, -nearD);
        	Plane farPlane = new Plane(newZ.x, newZ.y, newZ.z, farD + 100000.0d);

			this.sunFrustrum = new Frustum(leftPlane, rightPlane, bottomPlane, topPlane, nearPlane, farPlane);
		}

		public void renderFrustrum(DrawContext dc){
			Corners c = this.sunFrustrum.getCorners();

			GL2 gl = dc.getGL().getGL2();
			gl.glColor3f(1, 0, 0);
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d(c.nbl.x, c.nbl.y, c.nbl.z);
			gl.glVertex3d(c.nbr.x, c.nbr.y, c.nbr.z);
			gl.glVertex3d(c.ntr.x, c.ntr.y, c.ntr.z);
			gl.glVertex3d(c.ntl.x, c.ntl.y, c.ntl.z);
			gl.glVertex3d(c.nbl.x, c.nbl.y, c.nbl.z);
			gl.glEnd();

			gl.glColor3f(0, 1, 0);
			gl.glBegin(GL.GL_LINE_STRIP);
			gl.glVertex3d(c.fbl.x, c.fbl.y, c.fbl.z);
			gl.glVertex3d(c.fbr.x, c.fbr.y, c.fbr.z);
			gl.glVertex3d(c.ftr.x, c.ftr.y, c.ftr.z);
			gl.glVertex3d(c.ftl.x, c.ftl.y, c.ftl.z);
			gl.glVertex3d(c.fbl.x, c.fbl.y, c.fbl.z);
			gl.glEnd();
			gl.glColor3f(0, 0, 1);
			gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(c.fbl.x, c.fbl.y, c.fbl.z);
			gl.glVertex3d(c.nbl.x, c.nbl.y, c.nbl.z);
			gl.glColor3f(0.8f, 0.8f, 1.0f);
			gl.glVertex3d(c.ftr.x, c.ftr.y, c.ftr.z);
			gl.glVertex3d(c.ntr.x, c.ntr.y, c.ntr.z);
			gl.glVertex3d(c.ftl.x, c.ftl.y, c.ftl.z);
			gl.glVertex3d(c.ntl.x, c.ntl.y, c.ntl.z);
			gl.glColor3f(0, 0, 1);
			gl.glVertex3d(c.fbr.x, c.fbr.y, c.fbr.z);
			gl.glVertex3d(c.nbr.x, c.nbr.y, c.nbr.z);
			gl.glEnd();

			Vec4 fc = c.fbl.add3(c.ftr).divide3(2.0);
			Vec4 nc = c.nbl.add3(c.ntr).divide3(2.0);
			Vec4 tc = c.ftl.add3(c.ntr).divide3(2.0);
			Vec4 bc = c.fbl.add3(c.nbr).divide3(2.0);
			Vec4 lc = c.fbl.add3(c.ntl).divide3(2.0);
			Vec4 rc = c.fbr.add3(c.ntr).divide3(2.0);

			double length = 100000;
			Vec4 fcN = fc.add3(this.sunFrustrum.getFar().getNormal().multiply3(length));
			Vec4 ncN = nc.add3(this.sunFrustrum.getNear().getNormal().multiply3(length));
			Vec4 tcN = tc.add3(this.sunFrustrum.getTop().getNormal().multiply3(length));
			Vec4 bcN = bc.add3(this.sunFrustrum.getBottom().getNormal().multiply3(length));
			Vec4 lcN = lc.add3(this.sunFrustrum.getLeft().getNormal().multiply3(length));
			Vec4 rcN = rc.add3(this.sunFrustrum.getRight().getNormal().multiply3(length));
			gl.glColor3f(0.5f, 0.5f, 0.5f);
			gl.glBegin(GL.GL_LINES);
			gl.glVertex3d(fc.x, fc.y, fc.z);
			gl.glVertex3d(fcN.x, fcN.y, fcN.z);

			gl.glVertex3d(nc.x, nc.y, nc.z);
			gl.glVertex3d(ncN.x, ncN.y, ncN.z);

			gl.glVertex3d(tc.x, tc.y, tc.z);
			gl.glVertex3d(tcN.x, tcN.y, tcN.z);

			gl.glVertex3d(bc.x, bc.y, bc.z);
			gl.glVertex3d(bcN.x, bcN.y, bcN.z);

			gl.glVertex3d(lc.x, lc.y, lc.z);
			gl.glVertex3d(lcN.x, lcN.y, lcN.z);

			gl.glVertex3d(rc.x, rc.y, rc.z);
			gl.glVertex3d(rcN.x, rcN.y, rcN.z);
			gl.glEnd();
			gl.glColor3f(1, 1, 1);
		}

		@Override
		protected void selectVisibleTiles(DrawContext dc, RectTile tile)
		{
//			Extent extent = tile.getExtent();
//			if (extent != null && !extent.intersects(this.sunFrustrum))
//				return;

			if (this.currentLevel < this.maxLevel - 1 && !this.atBestResolution(dc, tile) && this.needToSplit(dc, tile))
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
	}
}
