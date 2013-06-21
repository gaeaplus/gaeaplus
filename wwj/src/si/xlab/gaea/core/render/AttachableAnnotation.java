package si.xlab.gaea.core.render;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.render.Annotation;

/**
 * @author marjan
 * An annotation that can 'attach' to and 'detach' from the event queue,
 * e.g. annotation with controller 
 */
public interface AttachableAnnotation extends Annotation, Movable {
	
    /**
     * Called when annotation is shown
     */
    public void attach();
    
    /**
     * Called when annotation is hidden and/or removed
     */
    public void detach();
}
