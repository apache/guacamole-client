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

package org.apache.guacamole.auth.jdbc.activeconnection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.guacamole.auth.jdbc.user.ModeledAuthenticatedUser;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.jdbc.base.DirectoryObjectService;
import org.apache.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;
import org.apache.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.auth.ActiveConnection;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating active connections.
 */
public class ActiveConnectionService
    implements DirectoryObjectService<TrackedActiveConnection, ActiveConnection> { 

    /**
     * Service for creating and tracking tunnels.
     */
    @Inject
    private GuacamoleTunnelService tunnelService;

    /**
     * Provider for active connections.
     */
    @Inject
    private Provider<TrackedActiveConnection> trackedActiveConnectionProvider;
    
    @Override
    public TrackedActiveConnection retrieveObject(ModeledAuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Pull objects having given identifier
        Collection<TrackedActiveConnection> objects = retrieveObjects(user, Collections.singleton(identifier));

        // If no such object, return null
        if (objects.isEmpty())
            return null;

        // The object collection will have exactly one element unless the
        // database has seriously lost integrity
        assert(objects.size() == 1);

        // Return first and only object
        return objects.iterator().next();

    }
    
    @Override
    public Collection<TrackedActiveConnection> retrieveObjects(ModeledAuthenticatedUser user,
            Collection<String> identifiers) throws GuacamoleException {

        String username = user.getIdentifier();
        boolean isAdmin = user.getUser().isAdministrator();
        Set<String> identifierSet = new HashSet<String>(identifiers);

        // Retrieve all visible connections (permissions enforced by tunnel service)
        Collection<ActiveConnectionRecord> records = tunnelService.getActiveConnections(user);

        // Restrict to subset of records which match given identifiers
        Collection<TrackedActiveConnection> activeConnections = new ArrayList<TrackedActiveConnection>(identifiers.size());
        for (ActiveConnectionRecord record : records) {

            // Sensitive information should be included if the connection was
            // started by the current user OR the user is an admin
            boolean includeSensitiveInformation =
                    isAdmin || username.equals(record.getUsername());

            // Add connection if within requested identifiers
            if (identifierSet.contains(record.getUUID().toString())) {
                TrackedActiveConnection activeConnection = trackedActiveConnectionProvider.get();
                activeConnection.init(user, record, includeSensitiveInformation);
                activeConnections.add(activeConnection);
            }

        }

        return activeConnections;
        
    }

    @Override
    public void deleteObject(ModeledAuthenticatedUser user, String identifier)
        throws GuacamoleException {

        // Only administrators may delete active connections
        if (!user.getUser().isAdministrator())
            throw new GuacamoleSecurityException("Permission denied.");

        // Close connection, if it exists (and we have permission)
        ActiveConnection activeConnection = retrieveObject(user, identifier);
        if (activeConnection != null) {

            // Close connection if not already closed
            GuacamoleTunnel tunnel = activeConnection.getTunnel();
            if (tunnel != null && tunnel.isOpen())
                tunnel.close();

        }
        
    }

    @Override
    public Set<String> getIdentifiers(ModeledAuthenticatedUser user)
        throws GuacamoleException {

        // Retrieve all visible connections (permissions enforced by tunnel service)
        Collection<ActiveConnectionRecord> records = tunnelService.getActiveConnections(user);

        // Build list of identifiers
        Set<String> identifiers = new HashSet<String>(records.size());
        for (ActiveConnectionRecord record : records)
            identifiers.add(record.getUUID().toString());

        return identifiers;
        
    }

    @Override
    public TrackedActiveConnection createObject(ModeledAuthenticatedUser user,
            ActiveConnection object) throws GuacamoleException {

        // Updating active connections is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public void updateObject(ModeledAuthenticatedUser user, TrackedActiveConnection object)
            throws GuacamoleException {

        // Updating active connections is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
