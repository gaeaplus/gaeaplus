/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.examples;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwindx.examples.ApplicationTemplate;
import java.awt.Color;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import si.xlab.gaea.core.event.FeatureSelectListener;
import si.xlab.gaea.core.layers.RenderToTextureLayer;
import si.xlab.gaea.core.layers.wfs.AbstractWFSLayer;
import si.xlab.gaea.core.layers.wfs.WFSGenericLayer;
import si.xlab.gaea.core.layers.wfs.WFSService;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;

/**
 * 
 * @author marjan
 */
public class GaeaApplicationExample extends ApplicationTemplate
{
    public static final Sector SLOVENIA_BOUNDING_BOX = new Sector(
            Angle.fromDegrees(45.1), Angle.fromDegrees(46.9),
            Angle.fromDegrees(13.3), Angle.fromDegrees(16.6));
    
    protected static void makeMenu(final AppFrame appFrame)
    {
        JMenuBar menuBar = new JMenuBar();
        appFrame.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem openWfsItem = new JMenuItem(new AbstractAction("Add WFS layer...")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    JOptionPane.showMessageDialog(null, "This is just a test of adding a WFS layer in run-time. It will use default parameters. GUI to set the parameters is in progress.");
                    addWfsLayer("http://demo.data.gaeaplus.eu/geo", "topp:planinske_poti_easy_wgs", SLOVENIA_BOUNDING_BOX, AbstractWFSLayer.DEFAULT_TILE_DELTA, 200000, Color.RED);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(openWfsItem);

        JMenuItem aboutItem = new JMenuItem(new AbstractAction("About...")
        {
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    String msg = "This is a demonstration of features that Gaea+ Open Source adds to NASA WorldWind Java SDK.\n"
                            + "For more information, visit http://www.gaeaplus.eu/en/, https://github.com/gaeaplus/gaeaplus, and http://worldwind.arc.nasa.gov/java/.";
                    JOptionPane.showMessageDialog(null, msg);            
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });

        fileMenu.add(aboutItem);
    }    
    
    protected static void addWfsLayer(String url, String featureTypeName, Sector sector, Angle tileDelta, double maxVisibleDistance, Color color)
    {
        WFSService service = new WFSService(url, featureTypeName, sector, tileDelta);
        WFSGenericLayer layer = new WFSGenericLayer(service, "WFS: " + featureTypeName + " (from " + url + ")");
        layer.setMaxActiveAltitude(maxVisibleDistance);
        KMLStyle style = layer.getDefaultStyle();
        style.getLineStyle().setField("color", KMLStyleFactory.encodeColorToHex(color));
        style.getPolyStyle().setField("color", KMLStyleFactory.encodeColorToHex(color).replaceFirst("^ff", "80")); //semi-transparent fill
        layer.setDefaultStyle(style);
        insertBeforePlacenames(appFrame.getWwd(), layer);       
        layer.setEnabled(true);
        appFrame.updateLayerPanel();
    }
    
    public static class GaeaAppFrame extends AppFrame
    {
        protected void updateLayerPanel()
        {
            //remove RTT layer, update layer panel, re-insert RTT; otherwise it will appear in the layer list
            int rttIndex = getWwd().getModel().getLayers().indexOf(RenderToTextureLayer.getInstance());
            if (rttIndex != -1)
                getWwd().getModel().getLayers().remove(rttIndex);
            this.layerPanel.update(getWwd());
            getWwd().getModel().getLayers().add(rttIndex, RenderToTextureLayer.getInstance());
        }
    }
    
    private static GaeaAppFrame appFrame = null;
    
    public static void main(String[] args)
    {
        Configuration.insertConfigurationDocument("si/xlab/gaea/core/examples/gaea-example-config.xml");
        appFrame = (GaeaAppFrame)ApplicationTemplate.start("Gaea+ Open Source Example Application", GaeaAppFrame.class);
        insertBeforeCompass(appFrame.getWwd(), RenderToTextureLayer.getInstance());
        appFrame.getWwd().addSelectListener(new FeatureSelectListener(appFrame.getWwd()));
        makeMenu(appFrame);
    }
}
