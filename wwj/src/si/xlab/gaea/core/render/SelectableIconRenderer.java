package si.xlab.gaea.core.render;

import com.jogamp.opengl.util.texture.TextureCoords;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.IconRenderer;
import gov.nasa.worldwind.render.WWIcon;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.view.orbit.OrbitView;
import java.awt.Dimension;
import java.awt.Rectangle;
import javax.media.opengl.GL2;
import si.xlab.gaea.core.event.Selectable;

/**
 *
 * @author marjan
 */
public class SelectableIconRenderer extends IconRenderer
{
    private double maxVisibleDistance;

    public SelectableIconRenderer()
    {
        this(Double.POSITIVE_INFINITY);
    }
	
    public SelectableIconRenderer(double maxVisibleDistance)
    {
        setMaxVisibleDistance(maxVisibleDistance);
        this.setAllowBatchPicking(false);
    }
	
    public void setMaxVisibleDistance(double maxVisibleDistance)
    {
        this.maxVisibleDistance = maxVisibleDistance;
    }	
	
    public double getMaxVisibleDistance()
    {
        return maxVisibleDistance;
    }
    
    @Override
    protected boolean meetsRenderCriteria(DrawContext dc, WWIcon icon, Vec4 iconPoint, double eyeDistance)
    {
        setAllowBatchPicking(false); //TODO: this should not be here, since it's a global (renderer-wide) setting!?
        if (!super.meetsRenderCriteria(dc, icon, iconPoint, eyeDistance))
        {
            return false;
        }
		
        if (icon instanceof Selectable && ((Selectable) icon).isSelected())
        {
            return true;
        }
		
        return calcPenalizedEyeDistance(dc, icon, iconPoint) < maxVisibleDistance;
    }
	
    protected final double calcPenalizedEyeDistance(DrawContext dc, WWIcon icon, Vec4 iconPoint)
    {
        double factor = 1;

        if (dc.getView() instanceof OrbitView)
        {
            factor = ((OrbitView) dc.getView()).getPitch().cos();
        }

        factor = Math.max(0.3, factor);

        double eyeDistance = dc.getView().getEyePoint().distanceTo3(iconPoint);

        eyeDistance /= factor;
    	
        if (icon instanceof SelectableIcon)
        {
            eyeDistance /= ((SelectableIcon) icon).getDistFactor();
        }
        
        return eyeDistance;
    }
    
