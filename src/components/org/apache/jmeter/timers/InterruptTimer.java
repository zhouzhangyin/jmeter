package org.apache.jmeter.timers;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

public class InterruptTimer extends AbstractTestElement implements Timer, Serializable, ThreadListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggingManager.getLoggerForClass();

    private static final String TIMEOUT = "InterruptTimer.timeout"; //$NON-NLS-1$

    private static class TPOOLHolder {
        static final ScheduledExecutorService EXEC_SERVICE =
                Executors.newScheduledThreadPool(1,
                        new ThreadFactory() {
                            public Thread newThread(Runnable r) {
                                Thread t = Executors.defaultThreadFactory().newThread(r);
                                t.setDaemon(true); // also ensures that Executor thread is daemon
                                return t;
                            }
                        });
    }

    private static ScheduledExecutorService getExecutorService() {
        return TPOOLHolder.EXEC_SERVICE;
    }

    private JMeterContext context;

    private ScheduledFuture<?> future;
    
    private final ScheduledExecutorService execService;
    
    private final boolean debug;

    /**
     * No-arg constructor.
     */
    public InterruptTimer() {
        LOG.setPriority(org.apache.log.Priority.DEBUG); // for local debugging when enabled
        debug = LOG.isDebugEnabled();
        execService = getExecutorService();
        if (debug) {
            LOG.debug(whoAmI("InterruptTimer()", this));
        }
    }

    /**
     * Set the timeout for this timer.
     * @param timeout The timeout for this timer
     */
    public void setTimeout(String timeout) {
        setProperty(TIMEOUT, timeout);
    }

    /**
     * Get the timeout value for display.
     *
     * @return the timeout value for display.
     */
    public String getTimeout() {
        return getPropertyAsString(TIMEOUT);
    }

    /**
     * Retrieve the delay to use during test execution.
     * This is called just before starting a sampler.
     * It is used to schedule a future task to interrupt the sampler.
     * It also cancels any existing timer
     * 
     * @return Always returns zero, because this timer does not wait
     */
    @Override
    public long delay() {
        if (debug) {
            LOG.debug(whoAmI("delay()", this));
        }
        cancelTask(); // cancel previous if any
        long timeout = getPropertyAsLong(TIMEOUT); // refetch each time so it can be a variable
        if (timeout <= 0) {
            return 0;
        }
        final Sampler samp = context.getCurrentSampler();
        if (!(samp instanceof Interruptible)) { // may be applied to a whole test 
            return 0; // Cannot time out in this case
        }
        final Interruptible sampler = (Interruptible) samp;
        Runnable run=new Runnable() {
            public void run() {
                  boolean interrupted = sampler.interrupt();
                  if (interrupted) {
                      LOG.warn("Done interrupting " + getInfo(samp));
                  } else {
                      if (debug) {
                          LOG.debug("Didn't interrupt: " + getInfo(samp));                          
                      }
                  }
            }
        };

        // schedule the interrupt to occur and save for possible cancellation 
        future = execService.schedule(run, timeout, TimeUnit.MILLISECONDS);
        if (debug) {
            LOG.debug("Scheduled timer: @" + System.identityHashCode(future) + " " + getInfo(samp));
        }
        return 0;
    }

    @Override
    public void threadStarted() {
        if (debug) {
            LOG.debug(whoAmI("threadStarted()", this));
        }
        context = JMeterContextService.getContext();
     }

    @Override
    public void threadFinished() {
        if (debug) {
            LOG.debug(whoAmI("threadFinished()", this));
        }
        cancelTask(); // cancel final if any
     }

    /**
     * Provide a description of this class.
     *
     * @return the description of this class.
     */
    @Override
    public String toString() {
        return JMeterUtils.getResString("interrupt_timer_memo"); //$NON-NLS-1$
    }

    private String whoAmI(String id, TestElement o) {
        return id + " @" + System.identityHashCode(o)+ " '"+ o.getName() + "' " + (debug ?  Thread.currentThread().getName() : "");         
    }

    private String getInfo(TestElement o) {
        return whoAmI(o.getClass().getSimpleName(), o); 
    }

    private void cancelTask() {
        if (future != null) {
            if (!future.isDone()) {
                boolean cancelled = future.cancel(false);
                if (debug) {
                    LOG.debug("Cancelled the task: @" + System.identityHashCode(future) + " with result " + cancelled);
                }
            }
            future = null;
        }        
    }
}
