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

package org.apache.guacamole.auth.quickconnect;

import com.google.inject.AbstractModule;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.AuthenticationProvider;

/**
 * Guice module to do QuickConnect injections.
 */
public class QuickConnectAuthenticationProviderModule extends AbstractModule {

    /**
     * Guacamole server environment.
     */
    private final Environment environment;

    /**
     * QuickConnect authentication provider.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Create a new instance of the authentication provider module
     * which configures injection for the QuickConnectAuthenticationProvider
     * class.
     *
     * @param authProvider
     *     The authentication provider for which injection is being
     *     configured.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the server environment.
     */
    public QuickConnectAuthenticationProviderModule(AuthenticationProvider authProvider)
        throws GuacamoleException {

        // Create a new local environment
        this.environment = new LocalEnvironment();

        // Initialize authProvider
        this.authProvider = authProvider;

    }

    @Override
    protected void configure() {

        // Bind core implementations of guacamole-ext classes
        bind(AuthenticationProvider.class).toInstance(authProvider);
        bind(Environment.class).toInstance(environment); 

        // Bind QuickConnect-specific classes;
        bind(QuickConnectConnectionGroup.class);
        bind(QuickConnectDirectory.class);
        bind(QuickConnectUserContext.class);
        bind(QuickConnection.class);

    }

}
