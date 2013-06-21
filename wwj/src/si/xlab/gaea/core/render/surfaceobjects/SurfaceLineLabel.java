package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.Renderable;
import java.util.HashMap;
import si.xlab.gaea.core.render.surfaceobjects.SurfaceLineSegment;

/**
 *
 * @author Vito Čuček <vito.cucek@xlab.si / vito.cucek@gmail.com>
 */
public abstract class SurfaceLineLabel implements Renderable, PreRenderable{
    
    public abstract void calcPositions(DrawContext dc, HashMap<Integer, SurfaceLineSegment> linesCalc);
    public abstract void preRender(DrawContext dc);
    public abstract void render(DrawContext dc);

    /**
     * Return object size in bytes.
     */
    public abstract long getSize();
}
