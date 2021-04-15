/*
 * Copyright 2014-2016 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company. If there are no 
 * contracts in place use of this source is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless 
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported by Solace 
 * unless such support is provided for under an agreement 
 * signed between you and Solace.
 */

package com.solace.psg.enterprisestats.statspump;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solace.psg.enterprisestats.statspump.PollerRunnable.PollerReturnCode;
import com.solace.psg.enterprisestats.statspump.stats.PollerStat;
import com.solace.psg.enterprisestats.statspump.stats.PollerStats;
import com.solace.psg.enterprisestats.statspump.stats.PollerStat.Stat;
import com.solace.psg.enterprisestats.statspump.util.StatsPumpConstants;

public class ScheduledPollerHandle {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledPollerHandle.class);

    /**
     * This inner class is used only to ensure the Poller finishes running in a
     * timely time, and if not (3x or 10x longer than interval, it will assume
     * it is dead and cancel it, reset, and restart)
     */
    static class PollerWatcherTask implements Callable<PollerReturnCode> {

        private final PollerRunnable pollerRunnable;
        // handle to the (one-time) poller runnable that is executing, this is
        // what the Watcher instance is watching
        private final Future<PollerReturnCode> pollerResult;

        private PollerWatcherTask(PollerRunnable pollerRunnable, Future<PollerReturnCode> pollerResult) {
            this.pollerRunnable = pollerRunnable;
            this.pollerResult = pollerResult;
        }

        @Override
        public PollerReturnCode call() {
            long startTimeNano = System.nanoTime();
            try {
                // logger.debug("Calling get() on "+pollerRunnable);
                PollerReturnCode result = pollerResult.get(
                        (long) (pollerRunnable.getIntervalMs() * StatsPumpConstants.MAX_POLLER_RUN_LIMIT_FACTOR),
                        TimeUnit.MILLISECONDS);
                long timeMs = (int) ((System.nanoTime() - startTimeNano) / 1000000f);
                // if you've made it here, it means get() returned... Poller
                // could still have failed though
                switch (result) {
                case SUCCESS:
                    pollerRunnable.getAppliance().resetMissedPoll();
                    // logger.debug("get() is done on "+pollerRunnable+" and
                    // returned in "+timeMs+" ms");
                    if (timeMs > pollerRunnable.getIntervalMs()) {
                        logger.info(String.format(
                                "*Warning* %s took %.1f sec to run, but is supposed to run every %.1f sec",
                                pollerRunnable, (timeMs) / 1000f, pollerRunnable.getIntervalMs() / 1000f));
                    }
                    break;
                case FAILURE:
                    pollerRunnable.getAppliance().incMissedPoll();
                    break;
                case NOT_RUNNING:
                default:
                    break; // don't do anything
                }
                return result;
            } catch (InterruptedException e) {
                long timeMs = (int) ((System.nanoTime() - startTimeNano) / 1000000f);
                logger.info("My PollerWatcher has been interrupted.  Cancelling pollerResult after " + timeMs + " ms.");
                pollerResult.cancel(true);
                pollerRunnable.getAppliance().incMissedPoll();
                return PollerReturnCode.FAILURE;
            } catch (ExecutionException e) {
                // unlikely as the PollerRunnable will catch his own errors and
                // deal with
                logger.error(String.format("Caught this while waiting for %s", pollerRunnable), e);
                pollerRunnable.getAppliance().incMissedPoll();
                return PollerReturnCode.FAILURE;
            } catch (TimeoutException e) {
                long timeMs = (int) ((System.nanoTime() - startTimeNano) / 1000000f);
                logger.info(String.format(
                        "Poller %s did not complete after %.1f seconds (supposed to have a period of %.1f seconds). Terminating the Poller.",
                        pollerRunnable, timeMs / 1000f, pollerRunnable.getIntervalMs() / 1000f));
                logger.debug("Attempting to cancel() pollerResult");
                pollerResult.cancel(true);
                // might need to check some counters here
                pollerRunnable.getAppliance().incMissedPoll();
                return PollerReturnCode.FAILURE;
            } finally {
                // logger.info("Inside finally of the WatcherRunnable");
            }
        }
    };

    /**
     * This is the guy that runs periodically. Basically, all he does is submits
     * the PollerRunnable to be executed once; grabs the Future from that, and
     * then creates a Watcher object to keep an eye to make sure it gets
     * completed in a timely manner.
     */
    static class ScheduledPollerTask implements Runnable {

        private final PollerRunnable pollerRunnable;
        @SuppressWarnings("unused")
        private final long initialDelayMs;
        private volatile Future<?> watcherResult = null;

        public ScheduledPollerTask(final PollerRunnable pollerRunnable, final long initialDelayMs) {
            this.pollerRunnable = pollerRunnable;
            this.initialDelayMs = initialDelayMs; // maybe in case we want to
                                                  // calculate when it's
                                                  // supposed to run..?
        }

        boolean isRunning() {
            return watcherResult != null && !watcherResult.isDone();
        }

        @Override
        public void run() { // this is the run() that gets executed every
                            // periodMs*1000 seconds
            try {
                // it is always a scheduling thread that will be running this
                // code to check
                if (isRunning()) {
                    logger.info("****  " + pollerRunnable + " is still running. not continuing");
                    // logger.info("inc missed poller count for
                    // "+pollerRunnable);
                    // also probably send a stat msg equivalent to a noop
                    PollerStat.Builder builder = new PollerStat.Builder();
                    builder.addStat(Stat.POLLER_MISS, 1);
                    PollerStats.INSTANCE.addStat(pollerRunnable.getPoller(), pollerRunnable.getAppliance(),
                            builder.build());
                    return;
                }
                if (!pollerRunnable.getAppliance().isReachable()) {
                    // logger.debug(String.format("Not running %s: appliance is
                    // down",pollerRunnable));
                    return;
                }
                // else, the PollerRunnable is not running, so submit it to the
                // executor to execute, and watch it it
                Future<PollerReturnCode> pollerResult = PumpManager.getInstance().pollerExecutor.submit(pollerRunnable);
                PollerWatcherTask watcher = new PollerWatcherTask(pollerRunnable, pollerResult);
                // this needs to be an instance variable so I can cancel it
                watcherResult = PumpManager.getInstance().pollerExecutor.submit(watcher);
            } catch (RuntimeException e) {
                // we really shouldn't be having any exceptions come out of
                // this.
                logger.error("Managed to generate a RuntimeException", e);
                PollerStat.Builder builder = new PollerStat.Builder();
                builder.addStat(Stat.POLLER_ERROR, 1);
                PollerStats.INSTANCE.addStat(pollerRunnable.getPoller(), pollerRunnable.getAppliance(),
                        builder.build());
            } catch (Throwable e) {
                logger.error("************** Have encountered a serious error! ****************", e);
                PollerStat.Builder builder = new PollerStat.Builder();
                builder.addStat(Stat.POLLER_ERROR, 1);
                PollerStats.INSTANCE.addStat(pollerRunnable.getPoller(), pollerRunnable.getAppliance(),
                        builder.build());
            }
        }

        /**
         * This custom cancel() method will cancel the watcher thread, which in
         * turn will attempt to cancel the PollerRunnable if it is executing.
         */
        public void cancel() {
            if (watcherResult != null)
                watcherResult.cancel(true); // the watcher thread will cancel
                                            // the poller runnable
        }
    }

    /* MAIN CLASS ****************************************************/

    final ScheduledPollerTask scheduledPollerTask; // the Runnable that gets run
                                                   // every (say) 10 seconds
    final ScheduledFuture<?> scheduledPollerHandle; // the Future holding that
                                                    // periodic execution, so I
                                                    // can cancel

    private ScheduledPollerHandle(final PollerRunnable pollerCallable, final long initialDelayMs) {
        scheduledPollerTask = new ScheduledPollerTask(pollerCallable, initialDelayMs);
        scheduledPollerHandle = PumpManager.getInstance().scheduleExecutor.scheduleAtFixedRate(scheduledPollerTask,
                initialDelayMs, pollerCallable.getIntervalMs(), TimeUnit.MILLISECONDS);
    }

    public static ScheduledPollerHandle schedulePoller(final PollerRunnable pollerCallable, final long initialDelayMs) {
        return new ScheduledPollerHandle(pollerCallable, initialDelayMs);
    }

    /**
     * Convenience method that return 'true' if the execution result of this
     * poller is SUCCESS. Note that this method will run against an appliance
     * even if that appliance is deemed unreachable.
     */
    public static boolean oneTimePoller(final PollerRunnable pollerRunnable) {
        Future<PollerReturnCode> pollerResult = PumpManager.getInstance().pollerExecutor.submit(pollerRunnable);
        PollerWatcherTask watcher = new PollerWatcherTask(pollerRunnable, pollerResult);
        return watcher.call() == PollerReturnCode.SUCCESS;
    }

    /**
     * Stops the periodic execution of this Poller, as well as an ongoing one,
     * if possible
     */
    public void cancel() {
        scheduledPollerTask.cancel(); // custom cancel() method to call the
                                      // watcher's cancel()
        scheduledPollerHandle.cancel(true);
    }

}
