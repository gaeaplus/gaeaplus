package si.xlab.gaea.core.ogc.gml;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;

/**
 * @author marjan
 * An abstract class from which all GML geometry types are derived
 */
public abstract class GMLGeometry {

    //static stuff**********************************
	public static final String
		GML_TAG_POINT			= "gml:Point",
		GML_TAG_MULTI_POINT		= "gml:MultiPoint",
		GML_TAG_BOX				= "gml:Box",
		GML_TAG_LINE_STRING		= "gml:LineString",
		GML_TAG_MULTI_LINE_STRING = "gml:MultiLineString",
		GML_TAG_POLYGON			= "gml:Polygon",
		GML_TAG_MULTI_POLYGON	= "gml:MultiPolygon",
		GML_TAG_GEOMETRY_COLLECTION = "gml:GeometryCollection",
		
		GML_TAG_GEOMETRY_MEMBER	= "gml:geometryMember",
		GML_TAG_POINT_MEMBER	= "gml:pointMember",
		GML_TAG_LINE_STRING_MEMBER	= "gml:lineStringMember",
		GML_TAG_POLYGON_MEMBER	= "gml:polygonMember",
		GML_TAG_OUTER_BOUNDARY_IS = "gml:outerBoundaryIs",
		GML_TAG_INNER_BOUNDARY_IS = "gml:innerBoundaryIs",
		GML_TAG_LINEAR_RING		= "gml:LinearRing",
		GML_TAG_COORDINATES		= "gml:coordinates",
		
		GML_ATTR_SRSNAME		= "srsName";
	
	private final static HashMap<String, Class<?>> supportedTypes = new HashMap<String, Class<?>>();
	static {
		supportedTypes.put(GML_TAG_POINT, GMLPoint.class);
		supportedTypes.put(GML_TAG_MULTI_POINT, GMLMultiPoint.class);
		supportedTypes.put(GML_TAG_POLYGON, GMLPolygon.class);
		supportedTypes.put(GML_TAG_MULTI_POLYGON, GMLMultiPolygon.class);
		supportedTypes.put(GML_TAG_LINE_STRING, GMLLineString.class);
		supportedTypes.put(GML_TAG_MULTI_LINE_STRING, GMLMultiLineString.class);
		supportedTypes.put(GML_TAG_BOX, GMLBox.class);
		supportedTypes.put(GML_TAG_GEOMETRY_COLLECTION, GMLGeometryCollection.class);
	}
	
	public static boolean supports(String tag)
	{
		return supportedTypes.containsKey(tag);
	}
	
	public static GMLGeometry parseGeometry(String defaultSrsName, Node n) throws GMLException
	{
		if (supports(n.getNodeName()))
		{
			try {
				Constructor<?> constructor = supportedTypes.get(n.getNodeName()).getConstructor(
				        new Class[] {String.class, GMLGeometry.class, Node.class});
				return (GMLGeometry)constructor.newInstance(defaultSrsName, null, n);
			} catch (Exception e) {
				throw new GMLException("Error parsing feature type " + n.getNodeName()
                                + ", error is: " + e.getMessage(), e);
			}
		} else {
			throw new GMLException("Unsupported feature of type " + n.getNodeName());
		}
	}

    //non-static stuff**********************************
    private final GMLGeometry parent;
	private final String srsName;
	
	private Sector sector; 

    protected GMLGeometry(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
    {
        Node srsNameAttr = n.getAttributes().getNamedItem(GML_ATTR_SRSNAME);
        if (srsNameAttr != null)
        {
            this.srsName = srsNameAttr.getNodeValue();
        } else {
            this.srsName = defaultSrsName;
        }

        if (this.srsName == null && parent == null)
        {
			throw new GMLException("Parentless geometry objects must have srsName attribute or default srsName must be supplied");
        }
		this.parent = parent;
		this.sector = null;
	}

	public GMLGeometry getParent()
	{
		return this.parent;
	}
	
	public String getSrsName()
	{
		if (this.srsName != null)
			return this.srsName;
		else
			return getParent().getSrsName();
	}
	
	public Sector getSector()
	{
		if (this.sector == null)
			this.sector = calculateSector();

		return this.sector;
	}
	
	protected abstract Sector calculateSector();
	
	public LatLon getCentroid()
	{
		return getSector().getCentroid();
	}
	
	@Override
	public String toString()
	{
		return this.getClass().getName() + " at " + getCentroid().toString();
	}
	
	
	protected static LatLon parseCoords(String s) throws GMLException
    {
        String[] coords = s.split(",");

        if (coords.length != 2)
        {
            throw new NumberFormatException(
                    coords.length + " coordinates; expected 2");
        }
    	
        try
        {
	        // in gml the lon comes before the lat!
	        double lon = Double.parseDouble(coords[0]);
	        double lat = Double.parseDouble(coords[1]);

	        //we don't check lat and lon for range because it might really be some other coordinates, not WGS;
	        //it's not strictly correct to use LatLon in those cases, but currently there are now such cases
	        //	(i.e. we parse all coordinates but only use the WGS ones)
            return LatLon.fromDegrees(lat, lon);
        }
        catch (NumberFormatException e)
        {
        	throw new GMLException("Error parsing coordinates (" + s + ")", e);
        }
    }

	protected static List<LatLon> parseMultiCoords(String s) throws GMLException
	{
		ArrayList<LatLon> rv = new ArrayList<LatLon>();
		
		String[] coordList = s.trim().split("\\s+");
		
		for (String coords : coordList)
			rv.add(parseCoords(coords));
		
		return rv;
	}
}
