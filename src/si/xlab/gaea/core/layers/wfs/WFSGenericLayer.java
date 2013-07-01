package si.xlab.gaea.core.layers.wfs;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.cache.MemoryCache.CacheListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.WWIcon;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import si.xlab.gaea.core.ogc.gml.GMLBox;
import si.xlab.gaea.core.ogc.gml.GMLFeature;
import si.xlab.gaea.core.ogc.gml.GMLGeometry;
import si.xlab.gaea.core.ogc.gml.GMLGeometryCollection;
import si.xlab.gaea.core.ogc.gml.GMLLineString;
import si.xlab.gaea.core.ogc.gml.GMLMultiLineString;
import si.xlab.gaea.core.ogc.gml.GMLMultiPoint;
import si.xlab.gaea.core.ogc.gml.GMLMultiPolygon;
import si.xlab.gaea.core.ogc.gml.GMLPoint;
import si.xlab.gaea.core.ogc.gml.GMLPolygon;
import si.xlab.gaea.core.render.SelectableIcon;
import si.xlab.gaea.core.render.SelectableIconRenderer;
/*import si.xlab.gaea.core.render.SelectableIconRenderer;*/
import si.xlab.gaea.core.render.surfaceobjects.AbstractSurfaceLines;
import si.xlab.gaea.core.render.surfaceobjects.AbstractSurfacePolygons;
import si.xlab.gaea.core.render.surfaceobjects.BasicSurfaceLines;
import si.xlab.gaea.core.render.surfaceobjects.BasicSurfacePolygons;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceLineFloatingLabels;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceLineLabel;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceLineLabelRenderer;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceLineSegment;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceObject;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceObjectRenderer;
import si.xlab.gaea.core.render.surfaceobjects.SurfacePolygon;

/**
 *
 * @author vito
 */
public class WFSGenericLayer extends AbstractWFSLayer
{
    protected static final int DEFAULT_LAYER_ORDERING_POSITION = 0;
    protected static final Logger logger = Logger.getLogger(WFSGenericLayer.class.getName());
    private static WFSTileCacheListener TileCacheListener; //one cache listener for removing objects from the rendered textures and for unselecting icons in the tile being disposed

    protected SelectableIconRenderer iconRenderer; //icon renderer for just this layer
    private SurfaceLineLabelRenderer labelRenderer;
    
    protected SurfaceObjectRenderer surfaceObjectRenderer; //surface objects renderer
    protected boolean dynamicIconScaling = true;
    private final boolean drawLineLabels;
    private final String lineLabelGMLTag;
    private String featureDescriptionFormat;
    
    public WFSGenericLayer(WFSService wfsService,
            String layerName)
    {
        this(wfsService, DEFAULT_LAYER_ORDERING_POSITION, layerName);
    }

    public WFSGenericLayer(WFSService wfsService,
            int position,
            String layerName)
    {
        this(wfsService, position, layerName, false, null);
    }

    /**
     * Creates a new WFSGenericLayer.
     * The line labels, if enabled, are by default the feature names. If you supply a non-null
     * lineLabelGMLTagPattern, one of the attributes matching the pattern will be used
     * (to be specific, the first one in the ordering used by Map.keySet())
     * @param wfsService    the source of GML data
     * @param position      ordering in the list of all layers (determines what will be drawn under/over other stuff)
     * @param layerName
     * @param drawLineLabels    true if you want labels drawn on lines, e.g. for streets
     * @param lineLabelGMLTagPattern   regexp for GML tag from which to derive the line label
     */
    public WFSGenericLayer(WFSService wfsService, int position, String layerName,
            boolean drawLineLabels, String lineLabelGMLTag)
    {
        super(wfsService, layerName);

        this.drawLineLabels = drawLineLabels;
        this.lineLabelGMLTag = lineLabelGMLTag;

        this.iconRenderer = new SelectableIconRenderer();
        this.iconRenderer.setMaxVisibleDistance(this.getMaxActiveAltitude());

        this.labelRenderer = new SurfaceLineLabelRenderer();
        //this.lableRenderer.

        if (TileCacheListener == null)
        {
            TileCacheListener = new WFSTileCacheListener();
            WorldWind.getMemoryCache(WFSBaseTile.class.getName()).addCacheListener(TileCacheListener);
        }

        this.surfaceObjectRenderer = new SurfaceObjectRenderer();
		this.setPickEnabled(false);

    }

