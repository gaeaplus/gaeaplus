package si.xlab.gaea.core.render;

import java.awt.image.BufferedImage;
import java.io.File;
import java.security.InvalidParameterException;

import javax.imageio.ImageIO;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.util.Logging;
import java.awt.Color;
import java.net.URL;

import si.xlab.gaea.core.retrieve.ImageCache;
import si.xlab.gaea.core.event.Selectable;
import si.xlab.gaea.core.ogc.kml.KMLStyleFactory;
import si.xlab.gaea.core.retrieve.ResourceRetriever;

/**
 * @author marjan
 * Encapsulates some functionality that most Selectables need, so as not to duplicate code in
 * the classes implementing Selectable
 */
/**
 * @author matej
 * setters for title and desc
 */
public class SelectableSupport
{

	private final Selectable parent;
	protected LatLon centroid;
	private KMLStyle style;
	protected String title, desc;
	private String unloadedImageSource; // the not-yet-loaded image (null if no image used or the image already loaded)
	protected BufferedImage image;
	private boolean selected;
	protected AnnotationLayer annotLayer;
	private AttachableAnnotation annotation;
    private AnnotationAttributes annotationAttrs;

	public SelectableSupport(Selectable parent, LatLon centroid,
		KMLStyle style, String title, String desc, Object image)
	{
		this.parent = parent;
		this.centroid = centroid;
		this.style = style;
		this.title = (null == title ? "" : title);
		this.desc = (null == desc ? "" : desc);

		setImage(image);

		this.annotation = null;
        this.annotationAttrs = DefaultLook.DEFAULT_ANNOTATION_ATTRIBUTES;
		this.selected = false;
	}

	final public void setImage(Object image)
	//image can be either null, an already loaded image, or URL/filename
	{
		if (image == null)
		{
			this.unloadedImageSource = null;
			this.image = null;
		} else if (image instanceof BufferedImage)
		{
			this.unloadedImageSource = null;
			this.image = (BufferedImage) image;
		} else if (image instanceof String)
		{
			this.unloadedImageSource = (String) image;
			this.image = null;
		} else
		{
			throw new InvalidParameterException("image must be BufferedImage or String containing URL");
		}
	}

	public void select(WorldWindow wwd, AnnotationLayer annotLayer, Position pickPosition)
	{
		if (this.selected)
		{
			unselect();
		}

		this.selected = true;
		this.annotLayer = annotLayer;
		if (null != this.annotLayer)
		{
			if (this.annotation == null)
			{
				this.annotation = parent.createAnnotation(wwd);

				if (this.annotation == null)
				{
					Logging.logger().warning("The Selectable did not create an annotation and will be unselected.");
					this.selected = false;
					return;
				}

				if (this.annotation instanceof IconAnnotation
					&& this.unloadedImageSource != null)
				{
					// we have a not-yet-applied image for the annotation
					String currentlyLoadingImage = unloadedImageSource;

					this.unloadedImageSource = null;
					ImageCache.ensureImageCached(currentlyLoadingImage,
						new ApplyImagePostProcessor(
						(IconAnnotation) this.annotation, currentlyLoadingImage));
				}

				if (null != this.style)
				{
					applyStyleToAnnotation(style);
				} else
				{
					applyStyleToAnnotation(DefaultLook.DEFAULT_FEATURE_STYLE);
				}
			}
			annotLayer.addAnnotation(this.annotation);
			this.annotation.attach();

			if (pickPosition != null)
			{
				this.annotation.moveTo(new Position(pickPosition, 0));
				this.centroid = pickPosition;
			}
		}
	}

	public void setStyle(KMLStyle style)
	{
		this.style = style;
		if (this.annotation != null)
		{
			if (this.style != null)
			{
				applyStyleToAnnotation(style);
			} else
			{
				applyStyleToAnnotation(DefaultLook.DEFAULT_FEATURE_STYLE);
			}
		}
	}

	public void showAnnotation(WorldWindow window)
	{
		this.select(window, annotLayer, null);
	}

