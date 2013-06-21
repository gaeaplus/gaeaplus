package si.xlab.gaea.core.ogc.gml;

import gov.nasa.worldwind.geom.Sector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Node;

public class GMLMultiLineString extends GMLGeometry {
	
	private final List<GMLLineString> lineStrings;
	
	public GMLMultiLineString(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_MULTI_LINE_STRING)) 
			throw new GMLException("Not a multi-linestring GML node");
		
		this.lineStrings = new ArrayList<GMLLineString>();
		
		for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (!child.getNodeName().equals(GML_TAG_LINE_STRING_MEMBER))
				continue;
			
			this.lineStrings.add(new GMLLineString(defaultSrsName, this, child.getFirstChild()));
		}
	}
	
	public List<GMLLineString> getLineStrings()
	{
		return Collections.unmodifiableList(lineStrings);
	}	
	
	@Override
    protected Sector calculateSector()
    {
    	Sector sector = null;
    	
    	for (GMLLineString lineString : getLineStrings())
    		sector = Sector.union(sector, lineString.getSector());
    	
    	return sector;
    }   
}
