package si.xlab.gaea.core.render.surfaceobjects;

import com.jogamp.opengl.util.awt.TextRenderer;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Plane;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.OGLTextRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import javax.media.opengl.GL2;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;

/**
 *
 * @author Vito Čuček <vito.cucek@xlab.si / vito.cucek@gmail.com>
 */
public class SurfaceLineFloatingLabels extends SurfaceLineLabel{

    private final List<SurfaceLineSegment> lines;
    private static TextRenderer textRendererScreen;

    //font style
    private Font font = Font.decode("TimesNewRoman-BOLD-10");
    private Color color = null;
    private float scaleFont = 1.0f;

    private final Object lock = new Object();
    private final HashSet<ProjectLineLableTask> commitedTasks = new HashSet<ProjectLineLableTask>();
    private final HashSet<ProjectLineLableTask> commitedTasksNew = new HashSet<ProjectLineLableTask>();
    
    private static final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
    protected static final Logger logger = Logger.getLogger(SurfaceLineFloatingLabels.class.getName());
    
    public SurfaceLineFloatingLabels(List<SurfaceLineSegment> lineSegments, Font font, KMLStyle style){
        this.lines = new ArrayList<SurfaceLineSegment>();

        for(SurfaceLineSegment line : lineSegments){
            if(line.getName() != null && !line.getName().isEmpty()){
                lines.add(line);
            }
        }

        //font style
        this.color = KMLStyleFactory.decodeHexToColor(style.getLabelStyle().getColor());
        this.scaleFont = style.getLabelStyle().getScale().floatValue();

        if(font != null){this.font = font;}
    }

    @Override
    public void calcPositions(DrawContext dc, HashMap<Integer, SurfaceLineSegment> linesCalc){
        int lineid; 
        
        commitedTasks.clear();
        for(SurfaceLineSegment line : lines){

            if(linesCalc != null){
                lineid = line.getID();
                if(linesCalc.put(lineid, line) != null){
                    continue;
                }
            }
            
            if(!line.getExtent(dc).intersects(dc.getView().getFrustumInModelCoordinates())){
                continue;
            }

            //calc
            ProjectLineLableTask calcTask = new ProjectLineLableTask(line.getName(), line.getVertexListV(dc), font);
            commitedTasks.add(calcTask);
            //
        }
    }

    public void preRender(DrawContext dc){
        sendRequests(dc);
    }

    private void sendRequests(DrawContext dc){
        
        final Matrix mvp = dc.getView().getProjectionMatrix().multiply(dc.getView().getModelviewMatrix());
        final int screenWidth = dc.getDrawableWidth();
        final int screenHeight = dc.getDrawableHeight();
        
        threadExecutor.execute(new Runnable() {
            public void run(){
                synchronized(lock){
                    for(ProjectLineLableTask task : commitedTasks){
                        task.setView(mvp, screenWidth, screenHeight);
                        task.run();
                    }
                }
            }
        });
    }

    public void render(DrawContext dc){
        render(dc, null);
    }

    public void render(DrawContext dc, HashMap<Integer, SurfaceLineSegment> linesRendered){

        GL2 gl2 = dc.getGL().getGL2();

        //set OpenGL state
		gl2.glPushAttrib(GL2.GL_VIEWPORT_BIT);
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPushMatrix();
		gl2.glLoadIdentity();

		gl2.glOrtho(0, dc.getDrawableWidth(),
				   0, dc.getDrawableHeight(),
				   dc.getView().getNearClipDistance(), dc.getView().getFarClipDistance());

		gl2.glMatrixMode(GL2.GL_MODELVIEW);
		gl2.glPushMatrix();
		gl2.glLoadIdentity();

		if(textRendererScreen == null){
			textRendererScreen = OGLTextRenderer.getOrCreateTextRenderer(dc.getTextRendererCache(), font, true, false,false);
			textRendererScreen.setUseVertexArrays(false);
		}
		textRendererScreen.begin3DRendering();
        //
        
        synchronized(lock){
            for(ProjectLineLableTask task : commitedTasks){
                List<ScreenCharPosRot> pathLable = task.getResoult();

                if(pathLable == null){
                    continue;
                }

                //render lable
                for(ScreenCharPosRot charPosRot : pathLable){
                    renderChar(dc, charPosRot.string, charPosRot.position, charPosRot.rotation);
                }
            }
        }

        //restore OpenGL state
		textRendererScreen.end3DRendering();
		gl2.glPopMatrix();
		gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glPopMatrix();
		gl2.glPopAttrib();
    }

