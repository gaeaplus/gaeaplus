package si.xlab.gaea.core.ogc.gml;

import org.w3c.dom.Node;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

public class GMLPoint extends GMLGeometry {
	private final LatLon coords;
	
	public GMLPoint(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_POINT)) 
			throw new GMLException("Not a point GML node");
		
		Node coordsNode = n.getFirstChild();
		if (coordsNode == null || !coordsNode.getNodeName().equals(GML_TAG_COORDINATES))
			throw new GMLException("Point should contain coordinates and nothing else.");
		
		this.coords = parseCoords(coordsNode.getNodeValue());
	}
	
	@Override 
	protected Sector calculateSector()
	{
		return new Sector(coords.latitude, coords.latitude, coords.longitude, coords.longitude);
	}
	
	@Override
	public LatLon getCentroid()
	{
		return this.coords;
	}
}
