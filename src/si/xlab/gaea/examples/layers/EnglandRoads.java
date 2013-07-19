package si.xlab.gaea.examples.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;
import si.xlab.gaea.core.layers.wfs.WFSGenericLayer;
import si.xlab.gaea.core.layers.wfs.WFSService;
import si.xlab.gaea.core.ogc.gml.GMLFeature;
import si.xlab.gaea.core.ogc.gml.GMLLineString;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceLineSegment;
import si.xlab.gaea.core.render.surfaceobjects.SurfacePolygon;

/**
 *
 * @author marjan
 */
public class EnglandRoads extends WFSGenericLayer
{
    private static final Sector BOUNDING_BOX = Sector.FULL_SPHERE;
    private static final int MAX_DISTANCE = 33000;
    private static final String SERVER_URL = "http://www.osmgb.org.uk/ogc/wfs";
    private static final String NAME = "OpenStreetMap Labeled Roads in UK";
    private static final String ATTR_PREFIX = "OSM-GB-Vector:";
    private static final String LABLE_TAG = "name";

	private static final HashMap<String, KMLStyle> styles = new HashMap<String, KMLStyle>();

    boolean wasRendered = false;
    
    public EnglandRoads()
    {
        super(new WFSService(SERVER_URL, "OSM-GB-Vector:Lines27700", BOUNDING_BOX, Angle.fromDegrees(0.2)), NAME);
        this.getWFSService().setMaxDisplayDistance(MAX_DISTANCE);
        this.setMaxActiveAltitude(MAX_DISTANCE);
		this.setMaxVisibleDistance(MAX_DISTANCE * 1.6);
		this.setLineLabelTag(ATTR_PREFIX + LABLE_TAG);
		this.setOpacity(1.0);

        KMLStyle style = this.getDefaultStyle();
        style.getLineStyle().setField("color", "ff000000");
        style.getLineStyle().setField("width", 1.0);
        this.setDefaultStyle(style);

		KMLStyle s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ffc09b80");
        s.getLineStyle().setField("width", 6.0);
		styles.put("motorway", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ffc09b80");
        s.getLineStyle().setField("width", 3.0);
		styles.put("motorway_link", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff7fc97f");
        s.getLineStyle().setField("width", 6.0);
		styles.put("trunk", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff7fc97f");
        s.getLineStyle().setField("width", 3.0);
		styles.put("trunk_link", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff716de4");
        s.getLineStyle().setField("width", 5.0);
		styles.put("primary", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff716de4");
        s.getLineStyle().setField("width", 2.0);
		styles.put("primary_link", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff6fbffd");
        s.getLineStyle().setField("width", 2.0);
		styles.put("secondary", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff6fbffd");
        s.getLineStyle().setField("width", 2.0);
		styles.put("secondary_link", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff74fafc");
        s.getLineStyle().setField("width", 2.0);
		styles.put("tertiary", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff74fafc");
        s.getLineStyle().setField("width", 2.0);
		styles.put("tertiary_link", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "ff706de1");
        s.getLineStyle().setField("width", 2.0);
		styles.put("living_street", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2aeeeeee");
        s.getLineStyle().setField("width", 2.0);
		styles.put("pedestrian", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2affffff");
        s.getLineStyle().setField("width", 2.0);
		styles.put("residential", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2affffff");
        s.getLineStyle().setField("width", 1.0);
		styles.put("track", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2a727272");
        s.getLineStyle().setField("width", 1.0);
		styles.put("path", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2a727272");
        s.getLineStyle().setField("width", 1.0);
		styles.put("footway", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2a727272");
        s.getLineStyle().setField("width", 1.0);
		styles.put("cycleway", s);

		s = new KMLStyle(style);
        s.getLineStyle().setField("color", "2a727272");
        s.getLineStyle().setField("width", 1.0);
		styles.put("bridleway", s);
    }

	@Override
	protected long createLineFeature(List<SurfaceLineSegment> linesList, 
									 List<SurfacePolygon> polygonsList, 
									 List<WWIcon> iconsList, 
									 GMLLineString geometry, GMLFeature feature) {
		String name = feature.getName();
		String type = null;
		
        KMLStyle style = getStyle(feature.getStyle());
        
		if (this.lineLabelGMLTag != null)
        {
            for (String attrKey : feature.getAttributes().keySet())
            {
                if (Pattern.matches(this.lineLabelGMLTag, attrKey))
                {
					type = feature.getAttributes().get(ATTR_PREFIX + "highway");
					if(type != null && (type.equals("residential") | type.equals("pedestrian") | type.equals("secondary"))){
                    	name = feature.getAttributes().get(attrKey);
					}
					else{
						name = null;
					}
                }
				else if (Pattern.matches(ATTR_PREFIX + "highway", attrKey))
                {
                    type = feature.getAttributes().get(attrKey);
					if(styles.containsKey(type)){
						KMLStyle featureStyle = styles.get(type);
						style = featureStyle != null ? featureStyle : style;
					}
                }
            }
        }

        linesList.add(new SurfaceLineSegment(geometry.getPoints(), name, style));

        return 0;
	}

    @Override
    public void render(DrawContext dc)
    {
        if (isEnabled() && !wasRendered)
        {
            wasRendered = true;
            BasicOrbitView view = (BasicOrbitView)dc.getView();
            if (view != null) {
                view.addPanToAnimator(Position.fromDegrees(51.51, -0.066, 500), view.getHeading(), view.getPitch(), 800);
            } else {
                Logging.logger().warning("Cannot fly to layer - view is not a BasicOrbitView!");                
            }
        }
        super.render(dc);
    }
}
