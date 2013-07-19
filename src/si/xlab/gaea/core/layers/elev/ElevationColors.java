/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package si.xlab.gaea.core.layers.elev;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author vito
 */
public class ElevationColors {

	//////// polar ////////

	public static float[] createColorsLocalP(){

		List<Color> lColors = new ArrayList<Color>();
		List<Float> lElev = new ArrayList<Float>();

		lElev.add(-12000f);
		lColors.add(new Color(0, 4, 16));
		lElev.add(-100f);
		lColors.add(new Color(16, 51, 99));
		lElev.add(-1f);
		lColors.add(new Color(24, 73, 132));

		lElev.add(1f);
		lColors.add(new Color(186, 176, 121));
		lElev.add(400f);
		lColors.add(new Color(184, 207, 229));
		lElev.add(800f);
		lColors.add(new Color(180, 180, 180));

		float[] colors = new float[lColors.size()*4];

		for(int j = 0; j<lColors.size(); j++){

			Color color = lColors.get(j);
			float[] c = new float[3];
			c = color.getColorComponents(c);

			for(int i = 0; i<4; i++){
				if(i == 3){
					colors[(j*4+i)] = ((lElev.get(j)/12000.0f)+1.0f)/2.0f;
				}
				else{
					colors[(j*4+i)] = c[i];
				}
			}
		}

		return colors;
	}

	//// normal ////

	public static float[] createColorsLocalN(){

		List<Color> lColors = new ArrayList<Color>();
		List<Float> lElev = new ArrayList<Float>();

		lColors.add(new Color(0, 4, 16));
		lColors.add(new Color(16, 51, 99));
		lColors.add(new Color(24, 73, 132));



		lColors.add(new Color(186, 176, 121));

		lColors.add(new Color(81, 218, 82));
		lColors.add(new Color(78, 176, 138));
		lColors.add(new Color(38, 161, 107));

		lColors.add(new Color(21, 88, 57));



		lColors.add(new Color(0, 76, 1));
		lColors.add(new Color(0, 97, 0));
		lColors.add(new Color(98, 122, 48));
		lColors.add(new Color(253, 243, 171));
		lColors.add(new Color(225, 157, 82));

		lColors.add(new Color(103, 63, 38));
		lColors.add(new Color(72, 44, 20));
		lColors.add(new Color(180, 180, 180));

		lElev.add(-12000f);
		lElev.add(-100f);
		lElev.add(-1f);

//		lElev.add(1f);
//		lElev.add(100f);
//		lElev.add(230f);
//		lElev.add(300f);
//		lElev.add(400f);
//
//		lElev.add(600f);
//		lElev.add(800f);
//		lElev.add(1000f);
//		lElev.add(1500f);
//		lElev.add(2000f);
//
//		lElev.add(2500f);
//		lElev.add(3000f);
//		lElev.add(4000f);

		lElev.add(1f);
		lElev.add(50f);
		lElev.add(100f);
		lElev.add(200f);
		lElev.add(300f);

		lElev.add(400f);
		lElev.add(600f);
		lElev.add(950f);
		lElev.add(1300f);
		lElev.add(1750f);

		lElev.add(2000f);
		lElev.add(3000f);
		lElev.add(6000f);

		float[] colors = new float[lColors.size()*4];

		for(int j = 0; j<lColors.size(); j++){

			Color color = lColors.get(j);
			float[] c = new float[3];
			c = color.getColorComponents(c);

			for(int i = 0; i<4; i++){
				if(i == 3){
					colors[(j*4+i)] = ((lElev.get(j)/12000.0f)+1.0f)/2.0f;
				}
				else{
					colors[(j*4+i)] = c[i];
				}
			}
		}

		return colors;
	}

	///// desert //////

