package si.xlab.gaea.core.ogc.gml;

import java.util.Collections;
import java.util.List;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

import org.w3c.dom.Node;

public class GMLLineString extends GMLGeometry {

	private final List<LatLon> points;
	
	public GMLLineString(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_LINE_STRING)) 
			throw new GMLException("Not a line string GML node");
		
		Node coordsNode = n.getFirstChild();
		if (coordsNode == null || !coordsNode.getNodeName().equals(GML_TAG_COORDINATES))
			throw new GMLException("Line string should contain coordinates and nothing else.");
		
		this.points = parseMultiCoords(coordsNode.getNodeValue());
	}
	
	public List<LatLon> getPoints()
	{
		return Collections.unmodifiableList(points);
	}
	
	@Override
    protected Sector calculateSector()
    {
        if (this.getPoints().size() < 1)
            return null;
        else
        	return Sector.boundingSector(getPoints());
    }   
}
