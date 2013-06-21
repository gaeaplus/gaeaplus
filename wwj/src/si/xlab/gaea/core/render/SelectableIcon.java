package si.xlab.gaea.core.render;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;

import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.UserFacingIcon;
import si.xlab.gaea.core.event.Draggable;
import si.xlab.gaea.core.event.Selectable;
import si.xlab.gaea.core.retrieve.ImageCache;

/**
 * An extension of Icon that also contains an Annotation, which is shown when the icon is selected
 * The icon can be loaded via HTTP
 */
public class SelectableIcon extends UserFacingIcon implements Selectable, Draggable, Cacheable
{

    private final static String DEFAULT_ICON = "images/pushpins/push-pin-yellow-32.png";
    protected SelectableSupport selectableSupport;
    private boolean dragable = false;
    /**
     * Indicating whether this icon should be scaled dynamically with the
     * distance from the observer or not.
     */
    private final boolean dynamicScale;

    /**
     * Importance of the icon. Range [0..1]. 
     * 1 for important icons, less for the unimportant ones, which are then
     * only visible at shorter distances
     *
     * This is only applicable when dynamicScale is turned on.
     */
    private final double distFactor; // 0..1; 	

    private KMLStyle style;
    
    public SelectableIcon(KMLStyle style, Position iconPosition,
            String title, String desc, String annotImageSource,
            double distFactor)
    {
        this(style, style.getIconStyle().getIcon().getHref(), iconPosition, title, desc,
                annotImageSource, distFactor, true);
    }

    public SelectableIcon(KMLStyle style, Position iconPosition,
            String title, String desc, String annotImageSource,
            double distFactor, boolean dynamicScale)
    {
        this(style, style.getIconStyle().getIcon().getHref(), iconPosition, title, desc,
                annotImageSource, distFactor, dynamicScale);
    }

    public SelectableIcon(String iconImage, Position iconPosition,
            String title, String desc, String annotImage,
            double distFactor)
    {
        this(null, iconImage, iconPosition, title, desc, annotImage,
                distFactor, true);
    }

    public SelectableIcon(String iconImage, Position iconPosition,
            String title, String desc, String annotImage,
            double distFactor, boolean dynamicScale)
    {
        this(null, iconImage, iconPosition, title, desc, annotImage,
                distFactor, dynamicScale);
    }

    public SelectableIcon(KMLStyle style, String iconImage, Position iconPosition,
            String title, String desc, String annotImage, double distFactor)
    {
        this(null, iconImage, iconPosition, title, desc, annotImage,
                distFactor, true);
    }

    public SelectableIcon(KMLStyle style, String iconImage, Position iconPosition,
            String title, String desc, String annotImage, double distFactor,
            boolean dynamicScale)
    {
        super(ImageCache.getImageWithFallback(iconImage, DEFAULT_ICON), iconPosition);

        this.style = style;
        this.dynamicScale = dynamicScale;
        this.distFactor = distFactor;
        this.selectableSupport = new SelectableSupport(this, iconPosition, style, title, desc, annotImage);
    }

    public SelectableIcon(KMLStyle style, Position iconPosition,
            double distFactor, boolean dynamicScale)
    {
        super(ImageCache.getImageWithFallback(style.getIconStyle().getIcon().getHref(), DEFAULT_ICON), iconPosition);

        this.style = style;
        this.dynamicScale = dynamicScale;
        this.distFactor = distFactor;
    }

    public double getIconScale()
    {
        if (style != null && style.getIconStyle() != null && style.getIconStyle().getScale() != null && style.getIconStyle().getScale() > 0)
            return style.getIconStyle().getScale();
        else
            return 1.0;
    }
    
    public void setSelectableSupport(SelectableSupport selectableSupport)
    {
        this.selectableSupport = selectableSupport;
    }

    @Override
    public void setImageSource(Object imageSource)
    {
        if (imageSource instanceof String)
        {
            super.setImageSource(
                    ImageCache.getImageWithFallback((String) imageSource,
                    DEFAULT_ICON));
        } else
        {
            super.setImageSource(imageSource);
        }

    }

    public void setStyle(KMLStyle style)
    {
        this.style = style;
        getSelectableSupport().setStyle(style);
    }
            
    public void setTitle(String title)
    {
        getSelectableSupport().setTitle(title);
    }

    public void setDescription(String desc)
    {
        getSelectableSupport().setDescription(desc);
    }
    
    @Override
    public void setHighlighted(boolean highlighted)
    {
        super.setHighlighted(highlighted);
        setShowToolTip(isHighlighted() && !isSelected());
    }

    public String getTitle()
    {
        return this.selectableSupport.getTitle();
    }

    public String getDescription()
    {
        return this.selectableSupport.getDescription();
    }

    public void select(WorldWindow wwd, AnnotationLayer annotLayer, Position pickPosition)
    {
        this.selectableSupport.select(wwd, annotLayer, pickPosition);
    }

    public void unselect()
    {
        this.selectableSupport.unselect();
    }

    public boolean isSelected()
    {
        return this.selectableSupport.isSelected();
    }

    public boolean isDynamicScale()
    {
        return dynamicScale;
    }

    public double getDistFactor()
    {
        return this.distFactor;
    }

    public boolean isDraggable()
    {
        return dragable;
    }

    public void setDragable(boolean dragable)
    {
        this.dragable = dragable;
    }

    public long getSizeInBytes()
    {
        return this.selectableSupport.getSizeInBytes();
    }

    public void setAnnotationAttributes(AnnotationAttributes attrs, WorldWindow wwd)
    {
        this.selectableSupport.setAnnotationAttributes(attrs, wwd, false);
        refreshAnnotation(wwd);
    }
    
    public AttachableAnnotation createAnnotation(WorldWindow wwd)
    {
        return this.selectableSupport.createDefaultAnnotation(wwd);
    }

    public void refreshAnnotation(WorldWindow wwd)
    {
        this.selectableSupport.refreshAnnotation(wwd);
    }

    public void showAnnotation(WorldWindow window)
    {
        this.selectableSupport.showAnnotation(window);
    }

    protected SelectableSupport getSelectableSupport()
    {
        return selectableSupport;
    }
}
