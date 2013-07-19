package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Vito Čuček <vito.cucek@xlab.si / vito.cucek@gmail.com>
 */
public class SurfaceLineLabelRenderer implements OrderedRenderable {

    private final List<SurfaceLineLabel> floatingLabels = new ArrayList<SurfaceLineLabel>();
    private final HashMap<Integer, SurfaceLineSegment> renderedLines = new HashMap<Integer, SurfaceLineSegment>();
    
    private long lastPrerender = 0;
    
    protected static final Logger logger = Logger.getLogger(SurfaceLineFloatingLabels.class.getName());
    
    public void addToRenderQueue(SurfaceLineLabel labels){
        this.floatingLabels.add(labels);
    }

	@Override
    public void render(DrawContext dc){

		calc(dc);
		
        for(SurfaceLineLabel fl : floatingLabels){

            if(fl == null){
                String str = "SurfaceLineLabelRenderer.render() : surfaceLineLabel is null!";
                logger.severe(str);
                continue;
            }
            
            fl.render(dc);
        }
        floatingLabels.clear();
    }

    public void calc(DrawContext dc){
        if(System.currentTimeMillis() - lastPrerender > 1000){
            for(SurfaceLineLabel fl : floatingLabels){

                if(fl == null){
                    String str = "SurfaceLineLabelRenderer.render() : surfaceLineLabel is null!";
                    logger.severe(str);
                    continue;
                }

                fl.calcPositions(dc, renderedLines);
            }
            renderedLines.clear();
            lastPrerender = System.currentTimeMillis() + (long)(Math.random() * 1000.0);
        }
        //else{
            for(SurfaceLineLabel fl : floatingLabels){

                if(fl == null){
                    String str = "SurfaceLineLabelRenderer.render() : surfaceLineLabel is null!";
                    logger.severe(str);
                    continue;
                }

                fl.preRender(dc);
            }
        //}
        
    }

	@Override
	public double getDistanceFromEye() {
		return Double.MAX_VALUE;
	}

	@Override
	public void pick(DrawContext dc, Point pickPoint) {
	}
}
