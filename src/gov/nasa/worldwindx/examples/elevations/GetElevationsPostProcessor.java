/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwindx.examples.elevations;

import gov.nasa.worldwind.geom.Position;

/**
 * @author Lado Garakanidze
 * @version $Id: GetElevationsPostProcessor.java 1171 2013-02-11 21:45:02Z dcollins $
 */
public interface GetElevationsPostProcessor
{
    public void onSuccess( Position[] positions );

    public void onError( String error );
}
