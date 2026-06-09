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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.ConnectionGroup;
import org.apache.guacamole.net.auth.DecoratingDirectory;
import org.apache.guacamole.net.auth.DelegatingUserContext;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;

/**
 * OpenBao/Hashicorp Vault-specific UserContext implementation which wraps the
 * UserContext of some other extension, providing (or hiding) additional data.
 */
public class HvUserContext extends DelegatingUserContext {

    /**
     * Creates a new HvUserContext which wraps the given UserContext,
     * providing (or hiding) additional Hv-specific data.
     *
     * @param userContext
     *     The UserContext to wrap.
     */
    public HvUserContext(final UserContext userContext) {
        super(userContext);
    }

    @Override
    public Directory<User> getUserDirectory() throws GuacamoleException {
        return new DecoratingDirectory<User>(super.getUserDirectory()) {

            @Override
            protected User decorate(final User object) {
                return new HvUser(object);
            }

            @Override
            protected User undecorate(final User object) {
                assert(object instanceof HvUser);
                return ((HvUser) object).getUnderlyingUser();
            }

        };
    }

    @Override
    public Collection<Form> getUserAttributes() {
        final Collection<Form> userAttrs = new HashSet<>(super.getUserAttributes());
        userAttrs.addAll(HvAttributeService.HV_ATTRIBUTES);
        return Collections.unmodifiableCollection(userAttrs);
    }   
    
    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return new DecoratingDirectory<Connection>(super.getConnectionDirectory()) {

            @Override
            protected Connection decorate(final Connection object) throws GuacamoleException {
                return new HvConnection(object);
            }

            @Override
            protected Connection undecorate(final Connection object) {
                assert(object instanceof HvConnection);
                return ((HvConnection) object).getUnderlyingConnection();
            }

        };
    }
    
    @Override
    public Collection<Form> getConnectionAttributes() {
        final Collection<Form> connectionAttrs = new HashSet<>(super.getConnectionAttributes());
        connectionAttrs.addAll(HvAttributeService.HV_CONNECTION_ATTRIBUTES);
        return Collections.unmodifiableCollection(connectionAttrs);
    }

    @Override
    public Directory<ConnectionGroup> getConnectionGroupDirectory() throws GuacamoleException {
        return new DecoratingDirectory<ConnectionGroup>(super.getConnectionGroupDirectory()) {

            @Override
            protected ConnectionGroup decorate(final ConnectionGroup object) throws GuacamoleException {
                return new HvConnectionGroup(object);
            }

            @Override
            protected ConnectionGroup undecorate(final ConnectionGroup object) {
                assert(object instanceof HvConnectionGroup);
                return ((HvConnectionGroup) object).getUnderlyingConnectionGroup();
            }

        };
    }
    
    @Override
    public Collection<Form> getConnectionGroupAttributes() {
        final Collection<Form> connectionGroupAttrs = new HashSet<>(super.getConnectionGroupAttributes());
        connectionGroupAttrs.addAll(HvAttributeService.HV_ATTRIBUTES);
        return Collections.unmodifiableCollection(connectionGroupAttrs);
    }
}
