package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.util.List;
import java.util.logging.Level;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public class BasicSurfaceLines extends AbstractSurfaceLines{

	public BasicSurfaceLines(List<SurfaceLineSegment> lines,
							Sector sector,
							KMLStyle style){
	
		super(lines, sector, style);
	}

	@Override
	protected void doRender(DrawContext dc) {

		try
		{
            GL2 gl2 = dc.getGL().getGL2();

			gl2.glColor4f(color[0], color[1], color[2], color[3] * this.opacity);

			///////RENDER///////////////////////////////////////////////////////////
			gl2.glEnableClientState(GL2.GL_VERTEX_ARRAY);
            gl2.glEnableClientState(GL2.GL_COLOR_ARRAY);
			gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, vboV[0]);
			gl2.glVertexPointer(2, GL.GL_FLOAT, 0, 0);
            gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, vboC[0]);
			gl2.glColorPointer(4, GL.GL_UNSIGNED_BYTE, 0, 0);
            
			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboI[currentLevel]);
			gl2.glLineWidth(this.lineWidth);
			gl2.glDrawElements(GL.GL_LINES, numberOfPoints[currentLevel], GL.GL_UNSIGNED_INT, 0);

			gl2.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
			gl2.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl2.glDisableClientState(GL2.GL_COLOR_ARRAY);
			gl2.glDisableClientState(GL2.GL_VERTEX_ARRAY);
			////////////////////////////////////////////////////////////////////////
			
			gl2.glLineWidth(1.0f);
		}
		catch(Exception e)
		{
			String message = Logging.getMessage("BasicSurfaceLine.ExceptionWhileRenderingTile",
                            (this.toString()));
            Logging.logger().log(Level.SEVERE, message, e);
		}
	}
}
