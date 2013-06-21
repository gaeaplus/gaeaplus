/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gov.nasa.worldwind.util;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 *
 * @author mariano
 */
public class GlobalClock implements RenderingListener
{

    public static final int USE_FRAMES = 1;
    public static final int USE_TIME = 2;
    private int fps;

    private static GlobalClock instance=null;
    private ReentrantReadWriteLock rwl;
    private long lastSystemTime;
    private long lastTime;
    private double factor;
    private int method;
    private WorldWindow wwd;
	
	private boolean skipFrame = false;


    public static long currentTimeMillis()
    {
        return getInstance(null).getCurrentTimeMillis();
    }

    public static double getFactor()
    {
            return getInstance(null)._getFactor();
    }

    public static  void setFactor(double factor)
    {
            getInstance(null)._setFactor(factor);
    }

    public static int getMethod()
    {
            return getInstance(null)._getMethod();
    }

    public static void setMethod(int method)
    {
        getInstance(null)._setMethod(method);
    }

    public static void shift(long delta)
    {
        getInstance(null)._shift(delta);
    }

	public static void skipFrame(){
		getInstance(null).skipFrame = true;
	}

	public static boolean isSkipFrame(){
		return getInstance(null).skipFrame;
	}

    public static GlobalClock getInstance(WorldWindow wwd)
    {
        if(instance==null)
        {
            synchronized(GlobalClock.class)
            {
                if(instance==null)
                {
                    if(wwd!=null)
                    {
                        instance=new GlobalClock(wwd);
                    }
                }
            }
        }
        return instance;
    }

    private GlobalClock(WorldWindow wwd)
    {
        this.rwl = new ReentrantReadWriteLock();
        this.lastTime = lastSystemTime = System.currentTimeMillis();
        this.method = USE_TIME;
        this.wwd = wwd;
        this.fps=25;
        this.factor=1.0;
        this.wwd.addRenderingListener(this);
    }


    public long getCurrentTimeMillis()
    {
        long calcTime = lastTime;
        switch(method)
        {
            case USE_TIME:
                long current = System.currentTimeMillis();
                rwl.readLock().lock();
                    calcTime = lastTime + (long) ((current - lastSystemTime) * factor);
                rwl.readLock().unlock();
                break;
            case USE_FRAMES:
                calcTime=lastTime;
        }

        return calcTime;
    }

    public void stageChanged(RenderingEvent event)
    {
        if(method==USE_FRAMES && event.getStage().equalsIgnoreCase(RenderingEvent.AFTER_BUFFER_SWAP))
        {
			if(this.skipFrame){
				this.skipFrame = false;
			}
			else{
            	long delta = ((long)(1000*factor/(double)fps));
            	rwl.readLock().lock();
                lastTime+=delta;
                lastSystemTime+=delta;
            	rwl.readLock().unlock();
			}
        }
    }


    public double _getFactor()
    {
            return factor;
    }

    public void _setFactor(double factor)
    {
            blockingRefreshState();
            this.factor=factor;
    }

    public int _getMethod()
    {
            return method;
    }

    public void _setMethod(int method)
    {
            blockingRefreshState();
            this.method=method;
    }

    public void _shift(long delta)
    {
        blockingRefreshState();
        rwl.writeLock().lock();
            lastTime+=delta;
        rwl.writeLock().unlock();
    }

    private void blockingRefreshState()
    {
        long current = System.currentTimeMillis();
        rwl.writeLock().lock();
            long newTime = lastTime + (long) ((current - lastSystemTime) * factor);
            lastTime=newTime;
            lastSystemTime=current;
        rwl.writeLock().unlock();
    }
}
