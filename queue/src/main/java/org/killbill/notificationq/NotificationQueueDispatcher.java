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

package org.killbill.notificationq;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.killbill.clock.Clock;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueConfig;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.killbill.notificationq.dao.NotificationEventModelDao;
import org.killbill.notificationq.dao.NotificationSqlDao;
import org.killbill.notificationq.dispatching.NotificationCallableCallback;
import org.killbill.queue.DBBackedQueue;
import org.killbill.queue.DBBackedQueue.ReadyEntriesWithMetrics;
import org.killbill.queue.DBBackedQueueWithPolling;
import org.killbill.queue.DefaultQueueLifecycle;
import org.killbill.queue.api.PersistentQueueConfig.PersistentQueueMode;
import org.killbill.queue.dao.EventEntryModelDao;
import org.killbill.queue.dispatching.BlockingRejectionExecutionHandler;
import org.killbill.queue.dispatching.Dispatcher;
import org.skife.jdbi.v2.IDBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;

/**
 * 队列派发器，将数据库中的消息派发给queue，并调用订阅端进行处理
 */
public class NotificationQueueDispatcher extends DefaultQueueLifecycle {

    protected static final Logger log = LoggerFactory.getLogger(NotificationQueueDispatcher.class);

    public static final int CLAIM_TIME_MS = (5 * 60 * 1000); // 5 minutes
    // 已处理事件总数
    private final AtomicLong nbProcessedEvents;

    protected final NotificationQueueConfig config;
    protected final Clock clock;
    // 队列名称，队列映射关系
    protected final Map<String, NotificationQueue> queues;
    // 数据库队列 -- 将队列操作映射到数据库操作
    protected final DBBackedQueue<NotificationEventModelDao> dao;
    protected final MetricRegistry metricRegistry;

    private final Map<String, Histogram> perQueueProcessingTime;

    // We could event have one per queue is required...
    private final Dispatcher<NotificationEvent, NotificationEventModelDao> dispatcher;

    // 是否初始化
    private final AtomicBoolean isInitialized;
    // 是否启用
    private volatile boolean isStarted;
    // 激活的队列数量
    private volatile int activeQueues;
    // 通知回调
    private final NotificationCallableCallback notificationCallableCallback;
    // 挖掘机
    private final NotificationReaper reaper;

