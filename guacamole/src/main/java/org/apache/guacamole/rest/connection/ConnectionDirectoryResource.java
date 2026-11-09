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

package org.apache.guacamole.rest.connection;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.ReachabilityProbe;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.Permissions;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.rest.directory.DirectoryObjectResourceFactory;
import org.apache.guacamole.rest.directory.DirectoryObjectTranslator;
import org.apache.guacamole.rest.directory.DirectoryResource;

/**
 * A REST resource which abstracts the operations available on a Directory of
 * Connections.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConnectionDirectoryResource
        extends DirectoryResource<Connection, APIConnection> {

    /**
     * Creates a new ConnectionDirectoryResource which exposes the operations
     * and subresources available for the given Connection Directory.
     *
     * @param authenticatedUser
     *     The user that is accessing this resource.
     *
     * @param userContext
     *     The UserContext associated with the given Directory.
     *
     * @param directory
     *     The Directory being exposed.
     *
     * @param translator
     *     A DirectoryObjectTranslator implementation which handles
     *     Connections.
     *
     * @param resourceFactory
     *     A factory which can be used to create instances of resources
     *     representing Connections.
     */
    /**
     * Factory for creating ConnectionResource instances for individual connections.
     * Stored here because {@code getConnectionParameters()} on ConnectionResource is
     * the only path that correctly loads connection parameters from the backend
     * (hostname, port, etc.). Calling {@code connection.getConfiguration()} directly
     * from the directory returns empty parameters.
     */
    private final DirectoryObjectResourceFactory<Connection, APIConnection> resourceFactory;

    @AssistedInject
    public ConnectionDirectoryResource(@Assisted AuthenticatedUser authenticatedUser,
            @Assisted UserContext userContext, @Assisted Directory<Connection> directory,
            DirectoryObjectTranslator<Connection, APIConnection> translator,
            DirectoryObjectResourceFactory<Connection, APIConnection> resourceFactory) {
        super(authenticatedUser, userContext, Connection.class, directory, translator, resourceFactory);
        this.resourceFactory = resourceFactory;
    }

    @Override
    protected ObjectPermissionSet getObjectPermissions(Permissions permissions)
            throws GuacamoleException {
        return permissions.getConnectionPermissions();
    }

    /**
     * Returns the default TCP port for the given Guacamole protocol.
     * Used when a connection does not have the "port" parameter configured.
     *
     * @param protocol  Guacamole protocol name (e.g. "rdp", "ssh").
     * @return Default TCP port, or -1 if the protocol is not recognised.
     */
    private static int defaultPortFor(String protocol) {
        if (protocol == null) return -1;
        switch (protocol.toLowerCase()) {
            case "rdp":        return 3389;
            case "ssh":        return 22;
            case "vnc":        return 5900;
            case "telnet":     return 23;
            case "kubernetes": return 8080;
            default:           return -1;
        }
    }

    /**
     * Returns a map indicating whether the host of each specified connection
     * responds on its configured TCP port.
     *
     * Only connections to which the current user has READ permission are probed.
     * Connections that do not exist, are inaccessible, or have no hostname
     * configured are silently omitted from the result.
     *
     * Example requests:
     *   GET /api/session/data/mysql/connections/reachable?ids=1&amp;ids=2&amp;ids=3
     *   GET /api/session/data/mysql/connections/reachable?ids=1,2,3
     *
     * Example response:
     *   {"1": true, "2": false, "3": true}
     *
     * TCP is used (rather than ICMP/ping) because Windows blocks ICMP by default
     * while keeping the RDP port open when the machine is running.
     *
     * @param ids  Connection identifiers to check. Both repeated parameters
     *             ({@code ids=1&ids=2}) and comma-separated values
     *             ({@code ids=1,2,3}) are accepted.
     * @return Map of connection ID (String) to boolean (true = reachable via TCP).
     * @throws GuacamoleException If the directory cannot be accessed.
     */
    @GET
    @Path("reachable")
    public Map<String, Boolean> getReachable(
            @QueryParam("ids") List<String> ids)
            throws GuacamoleException {

        if (ids == null || ids.isEmpty())
            return Collections.emptyMap();

        // Normalize: flatten possible comma-separated values (ids=1,2,3)
        List<String> flatIds = new ArrayList<>();
        for (String idParam : ids) {
            for (String id : idParam.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty())
                    flatIds.add(trimmed);
            }
        }

        List<ReachabilityProbe.Target> targets = new ArrayList<>();
        Directory<Connection> dir = getDirectory();

        for (String id : flatIds) {

            Connection connection;
            try {
                // Verify the connection exists and the user has READ access.
                // Returns null if not found; throws if permission is denied.
                connection = dir.get(id);
            } catch (GuacamoleException e) {
                continue;
            }

            if (connection == null)
                continue;

            // NOTE: connection.getConfiguration().getParameter() returns null here
            // because the MySQL backend does not load parameters when listing connections
            // (only when establishing a tunnel). ConnectionResource.getConnectionParameters()
            // uses the same path as GET /connections/{id}/parameters and does load them.
            Map<String, String> params;
            try {
                ConnectionResource connRes = (ConnectionResource) resourceFactory.create(
                    getAuthenticatedUser(), getUserContext(), dir, connection);
                params = connRes.getConnectionParameters();
            } catch (GuacamoleException e) {
                // Insufficient permission to read parameters (UPDATE or ADMINISTER required)
                continue;
            }

            String hostname = params.get("hostname");
            if (hostname == null || hostname.isEmpty())
                continue;

            String portStr = params.get("port");
            int port;
            try {
                port = (portStr != null && !portStr.isEmpty())
                    ? Integer.parseInt(portStr)
                    : defaultPortFor(connection.getConfiguration().getProtocol());
            } catch (NumberFormatException e) {
                port = defaultPortFor(connection.getConfiguration().getProtocol());
            }

            if (port <= 0 || port > 65535)
                continue;

            targets.add(new ReachabilityProbe.Target(id, hostname, port));
        }

        // Run TCP probes in parallel and return the results
        return ReachabilityProbe.probe(targets);
    }

}
