package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import java.util.List;
import java.util.logging.Level;
import javax.media.opengl.GL;

/**
 *
 * @author vito
 */
public class BasicSurfacePolygons extends AbstractSurfacePolygons{

	public BasicSurfacePolygons(List<SurfacePolygon> polygons,
							Sector sector,
							KMLStyle style, boolean useDisplayList){

		super(polygons, sector, style, useDisplayList);
	}

	public BasicSurfacePolygons(List<SurfacePolygon> polygons,
							Sector sector,
							KMLStyle style){

		super(polygons, sector, style);
	}

	@Override
	protected void doRender(DrawContext dc, SurfacePolygon polygon) {

		try
		{
			GL gl = dc.getGL();

			//set current style
			SurfacePolygon.PolygonStyle currentStyle = polygon.getStyle() == null ? this.style : polygon.getStyle();

			polygon.render(dc, currentStyle, this.opacity);

		}
		catch(Exception e)
		{
			String message = Logging.getMessage("SceneController.ExceptionWhileRenderingLayer",
                            (this.toString()));
            Logging.logger().log(Level.SEVERE, message, e);
		}
	}
}