    @Override
    protected Vec4 drawIcon(DrawContext dc, OrderedIcon uIcon)
    {
        if (uIcon.getPoint() == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.logger().severe(msg);

            // Record feedback data for this WWIcon if feedback is enabled.
            if (uIcon.getIcon() != null)
                this.recordFeedback(dc, uIcon.getIcon(), null, null);

            return null;
        }

        WWIcon icon = uIcon.getIcon();
        if (dc.getView().getFrustumInModelCoordinates().getNear().distanceTo(uIcon.getPoint()) < 0)
        {
            // Record feedback data for this WWIcon if feedback is enabled.
            this.recordFeedback(dc, icon, uIcon.getPoint(), null);

            return null;
        }

        final Vec4 screenPoint = dc.getView().project(uIcon.getPoint());
        if (screenPoint == null)
        {
            // Record feedback data for this WWIcon if feedback is enabled.
            this.recordFeedback(dc, icon, uIcon.getPoint(), null);

            return null;
        }

        double pedestalScale;
        double pedestalSpacing;
        if (this.pedestal != null)
        {
            pedestalScale = this.pedestal.getScale();
            pedestalSpacing = pedestal.getSpacingPixels();
        }
        else
        {
            pedestalScale = 0d;
            pedestalSpacing = 0d;
        }

        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.

        this.setDepthFunc(dc, uIcon, screenPoint);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        Dimension size = icon.getSize();
        double width = size != null ? size.getWidth() : icon.getImageTexture().getWidth(dc);
        double height = size != null ? size.getHeight() : icon.getImageTexture().getHeight(dc);
        gl.glTranslated(screenPoint.x - width / 2, screenPoint.y + (pedestalScale * height) + pedestalSpacing, 0d);

        // ////////////////////////////////////
        //scale icon with distance
        double eyeDistance;
        if (((SelectableIcon)icon).isDynamicScale())
        {
            eyeDistance = calcPenalizedEyeDistance(dc, uIcon.getIcon(),
                    uIcon.getPoint());
        } else {
            eyeDistance = dc.getView().getEyePoint().distanceTo3(uIcon.getPoint());
        }

        double alpha = calculateAlpha(eyeDistance);
        double scale;
        if (icon instanceof SelectableIcon)
            scale = ((SelectableIcon)icon).getIconScale();
        else
            scale = 1.0;
        scale /= calcDistanceReduceFactor(eyeDistance);

        gl.glTranslated(width / 2, 0, 0);
        gl.glScaled(scale, scale, scale);
        gl.glTranslated(-width / 2, 0, 0);
        // ////////////////////////////////////
        
        if (icon.isHighlighted())
        {
            double heightDelta = this.pedestal != null ? 0 : height / 2; // expand only above the pedestal
            gl.glTranslated(width / 2, heightDelta, 0);
            gl.glScaled(icon.getHighlightScale(), icon.getHighlightScale(), icon.getHighlightScale());
            gl.glTranslated(-width / 2, -heightDelta, 0);
        }

        Rectangle rect = new Rectangle((int) (screenPoint.x - width*scale / 2), (int) (screenPoint.y), (int) (width*scale),
            (int) (height*scale + (pedestalScale * height*scale) + pedestalSpacing));

        if (dc.isPickingMode())
        {
            //If in picking mode and pick clipping is enabled, check to see if the icon is within the pick volume.
            if (this.isPickFrustumClippingEnabled() && !dc.getPickFrustums().intersectsAny(rect))
            {
                // Record feedback data for this WWIcon if feedback is enabled.
                this.recordFeedback(dc, icon, uIcon.getPoint(), rect);

                return screenPoint;
            }
            else
            {
                java.awt.Color color = dc.getUniquePickColor();
                int colorCode = color.getRGB();
                this.pickSupport.addPickableObject(colorCode, icon, uIcon.getPosition(), false);
                gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
                
				gl.glScaled(width, height, 1d);
				dc.drawUnitQuad();
				this.recordFeedback(dc, icon, uIcon.getPoint(), rect);

				return screenPoint;
            }
        }

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL2.GL_GREATER, 0.001f);
        gl.glColor4d(1d, 1d, 1d, alpha);
        
        if (icon.getBackgroundTexture() != null)
            this.applyBackground(dc, icon, screenPoint, width, height, pedestalSpacing, pedestalScale);

        if (icon.getImageTexture().bind(dc))
        {
            TextureCoords texCoords = icon.getImageTexture().getTexCoords();
            gl.glScaled(width, height, 1d);
            dc.drawUnitQuad(texCoords);
        }

        if (this.pedestal != null && this.pedestal.getImageTexture() != null)
        {
            gl.glLoadIdentity();
            gl.glTranslated(screenPoint.x - (pedestalScale * (width / 2)), screenPoint.y, 0d);
            gl.glScaled(width * pedestalScale, height * pedestalScale, 1d);

            if (this.pedestal.getImageTexture().bind(dc))
            {
                TextureCoords texCoords = this.pedestal.getImageTexture().getTexCoords();
                dc.drawUnitQuad(texCoords);
            }
        }

        // Record feedback data for this WWIcon if feedback is enabled.
        this.recordFeedback(dc, icon, uIcon.getPoint(), rect);

        return screenPoint;
    }
    
    protected double calculateAlpha(double eyeDistance)
    {
        double speed = 0.6667;
        double alpha = 0.0;

        if (eyeDistance >= 0 && eyeDistance < maxVisibleDistance)
        {
            if (eyeDistance > maxVisibleDistance * speed)
            {
                alpha = (maxVisibleDistance - eyeDistance)
                        / (maxVisibleDistance - maxVisibleDistance * speed);
            }
            else
            {
                alpha = 1.0;
            }
        }
        
        return alpha;
    }
	
    protected double calcDistanceReduceFactor(double eyeDistance)
    {
        double speed = 0.25;
        double max_scale = 4.0; // 4 = scale down to quarter size
        double min_scale = 1.0; // 1 = original size
        double scale = max_scale;

        if (eyeDistance >= 0 && eyeDistance <= maxVisibleDistance)
        {
            if (eyeDistance >= maxVisibleDistance * speed)
            {
                scale = (maxVisibleDistance - eyeDistance)
                        / (maxVisibleDistance - maxVisibleDistance * speed);
                scale = ((max_scale - min_scale) * (1.0 - scale)) + min_scale; 
            }
            else
            {
                scale = min_scale;
            }
        }

        return scale;
    }
    
    
}
