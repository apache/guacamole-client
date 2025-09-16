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

package org.apache.guacamole.vault.hv.user;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.DecoratingDirectory;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.apache.guacamole.vault.hv.user.HvConnectionGroup;
import org.apache.guacamole.vault.user.VaultDirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HV-specific vault directory service that wraps the connection group directory
 * to sanitize sensitive data from exposed settings.
 */
public class HvDirectoryService extends VaultDirectoryService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(HvDirectoryService.class);

    /**
     * A factory for constructing new HvUser instances.
     */
    @Inject
    private HvUserFactory hvUserFactory;

    /**
     * Service for retrieving any custom attributes defined for the
     * current vault implementation and processing of said attributes.
     */
    @Inject
    private HvAttributeService attributeService;

    @Override
    public Directory<Connection> getConnectionDirectory(Directory<Connection> underlyingDirectory) throws GuacamoleException {

        return new DecoratingDirectory<Connection>(underlyingDirectory) {

            @Override
            protected Connection decorate(Connection connection) throws GuacamoleException {
                return new HvConnection(connection);
            }

            @Override
            protected Connection undecorate(Connection connection) throws GuacamoleException {
                return ((HvConnection) connection).getUnderlyingConnection();
            }

        };
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory(Directory<ConnectionGroup> underlyingDirectory) throws GuacamoleException {
        // A ConnectionGroup directory that will intercept add and update calls to
        // validate HV configurations, and translate one-time-tokens, if possible,
        // as well as ensuring that all ConnectionGroups returned include the
        // HV_CONFIGURATION_ATTRIBUTE attribute, so it will be available in the UI.
        // The value of the HV_CONFIGURATION_ATTRIBUTE will be sanitized if set.
        return new HvDirectory<ConnectionGroup>(underlyingDirectory) {

            @Override
            public void add(ConnectionGroup connectionGroup) throws GuacamoleException {

                // Process attribute values before saving
                connectionGroup.setAttributes(connectionGroup.getAttributes());

                super.add(connectionGroup);
            }

            @Override
            public void update(ConnectionGroup connectionGroup) throws GuacamoleException {

                // Unwrap the existing ConnectionGroup
                if (connectionGroup instanceof HvConnectionGroup)
                    connectionGroup = ((HvConnectionGroup) connectionGroup).getUnderlyingConnectionGroup();

                // Process attribute values before saving
                connectionGroup.setAttributes(connectionGroup.getAttributes());

                super.update(connectionGroup);

            }

            @Override
            protected ConnectionGroup wrap(ConnectionGroup object) {

                // Do not process the ConnectionGroup further if it does not exist
                if (object == null)
                    return null;

                // Sanitize values when a ConnectionGroup is fetched from the directory
                return new HvConnectionGroup(object);

            }

        };
    }

    @Override
    public Directory<User> getUserDirectory(Directory<User> underlyingDirectory) throws GuacamoleException {
        // A User directory that will intercept add and update calls to
        // validate HV configurations, and translate one-time-tokens, if possible
        // Additionally, this directory will will decorate all users with a
        // HvUser wrapper to ensure that all defined HV fields will be exposed
        // in the user attributes.  The value of the HV_CONFIGURATION_ATTRIBUTE
        // will be sanitized if set.
        return new HvDirectory<User>(underlyingDirectory) {

            @Override
            public void add(User user) throws GuacamoleException {

                // Process attribute values before saving
                user.setAttributes(user.getAttributes());

                super.add(user);
            }

            @Override
            public void update(User user) throws GuacamoleException {

                // Unwrap the existing user
                if (user instanceof HvUser)
                    user = ((HvUser) user).getUnderlyingUser();

                // Process attribute values before saving
                user.setAttributes(user.getAttributes());

                super.update(user);

            }

            @Override
            protected User wrap(User object) {

                // Do not process the user further if it does not exist
                if (object == null)
                    return null;

                // Sanitize values when a user is fetched from the directory
                return hvUserFactory.create(object);

            }

        };
    }

}
