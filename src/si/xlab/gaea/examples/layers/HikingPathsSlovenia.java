/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.examples.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import si.xlab.gaea.core.layers.MultiLayersLayer;
import si.xlab.gaea.core.layers.wfs.WFSGenericLayer;
import si.xlab.gaea.core.layers.wfs.WFSService;

/**
 *
 * @author marjan
 */
public class HikingPathsSlovenia extends MultiLayersLayer
{
    private static final Sector SLOVENIA_BOUNDING_BOX = new Sector(
            Angle.fromDegrees(45.1), Angle.fromDegrees(46.9),
            Angle.fromDegrees(13.3), Angle.fromDegrees(16.6));
    private static final int MAX_DISTANCE = 200000;
    private static final String SERVER_URL = "http://demo.data.gaeaplus.eu/geo";
    private static final String NAME = "Hiking paths in Slovenia";

    public HikingPathsSlovenia()
    {
        super();
        setName(NAME);
        setMaxActiveAltitude(MAX_DISTANCE);
        
        addLayer("topp:planinske_poti_easy_wgs", "ff00ffff");
        addLayer("topp:planinske_poti_medium_wgs", "ff0080ff");
        addLayer("topp:planinske_poti_hard_wgs", "ff0000ff");
    }
     
    final void addLayer(String featureType, String lineColor)
    {
        WFSService service = new WFSService(SERVER_URL, featureType, SLOVENIA_BOUNDING_BOX, Angle.fromDegrees(0.3));
        WFSGenericLayer layer = new WFSGenericLayer(service, "sublayer");
        layer.getWFSService().setMaxDisplayDistance(MAX_DISTANCE);
        KMLStyle style = layer.getDefaultStyle();
        style.getLineStyle().setField("color", lineColor);
        this.add(layer);
    }
}

