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
package org.apache.guacamole.auth.wol.user;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Connection;
import org.apache.guacamole.net.auth.DecoratingDirectory;
import org.apache.guacamole.net.auth.DelegatingUserContext;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.auth.wol.connection.WOLConnection;
import org.apache.guacamole.auth.wol.rest.WOLUserContextResource;
import org.apache.guacamole.form.Form;
import org.apache.guacamole.net.auth.Permissions;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A UserContext for the Wake-on-LAN extension that delegates storage
 * to another UserContext.
 */
public class WOLUserContext extends DelegatingUserContext {

    /**
     * The logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(WOLUserContext.class);

    /**
     * The AuthenticationProvider associated with this UserContext.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Establish a new WOLUserContext, which delegates all functions
     * to the specified UserContext object.
     * 
     * @param userContext 
     *     The UserContext object that this will delegate to.
     * 
     * @param authProvider
     *     The authentication provider associated with this user context.
     */
    public WOLUserContext(UserContext userContext, AuthenticationProvider authProvider) {
        super(userContext);
        this.authProvider = authProvider;
    }

    @Override
    public Directory<Connection> getConnectionDirectory() throws GuacamoleException {
        return new DecoratingDirectory<Connection>(super.getConnectionDirectory()) {

            @Override
            protected Connection decorate(Connection object) throws GuacamoleException {
                Permissions effective = self().getEffectivePermissions();
                SystemPermissionSet systemPermissions = effective.getSystemPermissions();
                ObjectPermissionSet objectPermissions = effective.getConnectionPermissions();
                boolean canUpdate = false;
                if (systemPermissions.hasPermission(SystemPermission.Type.ADMINISTER)
                        || objectPermissions.hasPermission(ObjectPermission.Type.UPDATE, object.getIdentifier()))
                    canUpdate = true;
                return new WOLConnection(object, canUpdate);
            }

            @Override
            protected Connection undecorate(Connection object) {
                assert(object instanceof WOLConnection);
                return ((WOLConnection) object).getUndecorated();
            }
        };
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return authProvider;
    }

    @Override
    public Object getResource() throws GuacamoleException {
        return new WOLUserContextResource(getConnectionDirectory());
    }
    
    @Override
    public Collection<Form> getConnectionAttributes() {
        Collection<Form> allAttrs = new HashSet<>(super.getConnectionAttributes());
        allAttrs.add(WOLConnection.WOL_ATTRIBUTE_FORM);
        return Collections.unmodifiableCollection(allAttrs);
    }

}
