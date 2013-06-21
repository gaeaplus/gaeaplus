package si.xlab.gaea.core.render.surfaceobjects;

import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.GLDisposable;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;
import si.xlab.gaea.core.layers.RenderToTextureLayer;

/**
 *
 * @author vito
 */
public abstract class AbstractSurfaceLines extends SurfaceObject implements GLDisposable
{

//	private static final int NUMBER_OF_LEVELS = 18;
//	private static final double FIRST_LEVEL_ANGLE_DELTA = 0.0005;
//	private static final double NEXT_LEVEL_ANGLE_MULTIPLY = 0.0005;
    private static final int NUMBER_OF_LEVELS = 3;
    private static final double FIRST_LEVEL_ANGLE_DELTA = 0.0015;
    private static final double NEXT_LEVEL_ANGLE_MULTIPLY = 0.0030;
    protected final Sector sector;
    protected Sector textureTileSector;
    protected float[] color;
    protected float lineWidthMax = 6.0f;
    protected float lineWidth = 2.0f;
    protected float lineWidthBase = 2.0f;
    protected float lineWidthBack = 4;
    protected float lineWidthBackBase = 4;
    protected List<SurfaceLineSegment> allSegments;
    protected FloatBuffer vboBuffer;
    protected ByteBuffer vboBufferColor;
    protected IntBuffer[] vboBufferI = new IntBuffer[NUMBER_OF_LEVELS];
    protected int[] vboV = new int[1];
    protected int[] vboI = new int[NUMBER_OF_LEVELS];
    protected int[] vboC = new int[1];
    protected int[] numberOfPoints = new int[NUMBER_OF_LEVELS];
    protected int currentLevel = 0;
    private boolean readyToRender;
    private final long sizeInBytes;

    public AbstractSurfaceLines(List<SurfaceLineSegment> lines,
            Sector sector,
            KMLStyle style)
    {
		super();
        this.allSegments = new ArrayList<SurfaceLineSegment>(lines.size());
        this.allSegments.addAll(lines);
        this.sector = sector;
        this.readyToRender = false;

        Color colorI;
        if (style != null)
        {
            colorI = KMLStyleFactory.decodeHexToColor(style.getLineStyle().getColor());
            lineWidth = style.getLineStyle().getWidth().floatValue();
            lineWidthBase = style.getLineStyle().getWidth().floatValue();
        } else
        {
            colorI = Color.lightGray;
        }
        color = new float[4];
        colorI.getColorComponents(color);
        color[3] = colorI.getAlpha()/255.0f; //getColorComponents sometimes does not write alpha!?
        makeBuffers();
        this.sizeInBytes = calcSize();
    }

    @Override
    public Sector getSector()
    {
        return this.sector;
    }

	/*
    public void setOpacity(float opacity)
    {
        color[3] =opacity;
    }
	*/

    private void init(DrawContext dc)
    {

        doVBO(dc);

        GL gl = dc.getGL();

        int[] maxLineWidth = new int[2];
        gl.glGetIntegerv(GL.GL_SMOOTH_LINE_WIDTH_RANGE, maxLineWidth, 0);
        this.lineWidthMax = maxLineWidth[1];
    }

    public long getSize()
    {
        return this.sizeInBytes;
    }

    private long calcSize()
    {
        long vboBufferSize = 0;
        long vboBufferISize = 0;

        if (vboBuffer != null)
        {
            vboBufferSize = vboBuffer.limit() * Buffers.SIZEOF_FLOAT;
        }

        for (int i = 0; i < vboBufferI.length; i++)
        {
            if (vboBufferI[i] != null)
            {
                vboBufferISize = vboBufferISize + vboBufferI[i].limit() * Buffers.SIZEOF_INT;
            }
        }

        return vboBufferSize + vboBufferISize;
    }

