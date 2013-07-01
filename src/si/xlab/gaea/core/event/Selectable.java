package si.xlab.gaea.core.event;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;

import si.xlab.gaea.core.render.AttachableAnnotation;

public interface Selectable {
	/**
	 * Called by the select listener when the object is clicked.
	 * The normal behaviour is to show an annotation with details about the feature.
	 * If pickPosition is not null, the object can decide to show the annotation at pickPosition
	 * instead of the object's centroid.
	 * @param wwd	the WorldWindow into which the annotation's controller should register itself as the listener
	 * @param annotLayer	the layer into which to add the annotation
	 * @param pickPosition	picked position
	 */
	public void select(WorldWindow wwd, AnnotationLayer annotLayer, Position pickPosition);
	public void unselect();
	
	public boolean isSelected();
	
	/**
	 * Creates the annotation to show on selection
	 * @param wwd
	 * @return
	 */
	public AttachableAnnotation createAnnotation(WorldWindow wwd);
}
