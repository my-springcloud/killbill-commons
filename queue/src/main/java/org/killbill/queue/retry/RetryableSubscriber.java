/*
 * Copyright 2014-2017 Groupon, Inc
 * Copyright 2014-2017 The Billing Project, LLC
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

package org.killbill.queue.retry;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import org.joda.time.DateTime;
import org.killbill.bus.api.BusEvent;
import org.killbill.clock.Clock;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueueService.NotificationQueueHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 描述：可重试订阅器，处理事件的业务逻辑
 * 场景：针对EventBus 的实现
 * 用法：
 */
public class RetryableSubscriber extends RetryableHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryableSubscriber.class);

    public RetryableSubscriber(final Clock clock,
                               final RetryableService retryableService,
                               final NotificationQueueHandler handlerDelegate) {
        super(clock, retryableService, handlerDelegate);
    }

    /**
     * 将 busEvent 可以通过 SubscriberNotificationEvent 适配到 NotificationEvent
     * @param event
     */
    public void handleEvent(final BusEvent event) {
        handleReadyNotification(new SubscriberNotificationEvent(event, event.getClass()),
                clock.getUTCNow(),
                event.getUserToken(),
                event.getSearchKey1(),
                event.getSearchKey2());
    }

    /**
     * Subscriber 端事件处理业务逻辑
     * @param <T>
     */
    public interface SubscriberAction<T extends BusEvent> {

        void run(T event);
    }

    /**
     * 处理通知队列中已经就绪的消息
     */
    public static final class SubscriberQueueHandler implements NotificationQueueHandler {

        // Similar to com.google.common.eventbus.SubscriberRegistry
        private static final LoadingCache<Class<?>, ImmutableSet<Class<?>>> FLATTEN_HIERARCHY_CACHE =
                CacheBuilder.newBuilder()
                        .build(
                                new CacheLoader<Class<?>, ImmutableSet<Class<?>>>() {
                                    @Override
                                    public ImmutableSet<Class<?>> load(final Class<?> concreteClass) {
                                        return ImmutableSet.<Class<?>>copyOf(TypeToken.of(concreteClass).getTypes().rawTypes());
                                    }
                                }
                        );
        /** 存放了event 和 业务逻辑的映射关系 */
        private final Map<Class<?>, SubscriberAction<? extends BusEvent>> actions = new HashMap<Class<?>, SubscriberAction<? extends BusEvent>>();

        public SubscriberQueueHandler() {
        }

        /**
         * 建立BusEvent 和 SubscriberAction 的关系
         * @param busEventClass
         * @param action
         * @param <B>
         */
        public <B extends BusEvent> void subscribe(final Class<B> busEventClass, final SubscriberAction<B> action) {
            actions.put(busEventClass, action);
        }

        // 处理通知队列中已经就绪的消息
        @Override
        public void handleReadyNotification(final NotificationEvent eventJson, final DateTime eventDateTime, final UUID userToken, final Long searchKey1, final Long searchKey2) {
            if (!(eventJson instanceof SubscriberNotificationEvent)) {
                log.error("SubscriberQueueHandler received an unexpected event className='{}'", eventJson.getClass());
            } else {
                // 将 SubscriberNotificationEvent 解析成 EventBus ，用于调用 EventBus 的事件处理程序
                final BusEvent busEvent = ((SubscriberNotificationEvent) eventJson).getBusEvent();

                final ImmutableSet<Class<?>> eventTypes = FLATTEN_HIERARCHY_CACHE.getUnchecked(busEvent.getClass());
                for (final Class<?> eventType : eventTypes) {
                    // 获取消息处理方法
                    final SubscriberAction<BusEvent> next = (SubscriberAction<BusEvent>) actions.get(eventType);
                    if (next != null) {
                        next.run(busEvent);
                    }
                }
            }
        }
    }
}