    private Vec4 getNearestPointInSector(DrawContext dc, Sector sector)
    {
        Position eyePos = dc.getView().getEyePosition();
        if (sector.contains(eyePos))
        {
            Vec4 point = dc.getPointOnTerrain(eyePos.getLatitude(), eyePos.getLongitude());
            if (point == null)
            {
                double elev = dc.getGlobe().getElevation(eyePos.getLatitude(), eyePos.getLongitude())
                        * dc.getVerticalExaggeration();
                return dc.getGlobe().computePointFromPosition(eyePos.getLatitude(), eyePos.getLongitude(),
                        elev);
            }

            return point;
        }

        LatLon nearestCorner = null;
        Angle nearestDistance = Angle.fromDegrees(Double.MAX_VALUE);
        for (LatLon ll : sector)
        {
            Angle d = LatLon.greatCircleDistance(ll, eyePos);
            if (d.compareTo(nearestDistance) < 0)
            {
                nearestCorner = ll;
                nearestDistance = d;
            }
        }

        if (nearestCorner == null)
            return null;

        Vec4 point = dc.getPointOnTerrain(nearestCorner.getLatitude(), nearestCorner.getLongitude());
        if (point != null)
            return point;

        double elev = dc.getGlobe().getElevation(nearestCorner.getLatitude(), nearestCorner.getLongitude())
                * dc.getVerticalExaggeration();
        return dc.getGlobe().computePointFromPosition(nearestCorner.getLatitude(), nearestCorner.getLongitude(),
                elev);
    }

    @Override
    public void preRender(DrawContext dc, Sector textureSector)
    {
        int texWidth = RenderToTextureLayer.TEXTURE_WIDTH;
        int texHeight = RenderToTextureLayer.TEXTURE_HEIGHT;

        double texelD = (textureSector.getDeltaLatDegrees()) / texWidth;
        int level = 0;
        while (FIRST_LEVEL_ANGLE_DELTA + (level * NEXT_LEVEL_ANGLE_MULTIPLY) <= (texelD * 4.0)
                && NUMBER_OF_LEVELS > level)
        {
            level = level + 1;
        }

        if (level >= NUMBER_OF_LEVELS - 1)
            level = NUMBER_OF_LEVELS - 1;

        currentLevel = level;

        textureTileSector = textureSector;
        doCalc(dc, textureSector, texWidth, texHeight);

        //compute line width
        Vec4 nearestPoint = getNearestPointInSector(dc, textureSector);
        double texelSize = nearestPoint.getLength3() * textureSector.getDeltaLatRadians() / (texHeight);

        // Compute the size in meters that a screen pixel would cover on the surface.
        double eyeToSurfaceDistance = dc.getView().getEyePoint().distanceTo3(nearestPoint);
        double pixelSize = dc.getView().computePixelSizeAtDistance(eyeToSurfaceDistance);
        double fac = Math.max(dc.getView().getPitch().cos(), Math.cos(Math.toRadians(60)));

        this.lineWidth = Math.max((float) (((double) this.lineWidthBase * pixelSize) / (texelSize * fac)), 1.5f);
        this.lineWidthBack = Math.max((float) (((double) this.lineWidthBackBase * pixelSize) / (texelSize * fac)), 1.5f);

        float wM = Math.max(lineWidth, lineWidthBack);
        if (wM > this.lineWidthMax)
        {
            float k = this.lineWidthMax / wM;
            this.lineWidth = k * this.lineWidth;
            this.lineWidthBack = k * this.lineWidthBack;
        }
    }

    @Override
    public void render(DrawContext dc)
    {

        if (!readyToRender)
        {
            init(dc);
            this.readyToRender = true;
            return;
        }

        if (numberOfPoints[currentLevel] == 0)
            return;

        doRender(dc);
    }

    protected abstract void doRender(DrawContext dc);

    protected void doCalc(DrawContext dc, Sector textureSector, int textureW, int textureH)
    {
    }

    ;

