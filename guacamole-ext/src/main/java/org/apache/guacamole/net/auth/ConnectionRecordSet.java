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

package org.apache.guacamole.net.auth;

import java.util.Collection;
import org.apache.guacamole.GuacamoleException;

/**
 * The set of all available connection records, or a subset of those records.
 */
public interface ConnectionRecordSet {

    /**
     * All properties of connection records which can be used as sorting
     * criteria.
     */
    enum SortableProperty {

        /**
         * The date and time when the connection associated with the
         * connection record began.
         */
        START_DATE

    };

    /**
     * Returns all connection records within this set as a standard Collection.
     *
     * @return
     *      A collection containing all connection records within this set.
     *
     * @throws GuacamoleException
     *      If an error occurs while retrieving the connection records within
     *      this set.
     */
    Collection<ConnectionRecord> asCollection() throws GuacamoleException;

    /**
     * Returns the subset of connection records to only those where the
     * connection name, user identifier, or any associated date field contain
     * the given value. This function may also affect the contents of the
     * current ConnectionRecordSet. The contents of the current
     * ConnectionRecordSet should NOT be relied upon after this function is
     * called.
     *
     * @param value
     *     The value which all connection records within the resulting subset
     *     should contain within their associated connection name or user
     *     identifier.
     *
     * @return
     *     The subset of connection history records which contain the specified
     *     value within their associated connection name or user identifier.
     *
     * @throws GuacamoleException
     *     If an error occurs while restricting the current subset.
     */
    ConnectionRecordSet contains(String value) throws GuacamoleException;

    /**
     * Returns the subset of connection history records containing only the
     * first <code>limit</code> records. If the subset has fewer than
     * <code>limit</code> records, then this function has no effect. This
     * function may also affect the contents of the current
     * ConnectionRecordSet. The contents of the current ConnectionRecordSet
     * should NOT be relied upon after this function is called.
     *
     * @param limit
     *     The maximum number of records that the new subset should contain.
     *
     * @return
     *     The subset of connection history records that containing only the
     *     first <code>limit</code> records.
     *
     * @throws GuacamoleException
     *     If an error occurs while limiting the current subset.
     */
    ConnectionRecordSet limit(int limit) throws GuacamoleException;

    /**
     * Returns a ConnectionRecordSet containing identically the records within
     * this set, sorted according to the specified criteria. The sort operation
     * performed is guaranteed to be stable with respect to any past call to
     * sort(). This function may also affect the contents of the current
     * ConnectionRecordSet. The contents of the current ConnectionRecordSet
     * should NOT be relied upon after this function is called.
     *
     * @param property
     *     The property by which the connection records within the resulting
     *     set should be sorted.
     *
     * @param desc
     *     Whether the records should be sorted according to the specified
     *     property in descending order. If false, records will be sorted
     *     according to the specified property in ascending order.
     *
     * @return
     *     The ConnnectionRecordSet, sorted according to the specified
     *     criteria.
     *
     * @throws GuacamoleException
     *     If an error occurs while sorting the current subset.
     */
    ConnectionRecordSet sort(SortableProperty property, boolean desc)
            throws GuacamoleException;

}
