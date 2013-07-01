package si.xlab.gaea.core.ogc.gml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

import gov.nasa.worldwind.geom.Sector;

public class GMLMultiPolygon extends GMLGeometry {
	
	private final List<GMLPolygon> polygons;
	
	public GMLMultiPolygon(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_MULTI_POLYGON)) 
			throw new GMLException("Not a multi-polygon GML node");
		
		this.polygons = new ArrayList<GMLPolygon>();
		
		for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (!child.getNodeName().equals(GML_TAG_POLYGON_MEMBER))
				continue;
			
			this.polygons.add(new GMLPolygon(defaultSrsName, this, child.getFirstChild()));
		}
	}
	
	public List<GMLPolygon> getPolygons()
	{
		return Collections.unmodifiableList(polygons);
	}
	
	@Override
    protected Sector calculateSector()
    {
    	Sector sector = null;
    	
    	for (GMLPolygon polygon : getPolygons())
    		sector = Sector.union(sector, polygon.getSector());
    	
    	return sector;
    }   
}
