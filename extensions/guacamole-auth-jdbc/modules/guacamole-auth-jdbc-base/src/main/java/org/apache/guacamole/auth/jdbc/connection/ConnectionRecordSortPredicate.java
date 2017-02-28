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

import org.apache.guacamole.net.auth.ConnectionRecordSet;

/**
 * A sort predicate which species the property to use when sorting connection
 * records, along with the sort order.
 */
public class ConnectionRecordSortPredicate {

    /**
     * The property to use when sorting ConnectionRecords.
     */
    private final ConnectionRecordSet.SortableProperty property;

    /**
     * Whether the sort order is descending (true) or ascending (false).
     */
    private final boolean descending;
    
    /**
     * Creates a new ConnectionRecordSortPredicate with the given sort property 
     * and sort order.
     * 
     * @param property 
     *     The property to use when sorting ConnectionRecords.
     * 
     * @param descending 
     *     Whether the sort order is descending (true) or ascending (false).
     */
    public ConnectionRecordSortPredicate(ConnectionRecordSet.SortableProperty property, 
            boolean descending) {
        this.property   = property;
        this.descending = descending;
    }
    
    /**
     * Returns the property that should be used when sorting ConnectionRecords.
     *
     * @return
     *     The property that should be used when sorting ConnectionRecords.
     */
    public ConnectionRecordSet.SortableProperty getProperty() {
        return property;
    }

    /**
     * Returns whether the sort order is descending.
     *
     * @return
     *     true if the sort order is descending, false if the sort order is
     *     ascending.
     */
    public boolean isDescending() {
        return descending;
    }
    
}