    private void renderChar(DrawContext dc, String s, Vec4 position, Vec4 rotation){

        GL2 gl2 = dc.getGL().getGL2();
        
        float beckgroundOffset = 0.0f;

        float scale = (float)position.w;
        float beckgroundScale = scale;

        float offset = 3.0f;
        Color colorB = new Color(0.0f, 0.0f, 0.0f, 1.0f);

        double far = dc.getView().getFarClipDistance();
		double near = dc.getView().getNearClipDistance();

        gl2.glLoadIdentity();
        gl2.glTranslated(position.x, position.y, (-position.z + 0.01) * (far-near));
        gl2.glRotated(rotation.w, rotation.x, rotation.y, rotation.z);

        textRendererScreen.setColor(colorB);
        textRendererScreen.draw3D(s, 1.0f + beckgroundOffset, 1.0f - offset + beckgroundOffset, 0.0f, beckgroundScale);
        textRendererScreen.draw3D(s, -1.0f - beckgroundOffset, -1.0f - offset - beckgroundOffset, 0.0f, beckgroundScale);
        textRendererScreen.draw3D(s, 1.0f + beckgroundOffset, -1.0f - offset - beckgroundOffset, 0.0f, beckgroundScale);
        textRendererScreen.draw3D(s, -1.0f - beckgroundOffset, 1.0f - offset + beckgroundOffset, 0.0f, beckgroundScale);
        
        textRendererScreen.draw3D(s,  0.0f, 1.0f - offset + beckgroundOffset, 0.0f, beckgroundScale);
        textRendererScreen.draw3D(s,  0.0f, -1.0f - offset - beckgroundOffset, 0.0f, beckgroundScale);
        textRendererScreen.draw3D(s,  1.0f + beckgroundOffset, 0.0f - offset, 0.0f, beckgroundScale);
        textRendererScreen.draw3D(s, -1.0f - beckgroundOffset, 0.0f - offset, 0.0f, beckgroundScale);

        textRendererScreen.setColor(color);
        textRendererScreen.draw3D(s, 0.0f, 0.0f - offset, 0.1f, scale);
        textRendererScreen.flush();
    }

    @Override
    public long getSize(){
        long size = 0;
        for(SurfaceLineSegment sls : this.lines){
            size += sls.getSize() * 4 * 8;
            size += sls.getSize() * 32;
        }
        return size;
    }

    private static class ProjectLineLableTask implements Runnable{

        private int screenWidth = 0;
        private int screenHeight = 0;

        private final String name;
        private final List<Vec4> positions;
        //private final FontMetrics fontMetrics;

        private Font font;
        private Matrix mvp = Matrix.IDENTITY;

        private List<ScreenCharPosRot> resoult;
        
        public ProjectLineLableTask(String name, List<Vec4> positions, Font font){
            
            this.name = name;
            this.font = font;
            
            //clone array
            this.positions = positions;
            
            //this.fontMetrics = new FontMetrics(font) {};
        }

        public void setView(Matrix mvp, int screenWidth, int screenHeight){
            this.screenHeight = screenHeight;
            this.screenWidth = screenWidth;
            this.mvp = mvp;
        }

        public void run(){
            if(positions == null){
                String str = "ProjectLineLableTask.run() : positions array is null!";
                logger.severe(str);
                throw new NullPointerException(str);
            }
            
            this.resoult = projectLineLable();
        }

        public List<ScreenCharPosRot> getResoult(){
           return this.resoult;
        }

