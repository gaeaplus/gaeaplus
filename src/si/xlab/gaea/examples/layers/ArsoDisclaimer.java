/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.examples.layers;

import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 * Shows the notification about the source of data for layers from the Slovenian Environment Agency (http://www.arso.gov.si/en).
 * 
 * @author marjan
 */
public class ArsoDisclaimer
{
    private static ArrayList<String> arsoLayerNames = new ArrayList<String>();
    
    synchronized public static void registerArsoLayer(String layerName)
    {
        arsoLayerNames.add(layerName);
    }
            
    synchronized public static void showIfNeeded()
    {
        
        if (!arsoLayerNames.isEmpty())
        {
            StringBuilder msg = new StringBuilder();
            msg.append("The source of the following layers:\n");
            for (String layerName: arsoLayerNames)
            {
                msg.append("    ");
                msg.append(layerName);
                msg.append('\n');
            }
            msg.append("is the Slovenian Environment Agency (http://www.arso.gov.si/en).");
            msg.append("\n\n");
            msg.append("Warning: this server seems to use an inaccurate method of conversion to WGS84 coordinates,\n");
            msg.append("thus all the features are offset approximately 300 m to the east.");
            JOptionPane.showMessageDialog(null, msg.toString());            
            arsoLayerNames.clear();
        }
    }
}
