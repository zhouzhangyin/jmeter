/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package org.apache.jmeter.timers;

import java.io.Serializable;
import java.util.concurrent.Callable;
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

/**
 * 
 * Sample timeout implementation using Executor threads
 *
 */
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
    
    private final transient ScheduledExecutorService execService;
    
    private final boolean debug;

    /**
     * No-arg constructor.
     */
    public InterruptTimer() {
//        LOG.setPriority(org.apache.log.Priority.DEBUG); // for local debugging when enabled
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
        
        if ("CALL".equals(getType())) {
        Callable<Object> call = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                long start = System.nanoTime();
                boolean interrupted = sampler.interrupt();
                String elapsed = Double.toString((double)(System.nanoTime()-start)/ 1000000000)+" secs";
                if (interrupted) {
                    LOG.warn("Call Done interrupting " + getInfo(samp) + " took " + elapsed);
                } else {
                    if (debug) {
                        LOG.debug("Call Didn't interrupt: " + getInfo(samp) + " took " + elapsed);
                    }
                }
                final long delay = Long.parseLong(getCallDelay());
                if (delay > 0) {
                    LOG.debug("Call will wait " + delay);
                    Thread.sleep(delay);
                }
                return null;
            }
            
        };
        // schedule the interrupt to occur and save for possible cancellation 
        future = execService.schedule(call, timeout, TimeUnit.MILLISECONDS);
        } else {
        Runnable run=new Runnable() {
            public void run() {
                long start = System.nanoTime();
                boolean interrupted = sampler.interrupt();
                String elapsed = Double.toString((double)(System.nanoTime()-start)/ 1000000000)+" secs";
                if (interrupted) {
                    LOG.warn("Run Done interrupting " + getInfo(samp) + " took " + elapsed);
                } else {
                    if (debug) {
                        LOG.debug("Run Didn't interrupt: " + getInfo(samp) + " took " + elapsed);
                    }
                }
            }
        };
            // schedule the interrupt to occur and save for possible cancellation 
            future = execService.schedule(run, timeout, TimeUnit.MILLISECONDS);
        }
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
                    LOG.debug("Cancelled timer: @" + System.identityHashCode(future) + " with result " + cancelled);
                }
            }
            future = null;
        }        
    }

    public void setType(String text) {
        setProperty("type", text);
    }

    public void setCallDelay(String text) {
        setProperty("calldelay", text, "0");
    }

    public String getType() {
        return getPropertyAsString("type");
    }

    public String getCallDelay() {
        return getPropertyAsString("calldelay","0");
    }
}
