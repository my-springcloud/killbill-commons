/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.bus;

import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.killbill.CreatorName;
import org.killbill.bus.api.BusEvent;
import org.killbill.bus.api.BusEventWithMetadata;
import org.killbill.bus.api.PersistentBus;
import org.killbill.bus.api.PersistentBusConfig;
import org.killbill.bus.dao.BusEventModelDao;
import org.killbill.bus.dao.PersistentBusSqlDao;
import org.killbill.bus.dispatching.BusCallableCallback;
import org.killbill.clock.Clock;
import org.killbill.clock.DefaultClock;
import org.killbill.commons.jdbi.notification.DatabaseTransactionNotificationApi;
import org.killbill.commons.profiling.Profiling;
import org.killbill.commons.profiling.ProfilingFeature;
import org.killbill.queue.DBBackedQueue;
import org.killbill.queue.DBBackedQueue.ReadyEntriesWithMetrics;
import org.killbill.queue.DBBackedQueueWithInflightQueue;
import org.killbill.queue.DBBackedQueueWithPolling;
import org.killbill.queue.DefaultQueueLifecycle;
import org.killbill.queue.InTransaction;
import org.killbill.queue.api.PersistentQueueConfig.PersistentQueueMode;
import org.killbill.queue.api.QueueEvent;
import org.killbill.queue.dao.EventEntryModelDao;
import org.killbill.queue.dispatching.BlockingRejectionExecutionHandler;
import org.killbill.queue.dispatching.CallableCallbackBase;
import org.killbill.queue.dispatching.Dispatcher;
import org.skife.config.ConfigurationObjectFactory;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.eventbus.EventBusThatThrowsException;

/**
 * 职责：
 * 1、注册事件监听器
 * 2、发布事件到数据库队列中（post()）
 * 3、从数据库中获取事件派发给订阅端 (dispatch)
 * 4、异常处理
 * 5、事件挖掘
 * 6、数据库队列管理
 */
public class DefaultPersistentBus extends DefaultQueueLifecycle implements PersistentBus {

    private static final Logger log = LoggerFactory.getLogger(DefaultPersistentBus.class);

    private final DBI dbi;
    // 事件总线
    private final EventBusThatThrowsException eventBusDelegate;
    // 基于数据库的队列
    private final DBBackedQueue<BusEventModelDao> dao;
    private final Clock clock;
    private final PersistentBusConfig config;
    private final Profiling<Iterable<BusEventModelDao>, RuntimeException> prof;
    // 挖掘
    private final BusReaper reaper;
    // 派发
    private final Dispatcher<BusEvent, BusEventModelDao> dispatcher;

    // Time it takes to handle the bus request (going through multiple handles potentially)
    private final Timer busHandlersProcessingTime;

    private final AtomicBoolean isInitialized;
    private final AtomicBoolean isStarted;
    private final String dbBackedQId;
    // 从数据库中获取到event后会调用busCallableCallback#dispatch()方法来派发消息给订阅端
    private final BusCallableCallback busCallableCallback;

    private static final class EventBusDelegate extends EventBusThatThrowsException {

        public EventBusDelegate(final String busName) {
            super(busName);
        }
    }