    // Package visibility on purpose
    NotificationQueueDispatcher(final Clock clock, final NotificationQueueConfig config, final IDBI dbi, final MetricRegistry metricRegistry) {
        super(config.getTableName(), config, metricRegistry);
        final ThreadFactory notificationQThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(final Runnable r) {
                final Thread th = new Thread(r);
                th.setName(config.getTableName() + "-th");
                th.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(final Thread t, final Throwable e) {
                        log.error("Uncaught exception for thread " + t.getName(), e);
                    }
                });
                return th;
            }
        };

        this.clock = clock;
        this.config = config;
        this.nbProcessedEvents = new AtomicLong();
        // 数据库队列
        this.dao = new DBBackedQueueWithPolling<NotificationEventModelDao>(clock, dbi, NotificationSqlDao.class, config, config.getTableName(), metricRegistry);

        this.queues = new TreeMap<String, NotificationQueue>();

        this.perQueueProcessingTime = new HashMap<String, Histogram>();

        this.metricRegistry = metricRegistry;
        this.isInitialized = new AtomicBoolean(false);
        this.isStarted = false;
        this.activeQueues = 0;
        // 构建 reaper
        this.reaper = new NotificationReaper(this.dao, config, clock);
        // 构建回调对象
        this.notificationCallableCallback = new NotificationCallableCallback(this);
        // 构建 dispatcher
        this.dispatcher = new Dispatcher<>(1, config, 10, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(config.getEventQueueCapacity()), notificationQThreadFactory, new BlockingRejectionExecutionHandler(),
                                           clock, notificationCallableCallback, this);
    }

    @Override
    public boolean initQueue() {
        if (isInitialized.compareAndSet(false, true)) {
            // 初始化持久化队列（初始化缓存等）
            dao.initialize();
            // 启动派发器
            dispatcher.start();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean startQueue() {

        if (!isInitialized.get()) {
            // Make it easy for our tests, so they simply call startQueue
            initQueue();
        }

        //
        // The first DefaultNotificationQueue#startQueue will call this method and start the reaper and lifecycle dispatch thread pool
        // All subsequent DefaultNotificationQueue#startQueue will simply increment the # activeQueues
        //
        synchronized (queues) {
            // Increment number of active queues
            activeQueues++;

            if (!isStarted) {
                if (config.getPersistentQueueMode() == PersistentQueueMode.STICKY_POLLING) {
                    reaper.start();
                }
                super.startQueue();
                isStarted = true;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public void stopQueue() {
        synchronized (queues) {
            activeQueues--;
            //
            // The last DefaultNotificationQueue#stopQueue will call this method and stop the reaper and lifecycle dispatch thread pool
            //
            if (activeQueues == 0) {
                isInitialized.set(false);
                reaper.stop();
                super.stopQueue();
                dispatcher.stop();
                dao.close();
                isStarted = false;
            }
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public DispatchResultMetrics doDispatchEvents() {
        final List<NotificationEventModelDao> notifications = getReadyNotifications();
        if (notifications.isEmpty()) {
            return new DispatchResultMetrics(0, -1);
        }
        log.debug("Notifications from {} to process: {}", config.getTableName(), notifications);

        for (final NotificationEventModelDao cur : notifications) {
            // 派发事件
            dispatcher.dispatch(cur);
        }
        // No need to return time, this is easy to compute from caller
        return new DispatchResultMetrics(notifications.size(), -1);
    }

    @Override
    public void doProcessCompletedEvents(final Iterable<? extends EventEntryModelDao> completed) {
        notificationCallableCallback.moveCompletedOrFailedEvents((Iterable<NotificationEventModelDao>) completed);
    }

    @Override
    public void doProcessRetriedEvents(final Iterable<? extends EventEntryModelDao> retried) {
        Iterator<? extends EventEntryModelDao> it = retried.iterator();
        while (it.hasNext()) {
            NotificationEventModelDao cur = (NotificationEventModelDao) it.next();
            notificationCallableCallback.updateRetriedEvents(cur);
        }
    }

    /**
     * 通知事件派发的真实逻辑
     * @param handler
     * @param notification
     * @param key
     * @throws NotificationQueueException
     */
    public void handleNotificationWithMetrics(final NotificationQueueHandler handler, final NotificationEventModelDao notification, final NotificationEvent key) throws NotificationQueueException {

        // Create specific metric name because:
        // - ':' is not allowed for metric name
        // - name would be too long (e.g entitlement-service:subscription-events-process-time -> ent-subscription-events-process-time)
        //
        final String[] parts = notification.getQueueName().split(":");
        final String metricName = new StringBuilder(parts[0].substring(0, 3))
                .append("-")
                .append(parts[1])
                .append("-ProcessingTime").toString();

        Histogram perQueueHistogramProcessingTime = perQueueProcessingTime.get(notification.getQueueName());
        if (perQueueHistogramProcessingTime == null) {
            synchronized (perQueueProcessingTime) {
                if (!perQueueProcessingTime.containsKey(notification.getQueueName())) {
                    perQueueProcessingTime.put(notification.getQueueName(), metricRegistry.histogram(MetricRegistry.name(NotificationQueueDispatcher.class, metricName)));
                }
                perQueueHistogramProcessingTime = perQueueProcessingTime.get(notification.getQueueName());
            }
        }

        final long beforeProcessing = System.nanoTime();
        try {
            // 执行通知业务逻辑
            handler.handleReadyNotification(key, notification.getEffectiveDate(), notification.getFutureUserToken(), notification.getSearchKey1(), notification.getSearchKey2());
        } catch (final RuntimeException e) {
            // 重新包装异常
            throw new NotificationQueueException(e);
        } finally {
            // 处理事件总数加1
            nbProcessedEvents.incrementAndGet();
            // Unclear if those stats should include failures
            perQueueHistogramProcessingTime.update(System.nanoTime() - beforeProcessing);
        }
    }

    /**
     * 获取queue绑定的处理器
     * @param compositeName
     * @return
     */
    public NotificationQueueHandler getHandlerForActiveQueue(final String compositeName) {
        final NotificationQueue queue = queues.get(compositeName);
        if (queue == null || !queue.isStarted()) {
            return null;
        }
        return queue.getHandler();
    }

    private List<NotificationEventModelDao> getReadyNotifications() {
        final ReadyEntriesWithMetrics<NotificationEventModelDao> result = dao.getReadyEntries();
        final List<NotificationEventModelDao> input = result.getEntries();
        final List<NotificationEventModelDao> claimedNotifications = new ArrayList<NotificationEventModelDao>();
        for (final NotificationEventModelDao cur : input) {

            // Skip non active queues...
            final NotificationQueue queue = queues.get(cur.getQueueName());
            if (queue == null || !queue.isStarted()) {
                continue;
            }
            claimedNotifications.add(cur);
        }
        return claimedNotifications;
    }

    public static String getCompositeName(final String svcName, final String queueName) {
        return svcName + ":" + queueName;
    }

    public NotificationQueueConfig getConfig() {
        return config;
    }

    public Clock getClock() {
        return clock;
    }

    public DBBackedQueue<NotificationEventModelDao> getDao() {
        return dao;
    }
}
