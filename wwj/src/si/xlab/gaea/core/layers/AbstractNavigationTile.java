/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.layers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.WWMath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is a generalization of the PlaceNameLayer.NavigationTile subclass.
 * It is used to implement a quad-tree of tiles, where only the lowest-level tiles
 * are actually downloaded from the server (ie there's no tile pyramid as such
 * but total number of tiles is too large to use an array).
 * @author marjan
 */
public abstract class AbstractNavigationTile<BaseTile>
{
    protected static final Logger LOG = Logger.getLogger(AbstractNavigationTile.class.getName());

    protected final LatLon tileDelta;
    private final Sector navSector;
    private final int level;

    private final String id;
    private List<AbstractNavigationTile<BaseTile>> subNavTiles
            = new ArrayList<AbstractNavigationTile<BaseTile>>();

    private List<BaseTile> baseTiles = null;

    /**
     * Constructs a top-level navigation tile.
     * The number of quad-tree levels is chosen to suit tileDelta and sector.
     * @param tileDelta size of underlying physical tiles
     * @param sector area covered by this navigation tile
     * @param memoryCache cache used for physical tiles
     * @param id
     */
    protected AbstractNavigationTile(LatLon tileDelta, Sector sector)
    {
        this(tileDelta, sector,
                (int) Math.log(Math.max(1, sector.getDeltaLatDegrees() / tileDelta.getLatitude().getDegrees())),
                "top");
    }

    /**
     * Creates a non-top-level navigation tile.
     * @param tileDelta
     * @param sector
     * @param levels
     * @param id
     */
    protected AbstractNavigationTile(LatLon tileDelta, Sector sector, int levels, String id)
    {
        this.tileDelta = tileDelta;
        this.id = id;
        this.navSector = sector;
        this.level = levels;
    }

    /*
     * should create a non-top-level instance of a descendant class 
     */
    protected abstract AbstractNavigationTile<BaseTile> createInstance(
            Sector sector, int level, String id);

    protected void buildSubNavTiles()
    {
        if (this.level > 0)
        {
            //split sector, create a navTile for each quad
            Sector[] subSectors = this.navSector.subdivide();
            for (int j = 0; j < subSectors.length; j++)
            {
                this.subNavTiles.add(createInstance(
                        subSectors[j], this.level-1, this.id + "." + j));
            }
        }
    }

    /**
     * Returns all visible bottom-level navigation tiles in the (sub-)tree whose root is this.
     * Visibility is calculated according to the given dc and min/max distances.
     * @param dc
     * @param minDistSquared
     * @param maxDistSquared
     * @return
     */
    public List<AbstractNavigationTile<BaseTile>> getVisibleNavTiles(DrawContext dc, double minDistSquared, double maxDistSquared)
    {
        ArrayList<AbstractNavigationTile<BaseTile>> navList = new ArrayList<AbstractNavigationTile<BaseTile>>();
        if (isNavSectorVisible(dc, navSector, minDistSquared, maxDistSquared))
        {
            if (this.level > 0 && !this.hasSubTiles())
                this.buildSubNavTiles();

            if (this.hasSubTiles())
            {
                for (AbstractNavigationTile subNavTile : subNavTiles)
                {
                    navList.addAll(subNavTile.getVisibleNavTiles(dc, minDistSquared, maxDistSquared));
                }
            }
            else  //at bottom level navigation tile
            {
                navList.add(this);
            }
        }

        return navList;
    }

    //Added for bulk download, does not use eye position to determine visibility
    //todo validate sector
    public List<AbstractNavigationTile<BaseTile>> navTilesVisible(Sector sector)
    {
        ArrayList<AbstractNavigationTile<BaseTile>> navList = new ArrayList<AbstractNavigationTile<BaseTile>>();
        if (navSector.intersects(sector))
        {
            if (this.level > 0 && !this.hasSubTiles())
                this.buildSubNavTiles();

            if (this.hasSubTiles())
            {
                for (AbstractNavigationTile nav : subNavTiles)
                {
                    navList.addAll(nav.navTilesVisible(sector));
                }
            }
            else  //at bottom level navigation tile
            {
                navList.add(this);
            }
        }

        return navList;
    }

    public boolean hasSubTiles()
    {
        return !subNavTiles.isEmpty();
    }

    protected List<AbstractNavigationTile<BaseTile>> getChildren()
    {
        return Collections.unmodifiableList(subNavTiles);
    }
    
    public boolean isBottomLevel()
    {
        return level == 0;
    }
    
    private static Angle clampAngle(Angle orig, Angle min, Angle max)
    {
        return Angle.fromDegrees(WWMath.clamp(orig.degrees, min.degrees, max.degrees));
    }
    
