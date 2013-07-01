package gov.nasa.worldwind.util;

/**
 *
 * @author Vito Čuček <vito.cucek@xlab.si / vito.cucek@gmail.com>
 */
public class ImmediateTaskService implements TaskService{

	public void shutdown(boolean immediately){
	}

	public boolean contains(Runnable runnable){
		return false;
	}

	public void addTask(Runnable runnable){
		runnable.run();
	}

	public boolean isFull(){
		return false;
	}

	public boolean hasActiveTasks(){
		return false;
	}
	
}
