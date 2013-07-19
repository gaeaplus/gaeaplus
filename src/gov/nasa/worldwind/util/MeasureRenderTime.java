/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.render.DrawContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;
import java.util.logging.Logger;
import javax.media.opengl.GL2;

/**
 *
 * @author vito
 */
public class MeasureRenderTime {

	private static int[] timeQueries = new int[250];
	private static Stack<Integer> freeQueries = new Stack<Integer>();

	private static ArrayList<GpuTimer> gpuTimers = new ArrayList<GpuTimer>();
	private static GpuTimer currentGpuTimer = new GpuTimer("root", null);
	
	private static boolean timeQueriesInit = true;
	
	private static boolean isEnabled = false;
	private static boolean isMesureGpu = false;
	
	private static Stack<Long> timeStartStack = new Stack<Long>();
	private static Stack<String> nameStack = new Stack<String>();

	private static final Logger logger = Logging.logger("MesureRenderTime");
	
	public static void startMeasure(DrawContext dc, String name){
		if(isEnabled){
			
			nameStack.push(name);
			
			if(isMesureGpu){
				GL2 gl = dc.getGL().getGL2();
				if(timeQueriesInit){
					gl.glGenQueries(timeQueries.length, timeQueries, 0);	
					for(int query : timeQueries){
						freeQueries.add(query);
					}
					timeQueriesInit = false;
				}


				String displayString = "";
				for(String s : nameStack){
					displayString = displayString.concat("-> ".concat(s));
				}

				GpuTimer gpuTimer = new GpuTimer(displayString, currentGpuTimer);
				gpuTimers.add(gpuTimer);
				currentGpuTimer = gpuTimer;
				gpuTimer.start(dc, freeQueries);
			}
			
			timeStartStack.push(System.currentTimeMillis());
		}
	}

	public static void stopMeasure(DrawContext dc){
		if(isEnabled){
			String displayString = "";
			for(String s : nameStack){
				displayString = displayString.concat("-> ".concat(s));
			}
			
			if(isMesureGpu){
				currentGpuTimer.stop(dc);
				currentGpuTimer = currentGpuTimer.getParent();
			}
			
			nameStack.pop();
			dc.setPerFrameStatistic(PerformanceStatistic.RENDER_TIME_CPU, displayString, System.currentTimeMillis() - timeStartStack.pop());
		}
	}

	public static void collectGpuStatistics(DrawContext dc){

		if(!currentGpuTimer.getName().equals("root")){
			logger.severe("Fatal ERROR");
		}

		if(currentGpuTimer.childrens.isEmpty()){
			return;
		}
	
		for(GpuTimer gpuTimer : gpuTimers){
			double time = ((double)(GpuTimer.getTimeRecrusive(dc, gpuTimer)))/1000000.0d;
			String name = gpuTimer.getName();
			if(time > 1.0){
				dc.setPerFrameStatistic(PerformanceStatistic.RENDER_TIME_GPU, name, time);
			}
		}
		gpuTimers.clear();

		currentGpuTimer = new GpuTimer("root", null);
		freeQueries.clear();
		for(int query : timeQueries){
			freeQueries.add(query);
		}
		
	}

	public static void setMesureGpu(boolean enable){
		isMesureGpu = enable;
	}

	public static void enable(boolean enable){
		isEnabled = enable;
	}

	private static class GpuTimer{

		private String name;
		private boolean isRunning = false;
		private Stack<Integer> timeQueries = new Stack<Integer>();
		
		private GpuTimer parent;
		private HashSet<GpuTimer> childrens = new HashSet<GpuTimer>();

		public GpuTimer(String name, GpuTimer parent)
		{
			this.name = name;
			this.parent = parent;
			if(parent != null){
				parent.childrens.add(this);
			}
		}

		public void start(DrawContext dc, Stack<Integer> freeTimeQueries){
		
			GL2 gl = dc.getGL().getGL2();

			if(freeTimeQueries.size() == 0){
				logger.warning("GpuTimer.start() - no free time queries (call \"collectGpuStatistics()\" to free resources!)");
				return;
			}
			
			if(!isRunning){
				if(parent != null){
					gl.glEndQuery(GL2.GL_TIME_ELAPSED_EXT);
					parent.timeQueries.push(freeTimeQueries.pop());
				}

				timeQueries.push(freeTimeQueries.pop());
				gl.glBeginQuery(GL2.GL_TIME_ELAPSED_EXT, timeQueries.peek());
				this.isRunning = true;
			}
		}

		public void stop(DrawContext dc){
			if(isRunning){
				GL2 gl = dc.getGL().getGL2();
				gl.glEndQuery(GL2.GL_TIME_ELAPSED_EXT);
			
				if(parent != null){
					gl.glBeginQuery(GL2.GL_TIME_ELAPSED_EXT, parent.timeQueries.peek());
				}
				isRunning = false;
			}
		}

		public String getName(){
			return this.name;
		}

		public long getTime(DrawContext dc){
			GL2 gl = dc.getGL().getGL2();
			int out = 0;
			for(int query : this.timeQueries){
				int[] isDone = new int[1];
				long[] timeNano = new long[1];
				gl.glGetQueryObjectiv(query,
									GL2.GL_QUERY_RESULT_AVAILABLE,
									isDone, 0);
				if(isDone[0] == GL2.GL_TRUE){
					gl.glGetQueryObjectui64vEXT(query, GL2.GL_QUERY_RESULT, timeNano, 0);
					out += timeNano[0];
				}
			}
			return out;
		}

		public static long getTimeRecrusive(DrawContext dc, GpuTimer timer){
			long out = timer.getTime(dc);
			for(GpuTimer gt : timer.childrens){
				out += getTimeRecrusive(dc, gt);
			}
			return out;
		}

		public GpuTimer getParent(){
			return this.parent;
		}

		public Collection<GpuTimer> getChildrens(){
			return this.childrens;
		}
	}
}
