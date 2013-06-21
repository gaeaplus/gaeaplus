/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.ogc.kml;

import gov.nasa.worldwind.ogc.kml.KMLAbstractObject;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.nasa.worldwind.ogc.kml.KMLParserContext;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.util.xml.XMLEventParserContextFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 *
 * @author marjan
 */
public class KMLParser
{
    public static KMLAbstractObject parseKml(InputStream is, KMLAbstractObject helperInstance) throws KMLParserException
    {
        KMLAbstractObject rv = null;

        XMLEventReader evReader = WWXML.openEventReader(is, false);
        KMLParserContext ctx = (KMLParserContext)XMLEventParserContextFactory.createParserContext(KMLConstants.KML_MIME_TYPE, null);
        ctx.setEventReader(evReader);
        try {
            while (ctx.hasNext())
            {
                XMLEvent event = ctx.nextEvent();
                if (event.isStartElement())
                {
                    rv = (KMLAbstractObject)helperInstance.parse(ctx, event);
                    if (rv != null)
                        return rv;
                }
            }
        } catch (XMLStreamException e) {
            throw new KMLParserException("Error during parsing: " + e.getMessage(), e);
        }
        throw new KMLParserException("Error parsing a " + helperInstance.getClass().getName());
    }
    
    public static KMLAbstractObject parseKml(String str, KMLAbstractObject helperInstance) throws KMLParserException
    {
        InputStream is = new ByteArrayInputStream(str.getBytes());
        return parseKml(is, helperInstance);
    }
    
}
