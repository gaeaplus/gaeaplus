/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwindx.examples.layermanager;

import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import java.awt.*;

/**
 * Combines the layer manager and elevation model manager in a single frame.
 *
 * @author tag
 * @version $Id: LayerAndElevationManagerPanel.java 1179 2013-02-15 17:47:37Z tgaskins $
 */
public class LayerAndElevationManagerPanel extends JPanel
{
    protected LayerManagerPanel layerManagerPanel;
    protected ElevationModelManagerPanel elevationModelManagerPanel;

    public LayerAndElevationManagerPanel(WorldWindow wwd)
    {
        super(new BorderLayout(10, 10));

        this.add(this.layerManagerPanel = new LayerManagerPanel(wwd), BorderLayout.CENTER);

        this.add(this.elevationModelManagerPanel = new ElevationModelManagerPanel(wwd), BorderLayout.SOUTH);
    }

    public void updateLayers(WorldWindow wwd)
    {
        this.layerManagerPanel.update(wwd);
    }

    public void updateElevations(WorldWindow wwd)
    {
        this.elevationModelManagerPanel.update(wwd);
    }
}
