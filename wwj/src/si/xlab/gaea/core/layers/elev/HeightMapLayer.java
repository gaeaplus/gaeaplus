package si.xlab.gaea.core.layers.elev;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.TextureTile;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Logging;
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
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLProfile;

/**
 *
 * @author vito
 */
public class HeightMapLayer extends TiledImageLayer{

	private static AVList makeParam() {
		AVList params = new AVListImpl();

		String s = "HeightMapLayer";
		params.setValue(AVKey.DATA_CACHE_NAME, s);

		s = "HeightMapLayer";
		params.setValue(AVKey.DATASET_NAME, s);

		s = TextureIO.PNG;
		params.setValue(AVKey.FORMAT_SUFFIX, s);

		Integer i = 0;
		params.setValue(AVKey.NUM_EMPTY_LEVELS, i);

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

	private final InitLoader initLoader;
	private static boolean init = true;

	public HeightMapLayer(LevelSet levelSet)
	{
		super(levelSet);
		this.setUseMipMaps(false);
		this.setUseTransparentTextures(false);

		initLoader = new InitLoader();
	}

	public HeightMapLayer(AVList params)
	{
		this(new LevelSet(params));
	}

	public HeightMapLayer()
	{
		this(new LevelSet(makeParam()));
	}

	@Override
	public void requestTexture(DrawContext dc, TextureTile tile)
	{
		RequestNormalsTask request = new RequestNormalsTask(dc.getGlobe(), tile);
		if(!requestTasks.contains(request)){
			requestTasks.offer(request);
		}
	}

	@Override
	protected void forceTextureLoad(TextureTile tile)
	{
	}

	@Override
    public void render(DrawContext dc) {
        /*
        if (init) {
            //WorldWind.setImmediateMode(true);
            initLoader.init(dc);
            init = false;
            //WorldWind.setImmediateMode(false);
        }
         * 
         */

        GL2 gl = dc.getGL().getGL2();
        gl.glDisable(GL2.GL_ALPHA_TEST);
        gl.glDisable(GL2.GL_BLEND);
        super.render(dc);
        gl.glEnable(GL2.GL_BLEND);
        gl.glEnable(GL2.GL_ALPHA_TEST);
    }

    @Override
    protected void setBlendingFunction(DrawContext dc){
    }

    @Override
    public void sendRequests() {
        Runnable task = this.requestTasks.poll();
        while (task != null) {
			if(((RequestNormalsTask)task).isLocal()){
				WorldWind.getTaskService().addTask(task);
			}
			else if (!WorldWind.getCalcTaskService().isFull() && WorldWind.getCalcTaskService().getNumberOfTasks() < 2) {
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

		public boolean isLocal(){
			URL textureURL = WorldWind.getDataFileStore().findFile(getTilePath(tile),
                    false);	
			return textureURL == null ? false : true;
		}

		@Override
        public void run() {
            URL textureURL = WorldWind.getDataFileStore().findFile(getTilePath(tile),
                    false);

            if (textureURL == null) {
                //calc normal map!
                if(!downloadTexture()){
                	getLevels().markResourceAbsent(tile);
					return;		
				}
                textureURL = WorldWind.getDataFileStore().findFile(getTilePath(tile), false);
            }

            if (textureURL != null) {
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

            firePropertyChange(AVKey.LAYER, null, this);
        }

        private boolean loadTexture(URL url) {
            InputStream is = null;
            try {
                is = url.openStream();

                //padding
                int width = tile.getLevel().getTileWidth() + 2;
                int height = tile.getLevel().getTileHeight() + 2;

                FloatBuffer buffer = Buffers.newDirectFloatBuffer(width * height);

                DataInputStream dis = new DataInputStream(is);
                for (int i = 0; i < width * height; i++) {
                    buffer.put(dis.readFloat());
                }
                dis.close();

                buffer.rewind();
                TextureData textureData = new TextureData(GLProfile.getDefault(), 
						GL2.GL_R32F, width, height, 0, GL2.GL_RED, GL.GL_FLOAT,
                        false, false, false, buffer, null);

                tile.setTextureData(textureData);

                if (tile.getLevelNumber() != 0 || !isRetainLevelZeroTiles()) {
                    addTileToCache(tile);
                }

                getLevels().unmarkResourceAbsent(tile);
                firePropertyChange(AVKey.LAYER, null, this);
            } catch (Exception e) {
                String msg = "HeightMapLayer.ExceptionAttemptingToReadFile, URL:" + url;
                Logging.logger().log(java.util.logging.Level.SEVERE, msg, e);
                return false;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
            return true;
        }

        private boolean downloadTexture() {
            Sector sector = tile.getSector();

            int width = tile.getLevel().getTileWidth();
            int height = tile.getLevel().getTileHeight();

            //calc padding
            double rMinLat = Math.max(sector.getMinLatitude().radians - sector.getDeltaLatRadians() / (double) height, -90);
            double rMaxLat = Math.min(sector.getMaxLatitude().radians + sector.getDeltaLatRadians() / (double) height, 90);
            double rMinLon = Math.max(sector.getMinLongitude().radians - sector.getDeltaLonRadians() / (double) width, -180);
            double rMaxLon = Math.min(sector.getMaxLongitude().radians + sector.getDeltaLonRadians() / (double) width, 180);

            Angle minLat = Angle.fromRadiansLatitude(rMinLat);
            Angle maxLat = Angle.fromRadiansLatitude(rMaxLat);
            Angle minLon = Angle.fromRadiansLongitude(rMinLon);
            Angle maxLon = Angle.fromRadiansLongitude(rMaxLon);

            sector = new Sector(minLat, maxLat, minLon, maxLon);
            width += 2;
            height += 2;

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
            
	    	if(actualResolution != Double.MAX_VALUE){
                saveHeights(getTilePath(tile), elevations, width, height);
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

        private boolean saveHeights(String path, double[] heights, int width, int height) {
            File file = WorldWind.getDataFileStore().newFile(path);
            boolean isDone = false;

            DataOutputStream dos = null;
            try {
                OutputStream os = new FileOutputStream(file);
                dos = new DataOutputStream(os);

                for (int i = 0; i < width * height; i++) {
                    dos.writeFloat((float) (heights[i]));
                }
                dos.close();
            } catch (IOException ex) {
				if(dos != null){
                	dos.close();
				}
                file.delete();
                return isDone;
            } finally {
                return isDone;
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

    private class InitLoader {

        private final int maxLevel = 1;
        private int numTextures = 0;
        private int numTexturesDone = 0;
        private int numTexturesAbsent = 0;
        private long startTime;

        public void init(DrawContext dc) {
            count(dc);
            load(dc);
        }

        private void count(DrawContext dc) {
            numTextures = 0;
            int n = 1;
            for (TextureTile tile : getTopLevels()) {
                recursiveCount(dc, tile);
                System.out.println("Base Tile: " + n);
                n += 1;
            }
            System.out.println("Number of all textures: " + numTextures);
            System.out.println("Number of all textures absent: " + numTexturesAbsent);
            System.out.println("Size (MB): " + numTextures * 0.1708);
        }

        private void load(DrawContext dc) {
            startTime = System.currentTimeMillis();
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

                URL textureURL = WorldWind.getDataFileStore().findFile(getTilePath(tile),
                        false);

                if (textureURL == null) {
                    RequestNormalsTask request = new RequestNormalsTask(dc.getGlobe(), tile);
                    request.downloadTexture();
                } else {
                    done = true;
                    double ratePerMinute = 60000.0 * (double) numTexturesDone
                            / (double) (System.currentTimeMillis() - startTime);
                    System.out.println("Done: " + numTexturesDone + ", tile left: "
                            + (double) (numTextures - numTexturesDone) / ratePerMinute);
                    numTexturesDone += 1;
                }
            }

            TextureTile[] tiles = tile.createSubTiles(getLevels().getLevel(tile.getLevelNumber() + 1));

            for (TextureTile subTiles : tiles) {
                recursiveLoad(dc, subTiles);
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
                numTexturesAbsent += 1;
                return;
            }

            numTextures += 1;

            TextureTile[] tiles = tile.createSubTiles(getLevels().getLevel(tile.getLevelNumber() + 1));

            for (TextureTile subTiles : tiles) {
                recursiveCount(dc, subTiles);
            }
        }
    }
}
