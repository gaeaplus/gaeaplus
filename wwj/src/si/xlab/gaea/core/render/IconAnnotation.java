package si.xlab.gaea.core.render;


import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.AnnotationFlowLayout;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.ScreenAnnotation;

import gov.nasa.worldwindx.examples.util.DialogAnnotation;
import gov.nasa.worldwindx.examples.util.ImageAnnotation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;

import si.xlab.gaea.core.event.Draggable;
import si.xlab.gaea.core.Util;
import si.xlab.gaea.core.event.Selectable;


/**
 * An extension of GlobeAnnotation that's never hidden below the terrain
 * and that uses DefaultLook.DEFAULT_ANNOTATION_ATTRIBUTES
 * @author marjan
 */
public class IconAnnotation extends DialogAnnotation
					implements java.awt.event.ActionListener, Draggable, AttachableAnnotation
{
    protected class DialogAnnotationController extends gov.nasa.worldwindx.examples.util.DialogAnnotationController
    {
        public DialogAnnotationController(WorldWindow wwd, DialogAnnotation annotation)
        {
            super(wwd, annotation);
        }
    }
    
    private final int CLOSE_BUTTON_SPACE = 20;
    
    private final Selectable owner;

    protected Annotation titleLabel;
    protected Annotation captionLabel;
    protected ImageAnnotation imageAnnotation;
//    protected ButtonAnnotation sizeButton;
    
    private String title, caption;
    
    private final DialogAnnotationController controller;
    
    public IconAnnotation(WorldWindow wwd, Position position, Selectable owner,
    						String title, String caption, BufferedImage image)
    {
        this(wwd, position, owner, title, caption, image,DefaultLook.DEFAULT_ANNOTATION_ATTRIBUTES);
    }
            
    public IconAnnotation(WorldWindow wwd, Position position, Selectable owner,
    						String title, String caption, BufferedImage image,
                            AnnotationAttributes attrs)
    {
        super(position);
        getAttributes().setDefaults(attrs);
        setAlwaysOnTop(true); // to draw over other OrderedRenderables (in particular, over icons)
        
        this.owner = owner;
        this.title = title;
        this.caption = caption;
		
        this.titleLabel.setText(this.title);
        this.captionLabel.setText(this.caption);
        if (image != null)
        	setImage(image);
        
        if (caption.length() > 500)
        {
            Dimension size = new Dimension(this.getAttributes().getSize());

            int width = (int) (Math.min(2.0, caption.length() / 500.0) * size.width);
            resize(width);
        }
        
        this.controller = new DialogAnnotationController(wwd, this);
    }
	
    public void setCaption(String caption)
    {
        this.caption = caption;
        this.captionLabel.setText(caption);
    }

    public void setTitle(String title)
    {
        this.title = title;
        this.titleLabel.setText(title);
    }
    
    public void setImage(BufferedImage image)
    {
        if (null != image)
        {
            // we have an image: first, resize it if it's too big.
            int width = image.getWidth();
            int height = image.getHeight();

            if (width > DefaultLook.ANNOTATION_MAX_IMAGE_SIZE
                    || height > DefaultLook.ANNOTATION_MAX_IMAGE_SIZE)
            {
                double fact = (double) DefaultLook.ANNOTATION_MAX_IMAGE_SIZE
                        / Math.max(width, height);

                width *= fact;
                height *= fact;
                image = Util.scaleImage(image, width, height);
            }
            
            // ensure image has an alpha channel
            if (image.getTransparency() == BufferedImage.OPAQUE)
            {
                // Copy the image into a buffered image with an alpha channel.
                // That ensures the annotation background color will not be covered with black
                BufferedImage newImage = new BufferedImage(width, height,
                        BufferedImage.TYPE_INT_ARGB);

                newImage.getGraphics().drawImage(image, 0, 0, null);
                image = newImage;
            }
        }
        imageAnnotation.setImageSource(image);
    }
    
    public boolean isDraggable()
    {
    	return false;
    }
    
    
    @Override
    protected void setDepthFunc(DrawContext dc, Vec4 screenPoint)
    {
        GL gl = dc.getGL();

        gl.glDepthFunc(GL.GL_ALWAYS); // always draw regardless of z-buffer content
    }

    @Override
    @SuppressWarnings({"StringEquality"})
    public void actionPerformed(ActionEvent e)
    {
    	if (e == null)
    		return;

        if (e.getActionCommand() == AVKey.CLOSE)
        {
            owner.unselect();
        }
        else
        {
            super.actionPerformed(e);
        }
    } 
    
    public void attach()
    {
        this.controller.setEnabled(true);
    }
    
    public void detach()
    {
        this.controller.setEnabled(false);
    }
    
    //**************************************************************//
    //********************  Annotation Components  *****************//
    //**************************************************************//

    protected void initComponents()
    {
        super.initComponents();

        this.titleLabel = new ScreenAnnotation("", new java.awt.Point());
        this.setupTitle(this.titleLabel);

        this.captionLabel = new ScreenAnnotation("", new java.awt.Point());
        this.setupCaption(this.captionLabel);

        this.imageAnnotation = new ImageAnnotation();
        this.setupImage(this.imageAnnotation);

        getCloseButton().setToolTipText(null);
    }

    protected void layoutComponents()
    {
        super.layoutComponents();

        Annotation contentContainer = new ScreenAnnotation("", new java.awt.Point()); //$NON-NLS-1$
        {
            this.setupContainer(contentContainer);
            contentContainer.setLayout(new AnnotationFlowLayout(AVKey.VERTICAL, AVKey.LEFT, 0, 5)); // hgap, vgap
            contentContainer.addChild(this.titleLabel);
           	contentContainer.addChild(this.imageAnnotation);
            contentContainer.addChild(this.captionLabel);
        }

        this.addChild(contentContainer);

        //AnnotationNullLayout layout = (AnnotationNullLayout) this.getLayout();
        //this.addChild(this.sizeButton);
        // Force the busy image to draw on top of its siblings.
        //layout.setConstraint(this.sizeButton, AVKey.SOUTHEAST);
    }

    @Override
    protected void setupDefaultAttributes(AnnotationAttributes attributes)
    {
        super.setupDefaultAttributes(attributes);
        
        //default attributes of child annotations should be the attributes of the parent annotation
        //thus when style is applied to parent, it will also be applied to all children
        //except those that override a particular property
        attributes.setOpacity(1.0);
        attributes.setDefaults(this.getAttributes());
    }
    
    @Override
    public java.awt.Dimension getPreferredSize(DrawContext dc)
    {
    	return super.getPreferredSize(dc);
    }
    
    @Override
    protected void setupLabel(Annotation annotation)
    {
    	super.setupLabel(annotation);
    	AnnotationAttributes attribs = annotation.getAttributes();
    	attribs.setFont(DefaultLook.DEFAULT_ANNOTATION_ATTRIBUTES.getFont());
    	attribs.setAdjustWidthToText(SIZE_FIXED);
    }
    
    protected void setupTitle(Annotation annotation)
    {
        this.setupLabel(annotation);
        
        AnnotationAttributes attribs = annotation.getAttributes();
        attribs.setFont(new Font(attribs.getFont().getName(),
        						Font.BOLD,
        						attribs.getFont().getSize()));
        attribs.setSize(new java.awt.Dimension(this.getAttributes().getSize().width - CLOSE_BUTTON_SPACE, 0));
        attribs.setTextAlign(AVKey.LEFT);
    }

    protected void setupCaption(Annotation annotation)
    {
        this.setupLabel(annotation);

        AnnotationAttributes attribs = annotation.getAttributes();
        //attribs.setSize(new java.awt.Dimension(this.getAttributes().getSize().width, 0));
        attribs.setTextAlign(AVKey.LEFT);
    }
    
    protected void setupImage(ImageAnnotation annotation)
    {
        annotation.setFitSizeToImage(true);
        annotation.setUseImageAspectRatio(false);
    }
    
    protected void resize(int width)
    {
    	getAttributes().setSize(new Dimension(width, 0));
    	this.titleLabel.getAttributes().setSize(new Dimension(width - CLOSE_BUTTON_SPACE, 0));
    	this.captionLabel.getAttributes().setSize(new Dimension(width, 0));
    }
}

