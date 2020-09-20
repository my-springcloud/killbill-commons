/*
 * Copyright 2010-2013 Ning, Inc.
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

package org.killbill.bus.dao;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.killbill.commons.jdbi.statement.SmartFetchSize;
import org.killbill.commons.jdbi.template.KillBillSqlDaoStringTemplate;
import org.killbill.queue.dao.QueueSqlDao;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.Define;

@KillBillSqlDaoStringTemplate
public interface PersistentBusSqlDao extends QueueSqlDao<BusEventModelDao> {

    /**
     * SQL:
     *     select
     *       record_id
     *     from <tableName>
     *     where
     *       record_id >= :from
     *       AND
     *       processing_state = 'AVAILABLE'
     * 	  <if(owner)>and creating_owner = '<owner>'<endif>
     *     order by
     *       <readyOrderByClause()>
     *     limit :max
     *     ;
     * @param now
     * @param from
     * @param max
     * @param owner
     * @param tableName
     * @return
     */
    @SqlQuery
    List<Long> getReadyEntryIds(@Bind("now") Date now,
                                @Bind("from") long from,
                                @Bind("max") int max,
                                // This is somewhat a hack, should really be a @Bind parameter but we also use it
                                // for StringTemplate to modify the query based whether value is null or not.
                                @Nullable @Define("owner") String owner,
                                @Define("tableName") final String tableName);

    @SqlQuery
    @SmartFetchSize(shouldStream = true)
    public Iterator<BusEventModelDao> getReadyQueueEntriesForSearchKeys(@Bind("searchKey1") final Long searchKey1,
                                                                        @Bind("searchKey2") final Long searchKey2,
                                                                        @Define("tableName") final String tableName);

    @SqlQuery
    @SmartFetchSize(shouldStream = true)
    public Iterator<BusEventModelDao> getReadyQueueEntriesForSearchKey2(@Bind("maxCreatedDate") final DateTime maxCreatedDate,
                                                                        @Bind("searchKey2") final Long searchKey2,
                                                                        @Define("tableName") final String tableName);

    @SqlQuery
    @SmartFetchSize(shouldStream = true)
    public Iterator<BusEventModelDao> getReadyOrInProcessingQueueEntriesForSearchKeys(@Bind("searchKey1") final Long searchKey1,
                                                                                      @Bind("searchKey2") final Long searchKey2,
                                                                                      @Define("tableName") final String tableName);

    @SqlQuery
    @SmartFetchSize(shouldStream = true)
    public Iterator<BusEventModelDao> getReadyOrInProcessingQueueEntriesForSearchKey2(@Bind("maxCreatedDate") final DateTime maxCreatedDate,
                                                                                      @Bind("searchKey2") final Long searchKey2,
                                                                                      @Define("tableName") final String tableName);

    /**
     *     select
     *       <allTableFields()>
     *     from <historyTableName>
     *     where
     *       queue_name = :queueName
     *       and search_key1 = :searchKey1
     *       and search_key2 = :searchKey2
     *     order by
     *       <readyOrderByClause()>
     * @param searchKey1
     * @param searchKey2
     * @param historyTableName
     * @return
     */
    @SqlQuery
    @SmartFetchSize(shouldStream = true)
    public Iterator<BusEventModelDao> getHistoricalQueueEntriesForSearchKeys(@Bind("searchKey1") final Long searchKey1,
                                                                             @Bind("searchKey2") final Long searchKey2,
                                                                             @Define("historyTableName") final String historyTableName);

    /**
     *  select
     *       <allTableFields()>
     *     from <historyTableName>
     *     where
     *           queue_name = :queueName
     *       and effective_date >= cast(coalesce(:minEffectiveDate, '1970-01-01') as datetime)
     *       and search_key2 = :searchKey2
     *     order by
     *       effective_date asc
     *       , created_date asc
     *       , record_id
     *
     * @param minCreatedDate
     * @param searchKey2
     * @param historyTableName
     * @return
     */
    @SqlQuery
    @SmartFetchSize(shouldStream = true)
    public Iterator<BusEventModelDao> getHistoricalQueueEntriesForSearchKey2(@Bind("minCreatedDate") final DateTime minCreatedDate,
                                                                             @Bind("searchKey2") final Long searchKey2,
                                                                             @Define("historyTableName") final String historyTableName);
}
