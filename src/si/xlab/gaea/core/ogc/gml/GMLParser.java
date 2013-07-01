/**
 * 
 */
package si.xlab.gaea.core.ogc.gml;

import gov.nasa.worldwind.util.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.metadata.IIOMetadataNode;

import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * @author marjan
 * SAX parser of GML streams. Just use the public static method parse!
 * 
 * Implemented as a SAX handler that builds and ArrayList from a feature collection.
 * The geometry tags are first transformed into a DOM representation,
 * then GMLGeometry.parse() is used; this is done to put encapsulate
 * the actual parsing into the geometry classes, and in case we will
 * need DOM parsing later.
 */
public class GMLParser extends org.xml.sax.helpers.DefaultHandler
{
    public static List<GMLFeature> parse(InputStream is) throws ParserConfigurationException, SAXException, IOException, GMLException
    {
        GMLParser handler = new GMLParser();
        javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().parse(
                is, handler);
        if (!handler.getErrors().isEmpty())
        {
            StringBuilder errors = new StringBuilder("Errors during parsing:\n");
            for (String error: handler.getErrors())
                errors.append(error);
            throw new GMLException(errors.toString());
        }
        
        return handler.getFeatures();
    }    
    
	private static final String GML_BOUNDED_BY = "gml:boundedBy".intern();
	private static final String GML_FEATURE_MEMBER = "gml:featureMember".intern();
    private static final String SERVICE_EXCEPTION = "ServiceException".intern();
    
    //the bounding geometry; we need this because its srsName is used by default
    private GMLGeometry boundedBy = null;
    private String boundedBySrsName = null; //used as the default srsName for features

	//the final contents of the tile
	private final List<GMLFeature> features;

    //the final list of exceptions
    private final List<String> errors;
    
	//parse-time stuff
	private LinkedList<String> internedQNameStack = new LinkedList<String>();

	private String internedCurrentFeatureType = null;
	private HashMap<String, String> currentFeatureAttrs = null; 
	private HashMap<String, GMLGeometry> currentFeatureGeoms = null;

	private Node currentGeomNode = null;
	private boolean geomEnclosedInATagJustEnded = false;
	private StringBuffer buf = new StringBuffer();

	private GMLParser()
	{
		this.features = new ArrayList<GMLFeature>();
        this.errors = new ArrayList<String>();
	}
	
	/**
	 * Use this after parsing to get the results.
	 * @return read-only list of features
	 */
	private List<GMLFeature> getFeatures()
	{
		return Collections.unmodifiableList(this.features);
	}
	
	/**
	 * Use this after parsing to get the exceptions.
	 * @return read-only list of exceptions
	 */
	private List<String> getErrors()
	{
		return Collections.unmodifiableList(this.errors);
	}
    
    
    @Override
	public void startElement(String uri, String localName, String qName, org.xml.sax.Attributes attributes)
	{
		// Intern the qName string so we can use pointer comparison.
		String internedQName = qName.intern();

		if (this.internedCurrentFeatureType == null)
		{
			if (this.internedQNameStack.size() > 0
					&& GML_FEATURE_MEMBER == this.internedQNameStack.getFirst())
			{
				// if we're not inside a feature yet, but previous qName was GML_FEATURE_MEMBER,
				// then we're starting the feature right now
				this.internedCurrentFeatureType = internedQName;
				this.currentFeatureAttrs = new HashMap<String, String>();
				this.currentFeatureGeoms = new HashMap<String, GMLGeometry>();
			}
            else if (GML_BOUNDED_BY == internedQName)
            {
                //we're starting the gml:boundedBy tag, which we'll parse as if it were a special type of feature member
				this.internedCurrentFeatureType = internedQName;
				this.currentFeatureAttrs = new HashMap<String, String>();
				this.currentFeatureGeoms = new HashMap<String, GMLGeometry>();
            } else if (SERVICE_EXCEPTION == internedQName)
            {
                //we're starting a ServiceException tag, which we'll parse as if it were a feature with no geometries
                this.internedCurrentFeatureType = internedQName;
                this.currentFeatureAttrs = new HashMap<String, String>();
            }
		} else {
			if (currentGeomNode != null || GMLGeometry.supports(internedQName))
			{
				//if we're inside a feature and either also inside a geometry,
				//or just starting a new geometry, create a new DOM node
				
				//using IIOMetadataNode is not the nicest of ideas but 
				//there isn't really any implementation of org.w3c.dom.Node
				//intended for such usage, and we don't want to reimplement something
				
				IIOMetadataNode newNode = new IIOMetadataNode(internedQName);
				for (int i = 0; i < attributes.getLength(); i++)
					newNode.setAttribute(attributes.getQName(i), attributes.getValue(i));

				if (this.currentGeomNode != null)
					this.currentGeomNode.appendChild(newNode);

				this.currentGeomNode = newNode;            		
			}
		}

		this.internedQNameStack.addFirst(internedQName);
	}

