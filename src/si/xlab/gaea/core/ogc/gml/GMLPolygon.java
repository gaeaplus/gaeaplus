package si.xlab.gaea.core.ogc.gml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.WWMath;

import org.w3c.dom.Node;

/**
 * @author marjan
 * A polygon consists of an outer ring and zero or more inner rings
 */
public class GMLPolygon extends GMLGeometry {
	
	public static class LinearRing
	{
		private final List<LatLon>vertices;
		
		public LinearRing(Node n) throws GMLException
		{
			if (n == null || !n.getNodeName().equals(GML_TAG_LINEAR_RING))
				throw new GMLException("Not a linear ring GML node");
			
			Node coordNode = n.getFirstChild();
			if (coordNode == null || !coordNode.getNodeName().equals(GML_TAG_COORDINATES))
				throw new GMLException("LinearRing should contain coordinates and nothing else.");
			
			vertices = parseMultiCoords(coordNode.getNodeValue());
			makeWindingCounterClockwise(vertices);
		}
		
		private static void makeWindingCounterClockwise(List<? extends LatLon> vertices)
		{
	        String windingOrder = WWMath.computeWindingOrderOfLocations(vertices);
	        //noinspection StringEquality
	        if (windingOrder != AVKey.COUNTER_CLOCKWISE)
	        {
	            java.util.Collections.reverse(vertices);
	        }			
		}
		
		public List<LatLon> getVertices()
		{
			return Collections.unmodifiableList(vertices);
		}
	}
	
	private final LinearRing outerRing;
	private final ArrayList<LinearRing> innerRings;
	
	public GMLPolygon(String defaultSrsName, GMLGeometry parent, Node n) throws GMLException
	{
		super(defaultSrsName, parent, n);
		
		if (n == null || !n.getNodeName().equals(GML_TAG_POLYGON)) 
			throw new GMLException("Not a polygon GML node");
		
		LinearRing outerRing = null;
		this.innerRings = new ArrayList<LinearRing>();
		
		for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
		{
			if (child.getNodeName().equals(GML_TAG_OUTER_BOUNDARY_IS))
			{
				if (outerRing != null)
					throw new GMLException("Polygon with more than one oter ring.");
				
				outerRing = new LinearRing(child.getFirstChild());
			}
			else if (child.getNodeName().equals(GML_TAG_INNER_BOUNDARY_IS))
			{
				this.innerRings.add(new LinearRing(child.getFirstChild()));
			}
		}
		if (outerRing == null)
			throw new GMLException("Polygon without outer ring.");
		
		this.outerRing = outerRing;
	}
	
	public LinearRing getOuterRing()
	{
		return this.outerRing;
	}
	
	public List<LinearRing> getInnerRings()
	{
		return Collections.unmodifiableList(this.innerRings);
	}
	
	public Iterable<Iterable<? extends LatLon>> getInnerRingsAsListOfLists()
	{
		List<Iterable<? extends LatLon>> rings = new ArrayList<Iterable<? extends LatLon>>();
		for (LinearRing ring : innerRings)
			rings.add(ring.getVertices());
		return rings;
	}
	
	@Override
    protected Sector calculateSector()
    {
        if (this.outerRing == null || this.outerRing.getVertices().size() < 1)
            return null;
        else
        	return Sector.boundingSector(outerRing.getVertices());
    }   
	

}
