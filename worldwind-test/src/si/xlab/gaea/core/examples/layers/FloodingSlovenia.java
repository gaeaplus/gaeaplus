/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.examples.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import si.xlab.gaea.core.examples.GaeaApplicationExample;
import si.xlab.gaea.core.layers.MultiLayersLayer;
import si.xlab.gaea.core.layers.wfs.WFSGenericLayer;
import si.xlab.gaea.core.layers.wfs.WFSService;

/**
 *
 * @author marjan
 */
public class FloodingSlovenia extends MultiLayersLayer
{
    private static final int MAX_DISTANCE = 200000;
    private static final String SERVER_URL = "http://gis.arso.gov.si/geoserver/ows";
    private static final String NAME = "Floods in Slovenia - regular, rare, catastrophic";

    public FloodingSlovenia()
    {
        super();
        setName(NAME);
        setMaxActiveAltitude(MAX_DISTANCE);
        
        addLayer("arso:MV_OPZ_KRT_POP_OBM_KAT", "70802020", "ff802060");
        addLayer("arso:MV_OPZ_KRT_POP_OBM_RED", "70907020", "ff907060");
        addLayer("arso:MV_OPZ_KRT_POP_OBM_POG", "70b0b020", "ffb0b060");
        
        ArsoDisclaimer.registerArsoLayer(NAME);
    }
     
    void addLayer(String featureType, String fillColor, String lineColor)
    {
        WFSService service = new WFSService(SERVER_URL, featureType, GaeaApplicationExample.SLOVENIA_BOUNDING_BOX, Angle.fromDegrees(0.3));
        WFSGenericLayer layer = new WFSGenericLayer(service, "sublayer");
        layer.getWFSService().setMaxDisplayDistance(MAX_DISTANCE);
        KMLStyle style = layer.getDefaultStyle();
        style.getLineStyle().setField("color", lineColor);
        style.getPolyStyle().setField("color", fillColor);
        this.add(layer);
    }
    
    @Override
    public void setEnabled(boolean enabled)
    {
        if (enabled)
            ArsoDisclaimer.showIfNeeded();
        super.setEnabled(enabled);
    }
    
}

