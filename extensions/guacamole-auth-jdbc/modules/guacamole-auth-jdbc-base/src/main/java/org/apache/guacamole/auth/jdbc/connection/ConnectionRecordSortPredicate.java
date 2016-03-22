/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.apache.guacamole.auth.jdbc.connection;

import org.apache.guacamole.net.auth.ConnectionRecordSet;

/**
 * A sort predicate which species the property to use when sorting connection
 * records, along with the sort order.
 *
 * @author James Muehlner
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
