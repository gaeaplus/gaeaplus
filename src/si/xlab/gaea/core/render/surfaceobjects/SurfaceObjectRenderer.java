package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import java.util.ArrayList;
import java.util.List;
import si.xlab.gaea.core.layers.RenderToTextureLayer;

/**
 *
 * @author vito
 */
public class SurfaceObjectRenderer {

	private boolean enabled;
	private double opacity;
	private double maxActiveAltitude;
	private Sector sector;

	private final RenderToTextureLayer rttl;
	private List<SurfaceObject> renderList;

	public SurfaceObjectRenderer(){
		this(Sector.FULL_SPHERE);
	}

	public SurfaceObjectRenderer(Sector sector)
	{
		this(RenderToTextureLayer.getInstance(), sector);
	}

	public SurfaceObjectRenderer(RenderToTextureLayer textureLayer, Sector sector){
		this.rttl = textureLayer;
		this.renderList = new ArrayList<SurfaceObject>();
		this.sector = sector;
		this.opacity = 1.0d;
		this.maxActiveAltitude = Double.MAX_VALUE;
		this.enabled = true;
	}

	/**
	 * Add <code>SurfaceObject</code> to <SurfaceLayer>SurfaceLayer</SurfaceLayer>
	 * render queue. When <code>SurfaceObject</code> is rendered it is removed from
	 * render queue.
	 *
	 * @param surfaceObject
	 */
	public void addToRenderQueue(SurfaceObject surfaceObject){
		if(surfaceObject != null){
			if(renderList.isEmpty()){
				rttl.addSurfaceLayer(this);
			}
			renderList.add(surfaceObject);
		}
	}

	public void setSector(Sector sector){
		this.sector = sector;
	}

	/**
	 * Render all surface objects to current FBO or screen buffer if FBO not bounded.
	 * This method should be called from render to texture renderer.
	 * Don't call this method directly unless you know what you are doing.
	 *
	 * @param dc the current <code>DrawContext</code>
	 * @param textureSector the <code>Sector</code> that outline current texture.
	 */
	public void renderTexture(DrawContext dc, Sector textureSector){

		if(!isLayerVisible(dc)){
			return;
		}
		for(SurfaceObject so : renderList){
			if(so == null || !so.getSector().intersects(textureSector))
				continue;

			so.setOpacity((float)this.opacity);
			so.enableShader(dc, so.getShader(dc));
			so.preRender(dc, textureSector);
			so.render(dc);
		}
	}

	/**
	 * Clears current render queue.
	 */
	public void flush(){
		renderList.clear();
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		if(!enabled){
			flush();
		}
		this.enabled = enabled;
		redraw();
	}

	public boolean isLayerVisible(DrawContext dc){
		return (isEnabled() && dc.getVisibleSector() != null && dc.getView() != null
				&& dc.getVisibleSector().intersects(sector)
				&& dc.getView().getEyePosition().elevation < maxActiveAltitude);
	}

	public void redraw(Sector sector){
		this.rttl.reload(sector);
	}

	public void redraw(){
		this.rttl.reloadAll();
	}

	public double getMaxActiveAltitude()
	{
		return maxActiveAltitude;
	}

	public void setMaxActiveAltitude(double maxElevation)
	{
		this.maxActiveAltitude = maxElevation;
	}

	public double getOpacity()
	{
		return opacity;
	}

	public void setOpacity(double opacity)
	{
		if(this.opacity != opacity){
			this.opacity = opacity;
			redraw();
		}
	}
}
