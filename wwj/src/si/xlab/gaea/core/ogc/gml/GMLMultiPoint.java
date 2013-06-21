package si.xlab.gaea.core.ogc.gml;

import gov.nasa.worldwind.geom.Sector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

public class GMLMultiPoint extends GMLGeometry {
	
	private final List<GMLPoint> points;
	
	public GMLMultiPoint(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_MULTI_POINT)) 
			throw new GMLException("Not a multi-point GML node");
		
		this.points = new ArrayList<GMLPoint>();
		
		for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (!child.getNodeName().equals(GML_TAG_POINT_MEMBER))
				continue;
			
			this.points.add(new GMLPoint(defaultSrsName, this, child.getFirstChild()));
		}
	}
	
	public List<GMLPoint> getPoints()
	{
		return Collections.unmodifiableList(points);
	}
	
	@Override
    protected Sector calculateSector()
    {
    	Sector sector = null;
    	
    	for (GMLPoint point : getPoints())
    		sector = Sector.union(sector, point.getSector());
    	
    	return sector;
    }   
}
