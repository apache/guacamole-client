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

package org.apache.guacamole.auth.radius;

import com.google.inject.AbstractModule;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.radius.conf.ConfigurationService;
import org.apache.guacamole.auth.radius.conf.RadiusAuthenticationProtocol;
import org.apache.guacamole.auth.radius.conf.RadiusGuacamoleProperties;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.environment.LocalEnvironment;
import org.apache.guacamole.net.auth.AuthenticationProvider;

/**
 * Guice module which configures RADIUS-specific injections.
 */
public class RadiusAuthenticationProviderModule extends AbstractModule {

    /**
     * Guacamole server environment.
     */
    private final Environment environment;

    /**
     * A reference to the RadiusAuthenticationProvider on behalf of which this
     * module has configured injection.
     */
    private final AuthenticationProvider authProvider;

    /**
     * Creates a new RADIUS authentication provider module which configures
     * injection for the RadiusAuthenticationProvider.
     *
     * @param authProvider
     *     The AuthenticationProvider for which injection is being configured.
     *
     * @throws GuacamoleException
     *     If an error occurs while retrieving the Guacamole server
     *     environment.
     */
    public RadiusAuthenticationProviderModule(AuthenticationProvider authProvider)
            throws GuacamoleException {

        // Get local environment
        this.environment = new LocalEnvironment();
        
        // Check for MD4 requirement
        RadiusAuthenticationProtocol authProtocol = environment.getProperty(RadiusGuacamoleProperties.RADIUS_AUTH_PROTOCOL);
        RadiusAuthenticationProtocol innerProtocol = environment.getProperty(RadiusGuacamoleProperties.RADIUS_EAP_TTLS_INNER_PROTOCOL);
        if (authProtocol == RadiusAuthenticationProtocol.MSCHAPv1 
                    || authProtocol == RadiusAuthenticationProtocol.MSCHAPv2
                    || innerProtocol == RadiusAuthenticationProtocol.MSCHAPv1 
                    || innerProtocol == RadiusAuthenticationProtocol.MSCHAPv2) {
            
            try {
                Provider md4Provider;
                Constructor providerConstructor = Provider.class
                        .getConstructor(String.class, String.class, String.class);
                if (providerConstructor != null)
                    md4Provider = (Provider) providerConstructor
                            .newInstance("MD4", "0.00", "MD4 for MSCHAPv1/2 Support");
                else
                    md4Provider = (Provider) Provider.class
                            .getConstructor(String.class, Double.class, String.class)
                            .newInstance("MD4", 0.00, "MD4 for MSCHAPv1/2 Support");

                assert(md4Provider != null);
                md4Provider.put("MessageDigest.MD4", org.bouncycastle.jce.provider.JDKMessageDigest.MD4.class.getName());
                Security.addProvider(md4Provider);
            }
            catch (IllegalAccessException
                    | InstantiationException
                    | InvocationTargetException
                    | NoSuchMethodException e) {
                throw new GuacamoleServerException("Unable to load MD4 support.", e);
            }
            
        }

        // Store associated auth provider
        this.authProvider = authProvider;

    }

    @Override
    protected void configure() {

        // Bind core implementations of guacamole-ext classes
        bind(AuthenticationProvider.class).toInstance(authProvider);
        bind(Environment.class).toInstance(environment);

        // Bind RADIUS-specific services
        bind(ConfigurationService.class);
        bind(RadiusConnectionService.class);

    }

}
