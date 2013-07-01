/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.render.DrawContext;

/**
 * @author Tom Gaskins
 * @version $Id: BasicSceneController.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public class BasicSceneController extends AbstractSceneController
{
    public void doRepaint(DrawContext dc)
    {
        this.initializeFrame(dc);
        try
        {
            this.applyView(dc);
            this.createPickFrustum(dc);
            this.createTerrain(dc);
            this.preRender(dc);
            this.clearFrame(dc);
            this.pick(dc);
            this.clearFrame(dc);
            this.draw(dc);
        }
        finally
        {
            this.finalizeFrame(dc);
        }
    }
}
