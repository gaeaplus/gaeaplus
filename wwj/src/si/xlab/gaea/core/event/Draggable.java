/**
 * 
 */
package si.xlab.gaea.core.event;

/**
 * @author marjan
 * Lets an object decide whether it wants to consume mouse drag events, e.g. to be dragged over terrain
 * (and thus the user cannot pan the view by dragging it).
 * All objects not implementing this interface are draggable by default, except for terrain.
 */
public interface Draggable {
	public boolean isDraggable();
}
