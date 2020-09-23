/*
 * Copyright 2015 Groupon, Inc
 * Copyright 2015 The Billing Project, LLC
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

package org.killbill.commons.concurrent;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

// See http://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing/19538899#19538899

/**
 * 可以动态调整线程池大小，并且在任务执行异常的时候打印出日志
 */
public class DynamicThreadPoolExecutorWithLoggingOnExceptions extends LoggingExecutor {
    // 初始的线程池中线程的最小数量
    private final int inputSpecifiedCorePoolSize;
    // 当前执行任务总数
    private int currentTasks;

    /**
     *
     * @param corePoolSize 线程池中线程的最小数量
     * @param maximumPoolSize
     * @param name
     * @param keepAliveTime
     * @param unit
     */
    public DynamicThreadPoolExecutorWithLoggingOnExceptions(final int corePoolSize, final int maximumPoolSize, final String name, final long keepAliveTime, final TimeUnit unit) {
        super(corePoolSize, maximumPoolSize, name, keepAliveTime, unit);
        this.inputSpecifiedCorePoolSize = corePoolSize;
    }

    public DynamicThreadPoolExecutorWithLoggingOnExceptions(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory);
        this.inputSpecifiedCorePoolSize = corePoolSize;
    }

    public DynamicThreadPoolExecutorWithLoggingOnExceptions(final int corePoolSize, final int maximumPoolSize, final String name, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, name, keepAliveTime, unit, workQueue);
        this.inputSpecifiedCorePoolSize = corePoolSize;
    }

    public DynamicThreadPoolExecutorWithLoggingOnExceptions(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        this.inputSpecifiedCorePoolSize = corePoolSize;
    }

    public DynamicThreadPoolExecutorWithLoggingOnExceptions(final int corePoolSize, final int maximumPoolSize, final String name, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue, final RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, name, keepAliveTime, unit, workQueue, handler);
        this.inputSpecifiedCorePoolSize = corePoolSize;
    }

    public DynamicThreadPoolExecutorWithLoggingOnExceptions(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.inputSpecifiedCorePoolSize = corePoolSize;
    }


    @Override
    public void execute(final Runnable runnable) {
        synchronized (this) { // 同步代码块
            currentTasks++;
            setCorePoolSizeToTaskCountWithinBounds();
        }
        // 执行任务
        super.execute(runnable);
    }

    @Override
    protected void afterExecute(final Runnable runnable, final Throwable throwable) {
        super.afterExecute(runnable, throwable);
        synchronized (this) {
            currentTasks--;
            setCorePoolSizeToTaskCountWithinBounds();
        }
    }
    // 将线程池核心大小设置在 currentTasks 以内
    private void setCorePoolSizeToTaskCountWithinBounds() {
        int updatedCorePoolSize = currentTasks;
        if (updatedCorePoolSize < inputSpecifiedCorePoolSize) {
            updatedCorePoolSize = inputSpecifiedCorePoolSize;
        }
        if (updatedCorePoolSize > getMaximumPoolSize()) {
            // 不允许超过线程池中的最大线程数量
            updatedCorePoolSize = getMaximumPoolSize();
        }
        // 动态调整线程池最小活跃线程数量
        setCorePoolSize(updatedCorePoolSize);
    }
}
