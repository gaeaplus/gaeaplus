/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Plane;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;
import javax.media.opengl.glu.GLUtessellatorCallback;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;

/**
 *
 * @author marjan
 */
public class SurfacePolygon
{

    protected static class PolygonStyle
    {

        public float[] colorFill = new float[3];
        public float[] colorLine = new float[3];
        protected boolean isOutline;
        protected boolean isFill;
        public float opacityFill;
        public float opacityLine;
        public float lineWidth;

        public PolygonStyle(KMLStyle style)
        {
            Color colorF;
            if (style != null && style.getPolyStyle() != null)
            {
                colorF = KMLStyleFactory.decodeHexToColor(style.getPolyStyle().getColor());
                if (style.getPolyStyle().isFill() == false)
                {
                    this.isFill = false;
                    this.opacityFill = 0.0F;
                } else
                {
                    this.isFill = true;
                    this.opacityFill = ((float) colorF.getAlpha()) / 255.0F;
                }
                if (style.getPolyStyle().isOutline() == false)
                    this.isOutline = false;
                else
                    this.isOutline = true;
            } else
            {
                colorF = new Color(0, 135, 60);
                this.opacityFill = 0.4F;
                this.isFill = true;
                this.isOutline = true;
            }
            this.colorFill = colorF.getRGBColorComponents(this.colorFill);
            Color colorL;
            if (style != null && style.getLineStyle() != null)
            {
                colorL = KMLStyleFactory.decodeHexToColor(style.getLineStyle().getColor());
                this.lineWidth = style.getLineStyle().getWidth().floatValue();
                this.opacityLine = ((float) colorL.getAlpha()) / 255.0F;
            } else
            {
                colorL = new Color(104, 232, 140);
                this.lineWidth = 3.0F;
                this.opacityLine = 1.0F;
            }
            this.colorLine = colorL.getRGBColorComponents(this.colorLine);
        }

        public long getSize()
        {
            return 37L;
        }
    }
    private static final long serialVersionUID = 1L;
    //input data
    private List<LatLon> vertexListOuter;
    private List<List<LatLon>> vertexListInner;
    List<TessellatedPolyPart> tessellatedPolyParts;
    List<List<LatLon>> cutPolyContours;
    PolygonStyle style;
    private Sector sector;

    public SurfacePolygon(String name, List<LatLon> latLonOuter, List<List<LatLon>> latLonInner)
    {
        vertexListOuter = latLonOuter;
        vertexListInner = latLonInner;
        this.style = null;
        this.sector = null;
    }

    public SurfacePolygon(String name, List<LatLon> latLonOuter, List<List<LatLon>> latLonInner, KMLStyle style)
    {
        this(name, latLonOuter, latLonInner);
        if (style != null)
        {
            this.style = new PolygonStyle(style);
        }
    }

    /**
     * Set sector for render visibility check.
     *
     * @param sector
     */
    public void setSector(Sector sector)
    {
        this.sector = sector;
    }

    /**
     *
     * @return sector - can be null if not set!
     */
    public Sector getSector()
    {
        return this.sector;
    }

    protected PolygonStyle getStyle()
    {
        return this.style;
    }

    public Sector getBoundingSector()
    {
        return Sector.boundingSector(vertexListOuter);
    }

