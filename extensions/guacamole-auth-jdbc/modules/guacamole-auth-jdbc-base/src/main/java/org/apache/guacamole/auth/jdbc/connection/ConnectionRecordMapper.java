/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.jdbc.connection;

import java.util.Collection;
import java.util.List;
import org.apache.guacamole.auth.jdbc.base.ActivityRecordSearchTerm;
import org.apache.guacamole.auth.jdbc.base.ActivityRecordSortPredicate;
import org.apache.ibatis.annotations.Param;
import org.apache.guacamole.auth.jdbc.user.UserModel;

/**
 * Mapper for connection record objects.
 */
public interface ConnectionRecordMapper {

    /**
     * Returns a collection of all connection records associated with the
     * connection having the given identifier.
     *
     * @param identifier
     *     The identifier of the connection whose records are to be retrieved.
     *
     * @return
     *     A collection of all connection records associated with the
     *     connection having the given identifier. This collection will be
     *     empty if no such connection exists.
     */
    List<ConnectionRecordModel> select(@Param("identifier") String identifier);

    /**
     * Inserts the given connection record.
     *
     * @param record
     *     The connection record to insert.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("record") ConnectionRecordModel record);

    /**
     * Searches for up to <code>limit</code> connection records that contain
     * the given terms, sorted by the given predicates, regardless of whether
     * the data they are associated with is is readable by any particular user.
     * This should only be called on behalf of a system administrator. If
     * records are needed by a non-administrative user who must have explicit
     * read rights, use searchReadable() instead.
     *
     * @param terms
     *     The search terms that must match the returned records.
     *
     * @param sortPredicates
     *     A list of predicates to sort the returned records by, in order of
     *     priority.
     *
     * @param limit
     *     The maximum number of records that should be returned.
     *
     * @return
     *     The results of the search performed with the given parameters.
     */
    List<ConnectionRecordModel> search(@Param("terms") Collection<ActivityRecordSearchTerm> terms,
            @Param("sortPredicates") List<ActivityRecordSortPredicate> sortPredicates,
            @Param("limit") int limit);

    /**
     * Searches for up to <code>limit</code> connection records that contain
     * the given terms, sorted by the given predicates. Only records that are
     * associated with data explicitly readable by the given user will be
     * returned. If records are needed by a system administrator (who, by
     * definition, does not need explicit read rights), use search() instead.
     *
     * @param user
     *    The user whose permissions should determine whether a record is
     *    returned.
     *
     * @param terms
     *     The search terms that must match the returned records.
     *
     * @param sortPredicates
     *     A list of predicates to sort the returned records by, in order of
     *     priority.
     *
     * @param limit
     *     The maximum number of records that should be returned.
     *
     * @return
     *     The results of the search performed with the given parameters.
     */
    List<ConnectionRecordModel> searchReadable(@Param("user") UserModel user,
            @Param("terms") Collection<ActivityRecordSearchTerm> terms,
            @Param("sortPredicates") List<ActivityRecordSortPredicate> sortPredicates,
            @Param("limit") int limit);

}
