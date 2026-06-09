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
import org.apache.guacamole.vault.user.VaultDirectoryService;

/**
 * A HV-specific vault directory service that wraps the connection group directory
 * to sanitize sensitive data from exposed settings.
 */
public class HvDirectoryService extends VaultDirectoryService {

    /**
     * A factory for constructing new HvUser instances.
     */
    @Inject
    private HvUser.HvUserFactory hvUserFactory;

    /**
     * Service for retrieving any custom attributes defined for the
     * current vault implementation and processing of said attributes.
     */
    @Inject
    private HvAttributeService attributeService;

    @Override
    public Directory<Connection> getConnectionDirectory(final Directory<Connection> underlyingDirectory) throws GuacamoleException {

        return new DecoratingDirectory<Connection>(underlyingDirectory) {

            @Override
            protected Connection decorate(final Connection connection) throws GuacamoleException {
                return new HvConnection(connection);
            }

            @Override
            protected Connection undecorate(final Connection connection) throws GuacamoleException {
                return ((HvConnection) connection).getUnderlyingConnection();
            }

        };
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory(final Directory<ConnectionGroup> underlyingDirectory) throws GuacamoleException {
        // A ConnectionGroup directory that will intercept add and update calls to
        // validate HV configurations, and ensure that all ConnectionGroups returned 
        // include the HV_URI_ATTRIBUTE, HV_TOKEN_ATTRIBUTE, HV_USERNAME_ATTRIBUTE
        // and HV_PASSWORD_ATTRIBUTE attributes, so they will be available in the UI..
        return new HvDirectory<ConnectionGroup>(underlyingDirectory) {

            @Override
            public void add(final ConnectionGroup connectionGroup) throws GuacamoleException {

                // Process attribute values before saving
                connectionGroup.setAttributes(connectionGroup.getAttributes());

                super.add(connectionGroup);
            }

            @Override
            public void update(final ConnectionGroup connectionGroup) throws GuacamoleException {
                // Process attribute values before saving
                connectionGroup.setAttributes(connectionGroup.getAttributes());

                super.update(connectionGroup);
            }

            @Override
            protected ConnectionGroup wrap(final ConnectionGroup object) {
                // Sanitize values when a ConnectionGroup is fetched from the directory
                // Do not process the ConnectionGroup further if it does not exist
                return (object == null) ? null : new HvConnectionGroup(object);
            }

        };
    }

    @Override
    public Directory<User> getUserDirectory(final Directory<User> underlyingDirectory) throws GuacamoleException {
        // A User directory that will intercept add and update calls to
        // validate HV configurations. Additionally, this directory will will
        // decorate all users with a HvUser wrapper to ensure that all defined
        // HV fields will be exposed in the user attributes.
        return new HvDirectory<User>(underlyingDirectory) {

            @Override
            public void add(final User user) throws GuacamoleException {
                // Process attribute values before saving
                user.setAttributes(user.getAttributes());

                super.add(user);
            }

            @Override
            public void update(final User oldUser) throws GuacamoleException {
                User user = oldUser;

                // Unwrap the existing user
                if (user instanceof HvUser) {
                    user = ((HvUser) user).getUnderlyingUser();
                }

                // Process attribute values before saving
                user.setAttributes(user.getAttributes());

                super.update(user);

            }

            @Override
            protected User wrap(final User object) {
                // If null, do not process the user further if it does not exist                
                return (object == null) ? null : hvUserFactory.create(object);

            }

        };
    }

}
