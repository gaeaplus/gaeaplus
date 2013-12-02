package si.xlab.gaea.core.layers.elev;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.DeferredRenderer;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.Shader;
import gov.nasa.worldwind.terrain.SectorGeometry;
import java.beans.PropertyChangeEvent;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public class SlopeLayer extends RenderableLayer{

	private Angle maxAngle = Angle.fromDegrees(30);

	public SlopeLayer() {
		this.setOpacity(0.6);
	}

	public void setMaxAngle(Angle angle){
		this.maxAngle = angle;
		this.firePropertyChange(AVKey.REPAINT, null, null);
	}
	
	@Override
	public void render(DrawContext dc)
	{
		super.render(dc);

		if(!isEnabled()){
			return;
		}

		GL2 gl = dc.getGL().getGL2();
		
		gl.glPushAttrib(GL2.GL_ENABLE_BIT 
				| GL2.GL_VIEWPORT_BIT 
				| GL.GL_COLOR_BUFFER_BIT 
				| GL.GL_DEPTH_BUFFER_BIT);
		DeferredRenderer dr = dc.getDeferredRenderer();
		if(!dr.isEnabled()){
			dr.begin(dc);
			renderTerrain(dc);
			dr.renderTerrainNormals(dc);
			dr.end(dc);
			dc.setCurrentLayer(this);
		}
		gl.glPopAttrib();

		gl.glPushAttrib(GL2.GL_ENABLE_BIT 
				| GL2.GL_VIEWPORT_BIT 
				| GL.GL_COLOR_BUFFER_BIT 
				| GL.GL_DEPTH_BUFFER_BIT);

		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glBindTexture(GL.GL_TEXTURE_2D, dr.getNormalTexture());

		gl.glEnable(GL.GL_BLEND);
		gl.glDepthFunc(GL.GL_LEQUAL);
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		Shader shader = dc.getShaderContext().getShader("SlopeLayer.glsl", "#version 120\n");
		dc.getShaderContext().enable(shader);

		shader.setParam("maxSlope", new float[]{(float)(maxAngle.radians)});
		shader.setParam("color", new float[]{1.0f, 0.0f, 0.0f, (float)getOpacity()});
		shader.setParam("normalTex", 0);
		
		renderTerrain(dc);
		
		gl.glDisable(GL.GL_BLEND);
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

	@Override
	public void propertyChange(PropertyChangeEvent propertyChangeEvent)
	{
		if(propertyChangeEvent.getPropertyName().equals("maxAngle")){
			if(propertyChangeEvent.getNewValue() instanceof Double){
				setMaxAngle(Angle.fromDegrees((Double)(propertyChangeEvent.getNewValue())));
			}
		}
	}
}