	public static float[] createColorsLocalD(){

		List<Color> lColors = new ArrayList<Color>();
		List<Float> lElev = new ArrayList<Float>();
/*
		lElev.add(-12000f);
		lColors.add(new Color(0, 4, 16));
		lElev.add(-100f);
		lColors.add(new Color(16, 51, 99));
		lElev.add(-1f);
		lColors.add(new Color(24, 73, 132));

		lElev.add(1f);
		lColors.add(new Color(186, 176, 121));
		lElev.add(50f);
		lColors.add(new Color(81, 218, 82));
		lElev.add(100f);
		lColors.add(new Color(78, 176, 138));
		lElev.add(200f);
		lColors.add(new Color(38, 161, 107));
		lElev.add(300f);
		lColors.add(new Color(21, 88, 57));
		lElev.add(400f);
		lColors.add(new Color(0, 76, 1));
		lElev.add(600f);
		lColors.add(new Color(0, 97, 0));
		lElev.add(950f);
		lColors.add(new Color(98, 122, 48));
		lElev.add(1300f);
		lColors.add(new Color(253, 243, 171));
		lElev.add(1750f);
		lColors.add(new Color(225, 157, 82));
		lElev.add(2000f);
		lColors.add(new Color(103, 63, 38));
		lElev.add(3000f);
		lColors.add(new Color(72, 44, 20));
		lElev.add(6000f);
		lColors.add(new Color(180, 180, 180));
*/
		lElev.add(-12000f);
		lColors.add(new Color(0, 4, 16));
		lElev.add(-100f);
		lColors.add(new Color(16, 51, 99));
		lElev.add(-1f);
		lColors.add(new Color(24, 73, 132));

		lElev.add(1f);
		lColors.add(new Color(186, 176, 121));
		lElev.add(600f);
		lColors.add(new Color(253, 243, 171));
		lElev.add(1500f);
		lColors.add(new Color(225, 157, 82));
		lElev.add(2000f);
		lColors.add(new Color(103, 63, 38));
		lElev.add(3000f);
		lColors.add(new Color(72, 44, 20));
		lElev.add(3500f);
		lColors.add(new Color(180, 180, 180));

		float[] colors = new float[lColors.size()*4];

		for(int j = 0; j<lColors.size(); j++){

			Color color = lColors.get(j);
			float[] c = new float[3];
			c = color.getColorComponents(c);

			for(int i = 0; i<4; i++){
				if(i == 3){
					colors[(j*4+i)] = ((lElev.get(j)/12000.0f)+1.0f)/2.0f;
				}
				else{
					colors[(j*4+i)] = c[i];
				}
			}
		}

		return colors;
	}


	///// equator /////

	public static float[] createColorsLocalE(){

		List<Color> lColors = new ArrayList<Color>();
		List<Float> lElev = new ArrayList<Float>();

		lElev.add(-12000f);
		lColors.add(new Color(0, 4, 16));
		lElev.add(-100f);
		lColors.add(new Color(16, 51, 99));
		lElev.add(-1f);
		lColors.add(new Color(24, 73, 132));

		lElev.add(1f);
		lColors.add(new Color(186, 176, 121));
		lElev.add(30f);
		lColors.add(new Color(81, 218, 82));
		lElev.add(50f);
		lColors.add(new Color(78, 176, 138));
		lElev.add(200f);
		lColors.add(new Color(38, 161, 107));
		lElev.add(500f);
		lColors.add(new Color(21, 88, 57));
		lElev.add(700f);
		lColors.add(new Color(0, 76, 1));
		lElev.add(1000f);
		lColors.add(new Color(0, 97, 0));
		lElev.add(1300f);
		lColors.add(new Color(98, 122, 48));
		lElev.add(1600f);
		lColors.add(new Color(253, 243, 171));
		lElev.add(1800f);
		lColors.add(new Color(225, 157, 82));
		lElev.add(2000f);
		lColors.add(new Color(103, 63, 38));
		lElev.add(4000f);
		lColors.add(new Color(72, 44, 20));
		lElev.add(4500f);
		lColors.add(new Color(180, 180, 180));

		float[] colors = new float[lColors.size()*4];

		for(int j = 0; j<lColors.size(); j++){

			Color color = lColors.get(j);
			float[] c = new float[3];
			c = color.getColorComponents(c);

			for(int i = 0; i<4; i++){
				if(i == 3){
					colors[(j*4+i)] = ((lElev.get(j)/12000.0f)+1.0f)/2.0f;
				}
				else{
					colors[(j*4+i)] = c[i];
				}
			}
		}

		return colors;
	}

}
