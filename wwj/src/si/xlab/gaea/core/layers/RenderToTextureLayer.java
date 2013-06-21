package si.xlab.gaea.core.layers;

import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.FrameBuffer;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.MeasureRenderTime;
import gov.nasa.worldwind.util.PerformanceStatistic;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceObjectRenderer;

/**
 *
 * @author vito
 */
public class RenderToTextureLayer extends TiledImageLayer
{
	private static RenderToTextureLayer Instance;

	public static final int TEXTURE_WIDTH = 512;
	public static final int TEXTURE_HEIGHT = 512;

	private RenderToTexture renderer = null;

	private List<SurfaceObjectRenderer> surfaceLayers = new ArrayList<SurfaceObjectRenderer>();

	private final Object reloadLock = new Object();
	private final HashSet<ReloadRequest> reloadRequests = new HashSet<ReloadRequest>();
	private int reloadAll = 0;
	
	private HashMap<Sector, TextureTileRTT> currentTiles = new HashMap<Sector, TextureTileRTT>();
	private HashSet<Sector> disposeTiles = new HashSet<Sector>();

	//private final Object lock = new Object();

	private static class ReloadRequest{
		public final Sector sector;
		public int reload = 2;

		public ReloadRequest(Sector sector) {
			this.sector = sector;
		}
	}

	public RenderToTextureLayer(LevelSet levelSet)
	{
		super(levelSet);
	}

	public RenderToTextureLayer(AVList params)
	{
		this(new LevelSet(params));
	}

	public RenderToTextureLayer()
	{
		this(makeParam());
	}

	public static RenderToTextureLayer getInstance()
	{
		if (Instance == null)
		{
			Instance = new RenderToTextureLayer();
			Instance.setName("RenderToTexture");
			Instance.setUseTransparentTextures(true);
			Instance.setNetworkRetrievalEnabled(false);
			Instance.setForceLevelZeroLoads(false);
			Instance.setPickEnabled(false);
    		Instance.setMaxActiveAltitude(Double.MAX_VALUE);
        	Instance.setMinActiveAltitude(0.0d);
            Instance.setUseMipMaps(false);
            Instance.setEnabled(true);      
		}
		return Instance;
	}

	private static AVList makeParam()
	{
		AVList params = new AVListImpl();

		String s = "RenderToTextureLayer";
		params.setValue(AVKey.DATA_CACHE_NAME, s);

		s = "RenderToTextureLayer";
		params.setValue(AVKey.DATASET_NAME, s);

		s = TextureIO.DDS;
		params.setValue(AVKey.FORMAT_SUFFIX, s);

		Integer i = 0;
		params.setValue(AVKey.NUM_EMPTY_LEVELS, i);

		i = 17;
		params.setValue(AVKey.NUM_LEVELS, i);

		i = 512;
		params.setValue(AVKey.TILE_WIDTH, i);

		i = 512;
		params.setValue(AVKey.TILE_HEIGHT, i);

		LatLon ll = new LatLon(Angle.fromDegrees(36.0d), Angle.fromDegrees(36.0d));
		params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, ll);

		Sector sector = new Sector(Angle.NEG90, Angle.POS90,
				Angle.NEG180, Angle.POS180);
		params.setValue(AVKey.SECTOR, sector);

