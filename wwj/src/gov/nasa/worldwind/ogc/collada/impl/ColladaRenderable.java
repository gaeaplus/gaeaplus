/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.collada.impl;

import gov.nasa.worldwind.render.DrawContext;

/**
 * Interface for rendering COLLADA elements.
 *
 * @author pabercrombie
 * @version $Id: ColladaRenderable.java 605 2012-05-22 17:38:42Z pabercrombie $
 */
public interface ColladaRenderable
{

    /**
     * Pre-Render this element.
     *
     * @param tc the current COLLADA traversal context.
     * @param dc the current draw context.
     *
     * @throws IllegalArgumentException if either the traversal context or the draw context is null.
     */
    void preRender(ColladaTraversalContext tc, DrawContext dc);

    /**
     * Render this element.
     *
     * @param tc the current COLLADA traversal context.
     * @param dc the current draw context.
     *
     * @throws IllegalArgumentException if either the traversal context or the draw context is null.
     */
    void render(ColladaTraversalContext tc, DrawContext dc);
}
