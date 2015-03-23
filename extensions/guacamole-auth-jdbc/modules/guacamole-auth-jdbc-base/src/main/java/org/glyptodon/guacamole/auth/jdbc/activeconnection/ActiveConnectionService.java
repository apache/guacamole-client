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

package org.glyptodon.guacamole.auth.jdbc.activeconnection;

import com.google.inject.Inject;
import com.google.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleSecurityException;
import org.glyptodon.guacamole.auth.jdbc.base.DirectoryObjectService;
import org.glyptodon.guacamole.auth.jdbc.tunnel.ActiveConnectionRecord;
import org.glyptodon.guacamole.auth.jdbc.tunnel.GuacamoleTunnelService;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.auth.ActiveConnection;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating active connections.
 *
 * @author Michael Jumper
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
    public TrackedActiveConnection retrieveObject(AuthenticatedUser user,
            String identifier) throws GuacamoleException {

        // Only administrators may retrieve active connections
        if (!user.getUser().isAdministrator())
            throw new GuacamoleSecurityException("Permission denied.");

        // Retrieve record associated with requested connection
        ActiveConnectionRecord record = tunnelService.getActiveConnection(user, identifier);
        if (record == null)
            return null;

        // Return tracked active connection using retrieved record
        TrackedActiveConnection activeConnection = trackedActiveConnectionProvider.get();
        activeConnection.init(user, record);
        return activeConnection;
        
    }
    
    @Override
    public Collection<TrackedActiveConnection> retrieveObjects(AuthenticatedUser user,
            Collection<String> identifiers) throws GuacamoleException {

        // Build list of all active connections with given identifiers
        Collection<TrackedActiveConnection> activeConnections = new ArrayList<TrackedActiveConnection>(identifiers.size());
        for (String identifier : identifiers) {

            // Add connection to list if it exists
            TrackedActiveConnection activeConnection = retrieveObject(user, identifier);
            if (activeConnection != null)
                activeConnections.add(activeConnection);
            
        }

        return activeConnections;
        
    }

    @Override
    public void deleteObject(AuthenticatedUser user, String identifier)
        throws GuacamoleException {

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
    public Set<String> getIdentifiers(AuthenticatedUser user)
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
    public TrackedActiveConnection createObject(AuthenticatedUser user,
            ActiveConnection object) throws GuacamoleException {

        // Updating active connections is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

    @Override
    public void updateObject(AuthenticatedUser user, TrackedActiveConnection object)
            throws GuacamoleException {

        // Updating active connections is not implemented
        throw new GuacamoleSecurityException("Permission denied.");

    }

}
