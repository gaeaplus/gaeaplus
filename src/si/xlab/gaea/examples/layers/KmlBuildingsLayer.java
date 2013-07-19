/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.examples.layers;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderAttributes;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.ogc.kml.KMLRoot;
import gov.nasa.worldwind.ogc.kml.impl.KMLController;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.view.orbit.BasicOrbitView;
import si.xlab.gaea.core.render.Shadow;

/**
 *
 * @author marjan
 */
public class KmlBuildingsLayer extends RenderableLayer implements Shadow
{
    private KMLController kml = null;
    boolean firstEnabled = false;

	public KmlBuildingsLayer() {
		this.getRenderAttributes().setRenderType(RenderAttributes.RenderType.SPATIAL);
		this.getRenderAttributes().setRenderMode(RenderAttributes.COLOR_MODE);
	}

    @Override
	public void renderShadow(DrawContext dc)
	{
		this.render(dc);
	}
    
    @Override
    public void render(DrawContext dc)
    {
        if (firstEnabled)
        {
            firstEnabled = false;
            BasicOrbitView view = (BasicOrbitView)dc.getView();
            if (view != null) {
                view.addPanToAnimator(Position.fromDegrees(39.435, -0.473, 500), view.getHeading(), view.getPitch(), 800);
            } else {
                Logging.logger().warning("Cannot fly to layer - view is not a BasicOrbitView!");                
            }
        }
        super.render(dc);
    }
            
    @Override
    public void setEnabled(boolean enabled)
    {
        if (enabled && kml == null)
        {
            try {
                this.setPickEnabled(false);
                KMLRoot kmlRoot = KMLRoot.createAndParse(WWIO.getFileOrResourceAsStream("si/xlab/gaea/examples/layers/buildings.kml", getClass()));
                kml = new KMLController(kmlRoot);
                addRenderable(kml);
                firstEnabled = true;
            } catch (Exception e) {
                Logging.logger().severe("Error load KML: " + e.getMessage());
            }
        }
        super.setEnabled(enabled);
    }
}
