package si.xlab.gaea.core.layers.wfs;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import java.util.ArrayList;
import java.util.List;

import si.xlab.gaea.core.layers.AbstractNavigationTile;

/**
 * Quad-tree of WFSBaseTiles
 * @author marjan
 */
public class WFSNavigationTile extends AbstractNavigationTile<WFSBaseTile>
{
    private final AbstractWFSLayer layer;

    protected WFSNavigationTile createInstance(Sector sector, int level, String id)
    {
        return new WFSNavigationTile(this.tileDelta, sector, level, id, this.layer);
    }

    public WFSNavigationTile(LatLon tileDelta, Sector sector, AbstractWFSLayer layer)
    {
        super(tileDelta, sector);
        this.layer = layer;
    }

    protected WFSNavigationTile(LatLon tileDelta, Sector sector,
            int level, String id, AbstractWFSLayer layer)
    {
        super(tileDelta, sector, level, id);
        this.layer = layer;
    }

    protected WFSBaseTile createBaseTile(Sector sector)
    {
        final Angle dLat = this.layer.getWFSService().getTileDelta().getLatitude();
        final Angle dLon = this.layer.getWFSService().getTileDelta().getLongitude();

        int row = Tile.computeRow(dLat, sector.getCentroid().getLatitude(),
                this.layer.getWFSService().getSector().getMinLatitude());
        int col = Tile.computeColumn(dLon, sector.getCentroid().getLongitude(),
                this.layer.getWFSService().getSector().getMinLongitude());

        return new WFSBaseTile(this.layer, sector, row, col);
    }

    /**
     * Returns all visible base tiles in the (sub-)tree whose root is this.
     * Visibility is calculated according to the given dc and min/max distances.
     * @param dc
     * @return
     */
    public List<WFSBaseTile> getVisibleBaseTiles(DrawContext dc)
    {
        ArrayList<WFSBaseTile> rv = new ArrayList<WFSBaseTile>();

        double minDist = layer.getWFSService().getMinDisplayDistance();
        double maxDist = layer.getWFSService().getMaxDisplayDistance();
        double minDistSquared = minDist * minDist;
        double maxDistSquared = maxDist * maxDist;

        if (isSectorVisible(dc, layer.getWFSService().getSector(), minDistSquared, maxDistSquared))
        {
            for (AbstractNavigationTile navTile
                    : getVisibleNavTiles(dc, minDistSquared, maxDistSquared))
            {
                WFSNavigationTile wfsNavTile = (WFSNavigationTile)navTile;
                if (wfsNavTile == null)
                {
                    Logging.logger().warning("Expected a WFSNavigationTile instance"
                            + " but found " + navTile.getClass().getName());
                    continue;
                }

                for (WFSBaseTile baseTile : wfsNavTile.getBaseTiles())
                {
                    if (isSectorVisible(dc, baseTile.getSector(),
                            minDistSquared, maxDistSquared))
                        rv.add(baseTile);
                }
            }
        }

        return rv;
    }

    public void flushTileData()
    {
        if (this.isBottomLevel())
        {
            for (WFSBaseTile baseTile: getBaseTiles())
            {
                baseTile.flush();
            }
        } else {
            for (AbstractNavigationTile<WFSBaseTile> subNavTile: getChildren())
            {
                WFSNavigationTile wfsSubNavTile = (WFSNavigationTile)subNavTile;
                if (wfsSubNavTile != null)
                    wfsSubNavTile.flushTileData();
                else
                {
                    Logging.logger().severe("Expected a WFSNavigationTile instance"
                            + " but found " + subNavTile.getClass().getName());
                }
            }
        }
    }
}
