package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import si.xlab.gaea.core.shaders.Shader;

/**
 *
 * @author vito
 */

public abstract class SurfaceObject
{
	protected float opacity;
	
	public boolean isVisible(DrawContext dc){
		
		boolean isInView = (dc.getVisibleSector() == null || dc.getVisibleSector().intersects(this.getSector()));
		return isInView;
	}

	public Shader getShader(DrawContext dc){
		return null;
	}

	public void enableShader(DrawContext dc, Shader shader){
		if(shader != null && shader.isValid()){
			if(dc.getShaderContext().getCurrentShader() != shader){
				shader.enable(dc.getShaderContext());
			}
		}
		else if(dc.getShaderContext().getCurrentShader() != null){
			dc.getShaderContext().getCurrentShader().disable(dc.getShaderContext());
		}
	}

	public float getOpacity(){
		return this.opacity;
	}

	public void setOpacity(float opacity){
		this.opacity = opacity;
	}
	
	public abstract Sector getSector();
	
    /**
	 * Return object size in bytes.
	 */
    public abstract long getSize();

	/**
	 * Set render state and parameters for render.
	 * Don't call this method directly unless you know what you are doing.
	 */
	public abstract void preRender(DrawContext dc, Sector sector);

	/**
	 * Render data in geographic coordinates space.
	 * Don't call this method directly unless you know what you are doing.
	 */
	public abstract void render(DrawContext dc);

	public abstract void dispose();
}