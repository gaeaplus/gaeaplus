package si.xlab.gaea.core;

import gov.nasa.worldwind.render.DrawContext;
import javax.media.opengl.GL;

/**
 *
 * @author vito
 */
public class ShadowMapFactory {

	private static ShadowMap worldShadowMap;
	private static ShadowVolume worldShadowVolume;

	public static ShadowVolume getWorldShadowVolumeInstance(){
		if(worldShadowVolume == null){
			worldShadowVolume = new ShadowVolume();
		}
		return worldShadowVolume;
	}

	public static ShadowMap getWorldShadowMapInstance(DrawContext dc){

		if(worldShadowMap == null){
			GL gl = dc.getGL();
			int[] maxSize = new int[1];
			gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, maxSize, 0);
			int texSize = Math.min(maxSize[0], 2048);
			worldShadowMap = new ShadowMap(texSize, texSize);
			return worldShadowMap;
		}
		else{
			return worldShadowMap;
		}
	}
}
