/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;

/**
 *
 * @author vito
 */
public interface GLTask {

	void run(DrawContext dc);
}