    @Override
    public void setMaxActiveAltitude(double maxActiveAltitude)
    {
        super.setMaxActiveAltitude(maxActiveAltitude);
        this.iconRenderer.setMaxVisibleDistance(maxActiveAltitude);
    }

    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
        this.surfaceObjectRenderer.setOpacity(opacity);
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        this.surfaceObjectRenderer.setEnabled(enabled);
		this.firePropertyChange(AVKey.LAYER, null, this);
    }

    @Override
    public void setDefaultStyle(KMLStyle style)
    {
        super.setDefaultStyle(style);
        flushTileData();
        surfaceObjectRenderer.redraw();
		this.firePropertyChange(AVKey.LAYER, null, this);
    }
    
    /*
     * Sets the format of feature description.
     * This is mostly useful for point layers, where description is shown when the user clicks on the icon representing the point feature.
     * The format is basic HTML and may contain placeholders for feature attributres, e.g $area$.
     */
    public void setFeatureDescriptionFormat(String featureDescriptionFormat)
    {
        this.featureDescriptionFormat = featureDescriptionFormat;
    }
    
    protected Cacheable createDataChunk(WFSBaseTile tile, List<GMLFeature> features)
    {
        List<SurfaceLineSegment> linesList =
                new ArrayList<SurfaceLineSegment>();
        List<SurfacePolygon> polygonsList = new ArrayList<SurfacePolygon>();
        List<WWIcon> iconsList = new ArrayList<WWIcon>();
        KMLStyle style = null;
        long iconsSize = 1; /* zero-sized object are rejected by the cache
        (and yes, we need an empty chunk in tiles with no data,
        otherwise it is handled as error) */

        for (GMLFeature feature : features)
        {
            iconsSize += createFeature(linesList, polygonsList, iconsList,
                    feature.getDefaultGeometry(), feature);
        }

        SurfaceObject lines = linesList.isEmpty() ? null : makeLines(linesList, tile.getSector(), getStyle(null));
        SurfaceObject polygons = polygonsList.isEmpty() ? null : makePolygons(polygonsList, tile.getSector(), getStyle(null));
        SurfaceLineLabel labels = (linesList.isEmpty() || !this.drawLineLabels) ? null : makeLineLables(linesList, getStyle(null));

        long polygonsSize = 0;
        if (polygons != null)
        {
            polygonsSize = polygons.getSize();
        }

        long linesSize = 0;
        if (lines != null)
        {
            linesSize = lines.getSize();
        }

        long labelsSize = 0;
        if(labels != null){
            labelsSize = labels.getSize();
        }
		
		//picking is needed only if layer has icons
		if(!iconsList.isEmpty()){
			this.setPickEnabled(true);
		}
		
		surfaceObjectRenderer.redraw(tile.getSector());
        return new GenericWFSChunk(new SurfaceObject[]{polygons, lines}, 
                                   new SurfaceLineLabel[]{labels},
                                   iconsList, iconsSize + polygonsSize + linesSize + labelsSize);
    }
    private static final int SIZE_OF_LATLON = 56; //4 doubles, plus a whole bunch of pointers

    /**
     * Creates one drawable object for each basic geometry element.
     * @param linesList     all created lines are added here
     * @param polygonsList  all created polygons are added here
     * @param iconsList     all created icons are added here
     * @param geometry      the geometry for which (or parts of which) to create the drawable objects
     * @param feature       the feature containing the geometry (used to get name, style etc)
     * @param tile          the drawing tile to which the created objects will later be added
     * @return
     */
    protected long createFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLGeometry geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;

        if (geometry instanceof GMLGeometryCollection)
        {
            iconsSizeInBytes += createGeometryCollectionFeature(
                    linesList, polygonsList, iconsList,
                    (GMLGeometryCollection) geometry, feature);
        } else if (geometry instanceof GMLMultiPoint)
        {
            iconsSizeInBytes += createMultiPointFeature(
                    linesList, polygonsList, iconsList,
                    (GMLMultiPoint) geometry, feature);
        } else if (geometry instanceof GMLMultiPolygon)
        {
            iconsSizeInBytes += createMultiPolyFeature(
                    linesList, polygonsList, iconsList,
                    (GMLMultiPolygon) geometry, feature);
        } else if (geometry instanceof GMLMultiLineString)
        {
            iconsSizeInBytes += createMultiLineFeature(
                    linesList, polygonsList, iconsList,
                    (GMLMultiLineString) geometry, feature);
        } else if (geometry instanceof GMLPoint)
        {
            iconsSizeInBytes += createPointFeature(
                    linesList, polygonsList, iconsList,
                    (GMLPoint) geometry, feature);
        } else if (geometry instanceof GMLPolygon)
        {
            iconsSizeInBytes += createPolyFeature(
                    linesList, polygonsList, iconsList,
                    (GMLPolygon) geometry, feature);
        } else if (geometry instanceof GMLLineString)
        {
            iconsSizeInBytes += createLineFeature(
                    linesList, polygonsList, iconsList,
                    (GMLLineString) geometry, feature);
        } else if (geometry instanceof GMLBox)
        {
            iconsSizeInBytes += createBoxFeature(
                    linesList, polygonsList, iconsList,
                    (GMLBox) geometry, feature);
        }

        return iconsSizeInBytes;
    }

    protected long createGeometryCollectionFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLGeometryCollection geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;
        for (GMLGeometry sub : geometry.getGeometries())
        {
            iconsSizeInBytes += createFeature(linesList, polygonsList, iconsList,
                    sub, feature);
        }
        return iconsSizeInBytes;
    }

    protected long createMultiPointFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLMultiPoint geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;
        for (GMLPoint sub : geometry.getPoints())
        {
            iconsSizeInBytes += createPointFeature(linesList, polygonsList, iconsList,
                    sub, feature);
        }
        return iconsSizeInBytes;
    }

    protected long createMultiLineFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLMultiLineString geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;
        for (GMLLineString sub : geometry.getLineStrings())
        {
            iconsSizeInBytes += createLineFeature(linesList, polygonsList, iconsList,
                    sub, feature);
        }
        return iconsSizeInBytes;
    }

    protected long createMultiPolyFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLMultiPolygon geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;
        for (GMLPolygon sub : geometry.getPolygons())
        {
            iconsSizeInBytes += createPolyFeature(linesList, polygonsList, iconsList,
                    sub, feature);
        }
        return iconsSizeInBytes;
    }

    protected long createPointFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLPoint geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;
        String desc = feature.buildDescription(featureDescriptionFormat);
        String imageURL = findFirstLinkedImage(desc);
        SelectableIcon icon = new SelectableIcon(
                getStyle(feature.getStyle()), new Position(geometry.getCentroid(), 0),
                feature.getName(), desc, imageURL,
                feature.getRelativeImportance(), dynamicIconScaling);

        iconsList.add(icon);
        iconsSizeInBytes += icon.getSizeInBytes();
        return iconsSizeInBytes;
    }

    public static String findFirstLinkedImage(String html)
    {
        String imageURL = null;
        Pattern imgPat = Pattern.compile(
                "<a href=\"([^\"]*)\\.((?i)JPG|JPEG|PNG|GIF)\">");
        Matcher imgMatcher = imgPat.matcher(html);
        if (imgMatcher.find() && imgMatcher.groupCount() >= 2)
        {
            imageURL = imgMatcher.group(1) + "." + imgMatcher.group(2);
        }
        return imageURL;
    }

    protected long createLineFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLLineString geometry, GMLFeature feature)
    {
        String name = feature.getName();
        if (this.lineLabelGMLTag != null)
        {
            for (String attrKey : feature.getAttributes().keySet())
            {
                if (Pattern.matches(this.lineLabelGMLTag, attrKey))
                {
                    name = feature.getAttributes().get(attrKey);
                    break;
                }
            }
        }
        
        KMLStyle style = null;
        if (feature.getStyle() != null)
        { 
            style = getStyle(feature.getStyle());
        }

        linesList.add(new SurfaceLineSegment(geometry.getPoints(), name, style));

        return 0;
    }

    protected long createPolyFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLPolygon geometry, GMLFeature feature)
    {
        int pointCount = geometry.getOuterRing().getVertices().size();

        List<List<LatLon>> innerRings = new ArrayList<List<LatLon>>();
        for (GMLPolygon.LinearRing innerRing : geometry.getInnerRings())
        {
            List<LatLon> contour = new ArrayList<LatLon>();
            for (LatLon ll : innerRing.getVertices())
            {
                contour.add(ll);
                pointCount++;
            }
            innerRings.add(contour);
        }

        KMLStyle style = getStyle(feature.getStyle());

        polygonsList.add(new SurfacePolygon(feature.getName(),
                geometry.getOuterRing().getVertices(), innerRings, style));
        return 0;
    }

    protected long createBoxFeature(
            List<SurfaceLineSegment> linesList,
            List<SurfacePolygon> polygonsList,
            List<WWIcon> iconsList,
            GMLBox geometry, GMLFeature feature)
    {
        long iconsSizeInBytes = 0;
        ArrayList<LatLon> corners = new ArrayList<LatLon>();
        corners.add(geometry.getSector().getSouthWest());
        corners.add(geometry.getSector().getSouthEast());
        corners.add(geometry.getSector().getNorthEast());
        corners.add(geometry.getSector().getNorthWest());
        corners.add(geometry.getSector().getSouthWest());
        polygonsList.add(new SurfacePolygon(feature.getName(), corners, null));
        iconsSizeInBytes += corners.size() * SIZE_OF_LATLON;
        return iconsSizeInBytes;
    }

    protected AbstractSurfaceLines makeLines(List<SurfaceLineSegment> linesList,
            Sector tileSector, KMLStyle style)
    {
        AbstractSurfaceLines lines = null;
        lines = new BasicSurfaceLines(linesList, tileSector, style);
        return lines;
    }

    protected SurfaceLineFloatingLabels makeLineLables(List<SurfaceLineSegment> linesList, KMLStyle style)
    {
        SurfaceLineFloatingLabels fll = new SurfaceLineFloatingLabels(linesList, null, style);
        return fll;
    }

    protected AbstractSurfacePolygons makePolygons(List<SurfacePolygon> polygonsList,
            Sector tileSector, KMLStyle style)
    {

        return new BasicSurfacePolygons(polygonsList, tileSector, style, true);
    }

    protected class GenericWFSChunk implements Cacheable
    {
        protected final SurfaceObject[] surfaceObjects;
        protected final SurfaceLineLabel[] labels;
        protected final List<WWIcon> icons;
        private final long approxSizeInBytes;
        private boolean hasRendered;
        
        public GenericWFSChunk(SurfaceObject[] surfaceObjects, 
                               SurfaceLineLabel[] labels, 
                               List<WWIcon> icons, long approxSizeInBytes)
        {
            this.surfaceObjects = surfaceObjects;
            this.labels = labels;
            this.icons = icons;
            this.approxSizeInBytes = approxSizeInBytes;
            this.hasRendered = false;
        }

        public void preRender(DrawContext dc){
            for(SurfaceLineLabel sll : labels){
                if(sll != null){
                    labelRenderer.addToRenderQueue(sll);
                }
            }
        }

        public void render(DrawContext dc)
        {
            if (icons != null)
            {
                dc.setCurrentLayer(WFSGenericLayer.this);
                iconRenderer.render(dc, icons);
            }
            
            for(SurfaceObject so : surfaceObjects){
                if(so != null){
                    surfaceObjectRenderer.addToRenderQueue(so);
                }
            }
            
            if (!hasRendered)
            {
                //on the first render we have to trigger another redraw, so that RenderToTextureLayer will get redrawn
    			WFSGenericLayer.this.firePropertyChange(AVKey.LAYER, null, this);
                hasRendered = true;
            }
        }

        public void pick(DrawContext dc, Point pickPoint)
        {
            if (icons != null)
            {
                iconRenderer.pick(dc, icons, pickPoint, WFSGenericLayer.this);
            }
        }

        public long getSizeInBytes()
        {
            return approxSizeInBytes;
        }
        
        public void dispose()
        {
            if(surfaceObjects != null){
                for(SurfaceObject so : surfaceObjects){
                    if (so != null) so.dispose();
                }
            }
            
            if (icons != null)
            {
                for (WWIcon icon: icons)
                {
                    SelectableIcon selectableIcon = (SelectableIcon)icon;
                    if (selectableIcon != null && selectableIcon.isSelected())
                        selectableIcon.unselect();
                }
            }
        }
    }

    public static class WFSTileCacheListener implements CacheListener
    {
        public void entryRemoved(Object key, Object clientObject)
        {
            if (clientObject instanceof GenericWFSChunk)
            {
                ((GenericWFSChunk) clientObject).dispose();
            }
        }

        public void removalException(Throwable exception, Object key, Object clientObject)
        {
            logger.warning("Exception when removing object from cache: " + exception.getMessage());
        }
    }

	@Override
	protected void doPreRender(DrawContext dc){

        if(dc.isPickingMode()){
            return;
        }
        if(dc.isShadowMode()){
            return;
        }
        super.doPreRender(dc);
        this.labelRenderer.preRender(dc);
	}

    @Override
    protected void doPreRenderTile(DrawContext dc, WFSBaseTile tile)
    {
        ((GenericWFSChunk) tile.getData()).preRender(dc);
    }

    @Override
    protected void doRender(DrawContext dc){

        super.doRender(dc);
        
        if(dc.isPickingMode()){
            return;
        }
        if(dc.isShadowMode()){
            return;
        }
        this.labelRenderer.render(dc);
    }
    
    @Override
    protected void doRenderTile(DrawContext dc, WFSBaseTile tile)
    {
        ((GenericWFSChunk) tile.getData()).render(dc);
    }

    @Override
    protected void doPickTile(DrawContext dc, WFSBaseTile tile, Point pickPoint)
    {
        ((GenericWFSChunk) tile.getData()).pick(dc, pickPoint);
    }

    @Override
    public void dispose()
    {
        MemoryCache m = WorldWind.getMemoryCache(WFSBaseTile.class.getName());
        m.clear();
        super.dispose();
    }
}
