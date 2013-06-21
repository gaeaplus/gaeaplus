package si.xlab.gaea.core.ogc.gml;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author marjan
 * A container for attributes and geometries parsed from a GML feature collection 
 */
public class GMLFeature
{
    private static final Logger LOG = Logger.getLogger(GMLFeature.class.getName());
    
    //constants representing various (de-facto) standard GML tags
	private final static String[] DEFAULT_GEOM_TAGS = {"topp:geom_wgs","topp:gml_wgs","topp:the_geom","topp:geom"};
	private final static String IGNORED_GEOM_TAGS_PATTERN = "gml:boundedBy";
	private final static String DEFAULT_GEOM_SRS_PATTERN = ".*[#:]4326";

	private static final String GML_ATTR_ID = "topp:oid";
	private static final String GML_ATTR_NAME = "topp:full_name";
	private static final String GML_ATTR_RELATIVE_IMPORTANCE = "topp:relative_importance";
	private static final String GML_ATTR_STYLE = "topp:style";
	private static final String GML_ATTR_DESC = "topp:description";

	private static final String[] GML_SPECIAL_ATTRS = new String[] {
		GML_ATTR_ID, GML_ATTR_NAME, GML_ATTR_RELATIVE_IMPORTANCE,
		GML_ATTR_STYLE,	GML_ATTR_DESC
	};

	private final String type;
	private final Map<String, String> attrs; 
	private final Map<String, GMLGeometry> geoms;

	public GMLFeature(String type,
			Map<String, String> attrs, Map<String, GMLGeometry> geoms)
	{	
        this.type = type;
		this.attrs = attrs;
        this.geoms = geoms;
	}

	public String getType()
	{
		return type;
	}
	
	public Map<String, String> getAttributes()
	{
		return Collections.unmodifiableMap(this.attrs);
	}
	
    public String getAttributeIgnoreNamespace(String attrName)
    {
        final String NAMESPACE_PAT = "^.*:";
        attrName = attrName.replaceFirst(NAMESPACE_PAT, "");
        for (String attr: attrs.keySet())
        {
            if (attrName.equalsIgnoreCase(attr.replaceFirst(NAMESPACE_PAT, "")))
                return attrs.get(attr);
        }
        return null;
    }
    
	public Map<String, GMLGeometry> getGeometries()
	{
		return Collections.unmodifiableMap(this.geoms);
	}
	
	/**
	 * The default geometry is one of those given in DEFAULT_GEOM_TAGS (in the order of preference);
	 * if none is found then ONE OF those with DEFAULT_GEOM_SRS_NAME is returned instead,
	 * but preferably not one of IGNORED_GEOM_TAGS.
	 * @return
	 */
	public GMLGeometry getDefaultGeometry()
	{
		for (String geomTag : DEFAULT_GEOM_TAGS)
		{
			//try DEFAULT_GEOM_TAGS in the given order
			if (geoms.containsKey(geomTag))
				return geoms.get(geomTag);
		}
		for (String geomKey : geoms.keySet())
		{
			//find any geom with DEFAULT_GEOM_SRS_PATTERN, except IGNORED_GEOM_TAGS
			if (Pattern.matches(DEFAULT_GEOM_SRS_PATTERN, geoms.get(geomKey).getSrsName())
					&& !Pattern.matches(IGNORED_GEOM_TAGS_PATTERN, geomKey))
				return geoms.get(geomKey);
		}
		for (GMLGeometry geom : geoms.values())
		{
			//last resort: find any geom with DEFAULT_GEOM_SRS_PATTERN
			if (Pattern.matches(DEFAULT_GEOM_SRS_PATTERN, geom.getSrsName()))
				return geom;
		}
		return null;
	}

    public String getName()
    {
        return getAttributes().get(GML_ATTR_NAME);
    }

    public String getStyle()
    {
        return getAttributes().get(GML_ATTR_STYLE);
    }

    /**
     * Returns the relative importance from the GML_ATTR_RELATIVE_IMPORTANCE tag.
     * If it does not contain a number from 0 to 1, it returns 1.
     * @return
     */
    public double getRelativeImportance()
    {
        double rv = 1.0;
        String relImportanceStr = getAttributes().get(GML_ATTR_RELATIVE_IMPORTANCE);
        if (relImportanceStr != null) {
            try {
                rv = Double.parseDouble(relImportanceStr);
                if (rv < 0 || rv > 1) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                LOG.warning("Invalid relative importance: " + relImportanceStr);
            }
        }
        return rv;
    }
    
    /**
     * Builds and returns desctiption of the feature from its attributes.
     * If the GML_ATTR_DESC is present, this is the description; otherwise
     * the description is a list of attribute-value pairs but without special
     * attributes (ID, name, style...)
     * @return description usable e.g. for the annotation shown above the feature
     */
	public String getDescription()
	{
		String rv = getAttributes().get(GML_ATTR_DESC);

		if (rv == null || rv.length() == 0)
		{
			StringBuilder desc = new StringBuilder();
			for (String key : getAttributes().keySet())
			{
				boolean isSpecialField = false;

				for (String specialField : GML_SPECIAL_ATTRS)
				{
					if (key.equalsIgnoreCase(specialField))
					{
						isSpecialField = true;
						break;
					}
				}

				if (!isSpecialField)
				{
					if (desc.length() > 0)
						desc.append("<br/>");

					desc.append("<b>");
					desc.append(key);
					desc.append(":</b> ");
					desc.append(getAttributes().get(key));
				}
			}
			rv = desc.toString();
		}

		return rv;
	}

    public String buildDescription(String descriptionFormat)
    {
        if (descriptionFormat == null || descriptionFormat.isEmpty())
            return getDescription();
        
        String rv = descriptionFormat;
        for (String attrName: attrs.keySet())
        {
            String attrNameNoNamespace = attrName.replaceAll("^[^:]+:", "");
            
            //replace attribute placeholder with value
            String attrValue = attrs.get(attrName);
            if (attrValue == null)
                attrValue = "";
            rv = rv.replaceAll("\\$" + attrNameNoNamespace + "\\$", attrValue);
        }
        //replace missing attributes with empty strings
        rv = rv.replaceAll("\\$[a-zA-Z0-9_-]+\\$","");
        return rv;
    }
}