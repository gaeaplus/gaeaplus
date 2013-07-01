/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.ogc.collada;

import gov.nasa.worldwind.util.xml.*;

/**
 * Base class for COLLADA parser classes.
 *
 * @author pabercrombie
 * @version $Id: ColladaAbstractObject.java 654 2012-06-25 04:15:52Z pabercrombie $
 */
public abstract class ColladaAbstractObject extends AbstractXMLEventParser
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    protected ColladaAbstractObject(String namespaceURI)
    {
        super(namespaceURI);
    }

    /** {@inheritDoc} Overridden to return ColladaRoot instead of a XMLEventParser. */
    @Override
    public ColladaRoot getRoot()
    {
        XMLEventParser root = super.getRoot();
        return root instanceof ColladaRoot ? (ColladaRoot) root : null;
    }
}
