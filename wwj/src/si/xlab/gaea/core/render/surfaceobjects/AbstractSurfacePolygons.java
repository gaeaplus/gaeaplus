package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.GLDisposable;
import java.awt.Color;
import java.util.List;
import java.util.logging.Logger;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import si.xlab.gaea.core.render.surfaceobjects.SurfacePolygon.TessellatedPolyPart;

/**
 *
 * @author vito
 */
public abstract class AbstractSurfacePolygons extends SurfaceObject implements GLDisposable{
    protected static final Logger logger = Logger.getLogger(AbstractSurfacePolygons.class.getName());

	private static final int SIZE_OF_LATLON = 64;
	private final long sizeInBytes;

	protected Sector sector;
	protected Sector textureTileSector;

	private final boolean useDisplayList;
	private int displayList;
	private boolean readyToRender;

	private List<SurfacePolygon> allPolygons;
	protected SurfacePolygon.PolygonStyle style;

	private static GLU glu = new GLU();

	public AbstractSurfacePolygons(List<SurfacePolygon> polygons,
							   Sector sector,
							   KMLStyle style, boolean useDisplayList){

		super();
		this.style = new SurfacePolygon.PolygonStyle(style);
		this.allPolygons = polygons;
		this.sector = sector;
		this.readyToRender = false;
		this.useDisplayList = useDisplayList;

		for(SurfacePolygon polygon : allPolygons){
				polygon.clipContour(sector);
				if(polygon.style != null){
					if(polygon.style.isFill && polygon.style.opacityFill > 0.1){
						polygon.buildPolytessellate(glu, sector);
					}
				}
				else if (this.style != null){
					if(this.style.isFill && this.style.opacityFill > 0.1){
						polygon.buildPolytessellate(glu, sector);
					}
				}
		}

		if(!useDisplayList){
			readyToRender = true;
		}

		this.sizeInBytes = calcSize();
	}

	public AbstractSurfacePolygons(List<SurfacePolygon> polygons,
							   Sector sector,
							   KMLStyle style){
		this(polygons, sector, style, false);
	}

	/**
	 * Color parameter is compiled in OpenGL DisplayList. If this parameter is
	 * set after first render call and if DisplayList is enabled this parameter
	 * has no effect!
	 *
	 * @param color
	 */
	public void setFillColor(Color color){
		this.style.colorFill = color.getColorComponents(this.style.colorFill);
	}

	/**
	 * Color parameter is compiled in OpenGL DisplayList. If this parameter is
	 * set after first render call and if DisplayList is enabled this parameter
	 * has no effect!
	 *
	 * @param color
	 */
	public void setLineColor(Color color){
		this.style.colorLine = color.getRGBColorComponents(this.style.colorLine);
	}

	/**
	 * Alpha parameter is compiled in OpenGL DisplayList. If this parameter is
	 * set after first render call and if DisplayList is enabled this parameter
	 * has no effect!
	 *
	 * @param color
	 */
	public void setFillAlpha(float alpha){
		this.style.opacityFill = alpha;
	}

	/**
	 * Alpha parameter is compiled in OpenGL DisplayList. If this parameter is
	 * set after first render call and if DisplayList is enabled this parameter
	 * has no effect!
	 *
	 * @param alpha
	 */
	public void setLineAlpha(float alpha){
		this.style.opacityLine = alpha;
	}

	/**
	 * Outline parameter is compiled in OpenGL DisplayList. If this parameter is
	 * set after first render call and if DisplayList is enabled this parameter
	 * has no effect!
	 */
	public void setDrawOutline(boolean enable){
		this.style.isOutline = enable;
	}

	/**
	 * Polygon style is compiled in OpenGL DisplayList. If style is
	 * set after first render call and if DisplayList is enabled this parameter
	 * has no effect!
	 */
	public void setDefaultStyle(){
		this.style = new SurfacePolygon.PolygonStyle(null);
	}

    public void setStyle(KMLStyle s)
    {
        this.style = new SurfacePolygon.PolygonStyle(s);
    }

	/**
	 *
	 * @return size in bytes
	 */
	public long getSize(){
		return sizeInBytes;
	}

	@Override
	public Sector getSector() {
		return this.sector;
	}

	private long calcSize(){
		long returnSize = 1;

		for(SurfacePolygon poly : this.allPolygons){

			long sizePoly = 1l;

			if(poly.tessellatedPolyParts != null){
				for(TessellatedPolyPart part : poly.tessellatedPolyParts){
					sizePoly = sizePoly + (part.getPositionsCount() * (SIZE_OF_LATLON + 8));
					sizePoly = sizePoly + 12l;
				}
			}

			if(poly.cutPolyContours != null){
				for(List<LatLon> contour : poly.cutPolyContours){
					sizePoly = sizePoly + (contour.size() * (SIZE_OF_LATLON + 8));
				}
			}

			if(style != null){
				sizePoly = sizePoly + style.getSize() + 8l;
			}

			if(sector != null){
				sizePoly = sizePoly + 32 + 8;
			}

			returnSize = returnSize + sizePoly;
		}

		return returnSize;
	}

	private void init(DrawContext dc){

		GL2 gl2 = dc.getGL().getGL2();

		if(useDisplayList){
			// create display list
			if(!gl2.glIsList(displayList))
				displayList = gl2.glGenLists(1);

			// compile display list
			gl2.glNewList(displayList, GL2.GL_COMPILE);
			for(SurfacePolygon polygon : allPolygons){
				doRender(dc, polygon);
				polygon.dispose();
			}
			gl2.glEndList();
			gl2.glCallList(displayList);
		}

		readyToRender = true;
	}

	@Override
	public void preRender(DrawContext dc, Sector textureTileSector) {

		this.textureTileSector = textureTileSector;
	}

	@Override
	public void render(DrawContext dc) {

		GL2 gl2 = dc.getGL().getGL2();

		if(!readyToRender){
			init(dc);
			return;
		}

		if(useDisplayList){

			/* two pass alpha blend
			gl.glBlendFunc(GL.GL_ZERO, GL.GL_SRC_ALPHA);
			gl.glCallList(displayList);
			gl.glBlendFunc(GL.GL_CONSTANT_ALPHA, GL.GL_ONE_MINUS_CONSTANT_ALPHA);
			*/

			gl2.glCallList(displayList);
		}
		else{
			for(SurfacePolygon polygon : allPolygons){
				if(polygon.getSector() != null){
					if(polygon.getSector().intersects(textureTileSector)){
						doRender(dc, polygon);
					}
				}
				else{
					doRender(dc, polygon);
				}
			}
		}
	}

	protected abstract void doRender(DrawContext dc, SurfacePolygon polygon);

	public void dispose(){
		WorldWind.getGLTaskService().addDisposable(this);
	}

    @Override
	public void dispose(GL2 gl) {
		for(SurfacePolygon polygon : allPolygons){
			polygon.dispose();
			polygon = null;
		}
		this.allPolygons.clear();
		this.allPolygons = null;

		if(useDisplayList){
			gl.glDeleteLists(displayList, 1);
		}
	}
}