        private List<ScreenCharPosRot> projectLineLable(){

            if(filter(screenWidth, screenHeight)){
                return null;
            }

            Rectangle2D viewport = new Rectangle2D.Float(0, 0, screenWidth, screenHeight);
            
            int stringLength = name.length() * font.getSize();

            //init temp arrays
            List<List<Vec4>> lineProjClamped = new ArrayList<List<Vec4>>();
            List<List<Float>> lineProjClampedLength = new ArrayList<List<Float>>();
            List<Vec4> currentLineProjClamped = null;
            List<Float> currentLineProjClampedLength = null;

            //tmp parameters
            float lengthOnScreen = 0;
            Vec4 vCurrentScreen = null;
            Vec4 vLastScreen = null;
            Vec4 vCurrentScreenClamped = null;
            Vec4 vLastScreenClamped = null;
            float lastToCurrentLength;

            //project line
            for(int i=0; i<positions.size(); i++){

                Vec4 vCurrent = positions.get(i);
                if(vCurrent == null){continue;}

                vCurrentScreen = vCurrent.transformBy4(mvp);
                vCurrentScreen = vCurrentScreen.divide3(vCurrentScreen.w);
                vCurrentScreen = new Vec4((vCurrentScreen.x + 1.0f)/2.0f * screenWidth,
                                         ((vCurrentScreen.y + 1.0f)/2.0f) * screenHeight, 
                                         ((vCurrentScreen.z + 1.0f)/2.0f), 1.0);

                if(vCurrentScreen.z < 0.0 || vCurrentScreen.z > 1.0){
                    continue;
                }
                
                if(vLastScreen == null){
                    vLastScreen = vCurrentScreen; 
                    continue;
                }

                if(calc2DDistance(vLastScreen, vCurrentScreen) < 3){
                    continue;
                }

                boolean intersectViewport = isIntersectingViewport(vLastScreen, vCurrentScreen, viewport);

                if(!intersectViewport){
                    vLastScreen = vCurrentScreen;
                    continue;
                }

                boolean v1 = viewport.contains(new Point2D.Double(vLastScreen.x, vLastScreen.y));
                boolean v2 = viewport.contains(new Point2D.Double(vCurrentScreen.x, vCurrentScreen.y));
                boolean includedInViewport = (v1 && v2);

                vLastScreenClamped = vLastScreen;
                vCurrentScreenClamped = vCurrentScreen;
                if(!includedInViewport){
                    Vec4[] clamped = clampLineToViewport(vLastScreen, vCurrentScreen, viewport);
                    vLastScreenClamped = clamped[0];
                    vCurrentScreenClamped = clamped[1];
                }

                if(currentLineProjClamped == null){
                    currentLineProjClamped = new ArrayList<Vec4>();
                    currentLineProjClampedLength = new ArrayList<Float>();
                    lineProjClamped.add(currentLineProjClamped);
                    lineProjClampedLength.add(currentLineProjClampedLength);
                }

                currentLineProjClamped.add(vLastScreenClamped);
                lastToCurrentLength = calc2DDistance(vLastScreenClamped, vCurrentScreenClamped);
                lengthOnScreen += lastToCurrentLength;
                currentLineProjClampedLength.add(lengthOnScreen);

                //add new subline
                //if line path moves out of viewport
                if(v1 && !v2 || (!v1 && !v2)){
                    currentLineProjClamped.add(vCurrentScreenClamped);
                    currentLineProjClamped = null;
                    currentLineProjClampedLength = null;
                    lengthOnScreen = 0;
                }
                vLastScreen = vCurrentScreen;
            }

            //add last position if missing
            if(currentLineProjClamped != null){
                currentLineProjClamped.add(vCurrentScreenClamped);
            }
            
            List<ScreenCharPosRot> out = new ArrayList<ScreenCharPosRot>();
            
            /*debug
            for(int i=0; i<lineProjClamped.size(); i++){
                for(Vec4 v : lineProjClamped.get(i)){
                    out.add(new ScreenCharPosRot("O", v, new Vec4(0, 0, 1, 0), 1));
                }
            }
            */
            
            //resoult list
            //calc characters positions and rotations for all sublines
            for(int i=0; i<lineProjClamped.size(); i++){
                
                currentLineProjClamped = lineProjClamped.get(i);
                currentLineProjClampedLength = lineProjClampedLength.get(i);

                lengthOnScreen = currentLineProjClampedLength.get(currentLineProjClampedLength.size()-1);
                
                //filter
                if(lengthOnScreen < 60 || lengthOnScreen < (stringLength + 5)){
                    continue;
                }

                if(currentLineProjClamped.size() != currentLineProjClampedLength.size() + 1){
                    String msg = "ProjectLineLableTask.projectLineLable : frustrum culling exception!";
                    logger.severe(msg);
                    throw new IllegalStateException(msg);
                }

//                List<ScreenCharPosRot> out = new ArrayList<ScreenCharPosRot>();
//                for(int i=0; i<lineProjClamped.size(); i++){
//                    out.add(new ScreenCharPosRot("O", lineProjClamped.get(i), new Vec4(1,0,0,0)));
//                }
//                return out;

                //calc center position
                float startLength = lengthOnScreen/2.0f - stringLength/2.0f;

                //calc optimal string position
                //TODO: implement this

                //calc per-character postions
                Vec4[] charPositions = calcPositions(currentLineProjClamped, currentLineProjClampedLength, startLength);

                if(charPositions == null){
                    continue;
                }

                //calc per-character rotations
                Vec4[] charRotations = calcRotations(charPositions);

                //fill return treemap
                for(int c=0; c<name.length(); c++){
                    out.add(new ScreenCharPosRot(name.substring(c, c+1), charPositions[c], charRotations[c], charPositions[c].w));
                }
            }
            return out;
        }

