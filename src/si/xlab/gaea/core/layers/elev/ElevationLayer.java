package si.xlab.gaea.core.layers.elev;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DeferredRenderer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.SectorGeometry;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import si.xlab.gaea.core.shaders.Shader;

public class ElevationLayer extends RenderableLayer {

	private int colorTexture = 0;

	private float[] getColorAtElevation(float[] colors, float elev) {
		float[] color = new float[]{colors[0], colors[1], colors[2], colors[3]};

		for (int i = 0; i < colors.length; i = i + 4) {
			if (elev > colors[i + 3]) {
				color = new float[]{colors[i], colors[i + 1], colors[i + 2], colors[i + 3]};
			} else {
				float elevInterval = (colors[i + 3] - color[3]);
				float blendFactor = (elevInterval == 0.0f) ? 0.0f : (elev - color[3]) / (elevInterval);
				color = new float[]{(1.0f - blendFactor) * color[0] + blendFactor * colors[i],
					(1.0f - blendFactor) * color[1] + blendFactor * colors[i + 1],
					(1.0f - blendFactor) * color[2] + blendFactor * colors[i + 2],
					elev
				};
				break;
			}
		}

		return color;
	}

	private int createTexture(GL gl, List<float[]> colors) {

		int width = 2048;
		int height = colors.size();

		ByteBuffer textureData = Buffers.newDirectByteBuffer(3 * width * height);

		for (int i = 0; i < colors.size(); i++) {
			for (int j = 0; j < width; j++) {
				float elev = ((float) j) / (float) width;
				float[] color = getColorAtElevation(colors.get(i), elev);

				textureData.put((byte) (color[0] * 255.0f));
				textureData.put((byte) (color[1] * 255.0f));
				textureData.put((byte) (color[2] * 255.0f));
			}
		}

		textureData.rewind();

		int[] texture = new int[1];
		gl.glGenTextures(1, texture, 0);

		gl.glBindTexture(GL.GL_TEXTURE_2D, texture[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGB8,
			width, height, 0, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, textureData);

		textureData.clear();
		return texture[0];
	}

	@Override
	public void render(DrawContext dc) {
		GL2 gl = dc.getGL().getGL2();

		if (!isEnabled()) {
			return;
		}

		DeferredRenderer dr = dc.getDeferredRenderer();
		if(!dr.isEnabled()){
			dr.begin(dc);
			renderTerrain(dc);
			dr.renderTerrainNormals(dc);
			dr.end(dc);
			dc.setCurrentLayer(this);
		}

		gl.glPushAttrib(GL2.GL_ENABLE_BIT
			| GL2.GL_VIEWPORT_BIT
			| GL2.GL_COLOR_BUFFER_BIT
			| GL2.GL_DEPTH_BUFFER_BIT);
		
		gl.glDepthFunc(GL.GL_LEQUAL);

		if (colorTexture == 0) {
			List<float[]> colors = new ArrayList<float[]>();

			colors.add(ElevationColors.createColorsLocalN());

			colorTexture = createTexture(gl, colors);
		}

		dc.getShaderContext().pushShader();

		Shader shader = dc.getShaderContext().getShader("TerrainElevation.glsl", "");

		if (shader.isValid()) {
			shader.enable(dc.getShaderContext());

			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL.GL_TEXTURE_2D, dc.getDeferredRenderer().getNormalTexture());
			gl.glActiveTexture(GL.GL_TEXTURE1);
			gl.glBindTexture(GL.GL_TEXTURE_2D, colorTexture);
			gl.glActiveTexture(GL.GL_TEXTURE0);

			shader.setParam("normalTex", 0);
			shader.setParam("elevationColorTex", 1);

			renderTerrain(dc);

			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
			gl.glActiveTexture(GL.GL_TEXTURE1);
			gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
			gl.glActiveTexture(GL.GL_TEXTURE0);

			shader.disable(dc.getShaderContext());
		}

		dc.getShaderContext().popShader();

		gl.glPopAttrib();
	}

	private void renderTerrain(DrawContext dc){
		dc.getSurfaceGeometry().beginRendering(dc);
		for(SectorGeometry sg : dc.getSurfaceGeometry()){
			sg.beginRendering(dc, 0);
			sg.renderMultiTexture(dc, 0);
			sg.endRendering(dc);
		}
		dc.getSurfaceGeometry().endRendering(dc);
	}
}