    /**
     * Clip contour to sector.
     * Don't call this method unless you know what you are doing.
     *
     * @param sector the sector used for clipping.
     */
    public void clipContour(Sector sector)
    {
        cutPolyContours = new ArrayList<List<LatLon>>();
        Sector dataSector = Sector.boundingSector(vertexListOuter);
        if (sector.contains(dataSector))
        {
            cutPolyContours.add(vertexListOuter);
            // Only step through inner lists when they are really present.
            if (vertexListInner != null)
            {
                for (List<LatLon> innerContour : vertexListInner)
                {
                    cutPolyContours.add(innerContour);
                }
            } // if
            return;
        }
        Plane left = new Plane(new Vec4(1.0, 0.0, 0.0, -sector.getMinLongitude().degrees));
        Plane right = new Plane(new Vec4(-1.0, 0.0, 0.0, sector.getMaxLongitude().degrees));
        Plane bottom = new Plane(new Vec4(0.0, 1.0, 0.0, -sector.getMinLatitude().degrees));
        Plane top = new Plane(new Vec4(0.0, -1.0, 0.0, sector.getMaxLatitude().degrees));
        Plane far = new Plane(new Vec4(0.0, 0.0, 1.0, 1.0));
        Plane near = new Plane(new Vec4(0.0, 0.0, 1.0, 1.0));
        Frustum frustum = new Frustum(left, right, bottom, top, near, far);
        LatLon lastPos = null;
        List<LatLon> currentContourSegment = new ArrayList<LatLon>();
        for (LatLon ll : vertexListOuter)
        {
            if (lastPos == null)
            {
                if (sector.contains(ll))
                {
                    currentContourSegment.add(ll);
                    lastPos = ll;
                    continue;
                } else
                {
                    lastPos = ll;
                    continue;
                }
            }
            Vec4 lastVec = new Vec4(lastPos.longitude.degrees, lastPos.latitude.degrees);
            Vec4 currentVec = new Vec4(ll.longitude.degrees, ll.latitude.degrees);
            Vec4[] inter = Line.clipToFrustum(lastVec, currentVec, frustum);
            if (inter != null)
            {
                //put point
                for (Vec4 v : inter)
                {
                    LatLon llAdd = new LatLon(Angle.fromDegrees(v.y), Angle.fromDegrees(v.x));
                    currentContourSegment.add(llAdd);
                }
                boolean isEndPos = !sector.contains(ll) ? true : false;
                if (isEndPos)
                {
                    cutPolyContours.add(currentContourSegment);
                    currentContourSegment = new ArrayList<LatLon>();
                }
            }
            lastPos = ll;
        }
        cutPolyContours.add(currentContourSegment);
        if (vertexListInner == null)
        {
            return;
        }
        for (List<LatLon> innerContour : vertexListInner)
        {
            lastPos = null;
            currentContourSegment = new ArrayList<LatLon>();
            for (LatLon ll : innerContour)
            {
                if (lastPos == null)
                {
                    if (sector.contains(ll))
                    {
                        currentContourSegment.add(ll);
                        lastPos = ll;
                        continue;
                    } else
                    {
                        lastPos = ll;
                        continue;
                    }
                }
                Vec4 lastVec = new Vec4(lastPos.longitude.degrees, lastPos.latitude.degrees);
                Vec4 currentVec = new Vec4(ll.longitude.degrees, ll.latitude.degrees);
                Vec4[] inter = Line.clipToFrustum(lastVec, currentVec, frustum);
                if (inter != null)
                {
                    //put point
                    for (Vec4 v : inter)
                    {
                        LatLon llAdd = new LatLon(Angle.fromDegrees(v.y), Angle.fromDegrees(v.x));
                        currentContourSegment.add(llAdd);
                    }
                    boolean isEndPos = !sector.contains(ll) ? true : false;
                    if (isEndPos)
                    {
                        cutPolyContours.add(currentContourSegment);
                        currentContourSegment = new ArrayList<LatLon>();
                    }
                }
                lastPos = ll;
            }
            cutPolyContours.add(currentContourSegment);
        }
    }

    public void dispose()
    {
        if (vertexListInner != null)
        {
            vertexListInner = null;
        }
        if (vertexListOuter != null)
        {
            vertexListOuter = null;
        }
        if (tessellatedPolyParts != null)
        {
            tessellatedPolyParts.clear();
            tessellatedPolyParts = null;
        }
        if (cutPolyContours != null)
        {
            cutPolyContours.clear();
            cutPolyContours = null;
        }
    }

