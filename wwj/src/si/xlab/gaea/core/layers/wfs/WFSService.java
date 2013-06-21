package si.xlab.gaea.core.layers.wfs;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.AbsentResourceList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.WWIO;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;


/**
 * @author marjan
 * Modelled after NASA's PlaceNameService
 * doesn't do anything on the net; merely construct request URLs etc
 */
public class WFSService
{
	protected static final Logger logger = Logger.getLogger(WFSService.class.getName());

    // Data retrieval and caching attributes.
    private final String service;
    private final String dataset;
    private final String urlBase; //full URL of a data ending with "&BBOX=", such that only the coordinates need to be appended
    private final String fileCachePath;
    private long expiryTime = 0;
    private static final String FORMAT_SUFFIX = ".xml.gz";
    // Geospatial attributes.
    private final Sector sector;
    private final LatLon tileDelta;
    private Extent extent = null;
    private double extentVerticalExaggeration = Double.MIN_VALUE;
    // Display attributes.
    private boolean enabled;
    private double minDisplayDistance;
    private double maxDisplayDistance;
    private int numColumns;
    private static final int MAX_ABSENT_TILE_TRIES = 2;
    private static final int MIN_ABSENT_TILE_CHECK_INTERVAL = 10000;
    private final AbsentResourceList absentTiles = new AbsentResourceList(
            MAX_ABSENT_TILE_TRIES, MIN_ABSENT_TILE_CHECK_INTERVAL);

    private static final Angle MIN_TILE_DELTA = Angle.fromDegrees(0.001);
    
