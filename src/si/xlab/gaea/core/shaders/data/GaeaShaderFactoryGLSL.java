package si.xlab.gaea.core.shaders.data;

import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.BasicShaderFactory;
import si.xlab.gaea.core.layers.atm.Atmosphere;

/**
 *
 * @author vito
 */
public class GaeaShaderFactoryGLSL extends BasicShaderFactory{

	private final Atmosphere atmosphere = Atmosphere.getInstance();

	private void initAtmosphere(DrawContext dc){
		if(!atmosphere.isTexturesDone()){
			atmosphere.precompute(dc);
		}
	}
}
