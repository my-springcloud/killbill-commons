/*
 * Copyright 2015-2016 Groupon, Inc
 * Copyright 2015-2016 The Billing Project, LLC
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
import org.killbill.clock.Clock;
import org.killbill.queue.DBBackedQueue;
import org.killbill.queue.api.PersistentQueueConfig;
import org.killbill.queue.api.PersistentQueueEntryLifecycleState;
import org.killbill.queue.api.QueueEvent;
import org.killbill.queue.dao.EventEntryModelDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;


public abstract class CallableCallbackBase<E extends QueueEvent, M extends EventEntryModelDao> implements CallableCallback<E, M> {

    private static final Logger log = LoggerFactory.getLogger(CallableCallbackBase.class);
    /**
     * 数据库队列
     */
    private final DBBackedQueue<M> dao;
    private final Clock clock;
    /** 持久化队列配置 */
    private final PersistentQueueConfig config;
    private final ObjectMapper objectMapper;

    public CallableCallbackBase(final DBBackedQueue<M> dao, final Clock clock, final PersistentQueueConfig config, final ObjectMapper objectMapper) {
        this.dao = dao;
        this.clock = clock;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * 将数据库行记录反序列化为 QueueEvent
     * @param modelDao
     * @return
     */
    @Override
    public E deserialize(final M modelDao) {
        return deserializeEvent(modelDao, objectMapper);
    }

    /**
     * JSON 反序列化
     * @param modelDao
     * @param objectMapper
     * @param <E>
     * @param <M>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <E extends QueueEvent, M extends EventEntryModelDao> E deserializeEvent(final M modelDao, final ObjectMapper objectMapper) {
        try {
            final Class<?> claz = Class.forName(modelDao.getClassName());
            // 将 EventEntryModelDao#getEventJson()反序列
            return (E) objectMapper.readValue(modelDao.getEventJson(), claz);
        } catch (final Exception e) {
            log.error(String.format("Failed to deserialize json object %s for class %s", modelDao.getEventJson(), modelDao.getClassName()), e);
            return null;
        }
    }

    @Override
    public void moveCompletedOrFailedEvents(final Iterable<M> entries) {
        dao.moveEntriesToHistory(entries);
    }

    @Override
    public void updateRetriedEvents(final M updatedEntry) {
        dao.updateOnError(updatedEntry);
    }

    @Override
    public abstract void dispatch(final E event, final M modelDao) throws Exception;

    public abstract M buildEntry(final M modelDao, final DateTime now, final PersistentQueueEntryLifecycleState newState, final long newErrorCount);

}