    public static boolean isSectorVisible(DrawContext dc, Sector sector, double minDistanceSquared, double maxDistanceSquared)
    {
        if (!sector.intersects(dc.getVisibleSector()))
            return false;

        View view = dc.getView();
        Position eyePos = view.getEyePosition();
        if (eyePos == null)
            return false;

        //check for eyePos over globe
        if (Double.isNaN(eyePos.getLatitude().getDegrees()) || Double.isNaN(eyePos.getLongitude().getDegrees()))
            return false;

        Angle lat = clampAngle(eyePos.getLatitude(), sector.getMinLatitude(),
            sector.getMaxLatitude());
        Angle lon = clampAngle(eyePos.getLongitude(), sector.getMinLongitude(),
            sector.getMaxLongitude());
        Vec4 p = dc.getGlobe().computePointFromPosition(lat, lon, 0d);
        double distSquared = dc.getView().getEyePoint().distanceToSquared3(p);
        //noinspection RedundantIfStatement
        if (minDistanceSquared > distSquared || maxDistanceSquared < distSquared)
            return false;

        return true;
    }

	public boolean isNavSectorVisible(DrawContext dc, Sector sector, double minDistanceSquared, double maxDistanceSquared)
    {
		Sector s = new Sector(sector.getMinLatitude().subtract(tileDelta.latitude), sector.getMaxLatitude().add(tileDelta.latitude),
							  sector.getMinLongitude().subtract(tileDelta.longitude), sector.getMaxLongitude().add(tileDelta.longitude));
        if (!s.intersects(dc.getVisibleSector()))
            return false;

        View view = dc.getView();
        Position eyePos = view.getEyePosition();
        if (eyePos == null)
            return false;

        //check for eyePos over globe
        if (Double.isNaN(eyePos.getLatitude().getDegrees()) || Double.isNaN(eyePos.getLongitude().getDegrees()))
            return false;

        Angle lat = clampAngle(eyePos.getLatitude(), sector.getMinLatitude(),
            sector.getMaxLatitude());
        Angle lon = clampAngle(eyePos.getLongitude(), sector.getMinLongitude(),
            sector.getMaxLongitude());
        Vec4 p = dc.getGlobe().computePointFromPosition(lat, lon, 0d);
        double distSquared = dc.getView().getEyePoint().distanceToSquared3(p);
        //noinspection RedundantIfStatement
        if (minDistanceSquared > distSquared || maxDistanceSquared < distSquared)
            return false;

        return true;
    }

    /*
     * should create a base tile, i.e. underlying physical tile (in contrast navigation tile)
     */
    protected abstract BaseTile createBaseTile(Sector sector);

    private void buildBaseTiles()
    {
        baseTiles = new ArrayList<BaseTile>();

        final Angle dLat = tileDelta.getLatitude();
        final Angle dLon = tileDelta.getLongitude();

        // Determine the row and column offset from the global tiling origin for the southwest tile corner
        int firstRow = Tile.computeRow(dLat, navSector.getMinLatitude(), Angle.NEG90);
        int firstCol = Tile.computeColumn(dLon, navSector.getMinLongitude(), Angle.NEG180);
        int lastRow = Tile.computeRow(dLat, navSector.getMaxLatitude().subtract(dLat), Angle.NEG90);
		int lastCol = Tile.computeColumn(dLon, navSector.getMaxLongitude().subtract(dLon), Angle.NEG180);
		
		int firstRowOfUpperTile = Tile.computeRow(dLat, navSector.getMaxLatitude(), Angle.NEG90);
		if(firstRowOfUpperTile - lastRow > 1){
			lastRow++;
		}

		int firstColOfRightTile = Tile.computeRow(dLon, navSector.getMaxLongitude(), Angle.NEG180);
		if(firstColOfRightTile - lastCol > 1){
			lastCol++;
		}

        int nLatTiles = lastRow - firstRow + 1;
        int nLonTiles = lastCol - firstCol + 1;

        Tile[] tiles = new Tile[nLatTiles * nLonTiles];

        Angle p1 = Tile.computeRowLatitude(firstRow, dLat, Angle.NEG90);
        for (int row = 0; row <= lastRow - firstRow; row++)
        {
            Angle p2;
            p2 = p1.add(dLat);

            Angle t1 = Tile.computeColumnLongitude(firstCol, dLon, Angle.NEG180);
            for (int col = 0; col <= lastCol - firstCol; col++)
            {
                Angle t2;
                t2 = t1.add(dLon);
                baseTiles.add(createBaseTile(new Sector(p1, p2, t1, t2)));
                t1 = t2;
            }
            p1 = p2;
        }

    }

    /**
     * Returns (and creates if not yet created) all base tiles of this sub-tree.
     * Be careful when calling this on non-bottom-level navigation tile!
     * @return
     */
    public List<BaseTile> getBaseTiles()
    {
        List<BaseTile> rv = new ArrayList<BaseTile>();

        if (this.level > 0)
        {
            for (AbstractNavigationTile subNavTile : this.subNavTiles)
                rv.addAll(subNavTile.getBaseTiles());
            return rv;
        } else {
            if (this.baseTiles == null)
                buildBaseTiles();

            rv.addAll(this.baseTiles);
            return rv;
        }
    }
}