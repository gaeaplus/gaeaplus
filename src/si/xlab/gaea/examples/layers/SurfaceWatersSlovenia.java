/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.examples.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import si.xlab.gaea.core.layers.wfs.WFSGenericLayer;
import si.xlab.gaea.core.layers.wfs.WFSService;

/**
 *
 * @author marjan
 */
public class SurfaceWatersSlovenia extends WFSGenericLayer
{
    private static final Sector SLOVENIA_BOUNDING_BOX = new Sector(
            Angle.fromDegrees(45.1), Angle.fromDegrees(46.9),
            Angle.fromDegrees(13.3), Angle.fromDegrees(16.6));
    private static final int MAX_DISTANCE = 200000;
    private static final String SERVER_URL = "http://gis.arso.gov.si/geoserver/ows";
    private static final String NAME = "Surface Waters in Slovenia";

    public SurfaceWatersSlovenia()
    {
        super(new WFSService(SERVER_URL, "arso:VT_POVR_CRT", SLOVENIA_BOUNDING_BOX, Angle.fromDegrees(0.3)), NAME);
        this.getWFSService().setMaxDisplayDistance(MAX_DISTANCE);
        this.setMaxActiveAltitude(MAX_DISTANCE);
        KMLStyle style = this.getDefaultStyle();
        style.getLineStyle().setField("color", "d0d0a000");
        style.getLineStyle().setField("width", 3.0);
        this.setDefaultStyle(style);
        ArsoDisclaimer.registerArsoLayer(NAME);
    }
    
    @Override
    public void setEnabled(boolean enabled)
    {
        if (enabled)
            ArsoDisclaimer.showIfNeeded();
        super.setEnabled(enabled);
    }
}
