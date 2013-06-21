/*
Copyright (C) 2001, 2006 United States Government
as represented by the Administrator of the
National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.WWObjectImpl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Vito Cucek - modeled after ThreadedTaskService
 * @version $Id: ThreadedTaskService.java 2471 2007-07-31 21:50:57Z tgaskins $
 */
public class ThreadedCalcTaskService extends WWObjectImpl implements TaskService, Thread.UncaughtExceptionHandler
{
    static final private int DEFAULT_CORE_POOL_SIZE = 2;
    static final private int DEFAULT_QUEUE_SIZE = 6;
    private static final String RUNNING_THREAD_NAME_PREFIX = "ThreadedCalcTaskService.runningCalc";
    private static final String IDLE_THREAD_NAME_PREFIX = "ThreadedCalcTaskService.idleThread";
    private ConcurrentLinkedQueue<Runnable> activeTasks; // tasks currently allocated a thread
    private TaskExecutor executor; // thread pool for running retrievers

    public ThreadedCalcTaskService()
    {
//        Integer poolSize = Configuration.getIntegerValue(AVKey.TASK_POOL_SIZE, DEFAULT_CORE_POOL_SIZE);
//        Integer queueSize = Configuration.getIntegerValue(AVKey.TASK_QUEUE_SIZE, DEFAULT_QUEUE_SIZE);

		Integer poolSize = DEFAULT_CORE_POOL_SIZE;
		Integer queueSize = DEFAULT_QUEUE_SIZE;

        // this.executor runs the tasks, each in their own thread
        this.executor = new TaskExecutor(poolSize, queueSize);

        // this.activeTasks holds the list of currently executing tasks
        this.activeTasks = new ConcurrentLinkedQueue<Runnable>();
    }

    public void shutdown(boolean immediately)
    {
        if (immediately)
            this.executor.shutdownNow();
        else
            this.executor.shutdown();

        this.activeTasks.clear();
    }

    public void uncaughtException(Thread thread, Throwable throwable)
    {
        String message = Logging.getMessage("ThreadedCalcTaskService.UncaughtExceptionDuringTask", thread.getName());
        Logging.logger().fine(message);
        Thread.currentThread().getThreadGroup().uncaughtException(thread, throwable);
    }

    private class TaskExecutor extends ThreadPoolExecutor
    {
        private static final long THREAD_TIMEOUT = 4; // keep idle threads alive this many seconds

        private TaskExecutor(int poolSize, int queueSize)
        {
            super(poolSize, poolSize, THREAD_TIMEOUT, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(queueSize),
                new ThreadFactory()
                {
                    public Thread newThread(Runnable runnable)
                    {
                        Thread thread = new Thread(runnable);
                        thread.setDaemon(true);
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.setUncaughtExceptionHandler(ThreadedCalcTaskService.this);
                        return thread;
                    }
                }, new ThreadPoolExecutor.DiscardPolicy() // abandon task when queue is full
            {
                @Override
                public void rejectedExecution(Runnable runnable,
                    ThreadPoolExecutor threadPoolExecutor)
                {
                    // Interposes logging for rejected execution
                    String message = Logging.getMessage("ThreadedTaskService.ResourceRejected", runnable);
                    Logging.logger().fine(message);
                    super.rejectedExecution(runnable, threadPoolExecutor);
                }
            });
        }

        @Override
        protected void beforeExecute(Thread thread, Runnable runnable)
        {
            if (thread == null)
            {
                String msg = Logging.getMessage("nullValue.ThreadIsNull");
                Logging.logger().fine(msg);
                throw new IllegalArgumentException(msg);
            }

            if (runnable == null)
            {
                String msg = Logging.getMessage("nullValue.RunnableIsNull");
                Logging.logger().fine(msg);
                throw new IllegalArgumentException(msg);
            }

            if (ThreadedCalcTaskService.this.activeTasks.contains(runnable))
            {
                // Duplicate requests are simply interrupted here. The task itself must check the thread's isInterrupted
                // flag and actually terminate the task.
                String message = Logging.getMessage("ThreadedCalcTaskService.CancellingDuplicateTask", runnable);
                Logging.logger().finer(message);
                thread.interrupt();
                return;
            }

            ThreadedCalcTaskService.this.activeTasks.add(runnable);

            if (RUNNING_THREAD_NAME_PREFIX != null)
                thread.setName(RUNNING_THREAD_NAME_PREFIX + runnable);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setUncaughtExceptionHandler(ThreadedCalcTaskService.this);

            super.beforeExecute(thread, runnable);
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable)
        {
            if (runnable == null)
            {
                String msg = Logging.getMessage("nullValue.RunnableIsNull");
                Logging.logger().fine(msg);
                throw new IllegalArgumentException(msg);
            }

            super.afterExecute(runnable, throwable);

            ThreadedCalcTaskService.this.activeTasks.remove(runnable);

            if (throwable == null && IDLE_THREAD_NAME_PREFIX != null)
                Thread.currentThread().setName(IDLE_THREAD_NAME_PREFIX);
        }
    }

    public synchronized boolean contains(Runnable runnable)
    {
        //noinspection SimplifiableIfStatement
        if (runnable == null)
            return false;

        return (this.activeTasks.contains(runnable) || this.executor.getQueue().contains(runnable));
    }

    /**
     * Enqueues a task to run.
     *
     * @param runnable the task to add
     * @throws IllegalArgumentException if <code>runnable</code> is null
     */
    public synchronized void addTask(Runnable runnable)
    {
        if (runnable == null)
        {
            String message = Logging.getMessage("nullValue.RunnableIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        // Do not queue duplicates.
        if (this.activeTasks.contains(runnable) || this.executor.getQueue().contains(runnable))
            return;

        this.executor.execute(runnable);
    }

	public int getProgress(){
		return (int)(100.0f * ((float)this.executor.getQueue().size()/(float)DEFAULT_QUEUE_SIZE));
	}

	public int getNumberOfTasks(){
		return this.executor.getQueue().size();
	}

    public boolean isFull()
    {
        return this.executor.getQueue().remainingCapacity() == 0;
    }

    public boolean hasActiveTasks()
    {
        Thread[] threads = new Thread[Thread.activeCount()];
        int numThreads = Thread.enumerate(threads);
        for (int i = 0; i < numThreads; i++)
        {
            if (threads[i].getName().startsWith(RUNNING_THREAD_NAME_PREFIX))
                return true;
        }
        return false;
    }
}
