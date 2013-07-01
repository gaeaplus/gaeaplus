/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import java.awt.Dimension;
import java.util.Calendar;
import si.xlab.gaea.avlist.AvKeyExt;
import si.xlab.gaea.core.layers.elev.SlopeLayer;

/**
 *
 * @author vito
 */
public class ApplicationLightingFull extends ApplicationTemplate{
	public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            super(true, true, true); // Don't include the layer panel; we're using the on-screen layer tree.

            // Size the World Window to take up the space typically used by the layer panel.
            Dimension size = new Dimension(800, 800);
            this.setPreferredSize(size);
            this.pack();
            WWUtil.alignComponent(null, this, AVKey.CENTER);

			WorldWindow wwd = getWwd();
			wwd.getModel().getGlobe().setSunlightFromTime(Calendar.getInstance());
			wwd.getSceneController().firePropertyChange(AvKeyExt.ENABLE_SUNLIGHT, false, true);
			wwd.getSceneController().firePropertyChange(AvKeyExt.ENABLE_ATMOSPHERE, false, true);
			wwd.getSceneController().firePropertyChange(AvKeyExt.ENABLE_ATMOSPHERE_WITH_AERIAL_PERSPECTIVE, false, true);
			wwd.getSceneController().firePropertyChange(AvKeyExt.ENABLE_SHADOWS, false, true);

			insertBeforePlacenames(wwd, new SlopeLayer());
            
			this.getLayerPanel().update(this.getWwd());
        }
    }	

	/*
	public static class AppPanel extends ApplicationTemplate.AppPanel{

		public AppPanel(Dimension canvasSize, boolean includeStatusBar) {
			super(canvasSize, includeStatusBar);
			wwd.getSceneController().firePropertyChange(AVKey.ENABLE_SUNLIGHT, false, true);
			wwd.getSceneController().firePropertyChange(AVKey.ENABLE_ATMOSPHERE, false, true);
			wwd.getSceneController().firePropertyChange(AVKey.ENABLE_ATMOSPHERE_WITH_AERIAL_PERSPECTIVE, false, true);
		}
		
	}

	protected ApplicationTemplate.AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar)
    {
    	return new AppPanel(canvasSize, includeStatusBar);
    }
	*/

	public static void main(String[] args)
    {
        //Configuration.setValue(AVKey.INITIAL_LATITUDE, 40.028);
        //Configuration.setValue(AVKey.INITIAL_LONGITUDE, -105.27284091410579);
        //Configuration.setValue(AVKey.INITIAL_ALTITUDE, 4000);
        //Configuration.setValue(AVKey.INITIAL_PITCH, 50);

		Configuration.setValue(AVKey.SCENE_CONTROLLER_CLASS_NAME, "si.xlab.gaea.core.GaeaSceneController");
		
		final AppFrame af = (AppFrame) start("World Wind COLLADA Viewer", AppFrame.class);
	}
}
