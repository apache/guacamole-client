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

package org.apache.guacamole.auth.nextcloud;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.StringListProperty;

/**
 * Service for retrieving configuration information regarding the Nextcloud JWT
 * authentication provider.
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * The encryption key to use for all decryption and signature verification.
     */
    private static final StringGuacamoleProperty NEXTCLOUD_JWT_PUBLIC_KEY = new StringGuacamoleProperty() {
        @Override
        public String getName() {
            return "nextcloud-jwt-public-key";
        }

    };

    /**
     * A comma-separated list of all IP addresses or CIDR subnets which should
     * be allowed to perform authentication. If not specified, ALL address will
     * be allowed.
     */
    private static final StringListProperty NEXTCLOUD_JWT_TRUSTED_NETWORKS = new StringListProperty() {

        @Override
        public String getName() {
            return "nextcloud-jwt-trusted-networks";
        }

    };

    /**
     * Property for retrieving the list of users allowed to authenticate via JWT.
     *
     * This property defines a configuration setting that specifies the users permitted
     * to use JWT for authentication.
     */
    private static final StringListProperty NEXTCLOUD_JWT_ALLOWED_USER = new StringListProperty() {

        @Override
        public String getName() {
            return "nextcloud-jwt-allowed-user";
        }

    };

    /**
     * Property for retrieving the name of the token used for authentication.
     *
     * This property defines a configuration setting that specifies the name of the
     * token to be used for authentication purposes.
     */
    private static final StringGuacamoleProperty NEXTCLOUD_JWT_TOKEN_NAME = new StringGuacamoleProperty() {
        @Override
        public String getName() {
            return "nextcloud-jwt-token-name";
        }

    };

    /**
     * Returns the symmetric key which will be used to encrypt and sign all
     * JSON data and should be used to decrypt and verify any received JSON
     * data. This is dictated by the "nextcloud-jwt-public-key" property specified
     * within guacamole.properties.
     *
     * @return
     *     The key which should be used to decrypt received JSON data.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the
     *     "nextcloud-jwt-public-key" property is missing.
     */
    public String getPublicKey() throws GuacamoleException {
        return environment.getRequiredProperty(NEXTCLOUD_JWT_PUBLIC_KEY);
    }

    /**
     * Returns a collection of all IP address or CIDR subnets which should be
     * allowed to submit authentication requests. If empty, authentication
     * attempts will be allowed through without restriction.
     *
     * @return
     *     A collection of all IP address or CIDR subnets which should be
     *     allowed to submit authentication requests.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public Collection<String> getTrustedNetworks() throws GuacamoleException {
        return environment.getProperty(NEXTCLOUD_JWT_TRUSTED_NETWORKS, Collections.<String>emptyList());
    }

    /**
     * Retrieves the collection of users allowed to authenticate via JWT.
     *
     * This method fetches the list of allowed users from the environment properties.
     * If the property is not set, it returns an empty list.
     *
     * @return
     *     A collection of allowed user identifiers.
     *
     * @throws GuacamoleException
     *     If there is an issue retrieving the property.
     */
    public Collection<String> getAllowedUser() throws GuacamoleException {
        return environment.getProperty(NEXTCLOUD_JWT_ALLOWED_USER, Collections.<String>emptyList());
    }

    /**
     * Retrieves the name of the token used for authentication.
     *
     * This method fetches the token name from the environment properties.
     * If the property is not set, it returns the default value "nctoken".
     *
     * @return
     *     The name of the token used for authentication.
     *
     * @throws GuacamoleException
     *     If there is an issue retrieving the property.
     */
    public String getTokenName() throws GuacamoleException {
        return environment.getProperty(NEXTCLOUD_JWT_TOKEN_NAME, "nctoken");
    }

}
