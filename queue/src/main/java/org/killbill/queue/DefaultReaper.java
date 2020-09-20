/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.queue;

import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.killbill.clock.Clock;
import org.killbill.commons.concurrent.Executors;
import org.killbill.queue.api.PersistentQueueConfig;
import org.killbill.queue.api.Reaper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DefaultReaper implements Reaper {

    static final long ONE_MINUTES_IN_MSEC = 60000;
    static final long FIVE_MINUTES_IN_MSEC = 5 * ONE_MINUTES_IN_MSEC;

    private final DBBackedQueue<?> dao;
    private final PersistentQueueConfig config;
    private final Clock clock;
    private final AtomicBoolean isStarted;
    private final String threadScheduledExecutorName;

    private ScheduledFuture<?> reapEntriesHandle;

    private static final Logger log = LoggerFactory.getLogger(DefaultReaper.class);
    // 定时执行器服务
    private ScheduledExecutorService scheduler;

    public DefaultReaper(final DBBackedQueue<?> dao, final PersistentQueueConfig config, final Clock clock, final String threadScheduledExecutorName) {
        this.dao = dao;
        this.config = config;
        this.clock = clock;
        this.isStarted = new AtomicBoolean(false);
        this.threadScheduledExecutorName = threadScheduledExecutorName;
    }

    @Override
    public void start() {
        if (!isStarted.compareAndSet(false, true)) {
            return;
        }

        final long reapThresholdMillis = getReapThreshold();
        final long schedulePeriodMillis = config.getReapSchedule().getMillis();

        log.info("{}: Starting... reapThresholdMillis={}, schedulePeriodMillis={}",
                 threadScheduledExecutorName, reapThresholdMillis, schedulePeriodMillis);

        final Runnable reapEntries = new Runnable() {
            @Override
            public void run() {
                dao.reapEntries(getReapingDate());
            }

            private Date getReapingDate() {
                return clock.getUTCNow().minusMillis((int) reapThresholdMillis).toDate();
            }
        };

        scheduler = Executors.newSingleThreadScheduledExecutor(threadScheduledExecutorName);
        reapEntriesHandle = scheduler.scheduleWithFixedDelay(reapEntries, schedulePeriodMillis, schedulePeriodMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        if (!isStarted.compareAndSet(true, false)) {
            return;
        }

        log.info("{}: Shutdown...", threadScheduledExecutorName);
        if (!reapEntriesHandle.isCancelled() || !reapEntriesHandle.isDone()) {
            reapEntriesHandle.cancel(true);
        }

        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                log.info("{} stop sequence has been interrupted",threadScheduledExecutorName);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted.get();
    }

    long getReapThreshold() {
        final long threshold;
        // if Claim time is greater than reap threshold
        if (config.getClaimedTime().getMillis() >= config.getReapThreshold().getMillis()) {
            // override reap threshold using claim time + 5 minutes
            threshold = config.getClaimedTime().getMillis() + FIVE_MINUTES_IN_MSEC;
            log.warn("{}: Reap threshold was mis-configured. Claim time [{}] is greater than reap threshold [{}]",
                                   threadScheduledExecutorName, config.getClaimedTime().toString(), config.getReapThreshold().toString());

        } else {
            threshold = config.getReapThreshold().getMillis();
        }

        return threshold;
    }
}
