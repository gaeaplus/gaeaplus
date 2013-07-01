package si.xlab.gaea.core.render;

import gov.nasa.worldwind.render.DrawContext;

/**
 *
 * @author vito
 */
public interface Shadow {

	/**
	 * Render geometry for shadow texture (shadow map). Shadow map is used
	 * for depth comparison. Only geometry (without other special effect) needs
	 * to be rendered in this method.
     */
	public void renderShadow(DrawContext dc);
}
