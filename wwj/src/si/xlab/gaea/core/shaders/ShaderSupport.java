package si.xlab.gaea.core.shaders;

import gov.nasa.worldwind.geom.Matrix;

/**
 *
 * @author vito
 */
public class ShaderSupport {

	private float[] matrixToArray(Matrix m){
		double[] mvD = new double[16];
		mvD = m.toArray(mvD, 0, false);
		float[] mvF = new float[16];
		for(int i = 0; i<mvD.length; i++){
			mvF[i] = (float)mvD[i];
		}
		return mvF;
	}
}