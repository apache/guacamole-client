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

package org.glyptodon.guacamole.auth.jdbc.socket;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;


/**
 * Mapping of object identifiers to lists of connection records. Records are
 * added or removed individually, and the overall list of current records
 * associated with a given object can be retrieved at any time. The public
 * methods of this class are all threadsafe.
 *
 * @author Michael Jumper
 */
public class ActiveConnectionMultimap {

    /**
     * All active connections to a connection having a given identifier.
     */
    private final Map<String, Set<ConnectionRecord>> records =
            new HashMap<String, Set<ConnectionRecord>>();

    /**
     * Stores the given connection record in the list of active connections
     * associated with the object having the given identifier.
     *
     * @param identifier
     *     The identifier of the object being connected to.
     *
     * @param record
     *     The record associated with the active connection.
     */
    public void put(String identifier, ConnectionRecord record) {
        synchronized (records) {

            // Get set of active connection records, creating if necessary
            Set<ConnectionRecord> connections = records.get(identifier);
            if (connections == null) {
                connections = Collections.newSetFromMap(new LinkedHashMap<ConnectionRecord, Boolean>());
                records.put(identifier, connections);
            }

            // Add active connection
            connections.add(record);

        }
    }

    /**
     * Removes the given connection record from the list of active connections
     * associated with the object having the given identifier.
     *
     * @param identifier
     *     The identifier of the object being disconnected from.
     *
     * @param record
     *     The record associated with the active connection.
     */
    public void remove(String identifier, ConnectionRecord record) {
        synchronized (records) {

            // Get set of active connection records
            Set<ConnectionRecord> connections = records.get(identifier);
            assert(connections != null);

            // Remove old record
            connections.remove(record);

            // If now empty, clean the tracking entry
            if (connections.isEmpty())
                records.remove(identifier);

        }
    }

    /**
     * Returns a collection of active connection records associated with the
     * object having the given identifier. The collection will be sorted in
     * insertion order. If there are no such connections, an empty collection is
     * returned.
     *
     * @param identifier
     *     The identifier of the object to check.
     *
     * @return
     *     An immutable collection of records associated with the object having
     *     the given identifier, or an empty collection if there are no such
     *     records.
     */
    public Collection<ConnectionRecord> get(String identifier) {
        synchronized (records) {

            // Get set of active connection records
            Collection<ConnectionRecord> connections = records.get(identifier);
            if (connections != null)
                return Collections.unmodifiableCollection(connections);

            return Collections.EMPTY_LIST;

        }
    }

}