    /**
     * Render polygon in geographic coordinates.
     * Don't call this method unless you know what you are doing.
     *
     * @param dc
     * @param style
     */
    public void render(DrawContext dc, PolygonStyle style, float opacity)
    {
        GL2 gl2 = dc.getGL().getGL2();
        
        if (style.isFill && style.opacityFill > 0.1F)
        {
            gl2.glColor4f(style.colorFill[0], style.colorFill[1], style.colorFill[2], style.opacityFill * opacity);
            //test for poly clipping
            //			gl.glColor4f((float)(Math.random()),
            //						 (float)(Math.random()),
            //						 (float)(Math.random()),
            //						 0.7f);
            for (TessellatedPolyPart tpp : tessellatedPolyParts)
            {
                tpp.render(dc);
            }
        }
        if (style.isOutline && style.opacityLine > 0.1F)
        {
            gl2.glColor4f(style.colorLine[0], style.colorLine[1], style.colorLine[2], (float)style.opacityLine * opacity);
            gl2.glLineWidth(style.lineWidth);
            if (cutPolyContours != null)
            {
                //render cut contoures
                for (List<LatLon> llList : cutPolyContours)
                {
                    gl2.glBegin(GL.GL_LINE_STRIP);
                    for (LatLon ll : llList)
                    {
                        gl2.glVertex2d(ll.longitude.degrees, ll.latitude.degrees);
                    }
                    gl2.glEnd();
                }
            }
            gl2.glLineWidth(1.0F);
        }
    }

    public void buildPoly()
    {
        this.tessellatedPolyParts = new ArrayList<TessellatedPolyPart>();
        TessellatedPolyPart pp = new TessellatedPolyPart(GL.GL_TRIANGLE_FAN);
        this.sector = Sector.boundingSector(vertexListOuter);
        pp.add(this.sector.getCentroid());
        for (LatLon ll : vertexListOuter)
        {
            pp.add(ll);
        }
        this.tessellatedPolyParts.add(pp);
        this.vertexListOuter = null;
        this.vertexListInner = null;
    }

