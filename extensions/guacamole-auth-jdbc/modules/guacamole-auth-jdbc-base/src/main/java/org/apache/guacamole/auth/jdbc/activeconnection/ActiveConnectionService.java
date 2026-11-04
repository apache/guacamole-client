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
import java.util.Date;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.cluster.ClusterStore;
import org.apache.guacamole.cluster.TunnelRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.guacamole.auth.jdbc.connection.ConnectionService;
import org.apache.guacamole.auth.jdbc.connection.ModeledConnection;
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
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating active connections.
 */
public class ActiveConnectionService
    implements DirectoryObjectService<TrackedActiveConnection, ActiveConnection> { 

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ActiveConnectionService.class);

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

    /**
     * The cluster store, providing a cluster-wide view of active tunnels and
     * the means of asking another replica to close one. When clustering is
     * disabled this store lists no tunnels, in which case this service
     * behaves exactly as it would without clustering.
     */
    @Inject
    private ClusterStore clusterStore;

    /**
     * Service for loading the connection behind a session owned by another
     * replica. The database is shared by every replica, so the connection is
     * available locally even when the session is not.
     */
    @Inject
    private ConnectionService connectionService;

    /**
     * The number of milliseconds to wait for another replica to honour a
     * request to close one of its sessions before reporting failure.
     */
    private static final long KILL_TIMEOUT_MS = 2000L;

    /**
     * The number of milliseconds between checks of whether a session that
     * another replica was asked to close has been removed from the cluster.
     */
    private static final long KILL_POLL_INTERVAL_MS = 100L;

    /**
     * Builds an administrative view of a session owned by another replica,
     * populated from the registration that replica published to the cluster.
     * The returned object has no tunnel and cannot be joined; it exists so
     * that the session can be listed and closed from any replica.
     *
     * @param user
     *     The user requesting the listing.
     *
     * @param registration
     *     The remote tunnel, as published to the cluster.
     *
     * @param hasPrivilegedAccess
     *     Whether sensitive fields (the username and remote host of the user
     *     who owns the session) should be populated.
     *
     * @return
     *     A tracked connection describing the remote session, with no tunnel.
     */
    private TrackedActiveConnection remoteActiveConnection(ModeledAuthenticatedUser user,
            TunnelRegistration registration, boolean hasPrivilegedAccess) {

        // Initialise without a local record, then populate every field from
        // the cluster registration
        TrackedActiveConnection activeConnection = trackedActiveConnectionProvider.get();
        activeConnection.init(user, null, false, false);

        activeConnection.setIdentifier(registration.getRecordUuid());
        activeConnection.setSharingProfileIdentifier(registration.getSharingProfileIdentifier());

        // getConnectionIdentifier() reads through to the connection, and
        // setConnectionIdentifier() deliberately throws, so the real connection
        // has to be supplied. It is in the shared database regardless of which
        // replica owns the session.
        try {
            activeConnection.setConnection(
                    connectionService.retrieveObject(user,
                            registration.getConnectionIdentifier()));
        }
        catch (GuacamoleException e) {
            logger.debug("Connection \"{}\" behind a remote session could not "
                    + "be read; it will be listed without connection detail.",
                    registration.getConnectionIdentifier(), e);
        }
        activeConnection.setStartDate(new Date(registration.getStartTime()));

        // A session on another replica cannot be joined from this listing, as
        // joining requires that replica's live tunnel, so it is never marked
        // connectable and its tunnel stays null
        activeConnection.setTunnel(null);

        if (hasPrivilegedAccess) {
            activeConnection.setUsername(registration.getUsername());
            activeConnection.setRemoteHost(registration.getRemoteHost());
        }

        return activeConnection;

    }
    
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
        boolean isPrivileged = user.isPrivileged();
        Set<String> identifierSet = new HashSet<String>(identifiers);

        // Retrieve all visible connections (permissions enforced by tunnel service)
        Collection<ActiveConnectionRecord> records = tunnelService.getActiveConnections(user);

        // Restrict to subset of records which match given identifiers
        Collection<TrackedActiveConnection> activeConnections = new ArrayList<TrackedActiveConnection>(identifiers.size());
        for (ActiveConnectionRecord record : records) {

            // The current user should have access to sensitive information and
            // be able to connect to (join) the active connection if they are
            // the user that started the connection OR the user is an admin
            boolean hasPrivilegedAccess =
                    isPrivileged || username.equals(record.getUsername());

            // Add connection if within requested identifiers
            if (identifierSet.contains(record.getUUID().toString())) {
                TrackedActiveConnection activeConnection = trackedActiveConnectionProvider.get();
                activeConnection.init(user, record, hasPrivilegedAccess, hasPrivilegedAccess);
                activeConnections.add(activeConnection);
            }

        }

        // Add sessions owned by other replicas, skipping any already covered
        // by a local record
        Set<String> localIdentifiers = new HashSet<String>();
        for (TrackedActiveConnection local : activeConnections)
            localIdentifiers.add(local.getIdentifier());

        for (TunnelRegistration registration : clusterStore.listTunnels()) {

            // Skip registrations without an identifier, and sessions owned by
            // this replica, which were already added above
            String recordUuid = registration.getRecordUuid();
            if (recordUuid == null || localIdentifiers.contains(recordUuid))
                continue;

            // Skip sessions which were not requested
            if (!identifierSet.contains(recordUuid))
                continue;

            // As with local sessions, only privileged users and the owner of
            // a session may see it
            boolean hasPrivilegedAccess =
                    isPrivileged || username.equals(registration.getUsername());

            if (hasPrivilegedAccess)
                activeConnections.add(
                        remoteActiveConnection(user, registration, hasPrivilegedAccess));

        }

        return activeConnections;
        
    }

    @Override
    public void deleteObject(ModeledAuthenticatedUser user, String identifier)
        throws GuacamoleException {

        // Close connection, if it exists and we have permission
        ActiveConnection activeConnection = retrieveObject(user, identifier);
        if (activeConnection == null)
            return;
        
        if (hasObjectPermissions(user, identifier, ObjectPermission.Type.DELETE)) {

            // Close connection if not already closed
            GuacamoleTunnel tunnel = activeConnection.getTunnel();
            if (tunnel != null && tunnel.isOpen()) {
                tunnel.close();
                return;
            }

            // A session owned by another replica has no tunnel here. Ask every
            // replica to close it, then wait for the cluster entry to go, so
            // this call never reports success for a kill that did not land.
            String seatToken = clusterStore.lookupSeatToken(identifier);
            if (seatToken == null)
                return;

            clusterStore.requestKill(identifier);

            // Poll until the owning replica removes the tunnel's cluster entry,
            // failing if it has not done so within the timeout
            long deadline = System.currentTimeMillis() + KILL_TIMEOUT_MS;
            while (System.currentTimeMillis() < deadline) {

                if (!clusterStore.isTunnelLive(seatToken))
                    return;

                try {
                    Thread.sleep(KILL_POLL_INTERVAL_MS);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

            }

            throw new GuacamoleServerException("The replica owning this "
                    + "connection did not close it in time.");

        }
        else
            throw new GuacamoleSecurityException("Permission denied.");
        
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

        // Include sessions owned by other replicas. Privileged users see the
        // whole cluster; unprivileged users see only their own. A locally-owned
        // session appears in both sources and is collapsed by the Set.
        for (TunnelRegistration registration : clusterStore.listTunnels()) {

            if (registration.getRecordUuid() == null)
                continue;

            if (user.isPrivileged()
                    || user.getIdentifier().equals(registration.getUsername()))
                identifiers.add(registration.getRecordUuid());

        }

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

    /**
     * Retrieve the permission set for the specified user that relates
     * to access to active connections.
     * 
     * @param user
     *     The user for which to retrieve the permission set.
     * 
     * @return
     *     A permission set associated with the given user that specifies
     *     the permissions available for active connection objects.
     * 
     * @throws GuacamoleException
     *     If permission to read permissions for the user is denied.
     */
    private ObjectPermissionSet getPermissionSet(ModeledAuthenticatedUser user) 
            throws GuacamoleException {
        return user.getUser().getActiveConnectionPermissions();
    }

    /**
     * Return a boolean value representing whether or not a user has the given
     * permission available to them on the active connection with the given
     * identifier.
     * 
     * @param user
     *     The user for which the permissions are being queried.
     * 
     * @param identifier
     *     The identifier of the active connection we are wondering about.
     * 
     * @param type
     *     The type of permission being requested.
     * 
     * @return
     *     True if the user has the necessary permission; otherwise false.
     * 
     * @throws GuacamoleException 
     *     If the user does not have access to read permissions.
     */
    private boolean hasObjectPermissions(ModeledAuthenticatedUser user,
            String identifier, ObjectPermission.Type type)
            throws GuacamoleException {
        
        ObjectPermissionSet permissionSet = getPermissionSet(user);
        
        return user.isPrivileged()
                || permissionSet.hasPermission(type, identifier);
        
    }

}
