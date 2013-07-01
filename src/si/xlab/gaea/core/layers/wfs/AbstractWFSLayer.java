package si.xlab.gaea.core.layers.wfs;


import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.retrieve.HTTPRetriever;
import gov.nasa.worldwind.retrieve.RetrievalPostProcessor;
import gov.nasa.worldwind.retrieve.Retriever;
import gov.nasa.worldwind.retrieve.URLRetriever;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import java.io.IOException;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import si.xlab.gaea.avlist.AvKeyExt;
import si.xlab.gaea.core.ogc.gml.GMLFeature;
import si.xlab.gaea.core.ogc.gml.GMLParser;
import si.xlab.gaea.core.ogc.kml.KMLParserException;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;
import si.xlab.gaea.core.render.DefaultLook;

/**
 * @author marjan
 * Modelled after NASA's PlaceNameLayer, but using a single WFSService
 * 		(while PlaceNameLayer uses PlaceNameServiceSet)
 */
abstract public class AbstractWFSLayer extends AbstractLayer
{
	protected static final Logger logger = Logger.getLogger(AbstractWFSLayer.class.getName());

    public static final Angle DEFAULT_TILE_DELTA = Angle.fromDegrees(0.1);
    public static final Angle TILE_DELTA_SINGLE_TILE_FOR_SLOVENIA = Angle.fromDegrees(3.45);
            
	private static final long DEFAULT_WFS_CACHE_SIZE = 200*1000*1000;

    private final WFSService wfsService;
    private final WFSNavigationTile rootTile;
    private PriorityBlockingQueue<Runnable> requestQ = new PriorityBlockingQueue<Runnable>(6);
    private Vec4 referencePoint;
    private final Object fileLock = new Object();
    private final Object loadFlushLock = new Object();

    private KMLStyle defaultStyle; //the style used to draw features that don't have a style attribute themselves in the GML
    
    public AbstractWFSLayer(WFSService wfsService, String layerName)
    {
		init(wfsService, layerName);

		this.wfsService = wfsService.deepCopy();
		rootTile = new WFSNavigationTile(wfsService.getTileDelta(),
										 wfsService.getSector(), this);
    }

	public AbstractWFSLayer(WFSService wfsService, String layerName, int navLevelCount)
    {
    	init(wfsService, layerName);

		this.wfsService = wfsService.deepCopy();
		rootTile = new WFSNavigationTile(wfsService.getTileDelta(), 
										 wfsService.getSector(),
										 navLevelCount, "top", this);
    }

    /**
     * Sets the default feature style for this layer.
     * Extending classes should add any necessary action to flush caches if needed, trigger redraw etc.
     * @param style
     */
    public void setDefaultStyle(KMLStyle style)
    {
        if (style != null)
        {
            this.defaultStyle = style;
        } else {
            logger.log(Level.WARNING, "Layer default style cannot be null - keeping the old default style instead.");
        }
    }

    public KMLStyle getDefaultStyle()
    {
        return this.defaultStyle;
    }
    
    protected void flushTileData()
    {
        //flushing must not occur while other threads are adding tiles
        synchronized (loadFlushLock)
        {
            this.rootTile.flushTileData();
        }
    }
    
    
    /**
     * Sets the default that uses the given icon but is otherwise identical to old default style.
     * The method name must be different from the setDefaultStyle(Style style) variant
     * so that classes that don't know Style can distinguish between the two.
     * @param icon filepath or URL of the icon image
     */
    public void setDefaultIconStyle(String icon)
    {
        try {
            setDefaultStyle(KMLStyleFactory.createStyle(
                    "<Style id=\"LAYER_DEFAULT_" + getName() + "\">"
                    + "<IconStyle><Icon><href>" + icon
                    + "</href></Icon></IconStyle></Style>",
                    this.defaultStyle));
        } catch (KMLParserException e) {
            logger.log(Level.WARNING, "Setting icon style failed - keeping the old default style instead.");
        }
    }

    /**
     * Sets the default feature style for this layer.
     * Note that this method also supports style references (e.g. "#layer_burger_style")
     * so it is not the sama as setDefaultStyle(StyleFactory.createStyle(styleString));
     * The method name must be different from the setDefaultStyle(Style style) variant
     * so that classes that don't know Style can distinguish between the two.
     * @param styleString
     */
    public void setDefaultStyleFromString(String styleString)
    {
        setDefaultStyle(getStyle(styleString));
    }

