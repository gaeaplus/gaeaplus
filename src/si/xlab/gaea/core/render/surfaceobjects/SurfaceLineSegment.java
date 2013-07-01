/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package si.xlab.gaea.core.render.surfaceobjects;

import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.ogc.kml.KMLStyle;
import gov.nasa.worldwind.render.DrawContext;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author marjan
 */
public class SurfaceLineSegment
{
    public List<LatLon> vertexList;
    private List<Vec4> vertexListV;
    private long vertexListVUpdateTime = 0;
    double distance;
    
    private String name = null;
    private Sector sector = null;
    private Extent extent = null;
    
    KMLStyle style;
    
    private long extentLastCalc = 0;

    public SurfaceLineSegment()
    {
        vertexList = new ArrayList<LatLon>();
    }

    public SurfaceLineSegment(List<LatLon> latLonList)
    {
        this();
        this.vertexList = latLonList;
        this.distance = 0;
        for (int i = 0; i < latLonList.size() - 1; i++)
        {
            this.distance = this.distance + (LatLon.rhumbDistance(latLonList.get(i), latLonList.get(i + 1))).degrees;
        }
    }

    public SurfaceLineSegment(List<LatLon> latLonList, String name)
    {
        this(latLonList);
        if (name != null)
        {
            this.name = name;
            this.sector = Sector.boundingSector(latLonList); //sector is only used when drawing labels
        }
    }

    public SurfaceLineSegment(List<LatLon> latLonList, String name, KMLStyle style)
    {
        this(latLonList, name);
        this.style = style;
    }

    public String getName(){
        return this.name;
    }

    public void add(LatLon ll)
    {
        vertexList.add(ll);
    }

    public List<LatLon> getVertexList()
    {
        return vertexList;
    }

    public List<Vec4> getVertexListV(DrawContext dc)
    {
        long currentTime = System.currentTimeMillis();
        if (vertexListV == null || vertexListV.isEmpty() || currentTime - vertexListVUpdateTime > 3000)
        {
            if (vertexListV == null)
            {
                vertexListV = new ArrayList<Vec4>();
            }
            vertexListV.clear();
            for (LatLon ll : vertexList)
            {
                double elev = dc.getGlobe().getElevation(ll) * dc.getVerticalExaggeration();
                Vec4 v = dc.getGlobe().computePointFromPosition(ll, elev);
                vertexListV.add(v);
            }
            vertexListVUpdateTime = System.currentTimeMillis() + (long)(3000.0 * Math.random());
            return vertexListV;
        } else
        {
            return vertexListV;
        }
    }

    public Sector getSector(){

        if(this.sector == null){
            this.sector = Sector.boundingSector(this.vertexList);
        }
        
        return this.sector;
    }

    public Extent getExtent(DrawContext dc){
        if(this.sector == null){
            return null;
        }

        if(System.currentTimeMillis() - extentLastCalc > 3000){
            this.extent = Sector.computeBoundingBox(dc.getGlobe(), dc.getVerticalExaggeration(), this.getSector());
            this.extentLastCalc = System.currentTimeMillis() + (long)(3000.0 * Math.random());
        }
        return this.extent;
    }
    
    public KMLStyle getStyle() 
    {
        return style;
    }

    public void setStyle(KMLStyle style) 
    {
        this.style = style;
    }

    public int getSize()
    {
        return vertexList.size();
    }

    public int getID(){
        
        int result = 1;
        if(name != null){
            result += 7 * result + name.hashCode();
        }
        result += 7 * result + vertexList.size();

        if(vertexList.size() > 0){
            result += 7 * result + vertexList.get(0).hashCode();
            result += 7 * result + vertexList.get(vertexList.size()-1).hashCode();
        }
        return result;
    }

//    @Override
//    public int hashCode()
//    {
//        int result = 0;
//        for (LatLon ll : vertexList)
//        {
//            result = 7 * result + ll.hashCode();
//        }
//        return result;
//    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final SurfaceLineSegment line = (SurfaceLineSegment) obj;
        if (hashCode() != line.hashCode())
            return false;
        return true;
    }
    
}
