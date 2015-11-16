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

package org.glyptodon.guacamole.net.basic.rest.history;

import org.glyptodon.guacamole.net.auth.ConnectionRecordSet;
import org.glyptodon.guacamole.net.basic.rest.APIError;
import org.glyptodon.guacamole.net.basic.rest.APIException;

/**
 * A sort predicate which species the property to use when sorting connection
 * records, along with the sort order.
 *
 * @author Michael Jumper
 */
public class APIConnectionRecordSortPredicate {

    /**
     * The prefix which will be included before the name of a sortable property
     * to indicate that the sort order is descending, not ascending.
     */
    public static final String DESCENDING_PREFIX = "-";

    /**
     * All possible property name strings and their corresponding
     * ConnectionRecordSet.SortableProperty values.
     */
    public enum SortableProperty {

        /**
         * The date that the connection associated with the connection record
         * began (connected).
         */
        startDate(ConnectionRecordSet.SortableProperty.START_DATE);

        /**
         * The ConnectionRecordSet.SortableProperty that this property name
         * string represents.
         */
        public final ConnectionRecordSet.SortableProperty recordProperty;

        /**
         * Creates a new SortableProperty which associates the property name
         * string (identical to its own name) with the given
         * ConnectionRecordSet.SortableProperty value.
         *
         * @param recordProperty
         *     The ConnectionRecordSet.SortableProperty value to associate with
         *     the new SortableProperty.
         */
        SortableProperty(ConnectionRecordSet.SortableProperty recordProperty) {
            this.recordProperty = recordProperty;
        }

    }

    /**
     * The property to use when sorting ConnectionRecords.
     */
    private ConnectionRecordSet.SortableProperty property;

    /**
     * Whether the requested sort order is descending (true) or ascending
     * (false).
     */
    private boolean descending;

    /**
     * Parses the given string value, determining the requested sort property
     * and ordering. Possible values consist of any valid property name, and
     * may include an optional prefix to denote descending sort order. Each
     * possible property name is enumerated by the SortableValue enum.
     *
     * @param value
     *     The sort predicate string to parse, which must consist ONLY of a
     *     valid property name, possibly preceded by the DESCENDING_PREFIX.
     *
     * @throws APIException
     *     If the provided sort predicate string is invalid.
     */
    public APIConnectionRecordSortPredicate(String value)
        throws APIException {

        // Parse whether sort order is descending
        if (value.startsWith(DESCENDING_PREFIX)) {
            descending = true;
            value = value.substring(DESCENDING_PREFIX.length());
        }

        // Parse sorting property into ConnectionRecordSet.SortableProperty
        try {
            this.property = SortableProperty.valueOf(value).recordProperty;
        }

        // Bail out if sort property is not valid
        catch (IllegalArgumentException e) {
            throw new APIException(
                APIError.Type.BAD_REQUEST,
                String.format("Invalid sort property: \"%s\"", value)
            );
        }

    }

    /**
     * Returns the SortableProperty defined by ConnectionRecordSet which
     * represents the property requested.
     *
     * @return
     *     The ConnectionRecordSet.SortableProperty which refers to the same
     *     property as the string originally provided when this
     *     APIConnectionRecordSortPredicate was created.
     */
    public ConnectionRecordSet.SortableProperty getProperty() {
        return property;
    }

    /**
     * Returns whether the requested sort order is descending.
     *
     * @return
     *     true if the sort order is descending, false if the sort order is
     *     ascending.
     */
    public boolean isDescending() {
        return descending;
    }

}
