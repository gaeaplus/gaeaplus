package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author vito
 */
public class GLTaskService{

	private int maxTaskPerFrame = 1;
	private int maxTaskQueueSize = 10000;

	private HashSet<GLTask> taskQueue = new HashSet<GLTask>();
	private HashSet<GLDisposable> taskQueueDispose = new HashSet<GLDisposable>();

	public synchronized boolean addDisposable(GLDisposable taskDispose){

		if(taskDispose == null)
			return false;

		if(!taskQueueDispose.contains(taskDispose)){
			taskQueueDispose.add(taskDispose);
		}
		return true;
	}

	public synchronized boolean addTask(GLTask task){

		if(task == null)
			return false;

		if(taskQueue.size() >= maxTaskQueueSize){
			return false;
		}

		if(!taskQueue.contains(task)){
			taskQueue.add(task);
		}
		return true;
	}
	public synchronized void removeTask(GLTask task){
		if(taskQueue.contains(task)){
			taskQueue.remove(task);
		}
	}

	public boolean hasTasks(){
		return (!taskQueue.isEmpty() && !taskQueueDispose.isEmpty());
	}

	public boolean hasDisposeTasks(){
		return !taskQueueDispose.isEmpty();
	}

	public boolean isFull(){
		return (taskQueue.size() >= maxTaskQueueSize);
	}

	public synchronized void flush(DrawContext dc)
	{
		if(taskQueue.isEmpty() && taskQueueDispose.isEmpty()){
			return;
		}

		if(!taskQueue.isEmpty()){
			Iterator<GLTask> iter = taskQueue.iterator();

			int i = 0;
			while(iter.hasNext() && i<maxTaskPerFrame)
			{
				iter.next().run(dc);
				iter.remove();
				i = i+1;
			}
		}

		if(!taskQueueDispose.isEmpty()){
			Iterator<GLDisposable> iter = taskQueueDispose.iterator();

			while(iter.hasNext())
			{
				iter.next().dispose(dc.getGL().getGL2());
				iter.remove();
			}
			taskQueueDispose.clear();
		}
	}

	public synchronized void setMaxTaskPerFrame(int num){
		this.maxTaskPerFrame = num;
	}
}