    /**
     * Tessellate polygon interior and clip it to sector.
     * Don't call this method unless you know what you are doing.
     *
     * @param glu the OpenGL utilities library.
     * @param sector the sector used for clipping.
     */
    public void buildPolytessellate(GLU glu, Sector sector)
    {
        GLUtessellator tobj = glu.gluNewTess();
        PolygonTessCallback callback = new PolygonTessCallback();
        this.tessellatedPolyParts = new ArrayList<TessellatedPolyPart>();
        glu.gluTessProperty(tobj, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ABS_GEQ_TWO);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_VERTEX, callback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_COMBINE, callback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_BEGIN, callback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_END, callback);
        glu.gluTessCallback(tobj, GLU.GLU_TESS_ERROR, callback);
        glu.gluTessBeginPolygon(tobj, null);
        List<LatLon> sectorLatLonList = new ArrayList<LatLon>();
        sectorLatLonList.add(new LatLon(sector.getMinLatitude(), sector.getMinLongitude()));
        sectorLatLonList.add(new LatLon(sector.getMinLatitude(), sector.getMaxLongitude()));
        sectorLatLonList.add(new LatLon(sector.getMaxLatitude(), sector.getMaxLongitude()));
        sectorLatLonList.add(new LatLon(sector.getMaxLatitude(), sector.getMinLongitude()));
        sectorLatLonList.add(new LatLon(sector.getMinLatitude(), sector.getMinLongitude()));
        //			Collections.reverse(sectorLatLonList);
        //-- Outer contour
        glu.gluTessBeginContour(tobj);
        for (int i = 0; i < sectorLatLonList.size(); i++)
        {
            double[] vals = new double[3];
            vals[0] = sectorLatLonList.get(i).latitude.degrees;
            vals[1] = sectorLatLonList.get(i).longitude.degrees;
            vals[2] = 0.0;
            glu.gluTessVertex(tobj, vals, 0, vals);
        }
        glu.gluTessEndContour(tobj);
        //-- Outer contour
        glu.gluTessBeginContour(tobj);
        for (int i = 0; i < vertexListOuter.size(); i++)
        {
            double[] vals = new double[3];
            vals[0] = vertexListOuter.get(i).latitude.degrees;
            vals[1] = vertexListOuter.get(i).longitude.degrees;
            vals[2] = 0.0;
            glu.gluTessVertex(tobj, vals, 0, vals);
        }
        glu.gluTessEndContour(tobj);
        //--  Inner contour
        if (vertexListInner != null && !vertexListInner.isEmpty())
        {
            for (List<LatLon> contourInner : vertexListInner)
            {
                Collections.reverse(contourInner);
                glu.gluTessBeginContour(tobj);
                for (LatLon ll : contourInner)
                {
                    double[] vals = new double[3];
                    vals[0] = ll.latitude.degrees;
                    vals[1] = ll.longitude.degrees;
                    vals[2] = 0.0;
                    glu.gluTessVertex(tobj, vals, 0, vals);
                }
                glu.gluTessEndContour(tobj);
            }
        }
        glu.gluTessEndPolygon(tobj);
        glu.gluDeleteTess(tobj);
        this.vertexListOuter = null;
        this.vertexListInner = null;
        if (callback.hasFailed())
        {
            System.out.println(callback.getErrorMessage());
        }
    }

    public class TessellatedPolyPart
    {

        private int renderType;
        private List<LatLon> positions;

        public TessellatedPolyPart(int renderType)
        {
            this.renderType = renderType;
            this.positions = new ArrayList<LatLon>();
        }

        public void add(LatLon ll)
        {
            positions.add(ll);
        }

        public int getPositionsCount()
        {
            return positions.size();
        }
        
        public void render(DrawContext dc)
        {
            GL2 gl2 = dc.getGL().getGL2();
            gl2.glBegin(renderType);
            for (LatLon ll : positions)
            {
                gl2.glVertex2d(ll.longitude.degrees, ll.latitude.degrees);
            }
            gl2.glEnd();
        }
    }

    private class PolygonTessCallback implements GLUtessellatorCallback
    {

        private String errorMessage;
        private boolean failed;
        private TessellatedPolyPart tpp;
        private LatLon currentLatLon;

        public void begin(int i)
        {
            tpp = new TessellatedPolyPart(i);
        }

        public void end()
        {
            tessellatedPolyParts.add(tpp);
        }

        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData)
        {
            outData[0] = coords;
        }

        public void vertex(Object o)
        {
            double[] vertex = (double[]) o;
            currentLatLon = new LatLon(Angle.fromDegrees(vertex[0]), Angle.fromDegrees(vertex[1]));
            for (LatLon ll : vertexListOuter)
            {
                if (LatLon.equals(ll, currentLatLon))
                {
                    tpp.add(ll);
                    return;
                }
            }
            for (List<LatLon> ic : vertexListInner)
            {
                for (LatLon ll : ic)
                {
                    if (LatLon.equals(ll, currentLatLon))
                    {
                        tpp.add(ll);
                        return;
                    }
                }
            }
            tpp.add(currentLatLon);
        }

        public String getErrorMessage()
        {
            return errorMessage;
        }

        public boolean hasFailed()
        {
            return failed;
        }

        public void error(int errnum)
        {
            switch (errnum)
            {
                case GLU.GLU_TESS_ERROR1:
                    errorMessage = "missing gluEndPolygon";
                    break;
                case GLU.GLU_TESS_ERROR2:
                    errorMessage = "missing gluBeginPolygon";
                    break;
                case GLU.GLU_TESS_ERROR3:
                    errorMessage = "isoriented contour";
                    break;
                case GLU.GLU_TESS_ERROR4:
                    errorMessage = "vertex/edge intersection";
                    break;
                case GLU.GLU_TESS_ERROR5:
                    errorMessage = "misoriented or self-intersecting loops";
                    break;
                case GLU.GLU_TESS_ERROR6:
                    errorMessage = "coincident vertices";
                    break;
                case GLU.GLU_TESS_ERROR7:
                    errorMessage = "all vertices collinear";
                    break;
                case GLU.GLU_TESS_ERROR8:
                    errorMessage = "intersecting edges";
                    break;
                default:
                    errorMessage = "unknown tesselation error";
                    break;
            }
            AbstractSurfacePolygons.logger.warning("Tesselator ERROR: " + errorMessage);
        }

        public void beginData(int arg0, Object arg1)
        {
        }

        public void setEdgeIndices()
        {
        }

        public void edgeFlag(boolean arg0)
        {
        }

        public void edgeFlagData(boolean arg0, Object arg1)
        {
        }

        public void vertexData(Object arg0, Object arg1)
        {
        }

        public void endData(Object arg0)
        {
        }

        public void combineData(double[] arg0, Object[] arg1, float[] arg2, Object[] arg3, Object arg4)
        {
        }

        public void errorData(int arg0, Object arg1)
        {
        }
    }
    
}