	private void makeBuffers()
    {

        HashSet<LatLon> allVertexSet = new HashSet<LatLon>();
        HashSet<LatLon> allFirstAndLastVertexSet = new HashSet<LatLon>();
        ArrayList<SurfaceLineSegment> segments = new ArrayList<SurfaceLineSegment>();

        SurfaceLineSegment currentSegment = new SurfaceLineSegment();

        for (SurfaceLineSegment segment : allSegments)
        {
            currentSegment.setStyle(segment.getStyle());
            List<LatLon> coord = segment.getVertexList();
            boolean first = false;

            for (int j = 0; j < segment.getSize() - 1; j++)
            {

                if (((sector.contains(coord.get(j))) && !(sector.contains(coord.get(j + 1))))
                        || (!(sector.contains(coord.get(j))) && (sector.contains(coord.get(j + 1))))
                        || (sector.contains(coord.get(j)) && sector.contains(coord.get(j + 1))))
                {

                    //TODO: fix duplicate lines elimination - this feature is currently disabled
                    if (!(allVertexSet.contains(coord.get(j)))
                            || !(allVertexSet.contains(coord.get(j + 1))) || true)
                    {

                        allVertexSet.add(coord.get(j));
                        currentSegment.add(coord.get(j));

                        if (j == segment.getSize() - 2)
                        {
                            allVertexSet.add(coord.get(j + 1));
                            currentSegment.add(coord.get(j + 1));
                        }
                        first = true;
                        if ((sector.contains(coord.get(j))) && !(sector.contains(coord.get(j + 1))))
                        {
                            allVertexSet.add(coord.get(j + 1));
                            currentSegment.add(coord.get(j + 1));
                            segments.add(currentSegment);
                            currentSegment = new SurfaceLineSegment();
                            currentSegment.setStyle(segment.getStyle());
                            first = false;
                        }

                    } else if (first)
                    {
                        currentSegment.add(coord.get(j));
                        allVertexSet.add(coord.get(j));

                        segments.add(currentSegment);
                        currentSegment = new SurfaceLineSegment();
                        currentSegment.setStyle(segment.getStyle());
                        first = false;
                    }
                }
            }
            if (first == true && currentSegment.getSize() != 0)
            {
                segments.add(currentSegment);
                currentSegment = new SurfaceLineSegment();
                currentSegment.setStyle(segment.getStyle());
            }
        }

        for (SurfaceLineSegment segment : segments)
        {
            allFirstAndLastVertexSet.add(segment.getVertexList().get(0));
            allFirstAndLastVertexSet.add(segment.getVertexList().get(segment.getVertexList().size() - 1));
        }

        int size = getVertexCount(segments);
        vboBuffer = Buffers.newDirectFloatBuffer(size * 2);
        vboBufferColor = Buffers.newDirectByteBuffer(size * 4);

        for (SurfaceLineSegment segment : segments)
        {
            for (LatLon ll : segment.getVertexList())
            {
                //put one latlon in buffer
                putBuffer(vboBuffer, ll);
                putBufferColor(vboBufferColor, segment.getStyle());
            }
        }

        //create LODs
        for (int level = 0; level < NUMBER_OF_LEVELS; level++)
        {

            vboBufferI[level] = Buffers.newDirectIntBuffer(size * 2);

            Angle lod = Angle.fromDegrees(FIRST_LEVEL_ANGLE_DELTA + (level * NEXT_LEVEL_ANGLE_MULTIPLY));
            int temp = 0;
            int counter = 0;
            LatLon lastPosition = LatLon.ZERO;

            for (int segNumber = 0; segNumber < segments.size(); segNumber++)
            {

                SurfaceLineSegment segment = segments.get(segNumber);
                int j = segment.getSize();

                for (int i = 0; i < j; i++)
                {

                    if (i == 0 || i == (j - 1))
                    {
                        vboBufferI[level].put(i + temp);
                        lastPosition = segment.getVertexList().get(i);
                        counter = counter + 1;
                    } else if (level == 0)
                    {
                        vboBufferI[level].put(i + temp);
                        vboBufferI[level].put(i + temp);
                        counter = counter + 2;
                    } else if (level != 0)
                    {

                        LatLon current = segment.getVertexList().get(i);
                        Angle d = LatLon.greatCircleDistance(current, lastPosition);

                        int compare = d.compareTo(lod);

                        if (compare > 0 || allFirstAndLastVertexSet.contains(current))
                        {

                            vboBufferI[level].put(i + temp);
                            vboBufferI[level].put(i + temp);
                            counter = counter + 2;
                            lastPosition = current;
                        }
                    }
                }
                temp = temp + j;
            }
            if (counter != 0)
            {
                vboBufferI[level].limit(counter);
            } else
            {
                vboBufferI[level] = null;
            }
        }

        allVertexSet.clear();
        allVertexSet = null;
        allFirstAndLastVertexSet.clear();
        allFirstAndLastVertexSet = null;
        segments.clear();
        segments = null;

        allSegments.clear();
        allSegments = null;
    }

