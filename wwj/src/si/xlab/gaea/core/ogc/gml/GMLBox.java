package si.xlab.gaea.core.ogc.gml;

import java.util.List;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

import org.w3c.dom.Node;

public class GMLBox extends GMLGeometry {

	private final Sector box;

	public GMLBox(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_BOX)) 
			throw new GMLException("Not a box GML node");
		
		Node coordsNode = n.getFirstChild();
		if (coordsNode == null || !coordsNode.getNodeName().equals(GML_TAG_COORDINATES))
			throw new GMLException("Box should contain coordinates and nothing else.");
		
		List<LatLon> coords = parseMultiCoords(coordsNode.getNodeValue());
		if (coords.size() != 2)
			throw new GMLException("Box should contain 2 coordinates (ie 2 lats and 2 lons");
		
		this.box = new Sector(coords.get(0).getLatitude(), coords.get(1).getLatitude(),
				coords.get(0).getLongitude(), coords.get(1).getLongitude());
	}
	
	@Override
    protected Sector calculateSector()
	{
		return this.box;
	}
}
