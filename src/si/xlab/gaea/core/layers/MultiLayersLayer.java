package si.xlab.gaea.core.layers;


import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

import java.util.Iterator;


public class MultiLayersLayer extends AbstractLayer
{
    protected LayerList layer_list;
	
    // private boolean enabled = false;

    public MultiLayersLayer()
    {
        this.layer_list = new LayerList();
    }
	
    public void add(Layer layer)
    {
        if (layer == null)
        {
            String msg = Logging.getMessage("nullValue.LayerIsNull");

            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.layer_list.add(layer);
        layer.setEnabled(super.isEnabled());
        
        if (layer.getExpiryTime() != getExpiryTime())
            layer.setExpiryTime(getExpiryTime());
        if (layer.getOpacity() != getOpacity())
            layer.setOpacity(getOpacity());
    }
    
    public void setEnabled(boolean enabled)
    {
        Iterator<Layer> it = layer_list.iterator();

        while (it.hasNext())
        {
            it.next().setEnabled(enabled);
        }

        super.setEnabled(enabled);
    }

	@Override
	protected void doPreRender(DrawContext dc) {
		Iterator<Layer> it = layer_list.iterator();

        while (it.hasNext())
        {
            it.next().preRender(dc);
        }
	}

    
    protected void doRender(DrawContext dc)
    {
        Iterator<Layer> it = layer_list.iterator();

        while (it.hasNext())
        {
            it.next().render(dc);
        }
    }
	
    public void pick(DrawContext dc, java.awt.Point point)
    {
        Iterator<Layer> it = layer_list.iterator();

        while (it.hasNext())
        {
            it.next().pick(dc, point);
        }
    }
	
    @Override
    public void setOpacity(double opacity)
    {
        super.setOpacity(opacity);
        Iterator<Layer> it = layer_list.iterator();

        while (it.hasNext())
        {
            it.next().setOpacity(super.getOpacity());
        }
    }
		
    @Override
    public void setExpiryTime(long expiryTime)
    {
        super.setExpiryTime(expiryTime);
        for (Layer sublayer: layer_list)
            sublayer.setExpiryTime(expiryTime);
    }

	public LayerList getLayers(){
		return this.layer_list;
	}
}

