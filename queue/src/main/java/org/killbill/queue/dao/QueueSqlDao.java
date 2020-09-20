/*
 * Copyright 2010-2013 Ning, Inc.
 * Copyright 2014-2019 Groupon, Inc
 * Copyright 2014-2019 The Billing Project, LLC
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

package org.killbill.queue.dao;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import org.killbill.commons.jdbi.binder.SmartBindBean;
import org.killbill.commons.jdbi.template.KillBillSqlDaoStringTemplate;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.unstable.BindIn;
import org.skife.jdbi.v2.util.LongMapper;

@KillBillSqlDaoStringTemplate
public interface QueueSqlDao<T extends EventEntryModelDao> extends Transactional<QueueSqlDao<T>>, CloseMe {

    @SqlQuery
    Long getMaxRecordId(@Define("tableName") final String tableName);

    /**
     * Notification:
     *    select
     *                  record_id
     *                   , class_name
     *                   , event_json
     *                   , user_token
     *                   , created_date
     *                   , creating_owner
     *                   , processing_owner
     *                   , processing_available_date
     *                   , processing_state
     *                   , error_count
     *                   , search_key1
     *                   , search_key2
     *                   , future_user_token
     *                   , effective_date
     *                   , queue_name
     *     from notifications
     *     where
     *     record_id = 1;
     *
     *
     * @param id
     * @param tableName
     * @return
     */
    @SqlQuery
    T getByRecordId(@Bind("recordId") Long id,
                    @Define("tableName") final String tableName);

    /**
     *  SQL：
     *     select
     *       <allTableFields()>
     *     from <tableName>
     *     where
     *       record_id in (<record_ids>)
     *     order by
     *       <readyOrderByClause()>
     *     ;
     * @param recordIds
     * @param tableName
     * @return
     */
    @SqlQuery
    List<T> getEntriesFromIds(@BindIn("record_ids") final List<Long> recordIds,
                              @Define("tableName") final String tableName);

    /**
     * Notification：
     * select
     *                  record_id
     *                   , class_name
     *                   , event_json
     *                   , user_token
     *                   , created_date
     *                   , creating_owner
     *                   , processing_owner
     *                   , processing_available_date
     *                   , processing_state
     *                   , error_count
     *                   , search_key1
     *                   , search_key2
     *                   , future_user_token
     *                   , effective_date
     *                   , queue_name
     *     from notifications
     *     where
     *           effective_date <= '2020-07-27 07:29:44'
     *           and processing_state = 'AVAILABLE'
     *       and creating_owner = 'Yop'
     *     order by
     *               effective_date asc
     *             , created_date asc
     *             , record_id
     *     limit 3；
     *
     *
     * @param now
     * @param max
     * @param owner
     * @param tableName
     * @return
     */
    @SqlQuery
    List<T> getReadyEntries(@Bind("now") Date now,
                            @Bind("max") int max,
                            // This is somewhat a hack, should really be a @Bind parameter but we also use it
                            // for StringTemplate to modify the query based whether value is null or not.
                            @Nullable @Define("owner") String owner,
                            @Define("tableName") final String tableName);

    /**
     * Notification:
     *      select
     *       count(*)
     *     from notifications
     *     where
     *           effective_date <= '2020-07-27 07:29:44'
     *           and processing_state = 'AVAILABLE'
     *       and creating_owner = 'Yop'
     * @param now
     * @param owner
     * @param tableName
     * @return
     */
    @SqlQuery
    long getNbReadyEntries(@Bind("now") Date now,
                            // This is somewhat a hack, should really be a @Bind parameter but we also use it
                            // for StringTemplate to modify the query based whether value is null or not.
                            @Nullable @Define("owner") String owner,
                            @Define("tableName") final String tableName);

    /**
     *     select
     *       <allTableFields()>
     *     from <tableName>
     *     where
     *       processing_state = 'IN_PROCESSING'
     *     order by
     *       <readyOrderByClause()>
     *     ;
     * @param tableName
     * @return
     */
    @SqlQuery
    List<T> getInProcessingEntries(@Define("tableName") final String tableName);

    /**
     *  SQL:
     *     select
     *       <allTableFields()>
     *     from <tableName>
     *     where
     *   		processing_state != 'PROCESSED'
     *     and processing_state != 'REMOVED'
     *     and (processing_owner IS NULL OR processing_available_date <= :now)
     *     and created_date <= :reapingDate
     *     order by created_date asc
     *     limit :max;
     *
     * @param max
     * @param now
     * @param reapingDate
     * @param tableName
     * @return
     */
    @SqlQuery
    List<T> getEntriesLeftBehind(@Bind("max") int max,
                                 @Bind("now") Date now,
                                 @Bind("reapingDate") Date reapingDate,
                                 @Define("tableName") final String tableName);

    /**
     * Notification:
     *     update notifications
     *     set
     *       processing_owner = '561b29ee-ba8e-4326-80cc-06dea2371d9e'
     *       , processing_available_date = '2020-07-27 07:34:44'
     *       , processing_state = 'IN_PROCESSING'
     *     where
     *       record_id = 1
     *       and processing_state != 'PROCESSED'
     *       and processing_state != 'REMOVED'
     *       and processing_owner IS NULL;
     *
     *
     * @param id
     * @param owner
     * @param nextAvailable
     * @param tableName
     * @return
     */
    @SqlUpdate
    int claimEntry(@Bind("recordId") Long id,
                   @Bind("owner") String owner,
                   @Bind("nextAvailable") Date nextAvailable,
                   @Define("tableName") final String tableName);

    @SqlUpdate
    int claimEntries(@BindIn("record_ids") final Collection<Long> recordIds,
                     @Bind("owner") String owner,
                     @Bind("nextAvailable") Date nextAvailable,
                     @Define("tableName") final String tableName);

    @SqlUpdate
    int updateOnError(@Bind("recordId") Long id,
                      @Bind("now") Date now,
                      @Bind("errorCount") Long errorCount,
                      @Define("tableName") final String tableName);

    /**
     * Notification：
     * delete from notifications where record_id = 1;
     *
     * @param id
     * @param tableName
     */
    @SqlUpdate
    void removeEntry(@Bind("recordId") Long id,
                     @Define("tableName") final String tableName);

    @SqlUpdate
    void removeEntries(@BindIn("record_ids") final Collection<Long> recordIds,
                       @Define("tableName") final String tableName);

    /**
     * Notification：
     * insert into notifications (
     *                    class_name
     *                    , event_json
     *                    , user_token
     *                    , created_date
     *                    , creating_owner
     *                    , processing_owner
     *                    , processing_available_date
     *                    , processing_state
     *                    , error_count
     *                    , search_key1
     *                    , search_key2
     *                    , future_user_token
     *                    , effective_date
     *                    , queue_name
     *     ) values (
     *                    'java.lang.String'
     *                    , '535df24b-2e23-4d86-bbf4-f78bd8dd39b8'
     *                    , 'f219a7b0-0aae-4565-9ff2-6787a29602a4'
     *                    , '2020-07-27 07:28:28'
     *                    , 'Yop'
     *                    , null
     *                    , null
     *                    , 'AVAILABLE'
     *                    , 0
     *                    , 1242
     *                    , 37
     *                    , 'f879f359-88a5-424c-9fdb-53c173247333'
     *                    , '2020-07-27 07:28:28'
     *                    , 'testBasic'
     *     )；
     *     insert into notifications_history (
     *                    class_name
     *                    , event_json
     *                    , user_token
     *                    , created_date
     *                    , creating_owner
     *                    , processing_owner
     *                    , processing_available_date
     *                    , processing_state
     *                    , error_count
     *                    , search_key1
     *                    , search_key2
     *                    , future_user_token
     *                    , effective_date
     *                    , queue_name
     *     ) values (
     *                    'java.lang.String'
     *                    , '535df24b-2e23-4d86-bbf4-f78bd8dd39b8'
     *                    , 'f219a7b0-0aae-4565-9ff2-6787a29602a4'
     *                    , '2020-07-27 07:28:28'
     *                    , 'Yop'
     *                    , 'DESKTOP-76LNS6C'
     *                    , '2020-07-27 07:32:55'
     *                    , 'PROCESSED'
     *                    , 0
     *                    , 1242
     *                    , 37
     *                    , 'f879f359-88a5-424c-9fdb-53c173247333'
     *                    , '2020-07-27 07:28:28'
     *                    , 'testBasic'
     *     );
     *
     * @param evt
     * @param tableName
     * @return
     */
    @SqlUpdate
    @GetGeneratedKeys(value = LongMapper.class, columnName = "record_id")
    Long insertEntry(@SmartBindBean T evt,
                     @Define("tableName") final String tableName);

    @SqlBatch
    @BatchChunkSize(100)
    void insertEntries(@SmartBindBean Iterable<T> evts,
                       @Define("tableName") final String tableName);
}