    /**
     * @param service
     * @param dataset
     * @param fileCachePath
     * @param sector
     * @param tileDelta
     * @param font
     * @throws IllegalArgumentException if any parameter is null
     */
    public WFSService(String service, String dataset,
            Sector sector, Angle tileDelta)
    {
        // Data retrieval and caching attributes.
        this.service = service;
        this.dataset = dataset;

        StringBuilder urlBase = new StringBuilder(this.service);
        if (!this.service.endsWith("?") && !this.service.endsWith("&"))
        {
            if (this.service.contains("?"))
                urlBase.append("&");
            else
                urlBase.append("?");
        }
        urlBase.append("Service=WFS&version=1.0.0&Request=GetFeature");
        urlBase.append("&TypeName=").append(dataset);
        urlBase.append("&srsname=EPSG:4326");
        urlBase.append("&OUTPUTFORMAT=GML2-GZIP");
        urlBase.append("&BBOX=");
        this.urlBase = urlBase.toString();
        
        URI mapRequestURI = null;
        try
        {
            mapRequestURI = new URI(service);
        }
        catch (URISyntaxException e)
        {
            String message = Logging.getMessage(
                    "WFSService.URISyntaxException: ", e);

            Logging.logger().severe(message);
        }
        
        // Geospatial attributes.
        if (tileDelta.compareTo(MIN_TILE_DELTA) == -1)
        {
            logger.warning("Tile delta too low for dataset " + dataset + ", " + MIN_TILE_DELTA.toString() + " will be used instead.");
            tileDelta = MIN_TILE_DELTA;
        }
        this.tileDelta = new LatLon(tileDelta, tileDelta);
        this.sector = roundSectorToTileDelta(sector, this.tileDelta);
        
        // Display attributes.
        this.enabled = true;
        this.minDisplayDistance = Double.MIN_VALUE;
        this.maxDisplayDistance = Double.MAX_VALUE;

        //cache path
        if (mapRequestURI != null)
        {
            this.fileCachePath = WWIO.formPath(mapRequestURI.getAuthority(),
                    mapRequestURI.getPath(), dataset, canonicalAngle2str(tileDelta));
        }
        else
        {
            this.fileCachePath = WWIO.formPath(service, dataset, canonicalAngle2str(tileDelta));
        }
        
        String message = this.validate();

        if (message != null)
        {
            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.numColumns = this.numColumnsInLevel();
    }
    
    private String canonicalAngle2str(Angle angle)
    {
        //avoid scientific notation, otherwise let Double.toString() do its thing
        double d = angle.degrees;
        if (d >= 1)
            return Double.toString(d);

        String rv = "0.";
        while (Double.toString(d).contains("E"))
        {
            rv = rv + "0";
            d *= 10;
        }
        rv = rv + Double.toString(d).substring(2);
        while (rv.endsWith("0"))
            rv = rv.substring(0, rv.length()-1);
        
        return rv;
    }

    private Sector roundSectorToTileDelta(Sector sector, LatLon tileDelta)
    {
        //round the northeast corner to tileDelta (otherwise some tiles at north and east edge will be missing)
        //DO NOT round southwest corner (otherwise indices of files in cache will change, so invalidation would be required)
        return Sector.fromDegrees(
                sector.getMinLatitude().degrees,
                ceilDeg( sector.getMaxLatitude(), tileDelta.latitude),
                sector.getMinLongitude().degrees,
                ceilDeg( sector.getMaxLongitude(), tileDelta.longitude));

 /*    when the abovementioned problem is fixed, round all corners to tileDelta:
        return Sector.fromDegrees(
                floorDeg(sector.getMinLatitude(), tileDelta.latitude),
                ceilDeg( sector.getMaxLatitude(), tileDelta.latitude),
                floorDeg(sector.getMinLongitude(), tileDelta.longitude),
                ceilDeg( sector.getMaxLongitude(), tileDelta.longitude));*/
    }

    private double floorDeg(Angle a, Angle delta)
    {
        return (delta.degrees*Math.floor(a.degrees/delta.degrees));
    }

    private double ceilDeg(Angle a, Angle delta)
    {
        return (delta.degrees*Math.ceil(a.degrees/delta.degrees));
    }

    /**
     * @param row
     * @param column
     * @return
     * @throws IllegalArgumentException if either <code>row</code> or <code>column</code> is less than zero
     */
    public String createFileCachePathFromTile(int row, int column)
    {
        if (row < 0 || column < 0)
        {
            String message = Logging.getMessage(
                    "PlaceNameService.RowOrColumnOutOfRange", row, column);

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        StringBuilder sb = new StringBuilder(this.fileCachePath);

        sb.append(java.io.File.separator).append(this.dataset);
        sb.append(java.io.File.separator).append(row);
        sb.append(java.io.File.separator).append(row).append('_').append(column);
        sb.append(FORMAT_SUFFIX);

        String path = sb.toString();

        return path.replaceAll("[:*?<>|]", "");
    }

    private int numColumnsInLevel()
    {
        int firstCol = Tile.computeColumn(this.tileDelta.getLongitude(), 
                sector.getMinLongitude(), Angle.NEG180);
        int lastCol = Tile.computeColumn(this.tileDelta.getLongitude(),
                sector.getMaxLongitude().subtract(this.tileDelta.getLongitude()), 
                Angle.NEG180);

        return lastCol - firstCol + 1;
    }

    public long getTileNumber(int row, int column)
    {
        return row * this.numColumns + column;
    }

    /**
     * @param sector
     * @return
     * @throws java.net.MalformedURLException
     * @throws IllegalArgumentException       if <code>sector</code> is null
     */
    public java.net.URL createServiceURLFromSector(Sector sector) throws java.net.MalformedURLException
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");

            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        StringBuilder sb = new StringBuilder(this.urlBase);
        sb.append(sector.getMinLongitude().getDegrees()).append(',');
        sb.append(sector.getMinLatitude().getDegrees()).append(',');
        sb.append(sector.getMaxLongitude().getDegrees()).append(',');
        sb.append(sector.getMaxLatitude().getDegrees()).append(",EPSG:4326");
        return new java.net.URL(sb.toString());
    }

    public synchronized final WFSService deepCopy()
    {
        WFSService copy = new WFSService(this.service, this.dataset, this.sector,
                this.tileDelta.getLatitude());

        copy.enabled = this.enabled;
        copy.minDisplayDistance = this.minDisplayDistance;
        copy.maxDisplayDistance = this.maxDisplayDistance;
        copy.expiryTime = this.expiryTime;
        return copy;
    }

    public final long getExpiryTime()
    {
        return this.expiryTime;
    }

    public final void setExpiryTime(long expiryTime)
    {
        this.expiryTime = expiryTime;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (o == null || this.getClass() != o.getClass())
        {
            return false;
        }

        final WFSService other = (WFSService) o;

        if (this.service != null
                ? !this.service.equals(other.service)
                : other.service != null)
        {
            return false;
        }

        if (this.dataset != null
                ? !this.dataset.equals(other.dataset)
                : other.dataset != null)
        {
            return false;
        }

        if (this.sector != null
                ? !this.sector.equals(other.sector)
                : other.sector != null)
        {
            return false;
        }

        if (this.tileDelta != null
                ? !this.tileDelta.equals(other.tileDelta)
                : other.tileDelta != null)
        {
            return false;
        }

        if (this.minDisplayDistance != other.minDisplayDistance)
        {
            return false;
        }

        // noinspection RedundantIfStatement
        if (this.maxDisplayDistance != other.maxDisplayDistance)
        {
            return false;
        }

        return true;
    }

    public final String getDataset()
    {
        return this.dataset;
    }

    /**
     * @param dc
     * @return
     * @throws IllegalArgumentException if <code>dc</code> is null
     */
    public final Extent getExtent(DrawContext dc)
    {
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        if (this.extent == null
                || this.extentVerticalExaggeration
                        != dc.getVerticalExaggeration())
        {
            this.extentVerticalExaggeration = dc.getVerticalExaggeration();
            this.extent = Sector.computeBoundingCylinder(dc.getGlobe(), 
                    this.extentVerticalExaggeration, this.sector);
        }

        return extent;
    }

    public final String getFileCachePath()
    {
        return this.fileCachePath;
    }

    public synchronized final double getMaxDisplayDistance()
    {
        return this.maxDisplayDistance;
    }

    public synchronized final double getMinDisplayDistance()
    {
        return this.minDisplayDistance;
    }

    public final LatLon getTileDelta()
    {
        return tileDelta;
    }

    public final Sector getSector()
    {
        return this.sector;
    }

    public final String getService()
    {
        return this.service;
    }

    @Override
    public int hashCode()
    {
        int result;

        result = (service != null ? service.hashCode() : 0);
        result = 29 * result
                + (this.dataset != null ? this.dataset.hashCode() : 0);
        result = 29 * result
                + (this.fileCachePath != null
                        ? this.fileCachePath.hashCode()
                        : 0);
        result = 29 * result
                + (this.sector != null ? this.sector.hashCode() : 0);
        result = 29 * result
                + (this.tileDelta != null ? this.tileDelta.hashCode() : 0);
        result = 29 * result + ((Double) minDisplayDistance).hashCode();
        result = 29 * result + ((Double) maxDisplayDistance).hashCode();
        return result;
    }

    public synchronized final boolean isEnabled()
    {
        return this.enabled;
    }

    public synchronized final void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @param maxDisplayDistance
     * @throws IllegalArgumentException if <code>maxDisplayDistance</code> is less than the current minimum display
     *                                  distance
     */
    public synchronized final void setMaxDisplayDistance(double maxDisplayDistance)
    {
        if (maxDisplayDistance < this.minDisplayDistance)
        {
            String message = Logging.getMessage(
                    "PlaceNameService.MaxDisplayDistanceLessThanMinDisplayDistance",
                    maxDisplayDistance, this.minDisplayDistance);

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.maxDisplayDistance = maxDisplayDistance;
    }

    /**
     * @param minDisplayDistance
     * @throws IllegalArgumentException if <code>minDisplayDistance</code> is less than the current maximum display
     *                                  distance
     */
    public synchronized final void setMinDisplayDistance(double minDisplayDistance)
    {
        if (minDisplayDistance > this.maxDisplayDistance)
        {
            String message = Logging.getMessage(
                    "PlaceNameService.MinDisplayDistanceGrtrThanMaxDisplayDistance",
                    minDisplayDistance, this.maxDisplayDistance);

            Logging.logger().severe(message);
            throw new IllegalArgumentException(message);
        }

        this.minDisplayDistance = minDisplayDistance;
    }

    public synchronized final void markResourceAbsent(long tileNumber)
    {
        this.absentTiles.markResourceAbsent(tileNumber);
    }

    public synchronized final boolean isResourceAbsent(long resourceNumber)
    {
        return this.absentTiles.isResourceAbsent(resourceNumber);
    }

    public synchronized final void unmarkResourceAbsent(long tileNumber)
    {
        this.absentTiles.unmarkResourceAbsent(tileNumber);
    }

    /**
     * Determines if this <code>PlaceNameService'</code> constructor arguments are valid.
     *
     * @return null if valid, otherwise a <code>String</code> containing a description of why it is invalid.
     */
    public final String validate()
    {
        String msg = "";

        if (this.service == null)
        {
            msg += Logging.getMessage("nullValue.ServiceIsNull") + ", ";
        }

        if (this.dataset == null)
        {
            msg += Logging.getMessage("nullValue.DataSetIsNull") + ", ";
        }

        if (this.fileCachePath == null)
        {
            msg += Logging.getMessage("nullValue.FileCachePathIsNull") + ", ";
        }

        if (this.sector == null)
        {
            msg += Logging.getMessage("nullValue.SectorIsNull") + ", ";
        }

        if (this.tileDelta == null)
        {
            msg += Logging.getMessage("nullValue.TileDeltaIsNull") + ", ";
        }

        if (msg.length() == 0)
        {
            return null;
        }

        return msg;
    }
}

