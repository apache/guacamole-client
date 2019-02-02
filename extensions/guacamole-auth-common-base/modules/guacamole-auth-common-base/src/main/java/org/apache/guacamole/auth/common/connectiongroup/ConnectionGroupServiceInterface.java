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

package org.apache.guacamole.auth.common.connectiongroup;

import java.util.Map;
import java.util.Set;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating connection groups.
 */
public interface ConnectionGroupServiceInterface {

    /**
     * Returns the set of all identifiers for all connection groups within the
     * connection group having the given identifier. Only connection groups that
     * the user has read access to will be returned.
     * 
     * Permission to read the connection group having the given identifier is
     * NOT checked.
     *
     * @param user
     *            The user retrieving the identifiers.
     * 
     * @param identifier
     *            The identifier of the parent connection group, or null to
     *            check the root connection group.
     *
     * @return The set of all identifiers for all connection groups in the
     *         connection group having the given identifier that the user has
     *         read access to.
     *
     * @throws GuacamoleException
     *             If an error occurs while reading identifiers.
     */
    public Set<String> getIdentifiersWithin(ModeledAuthenticatedUser user,
            String identifier) throws GuacamoleException;

    /**
     * Connects to the given connection group as the given user, using the given
     * client information. If the user does not have permission to read the
     * connection group, permission will be denied.
     *
     * @param user
     *            The user connecting to the connection group.
     *
     * @param connectionGroup
     *            The connectionGroup being connected to.
     *
     * @param info
     *            Information associated with the connecting client.
     *
     * @return A connected GuacamoleTunnel associated with a newly-established
     *         connection.
     *
     * @throws GuacamoleException
     *             If permission to connect to this connection is denied.
     */
    public GuacamoleTunnel connect(ModeledAuthenticatedUser user,
            ModeledConnectionGroup connectionGroup,
            GuacamoleClientInformation info, Map<String, String> tokens)
            throws GuacamoleException;

}
