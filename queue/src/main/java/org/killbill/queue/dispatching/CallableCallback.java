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

import org.joda.time.DateTime;
import org.killbill.queue.api.PersistentQueueEntryLifecycleState;
import org.killbill.queue.api.QueueEvent;
import org.killbill.queue.dao.EventEntryModelDao;

/**
 * 真正的事件派发逻辑，包含在java对象和数据库记录之间序列化、反序列化事件的功能
 */
public interface CallableCallback<E extends QueueEvent, M extends EventEntryModelDao> {

    E deserialize(final M modelDao);

    /**
     * 此处是真正的派发逻辑
     * @param event
     * @param modelDao
     * @throws Exception
     */
    void dispatch(final E event, final M modelDao) throws Exception;

    /**
     * 基于 modelDao 构建一个新的实体
     * @param modelDao
     * @param now
     * @param newState
     * @param newErrorCount
     * @return
     */
    M buildEntry(final M modelDao, final DateTime now, final PersistentQueueEntryLifecycleState newState, final long newErrorCount);

    /**
     * 将数据移动到 历史表
     * @param entries
     */
    void moveCompletedOrFailedEvents(final Iterable<M> entries);

    /**
     * 更新实体
     * @param updatedEntry
     */
    void updateRetriedEvents(final M updatedEntry);
}