	private void init(WFSService wfsService, String layerName)
	{
		if (wfsService == null)
        {
            String message = Logging.getMessage("nullValue.wfsServiceIsNull");

            logger.fine(message);
            throw new IllegalArgumentException(message);
        }

        this.defaultStyle = new KMLStyle(DefaultLook.DEFAULT_FEATURE_STYLE);

        if (!WorldWind.getMemoryCacheSet().containsCache(WFSBaseTile.class.getName()))
        {
        	synchronized (WFSBaseTile.class) {
        		if (!WorldWind.getMemoryCacheSet().containsCache(WFSBaseTile.class.getName()))
        		{
		        	//create a common cache for all WFS layers
		            long size = Configuration.getLongValue(AvKeyExt.WFS_LAYER_CACHE_SIZE,
		                    DEFAULT_WFS_CACHE_SIZE);
		            MemoryCache cache = new BasicMemoryCache((long) (0.85 * size), size);

		            cache.setName("WFS Tiles");
		            WorldWind.getMemoryCacheSet().addCache(WFSBaseTile.class.getName(), cache);
        		}
        	}
        }
		setName(layerName);
	}

    @Override
    public void setEnabled(boolean enabled)
    {
    	super.setEnabled(enabled);    	
    }

    public final WFSService getWFSService()
    {
        return this.wfsService;
    }

    /**
     * Should create a Cacheable object containing the data for the given tile
     * @param tile the tile for which to create the data chunk
     * @param features unmodifiable list of features in the tile's sector, as given by the parser 
     * @return
     */
    abstract protected Cacheable createDataChunk(WFSBaseTile tile, List<GMLFeature> features);
    
    
    private PriorityBlockingQueue<Runnable> getRequestQ()
    {
        return this.requestQ;
    }

    @Override
    protected void doRender(DrawContext dc)
    {
        this.referencePoint = this.computeReferencePoint(dc);

        if (!isServiceVisible(dc))
        {
            return;
        }

        setRenderState(dc);

        for (WFSBaseTile tile : rootTile.getVisibleBaseTiles(dc))
        {
            try
            {
                if (tile.isTileInMemory())
                {
                    doRenderTile(dc, tile);
                }
                else
                {
                    // Tile's data isn't available, so request it
                    if (!getWFSService().isResourceAbsent(
                            getWFSService().getTileNumber(tile.row,
                            tile.column)))
                    {
                        this.requestTile(dc, tile);
                    }
                }
            }
            catch (Exception e)
            {
                logger.severe("WFSLayer: Exception Rendering Tile: " + e);
                e.printStackTrace();
            }
        }
		popRenderState(dc);

        this.sendRequests();
        this.requestQ.clear();
    }

    @Override
    protected void doPreRender(DrawContext dc)
    {
        this.referencePoint = this.computeReferencePoint(dc);

        if (!isServiceVisible(dc))
        {
            return;
        }

        for (WFSBaseTile tile : rootTile.getVisibleBaseTiles(dc))
        {
            try
            {
                if (tile.isTileInMemory())
                {
                    // pre-rendering is only done on already-loaded tiles
                    doPreRenderTile(dc, tile);
                }
            }
            catch (Exception e)
            {
                logger.info(Logging.getMessage("layers.WFSLayer.ExceptionRenderingTile") + e);
            }
        }

        this.sendRequests();
        this.requestQ.clear();
    }
    
    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        this.referencePoint = this.computeReferencePoint(dc);

        if (!isServiceVisible(dc))
        {
            return;
        }

        for (WFSBaseTile tile : rootTile.getVisibleBaseTiles(dc))
        {
            try
            {
                if (tile.isTileInMemory())
                {
                    // picking is only done on already-loaded tiles
                    doPickTile(dc, tile, pickPoint);
                }
            }
            catch (Exception e)
            {
                logger.info(Logging.getMessage("layers.WFSLayer.ExceptionRenderingTile") + e);
            }
        }

