package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.util.Logging;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;

/**
 *
 * @author Vito Čuček <vito.cucek@xlab.si / vito.cucek@gmail.com>
 */
public final class ImmediateRetrievalSrvice extends WWObjectImpl
    implements RetrievalService, Thread.UncaughtExceptionHandler
{
    // These constants are last-ditch values in case Configuration lacks defaults
    private static final int DEFAULT_TIME_PRIORITY_GRANULARITY = 500; // milliseconds

    /**
     * Encapsulates a single threaded retrieval as a {@link java.util.concurrent.FutureTask}.
     */
    private static class RetrievalTask extends FutureTask<Retriever>
        implements RetrievalFuture, Comparable<RetrievalTask>
    {
        private Retriever retriever;
        private double priority; // retrieval secondary priority (primary priority is submit time)

        private RetrievalTask(Retriever retriever, double priority)
        {
            super(retriever);
            this.retriever = retriever;
            this.priority = priority;
        }

        public double getPriority()
        {
            return priority;
        }

        public Retriever getRetriever()
        {
            return this.retriever;
        }

        @Override
        public void run()
        {
            if (this.isDone() || this.isCancelled())
                return;

            super.run();
        }

        /**
         * @param that the task to compare with this one
         * @return 0 if task priorities are equal, -1 if priority of this is less than that, 1 otherwise
         * @throws IllegalArgumentException if <code>that</code> is null
         */
        public int compareTo(RetrievalTask that)
        {
            if (that == null)
            {
                String msg = Logging.getMessage("nullValue.RetrieverIsNull");
                Logging.logger().fine(msg);
                throw new IllegalArgumentException(msg);
            }

            if (this.priority > 0 && that.priority > 0) // only secondary priority used if either is negative
            {
                // Requests submitted within different time-granularity periods are ordered exclusive of their
                // client-specified priority.
                long now = System.currentTimeMillis();
                long thisElapsedTime = now - this.retriever.getSubmitTime();
                long thatElapsedTime = now - that.retriever.getSubmitTime();
                if (((thisElapsedTime - thatElapsedTime) / DEFAULT_TIME_PRIORITY_GRANULARITY) != 0)
                    return thisElapsedTime < thatElapsedTime ? -1 : 1;
            }

            // The client-pecified priority is compared for requests submitted within the same granularity period.
            return this.priority == that.priority ? 0 : this.priority < that.priority ? -1 : 1;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final RetrievalTask that = (RetrievalTask) o;

            // Tasks are equal if their retrievers are equivalent
            return this.retriever.equals(that.retriever);
            // Priority and submint time are not factors in equality
        }

        @Override
        public int hashCode()
        {
            return this.retriever.getName().hashCode();
        }
    }

    public void uncaughtException(Thread thread, Throwable throwable)
    {
        Logging.logger().log(Level.WARNING, "BasicRetrievalService:{0}", Logging.getMessage("BasicRetrievalService.UncaughtExceptionDuringRetrieval",
                         thread.getName()));
    }

    public ImmediateRetrievalSrvice()
    {
    }

    public void shutdown(boolean immediately)
    {
    }

    /**
     * @param retriever the retriever to run
     * @return a future object that can be used to query the request status of cancel the request.
     * @throws IllegalArgumentException if <code>retrieer</code> is null or has no name
     */
    public RetrievalFuture runRetriever(Retriever retriever)
    {
        if (retriever == null)
        {
            String msg = Logging.getMessage("nullValue.RetrieverIsNull");
            Logging.logger().fine(msg);
            throw new IllegalArgumentException(msg);
        }
        if (retriever.getName() == null)
        {
            String message = Logging.getMessage("nullValue.RetrieverNameIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        // Add with secondary priority that removes most recently added requests first.
        return this.runRetriever(retriever, (double) (Long.MAX_VALUE - System.currentTimeMillis()));
    }

    /**
     * @param retriever the retriever to run
     * @param priority  the secondary priority of the retriever, or negative if it is to be the primary priority
     * @return a future object that can be used to query the request status of cancel the request.
     * @throws IllegalArgumentException if <code>retriever</code> is null or has no name
     */
    public synchronized RetrievalFuture runRetriever(Retriever retriever, double priority)
    {
        if (retriever == null)
        {
            String message = Logging.getMessage("nullValue.RetrieverIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        if (retriever.getName() == null)
        {
            String message = Logging.getMessage("nullValue.RetrieverNameIsNull");
            Logging.logger().fine(message);
            throw new IllegalArgumentException(message);
        }

        if (!this.isAvailable())
        {
            Logging.logger().log(Level.FINER, "BasicRetrievalService: {0}", Logging.getMessage("BasicRetrievalService.ResourceRejected", retriever.getName()));
        }

        RetrievalTask task = new RetrievalTask(retriever, priority);
        retriever.setSubmitTime(System.currentTimeMillis());

		task.run();

        return task;
    }

    public void setRetrieverPoolSize(int poolSize)
    {
    }

    public int getRetrieverPoolSize()
    {
        return 1;
    }

    public boolean hasActiveTasks()
    {
        return false;
    }

    public boolean isAvailable()
    {
		return true;
    }

    public int getNumRetrieversPending()
    {
		return 0;
    }

    /**
     * @param retriever the retriever to check
     * @return <code>true</code> if the retriever is being run or pending execution
     * @throws IllegalArgumentException if <code>retriever</code> is null
     */
    public boolean contains(Retriever retriever)
    {
       return false;
    }

    public double getProgress()
    {
        return 50;
    }
    
    protected SSLExceptionListener sslExceptionListener;

    public SSLExceptionListener getSSLExceptionListener()
    {
        return sslExceptionListener;
    }

    public void setSSLExceptionListener(SSLExceptionListener sslExceptionListener)
    {
        this.sslExceptionListener = sslExceptionListener;
    }
}
