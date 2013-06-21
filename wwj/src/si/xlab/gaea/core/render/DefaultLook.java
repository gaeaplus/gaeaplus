package si.xlab.gaea.core.render;


import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.FrameFactory;
import gov.nasa.worldwind.util.Logging;

import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import si.xlab.gaea.core.ogc.kml.KMLParserException;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;


public class DefaultLook
{

    public static final Font FONT_BIG = new Font("Dialog", Font.BOLD, 15);
    public static final Font FONT_DEFAULT = new Font("Dialog", Font.BOLD, 11);
    public static final Font FONT_SMALL = new Font("Dialog", Font.BOLD, 9);
    public static final Font FONT_SMALLER = new Font("Dialog", Font.BOLD, 7);

    public static Font getDefaultFont()
    {
        return FONT_DEFAULT;
    }
	
    public static Font getSmallFont()
    {
        return FONT_SMALL;
    }
	
    public static final AnnotationAttributes DEFAULT_ANNOTATION_ATTRIBUTES;
	
    public static final KMLStyle DEFAULT_FEATURE_STYLE;
	
    public static int ANNOTATION_MAX_IMAGE_SIZE = 320;
	
    static
    {
        DEFAULT_ANNOTATION_ATTRIBUTES = new AnnotationAttributes();
        DEFAULT_ANNOTATION_ATTRIBUTES.setBackgroundColor(
//                new Color(0.85f, 0.85f, 0.2f, 0.9f));
                new Color(1.0f, 1.0f, 1.0f, 0.9f));
        DEFAULT_ANNOTATION_ATTRIBUTES.setOpacity(0.9);
        DEFAULT_ANNOTATION_ATTRIBUTES.setTextColor(
                new Color(0.0f, 0.0f, 0.0f, 1f));
        DEFAULT_ANNOTATION_ATTRIBUTES.setBorderColor(
                new Color(0.0f, 0.0f, 0.0f, 1f));
        DEFAULT_ANNOTATION_ATTRIBUTES.setLeader(FrameFactory.LEADER_NONE);
        DEFAULT_ANNOTATION_ATTRIBUTES.setDistanceMinScale(0.6d);
        DEFAULT_ANNOTATION_ATTRIBUTES.setDistanceMaxScale(1.0d);
        DEFAULT_ANNOTATION_ATTRIBUTES.setDistanceMinOpacity(0.6d);
        DEFAULT_ANNOTATION_ATTRIBUTES.setFont(
                new Font(DEFAULT_ANNOTATION_ATTRIBUTES.getFont().getName(),
                Font.PLAIN, 11));
        DEFAULT_ANNOTATION_ATTRIBUTES.setCornerRadius(8);
        DEFAULT_ANNOTATION_ATTRIBUTES.setDrawOffset(new Point(-10, 20)); 
        DEFAULT_ANNOTATION_ATTRIBUTES.setInsets(new Insets(5, 5, 5, 5));

        KMLStyle defaultFeatureStyle = null;

        try
        {
            defaultFeatureStyle = KMLStyleFactory.createStyle(
                    "<Style id=\"DEFAULT_FEATURE_STYLE\">"
                            + "<IconStyle>"
                            + 	"<color>ffffffff</color>"
                            + 	"<colorMode>normal</colorMode>"
                            + 	"<scale>1</scale>" + "<heading>0</heading>"
                            + 	"<Icon><href>images/pushpins/push-pin-yellow-32.png</href></Icon>"
                            + 	"<hotSpot x=\"0.5\"  y=\"0.0\" xunits=\"fraction\" yunits=\"fraction\"/>"
                            + "</IconStyle>"
                            + "<LabelStyle>"
                            + 	"<color>ffffffff</color>"
                            + 	"<colorMode>normal</colorMode>"
                            + 	"<scale>1</scale>"
                            + "</LabelStyle>"
                            + "<LineStyle>"
                            + 	"<color>ff0000ff</color>"
                            + 	"<colorMode>normal</colorMode>"
                            + 	"<width>2</width>"
                            + "</LineStyle>"
                            + "<PolyStyle>"
                            + 	"<color>80a0a0ff</color>"
                            + 	"<colorMode>normal</colorMode>" + "<fill>1</fill>"
                            + 		"<outline>1</outline>"
                            + "</PolyStyle>"
                            + "<BalloonStyle>"
                            + 	"<bgColor>DCFAFAf0</bgColor>"
                            + 	"<textColor>ff000000</textColor>"
                            + 	"<text></text>"
                            + 	"<displayMode>default</displayMode>"
                            + "</BalloonStyle>"
//                            + "<ContentFormatStyle>"
//                            + 	"<ContentFormat>"
//                            +		"<Language>en</Language>"
//                            +		"<Format>Name:{topp:naziv}&lt;BR /&gt;Address:{topp:naslov}</Format>"
//                            + 	"</ContentFormat>"
//                            + 	"<ContentFormat>"
//                            +		"<Language>sl</Language>"
//                            +		"<Format>Naziv:{topp:naziv}&lt;BR /&gt;Naslov:{topp:naslov}</Format>"
//                            + 	"</ContentFormat>"
//                            + "</ContentFormatStyle>"
                     + "</Style>");
        }
        catch (KMLParserException e)
        {
            Logging.logger().severe("ERROR parsing default feature style!");
        }

        DEFAULT_FEATURE_STYLE = defaultFeatureStyle;
    }
}

