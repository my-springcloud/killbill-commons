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

package org.killbill.queue.dispatching;

import org.killbill.clock.Clock;
import org.killbill.commons.concurrent.DynamicThreadPoolExecutorWithLoggingOnExceptions;
import org.killbill.queue.DefaultQueueLifecycle;
import org.killbill.queue.api.PersistentQueueConfig;
import org.killbill.queue.api.PersistentQueueEntryLifecycleState;
import org.killbill.queue.api.QueueEvent;
import org.killbill.queue.dao.EventEntryModelDao;
import org.killbill.queue.retry.RetryableInternalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;
import java.util.concurrent.*;

public class Dispatcher<E extends QueueEvent, M extends EventEntryModelDao> {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);

    // Dynamic ThreadPool Executor
    // 线程池设置的最小线程数量
    private final int corePoolSize;
    // 线程池允许的最大线程数量
    private final int maximumPoolSize;
    // 已活跃时间
    private final long keepAliveTime;
    private final TimeUnit keepAliveTimeUnit;
    // 存放要执行的任务
    private final BlockingQueue<Runnable> workQueue;
    // 线程工厂
    private final ThreadFactory threadFactory;
    // 任务被拒绝时候应该调用的处理器
    private final RejectedExecutionHandler rejectionHandler;

    //错误重试次数
    private final int maxFailureRetries;
    // 回调处理器
    private final CallableCallback<E, M> handlerCallback;
    private final DefaultQueueLifecycle parentLifeCycle;
    private final Clock clock;

    // Deferred in start sequence to allow for restart, which is not possible after the shutdown (mostly for test purpose)
    // 用于执行任务
    private ExecutorService handlerExecutor;

    public Dispatcher(final int corePoolSize,
                      final PersistentQueueConfig config,
                      final long keepAliveTime,
                      final TimeUnit keepAliveTimeUnit,
                      final BlockingQueue<Runnable> workQueue,
                      final ThreadFactory threadFactory,
                      final RejectedExecutionHandler rejectionHandler,
                      final Clock clock,
                      final CallableCallback<E, M> handlerCallback,
                      final DefaultQueueLifecycle parentLifeCycle) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = config.geMaxDispatchThreads();
        this.keepAliveTime = keepAliveTime;
        this.keepAliveTimeUnit = keepAliveTimeUnit;
        this.workQueue = workQueue;
        this.threadFactory = threadFactory;
        this.rejectionHandler = rejectionHandler;

        this.clock = clock;
        // 错误重试次数
        this.maxFailureRetries = config.getMaxFailureRetries();
        this.handlerCallback = handlerCallback;
        this.parentLifeCycle = parentLifeCycle;
    }

    public void start() {
        // 初始化线程执行器
        this.handlerExecutor = new DynamicThreadPoolExecutorWithLoggingOnExceptions(corePoolSize, maximumPoolSize, keepAliveTime, keepAliveTimeUnit, workQueue, threadFactory, rejectionHandler);
    }

    public void stop() {
        // 关闭线程执行器
        handlerExecutor.shutdown();
        try {
            handlerExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            log.info("Stop sequence, handlerExecutor has been interrupted");
        }
    }

    /**
     * 派发逻辑，创建一个包含回调的被派发实体
     *
     * @param modelDao
     */
    public void dispatch(final M modelDao) {
        log.debug("Dispatching entry {}", modelDao);
        // 构建 callable
        final CallableQueueHandler<E, M> entry = new CallableQueueHandler<E, M>(
                modelDao,
                handlerCallback,
                parentLifeCycle,
                clock,
                maxFailureRetries  // 重试次数
        );
        // 将任务提交给 ExecutorService
        handlerExecutor.submit(entry);
    }

    /**
     * 队列消息处理器
     * @param <E>
     * @param <M>
     */
    public static class CallableQueueHandler<E extends QueueEvent, M extends EventEntryModelDao> implements Callable<E> {

        private static final String MDC_KB_USER_TOKEN = "kb.userToken";

        private static final Logger log = LoggerFactory.getLogger(CallableQueueHandler.class);

        private final M entry;
        // 具有再次调用的回调处理器
        private final CallableCallback<E, M> callback;
        private final DefaultQueueLifecycle parentLifeCycle;
        // 重试次数
        private final int maxFailureRetries;
        private final Clock clock;

        public CallableQueueHandler(final M entry, final CallableCallback<E, M> callback, final DefaultQueueLifecycle parentLifeCycle, final Clock clock, final int maxFailureRetries) {
            this.entry = entry;
            this.callback = callback;
            this.parentLifeCycle = parentLifeCycle;
            this.clock = clock;
            this.maxFailureRetries = maxFailureRetries;
        }

        @Override
        public E call() throws Exception {
            try {
                final UUID userToken = entry.getUserToken();
                MDC.put(MDC_KB_USER_TOKEN, userToken != null ? userToken.toString() : null);

                log.debug("Starting processing entry {}", entry);
                final E event = callback.deserialize(entry);
                if (event != null) {
                    Throwable lastException = null;
                    long errorCount = entry.getErrorCount();
                    try {
                        // 重新派发
                        callback.dispatch(event, entry);
                    } catch (final Exception e) {
                        if (e.getCause() != null && e.getCause() instanceof InvocationTargetException) {
                            lastException = e.getCause().getCause();
                        } else if (e.getCause() != null && e.getCause() instanceof RetryableInternalException) {
                            lastException = e.getCause();
                        } else {
                            lastException = e;
                        }
                        errorCount++;
                    } finally {
                        // 如果 parentLifeCycle 存在，则构建新的数据库实体 调用 parentLifeCycle 的业务逻辑
                        if (parentLifeCycle != null) {
                            if (lastException == null) {
                                // 成功
                                final M newEntry = callback.buildEntry(entry, clock.getUTCNow(), PersistentQueueEntryLifecycleState.PROCESSED, entry.getErrorCount());
                                parentLifeCycle.dispatchCompletedOrFailedEvents(newEntry);

                                log.debug("Done handling notification {}, key = {}", entry.getRecordId(), entry.getEventJson());
                            } else if (lastException instanceof RetryableInternalException) {
                                // 失败重试
                                final M newEntry = callback.buildEntry(entry, clock.getUTCNow(), PersistentQueueEntryLifecycleState.FAILED, entry.getErrorCount());
                                parentLifeCycle.dispatchCompletedOrFailedEvents(newEntry);
                            } else if (errorCount <= maxFailureRetries) {
                                // 重试
                                log.info("Dispatch error, will attempt a retry ", lastException);

                                final M newEntry = callback.buildEntry(entry, clock.getUTCNow(), PersistentQueueEntryLifecycleState.AVAILABLE, errorCount);
                                parentLifeCycle.dispatchRetriedEvents(newEntry);
                            } else {
                                // 失败
                                log.error("Fatal NotificationQ dispatch error, data corruption...", lastException);

                                final M newEntry = callback.buildEntry(entry, clock.getUTCNow(), PersistentQueueEntryLifecycleState.FAILED, entry.getErrorCount());
                                parentLifeCycle.dispatchCompletedOrFailedEvents(newEntry);
                            }
                        }
                    }
                }
                return event;
            } finally {
                MDC.remove(MDC_KB_USER_TOKEN);
            }
        }
    }

}