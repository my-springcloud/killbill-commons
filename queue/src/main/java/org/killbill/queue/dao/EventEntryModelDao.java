/*
 * Copyright 2010-2013 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.queue.dao;

import java.util.UUID;

import org.joda.time.DateTime;

import org.killbill.queue.api.PersistentQueueEntryLifecycleState;

/**
 * 数据库行记录
 */
public interface EventEntryModelDao {

    Long getRecordId();

    String getClassName();

    String getEventJson();

    UUID getUserToken();

    String getProcessingOwner();

    String getCreatingOwner();

    DateTime getNextAvailableDate();

    PersistentQueueEntryLifecycleState getProcessingState();

    boolean isAvailableForProcessing(DateTime now);

    Long getErrorCount();

    Long getSearchKey1();

    Long getSearchKey2();

    // setters
    void setClassName(final String className);

    void setEventJson(final String eventJson);

    void setUserToken(final UUID userToken);

    void setCreatingOwner(final String creatingOwner);

    void setProcessingState(final PersistentQueueEntryLifecycleState processingState);

    void setProcessingOwner(final String processingOwner);

    void setCreatedDate(final DateTime createdDate);

    void setErrorCount(final Long errorCount);

    void setSearchKey1(final Long searchKey1);

    void setSearchKey2(final Long searchKey2);
}
