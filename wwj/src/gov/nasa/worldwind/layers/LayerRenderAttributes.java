package gov.nasa.worldwind.layers;

/**
 *
 * @author Vito Čuček <vito.cucek@xlab.si / vito.cucek@gmail.com>
 */
public class LayerRenderAttributes{
    
    public static enum RenderType{
        GLOBE,      //signals that layer should be rendered during "GLOBE" rendering pass
        SCREEN      //signals that layer should be rendered during "SCREEN" rendering pass
    }
    
    public static final int COLOR_MODE = 0x01;      
	public static final int TEXTURE_MODE = 0x02;
    public static final int VOLUME_MODE = 0x04;
	public static final int NORMAL_MODE = 0x08;
	public static final int MATERIAL_MODE = 0x16;
    
    private RenderType renderType;
    
    private int renderMode;

    public LayerRenderAttributes()
    {
        this.renderType = RenderType.GLOBE;
        this.renderMode = COLOR_MODE;
    }

    /**
     * Returns the layer type, which signals whether layer should be rendered during GLOBE or SCREEN rendering pass.
     * By default, all geo-referenced layers should use GLOBE and all screen-relative layers should use SCREEN.
     * @return 
     */
    public RenderType getRenderType(){
        return this.renderType;
    }
    
    public void setRenderType(RenderType renderType)
    {
        this.renderType = renderType;
    }
           
    /**
     * Returns OpenGL mode mask, which signals what shaders and buffers should be used during rendering.
     * By default, all layers use just TEXTURE_MODE.
     * @return 
     */    
    public int getRenderMode()
    {
        return renderMode;
    }
    
    public void setRenderMode(int renderMode)
    {
        this.renderMode = renderMode;
    }
}
