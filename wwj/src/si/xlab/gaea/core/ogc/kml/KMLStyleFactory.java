/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.ogc.kml;

import gov.nasa.worldwind.ogc.kml.KMLBalloonStyle;
import gov.nasa.worldwind.ogc.kml.KMLConstants;
import gov.nasa.worldwind.ogc.kml.KMLIcon;
import gov.nasa.worldwind.ogc.kml.KMLIconStyle;
import gov.nasa.worldwind.ogc.kml.KMLLabelStyle;
import gov.nasa.worldwind.ogc.kml.KMLLineStyle;
import gov.nasa.worldwind.ogc.kml.KMLListStyle;
import gov.nasa.worldwind.ogc.kml.KMLPolyStyle;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import java.awt.Color;

/**
 *
 * @author jure
 */
public class KMLStyleFactory
{

	/**
	 * Constructs a new style from the KML description of the style.
	 * @param styleDesc KML description of the style
	 * @throws KMLParserException
	 */
	public static KMLStyle createStyle(String styleDesc) throws KMLParserException
	{
		return createStyle(styleDesc, null);
	}

	/**
	 * Constructs a new style from the KML description of the style.
	 * If defaultStyle is supplied, its values are used for each property
	 * not explicitly given in styleDesc.
	 * @param styleDesc KML description of the style
	 * @param defaultStyle
	 * @throws KMLParserException
	 */
	public static KMLStyle createStyle(String styleDesc, KMLStyle defaultStyle) throws KMLParserException
	{
		KMLStyle newStyle;
		try
		{
			newStyle = (KMLStyle)KMLParser.parseKml(styleDesc, new KMLStyle((String)null));
		} catch (Exception ex)
		{
			throw new KMLParserException(ex.getMessage());
		}

		if (null != defaultStyle)
		{
			if (newStyle.getIconStyle() == null && defaultStyle.getIconStyle() != null)
			{
				newStyle.setField(KMLConstants.ICON_STYLE_FIELD, new KMLIconStyle(defaultStyle.getIconStyle()));
			}
			if (newStyle.getBaloonStyle() == null && defaultStyle.getBaloonStyle() != null)
			{
				newStyle.setField(KMLConstants.BALOON_STYLE_FIELD, new KMLBalloonStyle(defaultStyle.getBaloonStyle()));
			}
			if (newStyle.getLineStyle() == null && defaultStyle.getLineStyle() != null)
			{
				newStyle.setField(KMLConstants.LINE_STYLE_FIELD, new KMLLineStyle(defaultStyle.getLineStyle()));
			}
			if (newStyle.getPolyStyle() == null && defaultStyle.getPolyStyle() != null)
			{
				newStyle.setField(KMLConstants.POLY_STYLE_FIELD, new KMLPolyStyle(defaultStyle.getPolyStyle()));
			}
			if (newStyle.getLabelStyle() == null && defaultStyle.getLabelStyle() != null)
			{
				newStyle.setField(KMLConstants.LABEL_STYLE_FIELD, new KMLLabelStyle(defaultStyle.getLabelStyle()));
			}
			if (newStyle.getListStyle() == null && defaultStyle.getListStyle() != null)
			{
				newStyle.setField(KMLConstants.LIST_STYLE_FIELD, new KMLListStyle(defaultStyle.getListStyle()));
			}
		}

		return newStyle;
	}
    
	/**
	 * Decodes Color from passed string
	 * @param s
	 * @return
	 */
	public static Color decodeHexToColor(String s)
	{
		if (s == null)
		{
			//  nothing here
		} else if (s.length() == 6)
		{
			return new Color(Integer.parseInt(s, 16));
		} else if (s.length() == 8)
		{
			int alpha = Integer.parseInt(s.substring(0, 2), 16);
			int blue = Integer.parseInt(s.substring(2, 4), 16);
			int green = Integer.parseInt(s.substring(4, 6), 16);
			int red = Integer.parseInt(s.substring(6, 8), 16);

			return new Color(red, green, blue, alpha);
		}

		return Color.WHITE;
	}

	/**
	 * Decodes Hex String from passed color
	 * @param c
	 * @return
	 */
	public static String encodeColorToHex(Color c)
	{
		String colorString = "";

		String hex;

		hex = Integer.toHexString(c.getAlpha());
		if (hex.length() == 1)
		{
			hex = "0" + hex;
		}
		colorString += hex;

		hex = Integer.toHexString(c.getBlue());
		if (hex.length() == 1)
		{
			hex = "0" + hex;
		}
		colorString += hex;

		hex = Integer.toHexString(c.getGreen());
		if (hex.length() == 1)
		{
			hex = "0" + hex;
		}
		colorString += hex;

		hex = Integer.toHexString(c.getRed());
		if (hex.length() == 1)
		{
			hex = "0" + hex;
		}
		colorString += hex;

		return colorString;

	}
}
