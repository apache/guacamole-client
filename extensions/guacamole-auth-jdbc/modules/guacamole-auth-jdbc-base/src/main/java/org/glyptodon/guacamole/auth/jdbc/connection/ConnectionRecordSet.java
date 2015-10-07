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

package org.glyptodon.guacamole.auth.jdbc.connection;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.auth.jdbc.base.RestrictedObject;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;

/**
 * A JDBC implementation of ConnectionRecordSet. Calls to asCollection() will 
 * query connection history records from the database. Which records are
 * returned will be determined by the values passed in earlier.
 * 
 * @author James Muehlner
 */
public class ConnectionRecordSet extends RestrictedObject
        implements org.glyptodon.guacamole.net.auth.ConnectionRecordSet {

    /**
     * Mapper for accessing connection history.
     */
    @Inject
    private ConnectionRecordMapper connectionRecordMapper;
    
    /**
     * The set of strings that each must occur somewhere within the returned 
     * connection records, whether within the associated username, the name of 
     * the associated connection, or any associated date. If non-empty, any 
     * connection record not matching each of the strings within the collection 
     * will be excluded from the results.
     */
    private final Set<ConnectionRecordSearchTerm> requiredContents = 
            new HashSet<ConnectionRecordSearchTerm>();
    
    /**
     * The maximum number of connection history records that should be returned
     * by a call to asCollection().
     */
    private int limit = Integer.MAX_VALUE;
    
    /**
     * A list of predicates to apply while sorting the resulting connection
     * records, describing the properties involved and the sort order for those 
     * properties.
     */
    private final List<ConnectionRecordSortPredicate> connectionRecordSortPredicates =
            new ArrayList<ConnectionRecordSortPredicate>();
    
    @Override
    public Collection<ConnectionRecord> asCollection()
            throws GuacamoleException {

        // Perform the search against the database
        List<ConnectionRecordModel> searchResults =
                connectionRecordMapper.search(requiredContents,
                        connectionRecordSortPredicates, limit);

        List<ConnectionRecord> modeledSearchResults = 
                new ArrayList<ConnectionRecord>();

        // Convert raw DB records into ConnectionRecords
        for(ConnectionRecordModel model : searchResults) {
            modeledSearchResults.add(new ModeledConnectionRecord(model));
        }

        return modeledSearchResults;

    }

    @Override
    public ConnectionRecordSet contains(String value)
            throws GuacamoleException {
        requiredContents.add(new ConnectionRecordSearchTerm(value));
        return this;
    }

    @Override
    public ConnectionRecordSet limit(int limit) throws GuacamoleException {
        this.limit = Math.min(this.limit, limit);
        return this;
    }

    @Override
    public ConnectionRecordSet sort(SortableProperty property, boolean desc)
            throws GuacamoleException {
        
        connectionRecordSortPredicates.add(new ConnectionRecordSortPredicate(
            property,
            desc
        ));
        
        return this;

    }

}
