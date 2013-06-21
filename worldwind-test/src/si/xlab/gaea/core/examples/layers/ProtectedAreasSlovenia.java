/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.examples.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import si.xlab.gaea.core.examples.GaeaApplicationExample;
import si.xlab.gaea.core.layers.wfs.WFSGenericLayer;
import si.xlab.gaea.core.layers.wfs.WFSService;

/**
 *
 * @author marjan
 */
public class ProtectedAreasSlovenia extends WFSGenericLayer
{
    private static final int MAX_DISTANCE = 200000;
    private static final String SERVER_URL = "http://gis.arso.gov.si/geoserver/ows";
    private static final String NAME = "Protected Areas in Slovenia";

    public ProtectedAreasSlovenia()
    {
        super(new WFSService(SERVER_URL, "arso:ZAV_OBM_TOC&propertyname=ID_ZNAMEN,IME_ZNAMEN,DATUMPREDPISA,SHAPE", GaeaApplicationExample.SLOVENIA_BOUNDING_BOX, Angle.fromDegrees(0.3)), NAME);
        this.getWFSService().setMaxDisplayDistance(MAX_DISTANCE);
        this.setMaxActiveAltitude(MAX_DISTANCE);
        this.setDefaultIconStyle("si/xlab/gaea/core/examples/layers/protected_areas_24.png");
    }
}
