/*
 * Copyright (C) 2013 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwindx.examples;

import gov.nasa.worldwind.globes.Earth;

import java.io.IOException;

/**
 * Illustrates how to apply EGM96 geoid offsets to the World Wind globe.
 *
 * @author tag
 * @version $Id: EGM96.java 1173 2013-02-12 19:43:23Z tgaskins $
 */
public class EGM96 extends ApplicationTemplate
{
    public static class AppFrame extends ApplicationTemplate.AppFrame
    {
        public AppFrame()
        {
            try
            {
                ((Earth) getWwd().getModel().getGlobe()).applyEGMA96Offsets("config/EGM96.dat");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args)
    {
        ApplicationTemplate.start("World Wind EGM96", AppFrame.class);
    }
}
