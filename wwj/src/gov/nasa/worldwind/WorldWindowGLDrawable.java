/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import gov.nasa.worldwind.cache.*;

import javax.media.opengl.GLAutoDrawable;

/**
 * @author tag
 * @version $Id: WorldWindowGLDrawable.java 1399 2013-06-03 23:26:22Z tgaskins $
 */
public interface WorldWindowGLDrawable extends WorldWindow
{
    void initDrawable(GLAutoDrawable glAutoDrawable);

    void initGpuResourceCache(GpuResourceCache cache);

    void initGpuResourceCache(GpuResourceCache cache, boolean shared);

    void endInitialization();
}
