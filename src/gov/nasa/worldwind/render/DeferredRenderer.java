package gov.nasa.worldwind.render;

import gov.nasa.worldwind.layers.Layer;


/**
 *
 * @author vito
 */
public interface DeferredRenderer {

	public void setEnabled(boolean enabled);
	public boolean isEnabled();

	boolean isSupported(DrawContext dc);
	
	public int getColorTexture();

	public int getMaterialTexture();

	public int getNormalTexture();

	public int getDepthTexture();

	public void begin(DrawContext dc);
	public void end(DrawContext dc);

	public void renderLayer(DrawContext dc, Layer layer);
	public void renderTerrainNormals(DrawContext dc);
}
