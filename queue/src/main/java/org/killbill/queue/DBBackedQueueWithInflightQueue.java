/*
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.killbill.CreatorName;
import org.killbill.bus.dao.PersistentBusSqlDao;
import org.killbill.clock.Clock;
import org.killbill.commons.jdbi.notification.DatabaseTransactionEvent;
import org.killbill.commons.jdbi.notification.DatabaseTransactionEventType;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.queue.api.PersistentQueueConfig;
import org.killbill.queue.api.PersistentQueueEntryLifecycleState;
import org.killbill.queue.dao.EventEntryModelDao;
import org.killbill.queue.dao.QueueSqlDao;
import org.skife.jdbi.v2.IDBI;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

public class DBBackedQueueWithInflightQueue<T extends EventEntryModelDao> extends DBBackedQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(DBBackedQueueWithInflightQueue.class);

    // How many recordIds we pull per iteration during init to fill the inflightQ 每次从数据库获取的记录数量
    private static final int MAX_FETCHED_RECORDS_ID = 1000;

    // Drain inflightQ using getMaxInFlightEntries() config at a time and sleep for a maximum of 100 mSec if there is nothing to do
    // 如果 inflightEvents 中没有event，最多再等待100毫秒
    private static final long INFLIGHT_POLLING_TIMEOUT_MSEC = 100;
    // 队列
    private final LinkedBlockingQueue<Long> inflightEvents;

    private final DatabaseTransactionNotificationApi databaseTransactionNotificationApi;

    //
    // Per thread information to keep track or recordId while it is accessible and right before
    // transaction gets committed/rollback
    //
    private static final AtomicInteger QUEUE_ID_CNT = new AtomicInteger(0);
    private final int queueId;
    // 缓存队列，在数据写入到数据库的时候，会将id存放到这里，事务提交的时候，将id转移到 inflightEvents
    private final TransientInflightQRowIdCache transientInflightQRowIdCache;

    public DBBackedQueueWithInflightQueue(final Clock clock,
                                          final IDBI dbi,
                                          final Class<? extends QueueSqlDao<T>> sqlDaoClass,
                                          final PersistentQueueConfig config,
                                          final String dbBackedQId,
                                          final MetricRegistry metricRegistry,
                                          final DatabaseTransactionNotificationApi databaseTransactionNotificationApi) {
        super(clock, dbi, sqlDaoClass, config, dbBackedQId, metricRegistry);

        Preconditions.checkArgument(config.getMinInFlightEntries() <= config.getMaxInFlightEntries());

        this.queueId = QUEUE_ID_CNT.incrementAndGet();
        // We use an unboundedQ - the risk of running OUtOfMemory exists for a very large number of entries showing a more systematic problem...
        this.inflightEvents = new LinkedBlockingQueue<Long>();

        this.databaseTransactionNotificationApi = databaseTransactionNotificationApi;
        // 事务提交的时候通知
        databaseTransactionNotificationApi.registerForNotification(this);

        // Metrics the size of the inflightQ
        metricRegistry.register(MetricRegistry.name(DBBackedQueueWithInflightQueue.class, dbBackedQId, "inflightQ", "size"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return inflightEvents.size();
            }
        });

        this.transientInflightQRowIdCache = new TransientInflightQRowIdCache(queueId);
    }

    @Override
    public void initialize() {

        initializeInflightQueue();
        log.info("{} Initialized with queueId={}, mode={}",
                 DB_QUEUE_LOG_ID, queueId, config.getPersistentQueueMode());
    }

    @Override
    public void close() {
        databaseTransactionNotificationApi.unregisterForNotification(this);
    }


    @Override
    public void insertEntryFromTransaction(final QueueSqlDao<T> transactional, final T entry) {
        final Long lastInsertId = safeInsertEntry(transactional, entry);
        if (lastInsertId == 0) {
            log.warn("{} Failed to insert entry, lastInsertedId={}", DB_QUEUE_LOG_ID, lastInsertId);
            return;
        }

        // The current thread is in the middle of  a transaction and this is the only times it knows about the recordId for the queue event;
        // It keeps track of it as a per thread data. Very soon, when the transaction gets committed/rolled back it can then extract the info
        // and insert the recordId into a blockingQ that is highly optimized to dispatch events.
        // 先存到缓存中，当事务提交的时候，立即转移到 inflightEvents
        transientInflightQRowIdCache.addRowId(lastInsertId);
    }

    /**
     * 从inflightEvents 取值 并存到 result中
     * @param result
     * @return
     */
    private long pollEntriesFromInflightQ(final List<Long> result) {

        long pollSleepTime = 0;
        inflightEvents.drainTo(result, config.getMaxInFlightEntries());
        if (result.isEmpty()) {
            try {
                long beforePollTime = System.nanoTime();
                // We block until we see the first entry or reach the timeout (in which case we will rerun the doDispatchEvents() loop and come back here).
                final Long entryId = inflightEvents.poll(INFLIGHT_POLLING_TIMEOUT_MSEC, TimeUnit.MILLISECONDS);
                // Maybe there was at least one entry and we did not sleep at all, in which case this time is close to 0.
                pollSleepTime = System.nanoTime() - beforePollTime;
                if (entryId != null) {
                    result.add(entryId);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("{} Got interrupted", DB_QUEUE_LOG_ID);
                return 0;
            }
        }
        return pollSleepTime;
    }

    /**
     *  从 inflightEvents 中获取 获取特定数量的event_id,并从数据库中查询完整信息
     *
     * @return
     */
    @Override
    public ReadyEntriesWithMetrics<T> getReadyEntries() {

        final long ini = System.nanoTime();
        long pollSleepTime = 0;

        // 存放 inflightEvents 中的数据
        final List<Long> recordIds = new ArrayList<Long>(config.getMaxInFlightEntries());
        do {
            pollSleepTime += pollEntriesFromInflightQ(recordIds);
        } while (recordIds.size() < config.getMinInFlightEntries() && pollSleepTime < INFLIGHT_POLLING_TIMEOUT_MSEC);


        List<T> entries = ImmutableList.<T>of();
        if (!recordIds.isEmpty()) {
            log.debug("{} fetchReadyEntriesFromIds: {}", DB_QUEUE_LOG_ID, recordIds);
            // 通过ID从bus_events中查找数据
            entries = executeQuery(new Query<List<T>, QueueSqlDao<T>>() {
                @Override
                public List<T> execute(final QueueSqlDao<T> queueSqlDao) {

                    long ini = System.nanoTime();
                    final List<T> result = queueSqlDao.getEntriesFromIds(recordIds, config.getTableName());
                    rawGetEntriesTime.update(System.nanoTime() - ini, TimeUnit.NANOSECONDS);
                    return result;
                }
            });
        }
        return new ReadyEntriesWithMetrics<T>(entries, (System.nanoTime() - ini) - pollSleepTime);

    }

    /**
     * 更新错误次数，并重试
     * @param entry
     */
    @Override
    public void updateOnError(final T entry) {
        executeTransaction(new Transaction<Void, QueueSqlDao<T>>() {
            @Override
            public Void inTransaction(final QueueSqlDao<T> transactional, final TransactionStatus status) throws Exception {
                transactional.updateOnError(entry.getRecordId(), clock.getUTCNow().toDate(), entry.getErrorCount(), config.getTableName());
                transientInflightQRowIdCache.addRowId(entry.getRecordId());
                return null;
            }
        });
    }

    @Override
    protected void insertReapedEntriesFromTransaction(final QueueSqlDao<T> transactional, final List<T> entriesLeftBehind, final DateTime now) {
        for (final T entry : entriesLeftBehind) {
            entry.setCreatedDate(now);
            entry.setProcessingState(PersistentQueueEntryLifecycleState.AVAILABLE);
            entry.setCreatingOwner(CreatorName.get());
            entry.setProcessingOwner(null);
            insertEntryFromTransaction(transactional, entry);
        }
    }

    /**
     * 将threadlocal 中缓存的event转移到 inflightEvents
     * @param event
     */
    @AllowConcurrentEvents
    @Subscribe
    public void handleDatabaseTransactionEvent(final DatabaseTransactionEvent event) {
        // Either a transaction we are not interested in, or for the wrong queue; just return.
        if (transientInflightQRowIdCache == null || !transientInflightQRowIdCache.isValid()) {
            return;
        }

        // This is a ROLLBACK, clear the threadLocal and return
        if (event.getType() == DatabaseTransactionEventType.ROLLBACK) {
            transientInflightQRowIdCache.reset();
            return;
        }

        try {
            // Add entry in the inflightQ and clear threadlocal
            final Iterator<Long> entries = transientInflightQRowIdCache.iterator();
            while (entries.hasNext()) {
                final Long entry = entries.next();
                // 将threadlocal 中缓存的event转移到 inflightEvents
                final boolean result = inflightEvents.offer(entry);
                if (result) {
                    log.debug("{} Inserting entry {} into inflightQ", DB_QUEUE_LOG_ID, entry);
                } else {
                    log.warn("{} Inflight Q overflowed....", DB_QUEUE_LOG_ID, entry);
                }
            }
        } finally {
            transientInflightQRowIdCache.reset();
        }
    }

    @VisibleForTesting
    public int getInflightQSize() {
        return inflightEvents.size();
    }

    //
    // Hide the ThreadLocal logic required for inflightQ algorithm in that class and export an easy to use interface.
    //
    private static class TransientInflightQRowIdCache {

        private final ThreadLocal<RowRef> rowRefThreadLocal = new ThreadLocal<RowRef>();
        private final int queueId;

        private TransientInflightQRowIdCache(final int queueId) {
            this.queueId = queueId;
        }

        public boolean isValid() {
            final RowRef entry = rowRefThreadLocal.get();
            return (entry != null && entry.queueId == queueId);
        }

        public void addRowId(final Long rowId) {
            RowRef entry = rowRefThreadLocal.get();
            if (entry == null) {
                entry = new RowRef(queueId);
                rowRefThreadLocal.set(entry);
            }
            entry.addRowId(rowId);
        }

        public void reset() {
            rowRefThreadLocal.remove();
        }

        public Iterator<Long> iterator() {
            final RowRef entry = rowRefThreadLocal.get();
            Preconditions.checkNotNull(entry);
            return entry.iterator();
        }

        // Internal structure to keep track of recordId per queue
        private final class RowRef {

            private final int queueId;
            private final List<Long> rowIds;

            public RowRef(final int queueId) {
                this.queueId = queueId;
                this.rowIds = new ArrayList<Long>();
            }

            public void addRowId(final long rowId) {
                rowIds.add(rowId);
            }

            public Iterator<Long> iterator() {
                return rowIds.iterator();
            }
        }
    }

    /**
     * 从数据库中加载特定数量的数据到内存中
     */
    private void initializeInflightQueue() {

        inflightEvents.clear();

        int totalEntries = 0;
        long fromRecordId = -1;
        do {
            // 查找数据库中已经准备好的数据
            final List<Long> existingIds = ((PersistentBusSqlDao) sqlDao).getReadyEntryIds(clock.getUTCNow().toDate(), fromRecordId, MAX_FETCHED_RECORDS_ID, CreatorName.get(), config.getTableName());
            if (existingIds.isEmpty()) {
                break;
            }

            inflightEvents.addAll(existingIds);
            totalEntries += existingIds.size();
            if (existingIds.size() < MAX_FETCHED_RECORDS_ID) {
                break;
            }
            fromRecordId = existingIds.get(existingIds.size() - 1) + 1;
        } while (true);

        log.info("{} Inserting {} entries into inflightQ during initialization",
                 DB_QUEUE_LOG_ID, totalEntries);

    }

}
