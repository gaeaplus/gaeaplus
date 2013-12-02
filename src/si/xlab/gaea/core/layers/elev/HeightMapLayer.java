package si.xlab.gaea.core.layers.elev;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.awt.TextRenderer;
import com.jogamp.opengl.util.texture.TextureData;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderAttributes.RenderType;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
import java.awt.Font;
import java.awt.Point;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

/**
 *
 * @author vito
 */
public class HeightMapLayer extends TiledImageLayer {

	private static AVList makeParam() {
		AVList params = new AVListImpl();

		String s = "HeightMapLayer";
		params.setValue(AVKey.DATA_CACHE_NAME, s);

		s = "HeightMapLayer";
		params.setValue(AVKey.DATASET_NAME, s);

		Integer i = 0;
		params.setValue(AVKey.NUM_EMPTY_LEVELS, i);

		params.setValue(AVKey.FORMAT_SUFFIX, ".bil");

		i = 14;
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
	private static final Logger logger = Logging.logger("si.xlab.gaea.core.layers.HeightMapLayer");
	private final PriorityBlockingQueue<Runnable> requestTasks = new PriorityBlockingQueue<Runnable>(10);

	private StartupLoader loader = null;
	private boolean isLoading = false;
	private float percentageDone = 0.0f;

    protected final Object fileLock = new Object();
	
	public HeightMapLayer(LevelSet levelSet) {
		super(levelSet);
		this.setUseMipMaps(false);
		this.setUseTransparentTextures(false);
	}

	public HeightMapLayer(AVList params) {
		this(new LevelSet(params));
	}

	public HeightMapLayer() {
		this(new LevelSet(makeParam()));
	}

	@Override
	public void requestTexture(DrawContext dc, TextureTile tile) {
		RequestNormalsTask request = new RequestNormalsTask(dc.getGlobe(), tile);
		if (!requestTasks.contains(request)) {
			requestTasks.offer(request);
		}
	}

	@Override
	protected void forceTextureLoad(TextureTile tile) {
		final URL textureURL = this.getDataFileStore().findFile(tile.getPath(), true);

        if (textureURL != null)
        {
			RequestNormalsTask request = new RequestNormalsTask(null, tile);
			request.loadTexture(textureURL);
        }
	}

	private void renderLoadingScreen(DrawContext dc) {
		dc.addOrderedRenderable(new OrderedRenderable() {

			@Override
			public double getDistanceFromEye() {
				return 0.0;
			}

			@Override
			public void pick(DrawContext dc, Point pickPoint) {
			}

			@Override
			public void render(DrawContext dc) {
				GL2 gl = dc.getGL().getGL2();
				gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
				gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

				TextRenderer tr1 = new TextRenderer(Font.decode(Font.SANS_SERIF + "-" + "18"));
				tr1.beginRendering(dc.getDrawableWidth(), dc.getDrawableHeight());
				
				tr1.setColor(1.0f, 1.0f, 1.0f, 1.0f);
				tr1.draw("WorldWind with special effects has been run for the first time.", 20, 130);
				tr1.draw("Elevation data will be downloaded and pre-processed.", 20, 110);
				tr1.draw("Please be patient... Speed depends on internet connection and computer hardware.", 20, 90);
				tr1.draw("In the meantime you can disable the effects (from a menu, toolbar or command-line setting).", 20, 70);
				tr1.endRendering();
				tr1.dispose();
				
				TextRenderer tr2 = new TextRenderer(Font.decode(Font.SANS_SERIF + "-" + "16"));
				tr2.beginRendering(dc.getDrawableWidth(), dc.getDrawableHeight());
				tr2.setColor(1.0f, 0.5f, 0.5f, 1.0f);
				tr2.draw("Downloading elevations: " + String.format("%.2f", (percentageDone * 100.0f)) + "% done.", 20, 50);
				tr2.endRendering();
				tr2.dispose();
			}
		});
	}

	@Override
	public void render(DrawContext dc) {

		if(loader == null){
			loader = new StartupLoader(dc, new StartupLoaderCallback() { 
				@Override
				public void callback(float percentageDone) {
					HeightMapLayer.this.percentageDone = percentageDone;
					HeightMapLayer.this.isLoading = percentageDone < 1.0f ? true : false;
				}
			});

			if(!loader.isTexturePreloadingDone()){
				isLoading = true;
				percentageDone = 0.0f;
				WorldWind.getCalcTaskService().addTask(loader);
				return;
			}
		}

		if(isLoading){
			this.getRenderAttributes().setRenderType(RenderType.SCREEN);
			renderLoadingScreen(dc);
			return;
		}
		else{
			HeightMapLayer.this.getRenderAttributes().setRenderType(RenderType.GLOBE);
		}

		GL2 gl = dc.getGL().getGL2();
		gl.glDisable(GL2.GL_ALPHA_TEST);
		gl.glDisable(GL2.GL_BLEND);
		super.render(dc);
		gl.glEnable(GL2.GL_BLEND);
		gl.glEnable(GL2.GL_ALPHA_TEST);
	}

	@Override
	protected void setBlendingFunction(DrawContext dc) {
	}

	@Override
	public void sendRequests() {
		Runnable task = this.requestTasks.poll();
		while (task != null) {
			if (((RequestNormalsTask) task).isLocal() 
					&& !WorldWind.getTaskService().contains(task)) {
				WorldWind.getTaskService().addTask(task);
			} else if (!WorldWind.getCalcTaskService().isFull() 
					&& WorldWind.getCalcTaskService().getNumberOfTasks() < 2
					&& !WorldWind.getCalcTaskService().contains(task)) {
				WorldWind.getCalcTaskService().addTask(task);
			}
			task = this.requestTasks.poll();
		}
		requestTasks.clear();
	}

	private String getTilePath(TextureTile tile) {
		return tile.getPath();
	}

	protected void addTileToCache(TextureTile tile) {
		TextureTile.getMemoryCache().add(tile.getTileKey(), tile);
	}

	private class RequestNormalsTask implements Runnable, Comparable<RequestNormalsTask> {

		private final Globe globe;
		private final TextureTile tile;

		public RequestNormalsTask(Globe globe, TextureTile tile) {
			this.globe = globe;
			this.tile = tile;
		}

		public boolean isLocal() {
			URL textureURL = WorldWind.getDataFileStore().findFile(getTilePath(tile),
				false);
			return textureURL == null ? false : true;
		}

		@Override
		public void run() {
			final java.net.URL textureURL = HeightMapLayer.this.getDataFileStore().findFile(getTilePath(tile), false);

			if (textureURL == null) {
				//calc normal map!
				if (!downloadTexture()) {
					getLevels().markResourceAbsent(tile);
				}
				firePropertyChange(AVKey.LAYER, null, this);
				return;
			}
			else{
				if (loadTexture(textureURL)) {
					getLevels().unmarkResourceAbsent(tile);
					return;
				} else {
					// Assume that something's wrong with the file and delete it.
					getDataFileStore().removeFile(textureURL);
					String message = Logging.getMessage("generic.DeletedCorruptDataFile", textureURL);
					Logging.logger().warning(message);
				}
			}
		}

		public boolean loadTexture(URL url){
			
			InputStream is = null;
			BufferedInputStream bis = null;
			DataInputStream dis = null;
			
			try {
				synchronized (fileLock) {
					is = url.openStream();
					bis = new BufferedInputStream(is);

					//padding
					int width = tile.getLevel().getTileWidth();
					int height = tile.getLevel().getTileHeight();

					FloatBuffer buffer = Buffers.newDirectFloatBuffer(width * height);

					dis = new DataInputStream(bis);
					for (int i = 0; i < width * height; i++) {
						buffer.put(dis.readFloat());
					}
					buffer.rewind();

					TextureData textureData = new TextureData(GLProfile.getDefault(),
							GL2.GL_R32F, width, height, 0, GL2.GL_RED, GL2.GL_FLOAT,
							false, false, false, buffer, null);

					tile.setTextureData(textureData);

					if (tile.getLevelNumber() != 0 || !isRetainLevelZeroTiles()) {
						addTileToCache(tile);
					}

					getLevels().unmarkResourceAbsent(tile);
					firePropertyChange(AVKey.LAYER, null, this);
				}
			} catch (Exception e) {
				String msg = "HeightMapLayer.ExceptionAttemptingToReadFile, URL:" + url;
				Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
				return false;
			} finally {
				try {
					if(is != null)
						dis.close();
					if(bis != null)
						bis.close();
					if(dis != null)
						dis.close();
				} catch (IOException ex) {
					Logger.getLogger(HeightMapLayer.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
			return true;
		}

		private boolean downloadTexture() {
			Sector sector = tile.getSector();

			int width = tile.getLevel().getTileWidth();
			int height = tile.getLevel().getTileHeight();

			//calc padding
			/*
			double rMinLat = Math.max(sector.getMinLatitude().radians - sector.getDeltaLatRadians() / (double) height, -90);
			double rMaxLat = Math.min(sector.getMaxLatitude().radians + sector.getDeltaLatRadians() / (double) height, 90);
			double rMinLon = Math.max(sector.getMinLongitude().radians - sector.getDeltaLonRadians() / (double) width, -180);
			double rMaxLon = Math.min(sector.getMaxLongitude().radians + sector.getDeltaLonRadians() / (double) width, 180);

			Angle minLat = Angle.fromRadiansLatitude(rMinLat);
			Angle maxLat = Angle.fromRadiansLatitude(rMaxLat);
			Angle minLon = Angle.fromRadiansLongitude(rMinLon);
			Angle maxLon = Angle.fromRadiansLongitude(rMaxLon);

			 * 
			 */
			//sector = new Sector(minLat, maxLat, minLon, maxLon);
			//width += 2;
			//height += 2;

			double dLat = sector.getDeltaLatRadians();
			double bestResolution = globe.getElevationModel().getBestResolution(sector);
			double targetResolution = (dLat / height);

			if (bestResolution >= targetResolution * 3.5) {
				return false;
			}
			targetResolution = Math.max(bestResolution, targetResolution);
			double actualResolution = Double.MAX_VALUE;

			List<LatLon> latlons = buildLatLon(sector, height, width);
			double[] elevations = new double[latlons.size()];

			//System.out.println("El requested");
			actualResolution = globe.getElevationModel().getElevationsImmediate(sector, latlons, targetResolution, elevations);
			//System.out.println("El requested done");

			latlons.clear();

			if (actualResolution != Double.MAX_VALUE) {
				synchronized(fileLock){
					saveHeights(getTilePath(tile), elevations, width, height);
				}
				return true;
			}
			return false;
		}

		private List<LatLon> buildLatLon(Sector sector, int height, int width) {

			double dLat = sector.getDeltaLatRadians() / (double) height;
			double dLon = sector.getDeltaLonRadians() / (double) width;

			double minLat = sector.getMinLatitude().radians;
			double minLon = sector.getMinLongitude().radians;

			List<LatLon> latlon = new ArrayList<LatLon>();

			for (int j = 0; j < height; j++) {
				for (int i = 0; i < width; i++) {
					latlon.add(new LatLon(Angle.fromRadians(minLat + dLat * (double) j),
						Angle.fromRadians(minLon + dLon * (double) i)));
				}
			}
			return latlon;
		}

		private boolean saveHeights(String path, double[] heights, int width, int height){
			File file = WorldWind.getDataFileStore().newFile(path);
			boolean isDone = false;

			OutputStream fos = null;
			BufferedOutputStream bos = null;
			DataOutputStream dos = null;

			try {
				fos = new FileOutputStream(file);
				bos = new BufferedOutputStream(fos);
				dos = new DataOutputStream(bos);

				for (int i = 0; i < width * height; i++) {
					dos.writeFloat((float) (heights[i]));
				}
				dos.flush();
			} catch (IOException ex) {
				file.delete();
				Logger.getLogger(HeightMapLayer.class.getName()).log(Level.SEVERE, null, ex);
				return isDone;
			} finally {
				try {
					if(fos != null)
						fos.close();
					if(bos != null)
						bos.close();
					if(dos != null)
						dos.close();
					return isDone;
				} catch (IOException ex) {
					Logger.getLogger(HeightMapLayer.class.getName()).log(Level.SEVERE, null, ex);
					return false;
				}
			}
		}

		@Override
		public int compareTo(RequestNormalsTask o) {
			double p1 = this.tile.getPriority();
			double p2 = o.tile.getPriority();
			return (p1 == p2 ? 0 : (p1 > p2 ? 1 : -1));
		}

		@Override
		public boolean equals(Object obj) {
			boolean out = false;
			if (obj instanceof RequestNormalsTask) {
				out = this.tile.equals(((RequestNormalsTask) obj).tile);
			}
			return out;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 61 * hash + (this.tile != null ? this.tile.hashCode() : 0);
			return hash;
		}
	}

	private class StartupLoader implements Runnable{

		private final int maxLevel = 0;
		private int numTextures = 0;
		private int numTexturesDone = 0;

		private final String initIndicatorFilename = "heightMapInit.indicator"; 
		private DrawContext dc;

		private final StartupLoaderCallback callback;

		public StartupLoader(DrawContext dc, StartupLoaderCallback callback) {
			this.dc = dc;
			this.callback = callback;
		}

		@Override
		public void run(){
			preloadTextures(this.dc);
		}

		public void preloadTextures(DrawContext dc) {

			if(WorldWind.isOfflineMode()){
				return;
			}
			
			count(dc);
			load(dc);
			try {
				File file = WorldWind.getDataFileStore().newFile(initIndicatorFilename);
				file.createNewFile();
			} catch (IOException ex) {
				Logger.getLogger(HeightMapLayer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		public boolean isTexturePreloadingDone(){
			URL url = WorldWind.getDataFileStore().findFile(initIndicatorFilename, false);
			if(url == null){
				return false;
			}
			return true;
		}

		private void count(DrawContext dc) {
			numTextures = 0;
			int n = 1;
			for (TextureTile tile : getTopLevels()) {
				recursiveCount(dc, tile);
				//System.out.println("Base Tile: " + n);
				n += 1;
			}
			//System.out.println("Number of all textures: " + numTextures);
			//System.out.println("Number of all textures absent: " + numTexturesAbsent);
			//System.out.println("Size (MB): " + numTextures * 0.1708);
		}

		private void load(DrawContext dc) {
			numTexturesDone = 0;
			for (TextureTile tile : getTopLevels()) {
				recursiveLoad(dc, tile);
			}
		}

		private void recursiveLoad(DrawContext dc, TextureTile tile) {
			if (tile.getLevelNumber() > maxLevel) {
				return;
			}

			boolean done = false;
			while (!done) {

				if (getLevels().isResourceAbsent(tile)) {
					done = true;
					return;
				}

				final java.net.URL textureURL = HeightMapLayer.this.getDataFileStore().findFile(getTilePath(tile), false);

				if (textureURL == null) {
					RequestNormalsTask request = new RequestNormalsTask(dc.getGlobe(), tile);
					//System.out.println("Requesting tile level:"+ tile.getLevelNumber() +" row: "+tile.getRow()+" Column: "+tile.getColumn());
					request.downloadTexture();
				} else {
					done = true;
					numTexturesDone += 1;
					callback.callback((float)numTexturesDone/(float)numTextures);
				}
			}

			TextureTile[] tiles = tile.createSubTiles(getLevels().getLevel(tile.getLevelNumber() + 1));

			for (TextureTile subTile : tiles) {
				recursiveLoad(dc, subTile);
			}
		}

		private void recursiveCount(DrawContext dc, TextureTile tile) {
			if (tile.getLevelNumber() > maxLevel) {
				return;
			}

			Sector sector = tile.getSector();
			int height = tile.getLevel().getTileHeight();

			double dLat = sector.getDeltaLatRadians();
			double bestResolution = dc.getGlobe().getElevationModel().getBestResolution(sector);
			double targetResolution = 2.0 * (dLat / height);

			if (bestResolution >= targetResolution * 2.0) {
				getLevels().markResourceAbsent(tile);
				return;
			}

			numTextures += 1;

			TextureTile[] tiles = tile.createSubTiles(getLevels().getLevel(tile.getLevelNumber() + 1));

			for (TextureTile subTiles : tiles) {
				recursiveCount(dc, subTiles);
			}
		}
	}

	private static interface StartupLoaderCallback{
		public void callback(float percentageDone);
	}
}
