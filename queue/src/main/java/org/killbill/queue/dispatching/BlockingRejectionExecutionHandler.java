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

package org.killbill.queue.dispatching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class BlockingRejectionExecutionHandler implements RejectedExecutionHandler {

    private final Logger logger = LoggerFactory.getLogger(BlockingRejectionExecutionHandler.class);

    public BlockingRejectionExecutionHandler() {
    }

    /**
     * 当 ThreadPoolExecutor 无法执行 Runnable 时，就会调用这个方法
     * @param r
     * @param executor
     */
    @Override
    public void rejectedExecution(final Runnable r, final ThreadPoolExecutor executor) {
        try {
            if (!executor.isShutdown()) {
                logger.info("BlockingRejectionExecutionHandler will block request");
                // 入队，如果队列满，将会阻塞
                executor.getQueue().put(r);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException("Executor was interrupted while the task was waiting to put on work queue", e);
        }
    }
}
