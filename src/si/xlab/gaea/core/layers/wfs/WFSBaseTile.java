package si.xlab.gaea.core.layers.wfs;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.util.logging.Logger;

/**
 * The actual tile served by WFS (as opposed to WFSNavigationTile,
 * which is just an internal quad-tree node and contains no features)
 * @author marjan
 */
public class WFSBaseTile {
    private static final Logger LOG = Logger.getLogger(WFSBaseTile.class.getName());

    final AbstractWFSLayer layer;

    public Sector sector;
    final int row;
    final int column;

    // Computed data.
    private final String fileCachePath;
    private final int hash;
    private Extent extent = null;
    double extentVerticalExaggeration = Double.MIN_VALUE;
    private Vec4 centroid; // Cartesian coordinate of lat/lon center
    private double priority = Double.MAX_VALUE; // Default is minimum priority

    /**
     * Construct a new WFS tile.
     * The reference to the container layer is necessary so that a Tile equals() another Tile only
     * if they're part of the same layer, because the Tile is used as key in the (HashMap) memory cache,
     * and memory caches for layers must be separate (because e.g. the same database table can be shown
     * as Icons and Buildings at the same time, which are not compatible at the memory-cache level)
     * @param layer layer containing the tile
     * @param sector
     * @param row
     * @param column
     */
    public WFSBaseTile(AbstractWFSLayer layer, Sector sector, int row, int column)
    {
        this.layer = layer;
        this.sector = sector;
        this.row = row;
        this.column = column;
        this.fileCachePath = layer.getWFSService().createFileCachePathFromTile(
                row, column);
        this.hash = (this.fileCachePath != null ? this.fileCachePath.hashCode() : 0);
    }

    @Override
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

        final WFSBaseTile tile = (WFSBaseTile) o;

        if (this.layer != tile.layer)
        {
            return false;
        }

        return !(this.fileCachePath != null
                ? !this.fileCachePath.equals(tile.fileCachePath)
                : tile.fileCachePath != null);
    }

    protected java.net.URL getRequestURL() throws java.net.MalformedURLException
    {
        return this.layer.getWFSService().createServiceURLFromSector(this.sector);
    }

    public String getFileCachePath()
    {
        return this.fileCachePath;
    }
    
    protected Sector getSector()
    {
        return sector;
    }

    @Override
    public int hashCode()
    {
        return this.hash;
    }

    protected boolean isTileInMemory()
    {
        return WorldWind.getMemoryCache(WFSBaseTile.class.getName()).getObject(this)
                != null;
    }

    Cacheable getData()
    {
        return (Cacheable) WorldWind.getMemoryCache(WFSBaseTile.class.getName()).getObject(
                this);
    }

    public Vec4 getCentroidPoint(Globe globe)
    {
        if (globe == null)
        {
            String msg = Logging.getMessage("nullValue.GlobeIsNull");

            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (this.centroid == null)
        {
            LatLon c = this.getSector().getCentroid();

            this.centroid = globe.computePointFromPosition(c.getLatitude(),
                    c.getLongitude(), 0);
        }

        return this.centroid;
    }

    public double getPriority()
    {
        return priority;
    }

    public void setPriority(double priority)
    {
        this.priority = priority;
    }

    protected boolean isVisible(DrawContext dc,
            double minDistanceSquared, double maxDistanceSquared)
    {
        return WFSNavigationTile.isSectorVisible(dc, sector, minDistanceSquared, maxDistanceSquared);
    }

    void flush()
    {
        if (isTileInMemory())
        {
            WorldWind.getMemoryCache(WFSBaseTile.class.getName()).remove(this);
        }
    }

}
