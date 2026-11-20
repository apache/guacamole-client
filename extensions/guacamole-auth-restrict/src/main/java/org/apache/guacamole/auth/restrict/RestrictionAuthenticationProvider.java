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

package org.apache.guacamole.auth.restrict;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.restrict.conf.ConfigurationService;
import org.apache.guacamole.auth.restrict.user.RestrictedUserContext;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AuthenticationProvider implementation which provides additional restrictions
 * for users, groups of users, connections, and connection groups, allowing
 * administrators to further control access to Guacamole resources.
 */
public class RestrictionAuthenticationProvider extends AbstractAuthenticationProvider {
    
    /**
     * Logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictionAuthenticationProvider.class);
    
    /**
     * Injector which will manage the object graph of this authentication
     * provider.
     */
    private final Injector injector;
    
    /**
     * Create a new instance of the Restriction authentication provider,
     * setting up the Guice injector for dependency management.
     * 
     * @throws GuacamoleException
     *     If an error occurs configuring the Guice injector.
     */
    public RestrictionAuthenticationProvider() throws GuacamoleException {

        // Set up Guice injector.
        injector = Guice.createInjector(
            new RestrictionAuthenticationProviderModule(this)
        );
    }
    
    @Override
    public String getIdentifier() {
        return "restrict";
    }

    @Override
    public UserContext decorate(UserContext context,
            AuthenticatedUser authenticatedUser, Credentials credentials)
            throws GuacamoleException {

        ConfigurationService confService = injector.getInstance(ConfigurationService.class);
        
        String remoteAddress = credentials.getRemoteAddress();
        User currentUser = context.self();
        boolean isAdmin = currentUser
                    .getEffectivePermissions()
                    .getSystemPermissions()
                    .hasPermission(SystemPermission.Type.ADMINISTER);
        boolean restrictAdmins = confService.getRestrictAdminAccounts();
        
        // User is admin and restrictions do not apply, log warning.
        if (isAdmin && !restrictAdmins) {
            LOGGER.warn("Bypassing restrictions for administrator \"{}\"",
                    currentUser.getIdentifier());
        }
        
        // User is not an admin, or restrictions do apply to admins
        else {
            if (isAdmin) {
                LOGGER.warn("User \"{}\" is an administrator, but system is "
                        + "configured to enforce login restrictions on "
                        + "administrators.",
                        currentUser.getIdentifier());
            }
            else {
                LOGGER.debug("User \"{}\" is not an administrator, enforcing"
                        + "login restrictions.", currentUser.getIdentifier());
            }
            RestrictionVerificationService.verifyLoginRestrictions(context,
                    authenticatedUser.getEffectiveUserGroups(), remoteAddress);

        }

        // User has been verified, and authentication should be allowed to
        // continue
        return new RestrictedUserContext(context, remoteAddress,
                authenticatedUser.getEffectiveUserGroups());

    }

}