    @Override
	public void endElement(String uri, String localName, String qName)
	{
		if (this.internedCurrentFeatureType != null)
		{
			// Intern the qName string so we can use pointer comparison.
			String internedQName = qName.intern();

			if (internedQName == this.internedCurrentFeatureType)
			{
				// if we're in the feature named the same as the qName of the element ending,
				// then we're ending the feature now
                if (this.internedCurrentFeatureType == GML_BOUNDED_BY)
                {
                    //the special feature boundedBy: store it and its srsName
                    this.boundedBy = this.currentFeatureGeoms.get(GML_BOUNDED_BY);
                    if (this.boundedBy != null)
                    {
                        this.boundedBySrsName = this.boundedBy.getSrsName();
                    }
                } else if (this.internedCurrentFeatureType == SERVICE_EXCEPTION)
                {
                    //the exception geometry-less "feature": store it
                    StringBuilder error = new StringBuilder();
                    error.append("ServiceException: ");
                    for (String attr: currentFeatureAttrs.keySet())
                    {
                        error.append(attr);
                        error.append("=");
                        error.append(currentFeatureAttrs.get(attr));
                        error.append("; ");                        
                    }
                    error.append(buf);
                    this.errors.add(error.toString());
                } else {
                    //a normal feature: add it to feature list
                    this.features.add(new GMLFeature(this.internedCurrentFeatureType,
                            this.currentFeatureAttrs, this.currentFeatureGeoms));
                }

				this.internedCurrentFeatureType = null;
				this.currentFeatureAttrs = null;
				this.currentFeatureGeoms = null;                

				if (this.currentGeomNode != null)
				{
					Logging.logger().severe("End-of-feature inside a geometry!");
					this.currentGeomNode = null;
				}
			}
			else if (this.currentGeomNode != null)
			{
				this.currentGeomNode.setNodeValue(buf.toString());

				if (this.currentGeomNode.getParentNode() == null)
				{
					//we're ending a geometry
					try {
						GMLGeometry geom = GMLGeometry.parseGeometry(this.boundedBySrsName, this.currentGeomNode);
						currentFeatureGeoms.put(this.internedQNameStack.get(1), geom);
					} catch (GMLException e) {
						Logging.logger().warning("Error parsing geometry: " + e);
					}
					this.currentGeomNode = null;

                    if (this.internedCurrentFeatureType != GML_BOUNDED_BY)
                    {
                        /* geometries in featureMembers are enclosed in a special tag that is the geometry name;
                         * we note that such a geometry just ended, so that we will note to ignore the closing tag
                         * on the other hand, if we process the bounding box as if it were a special feature type,
                         * then this "feature"'s geometry is not enclosed in any special tag
                         */
                        this.geomEnclosedInATagJustEnded = true;
                    }
				} else {
					//we're just ending a sub-tag of a geometry
					this.currentGeomNode = this.currentGeomNode.getParentNode();
				}
			} else {
				if (this.geomEnclosedInATagJustEnded)
				{
					//we're just closing a tag that contained geometry
					//such tags have no content (apart from the geometry),
					//and are not attributes
					this.geomEnclosedInATagJustEnded = false;
				}
				else
				{
					//we're ending a non-geom attribute
					this.currentFeatureAttrs.put(internedQName, buf.toString());
				}
			}

			buf = new StringBuffer();
		}

		this.internedQNameStack.removeFirst();
	}

    @Override
	public void characters(char ch[], int start, int length)
	{
		if (this.internedCurrentFeatureType == null)
		{
			return;
		}

		buf.append(ch, start, length);
	}        
}