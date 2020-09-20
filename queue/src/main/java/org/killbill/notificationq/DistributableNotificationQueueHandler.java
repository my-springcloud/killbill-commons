package org.killbill.notificationq;

import com.google.common.eventbus.EventBusException;
import com.google.common.eventbus.EventBusThatThrowsException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.killbill.billing.util.queue.QueueRetryException;
import org.killbill.bus.api.PersistentBus;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.queue.api.QueueEvent;

import java.util.List;
import java.util.UUID;

/**
 * @author zenglw
 * @date 2020/9/20 14:23
 */
public class DistributableNotificationQueueHandler implements NotificationQueueService.NotificationQueueHandler {


    private final List<Period> retrySchedule;

    // 委托给其它实现
    private final EventBusThatThrowsException eventBusDelegate;

    private static final class EventBusDelegate extends EventBusThatThrowsException {

        public EventBusDelegate(final String queueName) {
            super(queueName);
        }
    }

    public DistributableNotificationQueueHandler(String queueName, List<Period> retrySchedule) {
        this.eventBusDelegate = new EventBusDelegate(queueName);
        this.retrySchedule = retrySchedule;
    }

    /**
     * 委托给 eventBusDelegate
     *
     * @param handlerInstance handler to register
     * @throws PersistentBus.EventBusException
     */
    public void register(final Object handlerInstance) throws PersistentBus.EventBusException {
        eventBusDelegate.register(handlerInstance);
    }

    public void unregister(final Object handlerInstance) throws PersistentBus.EventBusException {
        eventBusDelegate.unregister(handlerInstance);
    }

    /**
     * 真正的事件派发逻辑
     *
     * @param event
     * @throws com.google.common.eventbus.EventBusException
     */
    public void dispatcher(final QueueEvent event) throws com.google.common.eventbus.EventBusException {
        System.out.println("派发事件：" + event.toString());
        eventBusDelegate.postWithException(event);
    }

    @Override
    public void handleReadyNotification(NotificationEvent eventJson, DateTime eventDateTime, UUID userToken, Long searchKey1, Long searchKey2) {
        try {
            dispatcher(eventJson);
        } catch (EventBusException e) {
            if (retrySchedule != null) {
                throw new QueueRetryException(e, retrySchedule);
            }
            throw new RuntimeException(e);
        }
    }
}