        this.sendRequests();
        this.requestQ.clear();
    }

	protected void setRenderState(DrawContext dc){}

	protected void popRenderState(DrawContext dc){}

    abstract protected void doRenderTile(DrawContext dc, WFSBaseTile tile);
    
    abstract protected void doPreRenderTile(DrawContext dc, WFSBaseTile tile);
    
    abstract protected void doPickTile(DrawContext dc, WFSBaseTile tile, java.awt.Point pickPoint);

    private Vec4 computeReferencePoint(DrawContext dc)
    {
        if (dc.getViewportCenterPosition() != null)
        {
            return dc.getGlobe().computePointFromPosition(
                    dc.getViewportCenterPosition());
        }

        java.awt.geom.Rectangle2D viewport = dc.getView().getViewport();
        int x = (int) viewport.getWidth() / 2;

        for (int y = (int) (0.5 * viewport.getHeight()); y >= 0; y--)
        {
            Position pos = dc.getView().computePositionFromScreenPoint(x, y);

            if (pos == null)
            {
                continue;
            }

            return dc.getGlobe().computePointFromPosition(pos.getLatitude(),
                    pos.getLongitude(), 0d);
        }

        return null;
    }

    protected Vec4 getReferencePoint()
    {
        return this.referencePoint;
    }
   
    private boolean isServiceVisible(DrawContext dc)
    {
        if (!wfsService.isEnabled())
        {
            return false;
        }

        // noinspection SimplifiableIfStatement
        if (dc.getVisibleSector() != null
                && !wfsService.getSector().intersects(dc.getVisibleSector()))
        {
            return false;
        }

        return wfsService.getExtent(dc).intersects(
                dc.getView().getFrustumInModelCoordinates());
    }

    // ============== Image Reading and Downloading ======================= //

    private void requestTile(DrawContext dc, WFSBaseTile tile)
    {
        Vec4 centroid = tile.getCentroidPoint(dc.getGlobe());

        if (this.getReferencePoint() != null)
        {
            tile.setPriority(centroid.distanceTo3(this.getReferencePoint()));
        }

        RequestTask task = new RequestTask(tile, this);

        this.getRequestQ().add(task);
    }

    @SuppressWarnings("CallToThreadYield")
    private void sendRequests()
    {
        Runnable task = this.requestQ.poll();

        while (task != null)
        {
            if (!WorldWind.getTaskService().isFull())
            {
                WorldWind.getTaskService().addTask(task);
            }
            else {
	            /* jaKa: it's probably good practice to yield the thread here, so
	             * that the task service gets a chance to do some work ...
	             */
	            Thread.yield();
            }
            
            task = this.requestQ.poll();
        }
    }

    private static class RequestTask implements Runnable, Comparable<RequestTask>
    {
        private final AbstractWFSLayer layer;
        private final WFSBaseTile tile;

        RequestTask(WFSBaseTile tile, AbstractWFSLayer layer)
        {
            this.layer = layer;
            this.tile = tile;
        }

		//download tile
        public void run()
        {
			final java.net.URL tileURL = WorldWind.getDataFileStore().findFile(
				tile.getFileCachePath(), false);

			if(this.tile.isTileInMemory())
				return;

			if(tileURL != null){

				if(WorldWind.getGLTaskService().hasDisposeTasks())
					return;

				if (this.layer.loadTile(tile, tileURL))
				{
					tile.layer.getWFSService().unmarkResourceAbsent(
							tile.layer.getWFSService().getTileNumber(tile.row,
							tile.column));
					this.layer.firePropertyChange(AVKey.LAYER, null, this);
					return;
				}
				else
				{
					// Assume that something's wrong with the file and delete it.
					WorldWind.getDataFileStore().removeFile(tileURL);
					tile.layer.getWFSService().markResourceAbsent(
							tile.layer.getWFSService().getTileNumber(tile.row,
							tile.column));
					String message = Logging.getMessage(
							"generic.DeletedCorruptDataFile", tileURL);

					logger.info(message);
				}
			}

			this.layer.downloadTile(this.tile);
        }

        /**
         * @param that the task to compare
         * @return -1 if <code>this</code> less than <code>that</code>, 1 if greater than, 0 if equal
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RequestTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RequestTaskIsNull");

                logger.severe(msg);
                throw new IllegalArgumentException(msg);
            }

            return this.tile.getPriority() == that.tile.getPriority()
                    ? 0
                    : this.tile.getPriority() < that.tile.getPriority() ? -1 : 1;
        }

        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            if (o == null || getClass() != o.getClass())
            {
                return false;
            }

            final RequestTask that = (RequestTask) o;

            // Don't include layer in comparison so that requests are shared among layers
            return !(tile != null ? !tile.equals(that.tile) : that.tile != null);
        }

        @Override
        public int hashCode()
        {
            return (tile != null ? tile.hashCode() : 0);
        }

        @Override
        public String toString()
        {
            return this.tile.toString();
        }
    }

    private boolean loadTile(WFSBaseTile tile, java.net.URL url)
    {
        if (WWIO.isFileOutOfDate(url, getWFSService().getExpiryTime()))
        {
            // The file has expired. Delete it then request download of newer.
            WorldWind.getDataFileStore().removeFile(url);
            String message = Logging.getMessage("generic.DataFileExpired", url);

            logger.fine(message);
            return false;
        }

        //during loadTile, a new tileData object is being created but not yet inserted into cache
        //this may not be interleaved with flushing of the layer
        synchronized (this.loadFlushLock)
        {
            Cacheable tileData;
            
            synchronized (this.fileLock)
            {
                tileData = readTileData(tile, url);
            }

            if (tileData == null)
            {
                return false;
            }

            addTileToCache(tile, tileData);
            return true;
        }
    }

    protected Cacheable readTileData(WFSBaseTile tile, java.net.URL url)
    {
        java.io.InputStream is = null;

        try
        {
            String path = url.getFile();

            path = path.replaceAll("%20", " "); // TODO: find a better way to get a path usable by FileInputStream

            java.io.FileInputStream fis = new java.io.FileInputStream(path);
            java.io.BufferedInputStream buf = new java.io.BufferedInputStream(
                    fis);

            try {
            	is = new java.util.zip.GZIPInputStream(buf);
            } catch (IOException e) {
            	if (e.getMessage().contains("Not in GZIP format")) {
            		buf.close();
            		is = new java.io.BufferedInputStream(new java.io.FileInputStream(path));
            	}
            	else
				{
					Logger.getLogger(AbstractWFSLayer.class.getName()).log(Level.SEVERE, "Failed to read tile data : "+e.getMessage());
					return null;
				}
            }
            return createDataChunk(tile, GMLParser.parse(is));
        }
        catch (Exception e)
        {
            logger.log(Level.WARNING, "WFSLayer.ExceptionAttemptingToReadFile: " + url, e);
            WorldWind.getDataFileStore().removeFile(url);   
            getWFSService().markResourceAbsent(getWFSService().getTileNumber(tile.row, tile.column));
            logger.fine(Logging.getMessage("generic.DeletedCorruptDataFile", url));
        }
        finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
            }
            catch (java.io.IOException e)
            {
                logger.log(Level.INFO,
                        "WFSLayer.ExceptionAttemptingToReadFile: " + url, e);
            }
        }

        return null;
    }

    private void addTileToCache(WFSBaseTile tile, Cacheable tileData)
    {
        WorldWind.getMemoryCache(WFSBaseTile.class.getName()).add(tile, tileData);
    }

    private void downloadTile(final WFSBaseTile tile)
    {
        if (!WorldWind.getRetrievalService().isAvailable())
        {
            return;
        }

        java.net.URL url;

        try
        {
            url = tile.getRequestURL();
            if (WorldWind.getNetworkStatus().isHostUnavailable(url))
            {
                return;
            }
        }
        catch (java.net.MalformedURLException e)
        {
            logger.log(java.util.logging.Level.SEVERE,
                    Logging.getMessage(
                    "layers.TextureLayer.ExceptionCreatingTextureUrl", tile),
                    e);
            return;
        }

        Retriever retriever;

        if ("http".equalsIgnoreCase(url.getProtocol()))
        {
            retriever = new HTTPRetriever(url,
                    new DownloadPostProcessor(this, tile));
        }
        else
        {
            logger.severe(
                    Logging.getMessage(
                            "layers.TextureLayer.UnknownRetrievalProtocol",
                            url.toString()));
            return;
        }

        // Apply any overridden timeouts.
        Integer cto = AVListImpl.getIntegerValue(this, AVKey.URL_CONNECT_TIMEOUT);

        if (cto != null && cto > 0)
        {
            retriever.setConnectTimeout(cto);
        }
        Integer cro = AVListImpl.getIntegerValue(this, AVKey.URL_READ_TIMEOUT);

        if (cro != null && cro > 0)
        {
            retriever.setReadTimeout(cro);
        }
        Integer srl = AVListImpl.getIntegerValue(this,
                AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT);

        if (srl != null && srl > 0)
        {
            retriever.setStaleRequestLimit(srl);
        }

        WorldWind.getRetrievalService().runRetriever(retriever,
                tile.getPriority());
    }

    private void saveBuffer(java.nio.ByteBuffer buffer, java.io.File outFile) throws java.io.IOException
    {
        synchronized (this.fileLock) // sychronized with read of file in RequestTask.run()
        {
            WWIO.saveBuffer(buffer, outFile);
        }
    }

    private static class DownloadPostProcessor implements RetrievalPostProcessor
    {
        final AbstractWFSLayer layer;
        final WFSBaseTile tile;

        private DownloadPostProcessor(AbstractWFSLayer layer, WFSBaseTile tile)
        {
            this.layer = layer;
            this.tile = tile;
        }

        public java.nio.ByteBuffer run(Retriever retriever)
        {
            if (retriever == null)
            {
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                logger.fine(msg);
                throw new IllegalArgumentException(msg);
            }

            try
            {
                if (!retriever.getState().equals(
                        Retriever.RETRIEVER_STATE_SUCCESSFUL))
                {
                    return null;
                }

                URLRetriever r = (URLRetriever) retriever;
                ByteBuffer buffer = r.getBuffer();

                if (retriever instanceof HTTPRetriever)
                {
                    HTTPRetriever htr = (HTTPRetriever) retriever;

                    if (htr.getResponseCode()
                            == java.net.HttpURLConnection.HTTP_NO_CONTENT)
                    {
                        // Mark tile as missing to avoid further attempts
                        tile.layer.getWFSService().markResourceAbsent(
                                tile.layer.getWFSService().getTileNumber(tile.row,
                                tile.column));
                        return null;
                    }
                    else if (htr.getResponseCode()
                            != java.net.HttpURLConnection.HTTP_OK)
                    {
                        // Also mark tile as missing, but for an unknown reason.
                        tile.layer.getWFSService().markResourceAbsent(
                                tile.layer.getWFSService().getTileNumber(tile.row,
                                tile.column));
                        return null;
                    }
                }

                final java.io.File outFile = WorldWind.getDataFileStore().newFile(
                        this.tile.getFileCachePath());

                if (outFile == null)
                {
                    return null;
                }

                if (outFile.exists())
                {
                    return buffer;
                } // info is already here; don't need to do anything

                if (buffer != null)
                {
                    String contentType = retriever.getContentType();

                    // System.out.println("placenamelayer content type: "+contentType);
                    if (contentType == null)
                    {
                        // TODO: logger message
                        return null;
                    }

                    this.layer.saveBuffer(buffer, outFile);
                    this.layer.firePropertyChange(AVKey.LAYER, null, this);
                    return buffer;
                }
            }
            catch (java.io.IOException e)
            {
                tile.layer.getWFSService().markResourceAbsent(
                        tile.layer.getWFSService().getTileNumber(tile.row, tile.column));
                logger.log(Level.FINE,
                        Logging.getMessage(
                        "WFSLayer.ExceptionSavingRetrievedFile",
                        this.tile.getFileCachePath()),
                        e);
            }

            return null;
        }
    }
    
    /**
     * Constructs a per-feature Style, or returns layer default style. 
     * If styleString is null or there's an error parsing styleString, returns layer default style.
     * Otherwise, interprets it as KML description of style.
     * Override this to support style URLs!
     * @param styleString null
     * @return layer style
     */
    public KMLStyle getStyle(String styleString)
    {
        KMLStyle rv = this.defaultStyle;

        if (null != styleString)
        {
            try
            {
                rv = KMLStyleFactory.createStyle(styleString, this.defaultStyle);
            }
            catch (KMLParserException e)
            {
                logger.warning(
                        "Error parsing style: error is " + e
                        + "; style is " + styleString);
            }
        }

        return rv;
    }
    
    @Override
    public void setExpiryTime(long expiryTime)
    {
        super.setExpiryTime(expiryTime);
        this.wfsService.setExpiryTime(expiryTime);
    }
    
}