		return params;
	}

	public void addSurfaceLayer(SurfaceObjectRenderer surfaceLayer){
		if(surfaceLayer == null){
			return;
		}
		//if(!surfaceLayers.contains(surfaceLayer)){
			//synchronized(lock){
				surfaceLayers.add(surfaceLayer);
			//}
		//}
	}

	/*
	public void removeSurfaceLayer(SurfaceLayer surfaceLayer){
		if(surfaceLayer == null){
			return;
		}
		//if(surfaceLayers.contains(surfaceLayer)){
		//	synchronized(lock){
				surfaceLayers.remove(surfaceLayer);
		//	}
		//}
	}
	*/

	@Override
	protected void forceTextureLoad(TextureTile tile){
	}

	@Override
	public void requestTexture(DrawContext dc, TextureTile tile){
	}

	@Override
	public void dispose()
	{
		super.dispose();
	}

	public void dispose(DrawContext dc){
		for(TextureTileRTT tile : currentTiles.values()){
			tile.dispose(dc);
		}
	}

	public void reloadAll(){
		synchronized(reloadLock){
			reloadAll += 3;
			reloadAll = reloadAll > 3 ? 3 : reloadAll;
			firePropertyChange(AVKey.LAYER, null, this);
		}
	}

	public void reload(Sector sector){
		synchronized(reloadLock){
			this.reloadRequests.add(new ReloadRequest(sector));
			firePropertyChange(AVKey.LAYER, null, this);
		}
	}

	@Override
	protected void addTile(DrawContext dc, TextureTile tile){
		TextureTileRTT tt = (TextureTileRTT)tile;
		if(currentTiles.containsKey(tt.getSector())){
			currentTiles.get(tt.getSector()).setVisible(true);
		}
		else{
			tt.setVisible(true);
			currentTiles.put(tt.getSector(), tt);
		}
	}

	@Override
	public void doPreRender(DrawContext dc)
	{
		if(surfaceLayers.isEmpty()){
			return;
		}

		//reloading
		synchronized (reloadLock) {
			if (!reloadRequests.isEmpty()) {
				for (ReloadRequest rr : (HashSet<ReloadRequest>) reloadRequests.clone()) {
					rr.reload--;
					if (rr.reload == 0) {
						reloadRequests.remove(rr);
					}
				}
				firePropertyChange(AVKey.LAYER, null, this);
			}
		}
		//

		for(TextureTileRTT tile : currentTiles.values()){
			tile.setVisible(false);
		}
		
		//tile assambly
		if (this.forceLevelZeroLoads && !this.levelZeroLoaded){
            this.loadAllTopLevelTextures(dc);
		}
        this.assembleTiles(dc); // Determine the tiles to draw.

		//clear unused tiles
		boolean disposeOldestTile = true;
		TextureTileRTT dirtyTile = null;
		for(TextureTileRTT tile : currentTiles.values()){

			if (!tile.isVisible) {
				disposeTiles.add(tile.getSector());
				tile.dispose(dc);
				//disposeOldestTile = false;
				continue;
			}

			//handle reload requests
			synchronized (reloadLock) {
				for (ReloadRequest rr : reloadRequests) {
					if (rr.reload == 1 && tile.getSector().intersects(rr.sector)) {
						tile.dispose(dc);
						//disposeOldestTile = false;
						continue;
					}
				}
			}
			//

			if (dirtyTile == null) {
				dirtyTile = tile;
			} else if (tile.time < dirtyTile.time) {
				dirtyTile = tile;
			}
		}
		
		if(disposeOldestTile && dirtyTile != null){
			dirtyTile.dispose(dc);
		}

		for(Sector s : disposeTiles){
			currentTiles.remove(s);
		}
		disposeTiles.clear();

		synchronized (reloadLock) {
			if (reloadAll > 0) {
				if (reloadAll == 1) {
					dispose(dc);
				}
				firePropertyChange(AVKey.LAYER, null, this);
				reloadAll--;
			}
		}
		
		renderTextures(dc);
	}

	protected void renderTextures(DrawContext dc)
	{
		GL2 gl = dc.getGL().getGL2();

		//init texture renderer
		if(this.renderer == null){
			renderer = new RenderToTexture(dc);
		}

		gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT // for alpha func
				| GL2.GL_ENABLE_BIT
				| GL2.GL_VIEWPORT_BIT
				| GL2.GL_CURRENT_BIT
				| GL2.GL_DEPTH_BUFFER_BIT // for depth func
				| GL2.GL_TEXTURE_BIT // for texture env
				| GL2.GL_TRANSFORM_BIT
				| GL2.GL_HINT_BIT);

		gl.glLineWidth(2.0f);
		gl.glDisable(GL2.GL_POLYGON_SMOOTH);
		gl.glDisable(GL2.GL_LINE_SMOOTH);
		gl.glEnable(GL2.GL_POINT_SMOOTH);
		gl.glHint(GL2.GL_POINT_SMOOTH_HINT, GL2.GL_FASTEST);

		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFuncSeparate(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA,
							   GL2.GL_ONE, GL2.GL_ONE_MINUS_SRC_ALPHA);

		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glViewport(0,0, TEXTURE_WIDTH, TEXTURE_HEIGHT);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(-TEXTURE_WIDTH/2, TEXTURE_WIDTH/2,
				   -TEXTURE_HEIGHT/2, TEXTURE_HEIGHT/2,
				   -20000.0f, 20000.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		
		//render all current textures
		int numTexturesRendered = 0;
		dc.getShaderContext().pushShader();
		for(TextureTileRTT tile : currentTiles.values()){
			if(tile.texID <= 0){
				//tile.setAnimate(false);
				renderer.render(dc, tile);
				numTexturesRendered += 1;
			}
		}
		dc.getShaderContext().popShader();

		gl.glPopMatrix();
		
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		gl.glLineWidth(1.0f);

		gl.glDisable(GL2.GL_BLEND);
		gl.glPopAttrib();

		//synchronized(lock){
			for(SurfaceObjectRenderer sl : surfaceLayers){
				sl.flush();
			}
			surfaceLayers.clear();
		//}
		
		dc.setPerFrameStatistic("RenderToTexture", "RenderToTexture:texturesRendered: ", numTexturesRendered);
	}

	@Override
	public void render(DrawContext dc)
	{
        if (!this.isEnabled())
            return; // Don't check for arg errors if we're disabled

        if (null == dc)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getGlobe())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoGlobeSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (null == dc.getView())
        {
            String message = Logging.getMessage("layers.AbstractLayer.NoViewSpecifiedInDrawingContext");
            Logging.logger().severe(message);
            throw new IllegalStateException(message);
        }

        if (!this.isLayerActive(dc))
            return;

        if (!this.isLayerInView(dc))
            return;
		
		if (dc.getSurfaceGeometry() == null || dc.getSurfaceGeometry().size() < 1)
            return;

        dc.getGeographicSurfaceTileRenderer().setShowImageTileOutlines(this.isDrawTileBoundaries());
		
		if (dc.isPickingMode())
			return;

		if(surfaceLayers.isEmpty()){
			return;
		}
		
		if (this.currentTiles.size() >= 1)
        {
            if (this.getScreenCredit() != null)
            {
                dc.addScreenCredit(this.getScreenCredit());
            }

            GL2 gl = dc.getGL().getGL2();

            if (this.isUseTransparentTextures() || this.getOpacity() < 1)
            {
                gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT | GL2.GL_CURRENT_BIT);
                this.setBlendingFunction(dc);
            }
            else
            {
                gl.glPushAttrib(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_POLYGON_BIT);
            }

            gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);

            dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, this.getName() + " Tiles",
                this.currentTiles.size());
			
			//mesure render time
			MeasureRenderTime.startMesure(dc, "renderTiles");
            dc.getGeographicSurfaceTileRenderer().renderTiles(dc, this.currentTiles.values());
			MeasureRenderTime.stopMesure(dc);
			
            gl.glPopAttrib();
        }
	}
	
	@Override
	protected TextureTile createTile(Sector sector, Level level, int row, int col){
		TextureTileRTT tile = new TextureTileRTT(sector, level, row, col);
		return tile;
	}

	private class TextureTileRTT extends TextureTile{

		private int texID = 0;
		private boolean isVisible = true;
		private long time = 0;
		
		public TextureTileRTT(Sector sector){
			super(sector);
		}

		public TextureTileRTT(Sector sector, Level level, int row, int col){
			super(sector, level, row, col);
		}

		public void setTexture(int id){
			texID = id;
			time = System.currentTimeMillis();
		}

		public void setVisible(boolean set){
			isVisible = set;
		}

		public boolean isVisible(){
			return isVisible;
		}

		@Override
		public boolean bind(DrawContext dc){
			GL gl = dc.getGL();
			
			if(texID > 0){
				gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
				return true;
			}
			return false;
		}

		public void dispose(DrawContext dc){
			GL gl = dc.getGL();
			if(texID != 0){
				gl.glDeleteTextures(1, new int[]{texID}, 0);
				texID = 0;
			}
			isVisible = false;
		}

		@Override
		public TextureTile[] createSubTiles(Level nextLevel){
			if(nextLevel == null){
				String msg = Logging.getMessage("nullValue.LevelIsNull");
				Logging.logger().severe(msg);
				throw new IllegalArgumentException(msg);
			}
			Angle p0 = this.getSector().getMinLatitude();
			Angle p2 = this.getSector().getMaxLatitude();
			Angle p1 = Angle.midAngle(p0, p2);

			Angle t0 = this.getSector().getMinLongitude();
			Angle t2 = this.getSector().getMaxLongitude();
			Angle t1 = Angle.midAngle(t0, t2);

			int row = this.getRow();
			int col = this.getColumn();

			TextureTile[] subTiles = new TextureTile[4];

			subTiles[0] = new TextureTileRTT(new Sector(p0, p1, t0, t1), nextLevel, 2 * row, 2 * col);
			subTiles[1] = new TextureTileRTT(new Sector(p0, p1, t1, t2), nextLevel, 2 * row, 2 * col + 1);
			subTiles[2] = new TextureTileRTT(new Sector(p1, p2, t0, t1), nextLevel, 2 * row + 1, 2 * col);
			subTiles[3] = new TextureTileRTT(new Sector(p1, p2, t1, t2), nextLevel, 2 * row + 1, 2 * col + 1);

			return subTiles;
		}
	}

	private class RenderToTexture{
		
		private FrameBuffer fb;
		
		public RenderToTexture(DrawContext dc){
			this.fb = new FrameBuffer();
		}

		public void render(DrawContext dc, TextureTileRTT tile){

			GL gl = dc.getGL();
			
			int[] tmp = new int[1];
			gl.glGenTextures(1, tmp, 0);
			int texID = tmp[0];
			gl.glBindTexture(GL2.GL_TEXTURE_2D, texID);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
			gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, 512, 512, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, null);

            try {
                dc.getFramebufferController().push();
                dc.getFramebufferController().setCurrent(fb);
                fb.bind(dc);
                fb.attachTexture(dc, texID, GL2.GL_COLOR_ATTACHMENT0, 0);
                fb.isComplete(dc, true);
                fb.setDrawBuffers(dc, new int[]{GL2.GL_COLOR_ATTACHMENT0});

                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
                fb.isComplete(dc, true);
                doRender(dc, tile);
                gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                
    			tile.setTexture(texID);                
            } catch (IllegalStateException e) {
                gl.glDeleteTextures(1, tmp, 0);
                //proceed without this texture
            } finally {
                fb.releaseTextures(dc);
                dc.getFramebufferController().pop();
            }
		}

		public void doRender(DrawContext dc, TextureTile tile){
			GL2 gl = dc.getGL().getGL2();
			
			LatLon center = tile.getSector().getCentroid();
			gl.glLoadIdentity();
			gl.glScaled((double)TEXTURE_WIDTH/(tile.getSector().getDeltaLonDegrees()),
						(double)TEXTURE_HEIGHT/(tile.getSector().getDeltaLatDegrees()),
						1.0d);
			gl.glTranslated(-(center.getLongitude().degrees),
							-(center.getLatitude().degrees),
							0.0d);
			
			for(SurfaceObjectRenderer surfaceLayer : surfaceLayers){
				surfaceLayer.renderTexture(dc, tile.getSector());
			}	
		}
	}
}