    @Inject
    public DefaultPersistentBus(@Named(QUEUE_NAME) final IDBI dbi, final Clock clock, final PersistentBusConfig config, final MetricRegistry metricRegistry, final DatabaseTransactionNotificationApi databaseTransactionNotificationApi) {
        super(config.getTableName(), config, metricRegistry);
        this.dbi = (DBI) dbi;
        this.clock = clock;
        this.config = config;
        this.dbBackedQId = config.getTableName();
        // 队列模式
        this.dao = config.getPersistentQueueMode() == PersistentQueueMode.STICKY_EVENTS ?
                   new DBBackedQueueWithInflightQueue<BusEventModelDao>(clock, dbi, PersistentBusSqlDao.class, config, dbBackedQId, metricRegistry, databaseTransactionNotificationApi) :
                   new DBBackedQueueWithPolling<BusEventModelDao>(clock, dbi, PersistentBusSqlDao.class, config, dbBackedQId, metricRegistry);

        this.prof = new Profiling<Iterable<BusEventModelDao>, RuntimeException>();
        final ThreadFactory busThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                return new Thread(new ThreadGroup(EVENT_BUS_GROUP_NAME),
                                  r,
                                  config.getTableName() + "-th");
            }
        };

        this.busHandlersProcessingTime = metricRegistry.timer(MetricRegistry.name(DefaultPersistentBus.class, dbBackedQId, "busHandlersProcessingTime"));

        this.eventBusDelegate = new EventBusDelegate("Killbill EventBus");
        this.isInitialized = new AtomicBoolean(false);
        this.isStarted = new AtomicBoolean(false);
        // 挖掘机
        this.reaper = new BusReaper(this.dao, config, clock);
        // 从数据库获取到事件后的派发逻辑
        this.busCallableCallback = new BusCallableCallback(this);
        // 派发器，最终委派给busCallableCallback，而busCallableCallback又会调用 dispatchBusEventWithMetrics
        this.dispatcher = new Dispatcher<>(1, config, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(config.getEventQueueCapacity()), busThreadFactory, new BlockingRejectionExecutionHandler(),
                                           clock, busCallableCallback, this);

    }

    public DefaultPersistentBus(final DataSource dataSource, final Properties properties) {
        this(InTransaction.buildDDBI(dataSource), new DefaultClock(), new ConfigurationObjectFactory(properties).buildWithReplacements(PersistentBusConfig.class, ImmutableMap.<String, String>of("instanceName", "main")),
             new MetricRegistry(), new DatabaseTransactionNotificationApi());
    }


    @Override
    public boolean initQueue() {
        if (config.isProcessingOff()) {
            log.warn("PersistentBus processing is off, cannot be initialized");
            return false;
        }

        if (isInitialized.compareAndSet(false, true)) {
            dao.initialize();
            dispatcher.start();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean startQueue() {

        if (config.isProcessingOff()) {
            log.warn("PersistentBus processing is off, cannot be started");
            return false;
        }

        if (!isInitialized.get()) {
            // Make it easy for our tests, so they simply call startQueue
            initQueue();
        }

        if (isStarted.compareAndSet(false, true)) {
            // 和“当前节点”相关的持久化模式才会启用挖掘机
            if (config.getPersistentQueueMode() == PersistentQueueMode.STICKY_POLLING || config.getPersistentQueueMode() == PersistentQueueMode.STICKY_EVENTS) {
                // 启用挖掘机
                reaper.start();
            }
            super.startQueue();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void stopQueue() {
        if (isStarted.compareAndSet(true, false)) {
            isInitialized.set(false);
            reaper.stop();
            super.stopQueue();
            dispatcher.stop();
            dao.close();
        }
    }
    // 从数据库获取事件，并派发给订阅端的逻辑
    @Override
    public DispatchResultMetrics doDispatchEvents() {
        // Step 1、 获取数据库事件
        final ReadyEntriesWithMetrics<BusEventModelDao> eventsWithMetrics = dao.getReadyEntries();
        final List<BusEventModelDao> events = eventsWithMetrics.getEntries();
        if (events.isEmpty()) {
            return new DispatchResultMetrics(0, eventsWithMetrics.getTime());
        }
        log.debug("Bus events from {} to process: {}", config.getTableName(), events);

        long ini = System.nanoTime();
        for (final BusEventModelDao cur : events) {
            // Step 2、 派发事件给订阅端
            dispatcher.dispatch(cur);
        }
        return new DispatchResultMetrics(events.size(), (System.nanoTime() - ini) + eventsWithMetrics.getTime());
    }

    /**
     * 将事件移到历史表中
     * @param completed
     */
    @Override
    public void doProcessCompletedEvents(final Iterable<? extends EventEntryModelDao> completed) {
        busCallableCallback.moveCompletedOrFailedEvents((Iterable<BusEventModelDao>) completed);
    }

    /**
     * 1、更新error_count
     * 2、将record_id再次添加到本地线程缓存
     * @param retried
     */
    @Override
    public void doProcessRetriedEvents(final Iterable<? extends EventEntryModelDao> retried) {
        Iterator<? extends EventEntryModelDao> it = retried.iterator();
        while (it.hasNext()) {
            BusEventModelDao cur = (BusEventModelDao) it.next();
            busCallableCallback.updateRetriedEvents(cur);
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted.get();
    }

    /**
     * 委托给 eventBusDelegate
     *
     * @param handlerInstance handler to register
     * @throws EventBusException
     */
    @Override
    public void register(final Object handlerInstance) throws EventBusException {
        if (isInitialized.get()) {
            eventBusDelegate.register(handlerInstance);
        } else {
            log.warn("Attempting to register handler " + handlerInstance + " in a non initialized bus");
        }
    }

    @Override
    public void unregister(final Object handlerInstance) throws EventBusException {
        if (isInitialized.get()) {
            eventBusDelegate.unregister(handlerInstance);
        } else {
            log.warn("Attempting to unregister handler " + handlerInstance + " in a non initialized bus");
        }
    }

    /**
     * 发布消息到队列
     * @param event to be posted
     * @throws EventBusException
     */
    @Override
    public void post(final BusEvent event) throws EventBusException {
        try {
            if (isInitialized.get()) {
                final String json = objectMapper.writeValueAsString(event);
                final BusEventModelDao entry = new BusEventModelDao(CreatorName.get(), clock.getUTCNow(), event.getClass().getName(), json,
                                                                    event.getUserToken(), event.getSearchKey1(), event.getSearchKey2());
                // 写数据库，并存到缓存中
                dao.insertEntry(entry);

            } else {
                log.warn("Attempting to post event " + event + " in a non initialized bus");
            }
        } catch (final Exception e) {
            log.error("Failed to post BusEvent " + event, e);
        }
    }

    @Override
    public void postFromTransaction(final BusEvent event, final Connection connection) throws EventBusException {
        if (!isInitialized.get()) {
            log.warn("Attempting to post event " + event + " in a non initialized bus");
            return;
        }

        final String json;
        try {
            json = objectMapper.writeValueAsString(event);
        } catch (final JsonProcessingException e) {
            log.warn("Unable to serialize event " + event, e);
            return;
        }

        final BusEventModelDao entry = new BusEventModelDao(CreatorName.get(),
                                                            clock.getUTCNow(),
                                                            event.getClass().getName(),
                                                            json,
                                                            event.getUserToken(),
                                                            event.getSearchKey1(),
                                                            event.getSearchKey2());

        final InTransaction.InTransactionHandler<PersistentBusSqlDao, Void> handler = new InTransaction.InTransactionHandler<PersistentBusSqlDao, Void>() {

            @Override
            public Void withSqlDao(final PersistentBusSqlDao transactional) {
                dao.insertEntryFromTransaction(transactional, entry);
                return null;
            }
        };

        InTransaction.execute(dbi, connection, handler, PersistentBusSqlDao.class);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableBusEventsForSearchKeys(final Long searchKey1, final Long searchKey2) {
        return getAvailableBusEventsForSearchKeysInternal((PersistentBusSqlDao) dao.getSqlDao(), null, searchKey1, searchKey2);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableBusEventsFromTransactionForSearchKeys(final Long searchKey1, final Long searchKey2, final Connection connection) {
        final InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>> handler = new InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>>() {
            @Override
            public Iterable<BusEventWithMetadata<T>> withSqlDao(final PersistentBusSqlDao transactional) {
                return getAvailableBusEventsForSearchKeysInternal(transactional, null, searchKey1, searchKey2);
            }
        };
        return InTransaction.execute(dbi, connection, handler, PersistentBusSqlDao.class);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableBusEventsForSearchKey2(final DateTime maxCreatedDate, final Long searchKey2) {
        return getAvailableBusEventsForSearchKeysInternal((PersistentBusSqlDao) dao.getSqlDao(), maxCreatedDate, null, searchKey2);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableBusEventsFromTransactionForSearchKey2(final DateTime maxCreatedDate, final Long searchKey2, final Connection connection) {
        final InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>> handler = new InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>>() {
            @Override
            public Iterable<BusEventWithMetadata<T>> withSqlDao(final PersistentBusSqlDao transactional) {
                return getAvailableBusEventsForSearchKeysInternal(transactional, maxCreatedDate, null, searchKey2);
            }
        };
        return InTransaction.execute(dbi, connection, handler, PersistentBusSqlDao.class);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getInProcessingBusEvents() {
        return toBusEventWithMetadata(dao.getSqlDao().getInProcessingEntries(config.getTableName()));
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableOrInProcessingBusEventsForSearchKeys(final Long searchKey1, final Long searchKey2) {
        return getAvailableOrInProcessingBusEventsForSearchKeysInternal((PersistentBusSqlDao) dao.getSqlDao(), null, searchKey1, searchKey2);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableOrInProcessingBusEventsFromTransactionForSearchKeys(final Long searchKey1, final Long searchKey2, final Connection connection) {
        final InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>> handler = new InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>>() {
            @Override
            public Iterable<BusEventWithMetadata<T>> withSqlDao(final PersistentBusSqlDao transactional) {
                return getAvailableOrInProcessingBusEventsForSearchKeysInternal(transactional, null, searchKey1, searchKey2);
            }
        };
        return InTransaction.execute(dbi, connection, handler, PersistentBusSqlDao.class);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableOrInProcessingBusEventsForSearchKey2(final DateTime maxCreatedDate, final Long searchKey2) {
        return getAvailableOrInProcessingBusEventsForSearchKeysInternal((PersistentBusSqlDao) dao.getSqlDao(), maxCreatedDate, null, searchKey2);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableOrInProcessingBusEventsFromTransactionForSearchKey2(final DateTime maxCreatedDate, final Long searchKey2, final Connection connection) {
        final InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>> handler = new InTransaction.InTransactionHandler<PersistentBusSqlDao, Iterable<BusEventWithMetadata<T>>>() {
            @Override
            public Iterable<BusEventWithMetadata<T>> withSqlDao(final PersistentBusSqlDao transactional) {
                return getAvailableOrInProcessingBusEventsForSearchKeysInternal(transactional, maxCreatedDate, null, searchKey2);
            }
        };
        return InTransaction.execute(dbi, connection, handler, PersistentBusSqlDao.class);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getHistoricalBusEventsForSearchKeys(final Long searchKey1, final Long searchKey2) {
        return getHistoricalBusEventsForSearchKeysInternal((PersistentBusSqlDao) dao.getSqlDao(), null, searchKey1, searchKey2);
    }

    @Override
    public <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getHistoricalBusEventsForSearchKey2(final DateTime minCreatedDate, final Long searchKey2) {
        return getHistoricalBusEventsForSearchKeysInternal((PersistentBusSqlDao) dao.getSqlDao(), minCreatedDate, null, searchKey2);
    }

    @Override
    public long getNbReadyEntries(final DateTime maxCreatedDate) {
        return dao.getNbReadyEntries(maxCreatedDate.toDate());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultPersistentBus{");
        sb.append("dbBackedQId='").append(dbBackedQId).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /**
     * 委派给guava eventBus
     * @param event
     * @throws com.google.common.eventbus.EventBusException
     */
    public void dispatchBusEventWithMetrics(final QueueEvent event) throws com.google.common.eventbus.EventBusException {
        final Timer.Context dispatchTimerContext = busHandlersProcessingTime.time();
        try {
            eventBusDelegate.postWithException(event);
        } finally {
            dispatchTimerContext.stop();
        }
    }

    private <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableBusEventsForSearchKeysInternal(final PersistentBusSqlDao transactionalDao, @Nullable final DateTime maxCreatedDate, @Nullable final Long searchKey1, final Long searchKey2) {
        final Iterable<BusEventModelDao> entries = getReadyQueueEntriesForSearchKeysWithProfiling(transactionalDao, maxCreatedDate, searchKey1, searchKey2);
        return toBusEventWithMetadata(entries);
    }

    private <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getAvailableOrInProcessingBusEventsForSearchKeysInternal(final PersistentBusSqlDao transactionalDao, @Nullable final DateTime maxCreatedDate, @Nullable final Long searchKey1, final Long searchKey2) {
        final Iterable<BusEventModelDao> entries = getReadyOrInProcessingQueueEntriesForSearchKeysWithProfiling(transactionalDao, maxCreatedDate, searchKey1, searchKey2);
        return toBusEventWithMetadata(entries);
    }

    private <T extends BusEvent> Iterable<BusEventWithMetadata<T>> getHistoricalBusEventsForSearchKeysInternal(final PersistentBusSqlDao transactionalDao, @Nullable final DateTime minCreatedDate, @Nullable final Long searchKey1, final Long searchKey2) {
        final Iterable<BusEventModelDao> entries = getHistoricalQueueEntriesForSearchKeysWithProfiling(transactionalDao, minCreatedDate, searchKey1, searchKey2);
        return toBusEventWithMetadata(entries);
    }

    private Iterable<BusEventModelDao> getReadyQueueEntriesForSearchKeysWithProfiling(final PersistentBusSqlDao transactionalDao, @Nullable final DateTime maxCreatedDate, @Nullable final Long searchKey1, final Long searchKey2) {
        return prof.executeWithProfiling(ProfilingFeature.ProfilingFeatureType.DAO, "DAO:PersistentBusSqlDao:getReadyQueueEntriesForSearchKeys", new Profiling.WithProfilingCallback<Iterable<BusEventModelDao>, RuntimeException>() {
            @Override
            public Iterable<BusEventModelDao> execute() throws RuntimeException {
                return new Iterable<BusEventModelDao>() {
                    @Override
                    public Iterator<BusEventModelDao> iterator() {
                        return searchKey1 != null ?
                               transactionalDao.getReadyQueueEntriesForSearchKeys(searchKey1, searchKey2, config.getTableName()) :
                               transactionalDao.getReadyQueueEntriesForSearchKey2(maxCreatedDate, searchKey2, config.getTableName());
                    }
                };
            }
        });
    }

    private Iterable<BusEventModelDao> getReadyOrInProcessingQueueEntriesForSearchKeysWithProfiling(final PersistentBusSqlDao transactionalDao, @Nullable final DateTime maxCreatedDate, @Nullable final Long searchKey1, final Long searchKey2) {
        return prof.executeWithProfiling(ProfilingFeature.ProfilingFeatureType.DAO, "DAO:PersistentBusSqlDao:getReadyOrInProcessingQueueEntriesForSearchKeys", new Profiling.WithProfilingCallback<Iterable<BusEventModelDao>, RuntimeException>() {
            @Override
            public Iterable<BusEventModelDao> execute() throws RuntimeException {
                return new Iterable<BusEventModelDao>() {
                    @Override
                    public Iterator<BusEventModelDao> iterator() {
                        return searchKey1 != null ?
                               transactionalDao.getReadyOrInProcessingQueueEntriesForSearchKeys(searchKey1, searchKey2, config.getTableName()) :
                               transactionalDao.getReadyOrInProcessingQueueEntriesForSearchKey2(maxCreatedDate, searchKey2, config.getTableName());
                    }
                };
            }
        });
    }

    private Iterable<BusEventModelDao> getHistoricalQueueEntriesForSearchKeysWithProfiling(final PersistentBusSqlDao transactionalDao, @Nullable final DateTime minCreatedDate, @Nullable final Long searchKey1, final Long searchKey2) {
        return prof.executeWithProfiling(ProfilingFeature.ProfilingFeatureType.DAO, "DAO:PersistentBusSqlDao:getHistoricalQueueEntriesForSearchKeys", new Profiling.WithProfilingCallback<Iterable<BusEventModelDao>, RuntimeException>() {
            @Override
            public Iterable<BusEventModelDao> execute() throws RuntimeException {
                return new Iterable<BusEventModelDao>() {
                    @Override
                    public Iterator<BusEventModelDao> iterator() {
                        return searchKey1 != null ?
                               transactionalDao.getHistoricalQueueEntriesForSearchKeys(searchKey1, searchKey2, config.getHistoryTableName()) :
                               transactionalDao.getHistoricalQueueEntriesForSearchKey2(minCreatedDate, searchKey2, config.getHistoryTableName());
                    }
                };
            }
        });
    }

    private <T extends BusEvent> Iterable<BusEventWithMetadata<T>> toBusEventWithMetadata(final Iterable<BusEventModelDao> entries) {
        return Iterables.<BusEventModelDao, BusEventWithMetadata<T>>transform(entries,
                                                                              new Function<BusEventModelDao, BusEventWithMetadata<T>>() {
                                                                                  @Override
                                                                                  public BusEventWithMetadata<T> apply(final BusEventModelDao input) {
                                                                                      return toBusEventWithMetadata(input);
                                                                                  }
                                                                              });
    }

    private <T extends BusEvent> BusEventWithMetadata<T> toBusEventWithMetadata(final BusEventModelDao entry) {
        final T event = CallableCallbackBase.deserializeEvent(entry, objectMapper);
        return new BusEventWithMetadata<T>(entry.getRecordId(),
                                           entry.getUserToken(),
                                           entry.getCreatedDate(),
                                           entry.getSearchKey1(),
                                           entry.getSearchKey2(),
                                           event);
    }

    public DBBackedQueue<BusEventModelDao> getDao() {
        return dao;
    }

    public Clock getClock() {
        return clock;
    }

    public PersistentBusConfig getConfig() {
        return config;
    }
}
