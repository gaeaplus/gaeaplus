package si.xlab.gaea.core.event;


import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import gov.nasa.worldwindx.examples.util.DialogAnnotation;
import java.awt.Color;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import si.xlab.gaea.core.Util;
import si.xlab.gaea.core.render.SelectableIcon;

/**
 * @author marjan
 * Handles selecting and highlighting of ALL Selectables,
 * such that only one of those can be selected and/or highlighted simultaneously 
 */
public class FeatureSelectListener implements SelectListener
{
    private class SelectedAnnotationLayer extends AnnotationLayer
    {
        @Override
        protected void doPick(DrawContext dc, java.awt.Point pickPoint)
        {
            if (parentLayer == null || !parentLayer.isEnabled())
            {
                return;
            }
	    	
            super.doPick(dc, pickPoint);
        }

        @Override
        protected void doRender(DrawContext dc)
        {
            if (parentLayer == null || !parentLayer.isEnabled())
            {
                return;
            }

            super.doRender(dc);
        }
    }
	
    private final WorldWindow wwd;
    
    private Object lastPickedObject;
    private Object selectedObject;
    private Layer parentLayer;
    private Color savedAnnotationBorderColor;

    private SelectedAnnotationLayer selectedAnnotLayer;
    
    private final static Color SELECTED_ANNOTATION_BORDER_COLOR = Color.YELLOW;
    
    public FeatureSelectListener(WorldWindow wwd)
    {
    	this.wwd = wwd;
    	
        lastPickedObject = null;
        selectedObject = null;
        savedAnnotationBorderColor = null;
    }

    public void selected(SelectEvent event)
    {
        // Select/unselect object on left click,
        // or follow URL if one was clicked
        if (event.getEventAction().equals(SelectEvent.LEFT_CLICK))
        {
            if (event.hasObjects())
            {
                if (event.getTopObject() instanceof Annotation)
                {
                    // if URL was clicked, open in browser
                    PickedObject po = event.getTopPickedObject();

                    if (po.getValue(AVKey.URL) != null
                            && po.getValue(AVKey.URL) instanceof String)
                    {
                        // Execute the URL.
                        this.processUrl((String) po.getValue(AVKey.URL), true);

                        return;
                    }
                }    				
    				
                if (event.getTopObject() instanceof Selectable)
                {
                    if (this.selectedObject != event.getTopObject()
                            || (this.selectedObject instanceof Selectable
                                    && !((Selectable) this.selectedObject).isSelected()))
                    {
                        // either a new object was clicked, or this.selectedObject was clicked
                        // but it's been unselected already from outside this listener
                        select(event.getTopObject(),
                                event.getTopPickedObject().getParentLayer(),
                                event.getTopPickedObject().getPosition());
                        return;
                    }
                    else
                    {
                        // same, already selected object clikcked
                        select(null, null, null);
                        return;
                    }
                }
            }
        } // Highlight on rollover
        else if (event.getEventAction().equals(SelectEvent.ROLLOVER))
        {
            if (!(event.getTopObject() instanceof DialogAnnotation))
            {
                highlight(event.getTopObject());

                // if URL was rollod-over, show it in status bar
                PickedObject po = event.getTopPickedObject();

                if (po != null
                        && po.getValue(AVKey.URL) != null
                        && po.getValue(AVKey.URL) instanceof String)
                {
                    // show the URL.
                    this.processUrl((String) po.getValue(AVKey.URL), false);

                    return;
                }
            } // if
        }    
        
        setStatusText("");
    }

    public void unselect()
    {
        if (null != this.selectedObject)
        {
            if (this.selectedObject instanceof Annotation)
            {
                ((Annotation) this.selectedObject).getAttributes().setBorderColor(
                        this.savedAnnotationBorderColor);
                storeSelection(null, null);
            }
            else if (this.selectedObject instanceof Selectable)
            {
                ((Selectable) this.selectedObject).unselect();
                selectedAnnotLayer.removeAllAnnotations(); // should not be necessary, but we do this just in case we forgot to remove some annot.
                storeSelection(null, null);
            }
            else
            {
                Logging.logger().severe("Unsupported object type selected!");
            }
        } 
        setStatusText("");
    }
    
    public void select(Object o, Layer parentLayer)
    {
        select(o, parentLayer, null);
    }
    
    public void select(Object o, Layer parentLayer, Position pickPosition)
    {
        if (this.selectedAnnotLayer == null)
        {
            selectedAnnotLayer = new SelectedAnnotationLayer();
            selectedAnnotLayer.setEnabled(true);
            selectedAnnotLayer.setName("Layers.SelectedFeatureAnnotationLayer");

            wwd.getModel().getLayers().add(selectedAnnotLayer);
        } // if

        synchronized (selectedAnnotLayer)
        {
            // unselect the currently selected object
            unselect();
	    	
            // select o
            if (null != o)
            {
                if (o instanceof Annotation)
                {
                    Annotation ann = (Annotation) o;

                    this.savedAnnotationBorderColor = ann.getAttributes().getBorderColor();
                    ann.getAttributes().setBorderColor(
                            SELECTED_ANNOTATION_BORDER_COLOR);    			
                    storeSelection(o, parentLayer);
                }
                else if (o instanceof Selectable)
                {
                    ((Selectable) o).select(this.wwd, this.selectedAnnotLayer, pickPosition);
                    storeSelection(o, parentLayer);
                }
                else
                {
                    Logging.logger().severe("Unsupported object type selected!");
                }
            }
        }
    }
    
    private void storeSelection(Object o, Layer parentLayer)
    {
        this.selectedObject = o;
        this.parentLayer = parentLayer;
    }
    
    private void highlight(Object o)
    {
        // Manage highlighting of features
        if (this.lastPickedObject == o)
        {
            return;
        } // same thing picked

        // Turn off highlight if on.
        if (this.lastPickedObject != null)
        {
            if (this.lastPickedObject instanceof Annotation)
            {
                Annotation ann = (Annotation) this.lastPickedObject;

                ann.getAttributes().setHighlighted(false);
                this.lastPickedObject = null;
            }
            else if (this.lastPickedObject instanceof SelectableIcon)
            {
                ((SelectableIcon) this.lastPickedObject).setHighlighted(false);
                this.lastPickedObject = null;
            }
            else
            {
                Logging.logger().severe("Unsupported object type highlighted!");
            }
        }

        // Turn on highlight if object selected.
        if (o != null)
        {
            if (o instanceof Annotation)
            {
                // highlight the annotation UNLESS it's the annotation of the selected icon
                if (!(this.selectedObject instanceof Selectable)

                        /* || ((Selectable)this.selectedObject).getAnnotation() != o*/)
                {        		
                    Annotation ann = (Annotation) o;

                    ann.getAttributes().setHighlighted(true);
                }

                this.lastPickedObject = o;
            }
            else if (o instanceof SelectableIcon)
            {
                ((SelectableIcon) o).setHighlighted(true);
                this.lastPickedObject = o;
            }
        }
    }

    protected void setStatusText(String text)
    {
        //empty, override to implement!
    }
    
    private void processUrl(String url, boolean exec)
    {
        if (exec)
        {
            try
            {
                // Otherwise, try to open the given URL.
                Util.openURL(url);
            }
            catch (IOException e)
            {
                Logging.logger().warning(e.getMessage());
            }
        } // if
        else
        {
            this.setStatusText(url);
        }
    }

    public Object getSelectedObject()
    {
        return lastPickedObject;
    }
   
} // class FeatureSelectListener

