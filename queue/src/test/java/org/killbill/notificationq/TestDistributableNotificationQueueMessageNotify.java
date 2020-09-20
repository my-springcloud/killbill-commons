package org.killbill.notificationq;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.killbill.TestSetup;
import org.killbill.clock.DefaultClock;
import org.killbill.notificationq.api.NotificationEvent;
import org.killbill.notificationq.api.NotificationQueue;
import org.killbill.notificationq.api.NotificationQueueService;
import org.killbill.notificationq.dao.NotificationEventModelDao;
import org.killbill.notificationq.dao.NotificationSqlDao;
import org.killbill.queue.api.PersistentQueueEntryLifecycleState;
import org.killbill.queue.retry.RetryableHandler;
import org.killbill.queue.retry.RetryableService;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * @author zenglw
 * @date 2020/9/20 14:41
 */
public class TestDistributableNotificationQueueMessageNotify extends TestSetup {

    private final Logger log = LoggerFactory.getLogger(TestNotificationQueue.class);

    private static final UUID TOKEN_ID = UUID.randomUUID();
    private static final long SEARCH_KEY_1 = 65;
    private static final long SEARCH_KEY_2 = 34;

    private NotificationQueueService queueService;
    private RetryableNotificationQueueService retryableQueueService;

    private volatile int eventsReceived;

    private static final class RetryableNotificationQueueService extends RetryableService {

        public RetryableNotificationQueueService(final NotificationQueueService notificationQueueService) {
            super(notificationQueueService);
        }
    }


    @Override
    @BeforeMethod(groups = "slow")
    public void beforeMethod() throws Exception {
        super.beforeMethod();
        // 队列服务（创建删除队列）
        queueService = new DefaultNotificationQueueService(getDBI(), clock, getNotificationQueueConfig(), metricRegistry);
        retryableQueueService = new RetryableNotificationQueueService(queueService);
        eventsReceived = 0;
    }


    @Test(groups = "slow")
    public void testRetryStateForNotifications() throws Exception {
        // 4 retries
        final DistributableNotificationQueueHandler handlerDelegate = new DistributableNotificationQueueHandler("queueName", ImmutableList.<Period>of(Period.millis(1),
                Period.millis(1),
                Period.millis(1),
                Period.days(1)));
        handlerDelegate.register(new QueueMessageNotify());
        // retry 消息处理器，会委派给 handlerDelegate
        final NotificationQueueService.NotificationQueueHandler retryableHandler = new RetryableHandler(clock, retryableQueueService, handlerDelegate);
        // 创建队列
        final NotificationQueue queueWithExceptionAndFailed = queueService.createNotificationQueue("svc", "queueName", retryableHandler);
        try {
            retryableQueueService.initialize(queueWithExceptionAndFailed.getQueueName(), handlerDelegate);
            retryableQueueService.start();
            queueWithExceptionAndFailed.startQueue();

            final DateTime now = new DateTime();
            final DateTime readyTime = now.plusMillis(2000);

            queueWithExceptionAndFailed.recordFutureNotification(readyTime, new Event1(), TOKEN_ID, SEARCH_KEY_1, SEARCH_KEY_2);
            queueWithExceptionAndFailed.recordFutureNotification(readyTime, new Event2(), TOKEN_ID, SEARCH_KEY_1, SEARCH_KEY_2);
            queueWithExceptionAndFailed.recordFutureNotification(readyTime, new Event3(), TOKEN_ID, SEARCH_KEY_1, SEARCH_KEY_2);

            Thread.sleep(50000);
        } finally {
            queueWithExceptionAndFailed.stopQueue();
            retryableQueueService.stop();
        }
    }


    @Test
    public void testDistributableNotificationQueueHandler() throws Exception {

        final Map<NotificationEvent, Boolean> expectedNotifications = new TreeMap<NotificationEvent, Boolean>();
        DistributableNotificationQueueHandler distributableNotificationQueueHandler = new DistributableNotificationQueueHandler("test-svc",null);
        distributableNotificationQueueHandler.register(new QueueMessageNotify());
        // 创建通知队列
        final NotificationQueue queue = queueService.createNotificationQueue("test-svc",
                "foo", distributableNotificationQueueHandler
        );

        // 启动通知队列
        queue.startQueue();

        // ms will be truncated in the database
        final DateTime now = DefaultClock.truncateMs(new DateTime());
        // 现在+2000毫秒
        final DateTime readyTime = now.plusMillis(2000);

        final DBI dbi = getDBI();


        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                queue.recordFutureNotificationFromTransaction(conn.getConnection(), readyTime, new Event1(), TOKEN_ID, 1L, SEARCH_KEY_2);
                return null;
            }
        });

        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                queue.recordFutureNotificationFromTransaction(conn.getConnection(), readyTime, new Event1(), TOKEN_ID, SEARCH_KEY_1, 1L);
                return null;
            }
        });

        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                queue.recordFutureNotificationFromTransaction(conn.getConnection(), readyTime, new Event2(), TOKEN_ID, SEARCH_KEY_1, SEARCH_KEY_2);
                return null;
            }
        });

        dbi.inTransaction(new TransactionCallback<Object>() {
            @Override
            public Object inTransaction(final Handle conn, final TransactionStatus status) throws Exception {
                queue.recordFutureNotificationFromTransaction(conn.getConnection(), readyTime, new Event3(), TOKEN_ID, SEARCH_KEY_1, SEARCH_KEY_2);
                return null;
            }
        });
        Thread.sleep(5000);
    }

    public static class Event1 implements NotificationEvent {
        private final String value;

        public Event1() {
            this.value = "event1" + UUID.randomUUID().toString();
        }

        public String getValue() {
            return value;
        }
    }

    public static class Event2 implements NotificationEvent {
        private final String value;

        public Event2() {
            this.value = "event2" + UUID.randomUUID().toString();
        }

        public String getValue() {
            return value;
        }
    }

    public static class Event3 implements NotificationEvent {
        private final String value;

        public Event3() {
            this.value = "event3" + UUID.randomUUID().toString();
        }

        public String getValue() {
            return value;
        }
    }

    private class QueueMessageNotify {

        @AllowConcurrentEvents
        @Subscribe
        public void notifyForEvent1(Event1 event1) {
            System.out.println("通知Event1：" + event1.getValue());
        }

        @AllowConcurrentEvents
        @Subscribe
        public void notifyForEvent2(Event2 event2) {
            System.out.println("通知Event2：" + event2.getValue());
        }

        @AllowConcurrentEvents
        @Subscribe
        public void notifyForEvent3(Event3 event3) {
            System.out.println("通知Event3：" + event3.getValue());
            throw new RuntimeException("通知Event3错误");
        }
    }
}