        private boolean filter(float screenWidth, float screenHeight){

            if(positions == null){
                return false;
            }

            Vec4 start, middle, end;

            start = positions.get(0);
            start = start.transformBy4(mvp);
            start = start.divide3(start.w);
            start = new Vec4((start.x + 1.0f)/2.0f * screenWidth,
                            ((start.y + 1.0f)/2.0f) * screenHeight);

            middle = positions.get((positions.size()-1)/2);
            middle = middle.transformBy4(mvp);
            middle = middle.divide3(middle.w);
            middle = new Vec4((middle.x + 1.0f)/2.0f * screenWidth,
                             ((middle.y + 1.0f)/2.0f) * screenHeight);

            end = positions.get(positions.size()-1);
            end = end.transformBy4(mvp);
            end = end.divide3(end.w);
            end = new Vec4((end.x + 1.0f)/2.0f * screenWidth,
                          ((end.y + 1.0f)/2.0f) * screenHeight);

            if(calc2DDistance(start, middle) + calc2DDistance(middle, end) < 80.0d){
                return true;
            }

            return false;
        }

        private Vec4[] calcPositions(List<Vec4> projLinePoints, List<Float> projLineDist, float startDist){

            //float projLineLength = projLineDist.get(projLineDist.size()-1);
            float lastToCurrentLength;
            
            float currentDistance;
            float characterDistance = startDist;

            Vec4 vLastScreen = null;
            Vec4 vCurrentScreen = null;

            Vec4[] out = new Vec4[name.length()];

            //int startIndex = getIndexAtPathDistance(projLineDist, startDist);

            int counter = 0;
            for(int i=0; i<projLinePoints.size(); i++){
                vCurrentScreen = projLinePoints.get(i);

                if(vCurrentScreen == null){
                    String msg = "SurfaceLabels.calcPositions() : preojected point NULL!";
                    logger.severe(msg);
                    throw new IllegalStateException(msg);
                }
                if(vLastScreen == null){vLastScreen = vCurrentScreen; continue;}

                currentDistance = projLineDist.get(i-1);
                lastToCurrentLength = calc2DDistance(vCurrentScreen, vLastScreen);

                if(currentDistance > characterDistance){
                    while(characterDistance < currentDistance){
                        
                        float interpolant = (currentDistance-characterDistance)/lastToCurrentLength;
                        interpolant = interpolant > 1.0f ? 1.0f : (interpolant < 0.0f ? 0.0f : interpolant);
                        
                        Vec4 charPosition = vCurrentScreen.multiply3(1.0f-interpolant).add3(vLastScreen.multiply3(interpolant));
                        double sizeScale = (1.0 - (float)(Math.pow(charPosition.z, 12)));

                        if(sizeScale < 0.6){
                            return null;
                        }
                        
                        characterDistance += font.getSize() * sizeScale;
                        
                        out[counter] = new Vec4(charPosition.x, charPosition.y, charPosition.z, sizeScale);
                        
                        counter++;

                        if(counter == name.length()){
                            return out;
                        }
                    }
                }
                vLastScreen = vCurrentScreen;
            }

            if(counter != name.length()){
                String str = "ProjectLineLableTask.calcPositions() : geometry ERROR!";
                logger.severe(str);
                return null;
            }
            
            return out;
        }

        private Vec4[] calcRotations(Vec4[] positions){
            Vec4[] out = new Vec4[positions.length];

            Vec4 last;
            Vec4 current;
            Vec4 next;

            Vec4 n1, n2;

            Vec4[] d = new Vec4[positions.length];
            int flip = 0;

            for(int i = 0; i < positions.length; i++){
                current = positions[i];
                n1 = new Vec4(0, 0, 0, 0);
                n2 = new Vec4(0, 0, 0, 0);

                if(i > 0){
                    last = positions[i - 1];
                    n1 = current.subtract3(last);
                    n1 = n1.normalize3();
                    //n1 = new Vec4(n1.y, -n1.x);
                }

                if(i < positions.length - 1){
                    next = positions[i + 1];
                    n2 = next.subtract3(current);
                    n2 = n2.normalize3();
                    //n2 = new Vec4(n2.y, -n2.x);
                }

                d[i] = (n1.add3(n2)).normalize3();
                flip += Math.signum(d[i].x);
            }

            if(flip < 0){
                Vec4[] posNew = new Vec4[positions.length];
                Vec4[] dNew = new Vec4[positions.length];
                int counter = 0;
                for(int i=positions.length-1; i>=0; i--){
                    posNew[i] = positions[counter];
                    dNew[i] = d[counter].multiply3(-1.0d);
                    counter++;
                }
                
                for(int i=0; i<posNew.length; i++){
                    positions[i] = posNew[i];
                }
                d = dNew;
            }

            for(int i = 0; i < d.length; i++){
                double fac = d[i].y >= 0 ? 1.0 : -1.0;
                out[i] = new Vec4(0, 0, 1, fac * d[i].angleBetween3(Vec4.UNIT_X).degrees);
            }

            return out;
        }