	public void clear()
	{
		if (annotLayer != null)
		{
			annotLayer.removeAllAnnotations();
		}
	}

	public void unselect()
	{
		if (!this.selected)
		{
			return;
		}

		this.annotation.detach();
		this.selected = false;
		if (null != this.annotLayer)
		{
			annotLayer.removeAnnotation(this.annotation);
		}
	}

	public boolean isSelected()
	{
		return this.selected;
	}

	public long getSizeInBytes()
	{
		return 200 + 2 * title.length() + 2 * desc.length();
	}

	public AttachableAnnotation createDefaultAnnotation(WorldWindow wwd)
	{
		return new IconAnnotation(wwd, new Position(this.centroid, 0),
			this.parent, getTitle(), getDescription(), getImage(), annotationAttrs);
	}

	public void refreshAnnotation(WorldWindow wwd)
	{
		if (this.annotation != null)
		{
			unselect();
		}
		this.annotation = this.createDefaultAnnotation(wwd);
		setStyle(style);
	}

    public void setAnnotationAttributes(AnnotationAttributes attrs, WorldWindow wwd, boolean refresh)
    {
        this.annotationAttrs = attrs;
        if (refresh)
            refreshAnnotation(wwd);
    }
    
	public String getTitle()
	{
		return this.title;
	}

	public String getDescription()
	{
		return this.desc;
	}

	public BufferedImage getImage()
	{
		return image;
	}

	public void setDescription(String desc)
	{
		this.desc = desc;
        if (this.annotation instanceof IconAnnotation)
        {
            ((IconAnnotation)annotation).setCaption(desc);
        }
	}

	public void setTitle(String title)
	{
		this.title = title;
        if (this.annotation instanceof IconAnnotation)
        {
            ((IconAnnotation)annotation).setTitle(title);
        }
	}

	public void moveAnnotation(Position pos)
	{
		if (this.annotation != null)
		{
			this.annotation.move(pos);
			this.centroid = pos;
		}
	}

	private void applyStyleToAnnotation(KMLStyle style)
	{
		if (null != style.getBaloonStyle())
		{
			AnnotationAttributes aa = annotation.getAttributes();

			Color bgColor = KMLStyleFactory.decodeHexToColor(style.getBaloonStyle().getBgColor());
			if (bgColor != null)
			{
				aa.setBackgroundColor(bgColor);
			}

			Color textColor = KMLStyleFactory.decodeHexToColor(style.getBaloonStyle().getTextColor());
			if (textColor != null)
			{
				aa.setTextColor(textColor);
			}
		}
	}

    
	/**
	 * @author marjan
	 * Once the image is cached, loads it and applies it to the annotation
	 */
	private static class ApplyImagePostProcessor implements Runnable
	{

		private final String imageSource;
		private final IconAnnotation annotation;

		public ApplyImagePostProcessor(IconAnnotation annotation, String imageSource)
		{
			this.annotation = annotation;
			this.imageSource = imageSource;
		}

		public void run()
		{
			BufferedImage image = readLocalImage(imageSource);
			annotation.setImage(image);
		}

		private BufferedImage readLocalImage(String imageSource)
		{
			BufferedImage ret = null;

			try
			{
				String cachedImage = ImageCache.getImage(imageSource);
				File cachedFile = new File(cachedImage);

				if (cachedFile.exists())
				{
					ret = ImageIO.read(cachedFile);
				} else
				{ // since this is called after image is cached, the only legal reason file doesn't exist is because it's a resource, not absolute file path

					URL imageResource =
						ApplyImagePostProcessor.class.getResource("/"
						+ cachedImage);

					if (imageResource == null)
					{
                        imageResource = ResourceRetriever.getResource(cachedImage);
					}

					ret = ImageIO.read(imageResource);
				}
			} catch (Exception e)
			{
				Logging.logger().severe(
					"Cannot load image for annotation: " + imageSource);
				e.printStackTrace();
			}

			return ret;
		}
	}
}