    private int getVertexCount(ArrayList<SurfaceLineSegment> segmentList)
    {

        int size = 0;

        for (SurfaceLineSegment s : segmentList)
        {
            size = size + s.getSize();
        }
        return size;
    }

    private void putBuffer(FloatBuffer buffer, LatLon v)
    {

        buffer.put((float) v.getLongitude().degrees);
        buffer.put((float) v.getLatitude().degrees);
    }

    private void putBufferColor(ByteBuffer buffer, KMLStyle s)
    {
        if (s != null)
        {
            Color c = KMLStyleFactory.decodeHexToColor(s.getLineStyle().getColor());
            buffer.put((byte) (c.getRed() & 0xFF));
            buffer.put((byte) (c.getGreen() & 0xFF));
            buffer.put((byte) (c.getBlue() & 0xFF));
            buffer.put((byte) (c.getAlpha() & 0xFF));
        } else {
            for (float comp: color)
            {
                buffer.put( (byte)((int)(comp*255) & 0xFF) );
            }
        }
    }

    private void doVBO(DrawContext dc)
    {

        GL gl = dc.getGL();

        // TODO: implementiraj še barvanje glede na style
        // na koncu releasaj buffer z gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
        // za vsaki 2 števili vertexov (lat, lon) morajo biti 3 floati v color arrayu
        if (!gl.glIsBuffer(vboV[0]))
        {
            gl.glGenBuffers(1, vboV, 0);
            gl.glGenBuffers(NUMBER_OF_LEVELS, vboI, 0);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboV[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBuffer.limit()
                * Buffers.SIZEOF_FLOAT, vboBuffer.rewind(), GL.GL_STATIC_DRAW);

        if (!gl.glIsBuffer(vboC[0]))
        {
            gl.glGenBuffers(1, vboC, 0);
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboC[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBufferColor.limit()
                * Buffers.SIZEOF_BYTE, vboBufferColor.rewind(), GL.GL_STATIC_DRAW);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        vboBuffer.clear();
        vboBuffer = null;

        for (int level = 0; level < NUMBER_OF_LEVELS; level++)
        {
            if (vboBufferI[level] != null && vboBufferI[level].limit() != 0)
            {

                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, vboI[level]);
                gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, vboBufferI[level].limit()
                        * Buffers.SIZEOF_INT, vboBufferI[level].rewind(), GL.GL_STATIC_DRAW);

                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
                numberOfPoints[level] = vboBufferI[level].limit();

                vboBufferI[level].clear();
                vboBufferI[level] = null;
            } else
            {
                numberOfPoints[level] = 0;
            }
        }

        vboBufferI = null;
    }

    public void dispose()
    {
        WorldWind.getGLTaskService().addDisposable(this);
    }

    @Override
    public void dispose(GL2 gl)
    {
        gl.glDeleteBuffers(1, vboV, 0);
        vboV = null;
        gl.glDeleteBuffers(NUMBER_OF_LEVELS, vboI, 0);
        vboI = null;
    }
}