        private int getIndexAtPathDistance(List<Float> distances, float distance){
            float pathLength = distances.get(distances.size()-1);

            if(distance < 0){
                return 0;
            }

            if(distance > pathLength){
                return distances.size()-1;
            }

            int startIndex = 0;
            int endIndex = distances.size()-1;
            float currentLength;
            while(endIndex - startIndex > 1){
                int currentIndex = (endIndex+startIndex)/2;
                currentLength = distances.get(currentIndex);

                if(currentLength > distance){
                    endIndex = currentIndex;
                }
                else{
                    startIndex = currentIndex;
                }
            }
            return startIndex;
        }

        private float calc2DDistance(Vec4 v1, Vec4 v2){
            Vec4 v = v1.subtract3(v2);
            return (float)Math.sqrt(v.x*v.x + v.y*v.y);
        }

        private boolean isInView(Vec4 v, Rectangle2D viewport){
            if(v.x > viewport.getMinX() 
                    && v.x < viewport.getMaxX() 
                    && v.y > viewport.getMinY() 
                    && v.y < viewport.getMaxY()){
                return true;
            }
            return false;
        }

        private boolean isIntersectingViewport(Vec4 v1, Vec4 v2, Rectangle2D viewport){
            Vec4 va = new Vec4(v1.x, v1.y, 0.0, 0.0);
            Vec4 vb = new Vec4(v2.x, v2.y, 0.0, 0.0);
            
            if((va.x < viewport.getMinX() && vb.x < viewport.getMinX())
                    || (va.x > viewport.getMaxX() && vb.x > viewport.getMaxX())){
                return false;
            }
            if((va.y < viewport.getMinY() && vb.y < viewport.getMinY())
                    || (va.y > viewport.getMaxY() && vb.y > viewport.getMaxY())){
                return false;
            }

            Line line = new Line(va, vb.subtract3(va).normalize3());
            Vec4[] nearestPoints = new Vec4[5];
            nearestPoints[0] = line.nearestPointTo(new Vec4(viewport.getCenterX(), viewport.getCenterY()));
            nearestPoints[1] = line.nearestPointTo(new Vec4(viewport.getMinX(), viewport.getMaxY()));
            nearestPoints[2] = line.nearestPointTo(new Vec4(viewport.getMaxX(), viewport.getMaxY()));
            nearestPoints[3] = line.nearestPointTo(new Vec4(viewport.getMinX(), viewport.getMinY()));
            nearestPoints[4] = line.nearestPointTo(new Vec4(viewport.getMaxX(), viewport.getMinY()));
            for(Vec4 v : nearestPoints){
                if(isInView(v, viewport)){
                    return true;
                }
            }
            
            return true;
        }

        private Vec4[] clampLineToViewport(Vec4 v1, Vec4 v2, Rectangle2D viewport){

            ArrayList<Plane> frustrum = new ArrayList<Plane>();
            frustrum.add(new Plane(0.0d,  1.0d, 0.0d, viewport.getMinY()));
            frustrum.add(new Plane(0.0d, -1.0d, 0.0d, viewport.getMaxY()));
            frustrum.add(new Plane(1.0d,  0.0d, 0.0d, viewport.getMinX()));
            frustrum.add(new Plane(-1.0d, 0.0d, 0.0d, viewport.getMaxX()));

            Vec4[] clip = new Vec4[]{v1, v2};
            Vec4[] clipTmp;
            for(Plane p : frustrum){
                clipTmp = p.clip(clip[0], clip[1]);
                if(clipTmp != null){
                    clip = clipTmp;
                }
            }

            return clip;
        }
    }

    private static class ScreenCharPosRot{
        public final String string;
        public final Vec4 position;
        public final Vec4 rotation;
        public final double size;

        public ScreenCharPosRot(String string, Vec4 position, Vec4 rotation, double size){
            this.string = string;
            this.position = position;
            this.rotation = rotation;
            this.size = size;
        }
    }
}
