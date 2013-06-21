package si.xlab.gaea.core.ogc.gml;

import gov.nasa.worldwind.geom.Sector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

public class GMLGeometryCollection extends GMLGeometry {
	
	private final List<GMLGeometry> geometries;
	
	public GMLGeometryCollection(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_GEOMETRY_COLLECTION)) 
			throw new GMLException("Not a geometry collection GML node");
		
		this.geometries = new ArrayList<GMLGeometry>();
		
		for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (!child.getNodeName().equals(GML_TAG_GEOMETRY_MEMBER))
				continue;
			
			this.geometries.add(new GMLLineString(defaultSrsName, this, child.getFirstChild()));
		}
	}
	
	public List<GMLGeometry> getGeometries()
	{
		return Collections.unmodifiableList(geometries);
	}
	
	@Override
    protected Sector calculateSector()
    {
    	Sector sector = null;
    	
    	for (GMLGeometry geometry : getGeometries())
    		sector = Sector.union(sector, geometry.getSector());
    	
    	return sector;
    }   
}
