/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.worldwind.formats.dds;

/**
 * Filters implementing this interface are used (if enabled) when converting 
 * bitmaps to DDS. For example, if layer is fetched from server as jpg but
 * saved to disk cache as DDS.
 * 
 * @author marjan
 */
public interface ITransparencyFilter
{
	public void filter(ColorBlock4x4 colorBlock);
}
